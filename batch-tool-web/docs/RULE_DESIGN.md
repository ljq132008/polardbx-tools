# 批量迁移规则组管理系统设计文档

## 1. 系统概述

### 1.1 功能目标
在 batch-tool-web 基础上扩展规则组管理能力，支持用户：
- 定义规则组（Rule Group）
- 在规则组下定义多个库表迁移规则（Migration Rule）
- 根据规则生成批量迁移任务（Job）
- 记录任务执行详情和日志

### 1.2 技术栈
- Spring Boot 2.7.x
- MyBatis-Plus 3.5.x
- MySQL 8.0
- H2 Database（开发/测试环境可选）

---

## 2. 数据库设计

### 2.1 规则组表 `rule_group`

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 |
| name | VARCHAR(100) | 规则组名称 |
| description | VARCHAR(500) | 描述 |
| source_host | VARCHAR(100) | 源数据库主机 |
| source_port | INT | 源数据库端口 |
| source_user | VARCHAR(50) | 源数据库用户名 |
| source_password | VARCHAR(200) | 源数据库密码（加密存储） |
| target_host | VARCHAR(100) | 目标数据库主机 |
| target_port | INT | 目标数据库端口 |
| target_user | VARCHAR(50) | 目标数据库用户名 |
| target_password | VARCHAR(200) | 目标数据库密码（加密存储） |
| status | TINYINT | 状态：0-禁用，1-启用 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| create_by | VARCHAR(50) | 创建人 |
| deleted | TINYINT | 逻辑删除：0-未删除，1-已删除 |

```sql
CREATE TABLE `rule_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(100) NOT NULL COMMENT '规则组名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `source_host` VARCHAR(100) NOT NULL COMMENT '源数据库主机',
    `source_port` INT NOT NULL DEFAULT 3306 COMMENT '源数据库端口',
    `source_user` VARCHAR(50) NOT NULL COMMENT '源数据库用户名',
    `source_password` VARCHAR(200) NOT NULL COMMENT '源数据库密码',
    `target_host` VARCHAR(100) NOT NULL COMMENT '目标数据库主机',
    `target_port` INT NOT NULL DEFAULT 3306 COMMENT '目标数据库端口',
    `target_user` VARCHAR(50) NOT NULL COMMENT '目标数据库用户名',
    `target_password` VARCHAR(200) NOT NULL COMMENT '目标数据库密码',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则组表';
```

### 2.2 迁移规则表 `migration_rule`

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 |
| group_id | BIGINT | 规则组ID |
| rule_name | VARCHAR(100) | 规则名称 |
| source_database | VARCHAR(100) | 源数据库名 |
| source_table | VARCHAR(100) | 源表名（支持通配符） |
| target_database | VARCHAR(100) | 目标数据库名 |
| target_table | VARCHAR(100) | 目标表名（为空则与源表同名） |
| operation_type | VARCHAR(20) | 操作类型：EXPORT/IMPORT/UPDATE/DELETE |
| columns | VARCHAR(1000) | 指定列（逗号分隔，为空则全部） |
| where_condition | VARCHAR(500) | WHERE 条件 |
| separator | VARCHAR(10) | 字段分隔符 |
| file_directory | VARCHAR(200) | 文件目录 |
| file_prefix | VARCHAR(100) | 文件前缀 |
| compress | TINYINT | 是否压缩：0-否，1-是 |
| with_ddl | TINYINT | 是否导出DDL：0-否，1-是 |
| with_header | TINYINT | 是否包含表头：0-否，1-是 |
| producer_count | INT | 生产者并发数 |
| consumer_count | INT | 消费者并发数 |
| batch_size | INT | 批处理大小 |
| extra_params | TEXT | 额外参数（JSON格式） |
| priority | INT | 优先级（数值越小越优先） |
| status | TINYINT | 状态：0-禁用，1-启用 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | TINYINT | 逻辑删除 |

```sql
CREATE TABLE `migration_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_id` BIGINT NOT NULL COMMENT '规则组ID',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `source_database` VARCHAR(100) NOT NULL COMMENT '源数据库名',
    `source_table` VARCHAR(100) NOT NULL COMMENT '源表名',
    `target_database` VARCHAR(100) DEFAULT NULL COMMENT '目标数据库名',
    `target_table` VARCHAR(100) DEFAULT NULL COMMENT '目标表名',
    `operation_type` VARCHAR(20) NOT NULL COMMENT '操作类型',
    `columns` VARCHAR(1000) DEFAULT NULL COMMENT '指定列',
    `where_condition` VARCHAR(500) DEFAULT NULL COMMENT 'WHERE条件',
    `separator` VARCHAR(10) DEFAULT ',' COMMENT '字段分隔符',
    `file_directory` VARCHAR(200) DEFAULT NULL COMMENT '文件目录',
    `file_prefix` VARCHAR(100) DEFAULT NULL COMMENT '文件前缀',
    `compress` TINYINT NOT NULL DEFAULT 0 COMMENT '是否压缩',
    `with_ddl` TINYINT NOT NULL DEFAULT 0 COMMENT '是否导出DDL',
    `with_header` TINYINT NOT NULL DEFAULT 0 COMMENT '是否包含表头',
    `producer_count` INT NOT NULL DEFAULT 1 COMMENT '生产者并发数',
    `consumer_count` INT NOT NULL DEFAULT 1 COMMENT '消费者并发数',
    `batch_size` INT NOT NULL DEFAULT 200 COMMENT '批处理大小',
    `extra_params` TEXT DEFAULT NULL COMMENT '额外参数JSON',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_rule_group` FOREIGN KEY (`group_id`) REFERENCES `rule_group` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='迁移规则表';
```

