package com.alibaba.polardbx.batchweb.controller;

import com.alibaba.polardbx.batchweb.dto.*;
import com.alibaba.polardbx.batchweb.service.BatchExecutor;
import com.alibaba.polardbx.batchweb.service.JobManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 批量操作 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchController {

    private final JobManager jobManager;

    private final BatchExecutor batchExecutor;

    /**
     * 执行批量操作（统一入口）
     * 
     * 示例请求:
     * {
     *   "commandArgs": "-h 127.0.0.1 -P 3306 -u root -p password -D test -o export -t my_table -d /data/export"
     * }
     */
    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<String>> execute(@Validated @RequestBody BatchRequest request) {
        try {
            log.info("Received batch request: {}", request.getCommandArgs());
            String jobId = jobManager.createBatchJob(request);
            return ResponseEntity.ok(ApiResponse.success("Batch job created", jobId));
        } catch (IllegalArgumentException e) {
            log.error("Invalid command arguments", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create batch job", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to create batch job: " + e.getMessage()));
        }
    }

    /**
     * 验证命令行参数
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<String>> validate(@RequestBody BatchRequest request) {
        try {
            batchExecutor.validateArgs(request.getCommandArgs());
            return ResponseEntity.ok(ApiResponse.success("Command arguments are valid", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }
}
