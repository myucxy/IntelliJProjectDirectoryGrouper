# 实施计划

## 阶段 0：API 可行性验证

这是编码前置门槛，不通过时先调整架构。

- 创建最小插件骨架，基线选 242。
- 在沙箱 IDE 中确认动作 ID `RecentProjectListGroup` 和 `OpenProjectWindows` 存在。
- 验证 `ActionManager.replaceAction()` 后所有菜单引用都指向 wrapper。
- 验证项目打开/关闭后原 `OpenProjectWindows` delegate 是否继续更新。
- 验证动态卸载能恢复两个原动作，不要求 IDE 重启。
- 验证 `RecentProjectListActionProvider.getActionsWithoutGroups()` 与 `ReopenProjectAction.projectPath` 在 242、251、252、261、`IU-262.8665.258` 的签名。
- 验证欢迎页动作组 ID 在上述版本存在，并确认主要入口可见。
- 在至少 IDEA、PyCharm、WebStorm、Android Studio 各跑一次手工 smoke test。

退出条件：形成一页 P0 记录，列出每个 build 的可用能力和所选适配方案。

## 阶段 1：纯分组核心

- 实现 `ProjectEntry`、`GroupedEntry` 和 `DirectoryGroupingEngine`。
- 实现 Windows/macOS/Linux 路径键策略。
- 实现同名父目录的最短路径消歧。
- 实现组与成员稳定排序。
- 覆盖空输入、单项、同父目录、同名目录、UNC、根路径和无效路径测试。
- 添加 1000 和 10000 条输入的性能测试基准；1000 条作为发布门槛。

退出条件：领域模块不依赖 IntelliJ SDK，单元测试全部通过。

## 阶段 2：设置与生命周期

- 实现应用级设置状态，默认 `autoGroupMenus=true`。
- 实现 Tools 下的设置页和中英文资源。
- 实现 `ActionReplacementService` 的安装、幂等和恢复。
- 添加插件动态加载/卸载测试或可重复的人工测试脚本。

退出条件：开关即时生效；卸载后平台动作完全恢复。

## 阶段 3：最近项目列表

- 实现 `RecentProjectSource` 版本适配器。
- 构建最近项目虚拟组，组内复用原 `ReopenProjectAction`。
- 保留第三方 provider 项、无路径项、分隔符和清除列表动作。
- 检查重复项目名时的显示与路径描述。
- 验证设置关闭时 delegate 输出与未安装插件一致。

退出条件：FR-02 和对应测试通过。

## 阶段 4：打开项目列表

- 实现 `OpenProjectSource` 和 `ActivateProjectAction`。
- 实现当前项目选中、窗口激活和最小化恢复。
- 处理无路径项目和 delegate 独有项目项。
- 验证项目开关、重命名、窗口关闭和动态卸载。

退出条件：FR-03 和对应测试通过，不影响原生窗口切换。

## 阶段 5：欢迎页自动分组

- 实现插件管理组的持久状态和所有权判定。
- 实现 `GroupingPlan` 的纯计算与幂等测试。
- 实现预览对话框。
- 实现原生 `ProjectGroup` 增量应用，严格保护用户组。
- 注册两个欢迎页入口并验证可见性。
- 添加状态变化后的重新确认逻辑和结果通知。

退出条件：FR-04 至 FR-06 和取消无副作用测试通过。

## 阶段 6：兼容、质量与发布

- 运行所有单元和 IntelliJ Platform 集成测试。
- 运行 Plugin Verifier 覆盖目标 build 与产品。
- 按 [05-test-plan.md](05-test-plan.md) 完成人工矩阵。
- 检查动态卸载、无障碍、英文和简体中文。
- 检查日志不泄露绝对路径。
- 准备插件说明、变更记录、截图和隐私说明。

退出条件：所有发布级验收标准通过，无 Plugin Verifier compatibility errors。

## 推荐提交顺序

1. `chore: scaffold IntelliJ Platform plugin`
2. `feat: add directory grouping core`
3. `feat: add application settings`
4. `feat: wrap recent projects action group`
5. `feat: group open project window actions`
6. `feat: add welcome screen auto-group action`
7. `test: cover platform compatibility and UI flows`
8. `docs: prepare marketplace release`

每个提交应可编译并包含对应测试，避免把所有平台 API 风险集中到一个巨大提交。

## 完成定义

一个任务只有同时满足以下条件才算完成：

- 行为符合需求与交互规则。
- 新增逻辑有与风险相称的测试。
- 不引入新的 Plugin Verifier 错误。
- 设置关闭时能可靠回到原生行为。
- 平台 API 失败有降级路径。
- 中英文文案完整，无硬编码用户可见文本。
- 没有记录完整项目路径或访问网络。
