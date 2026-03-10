package com.alibaba.polardbx.batchweb.mapper;

import com.alibaba.polardbx.batchweb.entity.MigrationRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 迁移规则 Mapper
 */
@Mapper
public interface MigrationRuleMapper extends BaseMapper<MigrationRule> {
}
