// 领域类型统一管理（对齐 plugin-links 的组织方式：types/ 单独目录）

import type { Metadata } from "@halo-dev/api-client";

export interface AppPlatformInfo {
  name?: string;
  url?: string;
}

export interface AppInfoSpec {
  appid?: string;
  name?: string;
  description?: string;
  intro?: string;
  iconUrl?: string;
  screenshot?: string[];
  appAndroid?: AppPlatformInfo;
  appIos?: AppPlatformInfo;
  appHarmony?: AppPlatformInfo;
}

export interface AppInfo {
  metadata: Metadata;
  spec: AppInfoSpec;
}

export interface AppVersionSpec {
  appid?: string;
  name?: string;
  title?: string;
  contents?: string;
  platform?: string[];
  type?: "native_app" | "wgt";
  version?: string;
  versionCode?: number;
  isDeleted?: boolean;
  minUniVersion?: string;
  url?: string;
  stablePublish?: boolean;
  isSilently?: boolean;
  isMandatory?: boolean;
}

export interface AppVersion {
  metadata: Metadata;
  spec: AppVersionSpec;
}

export interface PageResult<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}

export interface ListQuery {
  page?: number;
  size?: number;
  keyword?: string;
  appid?: string;
  platform?: string;
  type?: string;
  stablePublish?: string;
}

export const TYPE_LABELS: Record<string, string> = {
  native_app: "整包",
  wgt: "wgt 资源包",
};

export const PLATFORMS = ["Android", "iOS", "Harmony"];

// ===== 恋爱管理 =====

export interface LoveAlbumPhoto {
  name?: string;
  url?: string;
  /** 照片标题 */
  title?: string;
  description?: string;
  takenDate?: string;
  /** 拍摄地点 */
  location?: string;
  priority?: number;
}

export interface LoveAlbumSpec {
  displayName?: string;
  description?: string;
  cover?: string;
  passwordEnabled?: boolean;
  priority?: number;
  photos?: LoveAlbumPhoto[];
}

export interface LoveAlbumStatus {
  photoCount?: number;
}

export interface LoveAlbum {
  metadata: Metadata;
  spec: LoveAlbumSpec;
  status?: LoveAlbumStatus;
}

export interface LoveDailyItemSpec {
  title?: string;
  content?: string;
  status?: "wait" | "doing" | "complete";
  /** 计划时间（可选，yyyy-MM-dd） */
  planDate?: string;
  completeDate?: string;
  /** 完成感想（可选） */
  completeRemark?: string;
  /** 图片列表（多图，Halo 附件 URL 列表） */
  images?: string[];
  priority?: number;
}

export interface LoveDailyItem {
  metadata: Metadata;
  spec: LoveDailyItemSpec;
}

export const LOVE_STATUS_LABELS: Record<string, string> = {
  wait: "未开始",
  doing: "进行中",
  complete: "已完成",
};

/** 恋爱清单状态选项（统一供下拉/筛选使用，数组形式） */
export const LOVE_STATUS_OPTIONS: { label: string; value: string }[] = [
  { label: "未开始", value: "wait" },
  { label: "进行中", value: "doing" },
  { label: "已完成", value: "complete" },
];

/** 恋爱故事展示方式 */
export type LoveStoryViewMode = "list" | "timeline";

/** 恋爱故事展示方式选项 */
export const LOVE_STORY_VIEW_MODES: { label: string; value: LoveStoryViewMode }[] = [
  { label: "列表", value: "list" },
  { label: "时间轴", value: "timeline" },
];

/** 恋爱故事日期排序选项 */
export const LOVE_STORY_SORT_OPTIONS: { label: string; value: string }[] = [
  { label: "按日期 · 最新在前", value: "date_desc" },
  { label: "按日期 · 最早在前", value: "date_asc" },
];

/** 列表创建时间排序选项（恋爱相册/恋爱清单通用） */
export const LOVE_LIST_SORT_OPTIONS: { label: string; value: string }[] = [
  { label: "最新创建", value: "created_desc" },
  { label: "最早创建", value: "created_asc" },
];

/** 恋爱清单展示方式 */
export type LoveDailyViewMode = "list" | "timeline";

