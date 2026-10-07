/**
 * UniHalo 主题应用展示面板 Lit 组件。
 *
 * 使用 shadow DOM 自定义元素：样式完全内聚（外部主题样式无法穿透，组件
 * 样式也不会泄漏）。两种形态：展开面板 ↔ 最小化小球；展开面板按顶部
 * 标题栏拖拽，小球无条件可拖。「提交申请 / 友链信息 / 条目详情」为独立
 * 居中弹窗，面板保持原位。
 *
 * 数据源：
 *  - 条目数据：window.__UNI_HALO_APP_SHOWCASE_WIDGET__ 注入的
 *    miniProgramItems / appItems 数组（setting.yaml 管理），按来源数组
 *    打类型标（小程序 / App）。
 *  - 申请提交：POST .../mini-program-links/submissions（验证码 query 参数，403 自动刷新）
 *  - 友链信息：GET .../getConfigs → featureConfig.linkInfo.miniInfo + featureConfig.profile.blogger
 */
import { LitElement, html } from "lit";
import QRCode from "qrcode";
import {
  CAPTCHA_URL,
  CONFIG,
  CONFIGS_URL,
  LINK_SUBMIT_URL,
  STATE_KEY,
  STORAGE_KEY,
  loadWidgetState,
  saveWidgetState,
} from "./config";
import { matchPage } from "./page-match";
import { styles } from "./styles";
import type {
  AppItem,
  AppShowcaseWidgetConfig,
  BloggerInfo,
  CaptchaResponse,
  MiniInfo,
  MiniProgramItem,
  OtherItem,
  ShowcaseEntry,
} from "./types";
import type { WidgetPersistedState } from "./config";

interface ApplyField {
  key: string;
  label: string;
  required: boolean;
  type?: "text" | "textarea";
  placeholder?: string;
}

/** 友链信息弹窗展示行（长文本用 textarea；copyable=false 不渲染复制按钮） */
interface LinkInfoRow {
  label: string;
  value?: string;
  textarea?: boolean;
  copyable?: boolean;
}

/** 面板拖拽中间态 */
interface DragState {
  startX: number;
  startY: number;
  left: number;
  top: number;
}

/** 小球拖拽中间态 */
interface MiniDotDrag {
  startX: number;
  startY: number;
  left: number;
  top: number;
  moved: boolean;
}

/** 申请表单-基础信息字段（对齐管理端「链接管理-申请审核」新增申请表单） */
const BASIC_FIELDS: ApplyField[] = [
  { key: "displayName", label: "应用名称", required: true },
  { key: "miniProgramCode", label: "太阳码（图片地址）", required: true },
  { key: "appId", label: "小程序 AppID", required: true, placeholder: "小程序 AppID，如 wx1234567890abcdef" },
  { key: "path", label: "跳转页面路径", required: false, placeholder: "如 pages/index/index" },
  { key: "link", label: "应用地址", required: false, placeholder: "#小程序://xxx" },
  { key: "description", label: "应用描述", required: false, type: "textarea" },
  { key: "applyRemark", label: "申请说明", required: false, type: "textarea" },
];

/** 申请表单-作者信息字段 */
const AUTHOR_FIELDS: ApplyField[] = [
  { key: "avatar", label: "作者头像（图片地址）", required: false },
  { key: "authorName", label: "作者昵称", required: false },
  { key: "website", label: "作者网站", required: false },
  { key: "email", label: "邮箱（选填，用于审核结果通知）", required: false },
];

/** 申请表单草稿缓存 key：输入自动保存，提交成功后清空（验证码不缓存） */
const DRAFT_KEY = "uh-asw-apply-draft";

/** 条目二维码缓存（app 无码图时按链接生成） */
const qrCache = new Map<string, string>();

/**
 * 图片地址规范化：http(s):// 或 // 协议相对或 data: 原样返回；
 * 其余相对/裸路径拼接当前站点域名（Halo 控制台附件/插件静态资源）。
 */
function normalizeImageUrl(url?: string): string {
  if (!url) {
    return "";
  }
  if (/^(https?:)?\/\//.test(url) || /^data:/i.test(url)) {
    return url;
  }
  return window.location.origin + (url.startsWith("/") ? url : "/" + url);
}

export class AppShowcaseWidgetElement extends LitElement {
  static styles = [styles];

  static properties = {
    tab: { state: true },
    minimized: { state: true },
    panelStyle: { state: true },
    dotStyle: { state: true },
    detail: { state: true },
    detailQr: { state: true },
    applyOpen: { state: true },
    applySubmitting: { state: true },
    applyTab: { state: true },
    captchaId: { state: true },
    captchaSrc: { state: true },
    screenshotRows: { state: true },
    linksOpen: { state: true },
    linksLoading: { state: true },
    linksError: { state: true },
    miniInfo: { state: true },
    blogger: { state: true },
  };

  declare tab: "all" | "miniprogram" | "app" | "other";
  declare minimized: boolean;
  declare panelStyle: string;
  declare dotStyle: string;
  declare detail: ShowcaseEntry | null;
  declare detailQr: string;
  declare applyOpen: boolean;
  declare applySubmitting: boolean;
  declare applyTab: "basic" | "author";
  declare captchaId: string;
  declare captchaSrc: string;
  declare screenshotRows: string[];
  declare linksOpen: boolean;
  declare linksLoading: boolean;
  declare linksError: boolean;
  declare miniInfo: MiniInfo | null;
  declare blogger: BloggerInfo | null;

  private config: AppShowcaseWidgetConfig;
  private panelDrag: DragState | null = null;
  private miniDotDrag: MiniDotDrag | null = null;
  private miniDotDragged = false;

