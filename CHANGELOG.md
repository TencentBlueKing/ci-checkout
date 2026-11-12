# Changelog

# v2.0.18
## 2026-11-12
### 新增
- feat: 插件记录提交日志时兼容MR_ACC事件 #58 [链接](https://github.com/TencentBlueKing/ci-checkout/issues/58)
- feat: 支持github公开仓库镜像拉取 #65 [链接](https://github.com/TencentBlueKing/ci-checkout/issues/65)
- feat: 增加Client-Agent请求头 #75 [链接](https://github.com/TencentBlueKing/ci-checkout/issues/75)
- feat: 插件上展示拉取的代码库链接 #81 [链接](https://github.com/TencentBlueKing/ci-checkout/issues/81)
- feat: 插件增加短commit输出 #86 [链接](https://github.com/TencentBlueKing/ci-checkout/issues/86)
- feat: 清理.git残留锁文件AUTO_MERGE.lock #94 [链接](https://github.com/TencentBlueKing/ci-checkout/issues/94)
- feat: 开启部分克隆,卡在获取日志步骤 #95 [链接](https://github.com/TencentBlueKing/ci-checkout/issues/95)
- feat: 代码库类型支持“拉取触发构建的代码库” #96 [链接](https://github.com/TencentBlueKing/ci-checkout/issues/96)

### 修复
- bug: git凭证并发控制 #61 [链接](https://github.com/TencentBlueKing/ci-checkout/issues/61)
- bug: read-tree命令禁用lfs拉取 #71 [链接](https://github.com/TencentBlueKing/ci-checkout/issues/71)
- bug: 开启部分克隆然后再关掉,下次构建还是使用部分克隆 #74 [链接](https://github.com/TencentBlueKing/ci-checkout/issues/74)
- bug: 拉取 submodule 时临时全局配置隔离了用户的 http 代理配置，导致网络超时 #77 [链接](https://github.com/TencentBlueKing/ci-checkout/issues/77)
- bug: submodule 循环依赖插件会卡住 #79 [链接](https://github.com/TencentBlueKing/ci-checkout/issues/79)

### 优化
- perf: sparse cone模式优化post-action阶段代码以及异常文案 #52 [链接](https://github.com/TencentBlueKing/ci-checkout/issues/52)
- perf: 升级okhttp从4.9.0到4.9.2 #60 [链接](https://github.com/TencentBlueKing/ci-checkout/issues/60)
- perf: 修改copyright #88 [链接](https://github.com/TencentBlueKing/ci-checkout/issues/88)
- perf: bcprov升级到1.78 #90 [链接](https://github.com/TencentBlueKing/ci-checkout/issues/90)
- perf: Git 错误信息识别经常漏匹配、误分类，需要优化 GitErrors 的匹配机制 #97 [链接](https://github.com/TencentBlueKing/ci-checkout/issues/97)