/** 恋爱清单展示方式选项 */
export const LOVE_DAILY_VIEW_MODES: { label: string; value: LoveDailyViewMode }[] = [
  { label: "列表", value: "list" },
  { label: "时间轴", value: "timeline" },
];

/** 恋爱清单时间轴排序选项 */
export const LOVE_DAILY_TIME_SORT_OPTIONS: { label: string; value: string }[] = [
  { label: "计划时间 · 最新在前", value: "plan_desc" },
  { label: "计划时间 · 最早在前", value: "plan_asc" },
  { label: "完成时间 · 最新在前", value: "complete_desc" },
  { label: "完成时间 · 最早在前", value: "complete_asc" },
];

export interface LoveStorySpec {
  title?: string;
  content?: string;
  date?: string;
  /** 故事地点 */
  location?: string;
  images?: string[];
  priority?: number;
}

export interface LoveStory {
  metadata: Metadata;
  spec: LoveStorySpec;
}

// ===== 通知公告 =====

export type NoticeStatus = "draft" | "published" | "offline";

export interface NoticeSpec {
  title?: string;
  /** 富文本正文（编辑器输出的 HTML） */
  content?: string;
  /** 摘要（手填可选，为空时服务端自动从正文剥离生成） */
  summary?: string;
  /** 封面图（Halo 附件 URL） */
  cover?: string;
  /** 外链地址（可选） */
  link?: string;
  /** 公告类型（关联 NoticeType.metadata.name，可为空=不分类） */
  typeName?: string;
  status?: NoticeStatus;
  /** 排序，越大越靠前 */
  priority?: number;
  /** 发布时间（状态为已发布时服务端自动记录） */
  publishTime?: string;
}

export interface Notice {
  metadata: Metadata;
  spec: NoticeSpec;
}

export interface NoticeTypeSpec {
  /** 类型名称（必填），如「活动」「维护」 */
  displayName?: string;
  /** 标签颜色（hex，如 #10B981） */
  color?: string;
  /** 排序，越大越靠前 */
  priority?: number;
}

export interface NoticeType {
  metadata: Metadata;
  spec: NoticeTypeSpec;
}

export const NOTICE_STATUS_LABELS: Record<string, string> = {
  draft: "草稿",
  published: "已发布",
  offline: "已下线",
};

/** 公告状态选项（统一供下拉/筛选使用） */
export const NOTICE_STATUS_OPTIONS: { label: string; value: string }[] = [
  { label: "草稿", value: "draft" },
  { label: "已发布", value: "published" },
  { label: "已下线", value: "offline" },
];

// ===== 轮播图 =====

export type BannerSource = "post" | "custom";

export interface BannerSpec {
  /** 标题（必填；文章模式为快照的文章标题） */
  title?: string;
  /** 封面图（必填；文章模式为快照的文章封面） */
  cover?: string;
  /** 展示日期（ISO；文章模式快照文章发布时间，自定义手填可选，留空不展示） */
  date?: string;
  /** 作者昵称（文章模式快照作者，User 缺失时回退 owner 用户名） */
  authorName?: string;
  /** 作者头像 */
  authorAvatar?: string;
  /** 来源：post/custom，服务端按 postId 非空自动判定，无需手动设置 */
  source?: BannerSource;
  /** 内容（富文本 HTML，仅自定义模式；文章模式恒为空） */
  content?: string;
  /** 备注（仅管理端可见，不对外展示） */
  remark?: string;
  /** 文章 id（Post 的 metadata.name；文章模式必填，小程序端据此跳转文章详情） */
  postId?: string;
  /** 外链（可选，自定义模式可填跳转链接） */
  link?: string;
  /** 排序，越大越靠前 */
  priority?: number;
}

export interface Banner {
  metadata: Metadata;
  spec: BannerSpec;
}

/** 文章候选（管理端文章选择器数据源，仅已发布文章） */
export interface BannerCandidate {
  /** Post 的 metadata.name（保存时写入 Banner.spec.postId） */
  name: string;
  title?: string;
  cover?: string;
  publishTime?: string;
  categories?: string[];
}

