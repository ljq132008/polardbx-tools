package com.alibaba.polardbx.batchweb.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * 任务进度
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobProgress {
    private String jobId;
    private int progress;
    private long totalRows;
    private long processedRows;
    private double speed; // rows/sec
    private String status;
    private String message;
}
