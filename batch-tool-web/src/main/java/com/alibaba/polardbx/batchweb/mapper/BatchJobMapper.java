package com.alibaba.polardbx.batchweb.mapper;

import com.alibaba.polardbx.batchweb.entity.BatchJobEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 批量任务 Mapper
 */
@Mapper
public interface BatchJobMapper extends BaseMapper<BatchJobEntity> {
}
