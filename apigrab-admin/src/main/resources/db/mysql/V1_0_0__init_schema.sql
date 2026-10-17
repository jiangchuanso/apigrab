CREATE TABLE `APIGRAB_AUTH_CONFIG` (
  `id`          BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(100) NOT NULL COMMENT '认证配置名称',
  `auth_type`   VARCHAR(20)  NOT NULL COMMENT '认证类型:BASIC/BEARER/OAUTH2/CUSTOM/PASSWORD_TOKEN',
  `auth_config` TEXT         COMMENT '认证内容 JSON',
  `status`      TINYINT(1)   DEFAULT 1 COMMENT '状态 0禁用 1启用',
  `remark`      VARCHAR(500),
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_config_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认证配置表(可被多个接口共享引用)';

CREATE TABLE `APIGRAB_API_CONFIG` (
  `id`                BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `name`              VARCHAR(100) NOT NULL COMMENT '接口名称',
  `url`               VARCHAR(500) NOT NULL COMMENT '接口URL',
  `method`            VARCHAR(10)  NOT NULL DEFAULT 'GET' COMMENT '请求方法 GET/POST/PUT/DELETE',
  `headers`           TEXT         COMMENT '请求头 JSON',
  `query_params`      TEXT         COMMENT '查询参数 JSON',
  `body_type`         VARCHAR(20)  DEFAULT 'NONE' COMMENT 'BODY类型:NONE/JSON/FORM/XML',
  `body_content`      TEXT         COMMENT '请求体内容',
  `connect_timeout`   INT(6)       DEFAULT 10 COMMENT '连接超时(秒)',
  `read_timeout`      INT(6)       DEFAULT 60 COMMENT '读取超时(秒)',
  `auth_config_id`    BIGINT(20)   NULL COMMENT '关联认证配置(空=无认证)',
  `resp_type`         VARCHAR(10)  DEFAULT 'JSON' COMMENT '响应类型 JSON/XML',
  `data_path`         VARCHAR(200) COMMENT '记录集合提取路径：JSON 用 JSONPath，XML 用 XPath；留空取整个响应',
  `page_enabled`      TINYINT(1)   DEFAULT 0 COMMENT '是否分页',
  `page_config`       TEXT         COMMENT '分页配置 JSON',
  `detail_config`     TEXT         COMMENT '详情补全配置 JSON',
  `transform_type`    VARCHAR(20)  DEFAULT 'FIELD_MAPPING' COMMENT '转换类型:FIELD_MAPPING/GROOVY',
  `convert_script`    TEXT         COMMENT '数据转换脚本(Groovy脚本)',
  `sample_data`       TEXT         NULL COMMENT '最近一次成功试跑的样例数据(单条记录 JSON)',
  `enable_multilines` TINYINT(1)   DEFAULT 0 COMMENT '是否按行解析响应(NDJSON)',
  `status`            TINYINT(1)   DEFAULT 1 COMMENT '状态 0禁用 1启用',
  `remark`            VARCHAR(500) COMMENT '备注',
  `create_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `update_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_config_name` (`name`),
  KEY `idx_api_auth_config` (`auth_config_id`),
  CONSTRAINT `fk_api_config_auth` FOREIGN KEY (`auth_config_id`) REFERENCES `APIGRAB_AUTH_CONFIG` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口配置表';

CREATE TABLE `APIGRAB_DATASOURCE_CONFIG` (
  `id`            BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `name`          VARCHAR(100) NOT NULL COMMENT '数据源名称',
  `db_type`       VARCHAR(30)  NOT NULL COMMENT '数据库类型 MYSQL/ORACLE/SQLSERVER/POSTGRESQL',
  `db_version`    VARCHAR(30)           COMMENT '数据库版本(驱动管理用)',
  `jdbc_url`      VARCHAR(500) NOT NULL COMMENT 'JDBC连接URL',
  `driver_class`  VARCHAR(200) NOT NULL COMMENT '驱动类名',
  `driver_path`   VARCHAR(300)          COMMENT '驱动JAR路径(支持热加载驱动)',
  `username`      VARCHAR(100) NOT NULL,
  `password`      VARCHAR(200) NOT NULL COMMENT 'AES加密存储',
  `pool_max_size` INT(5)       DEFAULT 10 COMMENT '连接池最大连接数',
  `pool_min_idle` INT(5)       DEFAULT 5  COMMENT '连接池最小空闲',
  `conn_props`    TEXT         COMMENT '连接属性JSON',
  `status`        TINYINT(1)   DEFAULT 1,
  `remark`        VARCHAR(500),
  `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_datasource_config_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源配置表';

CREATE TABLE `APIGRAB_FIELD_MAPPING` (
  `id`             BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `task_id`        BIGINT(20)   NOT NULL COMMENT '归属采集任务(结果集字段 -> 目标表字段)',
  `source_field`   VARCHAR(200) NOT NULL COMMENT '源字段 JSONPath表达式',
  `target_field`   VARCHAR(100) NOT NULL COMMENT '目标字段名',
  `target_type`    VARCHAR(30)  DEFAULT 'VARCHAR' COMMENT '目标字段类型',
  `target_length`  INT(11)      COMMENT '字段长度',
  `nullable`       TINYINT(1)   DEFAULT 1 COMMENT '是否可为空',
  `default_value`  VARCHAR(200) COMMENT '默认值',
  `convert_expr`   VARCHAR(500) COMMENT '转换表达式(空则直接映射)',
  `sort_order`     INT(5)       DEFAULT 0 COMMENT '排序',
  `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_mapping_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字段映射表(归属采集任务)';

CREATE TABLE `APIGRAB_COLLECT_TASK` (
  `id`                BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `name`              VARCHAR(100) NOT NULL COMMENT '任务名称',
  `api_config_id`     BIGINT(20)   NOT NULL COMMENT '关联接口配置(多任务可共享同一接口配置)',
  `datasource_id`     BIGINT(20)   NOT NULL COMMENT '目标数据源',
  `target_schema`     VARCHAR(100) NULL COMMENT '目标库/模式',
  `target_table`      VARCHAR(100) NOT NULL COMMENT '目标表名',
  `write_mode`        VARCHAR(20)  DEFAULT 'INSERT' COMMENT '写入模式:INSERT/MERGE/TRUNCATE_INSERT',
  `merge_keys`        VARCHAR(200) COMMENT 'MERGE模式主键字段(逗号分隔)',
  `auto_create_table` TINYINT(1)   DEFAULT 1 COMMENT '是否自动建表',
  `batch_size`        INT(5)       DEFAULT 500 COMMENT '批量入库条数',
  `retry_times`       INT          DEFAULT 0 COMMENT '任务失败重试次数',
  `timeout_ms`        BIGINT       DEFAULT 0 COMMENT '单次执行超时(ms,0=不限)',
  `alarm_enabled`     TINYINT(1)   DEFAULT 0 COMMENT '失败时是否触发告警',
  `schedule_type`     VARCHAR(20)  DEFAULT 'MANUAL' COMMENT '调度类型:MANUAL/ONCE/CRON',
  `cron_expression`   VARCHAR(100) COMMENT 'Cron表达式',
  `status`            TINYINT(1)   DEFAULT 0 COMMENT '状态 0未启动 1运行中 2暂停',
  `remark`            VARCHAR(500),
  `create_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `update_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collect_task_name` (`name`),
  KEY `idx_task_api_config` (`api_config_id`),
  KEY `idx_ds_id` (`datasource_id`),
  CONSTRAINT `fk_task_api_config` FOREIGN KEY (`api_config_id`) REFERENCES `APIGRAB_API_CONFIG` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集任务表(关联接口配置,可共享)';

-- 字段映射归属采集任务：因 APIGRAB_FIELD_MAPPING 先于 APIGRAB_COLLECT_TASK 建表，此处追加外键
ALTER TABLE `APIGRAB_FIELD_MAPPING`
  ADD CONSTRAINT `fk_mapping_task` FOREIGN KEY (`task_id`) REFERENCES `APIGRAB_COLLECT_TASK` (`id`);

CREATE TABLE `APIGRAB_EXECUTION_LOG` (
  `id`              BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `task_id`         BIGINT(20)   NOT NULL,
  `status`          VARCHAR(20)  DEFAULT 'RUNNING' COMMENT '状态 RUNNING/SUCCESS/FAILED/PARTIAL',
  `start_time`      DATETIME     NOT NULL,
  `end_time`        DATETIME,
  `total_records`   INT(11)      DEFAULT 0 COMMENT '采集总记录数',
  `success_count`   INT(11)      DEFAULT 0 COMMENT '成功入库条数',
  `fail_count`      INT(11)      DEFAULT 0 COMMENT '失败条数',
  `error_msg`       LONGTEXT               COMMENT '错误信息',
  `cost_ms`         BIGINT(20)   DEFAULT 0 COMMENT '耗时(毫秒)',
  `page_count`      INT(5)       DEFAULT 0 COMMENT '分页采集页数',
  `current_stage`   VARCHAR(20)  NULL COMMENT '当前阶段 READ/TRANSFORM/WRITE/DONE',
  `processed_count` INT          DEFAULT 0 COMMENT '当前阶段已处理条数',
  `updated_at`      DATETIME     NULL COMMENT '进度最后更新时间',
  `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行记录表';

CREATE TABLE `APIGRAB_EXECUTION_STAGE_LOG` (
  `id`              BIGINT(20)  NOT NULL AUTO_INCREMENT,
  `execution_id`    BIGINT(20)  NOT NULL COMMENT 'APIGRAB_EXECUTION_LOG.id',
  `stage`           VARCHAR(20) NOT NULL COMMENT 'READ/TRANSFORM/WRITE',
  `input_count`     INT         DEFAULT 0 COMMENT '阶段输入行数',
  `output_count`    INT         DEFAULT 0 COMMENT '阶段输出行数',
  `success_count`   INT         DEFAULT 0 COMMENT '阶段成功行数',
  `fail_count`      INT         DEFAULT 0 COMMENT '阶段失败行数',
  `cost_ms`         BIGINT(20)  DEFAULT 0 COMMENT '阶段耗时(ms)',
  `failure_samples` LONGTEXT    NULL COMMENT '限额失败样本 JSON 数组',
  `create_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_execution_id` (`execution_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行阶段明细表';

CREATE TABLE `APIGRAB_ALARM_CONFIG` (
  `id`             BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `status`         TINYINT(1)   DEFAULT 0 COMMENT '开关 0关闭 1开启',
  `endpoint`       VARCHAR(500) NOT NULL COMMENT '告警推送URL',
  `content_type`   VARCHAR(100) NOT NULL DEFAULT 'application/json' COMMENT '请求Content-Type',
  `input_template` TEXT         NOT NULL COMMENT '请求体模板(支持变量替换)',
  `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一告警配置表';

CREATE TABLE `APIGRAB_SYSTEM_USER` (
  `id`          BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
  `password`    VARCHAR(128) NOT NULL COMMENT 'BCrypt 密码哈希',
  `real_name`   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '真实姓名',
  `email`       VARCHAR(128) NOT NULL DEFAULT '' COMMENT '邮箱',
  `address`     VARCHAR(255) NOT NULL DEFAULT '' COMMENT '地址',
  `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色 ADMIN/USER',
  `status`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '状态 0禁用 1启用',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE `APIGRAB_USER_TOKEN` (
  `id`               BIGINT(20)  NOT NULL AUTO_INCREMENT,
  `token`            VARCHAR(64) NOT NULL COMMENT '访问token',
  `user_id`          BIGINT(20)  NOT NULL COMMENT 'APIGRAB_SYSTEM_USER.id',
  `username`         VARCHAR(64) NOT NULL COMMENT '用户名',
  `create_time`      DATETIME    DEFAULT CURRENT_TIMESTAMP,
  `expire_time`      DATETIME    NOT NULL COMMENT '过期时间',
  `last_access_time` DATETIME    NOT NULL COMMENT '最后访问时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户访问token表';