  constructor() {
    super();
    // 入口（index.ts）已校验 CONFIG 非空
    this.config = CONFIG as AppShowcaseWidgetConfig;
    this.tab = "all";
    this.minimized = false;
    this.panelStyle = "";
    this.dotStyle = "";
    this.detail = null;
    this.detailQr = "";
    this.applyOpen = false;
    this.applySubmitting = false;
    this.applyTab = "basic";
    this.captchaId = "";
    this.captchaSrc = "";
    this.screenshotRows = [""];
    this.linksOpen = false;
    this.linksLoading = false;
    this.linksError = false;
    this.miniInfo = null;
    this.blogger = null;
  }

  connectedCallback(): void {
    super.connectedCallback();
    // 页面范围不匹配或访客已关闭 → 零残留移除
    if (!matchPage() || this.isClosed()) {
      this.remove();
    }
  }

  protected firstUpdated(): void {
    this.applyPosition();
    // 默认状态依赖 getBoundingClientRect 的最终布局位置：
    // 延迟到下一帧再取，避免自定义元素刚 upgrade 时布局未稳定导致小球位置错（左上角）
    requestAnimationFrame(() => this.applyDefaultState());
  }

  /** 默认状态：访客已存状态（跨页记忆）> 站长 defaultState > 无 */
  private applyDefaultState(): void {
    const panel = this.panelEl;
    if (!panel) {
      return;
    }
    const saved = loadWidgetState();
    if (saved) {
      // 先恢复面板自由位置（覆盖锚点定位），最小化恢复基于该位置计算
      if (saved.panelPos) {
        this.setFree(saved.panelPos.left, saved.panelPos.top);
      }
      if (saved.minimized) {
        // 无 dotPos 时留空走锚点 CSS 定位：页面初始布局未完成时测量恒为 0，不可靠
        if (saved.dotPos) {
          this.dotStyle =
            `left:${Math.round(saved.dotPos.left)}px;top:${Math.round(saved.dotPos.top)}px;`;
        }
        this.minimized = true;
        return;
      }
      return; // 展开态：位置已在上面恢复
    }
    if (this.config.defaultState === "minimized") {
      // 不测量：主题常在初始化期间隐藏 body，此时卡片无布局盒子，测量恒为 0；
      // 小球直接用与面板同款锚点 CSS 定位
      this.minimized = true;
    }
  }

  /** 会话级持久化访客状态：读旧值打补丁后写回（附配置指纹） */
  private persistState(patch: Partial<WidgetPersistedState>): void {
    const current = loadWidgetState() ?? { minimized: false };
    saveWidgetState({ ...current, ...patch });
  }

  private isClosed(): boolean {
    if (this.config.rememberClosed === false) {
      return false;
    }
    try {
      // 会话级记忆：同一标签页会话内不再显示，新会话默认重新显示
      return sessionStorage.getItem(STORAGE_KEY) === "1";
    } catch {
      return false;
    }
  }

  private get panelEl(): HTMLElement | null {
    return this.renderRoot.querySelector(".uh-asw");
  }

  private get miniDotEl(): HTMLElement | null {
    return this.renderRoot.querySelector(".uh-asw-mini-dot");
  }

  // ===== 定位：9 向锚点 + 偏移 =====
  private applyPosition(): void {
    const panel = this.panelEl;
    if (!panel) {
      return;
    }
    // 锚点 class 由模板 class 表达式管理（classList.add 会在重渲染时被 Lit 覆盖抹除）
    panel.style.width = (Number(this.config.panelWidth) || 340) + "px";
    panel.style.zIndex = String(this.config.zIndex || 9999);
    panel.style.transform = this.anchorTransform(this.config.position || "bottom-right");
  }

  /** 锚点偏移 transform：居中类锚点按 -50% 基准，其余直接平移 */
  private anchorTransform(pos: string): string {
    const x = Number(this.config.offsetX) || 0;
    const y = Number(this.config.offsetY) || 0;
    const tx =
      pos === "center" || pos === "top-center" || pos === "bottom-center"
        ? "calc(-50% + " + x + "px)"
        : x + "px";
    const ty =
      pos === "center" || pos === "left-center" || pos === "right-center"
        ? "calc(-50% + " + y + "px)"
        : y + "px";
    return "translate(" + tx + ", " + ty + ")";
  }

  private setFree(left: number, top: number): void {
    const panel = this.panelEl;
    if (!panel) {
      return;
    }
    panel.style.left = left + "px";
    panel.style.top = top + "px";
    panel.style.right = "auto";
    panel.style.bottom = "auto";
    panel.style.transform = "translate(0, 0)";
  }

  private clampToViewport(left: number, top: number, el: HTMLElement): [number, number] {
    const left2 = Math.max(0, Math.min(left, window.innerWidth - el.offsetWidth));
    const top2 = Math.max(0, Math.min(top, window.innerHeight - el.offsetHeight));
    return [left2, top2];
  }

