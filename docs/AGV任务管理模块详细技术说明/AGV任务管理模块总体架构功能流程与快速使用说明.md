# AGV 智能巡检系统——任务管理模块

## 1. 模块说明

任务管理模块负责组织一次 AGV 巡检任务从创建到归档的完整过程，是系统中任务、故障、传感器、车辆控制和上传记录之间的业务入口。

该模块由前端和后端共同组成：

- 前端提供任务列表、任务表单、实时执行、任务复盘和数据上传页面。
- 后端提供任务查询、新增、修改、逻辑删除、启动、结束、上传前检查和数据上传接口。
- 任务状态用于约束不同阶段可执行的操作，避免重复启动、执行中修改、未复核故障直接上传等不合理情况。

## 2. 模块功能

任务管理模块目前包含以下功能：

1. 按任务编号、创建人、执行人和任务状态查询任务。
2. 分页展示任务列表。
3. 自动生成任务编号。
4. 新增和修改巡检任务。
5. 对尚未开始的任务进行逻辑删除。
6. 启动任务并进入实时巡检页面。
7. 完成或终止正在执行的任务。
8. 查看任务详情、故障记录、传感器记录和联动记录。
9. 检查故障是否全部完成确认。
10. 汇总任务相关数据并执行本地或远端上传。
11. 将任务状态从“待巡视”依次推进到“已完成”。

## 3. 任务状态流转

```text
新建任务
   │
   ▼
待巡视 ──启动──> 巡视中 ──完成/终止──> 待上传 ──上传──> 已完成
   │
   ├──允许修改
   └──允许删除
```

各状态允许的主要操作如下：

| 任务状态 | 允许操作 |
|---|---|
| 待巡视 | 查看、修改、删除、启动 |
| 巡视中 | 查看实时数据、车辆控制、结束、终止 |
| 待上传 | 查看详情、故障复核、上传前检查、数据上传 |
| 已完成 | 查看任务、故障和上传记录 |

终止任务不会删除已经产生的数据。终止后任务进入“待上传”状态，并在备注中记录终止信息。

## 4. 模块结构

```text
前端页面
├── TaskView.vue             任务列表、查询、统计和操作入口
├── TaskFormDialog.vue       新增与修改任务
├── TaskExecuteView.vue      实时巡检、车辆控制和数据轮询
└── TaskDetailView.vue       任务详情、故障复盘和数据上传
          │
          ▼
src/api/agv.js               taskApi 统一接口封装
          │
          ▼
AgvTaskController            请求处理、状态校验和上传闭环
          │
          ├── AgvTaskMapper
          ├── AgvFlawMapper
          ├── AgvUploadRecordMapper
          ├── AgvSensorRecordMapper
          ├── AgvIotActionRecordMapper
          └── CloudUploadService
          │
          ▼
MySQL / KingbaseES
```

当前实际控制器主要直接调用 Mapper 完成任务业务，并调用 `CloudUploadService` 完成上传。项目中保留了 `AgvTaskService`，其中包含基础任务业务方法，可在后续重构时由 Controller 统一调用。

## 5. 主要文件

### 后端

```text
src/main/java/com/example/agv/controller/AgvTaskController.java
src/main/java/com/example/agv/entity/AgvTask.java
src/main/java/com/example/agv/mapper/AgvTaskMapper.java
src/main/java/com/example/agv/service/AgvTaskService.java
src/main/java/com/example/agv/service/CloudUploadService.java
src/main/resources/schema.sql
src/main/resources/schema-kingbase.sql
```

### 前端

```text
src/views/TaskView.vue
src/views/TaskExecuteView.vue
src/views/TaskDetailView.vue
src/components/TaskFormDialog.vue
src/api/agv.js
src/api/http.js
src/router/index.js
```

## 6. 前端路由

| 页面 | 路由 | 说明 |
|---|---|---|
| 任务列表 | `/tasks` | 查询、新增、修改、删除和启动任务 |
| 实时巡检 | `/task/:id/execute` | 视频、车辆、故障和传感器实时操作 |
| 任务复盘 | `/task/:id/detail` | 查看详情、确认故障和上传数据 |

项目使用 Hash 路由，因此浏览器中的实际地址通常类似：

```text
http://localhost:5173/#/tasks
```

## 7. 后端接口概览

接口统一以 `/agv/task` 为前缀。

| 功能 | 请求方式 | 接口 |
|---|---|---|
| 查询任务列表 | GET | `/agv/task/list` |
| 查询任务详情 | GET | `/agv/task/{id}` |
| 获取下一个任务编号 | GET | `/agv/task/next-code` |
| 新增任务 | POST | `/agv/task` |
| 修改任务 | PUT | `/agv/task` |
| 删除任务 | DELETE | `/agv/task/{id}` |
| 启动任务 | POST | `/agv/task/start/{id}` |
| 结束或终止任务 | POST | `/agv/task/end/{id}` |
| 上传前检查 | GET | `/agv/task/preupload/{id}` |
| 上传任务数据 | POST | `/agv/task/upload/{id}` |

详细接口说明见 [README-BACKEND.md](AGV任务管理模块后端分层架构接口数据库与部署说明.md)。

## 8. 快速启动

### 启动后端

```bash
cd agv-backend-dify-ai-kingbase-merged
mvn spring-boot:run
```

默认地址：

```text
http://localhost:8088
```

后端启动前需要完成数据库配置和建表，具体步骤见 [README-BACKEND.md](AGV任务管理模块后端分层架构接口数据库与部署说明.md)。

### 启动前端

```bash
cd agv-frontend-clean
npm install
npm run dev
```

默认地址：

```text
http://localhost:5173
```

前端环境变量和页面说明见 [README-FRONTEND.md](AGV任务管理模块前端页面组件路由接口与运行说明.md)。

## 9. 与其他模块的关系

任务模块会调用或使用以下模块的数据：

- 故障模块：查询故障、查看实时故障、完成故障复核。
- 车辆控制模块：获取心跳、前进、停止和后退。
- 摄像头模块：获取视频通道并转换为浏览器可播放地址。
- 传感器模块：查询和触发温度、湿度、光照、烟雾和人员检测数据。
- AIoT 模块：查询联动记录、触发场景和处理语音指令。
- 数据上传模块：汇总任务、故障、传感器和联动数据。
- AI 分析模块：写入裂缝识别结果，并在详情页进行辅助复盘。

这些模块通过 `taskId` 将数据关联到具体任务。

## 10. 当前实现说明

1. 列表接口采用“先查询、后截取”的内存分页方式，适合课程项目规模。数据量较大时应改为数据库分页。
2. 任务编号采用“日期前缀 + 四位序号”，并发创建时建议增加唯一索引和冲突重试。
3. 删除操作使用 MyBatis-Plus 逻辑删除，不会直接清除数据库中的任务记录。
4. 上传前后端均会检查任务状态和故障确认情况。
5. 未开启真实云端上传时，系统仍可完成本地状态闭环。
6. 前端统计卡片统计的是当前分页结果中的状态数量，并非数据库全部任务的状态总数。
7. 当前详情页对“没有任何故障的任务”设置了较严格的本地上传条件，建议后续直接以服务端返回的 `summary.canUpload` 为最终依据。

## 11. 其他文档

- [后端开发说明](AGV任务管理模块后端分层架构接口数据库与部署说明.md)
- [前端开发说明](AGV任务管理模块前端页面组件路由接口与运行说明.md)
- [前后端联调与测试说明](AGV任务管理模块前后端联调测试用例与常见问题排查指南.md)
