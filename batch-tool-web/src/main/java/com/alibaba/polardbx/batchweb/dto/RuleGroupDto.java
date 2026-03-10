package com.alibaba.polardbx.batchweb.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 规则组 DTO
 */
@Data
public class RuleGroupDto {

    private Long id;

    @NotBlank(message = "规则组名称不能为空")
    private String name;

    private String description;

    // 源数据库配置
    @NotBlank(message = "源数据库主机不能为空")
    private String sourceHost;

    @NotNull(message = "源数据库端口不能为空")
    private Integer sourcePort;

    @NotBlank(message = "源数据库用户名不能为空")
    private String sourceUser;

    @NotBlank(message = "源数据库密码不能为空")
    private String sourcePassword;

    // 目标数据库配置
    @NotBlank(message = "目标数据库主机不能为空")
    private String targetHost;

    @NotNull(message = "目标数据库端口不能为空")
    private Integer targetPort;

    @NotBlank(message = "目标数据库用户名不能为空")
    private String targetUser;

    @NotBlank(message = "目标数据库密码不能为空")
    private String targetPassword;

    private Integer status;

    private String createBy;
}
