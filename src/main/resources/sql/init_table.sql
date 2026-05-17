-- 创作日期：2026-05-17，作者：leiyangjun — PostgreSQL 12+ 定时任务库 DDL（与 application 中 DataSource 一致）
-- 执行前请在目标库 CREATE DATABASE peach_job 并 \c 到目标库；执行顺序：先本脚本，再 init_data.sql（若有种子）
-- 策略：DROP TABLE IF EXISTS ... CASCADE 后 CREATE TABLE（破坏性重置，适合开发/空库初始化）
-- 主键策略：短雪花 BIGINT，与 IdUtil.shortSnowId() 一致（非 BIGSERIAL）

SET client_encoding = 'UTF8';
SET search_path = public;

-- ========== 删表（Quartz 表依赖顺序；CASCADE 避免残留外键）==========
DROP TABLE IF EXISTS public.QRTZ_FIRED_TRIGGERS CASCADE;
DROP TABLE IF EXISTS public.QRTZ_PAUSED_TRIGGER_GRPS CASCADE;
DROP TABLE IF EXISTS public.QRTZ_CALENDARS CASCADE;
DROP TABLE IF EXISTS public.QRTZ_BLOB_TRIGGERS CASCADE;
DROP TABLE IF EXISTS public.QRTZ_SIMPROP_TRIGGERS CASCADE;
DROP TABLE IF EXISTS public.QRTZ_CRON_TRIGGERS CASCADE;
DROP TABLE IF EXISTS public.QRTZ_SIMPLE_TRIGGERS CASCADE;
DROP TABLE IF EXISTS public.QRTZ_TRIGGERS CASCADE;
DROP TABLE IF EXISTS public.QRTZ_JOB_DETAILS CASCADE;
DROP TABLE IF EXISTS public.QRTZ_SCHEDULER_STATE CASCADE;
DROP TABLE IF EXISTS public.QRTZ_LOCKS CASCADE;
DROP TABLE IF EXISTS public.job_log CASCADE;
DROP TABLE IF EXISTS public.job_task CASCADE;

-- ========== 业务表 ==========
CREATE TABLE public.job_task (
    id                  BIGINT         NOT NULL,
    job_name            VARCHAR(50)    NOT NULL,
    job_group           VARCHAR(50)    NOT NULL,
    job_description     VARCHAR(1000)           NULL,
    job_cron_expression VARCHAR(120)   NOT NULL,
    retry_max           INT            NOT NULL DEFAULT 0,
    retry_interval_ms   INT                     NULL,
    job_type            INT            NOT NULL DEFAULT 0,
    http_method         VARCHAR(10)    NOT NULL DEFAULT 'GET',
    url_path            VARCHAR(100)   NOT NULL,
    service_name        VARCHAR(20)             NULL,
    external_base_url   VARCHAR(200)            NULL,
    headers             VARCHAR(2000)           NULL,
    timeout_ms          INT            NOT NULL DEFAULT 10000,
    valid               SMALLINT       NOT NULL DEFAULT 1,
    creator             BIGINT                  NULL,
    editor              BIGINT                  NULL,
    create_time         TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    edit_time           TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_job_task PRIMARY KEY (id),
    CONSTRAINT ck_job_task_valid CHECK (valid IN (0, 1))
);

COMMENT ON TABLE public.job_task IS '定时任务定义表：存储 Quartz 任务名/组、cron、重试、内外部调用方式及目标 API 摘要；供调度器注册与 HttpJob 执行。';
COMMENT ON COLUMN public.job_task.id IS '主键：短雪花 64 位，对应 Java long';
COMMENT ON COLUMN public.job_task.job_name IS 'Quartz JobDetail 名称，与 job_group 组成业务内唯一调度键。';
COMMENT ON COLUMN public.job_task.job_group IS 'Quartz JobDetail 分组，与 job_name 组成业务内唯一调度键。';
COMMENT ON COLUMN public.job_task.job_description IS '任务说明，便于运维与列表展示。';
COMMENT ON COLUMN public.job_task.job_cron_expression IS 'Quartz Cron 表达式（建议 6 域或按统一规范）。';
COMMENT ON COLUMN public.job_task.retry_max IS '失败后的最大重试次数（不含首次执行）。';
COMMENT ON COLUMN public.job_task.retry_interval_ms IS '重试间隔毫秒；为空表示由应用默认策略处理。';
COMMENT ON COLUMN public.job_task.job_type IS '任务类型：0=平台内(注册中心+LB)，1=外部 HTTP 等；枚举以应用代码为准。';
COMMENT ON COLUMN public.job_task.http_method IS 'HTTP 方法；当前产品约束多为 GET。';
COMMENT ON COLUMN public.job_task.url_path IS '相对路径或外部完整路径片段；与 service_name / external_base_url 组合由应用解析。';
COMMENT ON COLUMN public.job_task.service_name IS '平台内目标微服务注册名；外部任务可为空。';
COMMENT ON COLUMN public.job_task.external_base_url IS '外部调用基址；平台内任务可为空。';
COMMENT ON COLUMN public.job_task.headers IS '外部请求头 JSON 或约定格式文本。';
COMMENT ON COLUMN public.job_task.timeout_ms IS '单次 HTTP 调用超时毫秒。';
COMMENT ON COLUMN public.job_task.valid IS '有效标记：1=有效，0=无效（软停用）。';
COMMENT ON COLUMN public.job_task.creator IS '创建人用户 ID：短雪花 64 位。';
COMMENT ON COLUMN public.job_task.editor IS '最后修改人用户 ID：短雪花 64 位。';
COMMENT ON COLUMN public.job_task.create_time IS '创建时间（带时区）。';
COMMENT ON COLUMN public.job_task.edit_time IS '最后修改时间（带时区）。';

