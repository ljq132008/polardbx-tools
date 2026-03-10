package com.alibaba.polardbx.batchweb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务执行详情实体
 */
@Data
@TableName("job_execution_detail")
public class JobExecutionDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String jobId;

    private String phase;

    private String phaseStatus;

    private String detailInfo;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long durationMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