### 2.3 任务表 `batch_job`

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 |
| job_id | VARCHAR(50) | 任务唯一标识 |
| group_id | BIGINT | 规则组ID |
| rule_id | BIGINT | 规则ID（可为空，手动执行时无规则） |
| job_name | VARCHAR(100) | 任务名称 |
| operation_type | VARCHAR(20) | 操作类型 |
| source_database | VARCHAR(100) | 源数据库 |
| source_table | VARCHAR(100) | 源表 |
| target_database | VARCHAR(100) | 目标数据库 |
| target_table | VARCHAR(100) | 目标表 |
| command_args | TEXT | 命令行参数 |
| status | VARCHAR(20) | 状态：PENDING/RUNNING/COMPLETED/FAILED/CANCELLED |
| total_rows | BIGINT | 总行数 |
| processed_rows | BIGINT | 已处理行数 |
| progress | INT | 进度百分比 |
| speed | DECIMAL(10,2) | 处理速度（行/秒） |
| error_msg | TEXT | 错误信息 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| duration | BIGINT | 执行时长（秒） |
| create_time | DATETIME | 创建时间 |
| create_by | VARCHAR(50) | 创建人 |

```sql
CREATE TABLE `batch_job` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `job_id` VARCHAR(50) NOT NULL COMMENT '任务唯一标识',
    `group_id` BIGINT DEFAULT NULL COMMENT '规则组ID',
    `rule_id` BIGINT DEFAULT NULL COMMENT '规则ID',
    `job_name` VARCHAR(100) DEFAULT NULL COMMENT '任务名称',
    `operation_type` VARCHAR(20) NOT NULL COMMENT '操作类型',
    `source_database` VARCHAR(100) DEFAULT NULL COMMENT '源数据库',
    `source_table` VARCHAR(100) DEFAULT NULL COMMENT '源表',
    `target_database` VARCHAR(100) DEFAULT NULL COMMENT '目标数据库',
    `target_table` VARCHAR(100) DEFAULT NULL COMMENT '目标表',
    `command_args` TEXT DEFAULT NULL COMMENT '命令行参数',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    `total_rows` BIGINT NOT NULL DEFAULT 0 COMMENT '总行数',
    `processed_rows` BIGINT NOT NULL DEFAULT 0 COMMENT '已处理行数',
    `progress` INT NOT NULL DEFAULT 0 COMMENT '进度百分比',
    `speed` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '处理速度',
    `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `duration` BIGINT NOT NULL DEFAULT 0 COMMENT '执行时长秒',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_job_id` (`job_id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_rule_id` (`rule_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量任务表';
