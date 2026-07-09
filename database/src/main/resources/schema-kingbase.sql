-- AGV 智能巡检系统 KingbaseES 建表脚本
-- 使用前请先在金仓中创建数据库：agv_inspection
-- 本脚本与 MySQL 版 schema.sql 表结构保持一致，仅将 MySQL 方言改成金仓可执行写法。

CREATE TABLE IF NOT EXISTS agv_task (
    id BIGSERIAL PRIMARY KEY,
    task_code VARCHAR(64) NOT NULL,
    task_name VARCHAR(100) NOT NULL,
    start_pos VARCHAR(100),
    task_trip VARCHAR(50),
    creator VARCHAR(50),
    executor VARCHAR(50),
    exec_time TIMESTAMP,
    end_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    task_status VARCHAR(20) DEFAULT '待巡视',
    round INTEGER DEFAULT 1,
    uploaded INTEGER DEFAULT 0,
    remark VARCHAR(255),
    cloud_task_id BIGINT,
    delete_flag INTEGER DEFAULT 0
);

COMMENT ON TABLE agv_task IS 'AGV 巡检任务表';

CREATE TABLE IF NOT EXISTS agv_flaw (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    round INTEGER DEFAULT 1,
    flaw_type VARCHAR(50),
    flaw_name VARCHAR(100),
    flaw_desc VARCHAR(255),
    flaw_distance DOUBLE PRECISION,
    flaw_image VARCHAR(255),
    flaw_image_url VARCHAR(255),
    flaw_rtsp VARCHAR(255),
    shown INTEGER DEFAULT 0,
    confirmed INTEGER DEFAULT 0,
    uploaded INTEGER DEFAULT 0,
    level VARCHAR(20),
    count_num INTEGER DEFAULT 1,
    flaw_length DOUBLE PRECISION,
    flaw_area DOUBLE PRECISION,
    source VARCHAR(50),
    remark VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delete_flag INTEGER DEFAULT 0
);

COMMENT ON TABLE agv_flaw IS 'AGV 巡检故障缺陷表';

CREATE TABLE IF NOT EXISTS agv_config (
    id BIGSERIAL PRIMARY KEY,
    host VARCHAR(64),
    drive_port INTEGER,
    analysis_port INTEGER,
    cloud_url VARCHAR(255),
    cloud_api_key VARCHAR(255),
    db_host VARCHAR(64),
    db_port INTEGER,
    db_name VARCHAR(100),
    db_username VARCHAR(100),
    db_password VARCHAR(255),
    control_protocol VARCHAR(20),
    cam1 VARCHAR(255),
    username1 VARCHAR(50),
    password1 VARCHAR(100),
    cam2 VARCHAR(255),
    username2 VARCHAR(50),
    password2 VARCHAR(100),
    cam3 VARCHAR(255),
    username3 VARCHAR(50),
    password3 VARCHAR(100),
    cam4 VARCHAR(255),
    username4 VARCHAR(50),
    password4 VARCHAR(100),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delete_flag INTEGER DEFAULT 0
);

COMMENT ON TABLE agv_config IS 'AGV 系统配置表';

CREATE TABLE IF NOT EXISTS agv_upload_record (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    info VARCHAR(255),
    type VARCHAR(50),
    status VARCHAR(50),
    upload_time TIMESTAMP,
    remark VARCHAR(255),
    progress INTEGER DEFAULT 0,
    upload_result VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delete_flag INTEGER DEFAULT 0
);

COMMENT ON TABLE agv_upload_record IS 'AGV 任务上传记录表';

CREATE TABLE IF NOT EXISTS agv_sensor_record (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    sensor_type VARCHAR(50),
    sensor_name VARCHAR(100),
    sensor_value VARCHAR(50),
    status VARCHAR(20),
    distance DOUBLE PRECISION,
    action VARCHAR(100),
    remark VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delete_flag INTEGER DEFAULT 0
);

COMMENT ON TABLE agv_sensor_record IS 'AIoT 传感器巡检记录表';

CREATE TABLE IF NOT EXISTS agv_iot_action_record (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    sensor_record_id BIGINT,
    flaw_id BIGINT,
    trigger_type VARCHAR(50),
    command_text VARCHAR(255),
    scene_type VARCHAR(50),
    scene_name VARCHAR(100),
    device_type VARCHAR(50),
    device_name VARCHAR(100),
    action VARCHAR(50),
    before_status VARCHAR(50),
    after_status VARCHAR(50),
    result VARCHAR(50),
    feedback VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delete_flag INTEGER DEFAULT 0
);

COMMENT ON TABLE agv_iot_action_record IS 'AIoT 智能巡线车联动执行记录表';
