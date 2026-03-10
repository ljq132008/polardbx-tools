package com.alibaba.polardbx.batchweb.mapper;

import com.alibaba.polardbx.batchweb.entity.JobExecutionDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务执行详情 Mapper
 */
@Mapper
public interface JobExecutionDetailMapper extends BaseMapper<JobExecutionDetail> {
}
