任务管理模块后端 README
1. 模块定位
任务管理后端负责保存任务基础信息、维护任务生命周期、校验业务状态，并在任务结束后汇总故障、传感器、AIoT 联动和上传记录。
后端服务默认端口为：
```text
8088
```
任务接口统一前缀为：
```text
/agv/task
```
2. 技术栈
技术	项目版本或用途
Java	17
Spring Boot	2.7.18
MyBatis-Plus	3.5.5
MySQL Connector/J	8.0.33
KingbaseES JDBC	9.0.0
Maven	依赖管理和项目构建
数据库支持 MySQL 与 KingbaseES 二选一运行。
3. 核心文件
```text
src/main/java/com/example/agv/
├── controller/
│   └── AgvTaskController.java
├── entity/
│   └── AgvTask.java
├── mapper/
│   └── AgvTaskMapper.java
└── service/
    ├── AgvTaskService.java
    └── CloudUploadService.java
```
相关数据协作文件包括：
```text
AgvFlawMapper.java
AgvUploadRecordMapper.java
AgvSensorRecordMapper.java
AgvIotActionRecordMapper.java
```
数据库脚本：
```text
src/main/resources/schema.sql
src/main/resources/data.sql
src/main/resources/schema-kingbase.sql
src/main/resources/data-kingbase.sql
```
4. 实际调用结构
当前版本的主要调用路径为：
```text
前端请求
  ↓
AgvTaskController
  ├── AgvTaskMapper
  ├── AgvFlawMapper
  ├── AgvUploadRecordMapper
  ├── AgvSensorRecordMapper
  ├── AgvIotActionRecordMapper
  └── CloudUploadService
```
`AgvTaskService` 中已经提供任务查询、新增、启动、结束和上传等基础方法，但当前 `AgvTaskController` 没有直接注入该 Service。后续可将控制器中的业务代码迁移到 Service 层，使层次职责更加清晰。
5. 任务数据模型
实体类 `AgvTask` 映射数据库表 `agv_task`。
Java 属性	数据库字段	说明
id	id	主键，自增
taskCode	task_code	任务编号
taskName	task_name	任务名称
startPos	start_pos	起始地点
taskTrip	task_trip	巡检距离
creator	creator	创建人
executor	executor	执行人
execTime	exec_time	启动时间
endTime	end_time	结束时间
createTime	create_time	创建时间
taskStatus	task_status	任务状态
round	round	巡检轮次
uploaded	uploaded	上传标志，0 未上传，1 已上传
remark	remark	备注
cloudTaskId	cloud_task_id	云端任务编号
deleteFlag	delete_flag	逻辑删除标志
`deleteFlag` 使用 `@TableLogic`，调用 `deleteById` 时执行逻辑删除。
6. 状态规则
动作	操作前状态	操作后状态	主要处理
新增任务	无	待巡视	初始化创建时间、上传标志和轮次
修改任务	待巡视	待巡视	更新任务基础信息
删除任务	待巡视	逻辑删除	将删除标志改为 1
启动任务	待巡视	巡视中	写入执行时间
完成任务	巡视中	待上传	写入结束时间
终止任务	巡视中	待上传	写入结束时间和终止备注
上传任务	待上传	已完成	更新任务、故障和上传记录
上传任务还要求关联故障全部完成确认。
7. 接口说明
7.1 查询任务列表
```http
GET /agv/task/list
```
查询参数：
参数	必填	默认值	说明
taskCode	否	无	任务编号，模糊查询
creator	否	无	创建人，模糊查询
executor	否	无	执行人，模糊查询
taskStatus	否	无	任务状态，精确查询
pageNum	否	1	页码
pageSize	否	10	每页数量
示例：
```http
GET /agv/task/list?taskStatus=待巡视&pageNum=1&pageSize=10
```
列表返回结构：
```json
{
  "code": 200,
  "msg": "查询成功",
  "total": 1,
  "rows": [
    {
      "id": 1,
      "taskCode": "TASK202607100001",
      "taskName": "一号线路巡检",
      "taskStatus": "待巡视"
    }
  ]
}
```
当前实现先查询全部匹配记录，再使用 `subList` 截取当前页。
7.2 查询任务详情
```http
GET /agv/task/{id}
```
任务不存在或已逻辑删除时返回：
```json
{
  "code": 500,
  "msg": "任务不存在",
  "data": null
}
```
7.3 获取下一个任务编号
```http
GET /agv/task/next-code
```
编号规则：
```text
TASK + yyyyMMdd + 四位顺序号
```
示例：
```text
TASK202607100001
```
后端新增任务时会再次进行兜底生成。
7.4 新增任务
```http
POST /agv/task
Content-Type: application/json
```
请求示例：
```json
{
  "taskCode": "TASK202607100001",
  "taskName": "隧道一号线巡检",
  "startPos": "A 端入口",
  "taskTrip": "500m",
  "creator": "管理员",
  "executor": "巡检员",
  "round": 1,
  "remark": "重点检查潮湿和裂缝区域"
}
```
后端统一设置：
```text
taskStatus = 待巡视
uploaded   = 0
deleteFlag = 0
createTime = 当前时间
round      = 1（请求为空时）
```
7.5 修改任务
```http
PUT /agv/task
Content-Type: application/json
```
请求体必须包含 `id`，并且任务状态必须为“待巡视”。
```json
{
  "id": 1,
  "taskName": "修改后的任务名称",
  "executor": "新的执行人",
  "remark": "修改路线说明"
}
```
7.6 删除任务
```http
DELETE /agv/task/{id}
```
仅“待巡视”任务允许删除。该操作为逻辑删除。
7.7 启动任务
```http
POST /agv/task/start/{id}
```
仅“待巡视”任务允许启动。成功后：
```text
taskStatus = 巡视中
execTime   = 当前时间
```
7.8 结束或终止任务
正常结束：
```http
POST /agv/task/end/{id}?isAbort=false
```
终止任务：
```http
POST /agv/task/end/{id}?isAbort=true
```
仅“巡视中”任务允许操作。成功后：
```text
taskStatus = 待上传
endTime    = 当前时间
```
终止时还会设置：
```text
remark = 任务被终止
```
7.9 上传前检查
```http
GET /agv/task/preupload/{id}
```
返回内容包括：
任务信息。
任务下的故障列表。
上传记录。
故障总数。
未确认故障数。
未上传故障数。
是否允许上传。
返回示例：
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "task": {},
    "flaws": [],
    "uploadRecords": [],
    "summary": {
      "taskId": 1,
      "taskStatus": "待上传",
      "flawCount": 2,
      "unconfirmedCount": 0,
      "notUploadedFlawCount": 2,
      "canUpload": true
    }
  }
}
```
`canUpload` 的后端判定条件为：
```text
任务状态为“待上传”并且未确认故障数为 0
```
7.10 上传任务数据
```http
POST /agv/task/upload/{id}
```
处理顺序：
校验任务存在。
校验任务状态为“待上传”。
查询任务下所有故障。
拒绝存在未确认故障的任务。
查询上传记录、传感器记录和 AIoT 动作记录。
调用 `CloudUploadService`。
将任务改为“已完成”并设置 `uploaded=1`。
将所有故障设置为已上传。
新增或更新上传记录。
返回本次上传的完整结果。
8. 统一响应结构
普通接口使用：
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```
业务失败时当前项目仍返回 JSON 中的 `code=500`，HTTP 状态通常仍由 Spring 控制器正常返回。前端通过 `assertSuccess` 检查业务码。
列表接口使用：
```json
{
  "code": 200,
  "msg": "查询成功",
  "total": 0,
  "rows": []
}
```
9. 数据库配置
9.1 选择数据库
在 `src/main/resources/application.yml` 中设置：
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
9.2 MySQL
创建数据库：
```sql
CREATE DATABASE agv_inspection
DEFAULT CHARACTER SET utf8mb4;
```
依次执行：
```text
src/main/resources/schema.sql
src/main/resources/data.sql
```
配置示例：
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/agv_inspection?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
```
9.3 KingbaseES
创建数据库并依次执行：
```text
src/main/resources/schema-kingbase.sql
src/main/resources/data-kingbase.sql
```
配置示例：
```yaml
spring:
  datasource:
    driver-class-name: com.kingbase8.Driver
    url: jdbc:kingbase8://localhost:54321/agv_inspection
    username: your_username
    password: your_password
