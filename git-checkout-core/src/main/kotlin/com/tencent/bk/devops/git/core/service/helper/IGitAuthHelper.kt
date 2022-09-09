/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.devops.git.core.service.helper

interface IGitAuthHelper {

    /**
     * 清理上一次构建执行的授权
     *
     * 当用户取消时,post action可能没有执行导致auth没有清理,应该清理上一次构建执行的凭证
     */
    fun removePreviousAuth()

    /**
     * 配置当前仓库凭证
     */
    fun configureAuth()

    /**
     * 移除当前仓库凭证
     */
    fun removeAuth()

    /**
     * 配置全局凭证，用于拉取子模块时使用
     *
     */
    fun configGlobalAuth()

    /**
     * 移除全局凭证
     */
    fun removeGlobalAuth()

    /**
     * 配置子模块凭证，用户拉取完子模块后，再配置子模块的凭证
     */
    fun configureSubmoduleAuth()

    fun removeSubmoduleAuth()
}
