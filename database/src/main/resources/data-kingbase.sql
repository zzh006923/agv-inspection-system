-- AGV 智能巡检系统初始化演示数据

INSERT INTO agv_task
(task_code, task_name, start_pos, task_trip, creator, executor, exec_time, end_time, task_status, uploaded, remark)
VALUES
('TASK202312010001', '地铁1号线隧道例行巡检', '100米', '500米', '张三', '李四', '2023-12-01 09:00:00', '2023-12-01 10:30:00', '已完成', 1, '例行巡检任务'),
('TASK202312010002', '设备故障排查巡检', '200米', '300米', '王五', '赵六', NULL, NULL, '待巡视', 0, '设备故障排查'),
('TASK202312010003', '夜间安全巡检', '50米', '800米', '陈七', '刘八', '2023-12-01 22:00:00', NULL, '待上传', 0, '夜间安全检查');

INSERT INTO agv_flaw
(task_id, round, flaw_type, flaw_name, flaw_desc, flaw_distance, flaw_image_url, confirmed, uploaded, level, source, remark)
VALUES
(1, 1, '结构缺陷', '隧道壁面裂缝', '检测到隧道壁面存在明显裂缝', 100.5, '/images/flaw/crack_001.jpg', 1, 1, '中', '图像识别', '裂缝明显，建议复查'),
(1, 1, '渗漏缺陷', '隧道顶部渗水', '检测到顶部存在渗水痕迹', 235.0, '/images/flaw/water_001.jpg', 1, 1, '低', '图像识别', '已复核'),
(3, 1, '环境异常', '隧道湿度异常', '湿度传感器检测到当前区域湿度过高', 225.0, '', 0, 0, '中', '湿度传感器', 'AIoT 环境感知生成'),
(3, 1, '安全事件', '人员靠近安全事件', '人在检测模块发现巡线车前方存在人员靠近', 151.0, '', 0, 0, '高', '人员检测', '系统已触发自动停车');

INSERT INTO agv_config
(host, drive_port, analysis_port, cloud_url, cloud_api_key, db_host, db_port, db_name, db_username, db_password, control_protocol,
 cam1, username1, password1,
 cam2, username2, password2,
 cam3, username3, password3,
 cam4, username4, password4)
VALUES
('192.168.2.57', 9001, 9002, 'http://192.168.2.57/prod-api', '', 'localhost', 54321, 'agv_inspection', 'system', 'kingbase', 'http',
 'rtsp://192.168.2.57/live/cam1', 'admin', '123456',
 'rtsp://192.168.2.57/live/cam2', 'admin', '123456',
 'rtsp://192.168.2.57/live/cam3', 'admin', '123456',
 'rtsp://192.168.2.57/live/cam4', 'admin', '123456');

INSERT INTO agv_upload_record
(task_id, info, type, status, upload_time, remark)
VALUES
(3, 'TASK202312010003', '任务', '待上传', NULL, '夜间安全巡检任务信息待上传'),
(3, '隧道湿度异常', '故障', '待上传', NULL, 'AIoT 环境异常记录待上传'),
(3, '人员靠近安全事件', '故障', '待上传', NULL, 'AIoT 安全事件待上传'),
(1, 'TASK202312010001', '任务', '已上传', '2023-12-01 10:40:00', '例行巡检任务已上传');

INSERT INTO agv_sensor_record
(task_id, sensor_type, sensor_name, sensor_value, status, distance, action, remark)
VALUES
(3, 'humidity', '湿度传感器', '86%', '异常', 225.0, '生成隧道湿度异常记录', '湿度超过80%阈值'),
(3, 'person', '人员检测传感器', 'detected', '异常', 151.0, '自动停车并生成安全事件', '检测到人员靠近巡线车'),
(3, 'light', '光照传感器', '20lux', '异常', 310.0, '提示开启补光并生成照明异常', '隧道光照低于阈值'),
(3, 'smoke', '烟雾传感器', 'true', '异常', 420.0, '触发声光报警并建议终止巡检', '检测到烟雾异常'),
(1, 'temperature', '温度传感器', '28.5℃', '正常', 80.0, '无', '环境温度正常');
