# git-checkout-sdk
git拉取SDK，对git命令进行包装，其他git插件可以直接引用这个SDK拉取代码
# 新特性
- 性能优化
  - fetch支持拉取指定深度，默认为0，拉取所有提交。指定fetch深度，只会fetch当前的分支，其他分支不会同步
  - 不开启lfs功能，不再checkout大文件
  - 关闭git gc功能
- 安全性优化
  - 密码加密存储。只job运行结束密码会被清理，可以通过设置`persistCredentials:false`密码在插件运行完就清理
  - 子模块密码不再修改.gitsubmodule文件，通过在运行时设置临时全局git配置，子模块拉取结束还原全局配置
- 日志优化
  - fetch和checkout增加进度条
  - 对异常日志进行解析，会输出对应的错误码和描述

# 缺点
- 启用fetchDepth,提交记录日志只能获取`fetchDepth`条的日志记录
- 启用fetchDepth,只会拉取当前分支信息,如果在插件后执行切换分支操作,需要再次fetch
- 不启用lfs功能,lfs文件不再自动拉取


# 使用
## 引入
### gradle
```
compile("com.tencent.bk.devops:git-checkout-sdk:${version}")
```
## 输入适配
需要输入的参数含义:

| 参数                | 描述                                                         |
| ------------------- | ------------------------------------------------------------ |
| bkWorkspace         | 工作空间                                                     |
| pipelineId          | 流水线ID                                                     |
| pipelineTaskId      | 插件ID                                                       |
| pipelineBuildId     | 构建ID                                                       |
| postEntryParam      | 是否启用post Action，启用在job运行完后会清理账号信息         |
| scmType             | 代码库类型，可以是CODE_GIT，CODE_GITLAB、GITHUB、CODE_TGIT   |
| repositoryUrl       | 仓库url                                                      |
| repositoryPath      | 仓库保留的路径，绝对路径                                     |
| ref                 | 分支、tag或commitid                                          |
| pullType            | 拉取类型，BRANCH、TAG、COMMIT_ID。默认为BRANCH               |
| commit              | 从commit拉出分支，只在重试时生效                             |
| pullStrategy        | 拉取策略，FRESH_CHECKOUT、INCREMENT_UPDATE、REVERT_UPDATE    |
| clean               | 是否清理工作空间，只在REVERT_UPDATE下生效                    |
| fetchDepth          | 拉取深度                                                     |
| lfs                 | 开启大文件拉取                                               |
| submodules          | 开启拉取子模块                                               |
| nestedSubmodules    | 是否递归执行子模块                                           |
| submoduleRemote     | 是否同步远程子模块                                           |
| submodulesPath      | 指定子模块拉取，如果仓库中有多个子模块，但是只想拉其中一部分，可以指定 |
| includeSubPath      | sparse checkout包含路径                                      |
| excludeSubPath      | sparse checkout过滤路径                                      |
| username            | 用户名                                                       |
| password            | 密码                                                         |
| privateKey          | ssh 私钥                                                     |
| passPhrase          | ssh密码                                                      |
| persistCredentials  | 是否持久化凭证，如果拉取完代码还要操作仓库，则需要持久化     |
| preMerge            | 是否开启preMerge                                             |
| sourceRepositoryUrl | 源分支url                                                    |
| sourceBranchName    | 源分支                                                       |
| autoCrlf            | autoCrlf                                                     |
| usernameConfig      | 用户名                                                       |
| userEmailConfig     | 邮箱                                                         |

可以参考SDK内置适配器：

| 适配器                              | 对应插件             |
| ----------------------------------- | -------------------- |
| GitCodeAtomParamInputAdapter        | git拉取(命令行)      |
| GitCodeCommandAtomParamInputAdapter | git拉取(命令行)-通用 |
| CheckoutAtomParamInputAdapter       | checkout             |

## 调用

```java
GitCheckoutRunner().run(inputAdapter = inputAdapter, atomContext = context)
```

## 输出

| 参数                                 | 描述                                 |
| ------------------------------------ | ------------------------------------ |
| BK_CI_GIT_REPO_URL                   | 代码库的URL                          |
| BK_CI_GIT_REPO_NAME                  | 代码库的工程名称                     |
| BK_CI_GIT_REPO_BRANCH                | 当前代码库分支                       |
| BK_CI_GIT_REPO_LAST_COMMIT_ID        | 拉取代码时，上次构建最后的commit id  |
| BK_CI_GIT_REPO_HEAD_COMMIT_ID        | 拉取代码时，本次构建最后的commit id  |
| BK_CI_GIT_REPO_HEAD_COMMIT_COMMENT   | 拉取代码时，本次构建最后的commit注释 |
| BK_CI_GIT_REPO_HEAD_COMMIT_AUTHOR    | 本次产生的新的author                 |
| BK_CI_GIT_REPO_HEAD_COMMIT_COMMITTER | 本次产生的新的committer              |
| BK_CI_GIT_REPO_COMMITS               | 本次产生的新的commit id              |

## 开源协同
