package com.alibaba.polardbx.batchweb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务规则快照实体（保存执行时的规则副本）
 */
@Data
@TableName("job_rule_detail")
public class JobRuleDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String jobId;

    private Long ruleId;

    private String ruleName;

    // 源库表配置
    private String sourceDatabase;

    private String sourceTable;

    // 目标库表配置
    private String targetDatabase;

    private String targetTable;

    // 操作配置
    private String operationType;

    @TableField("`columns`")
    private String columns;

    private String whereCondition;

    @TableField("`separator`")
    private String separator;

    // 文件配置
    private String fileDirectory;

    private String filePrefix;

    // 执行参数
    private Integer compress;

    private Integer withDdl;

    private Integer withHeader;

    private Integer producerCount;

    private Integer consumerCount;

    private Integer batchSize;

    private String extraParams;

    private Integer priority;

    // 命令行参数快照
    private String commandArgs;

    // 执行状态
    private String status;

    private Long totalRows;

    private Long processedRows;

    private Integer progress;

    private String errorMsg;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long duration;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
