git-checkout-credential是一个自定义git凭证管理， 为了解决蓝盾拉取代码插件中凭证无法向下传递。它具有以下特性：

- 传递性: git插件中配置的凭证可以向其他插件传递，使其他插件可以执行git拉取的功能
- 安全性: 传递的凭证不会明文展示，linux存储在内存中，mac存储在钥匙串中，windows存储在凭证管理中,构建完成销毁凭证
- 隔离性: 同一台构建机同时运行多个构建，每次构建之间的凭证是完全隔离的
- 可见性：只对当前构建环境有效，不影响用户构建机环境
- 兼容性：如果拉取的是http协议的代码，https协议的凭证也会保存

## 如何使用

git-checkout-credential无法单独使用，必须嵌入到git-checkout-core，由git-checkout-core安装和调用。它以jar包的形式存储在git-checkout-core的script目录中。

## 注意事项
1. 建议构建机不要添加global级别的凭证管理，比如不要配置`git config --global credential.helper store`。这是因为git在验证凭证有效后，会自动调用所有的凭证管理存储，如果设置就会导致凭证明文存储或者修改构建机的凭证。
2. 如果同时使用两个拉取代码插件，后面一个插件的凭证会覆盖前面一个，比如checkout A -> checkout B -> bash git fetch A,这里fetch A用的是chekcout B配置的凭证，所以同一个job的拉取代码插件使用同一身份。

## 设计文档

[git-checkout-credential设计](../docs/wiki/credential设计文档.md)