```

### 2.4 任务执行详情表 `job_execution_detail`

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| job_id | VARCHAR(50) | 任务ID |
| phase | VARCHAR(50) | 执行阶段 |
| phase_status | VARCHAR(20) | 阶段状态 |
| detail_info | TEXT | 详情信息 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| duration_ms | BIGINT | 耗时（毫秒） |
| create_time | DATETIME | 创建时间 |

```sql
CREATE TABLE `job_execution_detail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `job_id` VARCHAR(50) NOT NULL COMMENT '任务ID',
    `phase` VARCHAR(50) NOT NULL COMMENT '执行阶段',
    `phase_status` VARCHAR(20) NOT NULL COMMENT '阶段状态',
    `detail_info` TEXT DEFAULT NULL COMMENT '详情信息',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `duration_ms` BIGINT NOT NULL DEFAULT 0 COMMENT '耗时毫秒',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行详情表';
```

### 2.5 任务日志表 `job_log`

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| job_id | VARCHAR(50) | 任务ID |
| log_level | VARCHAR(10) | 日志级别 |
| log_message | TEXT | 日志内容 |
| create_time | DATETIME | 创建时间 |

```sql
CREATE TABLE `job_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `job_id` VARCHAR(50) NOT NULL COMMENT '任务ID',
    `log_level` VARCHAR(10) NOT NULL COMMENT '日志级别',
    `log_message` TEXT NOT NULL COMMENT '日志内容',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务日志表';
```

---

## 3. 项目结构

```
batch-tool-web/
├── src/main/java/com/alibaba/polardbx/batchweb/
│   ├── config/                          # 配置类
│   │   └── MybatisPlusConfig.java       # MyBatis-Plus配置
│   │
│   ├── controller/                      # 控制器层
│   │   ├── BatchController.java         # 批量操作接口
│   │   ├── RuleGroupController.java     # 规则组管理接口
│   │   ├── MigrationRuleController.java # 迁移规则管理接口
│   │   └── JobController.java           # 任务管理接口
│   │
│   ├── dto/                             # 数据传输对象
│   │   ├── ApiResponse.java
│   │   ├── BatchRequest.java
│   │   ├── RuleGroupDto.java
│   │   ├── MigrationRuleDto.java
│   │   ├── JobQueryDto.java
│   │   └── JobExecuteDto.java
│   │
│   ├── entity/                          # 实体类
│   │   ├── RuleGroup.java
│   │   ├── MigrationRule.java
│   │   ├── BatchJob.java
│   │   ├── JobExecutionDetail.java
│   │   └── JobLog.java
│   │
│   ├── mapper/                          # MyBatis Mapper
│   │   ├── RuleGroupMapper.java
│   │   ├── MigrationRuleMapper.java
│   │   ├── BatchJobMapper.java
│   │   ├── JobExecutionDetailMapper.java
│   │   └── JobLogMapper.java
│   │
│   ├── model/                           # 业务模型
│   │   ├── JobStatus.java
│   │   └── JobTask.java
│   │
│   ├── service/                         # 服务层
│   │   ├── BatchExecutor.java
│   │   ├── JobManager.java
│   │   ├── WebSocketService.java
│   │   ├── RuleGroupService.java
│   │   ├── MigrationRuleService.java
│   │   ├── BatchJobService.java
│   │   └── JobLogService.java
│   │
│   └── util/                            # 工具类
│       ├── IdGenerator.java
│       └── PasswordEncryptor.java       # 密码加密工具
│
├── src/main/resources/
│   ├── application.yml                  # 应用配置
│   ├── application-dev.yml              # 开发环境配置
│   ├── application-prod.yml             # 生产环境配置
│   └── db/
│       └── schema.sql                   # 数据库初始化脚本
│
└── pom.xml
```

---

## 4. 实体类设计

### 4.1 RuleGroup

```java
@Data
@TableName("rule_group")
public class RuleGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    private String description;
    
    // 源数据库配置
    private String sourceHost;
    private Integer sourcePort;
    private String sourceUser;
    private String sourcePassword;
    
    // 目标数据库配置
    private String targetHost;
    private Integer targetPort;
    private String targetUser;
    private String targetPassword;
    
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    private String createBy;
    
    @TableLogic
    private Integer deleted;
}
```

### 4.2 MigrationRule

```java
@Data
@TableName("migration_rule")
public class MigrationRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long groupId;
    private String ruleName;
    
    // 源库表配置
    private String sourceDatabase;
    private String sourceTable;
    
    // 目标库表配置
    private String targetDatabase;
    private String targetTable;
    
    // 操作配置
    private String operationType;  // EXPORT, IMPORT, UPDATE, DELETE
    private String columns;
    private String whereCondition;
    private String separator;
    
    // 文件配置
    private String fileDirectory;
    private String filePrefix;
    
    // 执行参数
    private Integer compress;
    private Integer withDdl;
    private Integer withHeader;
    private Integer producerCount;
    private Integer consumerCount;
    private Integer batchSize;
    
    private String extraParams;  // JSON格式
    private Integer priority;
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
```

### 4.3 BatchJob

```java
@Data
@TableName("batch_job")
public class BatchJob {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String jobId;
    private Long groupId;
    private Long ruleId;
    private String jobName;
    
