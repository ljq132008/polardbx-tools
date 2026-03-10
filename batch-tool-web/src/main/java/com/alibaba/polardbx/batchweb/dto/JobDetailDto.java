package com.alibaba.polardbx.batchweb.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 任务详情 DTO
 */
@Data
@Builder
public class JobDetailDto {

    private Long id;

    private String jobId;

    private Long groupId;

    private Long ruleId;

    private String jobName;

    private String operationType;

    private String sourceDatabase;

    private String sourceTable;

    private String targetDatabase;

    private String targetTable;

    private String commandArgs;

    private String status;

    private Long totalRows;

    private Long processedRows;

    private Integer progress;

    private BigDecimal speed;

    private String errorMsg;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long duration;

    /**
     * 并发执行数（规则组执行时使用）
     */
    private Integer concurrency;

    /**
     * 规则总数（规则组执行时使用）
     */
    private Integer totalRules;

    /**
     * 已完成规则数（规则组执行时使用）
     */
    private Integer completedRules;

    /**
     * 失败规则数（规则组执行时使用）
     */
    private Integer failedRules;

    private LocalDateTime createTime;

    private String createBy;
}
