package com.alibaba.polardbx.batchweb.dto;

import lombok.Data;

/**
 * 任务查询 DTO
 */
@Data
public class JobQueryDto {

    private String jobId;

    private Long groupId;

    private Long ruleId;

    private String operationType;

    private String status;

    private String sourceDatabase;

    private String sourceTable;

    private String createBy;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
