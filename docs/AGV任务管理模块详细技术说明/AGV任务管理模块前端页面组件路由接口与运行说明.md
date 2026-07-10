# 任务管理模块前端 README

## 1. 模块定位

任务管理前端负责展示任务数据、接收用户操作，并调用后端接口完成任务生命周期管理。

模块包含三类主要页面：

1. 任务列表与编辑。
2. 实时任务执行。
3. 任务详情、故障复盘与数据上传。

## 2. 技术栈

| 技术 | 版本或用途 |
|---|---|
| Vue | 3.5.13 |
| Vue Router | 4.5.0 |
| Vite | 6.0.7 |
| Element Plus | 2.9.4 |
| Axios | 1.7.9 |
| Vitest | 组件和工具测试 |

## 3. 核心文件

```text
src/
├── api/
│   ├── agv.js
│   └── http.js
├── components/
│   ├── TaskFormDialog.vue
│   ├── FlawDialog.vue
│   ├── StatCard.vue
│   ├── StatusTag.vue
│   └── VideoPlayer.vue
├── router/
│   └── index.js
└── views/
    ├── TaskView.vue
    ├── TaskExecuteView.vue
    └── TaskDetailView.vue
```

## 4. 路由说明

| 路由 | 组件 | 功能 |
|---|---|---|
| `/tasks` | `TaskView.vue` | 任务列表、查询、新增、修改、删除、启动 |
| `/task/:id/execute` | `TaskExecuteView.vue` | 实时巡检、车辆控制、故障轮询、传感器联动 |
| `/task/:id/detail` | `TaskDetailView.vue` | 任务详情、故障复核、传感器记录、数据上传 |

路由参数 `id` 为任务主键。

## 5. 页面说明

### 5.1 TaskView.vue

任务列表页提供：

- 按任务编号、创建人、执行人和状态查询。
- 分页切换。
- 新增任务。
- 修改待巡视任务。
- 删除待巡视任务。
- 启动待巡视任务。
- 继续正在执行的任务。
- 进入待上传任务的复盘页面。
- 查看已完成任务的历史记录。

页面操作按钮会根据任务状态动态显示。

| 状态 | 页面主要按钮 |
|---|---|
| 待巡视 | 启动、修改、删除、详情 |
| 巡视中 | 继续巡视、详情 |
| 待上传 | 复盘上传、详情 |
| 已完成 | 查看记录、详情 |

页面顶部状态卡片中的分类数量由当前页 `tasks` 计算，因此只反映当前页数据；总任务数使用后端返回的 `total`。

### 5.2 TaskFormDialog.vue

任务表单组件同时支持新增和修改。

新增模式：

- 打开对话框后调用 `/agv/task/next-code` 获取编号。
- 任务编号也可以手动重新生成。
- 支持“创建后立即启动”。
- 保存成功后向父组件发送 `saved` 事件。

修改模式：

- 回显当前任务数据。
- 调用更新接口保存。
- 后端会再次校验任务是否处于“待巡视”状态。

必填字段：

```text
任务名称
任务编号
起始地点
巡检距离
创建人
执行人
```

### 5.3 TaskExecuteView.vue

实时执行页负责：

- 加载任务基础信息。
- 获取系统配置和摄像头列表。
- 展示多路视频通道。
- 获取车辆心跳和当前位置。
- 发送前进、停止和后退指令。
- 每 3 秒刷新车辆、故障和 AIoT 数据。
- 弹出新故障提醒。
- 触发传感器事件。
- 触发 AIoT 场景联动。
- 发送语音或 AI 指令。
- 模拟写入裂缝识别结果。
- 完成或终止任务。

刷新使用：

```js
Promise.allSettled([
  refreshMovement(),
  refreshFlaws(),
  refreshIot()
])
```

单个请求失败不会阻塞其他区域刷新。组件卸载时会清除定时器。

任务完成或终止前，页面会先尝试发送停车指令，然后调用任务结束接口并跳转到详情页。

### 5.4 TaskDetailView.vue

任务详情页负责：

- 展示任务状态和故障数量。
- 展示巡检路线和故障位置。
- 查看故障图片。
- 打开故障复核弹窗。
- 显示传感器最新记录。
- 显示 AIoT 动作记录。
- 显示上传记录。
- 执行上传前检查。
- 提交任务数据上传。

