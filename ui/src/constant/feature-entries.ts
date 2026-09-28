import type { FeatureConfigNavIconStyle, FeatureConfigQuickNavigationItem } from "@/types";

/**
 * 功能入口注册表
 */
export interface FeatureEntry extends FeatureConfigQuickNavigationItem {
  /** 归属：我的页-常用 / 我的页-其他 */
  group: "common" | "other";
}

/** 便捷构造：双风格图标集合（ri=remixicon / emoji-font=emoji 字体） */
function dualIcons(riName: string, emojiPrefix: string, emojiName: string): FeatureConfigNavIconStyle[] {
  return [
    { key: "ri", prefix: "ri", iconName: riName },
    { key: "emoji-font", prefix: emojiPrefix, iconName: emojiName },
  ];
}

/**
 * 统一功能入口注册表
 */
export const FEATURE_ENTRY_REGISTRY: FeatureEntry[] = [
  // ===== 我的页面-常用功能（默认 8 项，顺序即展示顺序）=====
  {key: "contact-blogger", title: "联系博主", subTitle: "博主常用联系方式", color: "#FF9800", iconColor: "#FF9800", bgColor: "#FF980024", iconPrefix: "uhemoji2-icon", icon: "-wink", icons: dualIcons("mail-line", "uhemoji2-icon", "-wink"), iconMode: "emoji-font", path: "/pages-blog/contact/contact", visible: true, group: "common"},
  {key: "notice", title: "通知公告", subTitle: "站点公告与通知", color: "#9C27B0", iconColor: "#9C27B0", bgColor: "#9C27B024", iconPrefix: "uhemoji-icon", icon: "-sleeping", icons: dualIcons("notification-2-line", "uhemoji-icon", "-sleeping"), iconMode: "emoji-font", path: "/pages-blog/notice/notice", visible: true, group: "common"},
  {key: "favorites", title: "我的收藏", subTitle: "笔记和瞬间收藏", color: "#FFB300", iconColor: "#FFB300", bgColor: "#FFB30024", iconPrefix: "uhemoji2-icon", icon: "-smiling", icons: dualIcons("star-smile-line", "uhemoji2-icon", "-smiling"), iconMode: "emoji-font", path: "/pages-blog/favorites/favorites", visible: true, group: "common"},
  {key: "love", title: "恋爱日记", subTitle: "博主的恋爱日记", color: "#FF4C67", iconColor: "#FF4C67", bgColor: "#FF4C6724", iconPrefix: "uhemoji2-icon", icon: "-in-love", icons: dualIcons("hearts-line", "uhemoji2-icon", "-in-love"), iconMode: "emoji-font", path: "/pages-blog/love/love", visible: true, group: "common"},
  {key: "friend-links", title: "友情链接", subTitle: "看看博主朋友们吧", color: "#009688", iconColor: "#009688", bgColor: "#00968824", iconPrefix: "uhemoji2-icon", icon: "-cool", icons: dualIcons("links-line", "uhemoji2-icon", "-cool"), iconMode: "emoji-font", path: "/pages-blog/friend-links/friend-links", visible: true, group: "common"},
  {key: "archives", title: "笔记归档", subTitle: "已经归档的笔记", color: "#03A9F4", iconColor: "#03A9F4", bgColor: "#03A9F424", iconPrefix: "uhemoji2-icon", icon: "-mask", icons: dualIcons("archive-line", "uhemoji2-icon", "-mask"), iconMode: "emoji-font", path: "/pages-blog/archives/archives", visible: true, group: "common"},
  {key: "vote", title: "投票中心", subTitle: "查看和进行投票", color: "#00BCD4", iconColor: "#00BCD4", bgColor: "#00BCD424", iconPrefix: "uhemoji2-icon", icon: "-confused", icons: dualIcons("chat-poll-line", "uhemoji2-icon", "-confused"), iconMode: "emoji-font", path: "/pages-blog/votes/votes", visible: true, group: "common"},
  {key: "data-visual", title: "数据看板", subTitle: "站点数据可视化", color: "#663CC9", iconColor: "#663CC9", bgColor: "#663CC924", iconPrefix: "uhemoji2-icon", icon: "-surprised", icons: dualIcons("pie-chart-box-line", "uhemoji2-icon", "-surprised"), iconMode: "emoji-font", path: "/pages-blog/data-visual/data-visual", visible: true, group: "common"},
  {key: "portfolio", title: "项目展示", subTitle: "博主的项目作品", color: "#3E87F7", iconColor: "#3E87F7", bgColor: "#3E87F724", iconPrefix: "uhemoji-icon", icon: "-shocked", icons: dualIcons("list-view", "uhemoji-icon", "-shocked"), iconMode: "emoji-font", path: "/pages-blog/portfolio/portfolio", visible: true, group: "common"},
  {key: "douban", title: "豆瓣展示", subTitle: "博主的书影音记录", color: "#43B024", iconColor: "#43B024", bgColor: "#43B02424", iconPrefix: "uhemoji-icon", icon: "-joy", icons: dualIcons("douban-line", "uhemoji-icon", "-joy"), iconMode: "emoji-font", path: "/pages-blog/douban/douban", visible: true, group: "common"},
  // ===== 我的页面-其他功能（默认 5 项，顺序即展示顺序）=====
  {key: "setting", title: "偏好设置", subTitle: "首页布局、卡片样式等设置", color: "#7986CB", iconColor: "#7986CB", bgColor: "#7986CB24", iconPrefix: "uhemoji2-icon", icon: "-tired", icons: dualIcons("settings-3-line", "uhemoji2-icon", "-tired"), iconMode: "emoji-font", path: "/pages-blog/setting/setting", visible: true, group: "other"},
  {key: "aboutProject", title: "关于项目", subTitle: "小莫唐尼的开源项目", color: "#607D8B", iconColor: "#607D8B", bgColor: "#607D8B24", iconPrefix: "uhemoji2-icon", icon: "-happy-", icons: dualIcons("information-2-line", "uhemoji2-icon", "-happy-"), iconMode: "emoji-font", path: "/pages-blog/about-project/about-project", visible: true, group: "other"},
  {key: "disclaimer", title: "免责声明", subTitle: "博客内容免责声明", color: "#795548", iconColor: "#795548", bgColor: "#79554824", iconPrefix: "uhemoji2-icon", icon: "-smirking", icons: dualIcons("telegram-2-line", "uhemoji2-icon", "-smirking"), iconMode: "emoji-font", path: "/pages-blog/disclaimer/disclaimer", visible: true, group: "other"},
  {key: "user-agreement", title: "用户协议", subTitle: "站点用户协议", color: "#8D6E63", iconColor: "#8D6E63", bgColor: "#8D6E6324", iconPrefix: "uhemoji2-icon", icon: "-wink", icons: dualIcons("contract-line", "uhemoji2-icon", "-wink"), iconMode: "emoji-font", path: "/pages-blog/user-agreement/user-agreement", visible: true, group: "other"},
  {key: "privacy-policy", title: "隐私政策", subTitle: "站点隐私政策", color: "#A1887F", iconColor: "#A1887F", bgColor: "#A1887F24", iconPrefix: "uhemoji2-icon", icon: "-secret", icons: dualIcons("spy-line", "uhemoji2-icon", "-secret"), iconMode: "emoji-font", path: "/pages-blog/privacy-policy/privacy-policy", visible: true, group: "other"},
];

