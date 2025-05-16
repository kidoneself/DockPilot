CREATE TABLE IF NOT EXISTS users
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    username
    TEXT
    NOT
    NULL
    UNIQUE,
    password
    TEXT
    NOT
    NULL,
    level
    TEXT
    NOT
    NULL
    DEFAULT
    'free'
    CHECK (
    level
    IN
(
    'free',
    'pro'
)),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

INSERT
OR IGNORE INTO users (username, password, level)
SELECT 'admin',
       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
       'free' WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');
-- 镜像状态表
CREATE TABLE IF NOT EXISTS image_status
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    name
    TEXT
    NOT
    NULL, -- 仓库名，如 jxxghp/moviepilot-v2
    tag
    TEXT
    NOT
    NULL, -- 镜像标签，如 latest、v1.0.0
    local_create_time
    TEXT, -- 本地镜像创建时间
    remote_create_time
    TEXT, -- 远程镜像创建时间
    need_update
    INTEGER
    DEFAULT
    0,    -- 0 表示 false，1 表示 true
    last_checked
    TEXT
    DEFAULT (
    datetime
(
    'now'
)), -- 检查时间，ISO8601格式
    created_at TEXT DEFAULT
(
    datetime
(
    'now'
)),
    updated_at TEXT DEFAULT
(
    datetime
(
    'now'
)),
    UNIQUE
(
    name,
    tag
)
    );
CREATE TABLE IF NOT EXISTS system_settings
(
    setting_key
    VARCHAR
(
    100
) PRIMARY KEY,
    setting_value TEXT
    );



CREATE TABLE IF NOT EXISTS application_templates
(
    id
    TEXT
    PRIMARY
    KEY,               -- 主键ID，应用模板的唯一标识
    name
    TEXT
    NOT
    NULL,              -- 应用名称，用于显示
    category
    TEXT,              -- 应用分类，用于分类展示
    version
    TEXT,              -- 应用版本号
    description
    TEXT,              -- 应用描述
    icon_url
    TEXT,              -- 应用图标URL
    template
    TEXT
    NOT
    NULL,              -- 应用模板数据，使用JSON格式存储完整的模板配置
    created_at
    TIMESTAMP
    DEFAULT
    CURRENT_TIMESTAMP, -- 创建时间
    updated_at
    TIMESTAMP
    DEFAULT
    CURRENT_TIMESTAMP, -- 更新时间
    sort_weight
    INTEGER
    DEFAULT
    0                  -- 排序权重
);

CREATE TABLE IF NOT EXISTS logs
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    type
    TEXT
    NOT
    NULL,             -- 日志类型：OPERATION-操作日志，SYSTEM-系统日志
    level
    TEXT
    NOT
    NULL,             -- 日志级别：INFO, ERROR, WARN
    content
    TEXT
    NOT
    NULL,             -- 日志内容
    create_time
    DATETIME
    DEFAULT
    CURRENT_TIMESTAMP -- 创建时间
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_logs_type ON logs(type);
CREATE INDEX IF NOT EXISTS idx_logs_level ON logs(level);
CREATE INDEX IF NOT EXISTS idx_logs_create_time ON logs(create_time);


-- Web服务表
CREATE TABLE IF NOT EXISTS web_servers
(
    id
    TEXT
    PRIMARY
    KEY, -- UUID 主键
    name
    VARCHAR
(
    100
) NOT NULL, -- 服务器名称
    icon TEXT, -- 图标URL
    internal_url TEXT, -- 内网访问地址
    external_url TEXT, -- 外网访问地址
    description TEXT, -- 描述信息
    category TEXT DEFAULT '默认分类', -- 分类名称
    item_sort INTEGER DEFAULT 0, -- 应用排序
    created_at TEXT DEFAULT
(
    datetime
(
    'now'
)), -- 创建时间
    updated_at TEXT DEFAULT
(
    datetime
(
    'now'
)) -- 更新时间
    );


CREATE TABLE IF NOT EXISTS container_info
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    container_id
    TEXT
    NOT
    NULL, -- Docker 容器 ID
    name
    TEXT
    NOT
    NULL, -- 容器名称
    image
    TEXT
    NOT
    NULL, -- 容器镜像
    status
    TEXT, -- 容器状态（running, exited, created 等）
    operation_status
    TEXT, -- 例如 creating, updating 等状态
    last_error
    TEXT, -- 启动失败时的错误描述
    created_at
    DATETIME
    DEFAULT
    CURRENT_TIMESTAMP,
    updated_at
    DATETIME
    DEFAULT
    CURRENT_TIMESTAMP
);