    private String operationType;
    private String sourceDatabase;
    private String sourceTable;
    private String targetDatabase;
    private String targetTable;
    
    private String commandArgs;
    
    private String status;
    private Long totalRows;
    private Long processedRows;
    private Integer progress;
    private BigDecimal speed;
    private String errorMsg;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long duration;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    private String createBy;
}
```

### 4.4 JobExecutionDetail

```java
@Data
@TableName("job_execution_detail")
public class JobExecutionDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String jobId;
    private String phase;
    private String phaseStatus;
    private String detailInfo;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

### 4.5 JobLog

```java
@Data
@TableName("job_log")
public class JobLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String jobId;
    private String logLevel;  // INFO, WARN, ERROR, DEBUG
    private String logMessage;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

---

## 5. API 接口设计

### 5.1 规则组管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/v1/rule-groups` | 获取规则组列表 |
| GET | `/v1/rule-groups/{id}` | 获取规则组详情 |
| POST | `/v1/rule-groups` | 创建规则组 |
| PUT | `/v1/rule-groups/{id}` | 更新规则组 |
| DELETE | `/v1/rule-groups/{id}` | 删除规则组 |
| POST | `/v1/rule-groups/{id}/execute` | 执行规则组下所有规则 |
| GET | `/v1/rule-groups/{id}/rules` | 获取规则组下的所有规则 |

### 5.2 迁移规则管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/v1/migration-rules` | 获取规则列表 |
| GET | `/v1/migration-rules/{id}` | 获取规则详情 |
| POST | `/v1/migration-rules` | 创建规则 |
| PUT | `/v1/migration-rules/{id}` | 更新规则 |
| DELETE | `/v1/migration-rules/{id}` | 删除规则 |
| POST | `/v1/migration-rules/{id}/execute` | 执行单个规则 |
| POST | `/v1/migration-rules/batch` | 批量创建规则 |

### 5.3 任务管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/v1/jobs` | 获取任务列表（支持分页、筛选） |
| GET | `/v1/jobs/{jobId}` | 获取任务详情 |
| GET | `/v1/jobs/{jobId}/progress` | 获取任务进度 |
| GET | `/v1/jobs/{jobId}/logs` | 获取任务日志 |
| GET | `/v1/jobs/{jobId}/details` | 获取任务执行详情 |
| POST | `/v1/jobs/{jobId}/cancel` | 取消任务 |
| POST | `/v1/jobs/{jobId}/retry` | 重试任务 |
| DELETE | `/v1/jobs/{jobId}` | 删除任务 |

---

## 6. 核心业务流程

### 6.1 规则组执行流程

