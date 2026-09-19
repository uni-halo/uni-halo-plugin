# Uni Halo 配置插件

> 为免费开源的 `uni-halo v3.x` 多端应用提供配套的配置插件。

- 官网主页：https://uni-halo.ialley.cn/
- 官方文档：https://uni-halo-doc.ialley.cn/
- 作者主页：https://www.xiaoxiaomo.cn/
- 作者博客：https://blog.xiaoxiaomo.cn/
- 源码仓库：https://github.com/uni-halo/uni-halo
- 插件源码：https://github.com/uni-halo/uni-halo-plugin
- 插件市场：https://www.halo.run/store/apps/app-ryemX

### 支持我

如果您觉得这个项目对您有帮助，可以帮作者买杯饮料鼓励鼓励，同时为了项目能够持续发展，可以根据您的喜好支持一下本项目哦，非常感谢您的支持，作者也会更有动力持续维护和更新新的功能哦~

|                                                 支付宝                                                 |                                                微信                                                 |                                                QQ                                                 |
| :----------------------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------------: |
| ![支付宝赞助](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static/author/rewards/ZFBRewardCode.png) | ![微信赞助](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static/author/rewards/WXRewardCode.png) | ![QQ赞助](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static/author/rewards/QQRewardCode.png) |

### 交流群

![QQ交流群](https://blog.xiaoxiaomo.cn/upload/qun.png)

## 一、uni-halo 小程序

### 1、应用简介

基于 Halo 2.x 提供的 API 接口，为 uni-halo 多端应用提供的一套开源的博客应用。

- 完全免费开源，包括程序源码、插件源码
- 页面支持插件配置
- 使用最新流行的技术栈
- 支持特色功能，恋爱日记
- 支持编译为 小程序（推荐）、APP、H5

### 2、页面截图

|                                             首页                                              |                                             分类                                              |                                             博主                                              |
| :-------------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------------: |
| ![首页](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static/screenshots/app/v3.x/首页.png) | ![分类](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static/screenshots/app/v3.x/分类.png) | ![博主](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static/screenshots/app/v3.x/博主.png) |

### 恋爱日记

- 在 uni-halo 中，我们为您准备了一个恋爱日记的模块，您可以在其中记录您的恋爱故事，分享给您的朋友们，请扫示例小程序，在线体验恋爱日记的功能。

![恋爱日记](https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static/screenshots/app/v3.x/恋爱日记.jpg)

<br/>

## 二、uni-halo 配置插件

### 1、关于插件

该插件仅为 `uni-halo v3.x` 提供配套的配置，目前支持以下功能：

- 通用配置：应用信息、博主资料、社交信息、页面排版、资源占位、维护模式等
- 内容管理：恋爱相册（查看密码锁定）、恋爱清单、恋爱故事、恋爱日记、公告、友情链接（投稿审核+邮件通知）、首页轮播
- 应用管理：应用信息维护、版本管理与检查更新
- 恋爱日记模板：内置前台模板，主题装上插件即可展示恋爱日记页面，支持主题整页接管或覆盖页头页脚
- 移动端登录：账号密码登录、微信小程序一键登录，登录后下发 Halo 原生令牌
- 插件设置：验证码安全控制、平台接入（第三方插件）、主题悬浮窗、移动端登录开关

### 亮点功能

**恋爱日记**：精心设计的秀恩爱模块 —— 恋爱相册（支持查看密码，页内解锁、服务端保证安全）、恋爱清单、我们的故事；内容在插件控制台维护，管理员也可以直接在移动端 App 内管理；插件同时内置前台模板，任何主题零成本接入，详见[官方文档](https://uni-halo-doc.ialley.cn/plugin/love-template)。

**登录管理**：为移动端提供账号密码登录、注册、微信小程序一键登录与微信绑定能力，登录后使用 Halo 原生令牌访问接口，内置登录限流保护，并按角色控制菜单/按钮的可见性。

### 2、使用方式

- 1、下载 `uni-halo v3.x` 小程序源码，参考：https://uni-halo-doc.ialley.cn/guide/introduction 部署指南将项目在本地运行起来。
- 2、在 Halo 插件市场搜索 `Uni Halo` 插件下载安装，或者通过 `github` 仓库 [点这里](https://github.com/uni-halo/uni-halo-plugin/releases) 找到发布包下载安装。
- 3、安装完成并且启动插件，进入插件配置页面，配置相关参数即可。

### 3、相关文档

- [插件配置](https://uni-halo-doc.ialley.cn/deploy/config)：插件设置（安全控制、平台接入、主题悬浮窗、移动端登录）说明
- [控制台功能](https://uni-halo-doc.ialley.cn/plugin/console)：内容管理（横幅、公告、友链、恋爱日记等）使用说明
- [移动端登录 · 使用与配置](https://uni-halo-doc.ialley.cn/plugin/mobile-login)：站点管理员如何开通与配置登录能力
- [移动端登录 · 接口文档](https://uni-halo-doc.ialley.cn/plugin/mobile-login-api)：App / 小程序端如何对接
- [恋爱日记前台模板](https://uni-halo-doc.ialley.cn/plugin/love-template)：主题接入说明

---

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

## 构建

```bash
./gradlew build
```

构建完成后，可以在 `build/libs` 目录找到插件 jar 文件。

## 许可证

[Apache License 2.0](./LICENSE) © 小莫唐尼
