# 传感器与AIoT联动模块 README

## 负责范围

本模块主要负责 AGV 巡检系统中的传感器接入、真实小车控制、AIoT 联动和前端实时展示部分，占项目工作量约 24%。核心目标是把真实传感器和小车状态接入巡检任务流程，让系统形成“传感器采集 -> 异常判断 -> 缺陷/安全事件生成 -> IoT 设备联动 -> 前端实时展示”的闭环。

## 代码位置

后端主要代码：

```text
backend/src/main/java/com/example/agv/controller/AgvSensorController.java
backend/src/main/java/com/example/agv/controller/AgvIotController.java
backend/src/main/java/com/example/agv/controller/AgvMovementController.java
backend/src/main/java/com/example/agv/dto/SensorReportDTO.java
backend/src/main/java/com/example/agv/entity/AgvSensorRecord.java
backend/src/main/java/com/example/agv/entity/AgvIotActionRecord.java
backend/src/main/java/com/example/agv/mapper/AgvSensorRecordMapper.java
backend/src/main/java/com/example/agv/mapper/AgvIotActionRecordMapper.java
backend/src/main/java/com/example/agv/service/AgvMovementStateService.java
```

前端主要代码：

```text
frontend/src/views/TaskExecuteView.vue
frontend/src/views/TaskDetailView.vue
frontend/src/components/StatusTag.vue
frontend/src/components/StatCard.vue
```

硬件和说明文档：

```text
hardware-sensor/
docs/传感器接入说明.md
docs/真实小车接入说明.md
docs/IoT实时数据说明.md
docs/车辆控制接口说明.md
```

## 功能说明

### 1. 传感器数据接入

`AgvSensorController` 提供传感器数据上报和测试触发能力。真实传感器或脚本可以通过 `POST /agv/sensor/report` 上报温湿度、光照、烟雾和人员检测数据；前端演示按钮可以通过 `POST /agv/sensor/trigger` 模拟异常事件。

系统支持的传感器类型：

| sensorType | 含义 | 异常判断 |
| --- | --- | --- |
| `th` | 温湿度一体传感器 | 温度或湿度分别判断 |
| `temperature` | 温度传感器 | 温度 >= 35 |
| `humidity` | 湿度传感器 | 湿度 >= 80 |
| `light` | 光照传感器 | 光照 < 50 |
| `smoke` | 烟雾传感器 | 检测到烟雾或烟雾值 >= 50 |
| `person` | 人员检测传感器 | 检测到人员靠近 |

如果上报时没有传 `taskId`，后端会自动绑定当前最新的“巡检中”任务，便于真实小车侧程序直接上报。

### 2. 异常事件和缺陷记录生成

传感器正常时，系统只写入 `agv_sensor_record` 传感器记录；传感器异常时，后端会同时生成：

- `AgvSensorRecord`：保存传感器类型、数值、状态、位置和处理动作。
- `AgvFlaw`：把异常转为巡检缺陷或安全事件，进入故障复核流程。
- `AgvIotActionRecord`：保存由异常触发的设备联动结果。

例如人员靠近会生成安全事件并触发 AGV 停车；光照不足会生成照明异常并触发补光动作；烟雾异常会触发报警和停车。

### 3. AIoT 场景联动

`AgvIotController` 负责 IoT 能力查询、任务总览、设备控制、场景联动和语音/AI 指令映射。

主要接口：

```http
GET  /agv/iot/capability
GET  /agv/iot/overview/{taskId}
POST /agv/iot/device/control
POST /agv/iot/scene/trigger
POST /agv/iot/voice/command
```

支持的场景：

| sceneType | 场景 | 联动动作 |
| --- | --- | --- |
| `safety` | 人员靠近安全保护 | 生成安全事件、AGV 停车、声光报警 |
| `environment` | 环境监测 | 生成湿度异常、远程断电保护 |
| `lighting` | 隧道补光巡检 | 生成光照异常、开启补光灯 |
| `fire` | 烟雾报警 | 生成烟雾风险、声光报警、AGV 停车 |

所有联动都会写入 `agv_iot_action_record`，供前端任务执行页和任务详情页展示。

### 4. 真实小车运动控制

`AgvMovementController` 对外提供统一车辆控制接口：

```http
GET  /agv/movement/heartbeat
POST /agv/movement/forward
POST /agv/movement/stop
POST /agv/movement/backward
```

`AgvMovementStateService` 底层支持三种模式：

- `mock`：本地模拟车辆运行状态，用于无车演示。
- `http`：把控制请求转发到真实车载 HTTP 服务。
- `tcp`：向真实小车 IP 和行驶端口发送 TCP 控制指令。

通过配置 `agv.control-mode` 可以在不同接入方式间切换，前端和 Controller 接口无需改动。安全类异常如人员靠近、烟雾报警会自动调用停车能力。

### 5. 前端实时展示

`TaskExecuteView.vue` 负责巡检过程中的实时页面，包含车辆状态、前进/停止/后退控制、传感器模拟触发、AIoT 场景触发、语音指令输入、故障记录和联动记录刷新。

`TaskDetailView.vue` 负责巡检完成后的复核页面，展示故障列表、传感器记录、IoT 联动闭环、上传前检查和复核进度。

`StatusTag.vue` 和 `StatCard.vue` 用于统一状态标签和统计卡片展示，保证任务列表、执行页和详情页视觉一致。

## 数据流

```text
真实小车/传感器脚本
        |
        v
POST /agv/sensor/report
        |
        v
AgvSensorController 判断阈值
        |
        +--> 正常：写入 agv_sensor_record
        |
        +--> 异常：写入 agv_sensor_record + agv_flaw + agv_iot_action_record
                         |
                         v
                 必要时调用 AgvMovementStateService.stop()
                         |
                         v
前端 TaskExecuteView / TaskDetailView 轮询 /agv/iot/overview/{taskId} 展示结果
```

## 演示流程

1. 在前端创建并启动巡检任务。
2. 进入任务执行页，观察车辆状态和当前巡检位置。
3. 使用传感器按钮模拟温湿度、光照、烟雾、人员靠近等事件，或由真实小车侧程序调用 `/agv/sensor/report`。
4. 触发异常后，检查故障列表、传感器记录和 IoT 联动记录是否同步刷新。
5. 对人员靠近或烟雾异常场景，确认 AGV 状态切换为停止。
6. 完成或终止巡检后进入详情页，复核故障并查看传感器与联动闭环。

## 测试建议

后端建议重点验证：

- 传感器正常上报只生成传感器记录。
- 传感器异常上报生成传感器记录、缺陷记录和联动记录。
- 未传 `taskId` 时能自动绑定当前巡检中任务。
- 人员靠近和烟雾报警能触发停车。
- `mock`、`http`、`tcp` 三种车辆控制模式返回结构稳定。

前端建议重点验证：

- 任务执行页能刷新车辆心跳、传感器记录和联动记录。
- 场景联动按钮和语音指令能正确调用后端。
- 任务详情页能按传感器类型展示最新记录。
- 故障复核、上传前检查和传感器/联动记录展示不互相影响。

## 相关文档

- `docs/传感器接入说明.md`
- `docs/真实小车接入说明.md`
- `docs/IoT实时数据说明.md`
- `docs/车辆控制接口说明.md`
