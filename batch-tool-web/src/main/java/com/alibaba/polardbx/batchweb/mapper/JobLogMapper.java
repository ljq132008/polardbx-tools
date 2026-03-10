package com.alibaba.polardbx.batchweb.mapper;

import com.alibaba.polardbx.batchweb.entity.JobLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务日志 Mapper
 */
@Mapper
public interface JobLogMapper extends BaseMapper<JobLog> {
}
