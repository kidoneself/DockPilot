
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
    -- 主键ID，应用模板的唯一标识
    id
    TEXT
    PRIMARY
    KEY,
    -- 应用名称，用于显示
    name
    TEXT
    NOT
    NULL,
    -- 应用分类，用于分类展示
    category
    TEXT,
    -- 应用版本号
    version
    TEXT,
    -- 应用描述
    description
    TEXT,
    -- 应用图标URL
    icon_url
    TEXT,
    -- 应用模板数据，使用JSON格式存储完整的模板配置
    template
    TEXT
    NOT
    NULL,
    -- 创建时间
    created_at
    TIMESTAMP
    DEFAULT
    CURRENT_TIMESTAMP,
    -- 更新时间
    updated_at
    TIMESTAMP
    DEFAULT
    CURRENT_TIMESTAMP,
    -- 排序权重
    sort_weight
    INTEGER
    DEFAULT
    0

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


-- INSERT INTO application_templates (id, name, category, version, description, icon_url, template, created_at, updated_at, sort_weight) VALUES ('1', '家庭影院', '媒体', '1.0', '一个强大的媒体管理应用', 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg', '{"services":[{"id":"moviepilot","name":"MoviePilot","template":{"name":"naspt-mpv2","image":"ccr.ccs.tencentyun.com/naspt/moviepilot-v2:latest","env":{"MOVIEPILOT_AUTO_UPDATE":"true","ICC2022_UID":"24730","SUPERUSER":"admin","API_TOKEN":"nasptnasptnasptnasptnaspt","AUTO_UPDATE_RESOURCE":"true","LEAVES_UID":"10971","LEAVES_PASSKEY":"e0405a9d0de9e3b112ef78ac3d9c7975","TZ":"Asia/Shanghai","AUTH_SITE":"icc2022,leaves","ICC2022_PASSKEY":"49c421073514d4d981a0cbc4174f4b23","PATH":"/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin","LANG":"C.UTF-8","GPG_KEY":"7169605F62C751356D054A26A821E680E5FA6305","PYTHON_VERSION":"3.12.8","PYTHON_SHA256":"c909157bb25ec114e5869124cc2a9c4a4d4c1e957ca4ff553f1edc692101154e","HOME":"/moviepilot","CONFIG_DIR":"/config","TERM":"xterm","DISPLAY":":987","PUID":"0","PGID":"0","UMASK":"000","PORT":"3001","NGINX_PORT":"3000","PROXY_HOST":"http://naspt:naspt@10.10.10.2:7890"},"ports":{"3000/tcp":"{{MP_PORT}}"},"volumes":{"{{DOCKER_PATH}}/naspt-mpv2/config":"/config","{{MEDIA_PATH}}":"/media","{{DOCKER_PATH}}/naspt-mpv2/core":"/moviepilot/.cache/ms-playwright"},"restartPolicy":"always"}},{"id":"Qbittorrent","name":"Qbittorrent","template":{"name":"naspt-qb","image":"ccr.ccs.tencentyun.com/naspt/qbittorrent:4.6.4","env":{"UMASK":"022","TZ":"Asia/Shanghai","SavePatch":"/media/downloads","TempPatch":"/media/downloads","WEBUI_PORT":"9000","PUID":"0","PGID":"0","PATH":"/lsiopy/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin","HOME":"/config","TERM":"xterm","S6_CMD_WAIT_FOR_SERVICES_MAXTIME":"0","PS1=$(whoami)@$(hostname)":"$(pwd)\\$","S6_VERBOSITY":"1","S6_STAGE2_HOOK":"/docker-mods","VIRTUAL_ENV":"/lsiopy","LSIO_FIRST_PARTY":"true","XDG_CONFIG_HOME":"/config","XDG_DATA_HOME":"/config"},"ports":{"9000/tcp":"{{QB_PORT}}"},"volumes":{"{{MEDIA_PATH}}":"/media","{{DOCKER_PATH}}/naspt-qb/config":"/config"},"restartPolicy":"always"}}],"parameters":[{"key":"MP_PORT","name":"MoviePilot端口","value":"3000"},{"key":"DOCKER_PATH","name":"Docker配置路径","value":"/Users/lizhiqiang/coding-my"},{"key":"MEDIA_PATH","name":"媒体文件路径","value":"/Users/lizhiqiang/coding-my/media"},{"key":"QB_PORT","name":"Qbittorrent端口","value":"9000"}]}', '1745506572000', '1745506574000', 0);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_logs_type ON logs(type);
CREATE INDEX IF NOT EXISTS idx_logs_level ON logs(level);
CREATE INDEX IF NOT EXISTS idx_logs_create_time ON logs(create_time);