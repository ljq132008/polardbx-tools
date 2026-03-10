package com.alibaba.polardbx.batchweb.service;

import com.alibaba.polardbx.batchweb.entity.JobExecutionDetail;
import com.alibaba.polardbx.batchweb.entity.JobLog;
import com.alibaba.polardbx.batchweb.mapper.JobExecutionDetailMapper;
import com.alibaba.polardbx.batchweb.mapper.JobLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务日志服务
 */
@Slf4j
@Service
public class JobLogService extends ServiceImpl<JobLogMapper, JobLog> {

    @Autowired
    private JobExecutionDetailMapper executionDetailMapper;

    /**
     * 添加日志
     */
    public void addLog(String jobId, String level, String message) {
        JobLog logEntry = new JobLog();
        logEntry.setJobId(jobId);
        logEntry.setLogLevel(level);
        logEntry.setLogMessage(message);
        save(logEntry);
    }

    /**
     * 添加 INFO 日志
     */
    public void info(String jobId, String message) {
        addLog(jobId, "INFO", message);
        log.info("[{}] {}", jobId, message);
    }

    /**
     * 添加 WARN 日志
     */
    public void warn(String jobId, String message) {
        addLog(jobId, "WARN", message);
        log.warn("[{}] {}", jobId, message);
    }

    /**
     * 添加 ERROR 日志
     */
    public void error(String jobId, String message) {
        addLog(jobId, "ERROR", message);
        log.error("[{}] {}", jobId, message);
    }

    /**
     * 获取任务日志
     */
    public List<JobLog> getLogsByJobId(String jobId) {
        return list(new LambdaQueryWrapper<JobLog>()
                .eq(JobLog::getJobId, jobId)
                .orderByAsc(JobLog::getCreateTime));
    }

    /**
     * 添加执行详情
     */
    public JobExecutionDetail addExecutionDetail(String jobId, String phase, String phaseStatus, String detailInfo) {
        JobExecutionDetail detail = new JobExecutionDetail();
        detail.setJobId(jobId);
        detail.setPhase(phase);
        detail.setPhaseStatus(phaseStatus);
        detail.setDetailInfo(detailInfo);
        detail.setStartTime(LocalDateTime.now());
        executionDetailMapper.insert(detail);
        return detail;
    }

    /**
     * 完成执行详情
     */
    public void completeExecutionDetail(JobExecutionDetail detail, String phaseStatus, String detailInfo) {
        if (detail != null) {
            detail.setEndTime(LocalDateTime.now());
            if (detail.getStartTime() != null) {
                detail.setDurationMs(java.time.Duration.between(detail.getStartTime(), detail.getEndTime()).toMillis());
            }
            detail.setPhaseStatus(phaseStatus);
            if (detailInfo != null) {
                detail.setDetailInfo(detailInfo);
            }
            executionDetailMapper.updateById(detail);
        }
    }

    /**
     * 获取执行详情列表
     */
    public List<JobExecutionDetail> getExecutionDetails(String jobId) {
        return executionDetailMapper.selectList(new LambdaQueryWrapper<JobExecutionDetail>()
                .eq(JobExecutionDetail::getJobId, jobId)
                .orderByAsc(JobExecutionDetail::getCreateTime));
    }

    /**
     * 清理过期日志
     */
    public void cleanOldLogs(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        
        // 清理日志
        remove(new LambdaQueryWrapper<JobLog>()
                .lt(JobLog::getCreateTime, cutoff));
        
        // 清理执行详情
        executionDetailMapper.delete(new LambdaQueryWrapper<JobExecutionDetail>()
                .lt(JobExecutionDetail::getCreateTime, cutoff));
        
        log.info("Cleaned logs and details older than {} days", days);
    }
}
