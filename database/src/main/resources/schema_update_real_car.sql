-- 真实小车接入补充字段迁移脚本
-- 如果你已经创建过旧版 agv_config 表，不要删除数据库，直接执行本脚本即可。

ALTER TABLE agv_config ADD COLUMN cloud_api_key VARCHAR(255) NULL COMMENT '云端API密钥/Authorization';
ALTER TABLE agv_config ADD COLUMN db_host VARCHAR(64) NULL COMMENT '数据库地址';
ALTER TABLE agv_config ADD COLUMN db_port INT NULL COMMENT '数据库端口';
ALTER TABLE agv_config ADD COLUMN db_name VARCHAR(100) NULL COMMENT '数据库名称';
ALTER TABLE agv_config ADD COLUMN db_username VARCHAR(100) NULL COMMENT '数据库用户名';
ALTER TABLE agv_config ADD COLUMN db_password VARCHAR(255) NULL COMMENT '数据库密码';
ALTER TABLE agv_config ADD COLUMN control_protocol VARCHAR(20) NULL COMMENT '车辆控制协议：http/tcp/mock';

UPDATE agv_config
SET cloud_api_key = IFNULL(cloud_api_key, ''),
    db_host = IFNULL(db_host, 'localhost'),
    db_port = IFNULL(db_port, 3306),
    db_name = IFNULL(db_name, 'agv_inspection'),
    db_username = IFNULL(db_username, 'root'),
    db_password = IFNULL(db_password, '1234'),
    control_protocol = IFNULL(control_protocol, 'http')
WHERE delete_flag = 0;