/** 来源标签映射（列表展示用） */
export const BANNER_SOURCE_LABELS: Record<string, string> = {
  post: "文章",
  custom: "自定义",
};

/** 来源筛选选项（列表 header 下拉） */
export const BANNER_SOURCE_OPTIONS: { label: string; value: string }[] = [
  { label: "文章", value: "post" },
  { label: "自定义", value: "custom" },
];

/** 列表排序选项（默认手动排序=拖拽顺序） */
export const BANNER_SORT_OPTIONS: { label: string; value: string }[] = [
  { label: "手动排序", value: "manual" },
  { label: "日期 · 最新在前", value: "date_desc" },
  { label: "日期 · 最早在前", value: "date_asc" },
];

/** 公告列表排序选项（右侧列表 header 使用） */
export const NOTICE_SORT_OPTIONS: { label: string; value: string }[] = [
  { label: "日期 · 最新在前", value: "date_desc" },
  { label: "日期 · 最早在前", value: "date_asc" },
  { label: "按类型分组", value: "type" },
];

// ===== 友情链接（小程序链接） =====

export interface MiniProgramLinkSpec {
  /** 小程序名称（必填） */
  displayName?: string;
  /** 太阳码（小程序码图片 URL，必填） */
  miniProgramCode?: string;
  /** 小程序 AppID（wx 开头，必填） */
  appId?: string;
  /** 跳转页面路径（非必填） */
  path?: string;
  /** 小程序地址（非必填） */
  link?: string;
  /** 作者昵称 */
  authorName?: string;
  /** 作者头像（图片 URL） */
  avatar?: string;
  /** 作者网站（归属作者信息，非必填） */
  website?: string;
  /** 分组（引用 MiniProgramLinkGroup 的 metadata.name；为空=未分组） */
  groupName?: string;
  /** 描述 */
  description?: string;
  /** 预览图（多图） */
  screenshots?: string[];
  /** 可见性：true 公开显示（默认）/ false 隐藏 */
  visible?: boolean;
  /** 来源（服务端按操作自动设置）：manual 手动添加 / submitted 自助申请 */
  source?: "manual" | "submitted";
  /** 排序，越大越靠前 */
  priority?: number;
}

/** 链接来源标签映射 */
export const LINK_SOURCE_LABELS: Record<string, string> = {
  manual: "手动",
  submitted: "申请",
};

/** 链接来源筛选选项（「全部」无 value=清除筛选） */
export const LINK_SOURCE_OPTIONS: { label: string; value?: string }[] = [
  { label: "全部" },
  { label: "手动", value: "manual" },
  { label: "申请", value: "submitted" },
];

export interface MiniProgramLink {
  metadata: Metadata;
  spec: MiniProgramLinkSpec;
}

export interface MiniProgramLinkGroupSpec {
  /** 分组名称（必填），如「工具」「生活」 */
  displayName?: string;
  /** 排序，越大越靠前 */
  priority?: number;
}

/** 分组模型（对标 plugin-links LinkGroup） */
export interface MiniProgramLinkGroup {
  metadata: Metadata;
  spec: MiniProgramLinkGroupSpec;
}

/** 分组选项（公开 /types 接口返回，供筛选/分组标题映射） */
export interface GroupOption {
  name: string;
  displayName: string;
}

/** 分组视图（公开接口 grouped=true 返回） */
export interface MiniProgramLinkGroupVo {
  /** 分组 name（空字符串=未分组） */
  groupName: string;
  /** 分组显示名（分组不存在或未分组时为空） */
  displayName: string;
  links: MiniProgramLink[];
}

export type SubmissionStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface MiniProgramLinkSubmissionSpec {
  displayName?: string;
  miniProgramCode?: string;
  /** 小程序 AppID（wx 开头，必填） */
  appId?: string;
  /** 跳转页面路径（非必填） */
  path?: string;
  link?: string;
  authorName?: string;
  avatar?: string;
  website?: string;
  /** 分组（引用 MiniProgramLinkGroup 的 metadata.name；为空=未分组） */
  groupName?: string;
  description?: string;
  /** 申请说明（小程序端提交时填写） */
  applyRemark?: string;
  screenshots?: string[];
  /** 申请人邮箱（非必填；填写则审核结果邮件通知） */
  email?: string;
  status?: SubmissionStatus;
  /** 审核结果说明（拒绝时必填） */
  reason?: string;
  /** 提交时间（服务端自动记录） */
  submittedAt?: string;
  /** 审核时间（服务端自动记录） */
  reviewedAt?: string;
  /** 审核通过后生成的链接 name */
  linkName?: string;
}

