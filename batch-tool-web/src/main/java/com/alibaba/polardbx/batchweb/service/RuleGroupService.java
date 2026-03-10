package com.alibaba.polardbx.batchweb.service;

import com.alibaba.polardbx.batchweb.dto.RuleGroupDto;
import com.alibaba.polardbx.batchweb.entity.RuleGroup;
import com.alibaba.polardbx.batchweb.mapper.RuleGroupMapper;
import com.alibaba.polardbx.batchweb.util.PasswordEncryptor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 规则组服务
 */
@Slf4j
@Service
public class RuleGroupService extends ServiceImpl<RuleGroupMapper, RuleGroup> {

    /**
     * 创建规则组
     */
    @Transactional(rollbackFor = Exception.class)
    public RuleGroup createRuleGroup(RuleGroupDto dto) {
        RuleGroup group = new RuleGroup();
        group.setName(dto.getName());
        group.setDescription(dto.getDescription());
        
        // 源数据库配置
        group.setSourceHost(dto.getSourceHost());
        group.setSourcePort(dto.getSourcePort());
        group.setSourceUser(dto.getSourceUser());
        group.setSourcePassword(PasswordEncryptor.encrypt(dto.getSourcePassword()));
        
        // 目标数据库配置
        group.setTargetHost(dto.getTargetHost());
        group.setTargetPort(dto.getTargetPort());
        group.setTargetUser(dto.getTargetUser());
        group.setTargetPassword(PasswordEncryptor.encrypt(dto.getTargetPassword()));
        
        group.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        group.setCreateBy(dto.getCreateBy());
        
        save(group);
        log.info("Created rule group: {}", group.getId());
        return group;
    }

    /**
     * 更新规则组
     */
    @Transactional(rollbackFor = Exception.class)
    public RuleGroup updateRuleGroup(Long id, RuleGroupDto dto) {
        RuleGroup group = getById(id);
        if (group == null) {
            throw new RuntimeException("Rule group not found: " + id);
        }
        
        group.setName(dto.getName());
        group.setDescription(dto.getDescription());
        
        // 源数据库配置
        group.setSourceHost(dto.getSourceHost());
        group.setSourcePort(dto.getSourcePort());
        group.setSourceUser(dto.getSourceUser());
        // 只有密码变化才加密
        if (!dto.getSourcePassword().equals(group.getSourcePassword())) {
            group.setSourcePassword(PasswordEncryptor.encrypt(dto.getSourcePassword()));
        }
        
        // 目标数据库配置
        group.setTargetHost(dto.getTargetHost());
        group.setTargetPort(dto.getTargetPort());
        group.setTargetUser(dto.getTargetUser());
        if (!dto.getTargetPassword().equals(group.getTargetPassword())) {
            group.setTargetPassword(PasswordEncryptor.encrypt(dto.getTargetPassword()));
        }
        
        if (dto.getStatus() != null) {
            group.setStatus(dto.getStatus());
        }
        
        updateById(group);
        log.info("Updated rule group: {}", id);
        return group;
    }

    /**
     * 获取所有启用的规则组
     */
    public List<RuleGroup> listEnabled() {
        return list(new LambdaQueryWrapper<RuleGroup>()
                .eq(RuleGroup::getStatus, 1)
                .orderByDesc(RuleGroup::getCreateTime));
    }

    /**
     * 获取解密后的规则组
     */
    public RuleGroup getDecryptedById(Long id) {
        RuleGroup group = getById(id);
        if (group != null) {
            group.setSourcePassword(PasswordEncryptor.decrypt(group.getSourcePassword()));
            group.setTargetPassword(PasswordEncryptor.decrypt(group.getTargetPassword()));
        }
        return group;
    }
}
