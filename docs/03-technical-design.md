# 技术设计

## 1. 技术基线

- Kotlin/JVM。
- Gradle Kotlin DSL。
- IntelliJ Platform Gradle Plugin 2.x。
- Java/Kotlin toolchain 版本跟随选定的最低平台 build。
- 只声明 `com.intellij.modules.platform`，避免限制到某个语言 IDE。
- 初始建议 `since-build=242`，在 P0 验证后确定 `until-build` 和是否需要多发布物。

插件涉及平台动作替换和部分 implementation API，不能仅用一个最新版 IDE 的 `runIde` 结果宣称兼容所有产品。

## 2. 架构边界

```text
Platform data/actions
        |
        v
  Platform Adapters  ---- compatibility boundary
        |
        v
  Pure Grouping Core ---- path normalization, grouping, naming, ordering
        |
        +------> Virtual Menu Groups
        |
        +------> Welcome Grouping Plan
                         |
                         v
                 Native ProjectGroup Repository
```

核心算法不得引用 Swing、`AnAction`、`Project` 或 application services，使其可以作为普通 Kotlin 单元测试运行。

## 3. 建议模块与类

首版可以保留单 Gradle 模块，但按包隔离职责：

```text
src/main/kotlin/dev/projectgroups/directorygrouper/
  domain/
    ProjectEntry.kt
    DirectoryGroup.kt
    GroupingPlan.kt
    DirectoryGroupingEngine.kt
    GroupNameDisambiguator.kt
    PathKeyStrategy.kt
  platform/
    RecentProjectSource.kt
    OpenProjectSource.kt
    NativeProjectGroupRepository.kt
    PlatformCompatibility.kt
  actions/
    GroupedRecentProjectsActionGroup.kt
    GroupedOpenProjectsActionGroup.kt
    ActivateProjectAction.kt
    AutoGroupWelcomeProjectsAction.kt
  settings/
    DirectoryGrouperSettings.kt
    DirectoryGrouperConfigurable.kt
  lifecycle/
    ActionReplacementService.kt
  ui/
    AutoGroupPreviewDialog.kt
  notifications/
    DirectoryGrouperNotifications.kt
```

资源：

```text
src/main/resources/
  META-INF/plugin.xml
  messages/DirectoryGrouperBundle.properties
  messages/DirectoryGrouperBundle_zh_CN.properties
```

## 4. 领域接口

建议先稳定以下接口，再接平台 UI：

```kotlin
data class ProjectEntry(
  val stableId: String,
  val displayName: String,
  val localPath: Path?,
  val sourceIndex: Int,
  val lastOpenedAt: Long?,
  val payload: Any?,
)

sealed interface GroupedEntry {
  data class Group(val key: String, val name: String, val children: List<ProjectEntry>) : GroupedEntry
  data class Item(val project: ProjectEntry) : GroupedEntry
}

interface DirectoryGroupingEngine {
  fun group(entries: List<ProjectEntry>, pathStrategy: PathKeyStrategy): List<GroupedEntry>
}
```

生产代码中的 `payload` 可由适配层替换为泛型；文档示意强调算法不能主动调用它。

## 5. 设置持久化

使用 application-level `PersistentStateComponent`：

```text
DirectoryGrouperState
  autoGroupMenus = true
  managedWelcomeGroups = []
```

建议存储文件：`ProjectDirectoryGrouper.xml`。

要求：

- 缺失字段按默认值 `true` 迁移。
- `managedWelcomeGroups` 只记录所有权信息，不复制所有最近项目数据。
- 修改设置后触发 action UI 更新；不要重装或重复替换动作。

## 6. 最近项目动作组

平台 `RecentProjectListActionProvider.getActionsWithoutGroups(...)` 能返回原生扁平动作，其中本地项目通常是 `ReopenProjectAction`，后者公开 `projectPath`。但该 provider 方法在当前源码中标记为 `@ApiStatus.Internal`，必须封装在 `RecentProjectSource` 适配器中。

实现原则：

1. 保存 `RecentProjectListGroup` 对应的原动作。
2. 用 `GroupedRecentProjectsActionGroup` 替换该动作 ID。
3. 设置关闭或适配器失败时，直接委托原动作的 `getChildren()`。
4. 设置开启时获取当前原生扁平动作。
5. 对 `ReopenProjectAction` 提取路径并进入纯算法；原动作对象本身作为 payload 放入组内，保留原生行为。
6. 无法识别路径的动作、第三方 provider 动作和尾部命令按原顺序保留。
7. 自定义组使用平台 `DefaultActionGroup`/`ActionGroup`，组为弹出子菜单。

不要持久化这里的计算结果；每次菜单展开重新构建。

## 7. 打开项目动作组

`OpenProjectWindows` 在平台中由项目窗口生命周期动态维护。推荐包装而非破坏其原对象：

