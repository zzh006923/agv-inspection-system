# 任务管理模块前后端联调与测试 README

## 1. 联调目标

本文档用于验证任务管理模块的完整业务流程：

```text
新增任务
→ 查询任务
→ 修改任务
→ 启动任务
→ 实时巡检
→ 完成或终止任务
→ 故障复核
→ 上传前检查
→ 上传任务数据
→ 查看已完成记录
```

## 2. 运行环境

建议环境：

```text
Java 17
Maven 3.8+
Node.js LTS
MySQL 8 或 KingbaseES V8
```

默认端口：

| 服务 | 端口 |
|---|---|
| 后端 | 8088 |
| 前端 | 5173 |
| MySQL | 3306 |
| KingbaseES | 54321（按实际安装配置） |

## 3. 启动顺序

### 第一步：准备数据库

MySQL：

```text
创建 agv_inspection 数据库
执行 schema.sql
执行 data.sql
```

KingbaseES：

```text
创建 agv_inspection 数据库
执行 schema-kingbase.sql
执行 data-kingbase.sql
```

### 第二步：选择后端 profile

```yaml
spring:
  profiles:
    active: mysql
```

或：

```yaml
spring:
  profiles:
    active: kingbase
```

### 第三步：启动后端

```bash
mvn spring-boot:run
```

确认接口可访问：

```bash
curl "http://localhost:8088/agv/task/list?pageNum=1&pageSize=10"
```

### 第四步：配置前端地址

`.env.development`：

```env
VITE_API_BASE_URL=http://localhost:8088
```

### 第五步：启动前端

```bash
npm install
npm run dev
```

访问：

```text
http://localhost:5173/#/tasks
```

## 4. 推荐联调模式

没有真实小车、摄像头或云端服务时，建议使用课堂演示模式：

```yaml
agv:
  mock-mode: true
  control-mode: mock
  cloud-upload-enabled: false
```

该模式可验证任务业务闭环，但不会向真实车辆或云端发送数据。

接入真实设备时：

```yaml
agv:
  mock-mode: false
  control-mode: http
```

真实设备联调建议先验证心跳和停止，再测试前进与后退。

## 5. 接口联调顺序

以下示例假设后端地址为：

```text
http://localhost:8088
```

### 5.1 获取任务编号

```bash
curl "http://localhost:8088/agv/task/next-code"
```

### 5.2 新增任务

```bash
curl -X POST "http://localhost:8088/agv/task" \
  -H "Content-Type: application/json" \
  -d '{
    "taskName": "联调测试任务",
    "startPos": "A端入口",
    "taskTrip": "500m",
    "creator": "测试人员",
    "executor": "巡检人员",
    "round": 1,
    "remark": "任务管理模块联调"
  }'
```

记录返回数据中的任务 `id`。

### 5.3 查询任务列表

```bash
curl "http://localhost:8088/agv/task/list?taskCode=TASK&pageNum=1&pageSize=10"
```

### 5.4 查询任务详情

```bash
curl "http://localhost:8088/agv/task/1"
```

将示例中的 `1` 替换为真实任务 ID。

### 5.5 修改任务

```bash
curl -X PUT "http://localhost:8088/agv/task" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "taskName": "修改后的联调任务",
    "executor": "新的执行人",
    "remark": "修改成功"
  }'
```

任务启动后再次调用该接口，应返回“只有待巡视任务可以修改”。

### 5.6 启动任务

```bash
curl -X POST "http://localhost:8088/agv/task/start/1"
```

预期：

```text
任务状态变为 巡视中
写入 execTime
```

重复启动应被拒绝。

### 5.7 完成任务

```bash
curl -X POST "http://localhost:8088/agv/task/end/1?isAbort=false"
```

预期：

```text
任务状态变为 待上传
写入 endTime
```

终止任务测试：

```bash
curl -X POST "http://localhost:8088/agv/task/end/1?isAbort=true"
```

终止后备注应记录“任务被终止”。

### 5.8 上传前检查

```bash
curl "http://localhost:8088/agv/task/preupload/1"
```

重点检查：

```text
data.summary.taskStatus
data.summary.flawCount
data.summary.unconfirmedCount
data.summary.canUpload
```

### 5.9 上传任务

```bash
curl -X POST "http://localhost:8088/agv/task/upload/1"
```

预期：

```text
任务状态变为 已完成
任务 uploaded 变为 1
故障 uploaded 变为 1
新增或更新上传记录
```

若存在未确认故障，应先通过故障接口完成确认。

## 6. 前端页面联调顺序

### 6.1 任务列表页

检查：

- 页面能够加载任务列表。
- 条件查询有效。
- 分页能够切换。
- 新增任务后列表刷新。
- 待巡视任务显示启动、修改和删除按钮。
- 其他状态不显示不允许的操作。

### 6.2 任务表单

检查：

