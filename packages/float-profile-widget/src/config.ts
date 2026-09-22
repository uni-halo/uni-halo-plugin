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

/**
 * 旧版（uh-fmp-* 时代）存储键一次性迁移：新键为空且旧键存在时搬移后删除旧键。
 * 悬浮卡片关闭记忆走 sessionStorage、申请草稿走 localStorage，两个存储各自迁移。
 */
function migrateLegacyKeys(newKey: string, legacyKey: string, storage: Storage): void {
  try {
    if (storage.getItem(newKey) === null) {
      const legacy = storage.getItem(legacyKey);
      if (legacy !== null) {
        storage.setItem(newKey, legacy);
        storage.removeItem(legacyKey);
      }
    } else {
      storage.removeItem(legacyKey);
    }
  } catch {
    // 存储不可用时静默跳过，不影响挂载
  }
}

migrateLegacyKeys(STORAGE_KEY, "uh-fmp-closed", sessionStorage);
migrateLegacyKeys("uh-fpw-apply-draft", "uh-fmp-apply-draft", localStorage);

// 公开接口（api.unihalo.ialley.cn 分组，匿名可访问；app 端同源接口）
// 端点统一注册在组根路径（无 plugins/<插件名> 前缀段），对齐 Halo 角色模板规范
export const API_BASE = "/apis/api.unihalo.ialley.cn/v1alpha1";
export const CAPTCHA_URL = API_BASE + "/captcha/generate";
export const LINK_LIST_URL = API_BASE + "/mini-program-links";
export const LINK_SUBMIT_URL = API_BASE + "/mini-program-links/-/submissions";
export const CONFIGS_URL = API_BASE + "/getConfigs";
