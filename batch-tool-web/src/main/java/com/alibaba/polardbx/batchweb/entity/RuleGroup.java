package com.alibaba.polardbx.batchweb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 规则组实体
 */
@Data
@TableName("rule_group")
public class RuleGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    // 源数据库配置
    private String sourceHost;

    private Integer sourcePort;

    private String sourceUser;

    private String sourcePassword;

    // 目标数据库配置
    private String targetHost;

    private Integer targetPort;

    private String targetUser;

    private String targetPassword;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private String createBy;

    @TableLogic
    private Integer deleted;
}