/**
 * 首页快捷导航默认 5 项
 */
export const DEFAULT_QUICK_NAV_KEYS = ["love", "contact-blogger", "favorites", "friend-links", "aboutProject"];

/** 我的页面-常用功能默认 8 项 */
export const DEFAULT_MY_PAGE_COMMON_KEYS = [
  "contact-blogger", "notice", "favorites", "love", "friend-links", "archives", "vote", "data-visual",
];

/** 我的页面-其他功能默认 5 项（顺序即展示顺序，与服务端默认一致） */
export const DEFAULT_MY_PAGE_OTHER_KEYS = [
  "setting", "aboutProject", "disclaimer", "user-agreement", "privacy-policy",
];

/** 按归属组过滤注册表（候选弹窗统一清单不用；默认配置一律走显式 key 列表） */
export function featureEntriesByGroup(group: FeatureEntry["group"]): FeatureEntry[] {
  return FEATURE_ENTRY_REGISTRY.filter((entry) => entry.group === group);
}

/** 按 key 列表从注册表取条目（保持 key 列表顺序；未知 key 跳过） */
export function featureEntriesByKeys(keys: string[]): FeatureEntry[] {
  return keys
    .map((key) => FEATURE_ENTRY_REGISTRY.find((entry) => entry.key === key))
    .filter((entry): entry is FeatureEntry => !!entry);
}

/** 注册表条目 → 快捷导航项快照（去掉 group，写入配置） */
export function toQuickNavigationItem(entry: FeatureEntry): FeatureConfigQuickNavigationItem {
  const {group: _group, ...item} = entry;
  return {...item};
}
