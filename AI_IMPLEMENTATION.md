# AI 实施说明

你要在此目录中实现一个 IntelliJ Platform 插件。开始编码前，完整阅读 `README.md` 和 `docs/` 中全部文档；它们共同构成功能合同。

## 最重要的约束

1. IDE 内两个菜单使用虚拟分组，绝不能因为设置默认开启而修改欢迎页原生 `ProjectGroup`。
2. 欢迎页只有用户点击、预览并确认后才能写入原生组。
3. 自动整理不能覆盖用户组；所有权不确定时必须跳过。
4. 设置关闭、适配失败或插件卸载时，原生动作必须仍然可用。
5. implementation API 只允许出现在 `platform` 兼容层。
6. 不扫描磁盘、不访问网络、不记录完整项目路径。
7. 不通过反射读取 private 字段来规避平台版本差异。

## 实施顺序

严格按以下顺序推进：

1. 完成 P0 spike，并新增 `docs/07-p0-compatibility-results.md` 记录真实结果。
2. 建立 Gradle 插件骨架、`plugin.xml`、中英文资源和测试基础设施。
3. 先实现无 IntelliJ 依赖的分组核心与完整单元测试。
4. 实现默认开启的设置和动作替换生命周期。
5. 实现最近项目虚拟分组。
6. 实现打开项目虚拟分组与窗口激活。
7. 实现欢迎页预览、所有权模型和原生组增量写入。
8. 完成跨版本 verifier、人工 smoke test 和动态卸载验证；最高目标必须包含 `IU-262.8665.258`。

不要跳过 P0 直接假设 action ID、类签名或动态更新行为稳定。

## 代码质量要求

- 领域逻辑使用不可变数据和纯函数。
- UI、平台适配、持久化分别隔离。
- 用户可见文本全部在 resource bundle。
- Action `update()` 快速、无磁盘 I/O、无状态写入。
- 捕获兼容性错误时记录可诊断上下文，但不吞掉普通编程错误。
- 对高风险平台适配写契约测试；对路径与计划算法写表驱动测试。
- 不复制平台类的完整实现，不使用未解释的魔法字符串；所有平台 action ID 集中定义并附源码依据。

## 每阶段验证

根据生成的 Gradle 配置使用对应任务，至少完成：

```text
test
verifyPluginProjectConfiguration
verifyPlugin
runPluginVerifier
runIde
```

如果任务名随 IntelliJ Platform Gradle Plugin 版本变化，应查阅该版本官方文档并更新本文件，不能假装已运行不存在的任务。

## 完成交付内容

- 可构建的插件源码。
- 领域和平台集成测试。
- `docs/07-p0-compatibility-results.md`。
- 目标产品/build 的 Plugin Verifier 报告摘要。
- 人工 smoke test 记录，至少包含 IDEA、PyCharm、WebStorm、Android Studio。
- Marketplace 说明、变更记录和隐私说明。

最终汇报必须明确列出：已验证的 IDE/build、未验证范围、已知降级和仍依赖的内部 API。仅“编译成功”不等于功能完成。
