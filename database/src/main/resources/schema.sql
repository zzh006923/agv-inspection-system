-- AGV 智能巡检系统数据库结构
-- 数据库：agv_inspection

CREATE TABLE IF NOT EXISTS agv_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    task_code VARCHAR(64) NOT NULL COMMENT '任务编号',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    start_pos VARCHAR(100) COMMENT '起始地点',
    task_trip VARCHAR(50) COMMENT '任务距离',
    creator VARCHAR(50) COMMENT '创建人',
    executor VARCHAR(50) COMMENT '执行人',
    exec_time DATETIME COMMENT '执行时间',
    end_time DATETIME COMMENT '完成时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    task_status VARCHAR(20) DEFAULT '待巡视' COMMENT '任务状态：待巡视/巡视中/待上传/已完成',
    round INT DEFAULT 1 COMMENT '巡视轮次',
    uploaded TINYINT DEFAULT 0 COMMENT '是否上传：0 未上传，1 已上传',
    remark VARCHAR(255) COMMENT '备注',
    cloud_task_id BIGINT COMMENT '云端任务 ID',
    delete_flag TINYINT DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除'
) COMMENT='AGV 巡检任务表';

CREATE TABLE IF NOT EXISTS agv_flaw (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    task_id BIGINT NOT NULL COMMENT '所属任务 ID',
    round INT DEFAULT 1 COMMENT '巡视轮次',
    flaw_type VARCHAR(50) COMMENT '缺陷类型',
    flaw_name VARCHAR(100) COMMENT '缺陷名称',
    flaw_desc VARCHAR(255) COMMENT '缺陷描述',
    flaw_distance DOUBLE COMMENT '距离原点的距离',
    flaw_image VARCHAR(255) COMMENT '缺陷图片路径',
    flaw_image_url VARCHAR(255) COMMENT '缺陷图片 URL',
    flaw_rtsp VARCHAR(255) COMMENT '缺陷视频流地址',
    shown TINYINT DEFAULT 0 COMMENT '是否已弹窗提示：0 否，1 是',
    confirmed TINYINT DEFAULT 0 COMMENT '是否确认属实：0 未确认，1 已确认',
    uploaded TINYINT DEFAULT 0 COMMENT '是否已上传：0 未上传，1 已上传',
    level VARCHAR(20) COMMENT '缺陷等级',
    count_num INT DEFAULT 1 COMMENT '缺陷数量',
    flaw_length DOUBLE COMMENT '缺陷长度',
    flaw_area DOUBLE COMMENT '缺陷面积',
    source VARCHAR(50) COMMENT '来源：图像识别/人工录入/湿度传感器/光照传感器/烟雾传感器/人员检测',
    remark VARCHAR(255) COMMENT '补充说明',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    delete_flag TINYINT DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除'
) COMMENT='AGV 巡检故障缺陷表';

CREATE TABLE IF NOT EXISTS agv_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    host VARCHAR(64) COMMENT 'AGV 主机 IP',
    drive_port INT COMMENT '硬件驱动端口',
    analysis_port INT COMMENT '分析程序端口',
    cloud_url VARCHAR(255) COMMENT '云端地址',
    cloud_api_key VARCHAR(255) COMMENT '云端API密钥/Authorization',
    db_host VARCHAR(64) COMMENT '数据库地址',
    db_port INT COMMENT '数据库端口',
    db_name VARCHAR(100) COMMENT '数据库名称',
    db_username VARCHAR(100) COMMENT '数据库用户名',
    db_password VARCHAR(255) COMMENT '数据库密码',
    control_protocol VARCHAR(20) COMMENT '车辆控制协议：http/tcp/mock',
    cam1 VARCHAR(255) COMMENT '摄像头 1 地址',
    username1 VARCHAR(50) COMMENT '摄像头 1 用户名',
    password1 VARCHAR(100) COMMENT '摄像头 1 密码',
    cam2 VARCHAR(255) COMMENT '摄像头 2 地址',
    username2 VARCHAR(50) COMMENT '摄像头 2 用户名',
    password2 VARCHAR(100) COMMENT '摄像头 2 密码',
    cam3 VARCHAR(255) COMMENT '摄像头 3 地址',
    username3 VARCHAR(50) COMMENT '摄像头 3 用户名',
    password3 VARCHAR(100) COMMENT '摄像头 3 密码',
    cam4 VARCHAR(255) COMMENT '摄像头 4 地址',
    username4 VARCHAR(50) COMMENT '摄像头 4 用户名',
    password4 VARCHAR(100) COMMENT '摄像头 4 密码',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag TINYINT DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除'
) COMMENT='AGV 系统配置表';

CREATE TABLE IF NOT EXISTS agv_upload_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    task_id BIGINT NOT NULL COMMENT '所属任务 ID',
    info VARCHAR(255) COMMENT '待上传的数据标识',
    type VARCHAR(50) COMMENT '数据类型：任务/故障/图片/传感器',
    status VARCHAR(50) COMMENT '上传状态：待上传/上传中/已上传/上传失败',
    upload_time DATETIME COMMENT '上传时间',
    remark VARCHAR(255) COMMENT '备注',
    progress INT DEFAULT 0 COMMENT '上传进度0-100',
    upload_result VARCHAR(255) COMMENT '上传结果说明',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    delete_flag TINYINT DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除'
) COMMENT='AGV 任务上传记录表';

CREATE TABLE IF NOT EXISTS agv_sensor_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    task_id BIGINT NOT NULL COMMENT '所属任务 ID',
    sensor_type VARCHAR(50) COMMENT '传感器类型：person/temperature/humidity/smoke/light',
    sensor_name VARCHAR(100) COMMENT '传感器名称',
    sensor_value VARCHAR(50) COMMENT '传感器数值',
    status VARCHAR(20) COMMENT '状态：正常/异常',
    distance DOUBLE COMMENT '当前巡检位置，单位米',
    action VARCHAR(100) COMMENT '触发动作',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    delete_flag TINYINT DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除'
) COMMENT='AIoT 传感器巡检记录表';

CREATE TABLE IF NOT EXISTS agv_iot_action_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    task_id BIGINT NOT NULL COMMENT '所属任务 ID',
    sensor_record_id BIGINT NULL COMMENT '关联传感器记录ID',
    flaw_id BIGINT NULL COMMENT '关联故障/异常记录ID',
    trigger_type VARCHAR(50) COMMENT '触发类型：sensor/voice/scene/manual',
    command_text VARCHAR(255) COMMENT '语音或控制指令文本',
    scene_type VARCHAR(50) COMMENT '场景类型：safety/environment/lighting/fire',
    scene_name VARCHAR(100) COMMENT '场景名称',
    device_type VARCHAR(50) COMMENT '执行设备类型：light/power/alarm/agv',
    device_name VARCHAR(100) COMMENT '执行设备名称',
    action VARCHAR(50) COMMENT '执行动作：开启/关闭/安全停车/断电保护等',
    before_status VARCHAR(50) COMMENT '执行前状态',
    after_status VARCHAR(50) COMMENT '执行后状态',
    result VARCHAR(50) COMMENT '执行结果：成功/失败',
    feedback VARCHAR(255) COMMENT '返回给前端、小鸿AI或日志的反馈内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    delete_flag TINYINT DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除'
) COMMENT='AIoT 智能巡线车联动执行记录表';
