# peach_job 数据库脚本（PostgreSQL）

## 文件说明

| 文件 | 连接库 | 说明 |
|------|--------|------|
| `init_table.sql` | `peach_job` | 业务表 `job_task` / `job_log` + Quartz `QRTZ_*` 表与索引（破坏性重置） |
| `init_data.sql` | `peach_job` | 种子数据（当前无默认演示任务，可留空执行） |

## 执行顺序

1. 手动创建数据库 `peach_job`（UTF8）。
2. 连接 `peach_job` 后依次执行 `init_table.sql`、`init_data.sql`。

```powershell
# 示例（按环境修改 -h、-U、密码）
$env:PGPASSWORD = "postgres"
psql -U postgres -h 192.168.99.100 -d peach_job -v ON_ERROR_STOP=1 -f src\main\resources\sql\init_table.sql -f src\main\resources\sql\init_data.sql
```

## 与 Java 实体对齐

- `job_task` ↔ `JobTask`
- `job_log` ↔ `JobLog`
- `QRTZ_*` ↔ Spring Quartz JDBC JobStore（`PostgreSQLDelegate`，表前缀 `QRTZ_`）
