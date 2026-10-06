import type { FeatureConfigNavIconStyle, FeatureConfigQuickNavigationItem } from "@/types";

/**
 * 功能入口注册表
 */
export interface FeatureEntry extends FeatureConfigQuickNavigationItem {
  /** 归属：我的页-常用 / 我的页-其他 */
  group: "common" | "other";
}

/**
 * 便捷构造：三风格图标集合
 * （animal-font=动物字体，默认生效 / emoji-font=emoji 字体 / ri=remixicon 兜底；
 * uhanimal 类名为单横杠拼接，iconName 不带前导 "-"）
 */
function navIcons(animalName: string, riName: string, emojiPrefix: string, emojiName: string): FeatureConfigNavIconStyle[] {
  return [
    { key: "animal-font", prefix: "uhanimal-icon", iconName: animalName },
    { key: "emoji-font", prefix: emojiPrefix, iconName: emojiName },
    { key: "ri", prefix: "ri", iconName: riName },
  ];
}

/**
 * 统一功能入口注册表
 */
export const FEATURE_ENTRY_REGISTRY: FeatureEntry[] = [
  // ===== 我的页面-常用功能（默认 11 项，顺序即展示顺序）=====
  {key: "footprint", title: "足迹地图", subTitle: "博主去过的地方", color: "#65a30d", iconColor: "#65a30d", bgColor: "#B9E42424", iconPrefix: "uhanimal-icon", icon: "keji", icons: navIcons("keji", "footprint-line", "uhemoji-icon", "-happy-"), iconMode: "animal-font", path: "/pages-blog/footprint/footprint", visible: true, group: "common"},
  {key: "contact-blogger", title: "联系博主", subTitle: "博主常用联系方式", color: "#FF9800", iconColor: "#FF9800", bgColor: "#FF980024", iconPrefix: "uhanimal-icon", icon: "chaiquan", icons: navIcons("chaiquan", "mail-line", "uhemoji2-icon", "-wink"), iconMode: "animal-font", path: "/pages-blog/contact/contact", visible: true, group: "common"},
  {key: "notice", title: "通知公告", subTitle: "站点公告与通知", color: "#9C27B0", iconColor: "#9C27B0", bgColor: "#9C27B024", iconPrefix: "uhanimal-icon", icon: "hashiqi", icons: navIcons("hashiqi", "notification-2-line", "uhemoji-icon", "-sleeping"), iconMode: "animal-font", path: "/pages-blog/notice/notice", visible: true, group: "common"},
  {key: "favorites", title: "我的收藏", subTitle: "笔记和瞬间收藏", color: "#FFB300", iconColor: "#FFB300", bgColor: "#FFB30024", iconPrefix: "uhanimal-icon", icon: "cangshu", icons: navIcons("cangshu", "star-smile-line", "uhemoji2-icon", "-smiling"), iconMode: "animal-font", path: "/pages-blog/favorites/favorites", visible: true, group: "common"},
  {key: "love", title: "恋爱日记", subTitle: "博主的恋爱日记", color: "#FF4C67", iconColor: "#FF4C67", bgColor: "#FF4C6724", iconPrefix: "uhanimal-icon", icon: "buoumao", icons: navIcons("buoumao", "hearts-line", "uhemoji2-icon", "-in-love"), iconMode: "animal-font", path: "/pages-blog/love/love", visible: true, group: "common"},
  {key: "friend-links", title: "友情链接", subTitle: "看看博主朋友们吧", color: "#009688", iconColor: "#009688", bgColor: "#00968824", iconPrefix: "uhanimal-icon", icon: "jinmao", icons: navIcons("jinmao", "links-line", "uhemoji2-icon", "-cool"), iconMode: "animal-font", path: "/pages-blog/friend-links/friend-links", visible: true, group: "common"},
  {key: "archives", title: "笔记归档", subTitle: "已经归档的笔记", color: "#03A9F4", iconColor: "#03A9F4", bgColor: "#03A9F424", iconPrefix: "uhanimal-icon", icon: "tianyuanquan", icons: navIcons("tianyuanquan", "archive-line", "uhemoji2-icon", "-mask"), iconMode: "animal-font", path: "/pages-blog/archives/archives", visible: true, group: "common"},
  {key: "vote", title: "投票中心", subTitle: "查看和进行投票", color: "#00BCD4", iconColor: "#00BCD4", bgColor: "#00BCD424", iconPrefix: "uhanimal-icon", icon: "nainiumao", icons: navIcons("nainiumao", "chat-poll-line", "uhemoji2-icon", "-confused"), iconMode: "animal-font", path: "/pages-blog/votes/votes", visible: true, group: "common"},
  {key: "data-visual", title: "数据看板", subTitle: "站点数据可视化", color: "#663CC9", iconColor: "#663CC9", bgColor: "#663CC924", iconPrefix: "uhanimal-icon", icon: "xianluomao", icons: navIcons("xianluomao", "pie-chart-box-line", "uhemoji2-icon", "-surprised"), iconMode: "animal-font", path: "/pages-blog/data-visual/data-visual", visible: true, group: "common"},
  {key: "portfolio", title: "项目展示", subTitle: "博主的项目作品", color: "#3E87F7", iconColor: "#3E87F7", bgColor: "#3E87F724", iconPrefix: "uhanimal-icon", icon: "cangao", icons: navIcons("cangao", "list-view", "uhemoji-icon", "-shocked"), iconMode: "animal-font", path: "/pages-blog/portfolio/portfolio", visible: true, group: "common"},
  {key: "douban", title: "豆瓣展示", subTitle: "博主的书影音记录", color: "#43B024", iconColor: "#43B024", bgColor: "#43B02424", iconPrefix: "uhanimal-icon", icon: "jumao", icons: navIcons("jumao", "douban-line", "uhemoji-icon", "-joy"), iconMode: "animal-font", path: "/pages-blog/douban/douban", visible: true, group: "common"},
  // ===== 我的页面-其他功能（默认 5 项，顺序即展示顺序）=====
  {key: "setting", title: "偏好设置", subTitle: "首页布局、卡片样式等设置", color: "#7986CB", iconColor: "#7986CB", bgColor: "#7986CB24", iconPrefix: "uhanimal-icon", icon: "sanhuamao", icons: navIcons("sanhuamao", "settings-3-line", "uhemoji2-icon", "-tired"), iconMode: "animal-font", path: "/pages-blog/setting/setting", visible: true, group: "other"},
  {key: "aboutProject", title: "关于项目", subTitle: "小莫唐尼的开源项目", color: "#607D8B", iconColor: "#607D8B", bgColor: "#607D8B24", iconPrefix: "uhanimal-icon", icon: "fadou", icons: navIcons("fadou", "information-2-line", "uhemoji2-icon", "-happy-"), iconMode: "animal-font", path: "/pages-blog/about-project/about-project", visible: true, group: "other"},
  {key: "disclaimer", title: "免责声明", subTitle: "博客内容免责声明", color: "#795548", iconColor: "#795548", bgColor: "#79554824", iconPrefix: "uhanimal-icon", icon: "helanzhu", icons: navIcons("helanzhu", "telegram-2-line", "uhemoji2-icon", "-smirking"), iconMode: "animal-font", path: "/pages-blog/disclaimer/disclaimer", visible: true, group: "other"},
  {key: "user-agreement", title: "用户协议", subTitle: "站点用户协议", color: "#8D6E63", iconColor: "#8D6E63", bgColor: "#8D6E6324", iconPrefix: "uhanimal-icon", icon: "bianmu", icons: navIcons("bianmu", "contract-line", "uhemoji2-icon", "-tongue"), iconMode: "animal-font", path: "/pages-blog/user-agreement/user-agreement", visible: true, group: "other"},
  {key: "privacy-policy", title: "隐私政策", subTitle: "站点隐私政策", color: "#A1887F", iconColor: "#A1887F", bgColor: "#A1887F24", iconPrefix: "uhanimal-icon", icon: "heimao", icons: navIcons("heimao", "spy-line", "uhemoji2-icon", "-secret"), iconMode: "animal-font", path: "/pages-blog/privacy-policy/privacy-policy", visible: true, group: "other"},
];

/**
 * 首页快捷导航默认 5 项
 */
export const DEFAULT_QUICK_NAV_KEYS = ["love", "contact-blogger", "favorites", "friend-links", "aboutProject"];

/** 我的页面-常用功能默认 11 项（足迹居首） */
export const DEFAULT_MY_PAGE_COMMON_KEYS = [
  "footprint", "contact-blogger", "notice", "favorites", "love", "friend-links", "archives", "vote", "data-visual", "portfolio", "douban",
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
