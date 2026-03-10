package com.alibaba.polardbx.batchweb.service;

import com.alibaba.polardbx.batchweb.entity.JobRuleDetail;
import com.alibaba.polardbx.batchweb.entity.MigrationRule;
import com.alibaba.polardbx.batchweb.mapper.JobRuleDetailMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务规则快照服务
 */
@Slf4j
@Service
public class JobRuleDetailService extends ServiceImpl<JobRuleDetailMapper, JobRuleDetail> {

    /**
     * 保存规则快照
     */
    @Transactional(rollbackFor = Exception.class)
    public JobRuleDetail saveRuleSnapshot(String jobId, MigrationRule rule, String commandArgs) {
        JobRuleDetail detail = new JobRuleDetail();
        detail.setJobId(jobId);
        detail.setRuleId(rule.getId());
        detail.setRuleName(rule.getRuleName());
        detail.setSourceDatabase(rule.getSourceDatabase());
        detail.setSourceTable(rule.getSourceTable());
        detail.setTargetDatabase(rule.getTargetDatabase());
        detail.setTargetTable(rule.getTargetTable());
        detail.setOperationType(rule.getOperationType());
        detail.setColumns(rule.getColumns());
        detail.setWhereCondition(rule.getWhereCondition());
        detail.setSeparator(rule.getSeparator());
        detail.setFileDirectory(rule.getFileDirectory());
        detail.setFilePrefix(rule.getFilePrefix());
        detail.setCompress(rule.getCompress());
        detail.setWithDdl(rule.getWithDdl());
        detail.setWithHeader(rule.getWithHeader());
        detail.setProducerCount(rule.getProducerCount());
        detail.setConsumerCount(rule.getConsumerCount());
        detail.setBatchSize(rule.getBatchSize());
        detail.setExtraParams(rule.getExtraParams());
        detail.setPriority(rule.getPriority());
        detail.setCommandArgs(commandArgs);
        detail.setStatus("PENDING");
        detail.setTotalRows(0L);
        detail.setProcessedRows(0L);
        detail.setProgress(0);
        detail.setDuration(0L);

        save(detail);
        log.info("Saved rule snapshot for job {}: rule {}", jobId, rule.getId());
        return detail;
    }

    /**
     * 根据任务ID获取所有规则快照
     */
    public List<JobRuleDetail> listByJobId(String jobId) {
        return list(new LambdaQueryWrapper<JobRuleDetail>()
                .eq(JobRuleDetail::getJobId, jobId)
                .orderByAsc(JobRuleDetail::getPriority)
                .orderByAsc(JobRuleDetail::getId));
    }

    /**
     * 更新规则快照状态为运行中
     */
    public void startRuleDetail(Long detailId) {
        JobRuleDetail detail = getById(detailId);
        if (detail != null) {
            detail.setStatus("RUNNING");
            detail.setStartTime(LocalDateTime.now());
            updateById(detail);
        }
    }

    /**
     * 更新规则快照状态为完成
     */
    public void completeRuleDetail(Long detailId) {
        JobRuleDetail detail = getById(detailId);
        if (detail != null) {
            detail.setStatus("COMPLETED");
            detail.setProgress(100);
            detail.setEndTime(LocalDateTime.now());
            if (detail.getStartTime() != null) {
                long duration = java.time.Duration.between(detail.getStartTime(), detail.getEndTime()).getSeconds();
                detail.setDuration(duration);
            }
            updateById(detail);
        }
    }

    /**
     * 更新规则快照状态为失败
     */
    public void failRuleDetail(Long detailId, String errorMsg) {
        JobRuleDetail detail = getById(detailId);
        if (detail != null) {
            detail.setStatus("FAILED");
            detail.setErrorMsg(errorMsg);
            detail.setEndTime(LocalDateTime.now());
            if (detail.getStartTime() != null) {
                long duration = java.time.Duration.between(detail.getStartTime(), detail.getEndTime()).getSeconds();
                detail.setDuration(duration);
            }
            updateById(detail);
        }
    }

    /**
     * 更新规则快照进度
     */
    public void updateProgress(Long detailId, long totalRows, long processedRows) {
        JobRuleDetail detail = getById(detailId);
        if (detail != null) {
            detail.setTotalRows(totalRows);
            detail.setProcessedRows(processedRows);
            if (totalRows > 0) {
                int progress = (int) (processedRows * 100 / totalRows);
                detail.setProgress(Math.min(progress, 100));
            }
            updateById(detail);
        }
    }
}
