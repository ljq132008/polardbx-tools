package com.alibaba.polardbx.batchweb.service;

import cmd.BaseOperateCommand;
import cmd.CommandUtil;
import cmd.ConfigResult;
import datasource.DataSourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;

/**
 * 批量操作执行器 - 基于命令行参数
 * 
 * 注意：由于 BatchTool 类在默认包中，使用反射方式调用
 */
@Slf4j
@Service
public class BatchExecutor {

    private Object batchToolInstance;
    private Method initDatasourceMethod;
    private Method doBatchOpMethod;

    @Autowired
    private JobLogService jobLogService;

    public BatchExecutor() {
        try {
            // 通过反射获取 BatchTool 类（在默认包中）
            Class<?> batchToolClass = Class.forName("BatchTool");
            
            // 获取单例实例
            Method getInstanceMethod = batchToolClass.getMethod("getInstance");
            batchToolInstance = getInstanceMethod.invoke(null);
            
            // 获取需要调用的方法
            initDatasourceMethod = batchToolClass.getMethod("initDatasource", DataSourceConfig.class);
            doBatchOpMethod = batchToolClass.getMethod("doBatchOp", BaseOperateCommand.class, DataSourceConfig.class);
            
            log.info("BatchTool initialized via reflection");
        } catch (Exception e) {
            log.error("Failed to initialize BatchTool", e);
            throw new RuntimeException("Failed to initialize BatchTool", e);
        }
    }

    /**
     * 执行批量操作
     *
     * @param commandArgs 命令行参数字符串
     * @param jobId       任务ID
     */
    public void executeBatchOp(String commandArgs, String jobId) {
        log.info("Starting batch job: {}", jobId);
        log.info("Command args: {}", commandArgs);

        try {
            // 解析命令行参数
            String[] args = parseArgs(commandArgs);
            ConfigResult configResult = CommandUtil.parseStartUpCommand(args);

            if (configResult == null) {
                throw new RuntimeException("Failed to parse command arguments");
            }

            // 获取数据源配置
            DataSourceConfig dsConfig = CommandUtil.getDataSourceConfigFromCmd(configResult);
            log.info("DataSource: {}@{}/{}", dsConfig.getUsername(), dsConfig.getHost(), dsConfig.getDbName());
            jobLogService.info(jobId, "Connecting to database: " + dsConfig.getHost() + "/" + dsConfig.getDbName());

            // 获取操作命令
            BaseOperateCommand command = CommandUtil.getOperateCommandFromCmd(configResult);
            log.info("Operation on database: {}, tables: {}", dsConfig.getDbName(), command.getTableNames());
            jobLogService.info(jobId, "Operation: " + command.getClass().getSimpleName() + 
                    ", Database: " + dsConfig.getDbName() + 
                    ", Tables: " + (command.getTableNames() != null ? command.getTableNames() : "*"));

            // 执行批量操作（通过反射）
            jobLogService.info(jobId, "Starting batch operation...");
            initDatasourceMethod.invoke(batchToolInstance, dsConfig);
            doBatchOpMethod.invoke(batchToolInstance, command, dsConfig);

            log.info("Batch operation completed successfully");
            jobLogService.info(jobId, "Batch operation completed successfully");

        } catch (Exception e) {
            log.error("Batch operation failed", e);
            jobLogService.error(jobId, "Batch operation failed: " + e.getMessage());
            throw new RuntimeException("Batch operation failed: " + e.getMessage(), e);
        }
    }

    /**
     * 解析命令行参数字符串为数组
     * 支持两种格式:
     * 1. 空格分隔: -h 127.0.0.1 -P 3306
     * 2. 引号包裹: "-h 127.0.0.1 -P 3306"
     */
    public String[] parseArgs(String commandArgs) {
        if (commandArgs == null || commandArgs.trim().isEmpty()) {
            throw new IllegalArgumentException("Command arguments cannot be empty");
        }

        // 使用正则表达式分割，支持引号内的空格
        java.util.List<String> args = new java.util.ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        java.util.regex.Matcher matcher = pattern.matcher(commandArgs.trim());

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                // 双引号内的内容
                args.add(matcher.group(1));
            } else if (matcher.group(2) != null) {
                // 单引号内的内容
                args.add(matcher.group(2));
            } else {
                // 非引号内容
                args.add(matcher.group());
            }
        }

        return args.toArray(new String[0]);
    }

    /**
     * 验证命令行参数
     */
    public void validateArgs(String commandArgs) {
        String[] args = parseArgs(commandArgs);
        ConfigResult configResult = CommandUtil.parseStartUpCommand(args);
        if (configResult == null) {
            throw new IllegalArgumentException("Invalid command arguments");
        }
        // 触发验证
        CommandUtil.getDataSourceConfigFromCmd(configResult);
        CommandUtil.getOperateCommandFromCmd(configResult);
    }
}
