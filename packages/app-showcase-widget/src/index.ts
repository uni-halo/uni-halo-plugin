/**
 * UniHalo 主题应用展示面板入口。
 *
 * 读取 window.__UNI_HALO_APP_SHOWCASE_WIDGET__ 配置后挂载 <uh-app-showcase-widget>
 * 自定义元素（Lit + shadow DOM，样式完全隔离）；未注入配置时不挂载
 * （fail closed，页面零残留）。本工程由 vite 构建压缩（packages/app-showcase-widget）。
 */
// 副作用导入：确保组件模块执行（customElements.define 注册自定义元素），
// 避免仅类型引用被 tree-shake 摇掉
import "./app-showcase-widget";
import type { AppShowcaseWidgetElement } from "./app-showcase-widget";
import { CONFIG } from "./config";

function mount(): void {
  // 防重复注入
  if (document.querySelector("uh-app-showcase-widget")) {
    return;
  }
  printBanner();
  const el = document.createElement("uh-app-showcase-widget") as AppShowcaseWidgetElement;
  document.body.appendChild(el);
}

/** 控制台标识日志：徽标 + 插件简介 + 边框包裹的三条链接（官网/文档/作者），上下空行与其他输出区隔 */
function printBanner(): void {
  const version = CONFIG?.version || "";
  const theme = "#c6f921";
  const dark = "#1f2a05";
  const accent = "color:#92cf57;";
  const chip = `background:${theme};color:${dark};font-weight:600;border-radius:4px;`;
  console.log("");
  console.log(
    `%c UniHalo %c v${version} `,
    chip + "padding:2px 8px;border-radius:4px 0 0 4px;",
    "background:#f4fbe3;color:#55700a;padding:2px 8px;border-radius:0 4px 4px 0;",
  );
  console.log(
    "%c作者：小莫唐尼丨Apache-2.0 License", "color:#8A6F38;font-size:11px;",
  );
  console.log(
    `%c基于 UniApp 与 Halo 构建的跨平台客户端 + 配置插件，
支持 40+ 功能，优雅、轻量、跨平台，让你的内容触达每一个角落。`,
    "color:#92cf57;",
  );
  const pad = (url: string): string => url + " ".repeat(31 - url.length);
  const link = (label: string, url: string): void => {
    console.log(`%c│%c ${label} %c ${pad(url)}%c  │`, accent, chip, "", accent);
  };
  console.log(`%c┌${"─".repeat(20)}┐`, accent);
  link("官网", "https://uni-halo.ialley.cn");
  link("文档", "https://uni-halo-doc.ialley.cn");
  link("作者", "https://www.xiaoxiaomo.cn");
  console.log(`%c└${"─".repeat(20)}┘`, accent);
  console.log("");
}

// 未注入配置时不挂载（fail closed）
if (CONFIG && typeof CONFIG === "object") {
  if (document.body) {
    mount();
  } else {
    document.addEventListener("DOMContentLoaded", mount);
  }
}
