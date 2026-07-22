# 测试计划

## 1. 单元测试

### 1.1 分组算法

| ID | 输入 | 预期 |
| --- | --- | --- |
| UT-G01 | 空列表 | 空结果 |
| UT-G02 | 一个本地项目 | 一个未分组项 |
| UT-G03 | 同父目录两个项目 | 一个含两个成员的组 |
| UT-G04 | 三个父目录各一个项目 | 三个未分组项 |
| UT-G05 | 两组与散项交错 | 组位置取首成员位置，成员相对顺序不变 |
| UT-G06 | 不同路径但父目录同名 | 组名带最短可区分上级路径 |
| UT-G07 | Windows 路径仅大小写不同 | 按 Windows 策略归为同一父目录 |
| UT-G08 | Linux 路径仅大小写不同 | 按 Linux 策略视为不同父目录 |
| UT-G09 | UNC 路径 | 服务器/共享部分保留且稳定分组 |
| UT-G10 | 根目录项目 | 保持未分组 |
| UT-G11 | 无本地路径项 | 保持未分组且顺序不变 |
| UT-G12 | 路径含 `.`/`..` | 词法规范化后正确分组 |

### 1.2 欢迎页计划

| ID | 场景 | 预期 |
| --- | --- | --- |
| UT-W01 | 无已有组 | 为合格父目录创建组 |
| UT-W02 | 项目已在用户组 | 跳过，不修改用户组 |
| UT-W03 | 第二次运行且输入未变 | 空变更计划 |
| UT-W04 | 插件管理组新增同父目录项目 | 更新该组 |
| UT-W05 | 插件管理组项目不再出现 | 移除失效成员，必要时删空组 |
| UT-W06 | 用户重命名插件组 | 放弃所有权，不修改组 |
| UT-W07 | 用户手工加入额外成员 | 放弃所有权，不修改组 |
| UT-W07B | 用户手工移除或重排成员 | 放弃所有权，不把成员改回去 |
| UT-W08 | 自动组名与用户组冲突 | 生成稳定且唯一的自动组名 |
| UT-W09 | 应用前 repository snapshot token 变化 | 拒绝旧计划并重新计算 |

### 1.3 设置与迁移

- 全新状态默认 `true`。
- 旧状态缺失字段默认 `true`。
- 用户明确设置 `false` 后重启仍为 `false`。
- 所有权记录序列化和反序列化不改变路径键。

## 2. 平台集成测试

- 用测试 Application 注册假的原动作，验证替换、委托、重复初始化和 dispose 恢复。
- 原动作不存在、类型不符或已被其他插件替换时安全降级。
- 最近项目 wrapper 保留非 `ReopenProjectAction` 和尾部命令。
- 设置关闭时 wrapper children 与 delegate children 对象及顺序一致。
- 打开项目 wrapper 不返回已 disposed 项目动作。
- 原生 `ProjectGroup` 写入按“移出成员，再删空组”的顺序执行。
- 欢迎页取消预览不产生任何 repository 调用。
- Action update 不在 EDT 执行阻塞操作。

## 3. 人工功能场景

### MF-01 默认安装

1. 准备 `workspace-a/api`、`workspace-a/web`、`workspace-b/cli` 三个最近项目。
2. 安装插件并重启或动态加载。
3. 打开“最近的项目”。
4. 预期看到 `workspace-a` 组，其中有 `api` 和 `web`；`cli` 保持散项。

### MF-02 开关

1. 关闭设置复选框并 Apply。
2. 再次打开最近项目与打开项目列表。
3. 预期两个列表恢复扁平展示。
4. 打开欢迎页，确认已有组没有变化。

### MF-03 项目窗口切换

1. 同时打开同一父目录下两个项目和另一个目录的项目。
2. 通过打开项目列表选择同组项目。
3. 预期目标窗口获得焦点，当前项目标记正确。
4. 最小化目标窗口后重复，预期窗口恢复。

### MF-04 欢迎页手动自动分组

1. 创建一个用户手动组并放入一个项目。
2. 点击 `按父目录自动分组...`。
3. 检查预览中的创建、移动和跳过数量。
4. 取消，确认无变化。
5. 再次执行并确认，确认新组正确、用户组不变。
6. 第三次执行，预期没有重复组或无意义移动。

### MF-05 所有权转移

1. 重命名一个插件创建的组，或手动加入其他父目录项目。
2. 再次执行自动分组。
3. 预期该组和成员不被插件修改。

### MF-06 动态卸载

1. 在无需重启的情况下卸载插件。
2. 打开最近项目和项目窗口切换菜单。
3. 预期平台原生列表正常显示，无异常日志和空菜单。

## 4. 产品与版本矩阵

最低建议矩阵如下；具体版本选择与目标 build 对齐。

| 产品 | 242 | 251 | 252 | 261 | 262 | 人工 Smoke |
| --- | --- | --- | --- | --- | --- | --- |
| IntelliJ IDEA Community | Verifier | Verifier | Verifier | Verifier | Verifier | 必须 |
| IntelliJ IDEA Ultimate | Verifier | Verifier | Verifier | Verifier | `IU-262.8665.258` | 必须 |
| PyCharm | Verifier | Verifier | Verifier | Verifier | Verifier | 必须 |
| WebStorm | Verifier | Verifier | Verifier | Verifier | Verifier | 必须 |
| Android Studio | 对应平台版 | 对应平台版 | 对应平台版 | 对应平台版 | 对应平台版 | 必须 |
| GoLand | Verifier | Verifier | Verifier | Verifier | Verifier | 最新 |
| CLion | Verifier | Verifier | Verifier | Verifier | Verifier | 最新 |
| DataGrip | Verifier | Verifier | Verifier | Verifier | Verifier | 最新 |
| Rider | Verifier | Verifier | Verifier | Verifier | Verifier | 最新，重点检查前后端模式 |

PhpStorm、RubyMine 至少完成 Plugin Verifier；正式声明兼容前应各做一次最新版本 smoke test。

## 5. 操作系统矩阵

| 系统 | 重点 |
| --- | --- |
| Windows 11 | 盘符大小写、反斜杠、UNC、窗口最小化恢复 |
| macOS 当前与前一主版本 | 默认大小写语义、窗口激活、系统菜单差异 |
| Ubuntu LTS | 大小写敏感、窗口管理器焦点行为 |

## 6. 性能与稳定性

- 1000 条项目数据分组 100 次，记录 p50/p95；单次 p95 目标小于 50 ms。
- 10000 条数据用于观察算法复杂度和内存增长，不作为常规 UI 数据规模。
- 连续展开/关闭菜单 500 次，不应持续增长 action 或 listener 数量。
- 连续开关 100 个临时项目，打开项目菜单始终与实际状态一致。
- 连续动态加载/卸载插件 10 次，动作恢复且无 ID 冲突。

## 7. 发布阻断条件

- 任一目标 IDE 出现菜单为空、项目无法打开/激活或动态卸载失败。
- Plugin Verifier 报告 compatibility error。
- 设置关闭后仍改变原生顺序或欢迎页组。
- 自动分组覆盖用户组或删除含成员的组。
- 日志包含未脱敏的绝对项目路径。
- Action update 触发可见 UI 卡顿或线程违规报告。
