package com.alibaba.polardbx.batchweb.service;

import com.alibaba.polardbx.batchweb.dto.BatchRequest;
import com.alibaba.polardbx.batchweb.dto.JobProgress;
import com.alibaba.polardbx.batchweb.entity.BatchJobEntity;
import com.alibaba.polardbx.batchweb.entity.JobExecutionDetail;
import com.alibaba.polardbx.batchweb.entity.JobRuleDetail;
import com.alibaba.polardbx.batchweb.entity.MigrationRule;
import com.alibaba.polardbx.batchweb.entity.RuleGroup;
import com.alibaba.polardbx.batchweb.util.IdGenerator;
import cmd.CommandType;
import cmd.CommandUtil;
import cmd.ConfigResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 任务管理器
 */
@Slf4j
@Service
public class JobManager {

    @Autowired
    private BatchExecutor batchExecutor;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private BatchJobService batchJobService;

    @Autowired
    private JobLogService jobLogService;

    @Autowired
    private RuleGroupService ruleGroupService;

    @Autowired
    private MigrationRuleService migrationRuleService;

    @Autowired
    private JobRuleDetailService jobRuleDetailService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final AtomicInteger runningJobs = new AtomicInteger(0);
    private static final int MAX_CONCURRENT_JOBS = 5;
    private static final int DEFAULT_CONCURRENCY = 1;

    @PostConstruct
    public void init() {
        log.info("JobManager initialized");
    }

    @PreDestroy
    public void destroy() {
        executorService.shutdown();
        log.info("JobManager shutdown");
    }

    /**
     * 创建批量操作任务
     */
    public String createBatchJob(BatchRequest request) {
        if (runningJobs.get() >= MAX_CONCURRENT_JOBS) {
            throw new RuntimeException("Too many concurrent jobs, please try again later");
        }

        // 预解析命令获取操作类型
        String commandArgs = request.getCommandArgs();
        CommandType commandType = parseCommandType(commandArgs);

        String jobId = IdGenerator.generateId();
        
        // 创建任务记录
        BatchJobEntity job = batchJobService.createJob(
                jobId, null, null, null,
                commandType.name(), null, null, null, null,
                commandArgs, null
        );

        log.info("Created {} job: {}", commandType.name(), jobId);

        // 提交异步任务
        executorService.submit(() -> executeJob(jobId));

        return jobId;
    }

    /**
     * 根据规则创建任务
     */
    public String createJobFromRule(Long ruleId) {
        if (runningJobs.get() >= MAX_CONCURRENT_JOBS) {
            throw new RuntimeException("Too many concurrent jobs, please try again later");
        }

        MigrationRule rule = migrationRuleService.getById(ruleId);
        if (rule == null) {
            throw new RuntimeException("Rule not found: " + ruleId);
        }

        RuleGroup group = ruleGroupService.getById(rule.getGroupId());
        if (group == null) {
            throw new RuntimeException("Rule group not found: " + rule.getGroupId());
        }

        String jobId = IdGenerator.generateId();
        String commandArgs = migrationRuleService.buildCommandArgs(rule, group);

        // 创建任务记录
        BatchJobEntity job = batchJobService.createJob(
                jobId, group.getId(), rule.getId(), rule.getRuleName(),
                rule.getOperationType(), rule.getSourceDatabase(), rule.getSourceTable(),
                rule.getTargetDatabase(), rule.getTargetTable(), commandArgs, null
        );

        log.info("Created job from rule {}: {}", ruleId, jobId);

        // 提交异步任务
        executorService.submit(() -> executeJob(jobId));

        return jobId;
    }

    /**
     * 执行规则组下所有规则（兼容旧接口，使用默认并发度）
     */
    public List<String> executeRuleGroup(Long groupId) {
        return executeRuleGroup(groupId, DEFAULT_CONCURRENCY);
    }

