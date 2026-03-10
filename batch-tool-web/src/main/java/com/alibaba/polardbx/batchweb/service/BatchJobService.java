package com.alibaba.polardbx.batchweb.service;

import com.alibaba.polardbx.batchweb.dto.JobDetailDto;
import com.alibaba.polardbx.batchweb.dto.JobQueryDto;
import com.alibaba.polardbx.batchweb.entity.BatchJobEntity;
import com.alibaba.polardbx.batchweb.mapper.BatchJobMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 批量任务服务
 */
@Slf4j
@Service
public class BatchJobService extends ServiceImpl<BatchJobMapper, BatchJobEntity> {

    @Autowired
    private JobLogService jobLogService;

    /**
     * 创建任务
     */
    public BatchJobEntity createJob(String jobId, Long groupId, Long ruleId, String jobName,
                                     String operationType, String sourceDatabase, String sourceTable,
                                     String targetDatabase, String targetTable, String commandArgs,
                                     String createBy) {
        BatchJobEntity job = new BatchJobEntity();
        job.setJobId(jobId);
        job.setGroupId(groupId);
        job.setRuleId(ruleId);
        job.setJobName(jobName);
        job.setOperationType(operationType);
        job.setSourceDatabase(sourceDatabase);
        job.setSourceTable(sourceTable);
        job.setTargetDatabase(targetDatabase);
        job.setTargetTable(targetTable);
        job.setCommandArgs(commandArgs);
        job.setStatus("PENDING");
        job.setTotalRows(0L);
        job.setProcessedRows(0L);
        job.setProgress(0);
        job.setSpeed(BigDecimal.ZERO);
        job.setDuration(0L);
        job.setCreateBy(createBy);

        save(job);
        log.info("Created job: {}", jobId);
        return job;
    }

    /**
     * 更新任务状态
     */
    public void updateStatus(String jobId, String status) {
        update(new LambdaUpdateWrapper<BatchJobEntity>()
                .eq(BatchJobEntity::getJobId, jobId)
                .set(BatchJobEntity::getStatus, status));
    }

    /**
     * 开始任务
     */
    public void startJob(String jobId) {
        update(new LambdaUpdateWrapper<BatchJobEntity>()
                .eq(BatchJobEntity::getJobId, jobId)
                .set(BatchJobEntity::getStatus, "RUNNING")
                .set(BatchJobEntity::getStartTime, LocalDateTime.now()));
    }

    /**
     * 完成任务
     */
    public void completeJob(String jobId) {
        BatchJobEntity job = getByJobId(jobId);
        if (job != null) {
            LocalDateTime endTime = LocalDateTime.now();
            long duration = 0;
            if (job.getStartTime() != null) {
                duration = java.time.Duration.between(job.getStartTime(), endTime).getSeconds();
            }
            
            update(new LambdaUpdateWrapper<BatchJobEntity>()
                    .eq(BatchJobEntity::getJobId, jobId)
                    .set(BatchJobEntity::getStatus, "COMPLETED")
                    .set(BatchJobEntity::getEndTime, endTime)
                    .set(BatchJobEntity::getDuration, duration)
                    .set(BatchJobEntity::getProgress, 100));
        }
    }

    /**
     * 任务失败
     */
    public void failJob(String jobId, String errorMsg) {
        BatchJobEntity job = getByJobId(jobId);
        if (job != null) {
            LocalDateTime endTime = LocalDateTime.now();
            long duration = 0;
            if (job.getStartTime() != null) {
                duration = java.time.Duration.between(job.getStartTime(), endTime).getSeconds();
            }

            update(new LambdaUpdateWrapper<BatchJobEntity>()
                    .eq(BatchJobEntity::getJobId, jobId)
                    .set(BatchJobEntity::getStatus, "FAILED")
                    .set(BatchJobEntity::getErrorMsg, errorMsg)
                    .set(BatchJobEntity::getEndTime, endTime)
                    .set(BatchJobEntity::getDuration, duration));
        }
    }

    /**
     * 取消任务
     */
    public void cancelJob(String jobId) {
        BatchJobEntity job = getByJobId(jobId);
        if (job != null) {
            LocalDateTime endTime = LocalDateTime.now();
            long duration = 0;
            if (job.getStartTime() != null) {
                duration = java.time.Duration.between(job.getStartTime(), endTime).getSeconds();
            }

            update(new LambdaUpdateWrapper<BatchJobEntity>()
                    .eq(BatchJobEntity::getJobId, jobId)
                    .set(BatchJobEntity::getStatus, "CANCELLED")
                    .set(BatchJobEntity::getEndTime, endTime)
                    .set(BatchJobEntity::getDuration, duration));
        }
    }

