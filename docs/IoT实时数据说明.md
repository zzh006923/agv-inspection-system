# IoT实时数据说明

## 能力接口

查询当前 IoT 模块支持的传感器、执行设备和语音命令：

```http
GET /agv/iot/capability
```

## 任务总览

查询指定任务下的传感器记录、IoT 执行动作和车辆状态：

```http
GET /agv/iot/overview/{taskId}
```

返回数据包括：

- `sensorRecords`: 传感器上报记录
- `actionRecords`: 设备联动记录
- `movementStatus`: 小车运动状态
- `summary`: 当前任务 IoT 汇总信息

## 场景联动

触发场景联动：

```http
POST /agv/iot/scene/trigger
Content-Type: application/json
```

```json
{
  "taskId": 1,
  "sceneType": "safety",
  "distance": 10.5
}
```

支持的 `sceneType`：

- `safety`: 人员靠近安全保护
- `environment`: 温湿度环境监测
- `lighting`: 光照补光
- `fire`: 烟雾报警