export interface MiniProgramLinkSubmission {
  metadata: Metadata;
  spec: MiniProgramLinkSubmissionSpec;
}

export const SUBMISSION_STATUS_LABELS: Record<string, string> = {
  PENDING: "待审核",
  APPROVED: "已通过",
  REJECTED: "已拒绝",
};

/** 申请状态选项（统一供下拉/筛选使用；「全部」无 value=清除筛选） */
export const SUBMISSION_STATUS_OPTIONS: { label: string; value?: string }[] = [
  { label: "全部" },
  { label: "待审核", value: "PENDING" },
  { label: "已通过", value: "APPROVED" },
  { label: "已拒绝", value: "REJECTED" },
];

// ===== 审核模式 =====

/** 被选中引用的快照（name 为扩展 metadata.name，其余字段按类型选择性填充） */
export interface AuditDataRef {
  name: string;
  title?: string;
  cover?: string;
  subTitle?: string;
  extra?: string;
  /** 排序权重（分类=spec.priority，精选分类快照排序用；其余类型可空） */
  priority?: number;
  /** 文章数（分类=status.postCount，缺失默认 0；app 端审核模式免请求复用） */
  postCount?: number;
}

/** 审核模式模拟数据的选中引用（对象快照存储，数组顺序即展示顺序） */
export interface AuditDataConfigSpec {
  /** 选中的文章 Post 引用列表 */
  posts?: AuditDataRef[];
  /** 选中的分类 Category 引用列表 */
  categories?: AuditDataRef[];
  /** 选中的图库分组 PhotoGroup 引用列表（未分组照片不展示） */
  galleryGroups?: AuditDataRef[];
  /** 选中的瞬间 Moment 引用列表 */
  moments?: AuditDataRef[];
  /** 选中的链接分组 LinkGroup 引用列表 */
  linkGroups?: AuditDataRef[];
  /** 备注（如「微信审核用模拟数据」） */
  description?: string;
}

export interface AuditDataConfig {
  metadata: Metadata;
  spec: AuditDataConfigSpec;
}

/** 审核模式详情（管理端 GET /audit-data 返回）：原始配置 + 各类型已选条目的最新详情 */
export interface AuditDataConfigDetail {
  config: AuditDataConfig;
  selections: Record<AuditCandidateType, AuditDataRef[]>;
}

export type AuditCandidateType =
  | "post"
  | "category"
  | "galleryGroup"
  | "moment"
  | "linkGroup";

/** 候选数据分页结果（条目即 AuditDataRef 快照，可直接用于已选列表渲染） */
export interface AuditDataCandidateResult {
  items: AuditDataRef[];
  page: number;
  size: number;
  total: number;
  /** 数据源插件未安装/扩展未注册（UI 提示「请先安装 XX 插件」） */
  pluginMissing?: boolean;
}

/** 候选类型中文名（UI 展示/提示用） */
export const AUDIT_CANDIDATE_TYPE_LABELS: Record<AuditCandidateType, string> = {
  post: "文章",
  category: "分类",
  galleryGroup: "图库",
  moment: "瞬间",
  linkGroup: "友链",
};

/** 候选类型对应依赖插件提示（pluginMissing 时展示） */
export const AUDIT_CANDIDATE_PLUGIN_HINTS: Partial<Record<AuditCandidateType, string>> = {
  galleryGroup: "请先安装 plugin-photos（图库插件）",
  moment: "请先安装 plugin-moments（瞬间插件）",
  linkGroup: "请先安装 plugin-links（链接管理插件）",
};

// ===== 通用配置（FeatureConfig 单例，name 固定 feature-config）=====

export interface FeatureConfig {
  metadata?: { name?: string };
  spec: FeatureConfigSpec;
}