CREATE UNIQUE INDEX uq_job_task_name_group ON public.job_task (job_name, job_group);
CREATE INDEX idx_job_task_valid_type ON public.job_task (valid, job_type);
CREATE INDEX idx_job_task_service_name ON public.job_task (service_name) WHERE service_name IS NOT NULL;
CREATE INDEX idx_job_task_edit_time ON public.job_task (edit_time DESC);

CREATE TABLE public.job_log (
    id              BIGINT         NOT NULL,
    job_id          BIGINT         NOT NULL,
    job_description VARCHAR(100)            NULL,
    job_api         VARCHAR(200)   NOT NULL,
    job_name        VARCHAR(64)    NOT NULL,
    job_group       VARCHAR(64)    NOT NULL,
    exe_time        INT            NOT NULL,
    httpstatus      INT                     NULL,
    response_msg    VARCHAR(200)            NULL,
    create_time     TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_job_log PRIMARY KEY (id)
);

COMMENT ON TABLE public.job_log IS '定时任务执行日志：记录某次触发对应 API、HTTP 状态与简要响应信息，便于排障与审计。';
COMMENT ON COLUMN public.job_log.id IS '主键：短雪花 64 位，对应 Java long';
COMMENT ON COLUMN public.job_log.job_id IS '关联 job_task.id；BIGINT 外键语义（非 BIGSERIAL）';
COMMENT ON COLUMN public.job_log.job_description IS '任务描述快照或冗余展示字段，便于日志列表不联表也能看懂。';
COMMENT ON COLUMN public.job_log.job_api IS '本次实际调用 API 快照（如 METHOD + URL 或完整 URL）。';
COMMENT ON COLUMN public.job_log.job_name IS '本次执行对应的 Quartz job_name 快照。';
COMMENT ON COLUMN public.job_log.job_group IS '本次执行对应的 Quartz job_group 快照。';
COMMENT ON COLUMN public.job_log.exe_time IS '本次执行对应的 API 耗时（毫秒）。';
COMMENT ON COLUMN public.job_log.httpstatus IS 'HTTP 状态码；非 HTTP 错误可用约定值（如 -1）由应用定义。';
COMMENT ON COLUMN public.job_log.response_msg IS '响应摘要或错误信息，截断存储。';
COMMENT ON COLUMN public.job_log.create_time IS '日志产生时间（带时区）。';

CREATE INDEX idx_job_log_job_id_time ON public.job_log (job_id, create_time DESC);
CREATE INDEX idx_job_log_create_time ON public.job_log (create_time DESC);
CREATE INDEX idx_job_log_job_name_group_time ON public.job_log (job_name, job_group, create_time DESC);
CREATE INDEX idx_job_log_httpstatus_time ON public.job_log (httpstatus, create_time DESC);

