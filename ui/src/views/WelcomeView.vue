<script setup lang="ts">
import confetti from 'canvas-confetti'
import { onMounted, ref, type Component } from 'vue'
import RiSettingsLine from '~icons/ri/settings-line'
import RiSmartphoneLine from '~icons/ri/smartphone-line'
import RiLayoutLine from '~icons/ri/layout-line'
import RiUserLine from '~icons/ri/user-line'
import RiImageLine from '~icons/ri/image-line'
import RiHeartLine from '~icons/ri/heart-line'
import RiArrowRightSLine from '~icons/ri/arrow-right-s-line'
import RiGithubLine from '~icons/ri/github-line'
import RiBookOpenLine from '~icons/ri/book-open-line'
import RiFileCodeLine from '~icons/ri/file-code-line'
import RiMoneyCnyCircleLine from '~icons/ri/money-cny-circle-line'
import RiWechatPayLine from '~icons/ri/wechat-pay-line'
import RiAlipayLine from '~icons/ri/alipay-line'
import RiQqLine from '~icons/ri/qq-line'
import RiCodeBoxLine from '~icons/ri/code-box-line'
import RiSparklingLine from '~icons/ri/sparkling-line'
import RiMailLine from '~icons/ri/mail-line'
import RiLinksLine from '~icons/ri/links-line'
import RiMessage2Line from '~icons/ri/message-2-line'
import RiGlobalLine from '~icons/ri/global-line'
import RiMapPinLine from '~icons/ri/map-pin-line'
import RiLightbulbLine from '~icons/ri/lightbulb-line'
import RiHomeLine from '~icons/ri/home-line'
import RiMegaphoneLine from '~icons/ri/megaphone-line'
import RiShieldCheckLine from '~icons/ri/shield-check-line'
import RiSlideshowLine from '~icons/ri/slideshow-line'
import RiSearchLine from '~icons/ri/search-line'
import RiArchiveLine from '~icons/ri/archive-line'
import RiThumbUpLine from '~icons/ri/thumb-up-line'
import RiNotificationLine from '~icons/ri/notification-line'
import RiBookmarkLine from '~icons/ri/bookmark-line'
import RiLoginCircleLine from '~icons/ri/login-circle-line'
import RiShapeLine from '~icons/ri/shape-line'
import RiChat3Line from '~icons/ri/chat-3-line'
import RiBarChart2Line from '~icons/ri/bar-chart-2-line'
import RiMovieLine from '~icons/ri/movie-line'
import RiPencilLine from '~icons/ri/pencil-line'
import RiPaintBrushLine from '~icons/ri/paint-brush-line'
import RiGalleryLine from '~icons/ri/gallery-line'
import RiDeviceLine from '~icons/ri/device-line'
import RiUserSmileLine from '~icons/ri/user-smile-line'
import RiApps2Line from '~icons/ri/apps-2-line'
import RiQuestionLine from '~icons/ri/question-line'
import RiMagicLine from '~icons/ri/magic-line'

import mpImg from '@/assets/mp.jpeg'
import qqqunImg from '@/assets/qqqun.jpeg'
import wxPng from '@/assets/wx.png'
import zfbPng from '@/assets/zfb.png'
import qqPng from '@/assets/qq.png'
import logoPng from '@/assets/logo.png'

const isLoaded = ref(false)
const activeFeatureTab = ref<'console' | 'mobile' | 'plugins'>('console')

/** 功能模块各分段下的区块副标题文案 */
const featureTabSubtitles: Record<'console' | 'mobile' | 'plugins', string> = {
  console: '全方位满足你的小程序配置需求',
  mobile: '25+ 功能开箱即用，移动端体验一站配齐',
  plugins: '与多款 Halo 插件无缝协作，按需安装启用',
}

onMounted(() => {
  setTimeout(() => {
    isLoaded.value = true
  }, 100)

  confetti({
    particleCount: 150,
    spread: 100,
    origin: { y: 0.4, x: 0.5 },
    colors: ['#B9E424', '#5BA4F5', '#2D7AD6'],
  })

  loadPluginLogos()
})

/** Halo 插件市场单应用接口地址 */
const STORE_APPLICATION_API = 'https://www.halo.run/apis/api.store.halo.run/v1alpha1/applications/'

/** 支持插件 logo 的 localStorage 缓存键 */
const PLUGIN_LOGOS_CACHE_KEY = 'unihalo-welcome-plugin-logos'

/** 支持插件 logo 表（市场应用 ID -> logo 地址），未加载成功时卡片回退为图标 */
const pluginLogos = ref<Record<string, string>>({})

/** 从插件市场链接中提取应用 ID */
function pluginAppId(href: string): string {
  return href.split('/').pop() ?? ''
}

/** 将市场返回的相对路径 logo 补全为站点绝对地址 */
function toAbsoluteLogoUrl(logo: string): string {
  return /^https?:\/\//.test(logo) ? logo : `https://www.halo.run${logo}`
}

/** 加载失败的 logo 地址记录，命中后卡片回退到下一候选 */
const failedLogoUrls = ref<Record<string, boolean>>({})

/** 解析插件卡片 logo 展示地址：市场接口结果优先，其次插件自带静态资源，都没有则回退渐变图标 */
function pluginLogoSrc(plugin: (typeof pluginFeatures)[number]): string {
  const candidates = [pluginLogos.value[pluginAppId(plugin.href)], plugin.logo]
  const available = candidates.find((item) => typeof item === 'string' && !failedLogoUrls.value[item])
  return available ?? ''
}

/** logo 图片加载失败时记录地址，卡片回退到下一候选 */
function onLogoError(src: string) {
  failedLogoUrls.value = { ...failedLogoUrls.value, [src]: true }
}

/** 从插件市场接口拉取各支持插件 logo 并缓存到 localStorage */
async function loadPluginLogos() {
  try {
    const cached = localStorage.getItem(PLUGIN_LOGOS_CACHE_KEY)
    if (cached) {
      pluginLogos.value = JSON.parse(cached)
      return
    }
  } catch {
    pluginLogos.value = {}
  }

  const appIds = pluginFeatures.map((plugin) => pluginAppId(plugin.href))
  const results = await Promise.allSettled(
    appIds.map((id) => fetch(`${STORE_APPLICATION_API}${id}`).then((res) => res.json())),
  )
  const logos: Record<string, string> = {}
  appIds.forEach((id, index) => {
    const result = results[index]
    if (!result || result.status !== 'fulfilled') return
    const logo: unknown = result.value?.application?.spec?.logo
    if (typeof logo === 'string' && logo) {
      logos[id] = toAbsoluteLogoUrl(logo)
    }
  })
  pluginLogos.value = logos
  try {
    localStorage.setItem(PLUGIN_LOGOS_CACHE_KEY, JSON.stringify(logos))
  } catch {
    // 缓存写入失败不影响本次展示
  }
}

