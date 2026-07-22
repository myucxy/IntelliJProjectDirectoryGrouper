# P0 兼容性验证结果

更新日期：2026-07-21

## 已验证环境

| 产品 | 构建 | 本机路径 | 运行时 | 状态 |
| --- | --- | --- | --- | --- |
| IntelliJ IDEA Ultimate 2026.2 | `IU-262.8665.258` | `D:\Dev\App\JetBrains\IntelliJ IDEA 2022.2.2` | JBR 25.0.3 | 最近项目功能通过编译、测试、结构检查、Verifier 兼容检查及启动/退出生命周期 smoke test |

## `IU-262.8665.258` 结构验证

直接检查本机产品 JAR 与 `idea/PlatformActions.xml`，得到以下结果：

- `RecentProjectListGroup` 存在，类型为 `com.intellij.ide.actions.RecentProjectsGroup`。
- `OpenProjectWindows` 存在，类型为 `com.intellij.openapi.wm.impl.ProjectWindowActionGroup`。
- `WelcomeScreen.QuickStart.ProjectsState` 与 `WelcomeScreenRecentProjectActionGroup` 均存在。
- `RecentProjectListActionProvider.getActionsWithoutGroups(boolean, Project)` 及无参便利方法存在。
- `ReopenProjectAction.getProjectPath()` 存在并返回 `String`。
- `RecentProjectsManager` 仍公开 `getGroups()`、`addGroup()`、`removeGroup()`、`moveProjectToGroup()` 与 `removeProjectFromGroup()`。
- `ProjectGroup` 实现 `ModificationTracker`，可用 `getModificationCount()` 参与 snapshot token。
- `ProjectWindowAction` 和 `ProjectWindowActionGroup` 仍位于 implementation 包；业务代码不得直接依赖它们。

## 构建与 Verifier 结果

- Gradle Wrapper：9.6.1。
- IntelliJ Platform Gradle Plugin：2.18.1。
- Kotlin Gradle Plugin：2.4.10。
- 构建 JVM：JBR 25.0.3；插件 JVM 字节码目标：21，以保留 build 242 的运行时兼容目标。
- 分组核心、最近项目动作编排及替换生命周期：22 个测试通过，0 failure，0 error，0 skipped。
- `buildPlugin`：成功生成 `IntelliJProjectDirectoryGrouper-0.1.0.zip`。
- `verifyPluginStructure`：通过。
- Plugin Verifier 1.409：`dev.projectgroups.directory-grouper:0.1.0` 对 `IU-262.8665.258` 的结果为 `Compatible`。
- Verifier 动态插件判断：`Plugin can probably be enabled or disabled without IDE restart`。

Verifier 报告一个已知内部 API：`RecentProjectListActionProvider.getActionsWithoutGroups(boolean, Project)`。技术设计要求使用该入口取得未受欢迎页手动组影响的原始动作；调用已集中在 `RecentProjectSource` 适配器中，并在 `LinkageError` 时回退到平台原生列表。`verifyPlugin` 末尾会解析每份 HTML 报告，要求内部 API 列表恰好只有这条完整签名；新增、删除或签名变化都会导致构建失败，其他兼容性和 override-only 问题仍由 Gradle 插件直接判定失败。

本地 IU 安装的 `product-info.json` 引用了若干发行包中不存在的可选 classpath 元素，Verifier 将其报告为 IDE layout warning；插件兼容结论仍为 `Compatible`，没有插件 compatibility error。

使用最高目标 262 编译、同时声明 `since-build=242` 和 Java 21 时，`verifyPluginProjectConfiguration` 会提示目标平台与最低基线不一致。该提示不能用提高 `since-build` 到 262 来掩盖；发布前必须再用最低目标 242 编译，并对 242、251、252、261、262 分别运行 Verifier。

## 运行时 smoke test

在 `IU-262.8665.258` 沙箱中打开本工程后，日志确认：

- 插件 `Project Directory Grouper 0.1.0` 成功加载。
- `ActionReplacementService` 记录 `Installed recent projects directory grouping`。
- 应用正常退出时记录 `Restored platform recent projects action`。
- 未发现本插件的安装或恢复异常。

这证明 `RecentProjectListGroup` 的替换和应用退出恢复路径已执行；不等同于动态卸载验证，也不代替实际展开菜单检查分组内容。

## 尚未完成的运行时验证

- 实际展开“最近的项目”菜单，确认目标 UI 使用替换后的动作且分组内容、顺序和点击行为正确。
- 替换 `OpenProjectWindows` 后，项目打开和关闭是否继续更新保存的 delegate。
- 动态卸载插件时恢复原动作后，菜单与 keymap 是否完整；当前只验证了应用退出释放。
- 两个欢迎页入口在 2026.2 新 UI 中的实际位置和可见性。
- IDEA、PyCharm、WebStorm、Android Studio 的人工 smoke test。
- 242、251、252、261 的签名与 Plugin Verifier 验证。

最近项目功能已经达到可安装测试阶段；在实际菜单交互、动态卸载和最低版本矩阵完成前，仍不视为达到发布条件。