-- ========== Quartz JDBC（PostgreSQL），来源 quartz 2.3.x tables_postgres.sql ==========
CREATE TABLE public.QRTZ_JOB_DETAILS (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    JOB_NAME          VARCHAR(200) NOT NULL,
    JOB_GROUP         VARCHAR(200) NOT NULL,
    DESCRIPTION       VARCHAR(250)          NULL,
    JOB_CLASS_NAME    VARCHAR(250) NOT NULL,
    IS_DURABLE        BOOL         NOT NULL,
    IS_NONCONCURRENT  BOOL         NOT NULL,
    IS_UPDATE_DATA    BOOL         NOT NULL,
    REQUESTS_RECOVERY BOOL         NOT NULL,
    JOB_DATA          BYTEA                 NULL,
    PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE public.QRTZ_TRIGGERS (
    SCHED_NAME     VARCHAR(120) NOT NULL,
    TRIGGER_NAME   VARCHAR(200) NOT NULL,
    TRIGGER_GROUP  VARCHAR(200) NOT NULL,
    JOB_NAME       VARCHAR(200) NOT NULL,
    JOB_GROUP      VARCHAR(200) NOT NULL,
    DESCRIPTION    VARCHAR(250)          NULL,
    NEXT_FIRE_TIME BIGINT                NULL,
    PREV_FIRE_TIME BIGINT                NULL,
    PRIORITY       INTEGER               NULL,
    TRIGGER_STATE  VARCHAR(16)  NOT NULL,
    TRIGGER_TYPE   VARCHAR(8)   NOT NULL,
    START_TIME     BIGINT       NOT NULL,
    END_TIME       BIGINT                NULL,
    CALENDAR_NAME  VARCHAR(200)          NULL,
    MISFIRE_INSTR  SMALLINT              NULL,
    JOB_DATA       BYTEA                 NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
        REFERENCES public.QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE public.QRTZ_SIMPLE_TRIGGERS (
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(200) NOT NULL,
    TRIGGER_GROUP   VARCHAR(200) NOT NULL,
    REPEAT_COUNT    BIGINT       NOT NULL,
    REPEAT_INTERVAL BIGINT       NOT NULL,
    TIMES_TRIGGERED BIGINT       NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES public.QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE public.QRTZ_CRON_TRIGGERS (
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(200) NOT NULL,
    TRIGGER_GROUP   VARCHAR(200) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID    VARCHAR(80),
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES public.QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE public.QRTZ_SIMPROP_TRIGGERS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_NAME  VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    STR_PROP_1    VARCHAR(512)          NULL,
    STR_PROP_2    VARCHAR(512)          NULL,
    STR_PROP_3    VARCHAR(512)          NULL,
    INT_PROP_1    INT                   NULL,
    INT_PROP_2    INT                   NULL,
    LONG_PROP_1   BIGINT                NULL,
    LONG_PROP_2   BIGINT                NULL,
    DEC_PROP_1    NUMERIC(13, 4)        NULL,
    DEC_PROP_2    NUMERIC(13, 4)        NULL,
    BOOL_PROP_1   BOOL                  NULL,
    BOOL_PROP_2   BOOL                  NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES public.QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE public.QRTZ_BLOB_TRIGGERS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_NAME  VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    BLOB_DATA     BYTEA                 NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES public.QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE public.QRTZ_CALENDARS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    CALENDAR_NAME VARCHAR(200) NOT NULL,
    CALENDAR      BYTEA        NOT NULL,
    PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
);

CREATE TABLE public.QRTZ_PAUSED_TRIGGER_GRPS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
);

CREATE TABLE public.QRTZ_FIRED_TRIGGERS (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    ENTRY_ID          VARCHAR(95)  NOT NULL,
    TRIGGER_NAME      VARCHAR(200) NOT NULL,
    TRIGGER_GROUP     VARCHAR(200) NOT NULL,
    INSTANCE_NAME     VARCHAR(200) NOT NULL,
    FIRED_TIME        BIGINT       NOT NULL,
    SCHED_TIME        BIGINT       NOT NULL,
    PRIORITY          INTEGER      NOT NULL,
    STATE             VARCHAR(16)  NOT NULL,
    JOB_NAME          VARCHAR(200)          NULL,
    JOB_GROUP         VARCHAR(200)          NULL,
    IS_NONCONCURRENT  BOOL                  NULL,
    REQUESTS_RECOVERY BOOL                  NULL,
    PRIMARY KEY (SCHED_NAME, ENTRY_ID)
);

CREATE TABLE public.QRTZ_SCHEDULER_STATE (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    INSTANCE_NAME     VARCHAR(200) NOT NULL,
    LAST_CHECKIN_TIME BIGINT       NOT NULL,
    CHECKIN_INTERVAL  BIGINT       NOT NULL,
    PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
);

CREATE TABLE public.QRTZ_LOCKS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME  VARCHAR(40)  NOT NULL,
    PRIMARY KEY (SCHED_NAME, LOCK_NAME)
);

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY ON public.QRTZ_JOB_DETAILS (SCHED_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_J_GRP ON public.QRTZ_JOB_DETAILS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_J ON public.QRTZ_TRIGGERS (SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_JG ON public.QRTZ_TRIGGERS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_C ON public.QRTZ_TRIGGERS (SCHED_NAME, CALENDAR_NAME);
CREATE INDEX IDX_QRTZ_T_G ON public.QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_T_STATE ON public.QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_STATE ON public.QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_G_STATE ON public.QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NEXT_FIRE_TIME ON public.QRTZ_TRIGGERS (SCHED_NAME, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST ON public.QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_MISFIRE ON public.QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE ON public.QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE_GRP ON public.QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_FT_TRIG_INST_NAME ON public.QRTZ_FIRED_TRIGGERS (SCHED_NAME, INSTANCE_NAME);
CREATE INDEX IDX_QRTZ_FT_INST_JOB_REQ_RCVRY ON public.QRTZ_FIRED_TRIGGERS (SCHED_NAME, INSTANCE_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_FT_J_G ON public.QRTZ_FIRED_TRIGGERS (SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_JG ON public.QRTZ_FIRED_TRIGGERS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_T_G ON public.QRTZ_FIRED_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_FT_TG ON public.QRTZ_FIRED_TRIGGERS (SCHED_NAME, TRIGGER_GROUP);
