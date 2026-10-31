drop table if exists `access_log`;
drop table  if exists `upload_task`;
drop table if exists `alert`;
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
CREATE TABLE `upload_task` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                               `file_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件名',
                               `total_rows` int DEFAULT '0' COMMENT '总行数',
                               `success_rows` int DEFAULT '0' COMMENT '成功入库数',
                               `fail_rows` int DEFAULT '0' COMMENT '失败数',
                               `fail_detail` text COLLATE utf8mb4_general_ci COMMENT '失败原因(JSON)',
                               `status` tinyint DEFAULT '0' COMMENT '0处理中,1完成,2失败',
                               `upload_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
                               `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
                               PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='上传任务表';
create table `access_log`
(
    id             bigint auto_increment comment '主键'
        primary key,
    ip             varchar(45)                           not null comment '访问IP',
    ip_location    varchar(100)                          null comment 'IP归属地',
    access_time    datetime                              not null comment '访问时间',
    url            varchar(500)                          null comment '请求URL',
    status_code    int                                   null comment 'HTTP状态码',
    method         varchar(10) default 'GET'             null comment '请求方法',
    response_time  int                                   null comment '响应耗时(ms)',
    user_agent     varchar(500)                          null comment 'User-Agent',
    dest_port      int                                   null comment '目标端口',
    protocol       varchar(20)                           null comment '协议(TCP/UDP/HTTP)',
    duration       bigint                                null comment '连接持续时间(微秒或毫秒)',
    src_bytes      bigint                                null comment '源端发送字节数',
    dst_bytes      bigint                                null comment '目标端发送字节数',
    rule_abnormal  tinyint     default 0                 null comment '规则判定:0正常,1异常',
    rule_type      varchar(100)                          null comment '命中规则类型',
    model_score    double                                null comment '模型异常分数',
    model_abnormal tinyint     default 0                 null comment '模型判定:0正常,1异常',
    final_abnormal tinyint     default 0                 null comment '最终判定:0正常,1异常',
    upload_task_id bigint                                null comment '所属上传任务',
    create_time    datetime    default CURRENT_TIMESTAMP null comment '创建时间'
)
    comment '访问日志表';

create index idx_abnormal
    on access_log (final_abnormal);

create index idx_ip
    on access_log (ip);

create index idx_task
    on access_log (upload_task_id);

create index idx_time
    on access_log (access_time);

