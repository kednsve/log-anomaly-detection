-- =============================================
-- 基于访问日志的异常检测与可视化分析系统
-- 数据库初始化脚本
-- =============================================

CREATE DATABASE IF NOT EXISTS log_anomaly
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE log_anomaly;
-- 删除旧表（注意顺序，先删子表/关联表）
DROP TABLE IF EXISTS `alert`;
DROP TABLE IF EXISTS `access_log`;
DROP TABLE IF EXISTS `upload_task`;
DROP TABLE IF EXISTS `user`;

-- =============================================
-- 1. 用户表
-- =============================================
CREATE TABLE `user` (
                        `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                        `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
                        `password`    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
                        `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
                        `role`        VARCHAR(20)  DEFAULT 'admin' COMMENT '角色',
                        `status`      TINYINT      DEFAULT 1 COMMENT '1启用,0禁用',
			`create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- =============================================
-- 2. 上传任务表
-- =============================================
CREATE TABLE `upload_task` (
                               `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                               `file_name`    VARCHAR(255) NOT NULL COMMENT '文件名',
                               `total_rows`   INT          DEFAULT 0 COMMENT '总行数',
                               `success_rows` INT          DEFAULT 0 COMMENT '成功入库数',
                               `fail_rows`    INT          DEFAULT 0 COMMENT '失败数',
                               `fail_detail`  TEXT         DEFAULT NULL COMMENT '失败原因(JSON)',
                               `status`       TINYINT      DEFAULT 0 COMMENT '0处理中,1完成,2失败',
                               `upload_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
                               `finish_time`  DATETIME     DEFAULT NULL COMMENT '完成时间',
                               PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='上传任务表';

-- =============================================
-- 3. 访问日志表（核心表，新增网络流特征字段）
-- =============================================
CREATE TABLE `access_log` (
                              `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',

    -- 基础信息
                              `ip`             VARCHAR(45)  NOT NULL COMMENT '访问IP',
                              `ip_location`    VARCHAR(100) DEFAULT NULL COMMENT 'IP归属地',
                              `access_time`    DATETIME     NOT NULL COMMENT '访问时间',
                              `url`            VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
                              `status_code`    INT          DEFAULT NULL COMMENT 'HTTP状态码',
                              `method`         VARCHAR(10)  DEFAULT 'GET' COMMENT '请求方法',
                              `response_time`  INT          DEFAULT NULL COMMENT '响应耗时(ms)',
                              `user_agent`     VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',

    -- 新增：网络流特征（兼容公开数据集与高级检测模型）
                              `dest_port`      INT          DEFAULT NULL COMMENT '目标端口',
                              `protocol`       VARCHAR(20)  DEFAULT NULL COMMENT '协议(TCP/UDP/HTTP)',
                              `duration`       BIGINT       DEFAULT NULL COMMENT '连接持续时间(微秒或毫秒)',
                              `src_bytes`      BIGINT       DEFAULT NULL COMMENT '源端发送字节数',
                              `dst_bytes`      BIGINT       DEFAULT NULL COMMENT '目标端发送字节数',

    -- 异常检测标记（三层）
                              `rule_abnormal`  TINYINT      DEFAULT 0 COMMENT '规则判定:0正常,1异常',
                              `rule_type`      VARCHAR(100) DEFAULT NULL COMMENT '命中规则类型',
                              `model_score`    DOUBLE       DEFAULT NULL COMMENT '模型异常分数',
                              `model_abnormal` TINYINT      DEFAULT 0 COMMENT '模型判定:0正常,1异常',
                              `final_abnormal` TINYINT      DEFAULT 0 COMMENT '最终判定:0正常,1异常',

    -- 关联信息
                              `upload_task_id` BIGINT       DEFAULT NULL COMMENT '所属上传任务',
                              `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

                              PRIMARY KEY (`id`),
                              KEY `idx_ip` (`ip`),
                              KEY `idx_time` (`access_time`),
                              KEY `idx_abnormal` (`final_abnormal`),
                              KEY `idx_task` (`upload_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='访问日志表';

-- =============================================
-- 4. 异常告警表
-- =============================================
CREATE TABLE `alert` (
                         `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
			 `task_id` BIGINT COMMENT '关联的上传任务ID',
                         `ip`            VARCHAR(45)  NOT NULL COMMENT '异常IP',
                         `alert_type`    VARCHAR(50)  NOT NULL COMMENT '告警类型',
                         `alert_source`  VARCHAR(20)  NOT NULL COMMENT '来源:rule/model/both',
                         `description`   VARCHAR(500) DEFAULT NULL COMMENT '告警描述',
                         `trigger_time`  DATETIME     NOT NULL COMMENT '触发时间',
                         `status`        TINYINT      DEFAULT 0 COMMENT '0未处理,1已处理,2误报',
                         `handle_time`   DATETIME     DEFAULT NULL COMMENT '处理时间',
                         `handle_remark` VARCHAR(255) DEFAULT NULL COMMENT '处理备注',
                         PRIMARY KEY (`id`),
                         KEY `idx_ip` (`ip`),
                         KEY `idx_status` (`status`),
                         KEY `idx_time` (`trigger_time`),
			 KEY `idx_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='异常告警表';

-- =============================================
-- 初始化管理员账号
-- 密码: 123456  (BCrypt加密后的值)
-- 注意: 下面这个密文对应的是 "123456"，可直接用
-- =============================================
INSERT INTO `user` (`username`, `password`, `nickname`, `role`)
VALUES ('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '管理员', 'admin');