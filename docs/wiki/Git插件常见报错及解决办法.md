## 一、授权失败

### 错误信息：

- The requested URL returned error: 403
- fatal: could not read Username for '[http://git.example.com/demo.git/](http://oauth2:***@git.example.com/demo.git/)': terminal prompts disabled
- fatal: Authentication failed for '[http://git.example.com/demo.git/](http://oauth2:***@git.example.com/demo.git/)'
- fatal: Could not read from remote repository.
- fatal: Authentication failed
- fatal: [http://git.example.com/demo.git/](http://oauth2:***@git.example.com/demo.git/)‘ 鉴权失败
- fatal: 无法读取远程仓库。
- fatal: repository '(.+)' not found
- fatal: Git repository not found
- fatal: 远程错误：Git repository not found
- ERROR: Repository not found
- fatal: remote error: Git:Project not found.
- fatal: could not read Username for '[http://git.example.com/demo.git/](http://oauth2:***@git.example.com/demo.git/)': No such device or address

以上报错都是权限问题导致的，出现这种报错的原因是：

1. 如果流水线已经运行了很长时间，而且流水线也没有做过更改，就要检查拉取代码的人是否离职、转岗或者已经不是代码库成员
2. 如果是在配置流水线，则要检查插件配置的人是否有仓库权限，或者用户名密码是否配置正确
3. 如果是fork仓库，拉取代码的人不是fork仓库成员。

### 解决方法:

先检查拉取代码使用的是谁的身份，查看插件代码库的配置

![img](../resource/img/repositoryType.png)

1. 如果使用的是“按代码库选择”或“按代码库别名输入”，则拉取代码使用的是代码库配置的授权人，在"蓝盾代码"库搜索选择的代码库，查看授权身份

![img](../resource/img/repositoryInfo.png)

- 如果代码库授权身份是oauth类型，则检查授权的用户是否有拉取仓库的权限，如果不是，请让是仓库成员的同学重新编辑代码库，那么授权身份就会变成最后编辑的同学
- 如果代码库授权身份是凭证类型，那么检查凭证中配置的用户名密码或者公私钥是否正确
- 如果是fork仓库，把拉取代码的人加入到fork仓库成员，并给developer以上权限

2. 如果使用的是“按仓库URL输入”，则检查凭证类型是否正确

   ![img](../resource/img/authType.png)

- 凭证：使用选择的凭证拉取代码
- access token：工蜂服务器选择这种少，gitlab使用的多
- username/password：检查用户密码是否正确，建议使用凭证的方式，这种方式流水线编排用户名密码会明文展示
- 流水线启动人：通过获取流水线启动人的token拉取代码，检查流水线启动人是否有拉取仓库的权限

## 二、远程服务端异常

### 错误信息：

- fatal: (the|The) remote end hung up unexpectedly
- fatal: unable to access '[http://git.example.com](http://git.example.com/)': The requested URL returned error: 502
- fatal: 远程错误：Internal server error
- fatal: 远端意外挂断了
- Git:Server is busy, please try again later
- fatal: unable to access '[http://git.example.com](http://git.example.com/)': Failed to connect to [http://git.example.com](http://git.example.com/): Host is down

以上报错都是服务端异常导致，出现这种报错的原因可能是服务端不稳定或者网络不稳定导致

## 三、分支或commit不存在

### 错误信息：

- fatal: couldn't find remote ref origin/refs/master
- fatal: 无法找到远程引用 origin/refs/master
- Your configuration specifies to merge with the ref 'origin/refs/master'
- 您的配置中指定要合并远程的引用 'origin/refs/master'
- fatal: 'origin/refs/master' is not a commit and a branch 'master' cannot be created from it
- fatal: 'origin/refs/master' 不是一个提交，不能基于它创建分支 'master'
- error: pathspec 'master' did not match any file(s) known to git
- error: 路径规格 'master' 未匹配任何 git 已知文件
- fatal: path 'master' does not exist .
- fatal: 路径 'master' 不存在
- fatal: 引用不是一个树：bff558ed86b2696d902ef6704792188105a52502
- fatal: reference is not a tree: bff558ed86b2696d902ef6704792188105a52502

以上报错是在执行checkout命令时报错，出现这个错的原因是因为分支或者commitId配置错误导致的，需要检查插件的分支配置

![img](../resource/img/branchConfig.png)

常见原因有：

1. 插件配置的分支在远端不存在或者已经被删除
2. 插件开启了preMerge功能，事件监听配置的Merge Request Accept Hook事件，合并后源分支就被删除

## 四、checkout的分支名为空

### 错误信息：

- fatal: You are on a branch yet to be born
- fatal: 您位于一个尚未初始化的分支

以上报错是checkout指定的分支为空导致。常见原因：

1. 插件分支配置的是一个变量的值，但是启动时没有给变量设置
2. 插件分支配置的是一个事件触发的变量，但是启动使用的是手工触发

### 解决办法：

1. 如果是变量没有设置，设置变量重新执行
2. 如果配置的是事件触发的变量，需要兼容手动触发的情况，需要把事件触发的变量设置成流水线启动变量，这样手动触发直接指定变量就可以。

![img](../resource/img/pipelineVariable.png)

## 五、部分检出错误,请检查部分检出的文件是否存在

### 错误信息：

- error: Sparse checkout leaves no entry on working directory

这个错误是因为配置了“拉取相对子路径"或”排除代码库以下路径“导致

![img](../resource/img/sparseCheckoutConfig.png)

报错原因是配置的路径在仓库中不存在，如果有时能 运行成功，有时运行不成功，则需要看路径是不是在有的分支存在，有的分支不存在

### 解决办法：

要使用部分检出的功能，则一定要确保配置的路径一定存在，如果出现可能不存在的情况，则不能开启这个功能

## 六、分支或路径名冲突

### 错误信息：

- fatal: 'test' 既可以是一个本地文件，也可以是一个跟踪分支。

这个错误是因为分支名和仓库中的文件或目录重名了，导致git不知道到底是切换分支还是切换路径

### 解决办法：

一般路径很难重命名，常规做法是把分支名改了

## 七、merge冲突，请先解决冲突然后再构建

### 错误信息：

- Automatic merge failed; fix conflicts and then commit the result.
- Resolve all conflicts manually, mark them as resolved with
- 自动合并失败，修正冲突然后提交修正的结果。

这个错误信息比较常见，有疑问的点是为什么插件会执行merge命令，因为插件开启了preMerge功能，当在MR事件触发，并且拉取的仓库和触发的仓库是同一个时，会把源分支和目标分支进行合并

## 八、合并失败，可能是因为源分支被删除导致

### 错误信息：

- merge: test - not something we can merge
- merge：test - 不能合并

这个错误一般出现在配置Merge Request Accept Hook事件，并且合并后把源分支删除的场景

### 解决办法:

Merge Request Accept Hook事件触发不需要开启Premerge，可以直接关掉。插件目前已经做了处理

## 九、合并失败，两个分支没有共同的祖先,导致无法合并

### 错误信息：

- fatal: refusing to merge unrelated histories
- fatal: 拒绝合并无关的历史

这个错误基本都是因为配置了depth=1导致的，因为源分支和目标分支都只fetch了一个commit，导致两条分支之间没有交际，无法进行合并

### 解决办法：

1. 去掉depth=1这个功能，如果需要preMerge功能，则只能使用全量拉取
2. 使用checkout插件，目前插件已经兼容了这个场景

## 十、切换分支失败，本地修改的文件将被覆盖

### 错误信息：

- error: Your local changes to the following files would be overwritten by merge:
- error: Your local changes to the following files would be overwritten by checkout:
- error: 您对下列文件的本地修改将被检出操作覆盖：

这个错误是因为工作空间有未提交的文件，切换时会导致修改的文件丢失。在ci场景，可以采取强制切换的方式，目前插件已经强制切换，所以应该不会报这个异常，如果checkout插件再报，可以联系devops-helper排查原因

### 解决办法:

1. checkout增加-f强制切换，插件已支持此功能
2. 插件先改成fresh checkout，然后再改成revert checkout

## 十一、子模块更新失败

### 错误信息：

- fatal: No submodule mapping found in .gitmodules for path 'aaa'
- fatal: 在 .gitmodules 中没有发现路径 'aaa' 的子模组映射
- fatal: 在 .gitmodules 中未找到子模组 'aaa' 的 url
- fatal: No url found for submodule path 'aaa' in .gitmodules

出现这个错误是因为移除子模块没有移除干净导致的，删除了.gitmodule中的配置，但是没有删除子模块路径，这个问题本地也能重现，

### 解决办法:

删除对应的子模块目录，并提交

git rm -r xxx --cached

git commit 

## 十二、子模块配置的仓库不存在

### 错误信息：

- Clone of 'http://git.example.com/demo.git' into submodule path 'demo' failed
- fatal：无法克隆 'http://git.example.com/demo.git' 到子模组路径 'demo'

克隆子模块失败，子模块配置的仓库不存在导致

## 十三、子模块指向的commit不存在

### 错误信息：

- Fetched in submodule path '(.+)', but it did not contain (.+). Direct fetching of that commit failed.
- 获取了子模组路径 'aaa'，但是它没有包含 (.+)。直接获取该提交失败。
- 无法在子模组路径 'aaa' 中找到当前版本
- fatal: Needed a single revision

出现这个错误的原因一般是在主库push的代码，但是没有把子模块的提交push到远程

### 解决办法：

把子模块的提交push到远程

## 十四、Git LFS文件下载错误

### 错误信息：

- Error downloading object: .*
- LFS: Repository or object not found: .+

出现这个错误信息是因为大文件没有push到远端，导致获取大文件报错

### 解决办法：

找到对应的大文件，重新push，然后重新构建

## 十五、LFS程序没有安装

### 错误信息：

- git: 'lfs' is not a git command. See 'git --help'.

git lfs命令没有安装导致的，

### 解决办法：

1. 如果仓库使用到大文件，则需要在构建机或者镜像中安装git lfs程序
2. 如果仓库没有使用大文件，关闭插件的lfs功能。 

![img](../resource/img/lfsConfig.png)
