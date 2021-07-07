##  背景

蓝盾原来拉取代码会把代码库url转换成http://username:password@git.example.com拉取，这种会将账号密码明文展示，很不安全。还有就是无法将凭证向其他插件传递，经常出现bash中git fetch、go build、pod install等命令无法找不到git凭证，需要额外配置git凭证，用户体验十分不友好。

## git凭证原理

git为了解决用户需要输入多次输入账号密码的问题，在1.7版本定义了凭证管理API，用户只需实现api的store/get/erase接口就能自定义用户名密码的存储。git查找凭证的流程如下:

1. 获取fetch/clone仓库的url，解析出protocol和host
2. 根据protocol和host去凭证管理中查找匹配的protocol和host
3. 如果匹配上，则将获取的用户名密码去git服务器验证账号密码是否正确，如果正确，则将把账号密码再存储到所有的凭证管理中
4. 如果没匹配到，则会调用GIT_ASKPASS、core.askPass、SSH_ASKPASS程序获取密码，如果这几个没有配置，会提示让用户输入账号密码。

![git-credentials架构图](../resource/img/git-credentials.png)

常见的git凭证实现有

- store: git官方提供，账号密码明文存储在~/.git-credentias文件中
- cache: git官方提供，账号密码存储在内存中，可以设置过期时间，windows无法使用
- manager-core: 微软提供的git凭证，windows系统默认的凭证管理。账号密码存储在windows的凭证管理中。mac和linux系统也能使用，不过还是试验版
- osxkeychain：mac系统默认的凭证管理，账号密码存储在钥匙串中，只支持mac系统

## git-checkout-credential设计

目前已有的凭证解决了凭证安全存储，但是无法满足构建时隔离性，兼容性，所以需要在已有的凭证基础上再包装一层，并能根据操作系统自动选择系统自带的凭证管理。需要解决以下问题：

1. 如果同一台构建机运行多个job，job之间拉取代码的凭证如何隔离

   输入http://git.example.com，存储到凭证会变成http://${buildId}.${jobId}.git.example.com，把url转换成带buildId和jobId的url

2. 如果拉取代码使用的是http协议，但是在后续插件中使用https协议拉取，如何兼容，比如go build在运行时就是使用https协议拉取？

   这有两种方式解决，把https协议转换成http协议，比如通过配置`git config --global url.http://git.example.com.insteadOf https://git.example.com`但是这么设计改变了用户环境,不是很友好。所以在存储http协议的凭证时，同时也会把https的凭证存储，就不需要修改任何的配置

3. 凭证在什么时候被清理？

   如果不想把插件的凭证向下传递，可以把`是否持久化凭证`选项去掉,那么在插件执行完就会清理凭证。如果传递了，会在构建完成的post action清理凭证

4. 如果保证配置只在当前构建生效，而不影响其他构建？

   git加载配置路径如下：

   - 加载系统配置，linux在/etc/gitconfig中
   - 加载环境变量${XDG_CONFIG_HOME}/git/.config，${XDG_CONFIG_HOME}默认是为空
   - 加载全局变量，这个配置在用户目录~/.gitconfig
   - 加载仓库的配置，这个在.git/config中

   如果相同的配置后面的会覆盖前面的，因为有些配置必须需要修改全局变量，但又不能直接修改~/.gitconfig，因为修改~/.gitconfig就会影响所有的。所以定义了XDG_CONFIG_HOME变量，所以的配置写到这个文件

最终git-checkout-credential的设计图如下:

![git-checkout-credential架构图](../resource/img/git-checkout-credential.png)

1. git插件调用git-checkout-credential存储凭证，git-checkout-credential再调用系统级的凭证管理真正存储凭证
2. git-checkout-credential在配置的时候，会禁用其他的凭证管理，然后再配置自己
3. git-checkout-credential根据操作系统选择凭证实现

## 参考

1. https://git-scm.com/docs/gitcredentials
2. https://git-scm.com/docs/git-credential
3. https://git-scm.com/docs/git-config
4. https://github.com/microsoft/Git-Credential-Manager-Core
5. https://github.com/microsoft/Git-Credential-Manager-for-Mac-and-Linux