const consoleFeatures = [
  {
    icon: RiSettingsLine,
    title: '通用配置',
    description: '应用资料、偏好设置、页面排版、移动端登录、恋爱友链等全局配置',
    color: '#368FEF',
  },
  {
    icon: RiSmartphoneLine,
    title: '应用管理',
    description: '应用信息维护、版本管理、小程序发布上线',
    color: '#8B5CF6',
  },
  {
    icon: RiHeartLine,
    title: '恋爱管理',
    description: '恋爱相册（支持查看密码锁定）、恋爱清单、我们的故事管理',
    color: '#EC4899',
  },
  {
    icon: RiLayoutLine,
    title: '恋爱日记模板',
    description: '内置前台模板，主题可整页接管或仅覆盖页头页脚，零成本展示恋爱日记',
    color: '#DB2777',
  },
  {
    icon: RiMegaphoneLine,
    title: '公告管理',
    description: '公告发布、公告类型管理、公告内容编辑',
    color: '#F59E0B',
  },
  {
    icon: RiLinksLine,
    title: '友链管理',
    description: '友链维护、友链审核、友链分组管理',
    color: '#10B981',
  },
  {
    icon: RiShieldCheckLine,
    title: '审核模式',
    description: '内容审核、数据引用管理、审核规则配置',
    color: '#06B6D4',
  },
  {
    icon: RiSlideshowLine,
    title: '轮播管理',
    description: '首页轮播配置、笔记同步、自定义轮播内容',
    color: '#8B5CF6',
  },
  {
    icon: RiApps2Line,
    title: '应用展示主题组件',
    description: '博客主题侧应用展示挂件，展示应用信息与下载入口',
    color: '#3E87F7',
  },
  {
    icon: RiPaintBrushLine,
    title: '绘画管理',
    description: '绘画作品管理，支持作品列表维护',
    color: '#8B5CF6',
  },
  {
    icon: RiLoginCircleLine,
    title: '移动端登录配置',
    description: '登录方式、注册与微信一键登录、登录限流配置',
    color: '#2D7AD6',
  },
  {
    icon: RiQuestionLine,
    title: '问题管理',
    description: '常见问题维护，旧称常见问题管理',
    color: '#F59E0B',
  },
]

const mobileFeatures = [
  { icon: RiHomeLine, title: '首页', description: '轮播展示、笔记列表、快捷导航、分类推荐', color: '#368FEF', tab: true },
  { icon: RiLayoutLine, title: '分类', description: '笔记分类浏览、分类详情、标签筛选', color: '#10B981', tab: true },
  { icon: RiImageLine, title: '图库', description: '图片瀑布流展示、相册浏览、图片预览', color: '#8B5CF6', tab: true },
  { icon: RiMessage2Line, title: '瞬间', description: '动态发布、生活记录、互动交流', color: '#EC4899', tab: true },
  { icon: RiUserLine, title: '博主', description: '博主信息、功能入口、个人中心', color: '#06B6D4', tab: true },
  { icon: RiMapPinLine, title: '足迹地图', description: '博主去过的地方，基于高德地图', color: '#65A30D' },
  { icon: RiMailLine, title: '联系博主', description: '博主常用联系方式', color: '#FF9800' },
  { icon: RiMegaphoneLine, title: '通知公告', description: '站点公告与通知、公告详情', color: '#9C27B0' },
  { icon: RiBookmarkLine, title: '我的收藏', description: '笔记和瞬间收藏管理', color: '#FFB300' },
  { icon: RiHeartLine, title: '恋爱日记', description: '恋爱日记、相册、清单与故事', color: '#FF4C67' },
  { icon: RiLinksLine, title: '友情链接', description: '看看博主的朋友们', color: '#009688' },
  { icon: RiArchiveLine, title: '笔记归档', description: '笔记按时间归档、历史检索', color: '#03A9F4' },
  { icon: RiThumbUpLine, title: '投票中心', description: '查看和参与投票', color: '#00BCD4' },
  { icon: RiBarChart2Line, title: '数据看板', description: '站点数据可视化统计', color: '#663CC9' },
  { icon: RiCodeBoxLine, title: '项目展示', description: '博主的项目与作品集', color: '#3E87F7' },
  { icon: RiMovieLine, title: '豆瓣展示', description: '博主的书影音记录', color: '#43B024' },
  { icon: RiPaintBrushLine, title: '绘画作品', description: '大家的绘画作品与画谜', color: '#8B5CF6' },
  { icon: RiDeviceLine, title: '装备展示', description: '博主的装备清单', color: '#EF6C00' },
  { icon: RiSettingsLine, title: '偏好设置', description: '页面布局与样式设置', color: '#7986CB' },
  { icon: RiMessage2Line, title: '瞬间管理', description: '管理已发布的瞬间，需权限', color: '#EC4899' },
  { icon: RiPencilLine, title: '瞬间发布', description: '发布图文、视频动态，需权限', color: '#F97316' },
  { icon: RiHeartLine, title: '恋爱日记管理', description: '相册、清单、故事管理，需权限', color: '#DB2777' },
  { icon: RiGalleryLine, title: '我的绘画管理', description: '管理我的绘画作品，需权限', color: '#6366F1' },
  { icon: RiShieldCheckLine, title: '绘画审核管理', description: '审核绘画作品，需管理员', color: '#2D7AD6' },
  { icon: RiLoginCircleLine, title: '登录注册', description: '账号密码登录、微信一键登录与绑定', color: '#2D7AD6' },
  { icon: RiNotificationLine, title: '消息通知', description: '评论、点赞等消息聚合查看', color: '#F59E0B' },
  { icon: RiUserSmileLine, title: '我的资料', description: '编辑个人资料信息', color: '#10B981' },
  { icon: RiMagicLine, title: 'AI对话', description: '智阅全能AI助手，站点全局智能问答', color: '#7C3AED' },
  { icon: RiSearchLine, title: '内容搜索', description: '全文内容搜索、关键词快速定位', color: '#06B6D4' },
]

/** 支持插件来源类型：core 本插件 / official Halo 官方 / third 社区第三方 */
type PluginVendor = 'core' | 'official' | 'third'

const vendorMeta: Record<PluginVendor, { label: string; color: string }> = {
  core: { label: '核心', color: '#65A30D' },
  official: { label: '官方', color: '#2D7AD6' },
  third: { label: '第三方', color: '#F59E0B' },
}

