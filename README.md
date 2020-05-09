# 通用git插件

# 编译配置
0. gradle 4.8 ~ 4.10
1. 生成一个token在[account settings page](https://github.com/settings/tokens)
2. 修改gradle.properties文件中的MAVEN_CRED_USERNAME和MAVEN_CRED_PASSWORD
或者在gradle命令运行时增加-DmavenCredUserName和-DmavenCredPassword
- mavenCredUserName：github用户名
- mavenCredPassword：第一步生成的token