export interface FeatureConfigSpec {
  profile: FeatureConfigProfile;
  pages: FeatureConfigPages;
  assets: FeatureConfigAssets;
  preferences: FeatureConfigPreferences;
  /** 恋爱模块（经 getConfigs loveConfig 组下发；恋爱日记仅密码状态，三模块入口
   * 自身承载 app 端入口列表数据：title/subTitle/颜色/iconBgColor/path/priority） */
  love: FeatureConfigLove;
  /** 友链设置：站长小程序展示信息，经 getConfigs 覆盖
   * pluginConfig.linksSubmitPlugin 对应键下发，供小程序端「申请信息」弹窗展示 */
  linkInfo: FeatureConfigLinkInfo;
  /** 维护模式 */
  maintenance: FeatureConfigMaintenance;
  /** 审核模式：开启后关闭小程序部分数据展示，小程序提交审核时建议开启；
   * 经 getConfigs 重建回旧 auditConfig.auditModeEnabled 形态下发 */
  auditMode: { enabled?: boolean };
}

/**
 * 友链设置：基本设置（公开提交申请开关）/ miniInfo 小程序信息 / siteInfo 站点信息
 * （字段对齐 Halo 官方 plugin-links 友链提交 API）；
 * 经 getConfigs 直接下发 pluginConfig.linkInfo
 */
export interface FeatureConfigLinkInfo {
  /** 是否开放公开提交申请（默认 true，关闭后公开提交接口返回「暂未开放提交申请」） */
  submissionEnabled?: boolean;
  /** 小程序信息（小程序名称/太阳码/跳转地址/描述/申请说明） */
  miniInfo?: FeatureConfigMiniInfo;
  /** 站点信息（本站站点名片，对齐 Halo 官方友链提交 API 字段） */
  siteInfo?: FeatureConfigSiteInfo;
}

/** 小程序信息（app 端「申请信息」弹窗展示项） */
export interface FeatureConfigMiniInfo {
  /** 小程序名称 */
  displayName?: string;
  /** 太阳码/小程序码图片 */
  miniProgramCode?: string;
  /** 小程序 AppID（wx 开头，必填） */
  appId?: string;
  /** 跳转页面路径（非必填） */
  path?: string;
  /** 跳转地址 */
  link?: string;
  /** 小程序描述 */
  description?: string;
  /** 申请说明 */
  applyRemark?: string;
}

/** 站点信息（字段对齐 Halo 官方 plugin-links 友链提交 API：link-applications 请求体） */
export interface FeatureConfigSiteInfo {
  /** 网站名称（官方 displayName） */
  displayName?: string;
  /** 网站地址（官方 url，HTTP/HTTPS） */
  url?: string;
  /** 网站 Logo 地址（官方 logo） */
  logo?: string;
  /** 网站描述（官方 description） */
  description?: string;
  /** 反链页面地址（官方 backlink） */
  backlink?: string;
  /** RSS/Atom 订阅地址（官方 feedUrls；表单换行分隔存数组） */
  feedUrls?: string[];
}

export interface FeatureConfigProfile {
  appInfo: {
    name?: string;
    logo?: string;
  };
  blogger: {
    nickname?: string;
    avatar?: string;
    email?: string;
    description?: string;
    /** 主页 */
    website?: string;
    /** 介绍（富文本 HTML，app 端联系博主页 mp-html 渲染） */
    intro?: string;
  };
  social: {
    /** 社交项列表（app 端联系博主页按序渲染） */
    items?: FeatureConfigSocialItem[];
  };
  /** 页脚版权（显示于关于页页脚） */
  copyrightConfig?: {
    enabled?: boolean;
    content?: string;
  };
}

/** 社交项（app 端联系博主页展示/复制；图标由 app 端按 color/bgColor 色块渲染；
 * 仅名称/内容/颜色/背景色/排序/显隐） */
export interface FeatureConfigSocialItem {
  /** 名称（如「企鹅号」「微信号」） */
  name?: string;
  /** 内容（账号/地址/链接，点击复制） */
  content?: string;
  /** 图标颜色（16 进制，支持透明） */
  color?: string;
  /** 背景色（16 进制，支持透明） */
  bgColor?: string;
  /** 排序（越大越靠前） */
  priority?: number;
  /** 是否展示 */
  visible?: boolean;
}

