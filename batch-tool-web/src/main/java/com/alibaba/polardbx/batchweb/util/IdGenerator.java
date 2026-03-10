package com.alibaba.polardbx.batchweb.util;

import java.util.UUID;

/**
 * ID 生成器
 */
public class IdGenerator {

    public static String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String generateJobId() {
        return "job_" + generateId();
    }
}