    /**
     * 执行规则组下所有规则（支持自定义并发度）
     * @param groupId 规则组ID
     * @param concurrency 并发执行数（1-10）
     * @return 任务ID列表
     */
    public List<String> executeRuleGroup(Long groupId, Integer concurrency) {
        RuleGroup group = ruleGroupService.getById(groupId);
        if (group == null) {
            throw new RuntimeException("Rule group not found: " + groupId);
        }

        if (group.getStatus() != 1) {
            throw new RuntimeException("Rule group is disabled: " + groupId);
        }

        List<MigrationRule> rules = migrationRuleService.listByGroupId(groupId);
        if (rules.isEmpty()) {
            throw new RuntimeException("No enabled rules found in group: " + groupId);
        }

        // 限制并发度范围
        int validConcurrency = Math.max(1, Math.min(concurrency != null ? concurrency : DEFAULT_CONCURRENCY, 10));

        // 创建一个主任务来跟踪整个规则组的执行
        String mainJobId = IdGenerator.generateId();
        BatchJobEntity mainJob = batchJobService.createJob(
                mainJobId, groupId, null, "RuleGroup-" + groupId,
                "GROUP", null, null, null, null, null, null
        );
        mainJob.setConcurrency(validConcurrency);
        mainJob.setTotalRules(rules.size());
        mainJob.setCompletedRules(0);
        mainJob.setFailedRules(0);
        batchJobService.updateById(mainJob);

        log.info("Created rule group job: {} with concurrency: {}", mainJobId, validConcurrency);
        jobLogService.info(mainJobId, "Rule group job created with " + rules.size() + " rules, concurrency: " + validConcurrency);

        // 提交异步执行任务
        executorService.submit(() -> executeRuleGroupConcurrently(mainJobId, groupId, rules, validConcurrency));

        List<String> jobIds = new java.util.ArrayList<>();
        jobIds.add(mainJobId);
        return jobIds;
    }

