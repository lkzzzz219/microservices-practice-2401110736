# Week 03：个人项目 Spring Boot 起步

- 本周任务：沿用第 02 周选题，创建可运行的 Spring Boot 工程（启动类、application.yml、`/api/hello`、Actuator 健康检查、`@SpringBootTest` 启动测试）。本周不涉及业务建模与数据库。

## 本周计划

1. 在 `docs/project-proposal.md` 记录选题衔接信息（项目名称、目标用户、优先实现的业务场景、两个核心模型）；
2. 在仓库根目录新建 `monolith/`，通过 Spring Initializr 生成 Maven 工程（Java 25、Spring Boot 4.0.8、Web MVC + Actuator，含 Maven Wrapper）；
3. 将配置统一改为 `application.yml`，编写 `/api/hello` 接口；
4. 运行 `./mvnw test` 通过启动测试，启动应用并验证 `/api/hello` 与 `/actuator/health`；
5. 更新根目录 README 的运行说明，整理截图与文档。

## 工程结构

```text
monolith/
├── pom.xml                      # Maven 工程描述（Spring Boot 4.0.8 / Java 25）
├── mvnw / mvnw.cmd              # Maven Wrapper，无需预装 Maven
├── .mvn/wrapper/                # Wrapper 配置（Maven 3.9.16）
└── src/
    ├── main/
    │   ├── java/com/zjgsu/linkunze/
    │   │   ├── CampustradeMonolithApplication.java   # 启动类
    │   │   └── HelloController.java                  # GET /api/hello
    │   └── resources/
    │       └── application.yml                       # 统一配置文件
    └── test/java/com/zjgsu/linkunze/
        └── CampustradeMonolithApplicationTests.java  # @SpringBootTest 启动测试
```

> 说明：Group 与包名均为 `com.zjgsu.linkunze`（姓名拼音缩写 linkunze，全小写），符合作业要求。

## 关键配置说明（application.yml）

- `server.port: 8080`：默认端口 8080（沙箱验证时因 8080 被占用，临时用 `--server.port=8081` 启动验证，工程配置保持 8080 不变）；
- `management.endpoints.web.exposure.include: health`：仅对外暴露健康检查端点；
- `management.health.diskspace.enabled: false`：关闭磁盘空间检查。原因：本周无磁盘持久化需求，且部分容器/挂载文件系统会误报可用空间为 0 导致健康状态 DOWN，关闭后健康检查只反映应用自身状态。

## 运行与验证记录

### 1. 启动测试（./mvnw test）

```bash
cd monolith
./mvnw test
```

结果（可重复通过）。以下为本人 Windows 机器（cmd，`C:\Users\linkunze\Desktop\weifuwu\microservices-practice-2401110736\monolith`）上的实际运行记录执行：

```text
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.107 s -- in com.zjgsu.linkunze.CampustradeMonolithApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  3.759 s
[INFO] Finished at: 2026-10-07T22:26:56+08:00
```

`contextLoads()` 验证 Spring 应用上下文可以正常加载，即启动类、配置与依赖装配正确。测试已连续多次执行均通过，可重复运行。

### 2. 启动应用并验证接口

```bash
./mvnw spring-boot:run
```

启动日志出现 `Started CampustradeMonolithApplication in 1.242 seconds`，Tomcat 监听 8080 端口（本地 Windows 验证，日志见 `screenshots/03-app-startup.png`）。

`GET /api/hello` 响应（200，浏览器实际访问见 `screenshots/04-api-hello.png`）：

```json
{"project":"CampusTrade 校园二手交易平台","message":"Hello from CampusTrade monolith! 应用启动成功。"}
```

`GET /actuator/health` 响应（200）：

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

## 

## 本周完成内容汇总

- [x] 选题衔接：`docs/project-proposal.md` 完成，优先业务场景为「发布商品 → 浏览下单 → 确认成交」，核心模型为商品（Item）与订单（Order）；
- [x] 工程与配置：`monolith/` 下同一 Maven 工程包含 `pom.xml`（Spring Boot 4.0.8、Java 25）与 `src/`，含 Maven Wrapper，配置统一为 `application.yml`；
- [x] 运行验证：`/api/hello` 返回项目名称与问候消息，`/actuator/health` 返回 `{"status":"UP"}`；
- [x] 自动化测试：`CampustradeMonolithApplicationTests.contextLoads` 通过，`./mvnw test` 可重复执行（BUILD SUCCESS）；
- [x] README 已补充运行说明（环境要求、启动/测试命令、访问地址、未实现的业务能力）。

## 问题记录

1. **沙箱端口冲突**：验证环境 8080 被占用导致应用无法绑定，通过命令行参数 `--server.port=8081` 临时规避，工程默认端口仍为 8080，本地无需任何修改。
2. **健康检查误报 DOWN**：排查后发现是验证环境的挂载文件系统报告可用磁盘为 0，磁盘空间健康指标误判；本地机器不存在该问题。为保证健康检查语义准确（反映应用自身状态），在 `application.yml` 中关闭了 diskspace 指标并注释说明。


