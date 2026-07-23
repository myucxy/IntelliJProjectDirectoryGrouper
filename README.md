# IntelliJ Project Directory Grouper

当前版本：`0.1.0` | 最后验证：2026-07-23

## 中文

Project Directory Grouper 是一个 IntelliJ Platform 插件，用项目的直接父目录整理最近项目，同时保留 IntelliJ 原有的项目动作、打开行为和欢迎页项目组。

### 当前功能

- 在 `文件 | 最近的项目` 中保留原生入口，将同一父目录的项目按标题分区平铺显示，单个项目统一归入“其他”。
- 在主工具栏项目微件中，将同一父目录的项目放在一个平铺分区内，不需要进入二级菜单。
- 微件分区使用加粗组名和连续细边框；组名嵌入顶部边框，组名与边框使用相同的主题适配彩虹色。
- 没有同目录项目的单项集中显示在中性灰色的“其他”分区。
- 微件保留项目图标、项目名、提供方路径、项目路径和 Git 分支，并支持按这些内容搜索；Git 分支按原有样式带分支图标独立成行显示。
- IntelliJ 未提供 Git 分支时，可在后台自动读取并缓存 `.git/HEAD`；该功能默认开启，可在插件设置中独立关闭，且不会执行 Git 命令。
- “打开的项目”、更新提示和微件工具动作继续使用平台原有分区和行为。
- 可在 `设置 | 工具 | 项目目录分组` 中分别开关项目分组和缺失分支回读，也可从项目微件底部进入设置。
- 关闭后恢复平台原始项目列表；遇到平台链接兼容错误时自动回退到原生列表。
- 不递归扫描磁盘、不修改项目文件；可选分支回读只检查项目上级目录的 `.git` 标记和 `HEAD`，也不会新增、删除或移动欢迎页中的项目组。

### 安装与使用

1. 发布 Release 后可从 GitHub Releases 下载插件 ZIP；也可以在源码目录执行下方的构建命令，生成 `build/distributions/IntelliJProjectDirectoryGrouper-0.1.0.zip`。
2. 在 IntelliJ IDEA 中打开 `设置 | 插件`，点击齿轮按钮并选择“从磁盘安装插件”，然后选择插件 ZIP。
3. 安装后可直接打开 `文件 | 最近的项目` 或主工具栏项目微件查看分组效果。
4. 在 `设置 | 工具 | 项目目录分组` 中配置项目分组和缺失 Git 分支回读。

## English

Project Directory Grouper organizes recent IntelliJ Platform projects by their direct parent directory while preserving the platform's project actions, opening behavior, and Welcome Screen groups.

### Current Features

- In `File | Recent Projects`, the native entry is preserved while projects sharing a parent directory appear in flat titled sections; single projects are collected under `Other`.
- In the main toolbar project widget, projects sharing a parent directory appear in a flat section without a secondary menu.
- Widget sections use bold legend labels embedded in continuous thin borders. Each parent-directory section receives a matching, theme-aware rainbow color.
- Projects without a matching sibling are collected under the neutral gray `Other` section.
- Project icons, names, provider paths, project paths, and Git branches remain visible and searchable. The Git branch keeps its original icon and separate detail line.
- When IntelliJ provides no Git branch, an optional background fallback reads and caches `.git/HEAD`. It is enabled by default, can be disabled independently, and never runs a Git command.
- Open projects, update notices, and widget tool actions retain their platform sections and behavior.
- Project grouping and missing-branch fallback can be toggled independently under `Settings | Tools | Project Directory Grouper`, with another settings entry at the bottom of the project widget.
- Disabling grouping restores the platform lists; platform linkage failures automatically fall back to native behavior.
- The plugin does not recursively scan disks or modify project files. Optional branch fallback only checks `.git` markers and `HEAD` above each project, and does not add, remove, or move Welcome Screen groups.

### Installation And Usage

1. Once a release is published, download its plugin ZIP from GitHub Releases. You can also run the build command below to generate `build/distributions/IntelliJProjectDirectoryGrouper-0.1.0.zip`.
2. In IntelliJ IDEA, open `Settings | Plugins`, click the gear button, choose `Install Plugin from Disk`, and select the plugin ZIP.
3. Open `File | Recent Projects` or the main toolbar project widget to use the grouped project list.
4. Configure grouping and missing Git branch fallback under `Settings | Tools | Project Directory Grouper`.

## Compatibility

| Field | Value |
| --- | --- |
| Plugin ID | `dev.projectgroups.directory-grouper` |
| Language | Kotlin |
| Platform dependency | `com.intellij.modules.platform` |
| Supported builds | `242` through `262.*` |
| Verified target | IntelliJ IDEA Ultimate `IU-262.8665.258` |

## Documentation

The following documents include design history and later-stage plans; they are not a list of currently implemented features.

| Document | Purpose |
| --- | --- |
| [01-product-requirements.md](docs/01-product-requirements.md) | Product scope, requirements, and acceptance criteria |
| [02-grouping-and-ux.md](docs/02-grouping-and-ux.md) | Grouping, ordering, naming, and interaction design |
| [03-technical-design.md](docs/03-technical-design.md) | IntelliJ Platform architecture and compatibility strategy |
| [04-implementation-plan.md](docs/04-implementation-plan.md) | Staged implementation plan |
| [05-test-plan.md](docs/05-test-plan.md) | Unit, integration, compatibility, and manual test matrix |
| [06-platform-research.md](docs/06-platform-research.md) | Verified platform behavior and risks |
| [07-p0-compatibility-results.md](docs/07-p0-compatibility-results.md) | Compatibility results for target builds |
| [AI_IMPLEMENTATION.md](AI_IMPLEMENTATION.md) | AI implementation notes |

## Build And Verification

The project uses Gradle Wrapper 9.6.1. Pass a local IDE installation when validating a target build:

```powershell
$env:JAVA_HOME = 'C:\path\to\IntelliJ IDEA\jbr'
.\gradlew.bat test buildPlugin verifyPluginStructure verifyPlugin '-PlocalPlatformPath=C:\path\to\IntelliJ IDEA'
```

The build targets JVM 21 bytecode. The configured Kotlin toolchain requires a local JDK 25 installation.

There are currently 33 passing tests. Compilation, plugin structure validation, Plugin Verifier compatibility, sandbox installation, and safe action restoration have been checked against `IU-262.8665.258`.

The compatibility layer uses a small set of IntelliJ internal APIs to obtain raw recent-project actions and native project presentation data such as paths and Git branches. These calls are centralized behind native fallbacks. `verifyPlugin` requires the generated internal-API report to match an exact allowlist, so any added or changed internal usage fails verification.
