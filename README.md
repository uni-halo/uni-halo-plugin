<p align="center">
    <a href="https://uni-halo.ialley.cn" target="_blank" rel="noopener noreferrer">
        <img width="100" src="https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static/logo.png" alt=UniHalo v3.x" />
    </a>
</p>

<p align="center"><b>UniHalo v3.x 配置插件</b></p>

<p align="center">为跨平台多端应用提供的官方配套插件，Halo 2.x 站点装上即用。</p>

<br />
<p align="center">
	<a href="https://github.com/uni-halo/uni-halo-plugin">插件源码</a>
	<a href="https://github.com/uni-halo/uni-halo">应用源码</a>
	<a href="https://www.halo.run/store/apps/app-aukgwe3y">应用市场</a>
	<a href="https://uni-halo.ialley.cn">官网主页</a>
	<a href="https://uni-halo-doc.ialley.cn">官方文档</a>
	<a href="https://www.xiaoxiaomo.cn">作者主页</a>
	<a href="https://blog.xiaoxiaomo.cn">作者博客</a>
</p>

---

## 📖 介绍

这是一个免费开源的跨平台多端应用（UniHalo）配置插件，基于 uni-app 与 Halo 2.x API 构建。一套源码，编译为微信小程序、APP 与 H5，Uni Halo 由两部分组成，当前为 **插件端**，配套使用：

