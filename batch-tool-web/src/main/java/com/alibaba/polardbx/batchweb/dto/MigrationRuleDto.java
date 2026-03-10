package com.alibaba.polardbx.batchweb.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 迁移规则 DTO
 */
@Data
public class MigrationRuleDto {

    private Long id;

    @NotNull(message = "规则组ID不能为空")
    private Long groupId;

    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    // 源库表配置
    @NotBlank(message = "源数据库名不能为空")
    private String sourceDatabase;

    @NotBlank(message = "源表名不能为空")
    private String sourceTable;

    // 目标库表配置
    private String targetDatabase;

    private String targetTable;

    // 操作配置
    @NotBlank(message = "操作类型不能为空")
    private String operationType;

    private String columns;

    private String whereCondition;

    private String separator = ",";

    // 文件配置
    private String fileDirectory;

    private String filePrefix;

    // 执行参数
    private Integer compress = 0;

    private Integer withDdl = 0;

    private Integer withHeader = 0;

    private Integer producerCount = 1;

    private Integer consumerCount = 1;

    private Integer batchSize = 200;

    private String extraParams;

    private Integer priority = 0;

    private Integer status = 1;
}