    /**
     * 更新进度
     */
    public void updateProgress(String jobId, long totalRows, long processedRows) {
        int progress = 0;
        if (totalRows > 0) {
            progress = (int) ((processedRows * 100) / totalRows);
        }

        // 计算速度
        BigDecimal speed = BigDecimal.ZERO;
        BatchJobEntity job = getByJobId(jobId);
        if (job != null && job.getStartTime() != null) {
            long seconds = java.time.Duration.between(job.getStartTime(), LocalDateTime.now()).getSeconds();
            if (seconds > 0) {
                speed = BigDecimal.valueOf(processedRows)
                        .divide(BigDecimal.valueOf(seconds), 2, RoundingMode.HALF_UP);
            }
        }

        update(new LambdaUpdateWrapper<BatchJobEntity>()
                .eq(BatchJobEntity::getJobId, jobId)
                .set(BatchJobEntity::getTotalRows, totalRows)
                .set(BatchJobEntity::getProcessedRows, processedRows)
                .set(BatchJobEntity::getProgress, progress)
                .set(BatchJobEntity::getSpeed, speed));
    }

    /**
     * 根据 jobId 获取任务
     */
    public BatchJobEntity getByJobId(String jobId) {
        return getOne(new LambdaQueryWrapper<BatchJobEntity>()
                .eq(BatchJobEntity::getJobId, jobId));
    }

    /**
     * 分页查询
     */
    public Page<BatchJobEntity> queryPage(JobQueryDto query) {
        LambdaQueryWrapper<BatchJobEntity> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getJobId())) {
            wrapper.eq(BatchJobEntity::getJobId, query.getJobId());
        }
        if (query.getGroupId() != null) {
            wrapper.eq(BatchJobEntity::getGroupId, query.getGroupId());
        }
        if (query.getRuleId() != null) {
            wrapper.eq(BatchJobEntity::getRuleId, query.getRuleId());
        }
        if (StringUtils.hasText(query.getOperationType())) {
            wrapper.eq(BatchJobEntity::getOperationType, query.getOperationType().toUpperCase());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(BatchJobEntity::getStatus, query.getStatus().toUpperCase());
        }
        if (StringUtils.hasText(query.getSourceDatabase())) {
            wrapper.eq(BatchJobEntity::getSourceDatabase, query.getSourceDatabase());
        }
        if (StringUtils.hasText(query.getSourceTable())) {
            wrapper.eq(BatchJobEntity::getSourceTable, query.getSourceTable());
        }
        if (StringUtils.hasText(query.getCreateBy())) {
            wrapper.eq(BatchJobEntity::getCreateBy, query.getCreateBy());
        }

        wrapper.orderByDesc(BatchJobEntity::getCreateTime);

        return page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    /**
     * 获取运行中的任务数量
     */
    public long countRunningJobs() {
        return count(new LambdaQueryWrapper<BatchJobEntity>()
                .eq(BatchJobEntity::getStatus, "RUNNING"));
    }

    /**
     * 转换为 DTO
     */
    public JobDetailDto toDto(BatchJobEntity entity) {
        return JobDetailDto.builder()
                .id(entity.getId())
                .jobId(entity.getJobId())
                .groupId(entity.getGroupId())
                .ruleId(entity.getRuleId())
                .jobName(entity.getJobName())
                .operationType(entity.getOperationType())
                .sourceDatabase(entity.getSourceDatabase())
                .sourceTable(entity.getSourceTable())
                .targetDatabase(entity.getTargetDatabase())
                .targetTable(entity.getTargetTable())
                .commandArgs(entity.getCommandArgs())
                .status(entity.getStatus())
                .totalRows(entity.getTotalRows())
                .processedRows(entity.getProcessedRows())
                .progress(entity.getProgress())
                .speed(entity.getSpeed())
                .errorMsg(entity.getErrorMsg())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .duration(entity.getDuration())
                .concurrency(entity.getConcurrency())
                .totalRules(entity.getTotalRules())
                .completedRules(entity.getCompletedRules())
                .failedRules(entity.getFailedRules())
                .createTime(entity.getCreateTime())
                .createBy(entity.getCreateBy())
                .build();
    }
}