- **应用端 `uni-halo`**：移动端源码，编译为微信小程序（推荐）、APP 等。[应用源码](https://github.com/uni-halo/uni-halo-plugin)
- **插件端 `uni-halo-plugin`**：UniHalo配置插件（Halo插件），提供全部后台配置能力 [插件源码](https://github.com/uni-halo/uni-halo)

注意：**v3.0.0 是全新版本**，与 v1.x / v2.x 无任何关联，不存在升级路径。插件为独立新插件，旧配置无法继承，也不支持旧版本的 **uni-halo** 源码。

## ✨ 特性一览

|     | 特性           | 说明                                                                 |
| :-: | -------------- | -------------------------------------------------------------------- |
| 🧩  | **零代码配置** | 页面内容、功能开关全部在插件控制台维护，无需改动应用源码             |
| 💕  | **恋爱日记**   | 相册（密码锁定，页内解锁）/ 清单 / 故事，专为秀恩爱设计              |
| 🖼️  | **主题模板**   | 内置恋爱日记主题模板，任何主题零成本接入，支持整页接管或覆盖页头页脚 |
| 🔐  | **移动端登录** | 账号密码登录、注册、微信一键登录与绑定，下发 Halo 原生令牌           |
| 📲  | **移动端管理** | 管理员可在手机上直接管理恋爱相册、清单、故事与瞬间                   |
| 🛡️  | **安全控制**   | 验证码防护、登录限流保护、按角色控制菜单与按钮可见性                 |
| 🆓  | **完全开源**   | 应用与插件源码全部开放，Apache License 2.0                           |

## 🧩 功能模块


### 应用端 · 内容展示

| 完成状态 | 功能名称   | 描述                                       |
| :------: | ---------- | ------------------------------------------ |
| ✅ 已完成 | 首页       | 轮播图、快捷导航、分类入口、公告           |
| ✅ 已完成 | 分类       | 全部分类聚合，支持筛选与订阅               |
| ✅ 已完成 | 图库       | 我的图库与收藏                             |
| ✅ 已完成 | 瞬间       | 瞬间列表与详情                             |
| ✅ 已完成 | 博主       | 博主主页，登录后进入个人中心               |
| ✅ 已完成 | 文章       | 文章列表、详情、时间归档、标签聚合         |
| ✅ 已完成 | 搜索       | 关键词搜索文章 / 瞬间 / 图库               |
| ✅ 已完成 | 公告       | 公告中心与公告详情                         |
| ✅ 已完成 | 友情链接   | 友链分组展示与投稿申请                     |
| ✅ 已完成 | 投票       | 投票中心与投票详情                         |
| ✅ 已完成 | 数据看板   | 站点数据可视化                             |
| ✅ 已完成 | 账号登录   | 账号密码登录、注册、微信一键登录与微信绑定 |
| ✅ 已完成 | 维护模式   | 站点维护期间统一提示页                     |
| ✅ 已完成 | 偏好设置   | 主题、字号等应用偏好                       |

### 应用端 · 内容管理

登录后按角色可见，管理员可直接在手机上维护内容，不必打开电脑。

| 完成状态 | 功能名称 | 描述                      |
| :------: | -------- | ------------------------- |
| ✅ 已完成 | 相册管理 | 移动端管理恋爱相册        |
| ✅ 已完成 | 日记管理 | 移动端管理恋爱清单 / 日记 |
| ✅ 已完成 | 故事管理 | 移动端管理恋爱故事        |
| ✅ 已完成 | 瞬间管理 | 移动端发布与管理瞬间      |

### 管理端（插件控制台）

内容与安全策略都在 Halo 后台维护，改完即生效。

| 完成状态 | 功能名称 | 描述                                               |
| :------: | -------- | -------------------------------------------------- |
| ✅ 已完成 | 横幅管理 | 小程序轮播图配置                                   |
| ✅ 已完成 | 公告管理 | 公告类型与公告内容                                 |
| ✅ 已完成 | 友链管理 | 友链分组、友链信息、投稿审核，支持审核结果邮件通知 |
| ✅ 已完成 | 恋爱管理 | 恋爱相册、恋爱清单、我们的故事                     |
| ✅ 已完成 | 应用管理 | 应用信息与版本维护                                 |
| ✅ 已完成 | 审核配置 | 投稿内容的候选与审核策略                           |
| ✅ 已完成 | 插件设置 | 验证码安全控制、平台接入、主题悬浮窗、移动端登录开关 |

![控制台首页](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static@main/screenshots/plugin/v3.x/%E6%8E%A7%E5%88%B6%E5%8F%B0%E9%A6%96%E9%A1%B5.png)

---

## 界面预览

### 主要界面

| 首页 | 分类 | 图库 | 瞬间 | 博主 |
|:---:|:---:|:---:|:---:|:---:|
| ![首页](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static@main/screenshots/app/v3.x/%E9%A6%96%E9%A1%B5.png) | ![分类](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static@main/screenshots/app/v3.x/%E5%88%86%E7%B1%BB.png) | ![图库](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static@main/screenshots/app/v3.x/%E5%9B%BE%E5%BA%93.png) | ![瞬间](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static@main/screenshots/app/v3.x/%E7%9E%AC%E9%97%B4.png) | ![博主](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static@main/screenshots/app/v3.x/%E5%8D%9A%E4%B8%BB.png) |

### 恋爱模块

| 恋爱日记 | 恋爱相册 | 恋爱清单 | 我们的故事 |
|:---:|:---:|:---:|:---:|
| ![恋爱日记](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static@main/screenshots/app/v3.x/%E6%81%8B%E7%88%B1%E6%97%A5%E8%AE%B0.png) | ![恋爱相册](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static@main/screenshots/app/v3.x/%E6%81%8B%E7%88%B1%E7%9B%B8%E5%86%8C.png) | ![恋爱清单](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static@main/screenshots/app/v3.x/%E6%81%8B%E7%88%B1%E6%B8%85%E5%8D%95.png) | ![恋爱故事](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static@main/screenshots/app/v3.x/%E6%81%8B%E7%88%B1%E6%95%85%E4%BA%8B.png) |

恋爱相册支持查看密码锁定、页内解锁，密码在服务端校验，暴力尝试会被验证码拦截。

### 主题模板

插件内置恋爱日记前台模板，任何主题装上插件即可展示，支持整页接管或仅覆盖页头页脚。

| 恋爱日记主页 | 恋爱相册 | 恋爱清单 | 我们的故事 |
|:---:|:---:|:---:|:---:|
| ![前台模板](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static@main/screenshots/plugin/v3.x/%E5%89%8D%E5%8F%B0%E6%A8%A1%E6%9D%BF.png) | ![前台模板-恋爱相册](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static@main/screenshots/plugin/v3.x/%E5%89%8D%E5%8F%B0%E6%A8%A1%E6%9D%BF-%E6%81%8B%E7%88%B1%E7%9B%B8%E5%86%8C.png) | ![前台模板-恋爱清单](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static@main/screenshots/plugin/v3.x/%E5%89%8D%E5%8F%B0%E6%A8%A1%E6%9D%BF-%E6%81%8B%E7%88%B1%E6%B8%85%E5%8D%95.png) | ![前台模板-恋爱故事](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static@main/screenshots/plugin/v3.x/%E5%89%8D%E5%8F%B0%E6%A8%A1%E6%9D%BF-%E6%81%8B%E7%88%B1%E6%95%85%E4%BA%8B.png) |

---

> 更多页面截图与功能演示，请访问 [官网](https://uni-halo.ialley.cn) 或 [官方文档](https://uni-halo-doc.ialley.cn)。

# 🚀 快速开始

## 🔗 配套应用 （uni-halo）

下载体验：[应用仓库](https://github.com/uni-halo/uni-halo) ｜ [官方文档](https://uni-halo-doc.ialley.cn/guide/introduction)

### 环境要求

- [Halo](https://www.halo.run/) ≥ 2.26（站点程序）
- 配套应用：[uni-halo](https://github.com/uni-halo/uni-halo) v3.x（Node.js ≥ 20、pnpm ≥ 9）

### 安装与配置


- 1、部署 uni-halo 应用：克隆 [应用源码](https://github.com/uni-halo/uni-halo)，参考 [部署指南](https://uni-halo-doc.ialley.cn/deploy/intro) 在本地运行起来。
- 2、在 Halo 插件市场搜索 `UniHalo` 安装启用，或从 [插件 Releases](https://github.com/uni-halo/uni-halo-plugin/releases) 下载安装包上传安装。
- 3、插件启动后进入插件配置页，按 [插件配置文档](https://uni-halo-doc.ialley.cn/deploy/config) 完成参数配置即可。

三步即可跑起来。

**1. 安装插件**

Halo 后台安装 `Uni Halo` 插件（要求 Halo 2.26.0 及以上），启用后在插件配置页完成设置。

**2. 配置应用**

获取应用源码 https://github.com/uni-halo/uni-halo ，安装依赖后在 `env/.env` 中填入小程序 AppID 与站点地址：

```bash
pnpm install
```

```ini
VITE_WX_APPID = '你的微信小程序AppID'
VITE_SERVER_BASEURL = 'https://你的域名'
VITE_SERVER_BASEURL__WEIXIN_RELEASE = 'https://你的域名'
```

**3. 构建发布**

```bash
pnpm build:mp    # 微信小程序（推荐）
pnpm build:app   # APP
pnpm build:h5    # H5
```

构建完成后，用微信开发者工具上传代码、提交审核，审核通过即可发布上线。

完整的图文部署流程见官方文档：https://uni-halo-doc.ialley.cn/deploy/full-content

## 📚 使用文档

- [插件配置](https://uni-halo-doc.ialley.cn/deploy/config)：插件设置（基本设置、安全控制、平台接入、主题展示、移动端登录）说明
- [控制台功能](https://uni-halo-doc.ialley.cn/plugin/console)：内容管理（横幅、公告、友链、恋爱日记等）使用说明
- [移动端登录 · 使用与配置](https://uni-halo-doc.ialley.cn/plugin/mobile-login)：站点管理员如何开通与配置登录能力
- [移动端登录 · 接口文档](https://uni-halo-doc.ialley.cn/plugin/mobile-login-api)：App / 小程序端如何对接
- [恋爱日记主题模板](https://uni-halo-doc.ialley.cn/plugin/love-template)：主题接入说明

## 🔒 隐私与数据说明

插件仅在你启用对应功能时处理以下数据，全部保存在你的站点数据库中，**不做任何遥测、统计、崩溃上报或跨站追踪**：

| 数据 | 用途 | 存储位置 |
| --- | --- | --- |
| 用户名 / 昵称 / 邮箱 | 移动端注册与登录（沿用 Halo 用户体系） | Halo `User` 扩展 |
| 微信 openid / unionid | 微信一键登录与绑定的身份标识 | Halo `UserConnection` 扩展 |
| 站长配置（横幅 / 公告 / 友链 / 恋爱日记等） | 站点内容管理 | 插件自定义模型与 ConfigMap |
| 站点签名密钥 | 恋爱模块解锁与注册票据的 HMAC 签名（启动时随机生成） | 插件私有 ConfigMap |

- 微信登录凭据（AppID / AppSecret）保存在 Halo `Secret` 资源中，不落入插件配置明文，也不下发到移动端。
- 密码不出现在通知、日志或任何对外接口中：微信自动注册的账号不提供可告知的初始密码，由用户在客户端内自行设置。
- 数据的删除与导出沿用 Halo 自身能力（删除用户、解绑微信、后台内容管理）。

## 🌐 外部服务说明

插件运行期的出站请求仅限以下两类，均可通过关闭对应功能停用：

- **微信开放平台（`api.weixin.qq.com`）**：仅在站长开启「小程序一键登录」并配置微信凭据后调用 `jscode2session`，将小程序临时登录凭证换取微信用户标识（openid / unionid）。请求仅携带 AppID、AppSecret 与一次性 `code`，响应中的会话密钥（session_key）不存储、不使用。关闭微信登录后不再发起任何请求。
- **SMTP 邮件服务（站点自配）**：仅在功能触发时经由站点已配置的邮件发送器发送注册验证码与站内通知，收件人为站点用户邮箱。

除上述外，插件不连接任何第三方服务。

## 📚 相关资源

| 资源          | 地址                                         |
| ------------- | -------------------------------------------- |
| 作者主页      | https://www.xiaoxiaomo.cn                    |
| 作者博客      | https://blog.xiaoxiaomo.cn                   |
| 官网主页      | https://uni-halo.ialley.cn                   |
| 官方文档      | https://uni-halo-doc.ialley.cn               |
| 插件源码      | https://github.com/uni-halo/uni-halo-plugin  |
| 应用源码      | https://github.com/uni-halo/uni-halo         |
| 应用市场 | https://www.halo.run/store/apps/app-aukgwe3y |

## 💰 赞助支持

如果您觉得这个项目对您有帮助，可以请作者喝杯饮料鼓励一下，同时为了项目能够持续发展，可以根据您的喜好支持一下本项目哦，非常感谢您的支持，作者也会更有动力持续维护和更新新的功能哦~

|                                                 支付宝                                                 |                                                微信                                                 |                                                QQ                                                 |
| :----------------------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------------: |
| ![支付宝赞助](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static/author/rewards/ZFBRewardCode.png) | ![微信赞助](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static/author/rewards/WXRewardCode.png) | ![QQ赞助](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static/author/rewards/QQRewardCode.png) |

## 💬 社群与反馈

使用问题、配置求助都可以在群里提，Bug 与功能建议请走这里，方便跟踪

- **QQ 交流群：632969367**
- **插件提交 issue**：https://github.com/uni-halo/uni-halo-plugin/issues
- **应用提交 issue**：https://github.com/uni-halo/uni-halo/issues

![QQ交流群](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static/images/qqqun.png)

## 🤝 参与贡献

插件还在持续更新中，欢迎您的参与！无论是提交建议、反馈问题还是提交 PR，我们都非常乐意接受。

欢迎通过 [GitHub Issues](https://github.com/uni-halo/uni-halo-plugin/issues) 反馈问题，或提交 Pull Request 共同完善项目。感谢所有给插件贡献过代码的[开发者](https://github.com/uni-halo/uni-halo-plugin/graphs/contributors)！

<a href="https://github.com/uni-halo/uni-halo-plugin/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=uni-halo/uni-halo-plugin" />
</a>

## 🙏 致谢

- [Halo](https://halo.run/)：一款好用又强大的开源建站工具
- [uni-app](https://uniapp.dcloud.net.cn/)：使用 Vue.js 开发所有前端应用的框架，一套代码多端编译

---

# 插件开发

以下为本插件的开发介绍。

插件源码地址：https://github.com/uni-halo/uni-halo-plugin

## 开发环境

- Java 21+
- Node.js ^20.19.0 or >=22.12.0
- pnpm

## 开发

```bash
# 启用插件
./gradlew haloServer
# 开发前端
cd ui
pnpm install
pnpm dev
```

### Api 文档

- 开启后，您可以在 `http://{ip|domain}/swagger-ui/index.html` 访问 swagger-ui 文档。

在 `workplace` 目录下 新建 `application.yaml` 文件，内容如下：

```yaml
springdoc:
    api-docs:
        enabled: true
    swagger-ui:
        enabled: true
    show-login-endpoint: true
    show-actuator: true
```

## 构建

```bash
./gradlew build
```

构建完成后，可以在 `build/libs` 目录找到插件 jar 文件。

## 许可证

[Apache License 2.0](./LICENSE) © 小莫唐尼