const pluginFeatures: {
  icon: Component
  /** 插件自带静态 logo 资源，市场接口 logo 拉取失败时兜底 */
  logo?: string
  title: string
  description: string
  color: string
  vendor: PluginVendor
  href: string
}[] = [
  {
    icon: RiSparklingLine,
    logo: logoPng,
    title: 'UniHalo 配置',
    description: '本插件本体，UniHalo 全部功能的基础，需安装并启用',
    color: '#65A30D',
    vendor: 'core',
    href: 'https://www.halo.run/store/apps/app-aukgwe3y',
  },
  {
    icon: RiImageLine,
    title: '图库管理',
    description: '官方图库插件，为移动端图库模块提供数据',
    color: '#368FEF',
    vendor: 'official',
    href: 'https://www.halo.run/store/apps/app-BmQJW',
  },
  {
    icon: RiLinksLine,
    title: '链接管理',
    description: '官方友链插件，为移动端友链模块提供数据',
    color: '#10B981',
    vendor: 'official',
    href: 'https://www.halo.run/store/apps/app-hfbQg',
  },
  {
    icon: RiMessage2Line,
    title: '瞬间',
    description: '官方瞬间插件，图文 / 视频 / 音频动态展示',
    color: '#EC4899',
    vendor: 'official',
    href: 'https://www.halo.run/store/apps/app-SnwWD',
  },
  {
    icon: RiSearchLine,
    title: '搜索组件',
    description: '官方搜索插件，为移动端提供全文搜索能力',
    color: '#06B6D4',
    vendor: 'official',
    href: 'https://www.halo.run/store/apps/app-DlacW',
  },
  {
    icon: RiChat3Line,
    title: '评论组件',
    description: '官方评论插件，提供完整的评论解决方案',
    color: '#8B5CF6',
    vendor: 'official',
    href: 'https://www.halo.run/store/apps/app-YXyaD',
  },
  {
    icon: RiThumbUpLine,
    title: '投票管理',
    description: '社区插件，为移动端投票模块提供支持',
    color: '#F59E0B',
    vendor: 'third',
    href: 'https://www.halo.run/store/apps/app-veyvzyhv',
  },
  {
    icon: RiBarChart2Line,
    title: '数据看板',
    description: '社区插件，数据可视化统计与站点流量分析',
    color: '#2D7AD6',
    vendor: 'third',
    href: 'https://www.halo.run/store/apps/app-rtnbbgfk',
  },
  {
    icon: RiCodeBoxLine,
    title: '项目集',
    description: '社区插件，项目与作品集集中展示',
    color: '#0EA5E9',
    vendor: 'third',
    href: 'https://www.halo.run/store/apps/app-ix3j4n6d',
  },
  {
    icon: RiMovieLine,
    title: '豆瓣',
    description: '社区插件，豆瓣影书记录展示',
    color: '#10B981',
    vendor: 'third',
    href: 'https://www.halo.run/store/apps/app-srBOL',
  },
  {
    icon: RiMapPinLine,
    title: '足迹',
    description: '社区插件，基于高德地图的足迹地图',
    color: '#F97316',
    vendor: 'third',
    href: 'https://www.halo.run/store/apps/app-XCtAQ',
  },
  {
    icon: RiArchiveLine,
    title: '装备管理',
    description: '社区插件，博主装备清单展示（支持分组）',
    color: '#64748B',
    vendor: 'third',
    href: 'https://www.halo.run/store/apps/app-ytygyqml',
  },
  {
    icon: RiMagicLine,
    title: '智阅全能AI助手',
    description: '社区 AI 插件，为移动端提供全局 AI 对话',
    color: '#7C3AED',
    vendor: 'third',
    href: 'https://www.halo.run/store/apps/app-OWBzA',
  },
]

const steps = [
  {
    title: '下载源码',
    desc: '从 GitHub 下载 UniHalo v3.x 小程序源码到本地，解压后导入微信开发者工具即可运行',
  },
  {
    title: '安装插件',
    desc: '在 Halo 后台插件市场搜索「UniHalo 配置」，一键安装并启用插件',
  },
  {
    title: '配置参数',
    desc: '进入插件设置页面，根据实际需求配置应用信息、页面内容、博主资料等参数',
  },
  {
    title: '启动运行',
    desc: '启动微信小程序预览配置效果，确认无误后发布上线',
  },
]

const contacts = [
  { icon: RiGithubLine, label: 'GitHub 主页', value: 'github.com/okay-xiaomo', href: 'https://github.com/okay-xiaomo' },
  { icon: RiBookOpenLine, label: '个人博客', value: 'blog.xiaoxiaomo.cn', href: 'https://blog.xiaoxiaomo.cn/' },
  { icon: RiGlobalLine, label: '个人主页', value: 'www.xiaoxiaomo.cn', href: 'https://www.xiaoxiaomo.cn/' },
  { icon: RiMailLine, label: '联系邮箱', value: '1431128779@qq.com', href: 'mailto:1431128779@qq.com' },
  { icon: RiMessage2Line, label: 'QQ 交流群', value: '加入开发者社群', href: 'https://qm.qq.com/q/Kh9QPnL6gO' },
  { icon: RiMapPinLine, label: '所在地', value: '中国·深圳', href: 'javascript:void(0)' },
]

// 界面预览数据（分段器三大块）
const SCREENSHOT_CDN_BASE = 'https://gcore.jsdelivr.net/gh/uni-halo/uni-halo-static/screenshots/'
const uiPreviewGroups = [
  {
    key: 'main',
    label: '主界面',
    items: [
      { name: '首页', file: 'app/v3.x/首页.png' },
      { name: '分类', file: 'app/v3.x/分类.png' },
      { name: '图库', file: 'app/v3.x/图库.png' },
      { name: '瞬间', file: 'app/v3.x/瞬间.png' },
      { name: '博主', file: 'app/v3.x/博主.png' },
    ],
  },
  {
    key: 'love',
    label: '特色功能',
    items: [
      { name: '恋爱主页', file: 'app/v3.x/恋爱日记.png' },
      { name: '恋爱相册', file: 'app/v3.x/恋爱相册.png' },
      { name: '恋爱清单', file: 'app/v3.x/恋爱清单.png' },
      { name: '我们的故事', file: 'app/v3.x/恋爱故事.png' },
    ],
  },
  {
    key: 'template',
    label: '主题模板',
    items: [
      { name: '恋爱日记主页', file: 'plugin/v3.x/前台模板.png' },
      { name: '恋爱相册', file: 'plugin/v3.x/前台模板-恋爱相册.png' },
      { name: '恋爱清单', file: 'plugin/v3.x/前台模板-恋爱清单.png' },
      { name: '我们的故事', file: 'plugin/v3.x/前台模板-恋爱故事.png' },
    ],
  },
]
const activeUiPreviewTab = ref('main')
</script>