上传前页面会重新加载故障和上传检查信息，避免使用过期数据。

当前页面本地上传条件为：

```text
故障总数大于 0，并且未确认故障数为 0
```

后端条件为：

```text
任务状态为待上传，并且未确认故障数为 0
```

因此，无故障任务可能出现后端允许上传、前端按钮判断较严格的差异。建议后续以 `summary.canUpload` 为主要判断依据。

## 6. API 封装

任务接口位于：

```text
src/api/agv.js
```

```js
export const taskApi = {
  list: params => http.get('/agv/task/list', { params }),
  get: id => http.get(`/agv/task/${id}`),
  nextCode: () => http.get('/agv/task/next-code'),
  add: data => http.post('/agv/task', data),
  update: data => http.put('/agv/task', data),
  remove: id => http.delete(`/agv/task/${id}`),
  start: id => http.post(`/agv/task/start/${id}`),
  end: (id, isAbort = false) =>
    http.post(`/agv/task/end/${id}`, null, { params: { isAbort } }),
  preupload: id => http.get(`/agv/task/preupload/${id}`),
  upload: id => http.post(`/agv/task/upload/${id}`)
}
```

页面不直接拼接任务接口地址，而是调用 `taskApi`。

## 7. HTTP 响应处理

`src/api/http.js` 中创建 Axios 实例：

```js
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8088',
  timeout: 12000
})
```

响应拦截器会直接返回 `response.data`。

普通接口还需要调用：

```js
unwrap(result, '操作失败')
```

`unwrap` 会检查业务状态码，并返回 `result.data`。

任务列表接口结构为顶层 `rows` 和 `total`，因此列表页直接读取：

```js
tasks.value = result.rows || []
total.value = result.total || 0
```

## 8. 环境配置

开发环境文件：

```text
.env.development
```

基础配置：

```env
VITE_API_BASE_URL=http://localhost:8088
```

若使用 Vite 代理，可设置：

```env
VITE_API_BASE_URL=/api
```

摄像头转流配置：

```env
VITE_WEBRTC_BASE_URL=http://your-device/webrtc-api
VITE_CAMERA_STREAM_1=stream-id-1
VITE_CAMERA_STREAM_2=stream-id-2
VITE_CAMERA_STREAM_3=stream-id-3
VITE_CAMERA_STREAM_4=stream-id-4
```

不要在公开仓库中提交真实设备凭据或敏感地址。

## 9. 安装与运行

推荐使用较新的 Node.js LTS 版本。

```bash
npm install
npm run dev
```

默认访问地址：

```text
http://localhost:5173
```

构建生产包：

```bash
npm run build
```

本地预览：

```bash
npm run preview
```

## 10. 测试

运行全部测试：

```bash
npm run test
```

监听模式：

```bash
npm run test:watch
```

覆盖率：

```bash
npm run test:coverage
```

当前项目已经包含 `TaskFormDialog.test.js`，并包含多个公共组件和工具函数测试。建议继续补充：

- 任务列表条件查询测试。
- 状态按钮显示测试。
- 创建后自动启动测试。
- 执行页定时器清理测试。
- 无故障任务上传条件测试。
- 上传前重新校验测试。
- API 失败后的错误提示测试。

## 11. 与其他模块的调用关系

`TaskExecuteView` 除任务接口外，还调用：

```text
cameraApi
configApi
movementApi
flawApi
sensorApi
iotApi
analysisApi
```

`TaskDetailView` 还调用：

```text
flawApi
iotApi
```

因此，单独运行任务前端页面时，应确保相关后端接口可用，或开启后端 Mock 模式。

## 12. 开发注意事项

1. 前端按钮控制只能改善用户体验，真正的状态限制仍必须由后端校验。
2. 页面切换时应保证执行页定时器被清理。
3. 实时数据接口失败时不要阻塞其他区域刷新。
4. 浏览器不能直接播放 RTSP，当前代码会将摄像头 ID 转为 FLV 地址。
5. 任务列表统计卡片不是全库统计。
6. 详情页上传条件应与后端保持一致。
7. `TaskExecuteView` 和 `TaskDetailView` 依赖的模块较多，修改接口结构时应同步检查这两个页面。
8. 接口基础地址、设备地址和视频流 ID 应放在环境变量中。
