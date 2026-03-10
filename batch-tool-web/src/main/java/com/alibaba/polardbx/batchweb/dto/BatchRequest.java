package com.alibaba.polardbx.batchweb.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 批量操作请求 - 使用命令行参数方式
 */
@Data
public class BatchRequest {
    /**
     * 命令行参数，如:
     * -h 127.0.0.1 -P 3306 -u root -p password -D test -o export -t my_table -d /data/export
     */
    @NotBlank(message = "命令行参数不能为空")
    private String commandArgs;
}
