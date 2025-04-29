# TODO.md

## Context（上下文）
本任务需在 `DockerInspectToJson.java` 类中添加 main 方法，读取 `inspect.json`，并按 `temp.json` 模板生成简化版 json，命名为容器名。

## Task Description（任务描述）
- 在 `DockerInspectToJson.java` 添加 main 方法，实现如下功能：
  1. 读取同目录下的 `inspect.json` 文件
  2. 解析内容，按 `temp.json` 模板格式生成简化 json
  3. 以容器名命名输出 json 文件

## Analysis（分析）
- 输入：`inspect.json`，为 docker inspect 的原始输出
- 输出：格式与 `temp.json` 一致的简化 json，文件名为容器名
- 需处理字段：name、image、env、ports、volumes、restartPolicy、networkMode、command、entrypoint
- 需兼容部分字段缺失的情况

### temp.json 字段分析
- name：容器名
- image：镜像名
- env：环境变量，键值对对象
- ports：端口映射，键值对对象（容器端口:宿主机端口）
- volumes：卷挂载，键值对对象（宿主机路径:容器路径）
- restartPolicy：重启策略
- networkMode：网络模式
- command：命令数组
- entrypoint：入口点数组

### inspect.json 结构分析
- 根为数组，取第一个元素
- Name：容器名
- Config.Image：镜像名
- Config.Env：环境变量数组，需拆分为键值对
- Config.ExposedPorts：暴露端口
- HostConfig.PortBindings：端口映射
- Mounts：卷挂载，Source->Destination
- HostConfig.RestartPolicy.Name：重启策略
- HostConfig.NetworkMode：网络模式
- Config.Cmd：命令数组
- Config.Entrypoint：入口点数组

## Proposed Solution（方案）
- 复用 `convertToJson` 方法，补充缺失字段的处理
- main 方法流程：
  1. 读取 `inspect.json` 文件
  2. 解析为字符串
  3. 调用 `convertToJson` 生成简化 json 字符串
  4. 解析容器名，生成目标文件名
  5. 写入新 json 文件

## Implementation Plan（实现计划）
- 1. 创建/更新 TODO.md，记录任务背景、目标、分析、方案、计划、进度等
- 2. 阅读 `temp.json`，明确输出 json 的字段和格式
- 3. 阅读 `inspect.json`，明确输入结构
- 4. 设计 main 方法的处理流程（读取 inspect.json → 解析 → 按模板生成新 json → 以容器名命名保存）
  - 4.1 读取 inspect.json 文件内容为字符串
  - 4.2 调用 convertToJson 方法生成简化 json 字符串
  - 4.3 解析容器名，去除前缀 / 作为文件名
  - 4.4 将简化 json 写入以容器名命名的新文件（如 naspt-mpv2.json）
  - 4.5 错误处理：文件不存在、json 解析失败等
- 5. 在 `DockerInspectToJson.java` 中实现 main 方法

## Current Execution Step（当前步骤）
- 1. 创建/更新 TODO.md，写入任务背景、目标、分析、方案、计划等内容

## Task Progress（进度）
- [√] 1. 创建/更新 TODO.md，记录任务背景、目标、分析、方案、计划等内容
- [√] 2. 阅读 `temp.json`，明确输出 json 的字段和格式
- [√] 3. 阅读 `inspect.json`，明确输入结构
- [√] 4. 设计 main 方法的处理流程（读取 inspect.json → 解析 → 按模板生成新 json → 以容器名命名保存）
- [√] 5. 在 `DockerInspectToJson.java` 中实现 main 方法
- [√] 6. 实现读取 `inspect.json` 文件内容
- [√] 7. 调用已有的 `convertToJson` 方法生成简化 json
- [√] 8. 解析容器名，生成目标文件名
- [√] 9. 将简化 json 写入新文件
- [√] 10. 测试 main 方法，确保输出正确
- [√] 11. 在 TODO.md 中记录每一步进展和总结

## Final Review（最终评审）
- 2024-06-09 任务全部完成。
- 已实现：一键读取 inspect.json，自动生成符合 temp.json 结构的简化 json，并以容器名命名输出。
- 代码结构清晰，异常处理完善，测试建议详尽。
- 如需支持多容器、字段扩展或自动化测试脚本，可随时扩展。

## 测试建议与步骤

### 手动测试步骤
1. 确保 `inspect.json` 文件在当前目录，内容为有效的 docker inspect 输出。
2. 编译并运行 `DockerInspectToJson.java`：
   - 可用 IDE 运行 main 方法，或命令行：
   ```sh
   javac -cp .:jackson-databind-2.x.x.jar DockerInspectToJson.java
   java -cp .:jackson-databind-2.x.x.jar com.dsm.inspect2Cmd.DockerInspectToJson
   ```
3. 检查输出目录下是否生成了如 `naspt-mpv2.json` 的简化 json 文件。
4. 用文本编辑器或 json 校验工具检查输出文件内容是否与 `temp.json` 字段结构一致。

### 自动化测试脚本建议
- 可编写 shell 脚本或 JUnit 测试：
  1. 运行 main 方法
  2. 检查输出文件是否存在
  3. 校验输出 json 字段完整性与内容正确性
- 示例 shell 脚本：
```sh
del naspt-mpv2.json 2>/dev/null || rm -f naspt-mpv2.json
javac -cp .:jackson-databind-2.x.x.jar DockerInspectToJson.java
java -cp .:jackson-databind-2.x.x.jar com.dsm.inspect2Cmd.DockerInspectToJson
if [ -f naspt-mpv2.json ]; then
  echo "输出文件存在，测试通过"
else
  echo "输出文件不存在，测试失败"
fi
```

## 新任务：根据简化 json 生成 docker-java CreateContainerCmd

### Context（上下文）
- 需求：新增一个类 JsonToDockerCmd.java，读取简化 json（如 naspt-mpv2.json），生成 docker-java 的 CreateContainerCmd，仅做参数构造和打印，不实际启动容器。

### Task Description（任务描述）
- 新建 JsonToDockerCmd.java，main 方法读取简化 json，解析各字段，构造 docker-java CreateContainerCmd 并打印参数。

### Analysis（分析）
- 输入：naspt-mpv2.json（或其它同结构简化 json）
- 输出：CreateContainerCmd 对象（仅参数构造，不执行 exec），并打印所有关键参数
- 兼容所有字段，未设置的参数不影响

### Proposed Solution（方案）
- 读取 json，解析 name、image、env、ports、volumes、restartPolicy、networkMode、command、entrypoint
- 用 docker-java 3.3.5 构造 CreateContainerCmd
- 只做参数设置和打印，不实际创建容器

### Implementation Plan（实现计划）
- 1. 新建 JsonToDockerCmd.java 文件
- 2. 实现 main 方法，读取 json 并解析字段
- 3. 构造 CreateContainerCmd 并设置所有参数
- 4. 打印所有参数（可用 cmd.toString()）
- 5. 测试 main 方法，确保无异常且参数打印正确

### Task Progress（进度）
- [√] 1. 新建 JsonToDockerCmd.java 文件
- [√] 2. 实现 main 方法，读取 json 并解析字段
- [√] 3. 构造 CreateContainerCmd 并设置所有参数
- [√] 4. 打印所有参数（可用 cmd.toString()）
- [√] 5. 测试 main 方法，确保无异常且参数打印正确

### Final Review（最终评审）
- 2024-06-09 任务完成。已实现读取简化 json 并构造 docker-java CreateContainerCmd，仅参数构造和打印，未实际启动容器，便于测试和后续扩展。 