- 新增弹窗自动获取编号。
- 必填项为空时无法提交。
- 保存后对话框关闭。
- 勾选“立即启动”后自动进入执行页。
- 修改模式能正确回显数据。

### 6.3 实时执行页

检查：

- 任务信息加载正确。
- 每 3 秒刷新车辆、故障和 AIoT 数据。
- 前进、停止、后退接口可调用。
- 新故障能够弹出提示。
- 完成或终止任务后跳转详情页。
- 离开页面后不再继续轮询。

### 6.4 任务详情页

检查：

- 任务、故障、传感器和动作记录正确显示。
- 故障图片候选地址能够正常切换。
- 故障复核后进度更新。
- 未确认故障存在时不能上传。
- 上传成功后任务状态变为已完成。
- 上传记录能够刷新显示。

## 7. 业务状态测试

| 编号 | 测试场景 | 预期结果 |
|---|---|---|
| T01 | 新增任务 | 状态为待巡视，uploaded=0 |
| T02 | 获取编号 | 返回 TASK+日期+四位序号 |
| T03 | 修改待巡视任务 | 修改成功 |
| T04 | 修改巡视中任务 | 返回状态错误 |
| T05 | 删除待巡视任务 | 逻辑删除成功 |
| T06 | 删除已启动任务 | 返回状态错误 |
| T07 | 启动待巡视任务 | 状态变为巡视中 |
| T08 | 重复启动 | 返回状态错误 |
| T09 | 结束巡视中任务 | 状态变为待上传 |
| T10 | 终止巡视中任务 | 状态变为待上传并记录终止 |
| T11 | 上传非待上传任务 | 返回状态错误 |
| T12 | 上传存在未确认故障的任务 | 拒绝上传 |
| T13 | 上传满足条件的任务 | 状态变为已完成 |
| T14 | 查询已删除任务 | 返回任务不存在 |
| T15 | 页码超过总页数 | 返回空 rows，不抛出下标异常 |

## 8. 常见问题

### 8.1 前端提示网络请求失败

检查：

```text
后端是否启动
VITE_API_BASE_URL 是否为 http://localhost:8088
浏览器 Network 面板中的实际请求地址
后端控制台是否有异常
```

### 8.2 后端启动时找不到 Kingbase 驱动

将 `lib/kingbase8.jar` 安装到本地 Maven 仓库：

```bash
mvn install:install-file -Dfile=lib/kingbase8.jar -DgroupId=com.kingbase -DartifactId=kingbase8 -Dversion=9.0.0 -Dpackaging=jar
```

### 8.3 任务列表为空

检查：

- 数据库是否执行初始化脚本。
- `spring.profiles.active` 是否与实际数据库一致。
- 数据库连接信息是否正确。
- `delete_flag` 是否为 0。
- 查询条件是否过于严格。

### 8.4 任务无法修改或删除

这是状态约束。只有“待巡视”任务允许修改和删除。

### 8.5 任务无法结束

只有“巡视中”任务允许结束或终止。

### 8.6 任务无法上传

检查：

- 任务是否为“待上传”。
- 是否存在未确认故障。
- 前端是否重新执行了上传前检查。
- 云端上传开启时，云端地址和认证是否正确。

### 8.7 无故障任务前端无法上传

当前前端本地条件要求故障总数大于 0，后端没有该限制。建议将详情页上传判断改为以：

```js
summary.value.canUpload
```

为准。

### 8.8 视频无法播放

浏览器不能直接播放 RTSP。检查：

```text
VITE_WEBRTC_BASE_URL
摄像头 stream ID
webrtc-api 是否正常
生成的 /live/{cameraId}_01.flv 地址是否可访问
```

### 8.9 状态统计与实际总数不一致

任务列表页的分类状态数量只统计当前页数据。需要全量统计时，应新增后端统计接口。

## 9. 验收清单

### 后端

- [ ] 数据库可以正常连接。
- [ ] 任务表已经创建。
- [ ] 列表与详情接口正常。
- [ ] 新增、修改和逻辑删除正常。
- [ ] 状态流转限制生效。
- [ ] 上传前检查结果正确。
- [ ] 上传后任务、故障和记录同步更新。
- [ ] MySQL 或 KingbaseES profile 可正确切换。

### 前端

- [ ] 任务列表正常显示。
- [ ] 条件查询和分页正常。
- [ ] 新增和修改表单正常。
- [ ] 状态按钮显示正确。
- [ ] 启动后进入执行页。
- [ ] 执行页定时刷新并能释放定时器。
- [ ] 结束后进入详情页。
- [ ] 故障复核和上传流程正常。
- [ ] 请求失败时有明确提示。

### 完整闭环

- [ ] 新建一条任务。
- [ ] 启动任务。
- [ ] 产生或导入巡检记录。
- [ ] 完成任务。
- [ ] 完成故障确认。
- [ ] 执行上传。
- [ ] 查看已完成任务和上传记录。
