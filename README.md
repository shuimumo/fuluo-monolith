# 福洛社区 · 单体（冻结维护）

> **本仓库已冻结**：功能与 `cloud` 仓库对齐（含通知中心与 `GET /search`），只接受安全与紧急缺陷修复。
> 新功能一律在 `cloud` 仓库开发，路线图见 `cloud/docs/roadmap.md`。

## 结构

单模块 Spring Boot 3.3.8 应用，包路径 `com.shuimumu.fuluogame`：

```
src/main/java/.../{config,controller,service,mapper,entity,dto,vo,mq,task,utils,constants}
src/main/resources/{application.yaml,mapper,lua,schema.sql}
```

## 构建与运行

```bash
mvn -o clean package -DskipTests
java -jar target/FuluoGame-3.3.8.jar
```

## 本地依赖

MySQL 8、Redis、RabbitMQ、Elasticsearch 8.x、**Canal**（帖子/评论审核通过后的缓存、ES、收件箱扇出依赖 binlog → `canal.cdc`）。

> 首次启动前：RabbitMQ 上不能存在同名但参数不同的旧队列（业务队列均已挂 DLX），否则会报 `PRECONDITION_FAILED`。

## 配置与数据库

- 配置：`src/main/resources/application.yaml` **不纳入版本控制**（含明文密钥），模板见 `application.example.yaml`。
- 建表：`src/main/resources/schema.sql`（含 `notification` 表）。

## 与 cloud 的关系

- 本仓库对应 cloud 的功能快照；两边的接口契约（`Result`、`PageVO`、`/notifications/**`、`/search`）一致，前端可共用。
- 差异属结构性的：单体保留 Canal 扇出、无网关/独立通知服务/独立搜索服务、无服务间 Feign。

## 提交规范

提交信息遵循 `<type>(<scope>): <描述>`，仓库启用了 `.githooks/commit-msg` 校验（`core.hooksPath=.githooks`）。
详见 [`docs/commit-convention.md`](docs/commit-convention.md)。