  // ===== 面板拖拽（按住顶部标题栏，dragEnabled 控制） =====
  private onHeaderPointerDown(e: PointerEvent): void {
    if (!this.config.dragEnabled) {
      return; // 后台未开启拖拽：不响应拖拽
    }
    const target = e.target as HTMLElement | null;
    // 操作按钮与页签按钮不触发拖拽：避免 setPointerCapture 干扰 click 合成事件
    if (target && target.closest("button")) {
      return;
    }
    const panel = this.panelEl;
    if (!panel) {
      return;
    }
    const rect = panel.getBoundingClientRect();
    this.panelDrag = {
      startX: e.clientX,
      startY: e.clientY,
      left: rect.left,
      top: rect.top,
    };
    panel.classList.add("uh-asw-dragging");
    (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
    e.preventDefault();
  }

  private onHeaderPointerMove(e: PointerEvent): void {
    const drag = this.panelDrag;
    const panel = this.panelEl;
    if (!drag || !panel) {
      return;
    }
    const [left, top] = this.clampToViewport(
      drag.left + (e.clientX - drag.startX),
      drag.top + (e.clientY - drag.startY),
      panel,
    );
    this.setFree(left, top);
  }

  private onHeaderPointerEnd(): void {
    const panel = this.panelEl;
    if (!this.panelDrag || !panel) {
      return;
    }
    this.panelDrag = null;
    panel.classList.remove("uh-asw-dragging");
    // 拖拽结束时的视口位置（此时 transform 已被 setFree 归零，rect 即 left/top）
    const rect = panel.getBoundingClientRect();
    // 记住拖拽后的自由位置，跨页保持
    this.persistState({
      minimized: false,
      panelPos: { left: Math.round(rect.left), top: Math.round(rect.top) },
    });
  }

  // ===== 最小化 =====
  private onMinimizeClick(): void {
    const panel = this.panelEl;
    if (!panel) {
      return;
    }
    // 记录面板当前视口位置：小球作为独立 fixed 元素定位到该处（不影响面板样式）
    const rect = panel.getBoundingClientRect();
    this.dotStyle = `left:${Math.round(rect.left)}px;top:${Math.round(rect.top)}px;`;
    this.minimized = true;
    // 记住最小化态与小球位置（小球即面板当前视口位置），跨页恢复
    this.persistState({
      minimized: true,
      dotPos: { left: Math.round(rect.left), top: Math.round(rect.top) },
      panelPos: { left: Math.round(rect.left), top: Math.round(rect.top) },
    });
  }

  private onRestoreClick(): void {
    if (this.miniDotDragged) {
      this.miniDotDragged = false;
      return; // 刚拖拽过，忽略本次 click（防拖拽松手误触发恢复）
    }
    if (!this.dotStyle) {
      // 小球处于锚点定位（默认小球态）：面板直接展开，保持锚点位置
      this.minimized = false;
      this.persistState({ minimized: false });
      return;
    }
    // 恢复：把面板移动到小球当前位置（小球可被拖到任意位置，面板应跟随出现）
    const dot = this.miniDotEl;
    const panel = this.panelEl;
    if (dot && panel) {
      const rect = dot.getBoundingClientRect();
      panel.style.left = Math.round(rect.left) + "px";
      panel.style.top = Math.round(rect.top) + "px";
      panel.style.right = "auto";
      panel.style.bottom = "auto";
      panel.style.transform = ""; // 清除锚点 transform（center 等 -50% 偏移），定位精确
    }
    this.minimized = false;
    // 面板显示后按实际尺寸约束到视口内（小球贴近屏幕底部/右侧时，面板不超出屏幕）
    this.updateComplete.then(() => {
      const shown = this.panelEl;
      if (!shown) {
        return;
      }
      const left = Number.parseInt(shown.style.left, 10) || 0;
      const top = Number.parseInt(shown.style.top, 10) || 0;
      const [l, t] = this.clampToViewport(left, top, shown);
      if (l !== left || t !== top) {
        this.setFree(l, t);
      }
      // 记住展开态与面板最终位置（含视口约束修正），跨页恢复
      this.persistState({
        minimized: false,
        panelPos: { left: l, top: t },
      });
    });
  }

  // ===== 最小化小球拖拽（独立 fixed 元素，不受 dragEnabled 约束，无条件可拖） =====
  private onMiniDotPointerDown(e: PointerEvent): void {
    const dot = e.currentTarget as HTMLElement;
    const rect = dot.getBoundingClientRect();
    this.miniDotDrag = {
      startX: e.clientX,
      startY: e.clientY,
      left: rect.left,
      top: rect.top,
      moved: false,
    };
    dot.setPointerCapture(e.pointerId);
    // 不 preventDefault：未移动时保留 click 恢复；移动后由 miniDotDragged 拦截恢复
  }

  private onMiniDotPointerMove(e: PointerEvent): void {
    const drag = this.miniDotDrag;
    if (!drag) {
      return;
    }
    const dx = e.clientX - drag.startX;
    const dy = e.clientY - drag.startY;
    if (!drag.moved && (Math.abs(dx) > 3 || Math.abs(dy) > 3)) {
      drag.moved = true; // 超过阈值视为拖拽
    }
    if (!drag.moved) {
      return;
    }
    const dot = e.currentTarget as HTMLElement;
    const [left, top] = this.clampToViewport(
      drag.left + dx,
      drag.top + dy,
      dot,
    );
    // 直接写内联样式，避免 state 重渲染干扰指针捕获
    dot.style.left = left + "px";
    dot.style.top = top + "px";
  }

  private onMiniDotPointerEnd(): void {
    const drag = this.miniDotDrag;
    if (drag?.moved) {
      this.miniDotDragged = true; // click 处理器据此忽略本次恢复
      // 小球拖拽落点写入状态，跨页保持在拖后的位置
      const dot = this.miniDotEl;
      if (dot && dot.style.left && dot.style.top) {
        this.persistState({
          dotPos: { left: Number.parseFloat(dot.style.left), top: Number.parseFloat(dot.style.top) },
        });
      }
    }
    this.miniDotDrag = null;
  }

  // ===== 关闭 =====
  private onCloseClick(): void {
    if (this.config.rememberClosed !== false) {
      try {
        // 会话级记忆：仅当前标签页会话内不再显示，新会话默认重新显示
        sessionStorage.setItem(STORAGE_KEY, "1");
      } catch {
        // sessionStorage 不可用时仅本次页面关闭
      }
    }
    // 关闭后清掉访客状态，避免下次会话残留最小化/位置
    try {
      sessionStorage.removeItem(STATE_KEY);
    } catch {
      // 忽略
    }
    const panel = this.panelEl;
    if (panel) {
      panel.classList.add("uh-asw-closing");
      setTimeout(() => this.remove(), 200);
    } else {
      this.remove();
    }
  }

  // ===== 条目数据合并（按来源数组打类型标，priority 越大越靠前） =====
  private get entries(): ShowcaseEntry[] {
    const c = this.config;
    const minis: ShowcaseEntry[] = (c.miniProgramItems || [])
      .filter((item: MiniProgramItem) => item.displayName?.trim())
      .map((item: MiniProgramItem) => ({
        type: "miniprogram" as const,
        typeLabel: "小程序",
        displayName: item.displayName?.trim() || "",
        group: item.group?.trim() || "",
        icon: normalizeImageUrl(item.icon),
        codeImage: normalizeImageUrl(item.codeImage),
        appId: item.appId?.trim() || "",
        path: item.path?.trim() || "",
        link: "",
        description: item.description?.trim() || "",
        priority: Number(item.priority) || 0,
      }));
    const apps: ShowcaseEntry[] = (c.appItems || [])
      .filter((item: AppItem) => item.displayName?.trim())
      .map((item: AppItem) => ({
        type: "app" as const,
        typeLabel: "App",
        displayName: item.displayName?.trim() || "",
        group: item.group?.trim() || "",
        icon: normalizeImageUrl(item.icon),
        codeImage: normalizeImageUrl(item.codeImage),
        appId: "",
        path: "",
        link: item.link?.trim() || "",
        description: item.description?.trim() || "",
        priority: Number(item.priority) || 0,
      }));
    const others: ShowcaseEntry[] = (c.otherItems || [])
      .filter((item: OtherItem) => item.displayName?.trim())
      .map((item: OtherItem) => ({
        type: "other" as const,
        typeLabel: item.typeName?.trim() || "其他",
        displayName: item.displayName?.trim() || "",
        group: item.group?.trim() || "",
        icon: normalizeImageUrl(item.icon),
        codeImage: normalizeImageUrl(item.codeImage),
        appId: "",
        path: "",
        link: item.link?.trim() || "",
        description: item.description?.trim() || "",
        priority: Number(item.priority) || 0,
      }));
    return [...minis, ...apps, ...others].sort((a, b) => b.priority - a.priority);
  }

  private get visibleEntries(): ShowcaseEntry[] {
    if (this.tab === "all") {
      return this.entries;
    }
    return this.entries.filter((entry) => entry.type === this.tab);
  }

  // ===== 条目详情弹窗 =====
  private async openDetail(entry: ShowcaseEntry): Promise<void> {
    this.detail = entry;
    this.detailQr = "";
    if ((entry.type === "app" || entry.type === "other") && !entry.codeImage && entry.link) {
      // App / 其他 无码图时按链接生成二维码（带缓存）
      const cached = qrCache.get(entry.link);
      if (cached) {
        this.detailQr = cached;
        return;
      }
      try {
        const dataUrl = await QRCode.toDataURL(entry.link, { margin: 1, width: 512 });
        qrCache.set(entry.link, dataUrl);
        // 请求期间可能已切换详情，仅当前条目仍匹配时回填
        if (this.detail === entry) {
          this.detailQr = dataUrl;
        }
      } catch {
        // 生成失败保持空白，由 tip 文案提示
      }
    }
  }

  private closeDetail(): void {
    this.detail = null;
    this.detailQr = "";
  }

  // ===== 弹窗通用 =====
  private onOverlayClick(e: Event): void {
    const target = e.target as HTMLElement;
    if (target.classList.contains("uh-asw-overlay")) {
      this.closeModals();
    }
  }

  private closeModals(): void {
    this.applyOpen = false;
    this.linksOpen = false;
    this.closeDetail();
  }

  // ===== 申请弹窗 =====
  private openApply(): void {
    this.applyOpen = true;
    this.refreshCaptcha();
    // 弹窗渲染完成后回填本地草稿（验证码不缓存）
    this.updateComplete.then(() => this.restoreDraft());
  }

  private restoreDraft(): void {
    const form = this.renderRoot.querySelector(".uh-asw-form") as HTMLFormElement | null;
    if (!form) {
      return;
    }
    const draft = this.loadDraft();
    [...BASIC_FIELDS, ...AUTHOR_FIELDS].forEach((field) => {
      const el = form.elements.namedItem(field.key) as HTMLInputElement | null;
      if (el && draft[field.key]) {
        el.value = String(draft[field.key]);
      }
    });
    // 预览图动态行回填（兼容旧草稿：字符串按行拆分）
    const shots = draft["screenshots"];
    if (Array.isArray(shots)) {
      this.screenshotRows = shots.length ? shots.slice() : [""];
    } else if (typeof shots === "string" && shots.trim()) {
      this.screenshotRows = shots
        .split("\n")
        .map((s) => s.trim())
        .filter(Boolean);
      if (!this.screenshotRows.length) {
        this.screenshotRows = [""];
      }
    }
  }

  private loadDraft(): Record<string, unknown> {
    try {
      const raw = localStorage.getItem(DRAFT_KEY);
      return raw ? (JSON.parse(raw) as Record<string, unknown>) : {};
    } catch {
      return {};
    }
  }

  /** 输入即自动保存草稿：刷新/关闭页面后重新打开仍显示已填内容 */
  private saveDraft(): void {
    const form = this.renderRoot.querySelector(".uh-asw-form") as HTMLFormElement | null;
    if (!form) {
      return;
    }
    const values: Record<string, unknown> = {};
    [...BASIC_FIELDS, ...AUTHOR_FIELDS].forEach((field) => {
      const el = form.elements.namedItem(field.key) as HTMLInputElement | null;
      values[field.key] = el?.value || "";
    });
    // 预览图动态行存数组草稿
    values["screenshots"] = this.screenshotRows.filter((s) => s.trim());
    try {
      localStorage.setItem(DRAFT_KEY, JSON.stringify(values));
    } catch {
      // localStorage 不可用时仅本次会话
    }
  }

  private clearDraft(): void {
    try {
      localStorage.removeItem(DRAFT_KEY);
    } catch {
      // 忽略
    }
  }

  /** 重置：清空表单与本地草稿 */
  private resetApply(): void {
    const form = this.renderRoot.querySelector(".uh-asw-form") as HTMLFormElement | null;
    if (form) {
      form.reset();
    }
    this.screenshotRows = [""]; // 预览图动态行重置为一行空
    this.applyTab = "basic"; // 回到基础信息面板
    this.clearDraft();
  }

  private onFormInput(): void {
    this.saveDraft();
  }

  private refreshCaptcha(): void {
    fetch(CAPTCHA_URL)
      .then((res) => res.json())
      .then((data: CaptchaResponse) => {
        if (data && data.imageBase64) {
          this.setCaptcha(data);
        }
      })
      .catch(() => {
        // 验证码加载失败静默处理
      });
  }

  private setCaptcha(captcha: CaptchaResponse): void {
    this.captchaId = captcha.id;
    // 后端 CaptchaVo.imageBase64 已是完整 data URI（data:image/png;base64,...），无需再拼前缀
    this.captchaSrc = captcha.imageBase64;
  }

  private async onApplySubmit(e: SubmitEvent): Promise<void> {
    e.preventDefault();
    const form = e.target as HTMLFormElement;
    // 手动校验必填字段（表单 novalidate：原生校验对隐藏面板的 required 不生效）
    const requiredChecks: Array<{ key: string; message: string; panel?: "basic" | "author" }> = [
      { key: "displayName", message: "请填写应用名称", panel: "basic" },
      { key: "miniProgramCode", message: "请填写太阳码图片地址", panel: "basic" },
      { key: "captchaCode", message: "请输入验证码" },
    ];
    for (const check of requiredChecks) {
      const el = form.elements.namedItem(check.key) as HTMLInputElement | null;
      if (!el || !el.value.trim()) {
        if (check.panel) {
          this.applyTab = check.panel;
        }
        alert(check.message);
        return;
      }
    }
    const spec: Record<string, unknown> = {};
    [...BASIC_FIELDS, ...AUTHOR_FIELDS].forEach((field) => {
      const el = form.elements.namedItem(field.key) as HTMLInputElement | null;
      if (el && el.value.trim()) {
        spec[field.key] = el.value.trim();
      }
    });
    // 预览图动态行 → 数组（对齐 MiniProgramLinkSubmissionSpec.screenshots；URL 规范化防控制台裂图）
    const shots = this.screenshotRows
      .map((s) => s.trim())
      .filter(Boolean)
      .map(normalizeImageUrl);
    if (shots.length) {
      spec["screenshots"] = shots;
    }
    const captchaCodeEl = form.elements.namedItem("captchaCode") as HTMLInputElement | null;
    const captchaCode = (captchaCodeEl?.value || "").trim();
    const query = "?captchaId=" + encodeURIComponent(this.captchaId || "")
      + "&captchaCode=" + encodeURIComponent(captchaCode);

    this.applySubmitting = true;
    try {
      const res = await fetch(LINK_SUBMIT_URL + query, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ spec }),
      });
      const data = (await res.json().catch(() => ({}))) as {
        message?: string;
        captcha?: CaptchaResponse;
      };
      if (res.status === 200 || res.status === 201) {
        this.applyOpen = false;
        this.clearDraft(); // 提交成功后清空本地草稿
        form.reset(); // 清空表单字段（含验证码，下次打开为空）
        this.screenshotRows = [""]; // 预览图动态行重置
        this.applyTab = "basic"; // 回到基础信息面板
        alert("申请提交成功，请等待审核");
        return;
      }
      // 403：验证码错误/过期，附新验证码即时刷新
      if (res.status === 403 && data.captcha) {
        this.setCaptcha(data.captcha);
      }
      alert(data.message || "提交失败，请重试");
    } catch {
      alert("网络异常，请稍后重试");
    } finally {
      this.applySubmitting = false;
    }
  }

  // ===== 友链信息弹窗（小程序信息 + 博主信息） =====
  private openLinks(): void {
    this.linksOpen = true;
    this.linksLoading = true;
    this.linksError = false;
    this.miniInfo = null;
    this.blogger = null;
    fetch(CONFIGS_URL)
      .then((res) => res.json())
      .then((data: Record<string, unknown>) => {
        // getConfigs 契约 v2：功能设置单例 spec 在顶层 featureConfig 键下
        const featureConfig = data?.featureConfig as
          | { linkInfo?: { miniInfo?: MiniInfo }; profile?: { blogger?: BloggerInfo } }
          | undefined;
        this.miniInfo = featureConfig?.linkInfo?.miniInfo || null;
        this.blogger = featureConfig?.profile?.blogger || null;
      })
      .catch(() => {
        this.linksError = true;
      })
      .finally(() => {
        this.linksLoading = false;
      });
  }

  // ===== 渲染 =====
  override render() {
    const c = this.config;
    const entryIcon = normalizeImageUrl(c.entryIcon);
    // 小球定位：有像素位置（手动最小化/拖拽记忆）用像素；否则与面板同款锚点 CSS 定位；
    // 层级与面板同源（内联 z-index 覆盖 CSS 默认值）
    const zIndexStyle = "z-index:" + (Number(c.zIndex) || 9999) + ";";
    const dotCls = this.dotStyle ? "uh-asw-mini-dot" : `uh-asw-mini-dot uh-asw-pos-${c.position || "bottom-right"}`;
    const dotStyleVal = (this.dotStyle || this.anchorTransform(c.position || "bottom-right")) + zIndexStyle;
    return html`
      ${this.applyOpen ? this.renderApplyModal() : ""}
      ${this.linksOpen ? this.renderLinksModal() : ""}
      ${this.detail ? this.renderDetailModal(this.detail) : ""}
      <div
        class="uh-asw uh-asw-pos-${c.position || "bottom-right"} ${c.dragEnabled ? "uh-asw-draggable" : ""} ${this.minimized ? "uh-asw-minimized" : ""}"
        style=${this.panelStyle}
      >
        <div
          class="uh-asw-header"
          @pointerdown=${this.onHeaderPointerDown}
          @pointermove=${this.onHeaderPointerMove}
          @pointerup=${this.onHeaderPointerEnd}
          @pointercancel=${this.onHeaderPointerEnd}
        >
          ${entryIcon
        ? html`<span class="uh-asw-header-icon"><img src=${entryIcon} alt="" /></span>`
        : ""}
          <span class="uh-asw-title">应用展示</span>
          <span class="uh-asw-topbar">
            <button type="button" class="uh-asw-topbar-btn" aria-label="最小化" @click=${this.onMinimizeClick}>&minus;</button>
            ${c.closeEnabled !== false
        ? html`<button type="button" class="uh-asw-topbar-btn" aria-label="关闭悬浮面板" @click=${this.onCloseClick}>&times;</button>`
        : ""}
          </span>
        </div>
        <div class="uh-asw-body">
          <div class="uh-asw-segmented">
            <button
              type="button"
              class="uh-asw-seg-item ${this.tab === "all" ? "uh-asw-seg-active" : ""}"
              @click=${() => (this.tab = "all")}
            >全部</button>
            <button
              type="button"
              class="uh-asw-seg-item ${this.tab === "miniprogram" ? "uh-asw-seg-active" : ""}"
              @click=${() => (this.tab = "miniprogram")}
            >小程序</button>
            <button
              type="button"
              class="uh-asw-seg-item ${this.tab === "app" ? "uh-asw-seg-active" : ""}"
              @click=${() => (this.tab = "app")}
            >APP</button>
            <button
              type="button"
              class="uh-asw-seg-item ${this.tab === "other" ? "uh-asw-seg-active" : ""}"
              @click=${() => (this.tab = "other")}
            >其他</button>
          </div>
          <div class="uh-asw-content">
            ${this.renderEntries()}
          </div>
        </div>
        ${c.applyEntryEnabled
        ? html`
              <div class="uh-asw-actions">
                <button type="button" class="uh-asw-btn" @click=${this.openApply}>我要申请</button>
                <button type="button" class="uh-asw-btn" @click=${this.openLinks}>友链信息</button>
                <div class="uh-asw-hint">小程序申请和友链信息</div>
              </div>`
        : ""}
      </div>
      ${this.minimized
        ? html`
            <button
              type="button"
              class=${dotCls}
              style=${dotStyleVal}
              @click=${this.onRestoreClick}
              @pointerdown=${this.onMiniDotPointerDown}
              @pointermove=${this.onMiniDotPointerMove}
              @pointerup=${this.onMiniDotPointerEnd}
              @pointercancel=${this.onMiniDotPointerEnd}
              aria-label="恢复应用展示面板"
            >
              ${entryIcon ? html`<img src=${entryIcon} alt="" />` : ""}
              <span class="uh-asw-mini-plus">+</span>
            </button>`
        : ""}
    `;
  }

  /** 条目列表渲染：按分组聚合（组顺序 = 排序后首次出现顺序），组内保持 priority 排序 */
  private renderEntries() {
    const entries = this.visibleEntries;
    if (!entries.length) {
      return html`<div class="uh-asw-empty">暂无内容</div>`;
    }
    const groups: Array<{ name: string; items: ShowcaseEntry[] }> = [];
    entries.forEach((entry) => {
      const name = entry.group || "未分组";
      let group = groups.find((g) => g.name === name);
      if (!group) {
        group = { name, items: [] };
        groups.push(group);
      }
      group.items.push(entry);
    });
    return groups.map(
      (group) => html`
        <div class="uh-asw-group-name">${group.name}</div>
        ${group.items.map((entry) => this.renderEntry(entry))}
      `,
    );
  }

  private renderEntry(entry: ShowcaseEntry) {
    const initial = entry.displayName.slice(0, 1);
    const thumb = entry.codeImage || ((entry.type === "app" || entry.type === "other") && entry.link ? qrCache.get(entry.link) : "");
    return html`
      <div class="uh-asw-item" @click=${() => this.openDetail(entry)}>
        <div class="uh-asw-item-icon ${entry.type}">${entry.icon ? html`<img src=${entry.icon} alt="" />` : initial}</div>
        <div class="uh-asw-item-info">
          <div class="uh-asw-item-name"><span class="uh-asw-tag">${entry.typeLabel}</span>${entry.displayName}</div>
          <div class="uh-asw-item-desc">${entry.description || "该应用暂无描述"}</div>
        </div>
        ${thumb ? html`<div class="uh-asw-item-thumb"><img src=${thumb} alt="" /></div>` : ""}
      </div>
    `;
  }

  /** 条目详情弹窗：小程序展示太阳码，App / 其他展示码图或按链接生成的二维码；无链接的其他条目仅作展示 */
  private renderDetailModal(entry: ShowcaseEntry) {
    const isMini = entry.type === "miniprogram";
    const qrSrc = isMini ? entry.codeImage : entry.codeImage || this.detailQr;
    const initial = entry.displayName.slice(0, 1);
    return html`
      <div class="uh-asw-overlay" @click=${this.onOverlayClick}>
        <div class="uh-asw-modal">
          <div class="uh-asw-modal-header">
            <div class="uh-asw-modal-title">${isMini ? "小程序详情" : "应用详情"}<span class="uh-asw-tag">${entry.typeLabel}</span></div>
            <button type="button" class="uh-asw-modal-close" aria-label="关闭" @click=${this.closeDetail}>&times;</button>
          </div>
          <div class="uh-asw-modal-body">
            <div class="uh-asw-detail-head">
              <div class="uh-asw-item-icon ${entry.type}">${entry.icon ? html`<img src=${entry.icon} alt="" />` : initial}</div>
              <div>
                <div class="uh-asw-detail-name">${entry.displayName}</div>
                <div class="uh-asw-detail-desc">${entry.description || entry.typeLabel}</div>
              </div>
            </div>
            ${qrSrc
        ? html`<div class="uh-asw-qr-card"><img src=${qrSrc} alt=${isMini ? "小程序太阳码" : "二维码"} /></div>`
        : entry.link
          ? html`<div class="uh-asw-qr-card"></div>`
          : ""}
            <div class="uh-asw-qr-tip">
              ${isMini
        ? "微信扫码打开小程序"
        : entry.link
          ? "手机扫码直接打开链接"
          : "该条目未配置链接，仅作展示"}
            </div>
            ${!isMini && entry.link
        ? html`
                  <div class="uh-asw-link-row">
                    <input class="uh-asw-copy-input" type="text" readonly value=${entry.link} @click=${(e: Event) => (e.target as HTMLInputElement).select()} />
                    <button type="button" class="uh-asw-btn" @click=${(e: Event) => this.copyText(entry.link, e.target as HTMLButtonElement)}>复制</button>
                  </div>
                  <div class="uh-asw-detail-actions">
                    <button type="button" class="uh-asw-btn uh-asw-btn-primary" @click=${() => window.open(entry.link, "_blank", "noopener")}>立即打开</button>
                    <button type="button" class="uh-asw-btn" @click=${this.closeDetail}>关闭</button>
                  </div>`
        : html`
                  <div class="uh-asw-detail-actions">
                    <button type="button" class="uh-asw-btn" @click=${this.closeDetail}>关闭</button>
                  </div>`}
          </div>
        </div>
      </div>
    `;
  }

  /** 申请表单字段渲染：text（默认）/ textarea */
  private renderApplyField(field: ApplyField) {
    const labelEl = html`<span>${field.label}${field.required ? " *" : ""}</span>`;
    if (field.type === "textarea") {
      return html`
        <label class="uh-asw-field">
          ${labelEl}
          <textarea name=${field.key} rows="2" placeholder=${field.placeholder ?? ""}></textarea>
        </label>`;
    }
    return html`
      <label class="uh-asw-field">
        ${labelEl}
        <input type="text" name=${field.key} ?required=${field.required} placeholder=${field.placeholder ?? ""} />
      </label>`;
  }

  private renderApplyModal() {
    return html`
      <div class="uh-asw-overlay" @click=${this.onOverlayClick}>
        <div class="uh-asw-modal">
          <div class="uh-asw-modal-header">
            <div class="uh-asw-modal-title">小程序申请</div>
            <button type="button" class="uh-asw-modal-close" aria-label="关闭" @click=${this.closeModals}>&times;</button>
          </div>
          <div class="uh-asw-segmented-modal">
            <button
              type="button"
              class="uh-asw-seg-item ${this.applyTab === "basic" ? "uh-asw-seg-active" : ""}"
              @click=${() => (this.applyTab = "basic")}
            >基础信息</button>
            <button
              type="button"
              class="uh-asw-seg-item ${this.applyTab === "author" ? "uh-asw-seg-active" : ""}"
              @click=${() => (this.applyTab = "author")}
            >作者信息</button>
          </div>
          <div class="uh-asw-modal-body uh-asw-apply-body">
            <!-- novalidate：隐藏面板的 required 不参与原生校验，由提交时手动校验 -->
            <form class="uh-asw-form" novalidate @submit=${this.onApplySubmit} @input=${this.onFormInput}>
              <div class="uh-asw-apply-panels">
                <div class="uh-asw-apply-panel" ?hidden=${this.applyTab !== "basic"}>
                  ${BASIC_FIELDS.map((field) => this.renderApplyField(field))}
                  ${this.renderScreenshotRows()}
                </div>
                <div class="uh-asw-apply-panel" ?hidden=${this.applyTab !== "author"}>
                  ${AUTHOR_FIELDS.map((field) => this.renderApplyField(field))}
                </div>
              </div>
              <div class="uh-asw-apply-footer">
                <label class="uh-asw-field uh-asw-captcha-row">
                  <span>验证码 *</span>
                  <span class="uh-asw-captcha-input">
                    <input type="text" name="captchaCode" required autocomplete="off" />
                    <img
                      class="uh-asw-captcha-img"
                      alt="验证码"
                      title="看不清？点击刷新"
                      src=${this.captchaSrc}
                      @click=${this.refreshCaptcha}
                    />
                  </span>
                </label>
                <div class="uh-asw-form-actions">
                  <button type="button" class="uh-asw-btn" @click=${this.resetApply}>重置</button>
                  <button type="button" class="uh-asw-btn" @click=${this.closeModals}>取消</button>
                  <button type="submit" class="uh-asw-btn uh-asw-btn-primary" ?disabled=${this.applySubmitting}>
                    ${this.applySubmitting ? "提交中…" : "提交申请"}
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      </div>
    `;
  }

  /** 预览图动态添加：URL 输入行 + 删除按钮 + 底部「添加一张」 */
  private renderScreenshotRows() {
    return html`
      <div class="uh-asw-field">
        <span>预览截图(可选)</span>
        ${this.screenshotRows.map(
      (url, index) => html`
            <div class="uh-asw-shot-row">
              <input
                class="uh-asw-shot-input"
                type="text"
                name="screenshots"
                placeholder="https://…/image.png"
                value=${url}
                @input=${(e: Event) =>
          this.updateScreenshotRow(index, (e.target as HTMLInputElement).value)}
              />
              <button
                type="button"
                class="uh-asw-shot-remove"
                aria-label="删除该预览图"
                @click=${() => this.removeScreenshotRow(index)}
              >&times;</button>
            </div>`,
    )}
        <button type="button" class="uh-asw-btn uh-asw-shot-add" @click=${this.addScreenshotRow}>
          + 添加一张预览图
        </button>
      </div>`;
  }

  private updateScreenshotRow(index: number, value: string): void {
    const rows = this.screenshotRows.slice();
    rows[index] = value;
    this.screenshotRows = rows;
  }

  private addScreenshotRow(): void {
    this.screenshotRows = [...this.screenshotRows, ""];
    // 新行渲染后自动滚动到底部（面板内滚动容器）
    this.updateComplete.then(() => {
      const rows = this.renderRoot.querySelectorAll(".uh-asw-shot-row");
      const last = rows[rows.length - 1];
      if (last) {
        last.scrollIntoView({ block: "nearest", behavior: "smooth" });
      }
    });
  }

  private removeScreenshotRow(index: number): void {
    const rows = this.screenshotRows.filter((_, i) => i !== index);
    // 至少保留一行空输入
    this.screenshotRows = rows.length ? rows : [""];
  }

  private renderLinksModal() {
    const miniRows: LinkInfoRow[] = [
      { label: "小程序名称", value: this.miniInfo?.displayName },
      { label: "太阳码地址", value: normalizeImageUrl(this.miniInfo?.miniProgramCode) },
      { label: "小程序 AppID", value: this.miniInfo?.appId },
      { label: "跳转页面路径", value: this.miniInfo?.path },
      { label: "小程序地址", value: this.miniInfo?.link },
      { label: "小程序描述", value: this.miniInfo?.description, textarea: true },
      { label: "申请说明", value: this.miniInfo?.applyRemark, textarea: true, copyable: false },
    ];
    const blogRows: LinkInfoRow[] = [
      { label: "博主昵称", value: this.blogger?.nickname },
      { label: "博主头像", value: normalizeImageUrl(this.blogger?.avatar) },
      { label: "博主主页", value: this.blogger?.website },
      { label: "博主简介", value: this.blogger?.description },
    ];
    const hasContent =
      miniRows.some((row) => row.value) || blogRows.some((row) => row.value);
    return html`
      <div class="uh-asw-overlay" @click=${this.onOverlayClick}>
        <div class="uh-asw-modal">
          <div class="uh-asw-modal-header">
            <div class="uh-asw-modal-title">小程序友链信息</div>
            <button type="button" class="uh-asw-modal-close" aria-label="关闭" @click=${this.closeModals}>&times;</button>
          </div>
          <div class="uh-asw-modal-body">
            ${this.linksLoading
        ? html`<div class="uh-asw-loading">加载中…</div>`
        : this.linksError
          ? html`<div class="uh-asw-loading">加载失败，请稍后重试</div>`
          : !hasContent
            ? html`<div class="uh-asw-loading">暂无友链信息</div>`
            : html`
                      <div class="uh-asw-info-card">
                        <div class="uh-asw-info-card-title">小程序信息</div>
                        ${miniRows.map((row) => this.renderCopyRow(row.label, row.value, row))}
                      </div>
                      <div class="uh-asw-info-card">
                        <div class="uh-asw-info-card-title">博主信息</div>
                        ${blogRows.map((row) => this.renderCopyRow(row.label, row.value, row))}
                      </div>
                      <button
                        type="button"
                        class="uh-asw-btn uh-asw-copy-all"
                        @click=${(e: Event) =>
                this.copyText(this.collectLinkText(), e.target as HTMLButtonElement)}
                      >复制全部</button>
                    `}
          </div>
        </div>
      </div>
    `;
  }

  /** 单条信息：只读输入框（长文本 textarea）+ 复制按钮（点击全选）；copyable=false 无复制按钮 */
  private renderCopyRow(label: string, value?: string, options?: Partial<LinkInfoRow>) {
    if (!value) {
      return "";
    }
    const textarea = !!options?.textarea;
    const copyable = options?.copyable ?? true;
    const input = textarea
      ? html`
          <textarea
            class="uh-asw-copy-input uh-asw-copy-textarea"
            readonly
            rows="2"
            @click=${(e: Event) => (e.target as HTMLTextAreaElement).select()}
          >${value}</textarea>`
      : html`
          <input
            class="uh-asw-copy-input"
            type="text"
            readonly
            value=${value}
            @click=${(e: Event) => (e.target as HTMLInputElement).select()}
          />`;
    return html`
      <div class="uh-asw-copy-row">
        <span class="uh-asw-copy-label">${label}</span>
        ${input}
        ${copyable
        ? html`
              <button
                type="button"
                class="uh-asw-btn uh-asw-copy-btn"
                @click=${(e: Event) => this.copyText(value, e.target as HTMLButtonElement)}
              >复制</button>`
        : ""}
      </div>`;
  }

  /** 复制到剪贴板（navigator.clipboard，失败回退 execCommand）；成功后按钮短暂显示「已复制」 */
  private async copyText(text: string, btn?: HTMLButtonElement): Promise<void> {
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      // 非安全上下文/权限拒绝时回退到 execCommand
      const ta = document.createElement("textarea");
      ta.value = text;
      ta.style.position = "fixed";
      ta.style.opacity = "0";
      document.body.appendChild(ta);
      ta.select();
      try {
        document.execCommand("copy");
      } catch {
        // 复制失败静默
      }
      ta.remove();
    }
    if (btn) {
      const original = btn.textContent;
      btn.textContent = "已复制";
      setTimeout(() => {
        if (btn.isConnected) {
          btn.textContent = original;
        }
      }, 800);
    }
  }

  /** 拼接全部信息文本（「复制全部」用，格式：标签：值，换行分隔） */
  private collectLinkText(): string {
    const parts: string[] = [];
    const push = (label: string, value?: string): void => {
      if (value) {
        parts.push(`${label}：${value}`);
      }
    };
    const mini = this.miniInfo;
    push("小程序名称", mini?.displayName);
    push("太阳码地址", normalizeImageUrl(mini?.miniProgramCode));
    push("小程序 AppID", mini?.appId);
    push("跳转页面路径", mini?.path);
    push("小程序地址", mini?.link);
    push("描述", mini?.description);
    push("申请说明", mini?.applyRemark);
    const blog = this.blogger;
    push("博主昵称", blog?.nickname);
    push("博主头像", normalizeImageUrl(blog?.avatar));
    push("博主主页", blog?.website);
    push("博主简介", blog?.description);
    return parts.join("\n");
  }
}

customElements.define("uh-app-showcase-widget", AppShowcaseWidgetElement);