<template>
  <div class="min-h-screen bg-slate-50 transition-opacity duration-500" :class="isLoaded ? 'opacity-100' : 'opacity-0'">
    <!-- Hero Section -->
    <section class="relative h-screen flex items-center justify-center overflow-hidden">
      <!-- Background Decorations - 仅使用主题色 #B9E424 淡化 -->
      <div class="absolute inset-0 overflow-hidden pointer-events-none">
        <div class="absolute -top-40 -right-40 w-[500px] h-[500px] rounded-full blur-3xl animate-float"
          style="background: radial-gradient(circle, rgb(185 228 36 / 0.33) 0%, rgba(54,143,239,0) 70%)"></div>
        <div class="absolute -bottom-40 -left-40 w-[500px] h-[500px] rounded-full blur-3xl animate-float-delayed"
          style="background: radial-gradient(circle, rgb(185 228 36 / 0.33) 0%, rgba(54,143,239,0) 70%)"></div>
        <div
          class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] rounded-full blur-3xl animate-pulse-slow"
          style="background: radial-gradient(circle, rgb(185 228 36 / 0.33) 0%, rgba(54,143,239,0) 70%)"></div>
        <!-- Floating Shapes -->
        <div class="absolute top-20 left-[10%] w-3 h-3 rounded-full animate-bounce-slow"
          style="background: rgb(185 228 36 / 0.47)"></div>
        <div class="absolute top-[30%] right-[15%] w-4 h-4 rounded-lg rotate-45 animate-bounce-slow-delayed"
          style="background: rgb(185 228 36 / 0.33)"></div>
        <div class="absolute bottom-[25%] left-[20%] w-2.5 h-2.5 rounded-full animate-bounce-slow"
          style="background: rgb(185 228 36 / 0.47)"></div>
        <div class="absolute top-[40%] right-[25%] w-2 h-2 rounded-full animate-bounce-slow-delayed"
          style="background: rgb(185 228 36 / 0.33)"></div>
        <div class="absolute bottom-[15%] right-[10%] w-3.5 h-3.5 rounded-full animate-bounce-slow"
          style="background: rgb(185 228 36 / 0.33)"></div>
      </div>

      <div class="relative z-10 max-w-5xl mx-auto px-4 text-center">
        <img :src="logoPng" alt="UniHalo Logo" class="w-20 h-20 mx-auto mb-6 drop-shadow-lg" />

        <div
          class="inline-flex items-center gap-2 bg-white/80 backdrop-blur-sm border rounded-full px-4 py-2 mb-8 shadow-sm"
          style="border-color: rgb(185 228 36 / 0.47)">
          <RiSparklingLine class="w-4 h-4 text-[#B9E424]" />
          <span class="text-sm font-medium text-[#B9E424]">v3.x Apache License 2.0</span>
        </div>

        <h1 class="text-5xl sm:text-6xl lg:text-7xl font-extrabold mb-6">
          <span style="color: #B9E424">UniHalo</span>
          <span class="block text-2xl sm:text-3xl lg:text-4xl mt-3 font-bold text-slate-700">配置插件</span>
        </h1>

        <p class="text-lg sm:text-xl text-slate-500 max-w-2xl mx-auto mb-10 leading-relaxed">
          免费开源的 UniHalo v3.x 跨平台移动端配套配置插件
        </p>

        <div class="flex flex-wrap justify-center gap-4 mb-10">
          <a href="https://uni-halo.ialley.cn"
            class="inline-flex items-center gap-2 border-slate-300 bg-white hover:bg-slate-50 text-slate-700 font-semibold px-7 py-3.5 rounded-xl border  transition-all duration-200 hover:shadow-lg hover:-translate-y-0.5"
            target="_blank">
            <RiBookOpenLine class="w-5 h-5" />
            <span>官网主页</span>
          </a>
          <a href="https://uni-halo-doc.ialley.cn/"
            class="inline-flex items-center gap-2 text-white font-semibold px-7 py-3.5 rounded-xl transition-all duration-200 hover:shadow-lg hover:-translate-y-0.5"
            style="background-color: #B9E424" target="_blank">
            <RiBookOpenLine class="w-5 h-5" />
            <span>官方文档</span>
          </a>
          <a href="https://github.com/uni-halo/uni-halo"
            class="inline-flex items-center gap-2 bg-slate-800 hover:bg-slate-900 text-white font-semibold px-7 py-3.5 rounded-xl transition-all duration-200 hover:shadow-lg hover:-translate-y-0.5"
            target="_blank">
            <RiGithubLine class="w-5 h-5" />
            <span>GitHub</span>
          </a>
          <a href="https://github.com/ialley-workshop-open/uni-halo/issues/23"
            class="inline-flex items-center gap-2 text-[#b9e424] border-[#b9e424]/50 bg-white hover:bg-slate-50 font-semibold px-7 py-3.5 rounded-xl border transition-all duration-200 hover:shadow-lg hover:-translate-y-0.5"
            target="_blank">
            <RiLightbulbLine class="w-5 h-5" />
            <span>提交案例</span>
          </a>
        </div>

        <!-- 统计数据 -->
        <div class="flex justify-center gap-10 sm:gap-16 mb-10">
          <div class="text-center">
            <div class="text-3xl sm:text-4xl font-bold" style="color: #B9E424">12+</div>
            <div class="text-sm text-slate-500 mt-1">管理模块</div>
          </div>
          <div class="text-center">
            <div class="text-3xl sm:text-4xl font-bold" style="color: #B9E424">40+</div>
            <div class="text-sm text-slate-500 mt-1">移动端页面</div>
          </div>
          <div class="text-center">
            <div class="text-3xl sm:text-4xl font-bold" style="color: #B9E424">v3.x</div>
            <div class="text-sm text-slate-500 mt-1">支持版本</div>
          </div>
        </div>

        <!-- 三个横向截图占位 -->
        <div class="grid grid-cols-2 gap-4 max-w-2xl mx-auto">
          <div
            class="box-border p-2 rounded-2xl overflow-hidden bg-white shadow-sm border border-slate-100 flex items-center justify-center">
            <img :src="mpImg" alt="小程序二维码" class="w-full h-full object-contain rounded-lg" />
          </div>
          <div
            class="box-border p-2 rounded-2xl overflow-hidden bg-white shadow-sm border border-slate-100 flex items-center justify-center">
            <img :src="qqqunImg" alt="QQ 交流群" class="w-full h-full object-contain rounded-lg" />
          </div>
        </div>
      </div>

      <!-- 底部滚动提示 -->
      <div class="absolute bottom-8 left-1/2 -translate-x-1/2 flex flex-col items-center gap-2 animate-bounce-slow">
        <span class="text-xs text-slate-400">向下滚动</span>
        <div class="w-5 h-8 rounded-full border-2 border-slate-300 flex justify-center pt-1.5">
          <div class="w-1 h-2 rounded-full bg-slate-300"></div>
        </div>
      </div>
    </section>

    <!-- Sponsor Section -->
    <section class="py-16 sm:py-20 bg-white">
      <div class="max-w-5xl mx-auto px-4">
        <div class="text-center mb-12">
          <h2 class="text-3xl sm:text-4xl font-bold text-slate-800 mb-4">赞助支持</h2>
          <p class="text-lg text-slate-500">如果这个项目对你有帮助，可以请作者喝杯饮料</p>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-6 max-w-5xl mx-auto">
          <div class="bg-slate-50 rounded-2xl p-6 text-center hover:shadow-md transition-all duration-300">
            <div class="bg-green-500 w-14 h-14 rounded-full flex items-center justify-center text-white mx-auto mb-4">
              <RiWechatPayLine class="w-7 h-7" />
            </div>
            <h4 class="font-semibold text-slate-800 mb-3">微信赞助</h4>
            <div class="w-full rounded-xl mx-auto mb-3 overflow-hidden bg-white flex items-center justify-center">
              <img :src="wxPng" alt="微信赞赏码" class="w-full h-full object-contain" />
            </div>
            <p class="text-sm text-slate-500">扫码请作者喝咖啡</p>
          </div>

          <div class="bg-slate-50 rounded-2xl p-6 text-center hover:shadow-md transition-all duration-300">
            <div class="bg-blue-500 w-14 h-14 rounded-full flex items-center justify-center text-white mx-auto mb-4">
              <RiAlipayLine class="w-7 h-7" />
            </div>
            <h4 class="font-semibold text-slate-800 mb-3">支付宝赞助</h4>
            <div class="w-full rounded-xl mx-auto mb-3 overflow-hidden bg-white flex items-center justify-center">
              <img :src="zfbPng" alt="支付宝赞赏码" class="w-full h-full object-contain" />
            </div>
            <p class="text-sm text-slate-500">扫码请作者喝奶茶</p>
          </div>

          <div class="bg-slate-50 rounded-2xl p-6 text-center hover:shadow-md transition-all duration-300">
            <div class="bg-yellow w-14 h-14 rounded-full flex items-center justify-center text-white mx-auto mb-4">
              <RiQqLine class="w-7 h-7" />
            </div>
            <h4 class="font-semibold text-slate-800 mb-3">QQ 赞助</h4>
            <div class="w-full rounded-xl mx-auto mb-3 overflow-hidden bg-white flex items-center justify-center">
              <img :src="qqPng" alt="QQ 赞赏码" class="w-full h-full object-contain" />
            </div>
            <p class="text-sm text-slate-500">扫码请作者吃零食</p>
          </div>
        </div>

        <div class="flex items-center justify-center gap-2 mt-8 text-slate-400 text-sm">
          <RiMoneyCnyCircleLine class="w-4 h-4" />
          <span>您的支持是作者持续维护的动力</span>
        </div>
      </div>
    </section>

    <!-- Highlights Section -->
    <section class="py-16 sm:py-20 bg-white">
      <div class="max-w-5xl mx-auto px-4">
        <div class="text-center mb-12">
          <h2 class="text-3xl sm:text-4xl font-bold text-slate-800 mb-4">亮点功能</h2>
          <p class="text-lg text-slate-500">为你的站点带来温度与安全感</p>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <!-- 恋爱日记 -->
          <div
            class="relative overflow-hidden bg-white rounded-3xl p-8 shadow-sm border border-slate-100 hover:shadow-lg transition-all duration-300">
            <div class="absolute top-0 right-0 w-40 h-40 rounded-full blur-3xl pointer-events-none"
              style="background: rgba(236, 72, 153, 0.12)"></div>
            <div class="w-12 h-12 rounded-2xl flex items-center justify-center text-white mb-5"
              style="background: linear-gradient(135deg, #EC4899 0%, #DB2777 100%)">
              <RiHeartLine class="w-6 h-6" />
            </div>
            <h3 class="text-xl font-bold text-slate-800 mb-2">恋爱日记</h3>
            <p class="text-slate-500 text-sm leading-relaxed mb-5">
              移动端与插件端双端管理，前台模板开箱即用。相册支持查看密码（页内解锁、服务端保证），记录只属于你们的故事。
            </p>
            <div class="flex flex-wrap gap-2">
              <span class="px-3 py-1 text-xs font-medium rounded-full"
                style="background: rgba(236, 72, 153, 0.10); color: #EC4899">恋爱相册 · 密码锁定</span>
              <span class="px-3 py-1 text-xs font-medium rounded-full"
                style="background: rgba(236, 72, 153, 0.10); color: #EC4899">恋爱清单</span>
              <span class="px-3 py-1 text-xs font-medium rounded-full"
                style="background: rgba(236, 72, 153, 0.10); color: #EC4899">我们的故事</span>
              <span class="px-3 py-1 text-xs font-medium rounded-full"
                style="background: rgba(236, 72, 153, 0.10); color: #EC4899">前台模板 · 主题可接管</span>
              <span class="px-3 py-1 text-xs font-medium rounded-full"
                style="background: rgba(236, 72, 153, 0.10); color: #EC4899">移动端直接管理</span>
            </div>
          </div>

          <!-- 登录管理 -->
          <div
            class="relative overflow-hidden bg-white rounded-3xl p-8 shadow-sm border border-slate-100 hover:shadow-lg transition-all duration-300">
            <div class="absolute top-0 right-0 w-40 h-40 rounded-full blur-3xl pointer-events-none"
              style="background: rgba(45, 122, 214, 0.12)"></div>
            <div class="w-12 h-12 rounded-2xl flex items-center justify-center text-white mb-5"
              style="background: linear-gradient(135deg, #368FEF 0%, #2D7AD6 100%)">
              <RiShieldCheckLine class="w-6 h-6" />
            </div>
            <h3 class="text-xl font-bold text-slate-800 mb-2">登录管理</h3>
            <p class="text-slate-500 text-sm leading-relaxed mb-5">
              账号密码登录、注册、微信一键登录与绑定，登录后使用 Halo 原生令牌，内置登录限流，按角色控制可见内容。
            </p>
            <div class="flex flex-wrap gap-2 mb-5">
              <span class="px-3 py-1 text-xs font-medium rounded-full"
                style="background: rgba(45, 122, 214, 0.10); color: #2D7AD6">账号密码登录</span>
              <span class="px-3 py-1 text-xs font-medium rounded-full"
                style="background: rgba(45, 122, 214, 0.10); color: #2D7AD6">微信一键登录</span>
              <span class="px-3 py-1 text-xs font-medium rounded-full"
                style="background: rgba(45, 122, 214, 0.10); color: #2D7AD6">注册与绑定</span>
              <span class="px-3 py-1 text-xs font-medium rounded-full"
                style="background: rgba(45, 122, 214, 0.10); color: #2D7AD6">按角色控制可见内容</span>
            </div>
            <a href="https://uni-halo-doc.ialley.cn/plugin/mobile-login" target="_blank"
              class="inline-flex items-center gap-1 text-sm font-medium" style="color: #2D7AD6">
              查看登录配置文档
              <RiArrowRightSLine class="w-4 h-4" />
            </a>
          </div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="py-16 sm:py-20">
      <div class="max-w-5xl mx-auto px-4">
        <div class="text-center mb-8">
          <h2 class="text-3xl sm:text-4xl font-bold text-slate-800 mb-4">功能模块</h2>
          <p class="text-lg text-slate-500">{{ featureTabSubtitles[activeFeatureTab] }}</p>
        </div>

        <!-- 分段器 -->
        <div class="flex justify-center mb-10">
          <div class="inline-flex bg-slate-100 rounded-xl p-1">
            <button class="px-6 py-2.5 rounded-lg text-sm font-medium transition-all duration-200"
              :class="activeFeatureTab === 'console' ? 'bg-white text-slate-800 shadow-sm' : 'text-slate-500 hover:text-slate-700'"
              @click="activeFeatureTab = 'console'">
              <div class="flex items-center gap-2">
                <RiSettingsLine class="w-4 h-4" />
                <span>管理模块</span>
              </div>
            </button>
            <button class="px-6 py-2.5 rounded-lg text-sm font-medium transition-all duration-200"
              :class="activeFeatureTab === 'mobile' ? 'bg-white text-slate-800 shadow-sm' : 'text-slate-500 hover:text-slate-700'"
              @click="activeFeatureTab = 'mobile'">
              <div class="flex items-center gap-2">
                <RiSmartphoneLine class="w-4 h-4" />
                <span>移动端模块</span>
              </div>
            </button>
            <button class="px-6 py-2.5 rounded-lg text-sm font-medium transition-all duration-200"
              :class="activeFeatureTab === 'plugins' ? 'bg-white text-slate-800 shadow-sm' : 'text-slate-500 hover:text-slate-700'"
              @click="activeFeatureTab = 'plugins'">
              <div class="flex items-center gap-2">
                <RiShapeLine class="w-4 h-4" />
                <span>支持插件</span>
              </div>
            </button>
          </div>
        </div>

        <!-- 管理模块 -->
        <div v-if="activeFeatureTab === 'console'" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          <div v-for="feature in consoleFeatures" :key="feature.title"
            class="group bg-white rounded-2xl p-6 shadow-sm border border-slate-100 hover:shadow-md transition-all duration-300 cursor-pointer">
            <div
              class="w-11 h-11 rounded-xl flex items-center justify-center text-white mb-4 group-hover:scale-110 transition-transform duration-300"
              :style="{ background: `linear-gradient(135deg, ${feature.color} 0%, ${feature.color}cc 100%)` }">
              <component :is="feature.icon" class="w-5.5 h-5.5" />
            </div>
            <h3 class="text-lg font-semibold text-slate-800 mb-2">{{ feature.title }}</h3>
            <p class="text-slate-500 text-sm leading-relaxed">{{ feature.description }}</p>
          </div>
        </div>

        <!-- 移动端模块 -->
        <div v-else-if="activeFeatureTab === 'mobile'" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          <div v-for="feature in mobileFeatures" :key="feature.title"
            class="group bg-white rounded-2xl p-5 shadow-sm border border-slate-100 hover:shadow-md transition-all duration-300 cursor-pointer relative">
            <div v-if="feature.tab" class="absolute top-3 right-3 px-2 py-0.5 text-xs font-medium rounded-full"
              :style="{ background: `${feature.color}20`, color: feature.color }">
              Tab
            </div>
            <div
              class="w-10 h-10 rounded-xl flex items-center justify-center text-white mb-3 group-hover:scale-110 transition-transform duration-300"
              :style="{ background: `linear-gradient(135deg, ${feature.color} 0%, ${feature.color}cc 100%)` }">
              <component :is="feature.icon" class="w-5 h-5" />
            </div>
            <h3 class="text-base font-semibold text-slate-800 mb-1.5">{{ feature.title }}</h3>
            <p class="text-slate-500 text-xs leading-relaxed">{{ feature.description }}</p>
          </div>
        </div>

        <!-- 支持插件 -->
        <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          <a v-for="plugin in pluginFeatures" :key="plugin.title" :href="plugin.href" target="_blank"
            class="group relative bg-white rounded-2xl p-6 shadow-sm border border-slate-100 hover:shadow-md hover:-translate-y-0.5 transition-all duration-300">
            <div class="absolute top-3 right-3 px-2 py-0.5 text-xs font-medium rounded-full"
              :style="{ background: `${vendorMeta[plugin.vendor].color}20`, color: vendorMeta[plugin.vendor].color }">
              {{ vendorMeta[plugin.vendor].label }}
            </div>
            <div v-if="pluginLogoSrc(plugin)"
              class="w-11 h-11 rounded-xl flex items-center justify-center mb-4 overflow-hidden bg-slate-50 border border-slate-100">
              <img :src="pluginLogoSrc(plugin)" :alt="plugin.title" class="w-full h-full object-contain"
                @error="onLogoError(pluginLogoSrc(plugin))" />
            </div>
            <div v-else
              class="w-11 h-11 rounded-xl flex items-center justify-center text-white mb-4 group-hover:scale-110 transition-transform duration-300"
              :style="{ background: `linear-gradient(135deg, ${plugin.color} 0%, ${plugin.color}cc 100%)` }">
              <component :is="plugin.icon" class="w-5.5 h-5.5" />
            </div>
            <h3 class="text-lg font-semibold text-slate-800 mb-2">{{ plugin.title }}</h3>
            <p class="text-slate-500 text-sm leading-relaxed">{{ plugin.description }}</p>
          </a>
        </div>
      </div>
    </section>

    <!-- 界面预览 Section -->
    <section class="py-16 sm:py-20 bg-white">
      <div class="max-w-6xl mx-auto px-4">
        <div class="text-center mb-8">
          <h2 class="text-3xl sm:text-4xl font-bold text-slate-800 mb-4">界面预览</h2>
          <p class="text-lg text-slate-500">主界面 / 特色功能 / 主题模板，一览 uni-halo 的界面设计</p>
        </div>

        <!-- 分段器 -->
        <div class="flex justify-center mb-10">
          <div class="inline-flex bg-slate-100 rounded-xl p-1">
            <button v-for="group in uiPreviewGroups" :key="group.key"
              class="px-6 py-2.5 rounded-lg text-sm font-medium transition-all duration-200"
              :class="activeUiPreviewTab === group.key ? 'bg-white text-slate-800 shadow-sm' : 'text-slate-500 hover:text-slate-700'"
              @click="activeUiPreviewTab = group.key">
              {{ group.label }}
            </button>
          </div>
        </div>

        <!-- 截图展示 -->
        <div v-for="group in uiPreviewGroups" v-show="activeUiPreviewTab === group.key" :key="group.key">
          <div class="grid grid-cols-2 sm:grid-cols-3 gap-5"
            :class="group.items.length % 2 === 0 ? 'lg:grid-cols-4' : 'lg:grid-cols-5'">
            <figure v-for="item in group.items" :key="item.name" class="flex flex-col items-center gap-3">
              <img :src="SCREENSHOT_CDN_BASE + item.file" :alt="item.name" loading="lazy"
                class="w-full rounded-2xl border border-slate-100 shadow-sm hover:shadow-lg hover:-translate-y-1 transition-all duration-300" />
              <figcaption class="text-sm text-slate-500">{{ item.name }}</figcaption>
            </figure>
          </div>
        </div>

        <p class="mt-8 text-center text-sm text-slate-400">
          更多页面截图与功能演示，请访问
          <a href="https://uni-halo-doc.ialley.cn/design/pages" target="_blank" class="font-medium"
            style="color: #2D7AD6">官方文档 · 界面预览</a>
        </p>
      </div>
    </section>

    <!-- Steps Section - 左右布局 -->
    <section class="py-16 sm:py-20 bg-white">
      <div class="max-w-4xl mx-auto px-4">
        <div class="text-center mb-12">
          <h2 class="text-3xl sm:text-4xl font-bold text-slate-800 mb-4">使用流程</h2>
          <p class="text-lg text-slate-500">从下载到上线，轻松四步</p>
        </div>

        <div class="space-y-4">
          <div v-for="(step, index) in steps" :key="index"
            class="flex items-start gap-5 bg-slate-50 rounded-2xl p-6 hover:bg-white hover:shadow-md transition-all duration-300">
            <div
              class="flex-shrink-0 w-12 h-12 rounded-xl flex items-center justify-center text-white font-bold text-lg"
              :style="{ background: index === 0 ? 'linear-gradient(135deg, #B9E424 0%, #2D7AD6 100%)' : index === 1 ? 'linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%)' : index === 2 ? 'linear-gradient(135deg, #10B981 0%, #059669 100%)' : 'linear-gradient(135deg, #F59E0B 0%, #D97706 100%)' }">
              {{ index + 1 }}
            </div>
            <div class="flex-1 min-w-0">
              <h3 class="text-lg font-semibold text-slate-800 mb-1.5">{{ step.title }}</h3>
              <p class="text-slate-500 text-sm leading-relaxed">{{ step.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Links Section -->
    <section class="py-16 sm:py-20">
      <div class="max-w-5xl mx-auto px-4">
        <div class="text-center mb-12">
          <h2 class="text-3xl sm:text-4xl font-bold text-slate-800 mb-4">相关链接</h2>
          <p class="text-lg text-slate-500">官方资源与社区支持</p>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <a href="https://uni-halo-doc.ialley.cn/"
            class="flex items-center gap-4 bg-white rounded-xl p-4 border border-slate-100 hover:shadow-md transition-all duration-200"
            target="_blank">
            <div class="w-10 h-10 rounded-lg flex items-center justify-center text-white flex-shrink-0"
              style="background: linear-gradient(135deg, #B9E424 0%, #2D7AD6 100%)">
              <RiBookOpenLine class="w-5 h-5" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="font-semibold text-slate-800 text-sm">官方文档</div>
              <div class="text-xs text-slate-400 truncate">UniHalo 使用指南</div>
            </div>
            <RiArrowRightSLine class="w-4 h-4 text-slate-300 flex-shrink-0" />
          </a>

          <a href="https://github.com/uni-halo/uni-halo-plugin"
            class="flex items-center gap-4 bg-white rounded-xl p-4 border border-slate-100 hover:shadow-md transition-all duration-200"
            target="_blank">
            <div class="w-10 h-10 rounded-lg flex items-center justify-center text-white flex-shrink-0"
              style="background: linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%)">
              <RiGithubLine class="w-5 h-5" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="font-semibold text-slate-800 text-sm">插件源码</div>
              <div class="text-xs text-slate-400 truncate">GitHub 仓库，欢迎 Star</div>
            </div>
            <RiArrowRightSLine class="w-4 h-4 text-slate-300 flex-shrink-0" />
          </a>

          <a href="https://github.com/ialley-workshop-open/uni-halo"
            class="flex items-center gap-4 bg-white rounded-xl p-4 border border-slate-100 hover:shadow-md transition-all duration-200"
            target="_blank">
            <div class="w-10 h-10 rounded-lg flex items-center justify-center text-white flex-shrink-0"
              style="background: linear-gradient(135deg, #06B6D4 0%, #0891B2 100%)">
              <RiCodeBoxLine class="w-5 h-5" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="font-semibold text-slate-800 text-sm">小程序源码</div>
              <div class="text-xs text-slate-400 truncate">UniHalo v3.x 开源代码</div>
            </div>
            <RiArrowRightSLine class="w-4 h-4 text-slate-300 flex-shrink-0" />
          </a>

          <a href="https://uni-halo.ialley.cn"
            class="flex items-center gap-4 bg-white rounded-xl p-4 border border-slate-100 hover:shadow-md transition-all duration-200"
            target="_blank">
            <div class="w-10 h-10 rounded-lg flex items-center justify-center text-white flex-shrink-0"
              style="background: linear-gradient(135deg, #10B981 0%, #059669 100%)">
              <RiBookOpenLine class="w-5 h-5" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="font-semibold text-slate-800 text-sm">官网主页</div>
              <div class="text-xs text-slate-400 truncate">快速访问官网主页</div>
            </div>
            <RiArrowRightSLine class="w-4 h-4 text-slate-300 flex-shrink-0" />
          </a>

          <a href="https://docs.halo.run/developer-guide/plugin/introduction"
            class="flex items-center gap-4 bg-white rounded-xl p-4 border border-slate-100 hover:shadow-md transition-all duration-200"
            target="_blank">
            <div class="w-10 h-10 rounded-lg flex items-center justify-center text-white flex-shrink-0"
              style="background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%)">
              <RiFileCodeLine class="w-5 h-5" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="font-semibold text-slate-800 text-sm">Halo 开发者文档</div>
              <div class="text-xs text-slate-400 truncate">插件开发指南与 API 参考</div>
            </div>
            <RiArrowRightSLine class="w-4 h-4 text-slate-300 flex-shrink-0" />
          </a>

          <a href="https://blog.xiaoxiaomo.cn/"
            class="flex items-center gap-4 bg-white rounded-xl p-4 border border-slate-100 hover:shadow-md transition-all duration-200"
            target="_blank">
            <div class="w-10 h-10 rounded-lg flex items-center justify-center text-white flex-shrink-0"
              style="background: linear-gradient(135deg, #EC4899 0%, #DB2777 100%)">
              <RiLinksLine class="w-5 h-5" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="font-semibold text-slate-800 text-sm">作者博客</div>
              <div class="text-xs text-slate-400 truncate">更多技术分享与经验</div>
            </div>
            <RiArrowRightSLine class="w-4 h-4 text-slate-300 flex-shrink-0" />
          </a>
        </div>
      </div>
    </section>

    <!-- Author Section -->
    <section class="py-16 sm:py-20 bg-white">
      <div class="max-w-5xl mx-auto px-4">
        <div class="text-center mb-12">
          <h2 class="text-3xl sm:text-4xl font-bold text-slate-800 mb-4">关于作者</h2>
          <p class="text-lg text-slate-500">项目的开发者与维护者</p>
        </div>

        <div class="bg-white rounded-3xl p-8 shadow-sm sm:p-10 border border-slate-100">
          <div class="flex flex-col sm:flex-row items-center gap-8">
            <!-- 头像 -->
            <div class="flex-shrink-0">
              <img src="https://www.xiaoxiaomo.cn/logo.jpg" alt="小莫唐尼" class="w-28 h-28 rounded-full object-cover" />
            </div>

            <!-- 信息 -->
            <div class="flex-1 text-center sm:text-left">
              <h3 class="text-2xl font-bold text-slate-800 mb-2">小莫唐尼</h3>
              <p class="text-slate-500 mb-1">全栈开发者 / 开源爱好者</p>
              <p class="text-sm text-slate-400">一个爱凑热闹，喜欢捣鼓前端的程序员</p>
            </div>

            <!-- 按钮 -->
            <div class="flex gap-3">
              <a href="https://www.xiaoxiaomo.cn/"
                class="inline-flex items-center gap-1.5 text-[#B9E424] bg-slate-100 text-sm font-medium px-3 py-1.5 rounded-lg transition-colors"
                target="_blank">
                <RiHomeLine class="w-3.5 h-3.5" />
                <span>主页</span>
              </a>
              <a href="https://blog.xiaoxiaomo.cn/"
                class="inline-flex items-center gap-1.5 text-[#B9E424] bg-slate-100 text-sm font-medium px-3 py-1.5 rounded-lg transition-colors"
                target="_blank">
                <RiBookOpenLine class="w-3.5 h-3.5" />
                <span>博客</span>
              </a>
              <a href="https://github.com/okay-xiaomo"
                class="inline-flex items-center gap-1.5 text-sm font-medium px-3 py-1.5 rounded-lg bg-slate-100 text-slate-600 hover:bg-slate-200 transition-colors"
                target="_blank">
                <RiGithubLine class="w-3.5 h-3.5" />
                <span>GitHub</span>
              </a>
              <a href="mailto:1431128779@qq.com"
                class="inline-flex items-center gap-1.5 text-sm font-medium px-3 py-1.5 rounded-lg bg-slate-100 text-slate-600 hover:bg-slate-200 transition-colors">
                <RiMailLine class="w-3.5 h-3.5" />
                <span>邮箱</span>
              </a>
            </div>
          </div>

          <!-- 联系方式网格 -->
          <div class="mt-8 pt-8 border-t border-slate-100">
            <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
              <a v-for="item in contacts" :key="item.label" :href="item.href"
                class="flex flex-col items-center gap-2 p-4 rounded-xl bg-slate-50 border border-slate-100 hover:border-slate-200 hover:shadow-sm transition-all duration-200"
                :target="item.href.startsWith('http') ? '_blank' : undefined">
                <component :is="item.icon" class="w-5 h-5" style="color: #B9E424" />
                <span class="text-xs text-slate-400">{{ item.label }}</span>
                <span class="text-xs font-medium text-slate-600 text-center leading-tight">{{ item.value }}</span>
              </a>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
@keyframes float {

  0%,
  100% {
    transform: translateY(0) rotate(0deg);
  }

  50% {
    transform: translateY(-20px) rotate(5deg);
  }
}

@keyframes float-delayed {

  0%,
  100% {
    transform: translateY(0) rotate(0deg);
  }

  50% {
    transform: translateY(-30px) rotate(-5deg);
  }
}

@keyframes pulse-slow {

  0%,
  100% {
    opacity: 0.3;
    transform: scale(1);
  }

  50% {
    opacity: 0.5;
    transform: scale(1.05);
  }
}

@keyframes bounce-slow {

  0%,
  100% {
    transform: translate(-50%, 0);
  }

  50% {
    transform: translate(-50%, -10px);
  }
}

@keyframes bounce-slow-delayed {

  0%,
  100% {
    transform: translateY(0) rotate(45deg);
  }

  50% {
    transform: translateY(-15px) rotate(45deg);
  }
}

.animate-float {
  animation: float 6s ease-in-out infinite;
}

.animate-float-delayed {
  animation: float-delayed 8s ease-in-out infinite;
}

.animate-pulse-slow {
  animation: pulse-slow 4s ease-in-out infinite;
}

.animate-bounce-slow {
  animation: bounce-slow 3s ease-in-out infinite;
}

.animate-bounce-slow-delayed {
  animation: bounce-slow-delayed 4s ease-in-out infinite;
}
</style>
