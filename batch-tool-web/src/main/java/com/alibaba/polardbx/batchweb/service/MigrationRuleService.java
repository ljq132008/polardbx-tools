package com.alibaba.polardbx.batchweb.service;

import com.alibaba.polardbx.batchweb.dto.MigrationRuleDto;
import com.alibaba.polardbx.batchweb.entity.MigrationRule;
import com.alibaba.polardbx.batchweb.entity.RuleGroup;
import com.alibaba.polardbx.batchweb.mapper.MigrationRuleMapper;
import com.alibaba.polardbx.batchweb.util.PasswordEncryptor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 迁移规则服务
 */
@Slf4j
@Service
public class MigrationRuleService extends ServiceImpl<MigrationRuleMapper, MigrationRule> {

    @Autowired
    public RuleGroupService ruleGroupService;  // public for controller access

    /**
     * 创建规则
     */
    @Transactional(rollbackFor = Exception.class)
    public MigrationRule createRule(MigrationRuleDto dto) {
        // 检查规则组是否存在
        RuleGroup group = ruleGroupService.getById(dto.getGroupId());
        if (group == null) {
            throw new RuntimeException("Rule group not found: " + dto.getGroupId());
        }

        MigrationRule rule = new MigrationRule();
        rule.setGroupId(dto.getGroupId());
        rule.setRuleName(dto.getRuleName());
        rule.setSourceDatabase(dto.getSourceDatabase());
        rule.setSourceTable(dto.getSourceTable());
        rule.setTargetDatabase(dto.getTargetDatabase());
        rule.setTargetTable(dto.getTargetTable());
        rule.setOperationType(dto.getOperationType().toUpperCase());
        rule.setColumns(dto.getColumns());
        rule.setWhereCondition(dto.getWhereCondition());
        rule.setSeparator(dto.getSeparator() != null ? dto.getSeparator() : ",");
        rule.setFileDirectory(dto.getFileDirectory());
        rule.setFilePrefix(dto.getFilePrefix());
        rule.setCompress(dto.getCompress() != null ? dto.getCompress() : 0);
        rule.setWithDdl(dto.getWithDdl() != null ? dto.getWithDdl() : 0);
        rule.setWithHeader(dto.getWithHeader() != null ? dto.getWithHeader() : 0);
        rule.setProducerCount(dto.getProducerCount() != null ? dto.getProducerCount() : 1);
        rule.setConsumerCount(dto.getConsumerCount() != null ? dto.getConsumerCount() : 1);
        rule.setBatchSize(dto.getBatchSize() != null ? dto.getBatchSize() : 200);
        rule.setExtraParams(dto.getExtraParams());
        rule.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        rule.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);

        save(rule);
        log.info("Created migration rule: {}", rule.getId());
        return rule;
    }

    /**
     * 更新规则
     */
    @Transactional(rollbackFor = Exception.class)
    public MigrationRule updateRule(Long id, MigrationRuleDto dto) {
        MigrationRule rule = getById(id);
        if (rule == null) {
            throw new RuntimeException("Migration rule not found: " + id);
        }

        if (dto.getGroupId() != null) {
            RuleGroup group = ruleGroupService.getById(dto.getGroupId());
            if (group == null) {
                throw new RuntimeException("Rule group not found: " + dto.getGroupId());
            }
            rule.setGroupId(dto.getGroupId());
        }

        rule.setRuleName(dto.getRuleName());
        rule.setSourceDatabase(dto.getSourceDatabase());
        rule.setSourceTable(dto.getSourceTable());
        rule.setTargetDatabase(dto.getTargetDatabase());
        rule.setTargetTable(dto.getTargetTable());
        rule.setOperationType(dto.getOperationType().toUpperCase());
        rule.setColumns(dto.getColumns());
        rule.setWhereCondition(dto.getWhereCondition());
        rule.setSeparator(dto.getSeparator());
        rule.setFileDirectory(dto.getFileDirectory());
        rule.setFilePrefix(dto.getFilePrefix());
        rule.setCompress(dto.getCompress());
        rule.setWithDdl(dto.getWithDdl());
        rule.setWithHeader(dto.getWithHeader());
        rule.setProducerCount(dto.getProducerCount());
        rule.setConsumerCount(dto.getConsumerCount());
        rule.setBatchSize(dto.getBatchSize());
        rule.setExtraParams(dto.getExtraParams());
        rule.setPriority(dto.getPriority());
        if (dto.getStatus() != null) {
            rule.setStatus(dto.getStatus());
        }

        updateById(rule);
        log.info("Updated migration rule: {}", id);
        return rule;
    }

    /**
     * 获取规则组下的所有启用规则
     */
    public List<MigrationRule> listByGroupId(Long groupId) {
        return list(new LambdaQueryWrapper<MigrationRule>()
                .eq(MigrationRule::getGroupId, groupId)
                .eq(MigrationRule::getStatus, 1)
                .orderByAsc(MigrationRule::getPriority)
                .orderByAsc(MigrationRule::getId));
    }

    /**
     * 构建命令行参数
     */
    public String buildCommandArgs(MigrationRule rule, RuleGroup group) {
        StringBuilder sb = new StringBuilder();

        // 数据源配置（使用源数据库）
        sb.append("-h ").append(group.getSourceHost());
        sb.append(" -P ").append(group.getSourcePort());
        sb.append(" -u ").append(group.getSourceUser());
        sb.append(" -p ").append(PasswordEncryptor.decrypt(group.getSourcePassword()));
        sb.append(" -D ").append(rule.getSourceDatabase());

        // 操作类型
        sb.append(" -o ").append(rule.getOperationType().toLowerCase());

        // 表名
        sb.append(" -t ").append(rule.getSourceTable());

        // 可选参数
        if (rule.getColumns() != null && !rule.getColumns().isEmpty()) {
            sb.append(" -c ").append(rule.getColumns());
        }
        if (rule.getWhereCondition() != null && !rule.getWhereCondition().isEmpty()) {
            sb.append(" -w \"").append(rule.getWhereCondition()).append("\"");
        }
        if (rule.getFileDirectory() != null && !rule.getFileDirectory().isEmpty()) {
            sb.append(" -d ").append(rule.getFileDirectory());
        }
        if (rule.getFilePrefix() != null && !rule.getFilePrefix().isEmpty()) {
            sb.append(" --prefix ").append(rule.getFilePrefix());
        }
        if (rule.getCompress() != null && rule.getCompress() == 1) {
            sb.append(" --compress gzip");
        }
        if (rule.getWithDdl() != null && rule.getWithDdl() == 1) {
            sb.append(" --with-ddl");
        }
        if (rule.getWithHeader() != null && rule.getWithHeader() == 1) {
            sb.append(" --with-header");
        }
        if (rule.getProducerCount() != null && rule.getProducerCount() > 0) {
            sb.append(" --producer ").append(rule.getProducerCount());
        }
        if (rule.getConsumerCount() != null && rule.getConsumerCount() > 0) {
            sb.append(" --consumer ").append(rule.getConsumerCount());
        }
        if (rule.getBatchSize() != null && rule.getBatchSize() > 0) {
            sb.append(" --batch-size ").append(rule.getBatchSize());
        }
        if (rule.getSeparator() != null && !rule.getSeparator().equals(",")) {
            sb.append(" --sep ").append(rule.getSeparator());
        }

        return sb.toString();
    }
}
