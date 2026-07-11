# AGV 智能巡检系统

本项目是一个面向 AGV 智能巡检场景的软件系统，围绕巡检任务管理、摄像头接入、缺陷识别、传感器数据采集、车辆运动控制、AI 智能助手、系统设置和测试验证等功能展开。

系统采用前后端分离架构：后端基于 Spring Boot 实现业务接口与数据库访问，前端基于 Vue 和 Vite 实现巡检任务页面、缺陷展示、视频播放和系统配置页面，同时结合 YOLO 识别脚本、传感器 Hub 和 Dify 智能体完成扩展功能。

## 一、项目功能

- 巡检任务管理：支持任务新增、查询、修改、删除和状态维护；
- 缺陷识别与确认：支持缺陷信息展示、确认、修改和处理；
- 摄像头接入：支持摄像头设备查询和前端视频播放；
- YOLO 识别结果上传：支持识别脚本将检测结果上传至后端保存；
- 传感器数据采集：支持温湿度、人体检测、光照、烟雾等传感器数据上报；
- AGV 运动控制：支持前进、后退、停止、心跳等小车控制接口；
- AI 智能助手：支持通过 Dify 接入智能问答能力；
- 系统设置与自检：支持系统参数配置和运行状态检查；
- 测试验证：包含后端单元测试、前端组件测试和并发测试材料。

## 二、技术栈

### 后端

- Java
- Spring Boot
- MyBatis / Mapper
- MySQL
- Kingbase
- Maven
- JUnit

### 前端

- Vue
- Vite
- JavaScript
- Element Plus / 页面组件
- EasyPlayer 视频播放库

### 扩展能力

- YOLO 缺陷识别
- Dify AI 智能体
- 传感器 Hub
- AGV 小车控制接口
- WebRunner 并发测试

## 三、目录结构

```text
agv-inspection-system/
├─ backend/              后端 Spring Boot 工程
├─ frontend/             前端 Vue 工程
├─ database/             数据库建表和初始化脚本
├─ hardware-sensor/      传感器 Hub 或真实小车接入相关代码
├─ docs/                 项目说明、开发指南、使用说明和模块文档
├─ .gitignore            Git 忽略规则
├─ LICENSE               开源协议
└─ README.md             项目总说明
```

## 四、快速启动

### 1. 初始化数据库

根据实际数据库类型选择执行：

```text
database/schema.sql
database/data.sql
database/schema_update_real_car.sql
```

或：

```text
database/schema-kingbase.sql
database/data-kingbase.sql
database/schema_update_real_car.sql
```

### 2. 启动后端

进入后端目录：

```bash
cd backend
mvn spring-boot:run
```

### 3. 启动前端

进入前端目录：

```bash
cd frontend
npm install
npm run dev
```

### 4. 访问系统

前端默认通过 Vite 启动，启动成功后根据终端提示访问本地地址，例如：

```text
http://localhost:5173
```

## 五、文档索引

项目文档位于 `docs/` 目录，包含：

```text
docs/开发指南.md
docs/使用说明.md
docs/数据库初始化与适配说明.md
docs/缺陷识别与摄像头模块说明.md
docs/测试说明.md
docs/AGV任务管理模块详细技术说明/
docs/AI助手使用说明.md
docs/AI智能体接入说明.md
docs/Dify启动与配置说明.md
docs/IoT实时数据说明.md
docs/任务执行流程说明.md
docs/传感器与AIoT联动模块.md
docs/传感器接入说明.md
docs/前端页面使用说明.md
docs/巡检任务管理说明.md
docs/真实小车接入说明.md
docs/车辆控制接口说明.md
```

