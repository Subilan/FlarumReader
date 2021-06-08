<img src="https://sotapmc.oss-cn-beijing.aliyuncs.com/img/logo/flarumreader.svg" draggable="false" width="300px"/>

[![Java CI with Gradle](https://github.com/sotapmc/FlarumReader/actions/workflows/gradle.yml/badge.svg)](https://github.com/sotapmc/FlarumReader/actions/workflows/gradle.yml)
![](https://img.shields.io/badge/11-darkred?logo=java)
![](https://img.shields.io/badge/poweredby-sotapmc-blue)
![](https://img.shields.io/badge/API-1.16.5-R0.1-orange)

**FlarumReader** 是一个用于在 Minecraft 服务器内阅读 Flarum 论坛内容并进行交互的插件。目前已经实现的功能有
- 以 10 个为单位，分页获取论坛所有帖子（不同用户的权限会有不同的结果，这一点由 API 决定）。至于单位个数将在以后提供配置。
- 使用书本 GUI 查看指定的帖子，并尽可能解析帖子的 Markdown。
  - 支持**粗体**、*斜体*、<u>下划线</u>（`<u></u>`）、~~删除线~~、***粗斜体***、[超链接](#)。图片（`![]()`）会被解析为文本 `[图片]`。
  - 解析论坛特有格式。目前支持 @Mention、@Mention#ID，均解析为绿色字符。*TODO：解析为相应超链接。*
- 回复指定的帖子
  - 支持纯文本回复（聊天框）和书本回复（获取书本完整文本内容）
- 发布新帖子
  - 仅支持书本回复
- 下载帖子
  - 相当于是把帖子内容获取后写入到书中，再给予玩家
  
[下载最新构建](https://nightly.link/sotapmc/FlarumReader/workflows/gradle/master/FlarumReader-latest.zip)
  
## 配置文件

```yml
# 论坛网站地址，末尾不带斜杠。
site: ~
# 管理员账号用户名
admin-name: ~
# 管理员账号密码
admin-password: ~
# 获取最大用户数据量
max-users: 500
```

注：`max-users` 项代表着插件最多获取的用户名数量。每次服务器开启，插件加载时，将会发送 `Math.ceil(maxUsers/50)` 个异步请求用于获取用户名及其对应的 ID。这是因为 Flarum API 的大部分返回值里对用户的描述都是基于 ID，所以为了显示对应的用户名，最好一次性获取所有需要的用户名及其 ID 的对应关系。而 Flarum API 单次只允许获取 50 个因此需要分开获取且需要规定一个最大上限。当此值小于论坛注册人数时，部分玩家的用户名会显示为 `null`。

## 链接

- [upperlevel/book-api](https://github.com/upperlevel/book-api)
- [SoTap Wiki - FlarumReader](https://wiki.sotap.org/#/plugins/flarum-reader)

## 协议

MIT

