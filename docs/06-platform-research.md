# IntelliJ Platform 调研记录

调研日期：2026-07-21
基准：JetBrains `intellij-community` `master` 当日源码。源码事实会随平台变化，实施时必须针对目标 build 再核对。

## 1. 已核实事实

### 1.1 平台已有原生项目组模型

`RecentProjectsManager` 公开 `groups`、`addGroup()`、`removeGroup()`、`moveProjectToGroup()` 和 `removeProjectFromGroup()`。

来源：

- [RecentProjectsManager.kt](https://github.com/JetBrains/intellij-community/blob/master/platform/ide-core/src/com/intellij/ide/RecentProjectsManager.kt)
- [ProjectGroup.java](https://github.com/JetBrains/intellij-community/blob/master/platform/ide-core/src/com/intellij/ide/ProjectGroup.java)

结论：欢迎页自动分组应复用平台 `ProjectGroup`，无需建立替代的欢迎页项目树。

### 1.2 最近项目数据提供器支持原生用户组，但菜单主动请求无组数据

`RecentProjectListActionProvider.getActions(...)` 有 `useGroups` 参数；`collectProjects()` 也会读取原生 groups。另一方面，平台 `RecentProjectsGroup` 当前调用 `getActionsWithoutGroups(...)`。

来源：

- [RecentProjectListActionProvider.kt](https://github.com/JetBrains/intellij-community/blob/master/platform/platform-impl/src/com/intellij/ide/RecentProjectListActionProvider.kt)
- [RecentProjectsGroup.kt](https://github.com/JetBrains/intellij-community/blob/master/platform/platform-impl/src/com/intellij/ide/actions/RecentProjectsGroup.kt)

结论：仅写入原生 `ProjectGroup` 不能保证 `File > Open Recent` 按组显示；需要包装/替换该动作组，或等待平台未来提供扩展点。

### 1.3 原生最近项目动作可携带路径

`ReopenProjectAction` 当前公开 `projectPath`，可以复用动作本身并仅改变容器层级。

来源：

- [ReopenProjectAction.kt](https://github.com/JetBrains/intellij-community/blob/master/platform/platform-impl/src/com/intellij/ide/ReopenProjectAction.kt)

结论：最近项目组内应保留原动作实例，避免复制项目打开与失效路径处理。

### 1.4 欢迎页已有手动组动作和可扩展动作组

平台动作配置包含：

- `WelcomeScreen.NewGroup`
- `WelcomeScreen.MoveToGroup`
- `WelcomeScreen.EditGroup`
- `WelcomeScreen.QuickStart.ProjectsState`
- `WelcomeScreenRecentProjectActionGroup`

来源：

- [PlatformActions.xml](https://github.com/JetBrains/intellij-community/blob/master/platform/platform-resources/src/idea/PlatformActions.xml)

结论：保留原生手动操作，并将一个插件动作添加到欢迎页动作组即可；不应替换整个欢迎页。

### 1.5 项目窗口动作属于内部 API

`ProjectWindowAction` 当前标记为 `@ApiStatus.Internal`，并由项目开关时动态创建和注册。

来源：

- [ProjectWindowAction.kt](https://github.com/JetBrains/intellij-community/blob/master/platform/platform-impl/src/com/intellij/openapi/wm/impl/ProjectWindowAction.kt)

结论：插件不应直接构造或强类型依赖它；开启分组时使用自己的窗口激活动作，关闭时委托原组。

### 1.6 动作替换 API 公开但官方不推荐用于平台动作改写

`ActionManager.replaceAction()` 是 public abstract API，其注释明确说明不推荐用它改变平台动作行为，理想方案是原动作暴露扩展点。

来源：

- [ActionManager.java](https://github.com/JetBrains/intellij-community/blob/master/platform/editor-ui-api/src/com/intellij/openapi/actionSystem/ActionManager.java)

结论：这是实现本需求的主要兼容风险，必须封装、可回滚、可卸载并按版本验证。

### 1.7 删除原生组需要谨慎

当前 `RecentProjectsManagerBase.removeGroup()` 会遍历组内项目并删除对应 additional info；`moveProjectToGroup()` 会先从所有组移除该项目。

来源：

- [RecentProjectsManagerBase.kt](https://github.com/JetBrains/intellij-community/blob/master/platform/platform-impl/src/com/intellij/ide/RecentProjectsManagerBase.kt)

结论：插件删除自己创建的旧组之前必须先把成员移出，且只删除已确认为空的组。

## 2. API 风险分级

| 能力 | 当前依据 | 风险 |
| --- | --- | --- |
| 读写原生 `ProjectGroup` | `RecentProjectsManager` 公共接口 | 中：接口公开但行为仍需版本测试 |
| 纯 Action System 注册与分组 | 公共 Action System | 低 |
| 替换平台动作 | public API，但注释不推荐 | 高 |
| 获取最近项目扁平动作 | provider 方法为 `@ApiStatus.Internal` | 高 |
| 复用 `ReopenProjectAction.projectPath` | 类和属性当前公开，位于 platform-impl | 中高 |
| 枚举打开项目 | `ProjectManager` 公共 API | 低 |
| 激活项目窗口 | 公共窗口/焦点 API，可有产品差异 | 中 |
| 欢迎页动作组 ID | 平台 XML 中存在，不是类型安全 API | 中高 |

## 3. P0 必须回答的问题

1. `RecentProjectListGroup` 和 `OpenProjectWindows` 在 242、251、252、261、`IU-262.8665.258` 中是否都存在，类型是否一致？
2. `replaceAction()` 是否会更新所有已经持有 action reference 的菜单组？
3. 替换 `OpenProjectWindows` 后，平台后续新增/删除窗口动作写入原 delegate、当前 ID 对象，还是两者之一？
4. 动态卸载时恢复原动作后，keymap、菜单引用和动态窗口项是否完整？
5. `RecentProjectListActionProvider.getActionsWithoutGroups()` 在各版本的签名和返回项结构是否一致？
6. 第三方 `recentProjectsProvider` 条目没有本地路径时，怎样保持相对顺序和原行为？
7. 两个欢迎页动作组 ID 在各目标产品中是否可见；Rider 与 Android Studio 是否有特殊页面？
8. `RecentProjectsManager` 修改组是否要求 EDT；各版本是否自动发出足够的 UI 刷新事件？

## 4. 建议的降级次序

1. 全能力：两个菜单虚拟分组 + 欢迎页手动自动分组。
2. 最近项目适配失败：保留原生最近项目，打开项目与欢迎页功能继续。
3. 打开项目适配失败：保留原生打开项目，最近项目与欢迎页功能继续。
4. 欢迎页入口 ID 失效：保留设置页或 Search Everywhere 可调用动作，但不静默写组。
5. 原生组写 API 失效：禁用欢迎页动作，仅保留虚拟分组。

任何降级都不能让原生菜单不可用。

## 5. 后续可向 JetBrains 推动的改进

长期最稳妥的方案是向 IntelliJ Platform 提交扩展点需求，使插件能够：

- 向最近项目和打开项目列表提供分组策略。
- 取得稳定的项目 location 模型，而不是 implementation action。
- 在不替换整组 action 的前提下改变分组容器。

若平台接受该扩展点，后续版本应迁移并删除动作替换逻辑。
