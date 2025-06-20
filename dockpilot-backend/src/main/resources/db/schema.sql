-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    level TEXT NOT NULL DEFAULT 'free' CHECK (level IN ('free', 'pro')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入默认管理员用户
INSERT OR IGNORE INTO users (username, password, level)
SELECT 'admin',
       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
       'free'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

-- 系统设置表
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT
);


-- 日志表
CREATE TABLE IF NOT EXISTS logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL,                -- 日志类型：OPERATION-操作日志，SYSTEM-系统日志
    level TEXT NOT NULL,               -- 日志级别：INFO, ERROR, WARN
    content TEXT NOT NULL,             -- 日志内容
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP -- 创建时间
);

-- 创建日志表索引
CREATE INDEX IF NOT EXISTS idx_logs_type ON logs(type);
CREATE INDEX IF NOT EXISTS idx_logs_level ON logs(level);
CREATE INDEX IF NOT EXISTS idx_logs_create_time ON logs(create_time);

-- ======================================
-- 分类表和Web应用表
-- ======================================

-- 分类表 (categories)
-- 用于管理应用分类
CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,           -- 分类名称（唯一）
    sort_order INTEGER DEFAULT 0,               -- 分类排序（数字越小越靠前）
    created_at TEXT DEFAULT (datetime('now')),  -- 创建时间
    updated_at TEXT DEFAULT (datetime('now'))   -- 更新时间
);

-- Web应用表 (web_servers)
-- 存储具体的应用信息
CREATE TABLE IF NOT EXISTS web_servers (
    id TEXT PRIMARY KEY,                        -- UUID 主键
    name VARCHAR(100) NOT NULL,                 -- 应用名称
    icon TEXT,                                  -- 图标URL
    internal_url TEXT,                          -- 内网访问地址
    external_url TEXT,                          -- 外网访问地址
    description TEXT,                           -- 描述信息
    category_id INTEGER NOT NULL,               -- 分类ID（外键）
    item_sort INTEGER DEFAULT 0,                -- 应用排序
    bg_color VARCHAR(200) DEFAULT 'rgba(255, 255, 255, 0.15)', -- 背景色
    card_type VARCHAR(20) DEFAULT 'normal',     -- 卡片类型（normal、text）
    icon_type VARCHAR(20) DEFAULT 'image',      -- 图标类型（image、text、icon）
    open_type VARCHAR(20) DEFAULT 'new',        -- 打开方式（current、new）
    is_favorite INTEGER DEFAULT 0,              -- 是否收藏（0=否，1=是）
    created_at TEXT DEFAULT (datetime('now')), -- 创建时间
    updated_at TEXT DEFAULT (datetime('now')), -- 更新时间

    -- 外键约束
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- 创建索引优化查询性能
CREATE INDEX IF NOT EXISTS idx_categories_sort_order ON categories(sort_order);
CREATE INDEX IF NOT EXISTS idx_web_servers_category_id ON web_servers(category_id);
CREATE INDEX IF NOT EXISTS idx_web_servers_item_sort ON web_servers(item_sort);
CREATE INDEX IF NOT EXISTS idx_web_servers_category_sort ON web_servers(category_id, item_sort);

-- 容器信息表
CREATE TABLE IF NOT EXISTS container_info (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    container_id TEXT NOT NULL,        -- Docker 容器 ID
    name TEXT NOT NULL,                -- 容器名称
    image TEXT NOT NULL,               -- 容器镜像
    status TEXT,                       -- 容器状态（running, exited, created 等）
    operation_status TEXT,             -- 例如 creating, updating 等状态
    last_error TEXT,                   -- 启动失败时的错误描述
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP, -- 更新时间
    need_update INTEGER DEFAULT 0,     -- 是否需要更新
    icon_url TEXT DEFAULT NULL,        -- 容器图标URL
    web_url TEXT                       -- Web访问URL
);

-- 镜像状态表
CREATE TABLE IF NOT EXISTS image_status (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,                -- 仓库名，如 jxxghp/moviepilot-v2
    tag TEXT NOT NULL,                 -- 镜像标签，如 latest、v1.0.0
    local_create_time TEXT,            -- 本地镜像创建时间
    remote_create_time TEXT,           -- 远程镜像创建时间
    need_update INTEGER DEFAULT 0,     -- 0 表示 false，1 表示 true
    last_checked TEXT DEFAULT (datetime('now')), -- 检查时间，ISO8601格式
    created_at TEXT DEFAULT (datetime('now')),   -- 创建时间
    updated_at TEXT DEFAULT (datetime('now')),   -- 更新时间
    image_id TEXT,                     -- 镜像ID
    pulling INTEGER DEFAULT 0,         -- 是否正在拉取中
    progress TEXT,                     -- 拉取进度
    UNIQUE (name, tag)
);

-- ======================================
-- 应用中心数据库表
-- ======================================

-- 应用表
CREATE TABLE IF NOT EXISTS applications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,                    -- 应用名称
    description TEXT,                      -- 应用描述
    category TEXT DEFAULT '容器应用',        -- 应用分类
    icon_url TEXT,                         -- 应用图标URL
    yaml_content TEXT NOT NULL,            -- YAML配置内容 (完整的应用配置)
    file_hash TEXT UNIQUE,                 -- 配置文件哈希值 (用于去重检查)
    env_vars TEXT,                         -- 用户安装时填写的变量
    created_at TEXT DEFAULT (datetime('now')),  -- 创建时间
    updated_at TEXT DEFAULT (datetime('now'))   -- 更新时间
);

-- 创建应用表索引 (只保留有效字段的索引)
CREATE INDEX IF NOT EXISTS idx_applications_category ON applications(category);
CREATE INDEX IF NOT EXISTS idx_applications_hash ON applications(file_hash);

-- -- ======================================
-- -- 插入默认分类数据
-- -- ======================================
--
-- INSERT OR IGNORE INTO categories (id, name, sort_order) VALUES
-- (1, '开发工具', 1);
--
--
-- -- ======================================
-- -- 插入示例应用数据
-- -- ======================================
--
-- INSERT OR IGNORE INTO web_servers (
--     id, name, icon, external_url, description, category_id, item_sort,
--     bg_color, card_type, icon_type, open_type
-- ) VALUES
-- ('dev-001', 'DockPilot', 'https://github.githubassets.com/favicons/favicon.svg',
--  'https://github.com/kidoneself/DockPilot', 'docker管理工具', 1, 1,
--  'rgba(36, 41, 47, 0.8)', 'normal', 'image', 'new');