/** 全站页面标题（页面设置-页面标题 tab 统一维护；app 端传入各页面 uh-navbar default-title，留空回退内置默认） */
export interface FeatureConfigPageTitles {
  home?: string;
  gallery?: string;
  category?: string;
  moments?: string;
  blogger?: string;
  articles?: string;
  archives?: string;
  /** 文章详情页导航栏默认标题（滚动后仍显示文章题目） */
  postDetail?: string;
  categoryArticles?: string;
  tags?: string;
  tagDetail?: string;
  search?: string;
  favorites?: string;
  friendLinks?: string;
  notice?: string;
  noticeDetail?: string;
  votes?: string;
  voteDetail?: string;
  contact?: string;
  setting?: string;
  aboutProject?: string;
  disclaimers?: string;
  dataVisual?: string;
  login?: string;
  register?: string;
}

export interface FeatureConfigPages {
  /** 全站页面标题 */
  titles?: FeatureConfigPageTitles;
  homeConfig: {
    useQuickNavigation?: boolean;
    /** 快捷导航项列表（每项可配置名称/排序/显示隐藏，排序=数组顺序） */
    quickNavigation?: FeatureConfigQuickNavigationItem[];
    useCategory?: boolean;
    /** 首页分类栏展示的分类引用（固定 3 个，数据在「分类管理」维护） */
    categories?: FeatureConfigCategoryItem[];
  };
  /** 博主页（资料卡视觉 + 常用功能布局） */
  aboutConfig: {
    bgImageUrl?: string;
    waveImageUrl?: string;
    /** 常用功能显示方式（grid=宫格 / list=列表，控制 app 端博主页常用功能布局；缺省网格） */
    commonFeaturesMode?: "grid" | "list";
    /** 页脚版权（显示于博主页页脚） */
    copyrightConfig?: {
      enabled?: boolean;
      content?: string;
    };
  };
  /** 我的页面功能入口（常用功能/其他功能两组，配置并入「博主页」tab，
   * 经 getConfigs 下发 pageConfig.myPageConfig） */
  myPageConfig?: FeatureConfigMyPage;
  /** 免责声明页（不再需要启用开关，仅内容） */
  disclaimers?: {
    content?: string;
  };
  /** 文章详情页内容与版权文案 */
  postDetailConfig?: {
    showComment?: boolean;
    copyrightEnabled?: boolean;
    copyrightAuthor?: string;
    copyrightDesc?: string;
    copyrightViolation?: string;
  };
}

/** 快捷导航项（字段与客户端 uh-home-quick-nav 对齐） */
export interface FeatureConfigQuickNavigationItem {
  key?: string;
  title?: string;
  /** 副标题（对标 app 端 rightText，如「全部文章」，可空） */
  subTitle?: string;
  /** 图标颜色（十六进制色值，如 #03A9F4） */
  color?: string;
  /** 背景色（rgba 半透明值） */
  bgColor?: string;
  /** 图标字体前缀（如 uhemoji2-icon） */
  iconPrefix?: string;
  /** 图标名（如 -mask） */
  icon?: string;
  /** 跳转路径（小程序页面路径） */
  path?: string;
  /** 是否显示 */
  visible?: boolean;
}

/** 我的页面功能入口（常用功能/其他功能两组，条目复用快捷导航项结构，
 * app 端 about 页按组渲染） */
export interface FeatureConfigMyPage {
  /** 常用功能 */
  commonFeatures?: FeatureConfigQuickNavigationItem[];
  /** 其他功能 */
  otherFeatures?: FeatureConfigQuickNavigationItem[];
}

/** 首页分类栏选中引用（固定 3 个；name = Category.metadata.name，快照含名称/封面/排序/文章数，
 * 数组顺序 = 展示排序；app 端直接按快照渲染，不发请求） */
export interface FeatureConfigCategoryItem {
  name?: string;
  /** 分类名称（展示用冗余快照） */
  displayName?: string;
  /** 分类封面图（展示用冗余快照，选中时保存） */
  cover?: string;
  /** 分类排序权重（Halo Category.spec.priority 冗余快照，越大越靠前） */
  priority?: number;
  /** 分类文章数（Halo Category.status.postCount 冗余快照，缺失默认 0） */
  postCount?: number;
}

