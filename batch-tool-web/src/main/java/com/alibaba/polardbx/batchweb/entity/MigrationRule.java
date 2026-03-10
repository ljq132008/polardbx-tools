package com.alibaba.polardbx.batchweb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 迁移规则实体
 */
@Data
@TableName("migration_rule")
public class MigrationRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

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

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
