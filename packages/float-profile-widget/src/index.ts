/**
 * UniHalo 主题悬浮窗（小程序太阳码悬浮卡片）入口。
 *
 * 读取 window.__UNI_HALO_FLOAT_PROFILE_WIDGET__ 配置后挂载 <uh-float-profile-widget>
 * 自定义元素（Lit + shadow DOM，样式完全隔离）；未注入配置时不挂载
 * （fail closed，页面零残留）。本工程由 vite 构建压缩（packages/float-profile-widget）。
 */
// 副作用导入：确保组件模块执行（customElements.define 注册自定义元素），
// 避免仅类型引用被 tree-shake 摇掉
import "./float-profile-widget";
import type { FloatProfileWidgetElement } from "./float-profile-widget";
import { CONFIG } from "./config";

function mount(): void {
  // 防重复注入
  if (document.querySelector("uh-float-profile-widget")) {
    return;
  }
  const el = document.createElement("uh-float-profile-widget") as FloatProfileWidgetElement;
  document.body.appendChild(el);
}

// 未注入配置时不挂载（fail closed）
if (CONFIG && typeof CONFIG === "object") {
  if (document.body) {
    mount();
  } else {
    document.addEventListener("DOMContentLoaded", mount);
  }
}
