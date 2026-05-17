# peach-job-service

基于 **Quartz JDBC + PostgreSQL + MyBatis** 的定时任务服务：任务配置存 `job_task`，执行日志存 `job_execution_log`；运行时由单一 **`HttpJob`** 执行 **GET** 请求——**INTERNAL** 走 **Nacos + Spring Cloud LoadBalancer** 访问 `http://{serviceId}`；**EXTERNAL** 直连配置的基址。管理端 API 目录同样经负载均衡直连各服务（不经网关）。

## 建议启动顺序（中文）

1. **PostgreSQL**：创建库并执行初始化脚本（见下）；已有库请按需执行增量补丁。
2. **Nacos**：与其它微服务一致，保证目标服务已注册。
3. **业务服务**（被调用的目标）：需已注册且暴露 `GET {adminPrefix}/apis/type/admin` 等 admin 接口。
4. **peach-job-service**：启动时将 `job_task` 全量同步到 Quartz。

## 数据库初始化（PostgreSQL）

脚本位于 `src/main/resources/sql/`，详见同目录 `README.md`。

### 全新安装（推荐）

在 **PowerShell** 中进入本模块目录后执行（按环境修改 `-h`、`-U`、密码；需已手动创建 `peach_job` 库）：

```powershell
cd d:\eclipse-workspace\peach-job-service

# 可选：非交互式传入密码
$env:PGPASSWORD = "postgres"

# 连接 peach_job，依次执行表结构 + 种子脚本
psql -U postgres -h 192.168.99.100 -d peach_job -v ON_ERROR_STOP=1 -f src\main\resources\sql\init_table.sql -f src\main\resources\sql\init_data.sql
```

Linux / macOS 将路径改为正斜杠即可：

```bash
psql -U postgres -h 192.168.99.100 -d peach_job -v ON_ERROR_STOP=1 -f src/main/resources/sql/init_table.sql -f src/main/resources/sql/init_data.sql
```

### 与 application.yml 对齐

默认 JDBC URL 已指向 `peach_job`：

`jdbc:postgresql://192.168.99.100:5432/peach_job?currentSchema=public`（环境变量 `JOB_DB_URL` 可覆盖）。

### 本地 PostgreSQL（可选）

仓库根目录仅有 `docker-compose.redis.yml`，无 Postgres 服务。可临时用官方镜像：

```powershell
docker run -d --name peach-postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:16-alpine
```

创建库后执行上述 `00`、`01` 脚本（`-h 127.0.0.1`）。

## 环境变量 / 配置项

| 含义 | 配置键 / 环境变量 | 默认示例 |
|------|-------------------|----------|
| 数据源 URL | `JOB_DB_URL` / `spring.datasource.url` | `jdbc:postgresql://192.168.99.100:5432/peach_job?currentSchema=public` |
| 数据库用户 | `JOB_DB_USERNAME` | `postgres` |
| 数据库密码 | `JOB_DB_PASSWORD` | `postgres` |
| 管理端路径前缀（拼到直连 URL） | `PEACH_JOB_ADMIN_API_PATH_PREFIX` / `peach.job.admin-api-path-prefix` | `/admin` |
| 可选：定时 HTTP 默认 Authorization | `PEACH_JOB_HTTP_AUTHORIZATION` / `peach.job.http-authorization` | 空 |
| Nacos | 与现有服务相同 `NACOS_*` | 见 `application.yml` |

> `peach.job.admin-base-url` 已废弃保留，执行与 API 目录不再经网关。

## 依赖说明

- **`spring-cloud-starter-loadbalancer`**：与 `spring-cloud-starter-alibaba-nacos-discovery` 配合，为 `RestTemplate` 提供 `http://{serviceId}/...` 解析与负载均衡。

## 管理端 API 路径（`peach.api.context` 开启时均带 `/admin` 前缀）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/job/tasks` | 任务列表 |
| GET | `/admin/job/tasks/{id}` | 任务详情 |
| GET | `/admin/job/tasks/{id}/logs?limit=` | 执行日志 |
| POST | `/admin/job/tasks` | 新建 |
| PUT | `/admin/job/tasks/{id}` | 更新 |
| DELETE | `/admin/job/tasks/{id}` | 删除 |
| POST | `/admin/job/tasks/{id}/pause` | 暂停调度 |
| POST | `/admin/job/tasks/{id}/resume` | 恢复调度 |
| POST | `/admin/job/tasks/{id}/trigger` | 立即触发 |
| POST | `/admin/job/scheduler/refresh` | 全量刷新 Quartz |
| GET | `/admin/job/registry/admin-apis?serviceId=&method=&keyword=` | 按服务拉取 admin API（LB 直连） |

## 已知 TODO / 限制

- 调用需鉴权的 admin 接口时，请在任务 `headers` 或 `peach.job.http-authorization` 中配置；生产建议服务账号 / mTLS。
- Quartz 集群与多实例 `isClustered` 未默认开启，可按需调整 `spring.quartz.properties`。
- Cron 由管理端表单维护；五段式在前端会补 `0` 秒前缀以贴近 Quartz。