```
项目 `pom.xml` 声明了金仓驱动依赖。若本地 Maven 仓库中没有该驱动，可将项目内的 JAR 安装到本地仓库：
```bash
mvn install:install-file \
  -Dfile=lib/kingbase8.jar \
  -DgroupId=com.kingbase \
  -DartifactId=kingbase8 \
  -Dversion=9.0.0 \
  -Dpackaging=jar
```
Windows PowerShell 可写成单行执行。
10. 启动方式
```bash
mvn clean spring-boot:run
```
指定 profile：
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```
或：
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=kingbase
```
接口连通测试：
```bash
curl "http://localhost:8088/agv/task/list?pageNum=1&pageSize=10"
```
11. 云端上传配置
`application.yml` 中的关键配置：
```yaml
agv:
  cloud-upload-enabled: false
  cloud-upload-path: /agv/task/upload
  cloud-api-key: ""
```
`false`：只完成本地状态闭环。
`true`：将任务、故障、上传记录、传感器记录和联动记录发送到云端。
云端基础地址从系统配置中的 `cloudUrl` 读取。
API Key 建议使用环境变量或外部配置，不要提交真实密钥。
12. 开发注意事项
`AgvTaskController` 当前承担了较多业务逻辑，后续建议迁移到 `AgvTaskService`。
上传操作包含多次数据库更新，建议增加 `@Transactional`，避免中途失败造成部分数据已更新。
新增和修改接口目前未使用 Bean Validation，可增加 `@Valid`、`@NotBlank` 等校验。
任务编号生成使用“查询最大编号后加一”，建议为 `task_code` 增加唯一索引。
列表查询建议改用 MyBatis-Plus `Page` 和 `selectPage`。
更新接口直接接收实体对象，建议后续使用 DTO，限制前端可修改的字段。
异常返回建议进一步统一 HTTP 状态码和业务状态码。
切勿把真实数据库密码、Dify Key、云端 Key 或设备认证信息提交到公开仓库。
