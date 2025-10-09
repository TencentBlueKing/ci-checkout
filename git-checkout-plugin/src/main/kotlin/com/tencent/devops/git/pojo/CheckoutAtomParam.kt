package com.tencent.devops.git.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.bk.devops.atom.pojo.AtomBaseParam
import com.tencent.bk.devops.git.core.enums.AuthType
import lombok.Data
import lombok.EqualsAndHashCode

@Data
@EqualsAndHashCode(callSuper = true)
class CheckoutAtomParam : AtomBaseParam() {
    /**
     * 代码库, 必选, 默认: ID, options: 按代码库选择[ID] | 按代码库别名输入[NAME] | 按仓库URL输入[URL]
     */
    var repositoryType: String = "ID"

    /**
     * 按代码库选择, 当 [repositoryType] = [ID] 时必选
     */
    var repositoryHashId: String? = null

    /**
     * 按代码库别名输入, 当 [repositoryType] = [NAME] 时必选
     */
    var repositoryName: String? = null

    /**
     * 代码库链接, 当 [repositoryType] = [URL] 时必选
     */
    var repositoryUrl: String = ""

    /**
     * 授权类型, 默认: TICKET,
     * 当 [repositoryType] = [URL] 时必选, single,
     *
     * options:
     *
     * EMPTY[空] | TICKET[凭证] | ACCESS_TOKEN[access token] | USERNAME_PASSWORD[username/password] |
     * START_USER_TOKEN[流水线启动人token] | PERSONAL_ACCESS_TOKEN[工蜂personal_access_token]
     */
    var authType: AuthType? = null
    var authUserId: String? = null

    /**
     * 代码库凭证, 当 [repositoryType] = [URL] 和 [authType] = [TICKET] 时必选
     */
    var ticketId: String? = null

    /**
     * access token, 当 [repositoryType] = [URL] 和 [authType] = [ACCESS_TOKEN] 时必选
     */
    var accessToken: String? = null

    /**
     * 工蜂personal_access_token, 当 [repositoryType] = [URL] 和 [authType] = [PERSONAL_ACCESS_TOKEN] 时必选
     */
    var personalAccessToken: String? = null

    /**
     * username, 当 [repositoryType] = [URL] 和 [authType] = [USERNAME_PASSWORD] 时必选
     */
    var username: String? = null

    /**
     * password, 当 [repositoryType] = [URL] 和 [authType] = [USERNAME_PASSWORD] 时必选
     */
    var password: String? = null

    /**
     * 指定拉取方式, 默认: BRANCH, single, options: BRANCH[BRANCH] | TAG[TAG] | COMMIT_ID[COMMIT_ID]
     */
    var pullType: String = "BRANCH"

    /**
     * 分支/TAG/COMMIT, 必选, 默认: master
     */
    var refName: String = "master"

    /**
     * 代码保存路径
     */
    var localPath: String? = null

    /**
     * 拉取策略, 默认: REVERT_UPDATE,
     *
     * options:
     *
     * Revert Update[REVERT_UPDATE] | Fresh Checkout[FRESH_CHECKOUT] | Increment Update[INCREMENT_UPDATE]
     */
    var strategy: String = "REVERT_UPDATE"

    /**
     * git fetch的depth参数值
     */
    var fetchDepth: Int = 0

    /**
     * 启用拉取指定分支, 默认: false
     */
    val enableFetchRefSpec: Boolean = false

    /**
     * 插件配置的分支不需要设置，默认会设置.配置的分支必须存在，否则会报错, 当 [enableFetchRefSpec] = [true] 时必选
     */
    val fetchRefSpec: String? = null

    /**
     * 是否开启Git Lfs, 默认: true
     */
    var enableGitLfs: Boolean = true

    /**
     * lfs并发上传下载的数量
     */
    val lfsConcurrentTransfers: Int = 0

    /**
     * 是否开启Git Lfs清理, 默认: false
     */
    val enableGitLfsClean = false

    /**
     * MR事件触发时执行Pre-Merge, 必选, 默认: true
     */
    var enableVirtualMergeBranch: Boolean = true

    /**
     * 启用子模块, 默认: true
     */
    var enableSubmodule: Boolean = true

    /**
     * 子模块路径当 [enableSubmodule] = [true] 时必选
     */
    var submodulePath: String? = ""

    /**
     * 执行git submodule update后面是否带上--remote参数, 默认: false, 当 [enableSubmodule] = [true] 时必选
     */
    var enableSubmoduleRemote: Boolean = false

    /**
     * 执行git submodule update后面是否带上--recursive参数, 默认: true, 当 [enableSubmodule] = [true] 时必选
     */
    var enableSubmoduleRecursive: Boolean? = false

    /**
     * submodule并发拉取数量
     */
    val submoduleJobs: Int = 0

    /**
     * submodule depth
     */
    val submoduleDepth: Int? = 0

    /**
     * AutoCrlf配置值, 默认: false, single, options: false[false] | true[true] | input[input]
     */
    var autoCrlf: String? = ""

    /**
     * 是否开启Git Clean, 必选, 默认: true, 当 [strategy] = [REVERT_UPDATE] 时必选
     */
    var enableGitClean: Boolean = true

    /**
     * 清理没有版本跟踪的ignored文件, 必选, 默认: true, 当 [strategy] = [REVERT_UPDATE] 和 [enableGitClean] = [true] 时必选
     */
    val enableGitCleanIgnore: Boolean = true

    /**
     * 清理没有版本跟踪的嵌套仓库, 必选, 默认: false, 当 [strategy] = [REVERT_UPDATE] 和 [enableGitClean] = [true] 时必选
     */
    val enableGitCleanNested: Boolean = false

    /**
     * 拉取代码库以下路径
     */
    var includePath: String? = ""

    /**
     * 排除代码库以下路径
     */
    var excludePath: String? = ""

    // 非前端传递的参数
    @JsonProperty("BK_CI_START_TYPE")
    val pipelineStartType: String? = null
    @JsonProperty("BK_CI_HOOK_EVENT_TYPE")
    val hookEventType: String? = null
    @JsonProperty("BK_CI_HOOK_SOURCE_BRANCH")
    val hookSourceBranch: String? = null
    @JsonProperty("BK_CI_HOOK_TARGET_BRANCH")
    val hookTargetBranch: String? = null
    @JsonProperty("BK_CI_HOOK_SOURCE_URL")
    val hookSourceUrl: String? = null
    @JsonProperty("BK_CI_HOOK_TARGET_URL")
    val hookTargetUrl: String? = null
    @JsonProperty("BK_CI_GIT_MR_NUMBER")
    val gitMrNumber: String? = null

    // 重试时检出的commitId
    var retryStartPoint: String? = ""

    /**
     * 是否持久化凭证, 默认: true
     */
    var persistCredentials: Boolean? = true

    /**
     * 是否开启调试, 必选, 默认: false
     */
    var enableTrace: Boolean? = false

    /**
     * 是否开启部分克隆,部分克隆只有git版本大于2.22.0才可以使用
     */
    var enablePartialClone: Boolean? = false

    /**
     * 归档的缓存路径
     */
    val cachePath: String = ""
    val usernameConfig: String? = null
    val userEmailConfig: String? = null

    /**
     * 启用工蜂缓存加速
     */
    val enableTGitCache: Boolean = false
    /**
     * 是否设置安全目录
     */
    val setSafeDirectory: Boolean? = false
    /**
     * 是否为源材料主仓库
     */
    val mainRepo: Boolean? = false
    /**
     * 启用服务器预合并
     */
    val enableServerPreMerge: Boolean? = false
}
