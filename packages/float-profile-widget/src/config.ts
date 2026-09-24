/**
 * 配置读取与全局常量。
 *
 * 配置由 FloatingWindowHeadProcessor 在页面 <head> 内联注入
 * window.__UNI_HALO_FLOAT_PROFILE_WIDGET__；未注入时 CONFIG 为 undefined，
 * 由 index.ts 判定不挂载（fail closed）。
 */
import type { FloatProfileWidgetConfig } from "./types";

export const CONFIG: FloatProfileWidgetConfig | undefined =
  window.__UNI_HALO_FLOAT_PROFILE_WIDGET__;

export const STORAGE_KEY = "uh-fpw-closed";
export const EDGE_TRIGGER = 80; // 距视口边缘小于该值视为贴边

/** 悬浮窗访客状态（sessionStorage，会话级跨页记忆；新会话复位站长默认） */
export interface WidgetPersistedState {
  minimized: boolean;
  edge: "left" | "right" | "top" | "bottom" | null;
  /** 卡片自由位置（拖拽/恢复后），无则用锚点默认定位 */
  cardPos?: { left: number; top: number };
  /** 最小化小球位置 */
  dotPos?: { left: number; top: number };
  /** 配置指纹：站长改动布局类配置后旧位置作废 */
  fp?: string;
}

export const STATE_KEY = "uh-fpw-state";

/** 影响默认布局的配置组合指纹 */
export function configFingerprint(): string {
  if (!CONFIG) {
    return "";
  }
  return [CONFIG.position, CONFIG.offsetX, CONFIG.offsetY, CONFIG.cardWidth].join("|");
}

/** 读取访客状态；配置指纹不匹配（站长改过布局配置）时作废清空 */
export function loadWidgetState(): WidgetPersistedState | null {
  try {
    const raw = sessionStorage.getItem(STATE_KEY);
    if (!raw) {
      return null;
    }
    const state = JSON.parse(raw) as WidgetPersistedState;
    if (state.fp !== configFingerprint()) {
      sessionStorage.removeItem(STATE_KEY);
      return null;
    }
    return state;
  } catch {
    return null;
  }
}

/** 写入访客状态（自动附带配置指纹） */
export function saveWidgetState(state: WidgetPersistedState): void {
  try {
    state.fp = configFingerprint();
    sessionStorage.setItem(STATE_KEY, JSON.stringify(state));
  } catch {
    // sessionStorage 不可用时静默跳过
  }
}

// 公开接口（api.unihalo.ialley.cn 分组，匿名可访问；app 端同源接口）
// 端点统一注册在组根路径（无 plugins/<插件名> 前缀段），对齐 Halo 角色模板规范
export const API_BASE = "/apis/api.unihalo.ialley.cn/v1alpha1";
export const CAPTCHA_URL = API_BASE + "/captcha/generate";
export const LINK_LIST_URL = API_BASE + "/mini-program-links";
export const LINK_SUBMIT_URL = API_BASE + "/mini-program-links/-/submissions";
export const CONFIGS_URL = API_BASE + "/getConfigs";
