# AGENTS.md

API Grab（接口数据采集入库系统）— 配置化 REST 接口采集/入库平台。Spring Boot 单体后端 + Vue 2 SPA 前端。

功能与架构的权威说明在 `docs/apigrab_function_document.md`，改动行为前先读它。本文件只记录代码/构建中不易从文件名推断、且容易踩坑的事实。

## 模块结构（Maven 多模块，聚合于根 `pom.xml`）

依赖方向：`admin → core → persistence → common`。

- `apigrab-common` — 统一响应体、异常体系、通用工具。
- `apigrab-persistence` — MyBatis 实体/枚举/TypeHandler/DAO；XML 映射在 `src/main/resources/mapper/`。
- `apigrab-core` — 采集引擎：HTTP、解析、认证、字段映射、动态目标数据源、告警。
- `apigrab-admin` — 可启动应用：Controller、Service、Quartz 调度、Flyway 迁移、`application.yml`。主类 `com.gitee.apigrab.admin.ApigrabApplication`，端口 5880。
- `apigrab-web-ui` — Vue 2 + Element UI 前端，**不是 Maven 模块**，独立构建。

基础包统一为 `com.gitee.apigrab`。

## 工具链与命令

- 目标 Java 8。无 Maven Wrapper，用系统 Maven 3.6+。
- 依赖已缓存，可离线：`mvn -o clean package`。
- 启动后端：`mvn -pl apigrab-admin -am spring-boot:run`（或直接运行 `ApigrabApplication`）。启动依赖元数据库（见下）。
- 测试为纯 JUnit 5 + Mockito，**无** `@SpringBootTest`、无 DB/容器依赖。surefire 只识别 `*Test` / `*Tests` 结尾的类。
- 全部测试：`mvn test`。
- 单个测试：`mvn -pl <module> -am test -Dtest=<ClassName> -DfailIfNoTests=false`（`-am` 时若上游模块没有匹配测试，必须加 `-DfailIfNoTests=false`，否则构建失败）。

前端（`apigrab-web-ui`，npm）：

- `npm run serve` — 开发服务器在 **8081**，`/api` 代理到 `http://localhost:5880`。
- `npm run build` — 产物到 `dist/`。
- **没有 `npm test` 脚本**。单元测试用 Node 内置 runner：`node --test src/utils/*.test.js`（需 Node ≥ 20.19 / 22；测试文件与源码同目录，命名 `*.test.js`，用 `node:test` + `node:assert`）。

## 运行时 / 配置要点

- 元数据库：MySQL，配置在 `application.yml`（URL 带 `createDatabaseIfNotExist=true`，会自建库）。
- Flyway 按激活的 Spring Profile 从 `classpath:db/mysql`（mysql 配置）或 `classpath:db/h2`（h2 配置）执行迁移（脚本 `V1_0_0__init_schema.sql`），历史表 `FLYWAY_SCHEMA_HISTORY`。
- Quartz 用 JDBC JobStore 且 `initialize-schema: always`（表在元数据库自动创建），集群模式。
- `apigrab.http.insecure-tls: true` 默认开启 → **所有**采集请求跳过 TLS 证书/主机名校验。这是为内网自签证书接口有意设置，勿当 bug “修复”。
- 目标库（MySQL/Oracle/SQL Server/PostgreSQL + 达梦/人大金仓/神通/GBase8A/瀚高/openGauss/OceanBase/UXDB）连接池在运行期按需动态创建，应用启动不依赖它们可达。国产驱动不进 Maven classpath，放在 `drivers/<类型名小写>/<版本>/*.jar`（由 `apigrab.datasource.drivers-path` 配置根目录），经数据源的 `driverPath` 用隔离 `URLClassLoader` 加载；类型清单以后端 `DbType` 枚举为准，前端由 `/api/datasource-meta/db-types` 与 `/drivers` 拉取。
- 鉴权：除 `/api/auth/login` 外所有 `/api/**` 需 Bearer 令牌（`WebMvcConfig` + `AuthInterceptor`）。

## 前端打包

后端直接提供 `apigrab-admin/src/main/resources/dist/` 下**已提交**的构建产物（内容等同 `apigrab-web-ui/dist/`）。`web-ui` 单独构建后需**手工**把 `dist` 拷进 admin 的 resources——仓库里没有对应的拷贝脚本，别以为改前端源码后后端会自动更新。

## 约定

- 接口统一返回 `{code, message, data}`，业务异常按语义映射 HTTP 状态码。

<!-- OPENFLOW DOCS GUIDE:BEGIN -->
## 文档阅读指南

以下目录均为按需阅读，不要求一次性通读；只在对应工作阶段或问题需要时再打开相关文件。

- `docs/current/requirements/`：当前需求事实与验收要点，优先作为“要做什么”的依据。
- `docs/current/design/`：当前设计事实、模块边界与实现约束，优先作为“怎么做”的依据。
- `docs/current/spec/`：当前规格说明与可执行规范，优先用于校准行为细节与接口约定。
- `docs/current/workflow/`：当前流程规则与协作步骤，优先用于确认文档与执行顺序。
- `docs/decisions/`：跨版本的架构与治理决策，只有在需要理解长期原则或取舍时才阅读。
- `docs/changes/`：进行中的变更工作区，仅在处理某个具体 feature、需求或设计任务时阅读。
- `docs/archive/`：已完成并冻结的历史归档，仅在追溯背景、对比演进或审计时阅读。
- `docs/current/workflow/ai-reflection/`：AI 自我反思记录与纠正规则，当该目录存在相关内容时，按需阅读与当前任务相关的纠正规则或反思记录，避免重复犯错。

阅读原则：先读当前、再读相关、最后才读历史；先看最小必要集合，再按需要扩展。
<!-- OPENFLOW DOCS GUIDE:END -->
