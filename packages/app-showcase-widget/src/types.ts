/**
 * 应用展示面板类型定义。
 */

/** 面板配置（ThemeWidgetHeadProcessor 内联注入 window.__UNI_HALO_APP_SHOWCASE_WIDGET__） */
export interface AppShowcaseWidgetConfig {
  /** 插件版本号（注入时取自 PluginWrapper，控制台标识日志用） */
  version: string;
  enabled: boolean;
  entryIcon: string;
  pageScope: "all" | "only" | "except";
  pagePatterns: string;
  position: string;
  offsetX: number;
  offsetY: number;
  /** 面板与小球的层叠层级（详情/申请弹窗始终置顶，不受此项影响） */
  zIndex: number;
  panelWidth: number;
  defaultState: "default" | "minimized";
  dragEnabled: boolean;
  closeEnabled: boolean;
  rememberClosed: boolean;
  applyEntryEnabled: boolean;
  miniProgramItems: MiniProgramItem[];
  appItems: AppItem[];
}

/** 小程序条目（setting.yaml miniProgramItems 数组元素） */
export interface MiniProgramItem {
  displayName?: string;
  group?: string;
  icon?: string;
  codeImage?: string;
  appId?: string;
  path?: string;
  description?: string;
  priority?: number;
}

/** App 条目（setting.yaml appItems 数组元素） */
export interface AppItem {
  displayName?: string;
  group?: string;
  icon?: string;
  codeImage?: string;
  link?: string;
  description?: string;
  priority?: number;
}

/** 面板列表统一条目（两数组合并后按来源打类型标） */
export interface ShowcaseEntry {
  type: "miniprogram" | "app";
  typeLabel: string;
  displayName: string;
  group: string;
  icon: string;
  codeImage: string;
  appId: string;
  path: string;
  link: string;
  description: string;
  priority: number;
}

/** 验证码接口响应：GET .../captcha/generate → { id, imageBase64 } */
export interface CaptchaResponse {
  id: string;
  imageBase64: string;
}

/** 小程序信息（getConfigs → featureConfig.linkInfo.miniInfo，申请信息同源） */
export interface MiniInfo {
  displayName?: string;
  miniProgramCode?: string;
  appId?: string;
  path?: string;
  link?: string;
  description?: string;
  applyRemark?: string;
}

/** 博主信息（getConfigs → featureConfig.profile.blogger） */
export interface BloggerInfo {
  nickname?: string;
  avatar?: string;
  website?: string;
  description?: string;
}

declare global {
  interface Window {
    __UNI_HALO_APP_SHOWCASE_WIDGET__?: AppShowcaseWidgetConfig;
  }
}