1. 保存原动作组引用作为 delegate。
2. 替换动作 ID 的对象只负责展示。
3. 设置关闭时返回 delegate 的原始 children。
4. 设置开启时从 `ProjectManager.getOpenProjects()` 生成 `ProjectEntry`。
5. 使用插件自己的 `ActivateProjectAction` 激活窗口，避免直接编译依赖 `@ApiStatus.Internal` 的 `ProjectWindowAction`。
6. 无法映射到 `Project` 的原 delegate children 作为未分组项追加，防止 LightEdit 或产品扩展消失。

`ActivateProjectAction` 要实现：

- 使用平台窗口与焦点 API激活目标项目窗口。
- 当前项目显示选中状态。
- 处理最小化窗口。
- `getActionUpdateThread()` 明确返回合适线程。
- 不缓存已释放的 `Project`；执行和更新时检查 `isDisposed`。

P0 必须验证平台是否持有原动作组引用并在项目开关时继续更新 delegate。如果行为不同，改用兼容适配器，不可让替换动作截断平台更新。

## 8. 动作替换生命周期

`ActionManager.replaceAction()` 是公开 API，但平台源码明确说明“不推荐用它改变平台动作行为，最好由原动作提供扩展点”。本需求没有对应公开扩展点，因此它是已接受但受控的风险。

`ActionReplacementService` 要求：

- application service 初始化时记录原动作对象。
- 只替换已存在且类型符合预期的动作 ID。
- 相同插件实例只替换一次。
- 动态卸载时，仅当当前注册对象仍是本插件 wrapper 时恢复原对象。
- 任一步失败就回滚已经完成的替换。
- 与其他替换同一动作的插件冲突时不强行覆盖后来的对象，并记录可诊断信息。
- 插件禁用分组时 wrapper 仍保留，只切换到 delegate，减少注册抖动。

## 9. 欢迎页集成

在 `plugin.xml` 注册一个动作，并添加到：

- `WelcomeScreen.QuickStart.ProjectsState`
- `WelcomeScreenRecentProjectActionGroup`

动作执行流程：

```text
读取最近项目路径和原生 groups
  -> 判定插件拥有的组与用户组
  -> 纯函数生成 GroupingPlan
  -> 展示预览
  -> 再次读取 repository snapshot token
  -> 未变化：应用；已变化：重新计划并重新确认
  -> 发布 RECENT_PROJECTS_CHANGE_TOPIC / 使用 repository API 自带通知
```

`NativeProjectGroupRepository` 只能通过 `RecentProjectsManager` 公共的 `groups`、`addGroup()`、`moveProjectToGroup()`、`removeProjectFromGroup()` 和 `removeGroup()` 操作。删除组前必须确保组为空。

`snapshot token` 优先使用目标版本可用的 modification count；不可用时使用组名、成员顺序和最近项目路径生成的确定性哈希。它只用于发现预览期间的并发变化。

## 10. 并发与线程

- 路径分组计算在 BGT 执行。
- Swing 对话框和通知在 EDT 创建。
- 应用原生项目组修改前后遵守目标平台 API 的线程约束，并在集成测试中使用平台测试调度器。
- 不在 `AnAction.update()` 中读取磁盘、弹窗或写状态。
- 菜单数据是快照；用户展开菜单后项目状态变化，下一次展开再更新即可。

## 11. 兼容层

建立单一的 `PlatformCompatibility` 门面，禁止 implementation API 分散到业务代码。

建议能力标记：

```text
canReadRecentProjectActions
canReplaceRecentProjectsGroup
canWrapOpenProjectsGroup
canMutateNativeWelcomeGroups
```

启动时做结构性检查，运行时捕获 `LinkageError`、缺失动作和类型变化。某项能力不可用时只关闭对应功能，其余功能继续工作。

版本策略：

- 首选一个主分支加少量版本适配实现。
- 若 242 与 `IU-262.8665.258` 的二进制签名无法共存，使用 Gradle source set 或为旧平台维护发布分支。
- 每个发布物必须用 Plugin Verifier 覆盖声明范围，不能用宽泛的 `until-build` 代替测试。

## 12. 日志与诊断

日志事件建议包括：

- 替换/恢复动作是否成功及动作 ID。
- 平台能力探测结果。
- 分组输入数量、输出组数量和耗时，不记录完整路径。
- 欢迎页计划创建、更新、跳过和失败的计数。

日志不应包含完整绝对路径。开发调试需要定位时可通过一次性的调试开关输出脱敏信息。

## 13. 依赖限制

- 首版不引入第三方 UI、集合或路径处理库。
- 使用 IntelliJ 平台图标和 UI 组件。
- 不使用反射读取 private 字段作为正式方案。
- 不复制 JetBrains 内部动作实现的整段源码；只实现本插件必要行为并保留源码依据链接。