    /**
     * 并发执行规则组中的所有规则
     */
    private void executeRuleGroupConcurrently(String mainJobId, Long groupId, List<MigrationRule> rules, int concurrency) {
        batchJobService.startJob(mainJobId);
        runningJobs.incrementAndGet();

        log.info("Starting rule group execution: {}, rules: {}, concurrency: {}", mainJobId, rules.size(), concurrency);
        jobLogService.info(mainJobId, "Starting concurrent execution of " + rules.size() + " rules");

        RuleGroup group = ruleGroupService.getById(groupId);

        // 创建规则快照并构建执行任务列表
        List<RuleExecutionTask> tasks = new java.util.ArrayList<>();
        for (MigrationRule rule : rules) {
            String ruleJobId = IdGenerator.generateId();
            String commandArgs = migrationRuleService.buildCommandArgs(rule, group);

            // 保存规则快照到 JobRuleDetail 表
            JobRuleDetail ruleDetail = jobRuleDetailService.saveRuleSnapshot(mainJobId, rule, commandArgs);

            tasks.add(new RuleExecutionTask(ruleJobId, mainJobId, rule, ruleDetail.getId(), commandArgs));
        }

        // 创建并发执行线程池
        ExecutorService concurrentExecutor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch latch = new CountDownLatch(rules.size());

        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        try {
            // 提交所有任务
            for (RuleExecutionTask task : tasks) {
                concurrentExecutor.submit(() -> {
                    try {
                        executeSingleRule(task, completedCount, failedCount, rules.size());
                    } catch (Exception e) {
                        log.error("Failed to execute rule {}: {}", task.getRule().getId(), e.getMessage());
                        jobRuleDetailService.failRuleDetail(task.getDetailId(), e.getMessage());
                        failedCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                        // 更新主任务进度
                        updateGroupJobProgress(mainJobId, completedCount.get(), failedCount.get(), rules.size());
                    }
                });
            }

            // 等待所有任务完成（最多等待24小时）
            latch.await(24, TimeUnit.HOURS);

            // 更新主任务状态
            if (failedCount.get() == 0) {
                batchJobService.completeJob(mainJobId);
                jobLogService.info(mainJobId, "All rules completed successfully");
            } else if (completedCount.get() > 0) {
                // 部分成功
                batchJobService.failJob(mainJobId, 
                    String.format("Partially completed: %d success, %d failed", completedCount.get(), failedCount.get()));
                jobLogService.warn(mainJobId, 
                    String.format("Partially completed: %d success, %d failed", completedCount.get(), failedCount.get()));
            } else {
                batchJobService.failJob(mainJobId, "All rules failed");
                jobLogService.error(mainJobId, "All rules failed");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            batchJobService.failJob(mainJobId, "Execution interrupted");
            jobLogService.error(mainJobId, "Execution interrupted");
        } finally {
            concurrentExecutor.shutdown();
            runningJobs.decrementAndGet();
            broadcastProgress(mainJobId);
            log.info("Rule group execution completed: {}, success: {}, failed: {}", 
                mainJobId, completedCount.get(), failedCount.get());
        }
    }

    /**
     * 执行单个规则
     */
    private void executeSingleRule(RuleExecutionTask task, AtomicInteger completedCount, AtomicInteger failedCount, int total) {
        JobExecutionDetail detail = null;
        try {
            // 更新规则快照状态为运行中
            jobRuleDetailService.startRuleDetail(task.getDetailId());
            jobLogService.info(task.getMainJobId(), 
                String.format("Executing rule [%d/%d]: %s", completedCount.get() + failedCount.get() + 1, total, task.getRule().getRuleName()));

            // 执行批量操作
            batchExecutor.executeBatchOp(task.getCommandArgs(), task.getRuleJobId());

            // 更新规则快照状态为完成
            jobRuleDetailService.completeRuleDetail(task.getDetailId());
            completedCount.incrementAndGet();

            jobLogService.info(task.getMainJobId(), 
                String.format("Completed rule: %s", task.getRule().getRuleName()));

        } catch (Exception e) {
            jobRuleDetailService.failRuleDetail(task.getDetailId(), e.getMessage());
            failedCount.incrementAndGet();
            jobLogService.error(task.getMainJobId(), 
                String.format("Failed rule %s: %s", task.getRule().getRuleName(), e.getMessage()));
            log.error("Failed to execute rule {}: {}", task.getRule().getId(), e.getMessage(), e);
        }
    }

    /**
     * 更新规则组任务进度
     */
    private void updateGroupJobProgress(String mainJobId, int completed, int failed, int total) {
        BatchJobEntity job = batchJobService.getByJobId(mainJobId);
        if (job != null) {
            job.setCompletedRules(completed);
            job.setFailedRules(failed);
            // 计算整体进度百分比
            int progress = (int) ((completed + failed) * 100.0 / total);
            job.setProgress(Math.min(progress, 100));
            batchJobService.updateById(job);
            broadcastProgress(mainJobId);
        }
    }

    /**
     * 规则执行任务内部类
     */
    private static class RuleExecutionTask {
        private final String ruleJobId;
        private final String mainJobId;
        private final MigrationRule rule;
        private final Long detailId;
        private final String commandArgs;

        public RuleExecutionTask(String ruleJobId, String mainJobId, MigrationRule rule, Long detailId, String commandArgs) {
            this.ruleJobId = ruleJobId;
            this.mainJobId = mainJobId;
            this.rule = rule;
            this.detailId = detailId;
            this.commandArgs = commandArgs;
        }

        public String getRuleJobId() { return ruleJobId; }
        public String getMainJobId() { return mainJobId; }
        public MigrationRule getRule() { return rule; }
        public Long getDetailId() { return detailId; }
        public String getCommandArgs() { return commandArgs; }
    }

    /**
     * 从命令行参数解析操作类型
     */
    private CommandType parseCommandType(String commandArgs) {
        try {
            String[] args = batchExecutor.parseArgs(commandArgs);
            ConfigResult configResult = CommandUtil.parseStartUpCommand(args);
            if (configResult == null) {
                throw new IllegalArgumentException("Invalid command arguments");
            }
            String opValue = configResult.getOptionValue(cmd.ConfigArgOption.ARG_SHORT_OPERATION);
            if (opValue == null) {
                throw new IllegalArgumentException("Missing -o/--operation parameter");
            }
            return CommandType.valueOf(opValue.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse command: " + e.getMessage(), e);
        }
    }

    /**
     * 执行任务
     */
    @Async
    public void executeJob(String jobId) {
        BatchJobEntity job = batchJobService.getByJobId(jobId);
        if (job == null) {
            log.error("Job not found: {}", jobId);
            return;
        }

        JobExecutionDetail detail = null;
        try {
            batchJobService.startJob(jobId);
            runningJobs.incrementAndGet();

            log.info("Job started: {}", jobId);
            jobLogService.info(jobId, "Job started");
            broadcastProgress(jobId);

            // 记录执行详情
            detail = jobLogService.addExecutionDetail(jobId, "EXECUTE", "RUNNING", "Executing batch operation");

            // 执行批量操作
            batchExecutor.executeBatchOp(job.getCommandArgs(), jobId);

            batchJobService.completeJob(jobId);
            jobLogService.info(jobId, "Job completed successfully");
            log.info("Job completed: {}", jobId);

            if (detail != null) {
                jobLogService.completeExecutionDetail(detail, "COMPLETED", "Batch operation completed");
            }

        } catch (Exception e) {
            batchJobService.failJob(jobId, e.getMessage());
            jobLogService.error(jobId, "Job failed: " + e.getMessage());
            log.error("Job failed: {}", jobId, e);

            if (detail != null) {
                jobLogService.completeExecutionDetail(detail, "FAILED", e.getMessage());
            }
        } finally {
            runningJobs.decrementAndGet();
            broadcastProgress(jobId);
        }
    }

    /**
     * 取消任务
     */
    public boolean cancelJob(String jobId) {
        BatchJobEntity job = batchJobService.getByJobId(jobId);
        if (job == null) {
            return false;
        }

        if ("RUNNING".equals(job.getStatus())) {
            batchJobService.cancelJob(jobId);
            jobLogService.warn(jobId, "Job cancelled by user");
            runningJobs.decrementAndGet();
            log.info("Job cancelled: {}", jobId);
            broadcastProgress(jobId);
            return true;
        }
        return false;
    }

    /**
     * 重试任务
     */
    public String retryJob(String jobId) {
        BatchJobEntity job = batchJobService.getByJobId(jobId);
        if (job == null) {
            throw new RuntimeException("Job not found: " + jobId);
        }

        if (!"FAILED".equals(job.getStatus()) && !"CANCELLED".equals(job.getStatus())) {
            throw new RuntimeException("Only failed or cancelled jobs can be retried");
        }

        // 创建新任务
        String newJobId = IdGenerator.generateId();
        BatchJobEntity newJob = batchJobService.createJob(
                newJobId, job.getGroupId(), job.getRuleId(), job.getJobName(),
                job.getOperationType(), job.getSourceDatabase(), job.getSourceTable(),
                job.getTargetDatabase(), job.getTargetTable(), job.getCommandArgs(),
                job.getCreateBy()
        );

        log.info("Created retry job: {} from {}", newJobId, jobId);
        executorService.submit(() -> executeJob(newJobId));

        return newJobId;
    }

    /**
     * 获取任务信息
     */
    public Optional<BatchJobEntity> getJob(String jobId) {
        return Optional.ofNullable(batchJobService.getByJobId(jobId));
    }

    /**
     * 获取任务进度
     */
    public JobProgress getJobProgress(String jobId) {
        BatchJobEntity job = batchJobService.getByJobId(jobId);
        if (job == null) {
            return null;
        }
        return JobProgress.builder()
                .jobId(job.getJobId())
                .progress(job.getProgress())
                .totalRows(job.getTotalRows())
                .processedRows(job.getProcessedRows())
                .speed(job.getSpeed() != null ? job.getSpeed().doubleValue() : 0.0)
                .status(job.getStatus())
                .message(job.getErrorMsg())
                .build();
    }

    /**
     * 更新任务进度
     */
    public void updateJobProgress(String jobId, long processedRows) {
        BatchJobEntity job = batchJobService.getByJobId(jobId);
        if (job != null) {
            batchJobService.updateProgress(jobId, job.getTotalRows(), processedRows);
            broadcastProgress(jobId);
        }
    }

    /**
     * 设置总行数
     */
    public void setJobTotalRows(String jobId, long totalRows) {
        BatchJobEntity job = batchJobService.getByJobId(jobId);
        if (job != null) {
            batchJobService.updateProgress(jobId, totalRows, job.getProcessedRows());
            broadcastProgress(jobId);
        }
    }

    /**
     * 广播进度
     */
    private void broadcastProgress(String jobId) {
        JobProgress progress = getJobProgress(jobId);
        if (progress != null) {
            webSocketService.broadcastProgress(jobId, progress);
        }
    }
}
