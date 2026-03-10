package com.alibaba.polardbx.batchweb.controller;

import com.alibaba.polardbx.batchweb.dto.ApiResponse;
import com.alibaba.polardbx.batchweb.dto.RuleGroupDto;
import com.alibaba.polardbx.batchweb.entity.RuleGroup;
import com.alibaba.polardbx.batchweb.service.JobManager;
import com.alibaba.polardbx.batchweb.service.MigrationRuleService;
import com.alibaba.polardbx.batchweb.service.RuleGroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 规则组管理 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rule-groups")
public class RuleGroupController {

    @Autowired
    private RuleGroupService ruleGroupService;

    @Autowired
    private MigrationRuleService migrationRuleService;

    @Autowired
    private JobManager jobManager;

    /**
     * 获取规则组列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RuleGroup>>> list() {
        List<RuleGroup> groups = ruleGroupService.list();
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    /**
     * 获取启用的规则组列表
     */
    @GetMapping("/enabled")
    public ResponseEntity<ApiResponse<List<RuleGroup>>> listEnabled() {
        List<RuleGroup> groups = ruleGroupService.listEnabled();
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    /**
     * 获取规则组详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RuleGroup>> getById(@PathVariable Long id) {
        RuleGroup group = ruleGroupService.getById(id);
        if (group == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(group));
    }

    /**
     * 创建规则组
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RuleGroup>> create(@Validated @RequestBody RuleGroupDto dto) {
        try {
            RuleGroup group = ruleGroupService.createRuleGroup(dto);
            return ResponseEntity.ok(ApiResponse.success("Rule group created", group));
        } catch (Exception e) {
            log.error("Failed to create rule group", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 更新规则组
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RuleGroup>> update(@PathVariable Long id, @Validated @RequestBody RuleGroupDto dto) {
        try {
            RuleGroup group = ruleGroupService.updateRuleGroup(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Rule group updated", group));
        } catch (Exception e) {
            log.error("Failed to update rule group", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 删除规则组
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            ruleGroupService.removeById(id);
            return ResponseEntity.ok(ApiResponse.success("Rule group deleted", null));
        } catch (Exception e) {
            log.error("Failed to delete rule group", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 启用/禁用规则组
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        try {
            RuleGroup group = ruleGroupService.getById(id);
            if (group == null) {
                return ResponseEntity.notFound().build();
            }
            group.setStatus(status);
            ruleGroupService.updateById(group);
            return ResponseEntity.ok(ApiResponse.success("Status updated", null));
        } catch (Exception e) {
            log.error("Failed to update status", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取规则组下的所有规则
     */
    @GetMapping("/{id}/rules")
    public ResponseEntity<ApiResponse<?>> getRules(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(migrationRuleService.listByGroupId(id)));
    }

    /**
     * 执行规则组下所有规则（支持自定义并发度）
     * @param id 规则组ID
     * @param concurrency 并发执行数（可选，默认1，范围1-10）
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<ApiResponse<List<String>>> execute(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "1") Integer concurrency) {
        try {
            List<String> jobIds = jobManager.executeRuleGroup(id, concurrency);
            return ResponseEntity.ok(ApiResponse.success("Jobs created: " + jobIds.size(), jobIds));
        } catch (Exception e) {
            log.error("Failed to execute rule group", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }
}