```
┌─────────────────────────────────────────────────────────────────┐
│                    规则组执行流程                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 选择规则组                                                   │
│     │                                                           │
│     ▼                                                           │
│  2. 加载规则组下所有启用的规则（按 priority 排序）                  │
│     │                                                           │
│     ▼                                                           │
│  3. 对每条规则:                                                  │
│     ├─► 构建命令行参数                                           │
│     ├─► 创建 Job 记录                                            │
│     ├─► 提交到任务队列                                           │
│     └─► 记录日志                                                 │
│     │                                                           │
│     ▼                                                           │
│  4. 异步执行任务                                                 │
│     ├─► 更新 Job 状态                                            │
│     ├─► 记录执行详情                                             │
│     ├─► 记录日志                                                 │
│     └─► WebSocket 推送进度                                       │
│     │                                                           │
│     ▼                                                           │
│  5. 任务完成/失败                                                │
│     └─► 更新最终状态、统计信息                                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 命令行参数构建规则

```java
public String buildCommandArgs(MigrationRule rule, RuleGroup group) {
    StringBuilder sb = new StringBuilder();
    
    // 数据源配置
    sb.append("-h ").append(group.getSourceHost());
    sb.append(" -P ").append(group.getSourcePort());
    sb.append(" -u ").append(group.getSourceUser());
    sb.append(" -p ").append(decrypt(group.getSourcePassword()));
    sb.append(" -D ").append(rule.getSourceDatabase());
    
    // 操作类型
    sb.append(" -o ").append(rule.getOperationType().toLowerCase());
    
    // 表名
    sb.append(" -t ").append(rule.getSourceTable());
    
    // 可选参数
    if (rule.getColumns() != null) {
        sb.append(" -c ").append(rule.getColumns());
    }
    if (rule.getWhereCondition() != null) {
        sb.append(" -w \"").append(rule.getWhereCondition()).append("\"");
    }
    if (rule.getFileDirectory() != null) {
        sb.append(" -d ").append(rule.getFileDirectory());
    }
    if (rule.getFilePrefix() != null) {
        sb.append(" --prefix ").append(rule.getFilePrefix());
    }
    if (rule.getCompress() == 1) {
        sb.append(" --compress gzip");
    }
    if (rule.getWithDdl() == 1) {
        sb.append(" --with-ddl");
    }
    if (rule.getWithHeader() == 1) {
        sb.append(" --with-header");
    }
    if (rule.getProducerCount() != null) {
        sb.append(" --producer ").append(rule.getProducerCount());
    }
    if (rule.getConsumerCount() != null) {
        sb.append(" --consumer ").append(rule.getConsumerCount());
    }
    if (rule.getBatchSize() != null) {
        sb.append(" --batch-size ").append(rule.getBatchSize());
    }
    
    return sb.toString();
}
```

---

## 7. 配置文件

### 7.1 application.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/batch_tool_web?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
    
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.alibaba.polardbx.batchweb.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

# 密码加密密钥
encrypt:
  secret-key: your-secret-key-here
```

### 7.2 pom.xml 新增依赖

```xml
<!-- MyBatis-Plus -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.3.1</version>
</dependency>

<!-- MySQL Connector -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>

<!-- H2 Database (可选，用于测试) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 8. 开发计划

### Phase 1: 基础框架搭建
- [ ] 创建数据库表
- [ ] 创建实体类
- [ ] 创建 Mapper 接口
- [ ] 配置 MyBatis-Plus

### Phase 2: 规则组管理
- [ ] 实现 RuleGroupService
- [ ] 实现 RuleGroupController
- [ ] 单元测试

### Phase 3: 迁移规则管理
- [ ] 实现 MigrationRuleService
- [ ] 实现 MigrationRuleController
- [ ] 单元测试

### Phase 4: 任务管理重构
- [ ] 重构 JobManager 使用数据库
- [ ] 实现 BatchJobService
- [ ] 实现 JobLogService
- [ ] 集成测试

### Phase 5: 执行流程
- [ ] 规则组批量执行
- [ ] 单规则执行
- [ ] 执行详情记录
- [ ] 日志记录

### Phase 6: 前端对接
- [ ] API 文档
- [ ] 接口联调

---

## 9. 注意事项

1. **密码安全**：数据库密码需加密存储，使用 AES 加密
2. **并发控制**：限制最大并发任务数，避免资源耗尽
3. **日志清理**：定期清理过期日志，避免表过大
4. **事务管理**：规则组执行需考虑事务边界
5. **异常处理**：完善的异常捕获和日志记录
