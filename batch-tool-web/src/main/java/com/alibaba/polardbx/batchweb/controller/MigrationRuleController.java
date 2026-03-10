package com.alibaba.polardbx.batchweb.controller;

import com.alibaba.polardbx.batchweb.dto.ApiResponse;
import com.alibaba.polardbx.batchweb.dto.MigrationRuleDto;
import com.alibaba.polardbx.batchweb.entity.MigrationRule;
import com.alibaba.polardbx.batchweb.service.JobManager;
import com.alibaba.polardbx.batchweb.service.MigrationRuleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 迁移规则管理 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/migration-rules")
public class MigrationRuleController {

    @Autowired
    private MigrationRuleService migrationRuleService;

    @Autowired
    private JobManager jobManager;

    /**
     * 获取规则列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MigrationRule>>> list(
            @RequestParam(required = false) Long groupId) {
        LambdaQueryWrapper<MigrationRule> wrapper = new LambdaQueryWrapper<>();
        if (groupId != null) {
            wrapper.eq(MigrationRule::getGroupId, groupId);
        }
        wrapper.orderByAsc(MigrationRule::getPriority);
        List<MigrationRule> rules = migrationRuleService.list(wrapper);
        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    /**
     * 获取规则详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MigrationRule>> getById(@PathVariable Long id) {
        MigrationRule rule = migrationRuleService.getById(id);
        if (rule == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(rule));
    }

    /**
     * 创建规则
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MigrationRule>> create(@Validated @RequestBody MigrationRuleDto dto) {
        try {
            MigrationRule rule = migrationRuleService.createRule(dto);
            return ResponseEntity.ok(ApiResponse.success("Migration rule created", rule));
        } catch (Exception e) {
            log.error("Failed to create migration rule", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 批量创建规则
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<MigrationRule>>> batchCreate(@Validated @RequestBody List<MigrationRuleDto> dtos) {
        try {
            List<MigrationRule> rules = new java.util.ArrayList<>();
            for (MigrationRuleDto dto : dtos) {
                rules.add(migrationRuleService.createRule(dto));
            }
            return ResponseEntity.ok(ApiResponse.success("Migration rules created: " + rules.size(), rules));
        } catch (Exception e) {
            log.error("Failed to batch create migration rules", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 更新规则
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MigrationRule>> update(@PathVariable Long id, @Validated @RequestBody MigrationRuleDto dto) {
        try {
            MigrationRule rule = migrationRuleService.updateRule(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Migration rule updated", rule));
        } catch (Exception e) {
            log.error("Failed to update migration rule", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 删除规则
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            migrationRuleService.removeById(id);
            return ResponseEntity.ok(ApiResponse.success("Migration rule deleted", null));
        } catch (Exception e) {
            log.error("Failed to delete migration rule", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 启用/禁用规则
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        try {
            MigrationRule rule = migrationRuleService.getById(id);
            if (rule == null) {
                return ResponseEntity.notFound().build();
            }
            rule.setStatus(status);
            migrationRuleService.updateById(rule);
            return ResponseEntity.ok(ApiResponse.success("Status updated", null));
        } catch (Exception e) {
            log.error("Failed to update status", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 执行单个规则
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<ApiResponse<String>> execute(@PathVariable Long id) {
        try {
            String jobId = jobManager.createJobFromRule(id);
            return ResponseEntity.ok(ApiResponse.success("Job created", jobId));
        } catch (Exception e) {
            log.error("Failed to execute migration rule", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 预览命令行参数
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<ApiResponse<String>> previewCommand(@PathVariable Long id) {
        try {
            MigrationRule rule = migrationRuleService.getById(id);
            if (rule == null) {
                return ResponseEntity.notFound().build();
            }
            var groupService = migrationRuleService.ruleGroupService;
            var group = groupService.getById(rule.getGroupId());
            if (group == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("Rule group not found"));
            }
            String commandArgs = migrationRuleService.buildCommandArgs(rule, group);
            return ResponseEntity.ok(ApiResponse.success(commandArgs));
        } catch (Exception e) {
            log.error("Failed to preview command", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }
}
