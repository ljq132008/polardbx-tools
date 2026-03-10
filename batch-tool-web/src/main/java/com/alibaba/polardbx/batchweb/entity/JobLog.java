package com.alibaba.polardbx.batchweb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务日志实体
 */
@Data
@TableName("job_log")
public class JobLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String jobId;

    private String logLevel;

    private String logMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
