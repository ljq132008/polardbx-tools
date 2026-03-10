package com.alibaba.polardbx.batchweb.controller;

import com.alibaba.polardbx.batchweb.dto.*;
import com.alibaba.polardbx.batchweb.entity.BatchJobEntity;
import com.alibaba.polardbx.batchweb.entity.JobExecutionDetail;
import com.alibaba.polardbx.batchweb.entity.JobLog;
import com.alibaba.polardbx.batchweb.entity.JobRuleDetail;
import com.alibaba.polardbx.batchweb.service.BatchJobService;
import com.alibaba.polardbx.batchweb.service.JobLogService;
import com.alibaba.polardbx.batchweb.service.JobManager;
import com.alibaba.polardbx.batchweb.service.JobRuleDetailService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务管理 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    @Autowired
    private JobManager jobManager;

    @Autowired
    private BatchJobService batchJobService;

    @Autowired
    private JobLogService jobLogService;

    @Autowired
    private JobRuleDetailService jobRuleDetailService;

    /**
     * 获取任务列表（分页）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobDetailDto>>> list(JobQueryDto query) {
        Page<BatchJobEntity> page = batchJobService.queryPage(query);
        Page<JobDetailDto> dtoPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        dtoPage.setRecords(page.getRecords().stream()
                .map(batchJobService::toDto)
                .collect(Collectors.toList()));
        return ResponseEntity.ok(ApiResponse.success(dtoPage));
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<JobDetailDto>> getById(@PathVariable String jobId) {
        BatchJobEntity job = batchJobService.getByJobId(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(batchJobService.toDto(job)));
    }

    /**
     * 获取任务进度
     */
    @GetMapping("/{jobId}/progress")
    public ResponseEntity<ApiResponse<JobProgress>> getProgress(@PathVariable String jobId) {
        JobProgress progress = jobManager.getJobProgress(jobId);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(progress));
    }

    /**
     * 获取任务日志
     */
    @GetMapping("/{jobId}/logs")
    public ResponseEntity<ApiResponse<List<JobLog>>> getLogs(@PathVariable String jobId) {
        List<JobLog> logs = jobLogService.getLogsByJobId(jobId);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * 获取任务执行详情
     */
    @GetMapping("/{jobId}/details")
    public ResponseEntity<ApiResponse<List<JobExecutionDetail>>> getDetails(@PathVariable String jobId) {
        List<JobExecutionDetail> details = jobLogService.getExecutionDetails(jobId);
        return ResponseEntity.ok(ApiResponse.success(details));
    }

    /**
     * 获取任务规则快照列表（规则组执行时使用）
     */
    @GetMapping("/{jobId}/rules")
    public ResponseEntity<ApiResponse<List<JobRuleDetail>>> getRuleDetails(@PathVariable String jobId) {
        List<JobRuleDetail> ruleDetails = jobRuleDetailService.listByJobId(jobId);
        return ResponseEntity.ok(ApiResponse.success(ruleDetails));
    }

    /**
     * 取消任务
     */
    @PostMapping("/{jobId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable String jobId) {
        boolean success = jobManager.cancelJob(jobId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("Job cancelled", null));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("Job cannot be cancelled"));
        }
    }

    /**
     * 重试任务
     */
    @PostMapping("/{jobId}/retry")
    public ResponseEntity<ApiResponse<String>> retry(@PathVariable String jobId) {
        try {
            String newJobId = jobManager.retryJob(jobId);
            return ResponseEntity.ok(ApiResponse.success("Retry job created", newJobId));
        } catch (Exception e) {
            log.error("Failed to retry job", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{jobId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String jobId) {
        BatchJobEntity job = batchJobService.getByJobId(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        if ("RUNNING".equals(job.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("Cannot delete running job"));
        }
        batchJobService.removeById(job.getId());
        return ResponseEntity.ok(ApiResponse.success("Job deleted", null));
    }

    /**
     * 清理过期任务和日志
     */
    @PostMapping("/clean")
    public ResponseEntity<ApiResponse<Void>> cleanOldJobs(@RequestParam(defaultValue = "7") int days) {
        try {
            jobLogService.cleanOldLogs(days);
            return ResponseEntity.ok(ApiResponse.success("Old jobs and logs cleaned", null));
        } catch (Exception e) {
            log.error("Failed to clean old jobs", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取运行中的任务数量
     */
    @GetMapping("/running-count")
    public ResponseEntity<ApiResponse<Long>> getRunningCount() {
        long count = batchJobService.countRunningJobs();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
