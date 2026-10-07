/**
 * 应用展示面板样式（Lit css 模板）。
 *
 * 组件使用 shadow DOM，样式完全内聚不泄漏、外部主题样式也无法穿透影响组件；
 * 弹窗（overlay/modal）渲染在 shadow DOM 内部，按钮等选择器直接用类名。
 * 视觉基调：白色卡片 + 白色细边框 + 柔和阴影 + 玻璃拟态弹窗。
 */
import { css } from "lit";

export const styles = css`
  :host {
    display: block;
  }

  /* ===== 展开面板 ===== */
  .uh-asw {
    position: fixed;
    z-index: 9999;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    width: 340px;
    max-width: calc(100vw - 24px);
    max-height: min(530px, calc(100vh - 48px));
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC",
      "Microsoft YaHei", sans-serif;
    background: rgba(255, 255, 255, 1);
    border: 2px solid rgba(255, 255, 255, 0.65);
    border-radius: 14px;
    box-shadow: 0 16px 60px rgba(0, 0, 0, 0.1);
    user-select: none;
    -webkit-user-select: none;
    line-height: 1.4;
  }
  .uh-asw-minimized {
    display: none !important;
  }
  /* 拖拽进行中：关闭过渡，指针移动直接跟手 */
  .uh-asw.uh-asw-dragging {
    transition: none !important;
  }
  /* 关闭动画：淡出后由 JS remove */
  .uh-asw.uh-asw-closing {
    opacity: 0;
    transition: opacity 0.2s ease;
  }

  /* ===== 9 向锚点（配合 JS inline transform 偏移） ===== */
  .uh-asw.uh-asw-pos-top-left { top: 8px; left: 8px; }
  .uh-asw.uh-asw-pos-top-center { top: 8px; left: 50%; }
  .uh-asw.uh-asw-pos-top-right { top: 8px; right: 8px; }
  .uh-asw.uh-asw-pos-right-center { top: 50%; right: 8px; }
  .uh-asw.uh-asw-pos-bottom-right { bottom: 8px; right: 8px; }
  .uh-asw.uh-asw-pos-bottom-center { bottom: 8px; left: 50%; }
  .uh-asw.uh-asw-pos-bottom-left { bottom: 8px; left: 8px; }
  .uh-asw.uh-asw-pos-left-center { top: 50%; left: 8px; }
  .uh-asw.uh-asw-pos-center { top: 50%; left: 50%; }

  /* ===== 顶部标题栏（拖拽手柄） ===== */
  .uh-asw-header {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 14px 10px;
    cursor: default;
  }
  .uh-asw-draggable .uh-asw-header {
    cursor: grab;
  }
  .uh-asw-dragging .uh-asw-header {
    cursor: grabbing;
  }
  .uh-asw-header-icon {
    width: 18px;
    height: 18px;
    flex: none;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .uh-asw-header-icon img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
  .uh-asw-title {
    flex: 1;
    font-size: 15px;
    font-weight: 600;
    color: #1a1a1a;
  }
  /* 右上角操作按钮（最小化/关闭统一定位，任一隐藏不位移） */
  .uh-asw-topbar {
    display: flex;
    gap: 4px;
  }
  .uh-asw-topbar-btn {
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    border-radius: 6px;
    border: 1px solid rgba(255, 255, 255, 0.5);
    background: rgba(255, 255, 255, 0.75);
    box-shadow: 0 0 12px rgba(0, 0, 0, 0.075);
    color: #999999;
    font-size: 14px;
    line-height: 20px;
    text-align: center;
    cursor: pointer;
    padding: 0;
  }
  .uh-asw-topbar-btn:hover {
    color: #333;
  }

  /* ===== 分段器（shadcn Tabs 风格） ===== */
  .uh-asw-body {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    padding: 0 12px 12px;
  }
  /* 列表滚动区：与页签（uh-asw-segmented）同级，页签常驻、仅列表滚动 */
  .uh-asw-content {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    scrollbar-width: thin;
    scrollbar-color: #8b8b8b transparent;
  }
  .uh-asw-content::-webkit-scrollbar {
    width: 6px;
  }
  .uh-asw-content::-webkit-scrollbar-track {
    background: transparent;
  }
  .uh-asw-content::-webkit-scrollbar-thumb {
    background: #8b8b8b;
    border-radius: 3px;
  }
  .uh-asw-segmented {
    display: flex;
    gap: 4px;
    padding: 4px;
    background: rgba(0, 0, 0, 0.05);
    border-radius: 8px;
    margin-bottom: 10px;
  }
  .uh-asw-seg-item {
    flex: 1;
    box-sizing: border-box;
    padding: 6px 0;
    border: none;
    border-radius: 8px;
    background: transparent;
    color: #666666;
    font-size: 13px;
    cursor: pointer;
    font-family: inherit;
  }
  .uh-asw-seg-active {
    background: #1A1B1D;
    color: #ffffff;
    font-weight: 600;
    box-shadow: 0 1px 2px rgba(14, 23, 49, 0.35);
  }

  /* ===== 条目列表（info-card 灰底卡） ===== */
  .uh-asw-group-name {
    font-size: 11px;
    color: rgba(0, 0, 0, 0.45);
    margin: 6px 2px 4px;
  }
  .uh-asw-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    border-radius: 10px;
    background: rgba(0, 0, 0, 0.03);
    cursor: pointer;
    margin-bottom: 6px;
    transition: background 0.15s ease;
  }
  .uh-asw-item:hover {
    background: rgba(0, 0, 0, 0.06);
  }
  .uh-asw-item-icon {
    width: 40px;
    height: 40px;
    border-radius: 7px;
    flex: none;
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #ffffff;
    font-size: 15px;
    font-weight: 600;
    background: #1A1B1D;
  }
  .uh-asw-item-icon.app {
    background: #c6f921;
    color: #1A1B1D;
  }
  .uh-asw-item-icon img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  .uh-asw-item-info {
    flex: 1;
    min-width: 0;
  }
  .uh-asw-item-name {
    font-size: 13px;
    font-weight: 600;
    color: #1a1a1d;
    display: flex;
    align-items: center;
    gap: 6px;
  }
  .uh-asw-tag {
    flex: none;
    font-size: 10px;
    font-weight: 400;
    padding: 1px 6px;
    border-radius: 4px;
    background: #c6f91f;
    color: #1f2a05;
  }
  /* 详情弹窗标题后置的类型标签间距（列表内标签位于名称前，间距由 item-name 的 gap 提供） */
  .uh-asw-modal-title .uh-asw-tag {
    margin-left: 6px;
  }
  .uh-asw-item-desc {
    font-size: 11px;
    color: rgba(0, 0, 0, 0.45);
    margin-top: 2px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .uh-asw-item-thumb {
    width: 34px;
    height: 34px;
    flex: none;
    border-radius: 8px;
    border: 1px solid rgba(0, 0, 0, 0.08);
    background: #ffffff;
    padding: 2px;
    box-sizing: border-box;
  }
  .uh-asw-item-thumb img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
  .uh-asw-empty {
    padding: 20px 0;
    text-align: center;
    font-size: 13px;
    color: #999999;
  }

  /* ===== 底部操作区（申请入口开关控制显隐） ===== */
  .uh-asw-actions {
    flex-shrink: 0;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    border-top: 1px solid rgba(0, 0, 0, 0.06);
    padding: 10px 14px 12px;
  }
  .uh-asw-hint {
    flex-basis: 100%;
    text-align: center;
    font-size: 10px;
    line-height: 1;
    color: rgba(0, 0, 0, 0.45);
  }

  /* ===== 按钮（对齐 uh-fpw-btn） ===== */
  .uh-asw-btn {
    flex: 1;
    box-sizing: border-box;
    border: 1px solid rgba(0, 0, 0, 0.05);
    border-radius: 6px;
    background: #ffffff;
    color: #1A1B1D;
    font-size: 12px;
    line-height: 1;
    padding: 8px 0;
    cursor: pointer;
    text-align: center;
    font-family: inherit;
    box-shadow: 0 0 12px rgba(0, 0, 0, 0.05);
    transition: background 0.15s ease;
  }
  .uh-asw-btn:hover {
    background: #f1f5f9;
  }
  .uh-asw-btn-primary {
    background: #1A1B1D;
    border-color: transparent;
    color: #ffffff;
  }
  .uh-asw-btn-primary:hover {
    background: #1a1b1de3;
  }

  /* ===== 最小化小球（独立 fixed 元素，无条件可拖） ===== */
  .uh-asw-mini-dot {
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    width: 40px;
    height: 40px;
    padding: 0;
    border: 2px solid rgba(255, 255, 255, 1);
    border-radius: 50%;
    overflow: hidden;
    position: fixed;
    z-index: 2147482999; /* 兜底层级，实际以内联 z-index（后台 zIndex 配置）为准 */
    cursor: pointer;
    background: rgba(255, 255, 255, 0.9);
    -webkit-backdrop-filter: blur(2px);
    backdrop-filter: blur(2px);
    box-shadow: 0 0 16px rgba(0, 0, 0, 0.25);
    font-family: inherit;
  }
  .uh-asw-mini-dot img {
    display: block;
    width: 60%;
    height: 60%;
    object-fit: contain;
  }
  /* 小球默认态复用面板的 9 向锚点（不依赖 JS 测量，规避主题初始化期间 body 隐藏） */
  .uh-asw-mini-dot.uh-asw-pos-top-left { top: 8px; left: 8px; }
  .uh-asw-mini-dot.uh-asw-pos-top-center { top: 8px; left: 50%; }
  .uh-asw-mini-dot.uh-asw-pos-top-right { top: 8px; right: 8px; }
  .uh-asw-mini-dot.uh-asw-pos-right-center { top: 50%; right: 8px; }
  .uh-asw-mini-dot.uh-asw-pos-bottom-right { bottom: 8px; right: 8px; }
  .uh-asw-mini-dot.uh-asw-pos-bottom-center { bottom: 8px; left: 50%; }
  .uh-asw-mini-dot.uh-asw-pos-bottom-left { bottom: 8px; left: 8px; }
  .uh-asw-mini-dot.uh-asw-pos-left-center { top: 50%; left: 8px; }
  .uh-asw-mini-dot.uh-asw-pos-center { top: 50%; left: 50%; }
  .uh-asw-mini-plus {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    font-weight: 600;
    color: #ffffff;
    background: rgba(0, 0, 0, 0.45);
    opacity: 0;
    transition: opacity 0.15s ease;
  }
  .uh-asw-mini-dot:hover .uh-asw-mini-plus {
    opacity: 1;
  }

  /* ===== 弹窗（遮罩 + 居中卡片，渲染于 shadow DOM 内） ===== */
  .uh-asw-overlay {
    position: fixed;
    inset: 0;
    z-index: 2147483000;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.45);
    -webkit-backdrop-filter: blur(4px);
    backdrop-filter: blur(4px);
    padding: 16px;
    box-sizing: border-box;
  }
  .uh-asw-modal {
    box-sizing: border-box;
    width: 400px;
    max-height: 80vh;
    display: flex;
    flex-direction: column;
    background: rgba(255, 255, 255, 0.98);
    -webkit-backdrop-filter: blur(16px);
    backdrop-filter: blur(16px);
    border: 1px solid rgba(255, 255, 255, 0.6);
    border-radius: 14px;
    box-shadow: 0 16px 60px rgba(0, 0, 0, 0.18);
    overflow: hidden;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC",
      "Microsoft YaHei", sans-serif;
  }
  .uh-asw-modal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 14px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  }
  .uh-asw-modal-title {
    font-size: 15px;
    font-weight: 600;
    color: #1a1a1a;
  }
  .uh-asw-modal-close {
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    border-radius: 6px;
    border: 1px solid rgba(255, 255, 255, 0.5);
    background: rgba(255, 255, 255, 0.75);
    box-shadow: 0 0 12px rgba(0, 0, 0, 0.075);
    font-size: 14px;
    color: #999999;
    cursor: pointer;
    padding: 0;
  }
  .uh-asw-modal-close:hover {
    color: #333333;
  }
  .uh-asw-modal-body {
    padding: 14px;
    overflow-y: auto;
    scrollbar-width: thin;
    scrollbar-color: #8b8b8b transparent;
  }

  /* ===== 条目详情弹窗 ===== */
  .uh-asw-detail-head {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 14px;
  }
  .uh-asw-detail-head .uh-asw-item-icon {
    width: 40px;
    height: 40px;
  }
  .uh-asw-detail-name {
    font-size: 14px;
    font-weight: 600;
    color: #1a1a1d;
  }
  .uh-asw-detail-desc {
    font-size: 11px;
    color: rgba(0, 0, 0, 0.45);
    margin-top: 2px;
  }
  .uh-asw-qr-card {
    width: 360px;
    height: 360px;
    max-width: calc(100vw - 48px);
    max-height: calc(100vw - 48px);
    margin: 0 auto;
    border: 1px solid rgba(0, 0, 0, 0.08);
    border-radius: 10px;
    background: #ffffff;
    padding: 10px;
    box-sizing: border-box;
  }
  .uh-asw-qr-card img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
  .uh-asw-qr-tip {
    text-align: center;
    font-size: 11px;
    color: rgba(0, 0, 0, 0.45);
    margin: 10px 0 12px;
  }
  .uh-asw-link-row {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 12px;
  }
  .uh-asw-link-row .uh-asw-btn {
    flex: none;
    width: 60px;
  }
  .uh-asw-detail-actions {
    display: flex;
    gap: 8px;
  }

  /* ===== 申请表单 ===== */
  .uh-asw-form {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .uh-asw-field {
    display: flex;
    flex-direction: column;
    gap: 4px;
    font-size: 12px;
    color: #666666;
  }
  .uh-asw-apply-panel .uh-asw-field {
    margin-bottom: 10px;
  }
  .uh-asw-apply-panel .uh-asw-field:last-child {
    margin-bottom: 0;
  }
  .uh-asw-field input[type="text"] {
    box-sizing: border-box;
    width: 100%;
    height: 32px;
    padding: 0 10px;
    border: 1px solid rgba(0, 0, 0, 0.12);
    border-radius: 8px;
    font-size: 13px;
    color: #1a1a1a;
    outline: none;
    font-family: inherit;
    background: #ffffff;
  }
  .uh-asw-field input[type="text"]:focus,
  .uh-asw-field textarea:focus {
    border-color: #c6f921;
  }
  .uh-asw-field textarea {
    box-sizing: border-box;
    width: 100%;
    border: 1px solid rgba(0, 0, 0, 0.12);
    border-radius: 8px;
    font-size: 13px;
    color: #1a1a1a;
    outline: none;
    font-family: inherit;
    background: #ffffff;
    padding: 6px 10px;
    resize: vertical;
  }
  .uh-asw-captcha-input {
    display: flex;
    gap: 8px;
  }
  .uh-asw-captcha-input input {
    flex: 1;
  }
  .uh-asw-captcha-img {
    width: 100px;
    height: 32px;
    border-radius: 8px;
    border: 1px solid rgba(0, 0, 0, 0.1);
    cursor: pointer;
    object-fit: cover;
  }
  .uh-asw-form-actions {
    display: flex;
    gap: 8px;
    margin-top: 12px;
  }
  /* 预览图动态行 */
  .uh-asw-shot-row {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 6px;
  }
  .uh-asw-shot-input {
    flex: 1;
    min-width: 0;
    box-sizing: border-box;
    height: 32px;
    padding: 0 10px;
    border: 1px solid rgba(0, 0, 0, 0.12);
    border-radius: 8px;
    font-size: 13px;
    color: #1a1a1a;
    outline: none;
    font-family: inherit;
    background: #ffffff;
  }
  .uh-asw-shot-input:focus {
    border-color: #c6f921;
  }
  .uh-asw-shot-remove {
    flex-shrink: 0;
    width: 28px;
    height: 28px;
    border: none;
    border-radius: 6px;
    background: rgba(0, 0, 0, 0.05);
    color: #999999;
    font-size: 16px;
    line-height: 1;
    cursor: pointer;
    padding: 0;
  }
  .uh-asw-shot-remove:hover {
    background: rgba(0, 0, 0, 0.1);
    color: #333333;
  }

  /* ===== 申请弹窗：分段器 + 面板 + 底部固定操作区 ===== */
  .uh-asw-segmented-modal {
    display: flex;
    gap: 4px;
    margin: 8px 14px 0;
    padding: 4px;
    background: rgba(0, 0, 0, 0.05);
    border-radius: 8px;
  }
  .uh-asw-apply-body {
    display: flex;
    flex-direction: column;
    padding: 10px 14px 14px;
    overflow: hidden;
  }
  .uh-asw-apply-panels {
    flex: 1;
    min-height: 0;
    max-height: 40vh;
    overflow-y: auto;
    scrollbar-width: thin;
    scrollbar-color: #8b8b8b transparent;
  }
  .uh-asw-apply-footer {
    flex-shrink: 0;
    border-top: 1px solid rgba(0, 0, 0, 0.06);
    padding-top: 10px;
    margin-top: 10px;
  }

  /* ===== 友链信息（小程序信息 + 博主信息，输入框行 + 复制） ===== */
  .uh-asw-info-card {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 12px;
    border-radius: 10px;
    background: rgba(0, 0, 0, 0.03);
    margin-bottom: 10px;
  }
  .uh-asw-info-card-title {
    font-size: 13px;
    font-weight: 600;
    color: #1a1a1a;
    margin-bottom: 2px;
  }
  .uh-asw-copy-row {
    display: flex;
    align-items: center;
    gap: 6px;
  }
  .uh-asw-copy-label {
    flex-shrink: 0;
    font-size: 12px;
    color: #000000;
    min-width: 64px;
    text-align: right;
  }
  .uh-asw-copy-input {
    flex: 1;
    min-width: 0;
    box-sizing: border-box;
    height: 28px;
    padding: 0 8px;
    border: 1px solid #ffffff;
    border-radius: 6px;
    font-size: 12px;
    color: #1a1a1a;
    background: rgba(255, 255, 255, 0.75);
    outline: none;
    font-family: inherit;
  }
  .uh-asw-copy-input:focus {
    border-color: #c6f921;
  }
  .uh-asw-copy-textarea {
    height: auto;
    min-height: 40px;
    padding: 5px 8px;
    line-height: 1.4;
    resize: none;
    font-family: inherit;
  }
  .uh-asw-copy-btn {
    flex-shrink: 0;
    flex: none;
    width: 60px;
    padding: 8px 0;
  }
  .uh-asw-copy-all {
    width: 100%;
  }
  .uh-asw-loading {
    padding: 20px 0;
    text-align: center;
    font-size: 13px;
    color: #999999;
  }
`;