export interface FeatureConfigAssets {
  /** 加载中的图片 */
  loadingGifUrl?: string;
  /** 加载失败图片 */
  loadingErrUrl?: string;
}

export interface FeatureConfigPreferences {
  /** 首页列表布局（L0 默认，客户端 layout.home.listLayout）：single 单列 / double 双列 */
  homeListLayout?: "single" | "double";
  /** 首页卡片样式（L0 默认，客户端 layout.home.cardType） */
  homeCardType?: "image_top" | "image_right" | "image_bottom" | "image_left";
  /** 文章列表页列表布局（L0 默认，客户端 layout.articles.listLayout）：single / double */
  articlesListLayout?: "single" | "double";
  /** 文章列表页卡片样式（L0 默认，客户端 layout.articles.cardType） */
  articleCardType?: "image_top" | "image_right" | "image_bottom" | "image_left";
  /** 文章归档页列表布局（L0 默认，客户端 layout.archives.listLayout）：single / double */
  archivesListLayout?: "single" | "double";
  /** 文章归档页卡片样式（L0 默认，客户端 layout.archives.cardType） */
  archivesCardType?: "image_top" | "image_right" | "image_bottom" | "image_left";
  /** 头像外观（L0 默认，客户端 preferences.avatarShape）：square 方形 / circle 圆形；应用于文章卡片(上文下图)与瞬间卡片 */
  avatarShape?: "square" | "circle";
  /** 友情链接页展示偏好（小程序打开模式等） */
  linkPage?: FeatureConfigLinkPage;
}

/** 友情链接页展示偏好 */
export interface FeatureConfigLinkPage {
  /** 小程序打开模式：fullscreen 全屏（navigateToMiniProgram，默认）/ halfScreen 半屏（openEmbeddedMiniProgram） */
  miniProgramOpenMode?: "fullscreen" | "halfScreen";
}

/** 恋爱模块（恋爱日记入口仅密码状态无开关；三模块入口自身即 app 端入口列表数据） */
export interface FeatureConfigLove {
  /** 恋爱日记入口（恋爱页本身，仅密码设置无 enabled 开关；入口显隐由
   * 页面设置-快捷导航/关于页功能入口注册表控制） */
  loveDiary?: FeatureConfigLoveModule;
  /** 恋爱故事模块入口（数据在「恋爱管理-恋爱故事」维护） */
  ourStory?: FeatureConfigLoveModule;
  /** 恋爱相册模块入口（数据在「恋爱管理-恋爱相册」维护） */
  lovePhoto?: FeatureConfigLoveModule;
  /** 恋爱清单模块入口（数据在「恋爱管理-恋爱清单」维护） */
  loveDaily?: FeatureConfigLoveModule;
  /** 恋爱信息（纪念日 + 恋人信息，配置于恋爱设置-恋爱信息 tab，
   * 经 getConfigs 下发 loveConfig.loveInfo） */
  loveInfo?: {
    /** 纪念日标题（默认「这是我们一起走过的」） */
    loveDateTitle?: string;
    /** 恋爱纪念日（yyyy-MM-dd），用于计算恋爱天数 */
    loveDate?: string;
    /** 男生昵称 */
    boyNickname?: string;
    /** 男生头像 */
    boyAvatar?: string;
    /** 女生昵称 */
    girlNickname?: string;
    /** 女生头像 */
    girlAvatar?: string;
  };
  /** 恋爱日记页面设置（配置于恋爱设置-页面设置 tab；app 端输出 shape 不变，
   * 仍为 pageConfig.loveDiaryConfig） */
  diaryPage?: {
    /** 页面标题（留空客户端回退内置标题） */
    pageTitle?: string;
    /** 恋爱页背景图（留空客户端内置回退） */
    bgImageUrl?: string;
  };
}

