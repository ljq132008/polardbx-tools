package com.alibaba.polardbx.batchweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Batch Tool Web 应用启动类
 */
@SpringBootApplication
@EnableAsync
public class BatchToolWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchToolWebApplication.class, args);
    }
}