/**
 * 恋爱模块入口（三模块共用；loveDiary 仅使用密码相关字段）。
 * 密码语义与恋爱相册一致：passwordEnabled 为「已设置密码」视图状态（由后端按哈希
 * 派生，保存时忽略）；password 为新密码（留空 = 保持原密码）；passwordRemoved=true
 * = 清除该入口密码。后端一律不回显哈希，故无 passwordHash 字段。
 * 三模块（ourStory/lovePhoto/loveDaily）另承载 app 端入口列表数据：
 * title/subTitle/titleColor/subTitleColor/iconBgColor/path/priority
 * （app 端直接按模块 key 渲染入口，无需本地硬编码）。
 */
export interface FeatureConfigLoveModule {
  /** 是否在恋爱页展示该模块入口（loveDiary 不使用：入口显隐由快捷导航/功能入口注册表控制） */
  enabled?: boolean;
  /** 入口名称（app 端入口列表标题） */
  title?: string;
  /** 入口副标题（app 端入口列表副标题） */
  subTitle?: string;
  /** 标题颜色（hex8 #rrggbbaa） */
  titleColor?: string;
  /** 副标题颜色（hex8 #rrggbbaa） */
  subTitleColor?: string;
  /** 图标背景色（hex8 #rrggbbaa） */
  iconBgColor?: string;
  /** app 端跳转路径 */
  path?: string;
  /** 排序字段（越大越靠前） */
  priority?: number;
  /** 是否已设置密码（控制台 GET 返回，由后端派生；保存时忽略） */
  passwordEnabled?: boolean;
  /** 新密码（仅写请求；留空表示保持原密码不变） */
  password?: string;
  /** 是否清除密码（仅写请求；true = 保存后清除该入口密码并关闭验证） */
  passwordRemoved?: boolean;
}

/** 维护模式：维护页标题/富文本说明与排期窗口；实际状态由服务端
 * 按 enabled + startTime/endTime 与当前时间计算（scheduled/active 时 getConfigs
 * 顶层下发 maintenance 键，enabled=false 或到点自动结束则不输出，键缺失即未维护） */
export interface FeatureConfigMaintenance {
  /** 安排开关：开启后按时间窗口即时生效（startTime 为空/已过 = 立即进入维护中） */
  enabled?: boolean;
  /** 维护页标题（默认「站点维护中」） */
  title?: string;
  /** 维护说明（纯文本，textarea 编辑；小程序端维护页标题下方直接展示，留空则展示默认文案） */
  notice?: string;
  /** 维护详情（富文本 HTML，RichTextEditorField 编辑；小程序端「维护详情」弹窗展示） */
  description?: string;
  /** 维护开始时间（RFC3339 UTC，如 2026-09-05T02:00:00Z）；空 = 立即维护 */
  startTime?: string;
  /** 预计恢复时间（RFC3339 UTC）；空 = 持续至手动关闭；到点自动结束 */
  endTime?: string;
}

/** 用户详情「微信绑定」选项卡数据 */
export interface WechatBinding {
  /** Halo 用户名 */
  username: string;
  /** 是否已绑定微信 */
  bound: boolean;
  /** 绑定的微信标识（openid 或 unionid），未绑定为 undefined */
  providerUserId?: string;
  /** 绑定关系最近一次更新时间（RFC3339），未绑定为 undefined */
  boundAt?: string;
}

/** 扫码绑定票据（UC 侧创建返回） */
export interface BindTicketIssued {
  ticket: string;
  /** 二维码内容：uh-bindwx-{ticket}，小程序端按前缀识别 */
  qrContent: string;
  /** 过期时间（RFC3339） */
  expiresAt: string;
}

/** 扫码绑定票据轮询状态 */
export interface BindTicketStatus {
  ticket: string;
  /** PENDING 等待扫码 / CONFIRMED 已绑定 / EXPIRED 已过期 */
  status: "PENDING" | "CONFIRMED" | "EXPIRED";
}

/** getConfigs 顶层 maintenance 键（additive，仅 scheduled/active 时由服务端输出；
 * status 判定权威在服务端，客户端只算倒计时差值） */
export interface PublicMaintenance {
  /** scheduled 维护预告（倒计时至 startTime）/ active 维护中（倒计时至 endTime） */
  status: "scheduled" | "active";
  title?: string;
  description?: string;
  startTime?: string;
  endTime?: string;
}
