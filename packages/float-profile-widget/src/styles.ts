/**
 * 悬浮卡片样式（Lit css 模板）。
 *
 * 组件使用 shadow DOM，样式完全内聚不泄漏、外部主题样式也无法穿透影响组件；
 * 弹窗（overlay/modal）渲染在 shadow DOM 内部，按钮等选择器直接用类名
 * （无需 .uh-fpw 前缀），修复了此前「弹窗内按钮无样式」的问题。
 * 设计参考 app 端 glass（uh-styles / tabbar：半透明背景 + backdrop blur + 白色细边框 + 柔和阴影）。
 */
import { css } from "lit";

export const styles = css`
  :host {
    display: block;
  }

  .uh-fpw {
    position: fixed;
    z-index: 9999;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 10px;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC",
      "Microsoft YaHei", sans-serif;
    background: rgba(255, 255, 255, 1);
    border: 2px solid rgba(255, 255, 255, 0.65);
    border-radius: 14px;
    box-shadow: 0 16px 60px rgba(0, 0, 0, 0.06);
    user-select: none;
    -webkit-user-select: none;
    cursor: default;
    max-width: 40vw;
    line-height: 1.4;
    transition: transform 0.3s ease;
  }
  /* 允许拖拽（dragEnabled）时显示抓手光标 */
  .uh-fpw-draggable {
    cursor: grab;
  }

  /* ===== 9 向锚点（配合 JS inline transform 偏移） ===== */
  .uh-fpw.uh-fpw-pos-top-left { top: 8px; left: 8px; }
  .uh-fpw.uh-fpw-pos-top-center { top: 8px; left: 50%; }
  .uh-fpw.uh-fpw-pos-top-right { top: 8px; right: 8px; }
  .uh-fpw.uh-fpw-pos-right-center { top: 50%; right: 8px; }
  .uh-fpw.uh-fpw-pos-bottom-right { bottom: 8px; right: 8px; }
  .uh-fpw.uh-fpw-pos-bottom-center { bottom: 8px; left: 50%; }
  .uh-fpw.uh-fpw-pos-bottom-left { bottom: 8px; left: 8px; }
  .uh-fpw.uh-fpw-pos-left-center { top: 50%; left: 8px; }
  .uh-fpw.uh-fpw-pos-center { top: 50%; left: 50%; }

  /* ===== 内容 ===== */
  .uh-fpw-main{
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
  }
  .uh-fpw-img {
    display: block;
    width: 100%;
    /* 太阳码为正方形：宽度撑满卡片，高度按 aspect-ratio 自动 */
    aspect-ratio: 1 / 1;
    object-fit: cover;
    border-radius: 8px;
  }
  .uh-fpw-name {
    font-weight: 600;
    text-align: center;
    max-width: 100%;
    word-break: break-all;
  }
  .uh-fpw-desc {
    text-align: center;
    word-break: break-all;
  }

  /* ===== 右上角操作按钮组（最小化/关闭统一定位，任一隐藏不位移） ===== */
  .uh-fpw-topbar {
    position: absolute;
    top: 8px;
    right: 8px;
    display: flex;
    gap: 4px;
    z-index: 3;
  }
  .uh-fpw-close {
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    border-radius: 6px;
    border: 1px solid rgba(255,255,255, 0.5);
    background: rgba(255, 255, 255, 0.75);
    box-shadow: 0 0 12px rgba(0, 0, 0, 0.075);
    color: #999999;
    font-size: 14px;
    line-height: 20px;
    text-align: center;
    cursor: pointer;
  }
  .uh-fpw-close:hover {
    color: #333;
  }

  /* ===== 最小化按钮（右上角按钮组内，关闭按钮左侧） ===== */
  .uh-fpw-minimize {
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
  }
  .uh-fpw-minimize:hover {
    color: #333;
  }
  /* 最小化态：卡片容器整体隐藏（小图为独立 fixed 元素，不影响卡片样式） */
  .uh-fpw-minimized {
    display: none !important;
  }
  /* 圆形小图：独立 fixed 元素（minimized 时显示在卡片原位置），hover 显示 + 覆盖层 */
  .uh-fpw-mini-dot {
    display: block;
    box-sizing: border-box;
    width: 50px;
    height: 50px;
    padding: 0;
    border: 2px solid rgba(255, 255, 255, 1);
    border-radius: 50%;
    overflow: hidden;
    position: fixed;
    z-index: 2147482999; /* 与贴边把手一致：浮在最上层，避免被页面元素遮挡无法点击 */
    cursor: pointer;
    background: rgba(255, 255, 255, 0.95);
    box-shadow: 0 0 16px rgba(0, 0, 0, 0.25);
  }
  .uh-fpw-mini-dot img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  .uh-fpw-mini-plus {
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
  .uh-fpw-mini-dot:hover .uh-fpw-mini-plus {
    opacity: 1;
  }

  /* ===== 拖拽 ===== */
  .uh-fpw.uh-fpw-dragging {
    transition: none !important;
    cursor: default;
  }

  /* ===== 贴边隐藏（完全隐藏 + 边缘触发把手，JS 控制 hover 类滑出） ===== */
  .uh-fpw.uh-fpw-edge-left { transform: translateX(-100%); }
  .uh-fpw.uh-fpw-edge-right { transform: translateX(100%); }
  .uh-fpw.uh-fpw-edge-top { transform: translateY(-100%); }
  .uh-fpw.uh-fpw-edge-bottom { transform: translateY(100%); }
  .uh-fpw.uh-fpw-edge-hover {
    transform: translate(0, 0) !important;
  }
  /* 边缘触发把手（贴边后露出的触发元素，hover 滑出、点击完全恢复） */
  .uh-fpw-edge-trigger {
    position: fixed;
    width: 8px;
    height: 35px;
    border: none;
    border-radius: 4px;
    padding: 0;
    background: rgb(255, 255, 255, 0.85);
    box-shadow: 0 0 12px rgba(0, 0, 0, 0.15);
    cursor: pointer;
    z-index: 99999999;
  }
  .uh-fpw-edge-trigger:hover {
    background: #1A1B1D;
  }
  .uh-fpw-edge-trigger-top,
  .uh-fpw-edge-trigger-bottom {
    width: 35px;
    height: 8px;
  }

  /* ===== 关闭动画 ===== */
  .uh-fpw.uh-fpw-closing {
    opacity: 0;
    transition: opacity 0.2s ease;
  }

  /* ===== 底部操作按钮（小程序申请开关开启后显示） ===== */
  .uh-fpw-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    width: 100%;
    margin-top: 2px;
  }
  .uh-fpw-btn {
    flex: 1;
    box-sizing: border-box;
    border: 1px solid rgba(0,0,0,0.05);
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
  .uh-fpw-btn:hover {
    background: #f1f5f9;
  }
  .uh-fpw-btn-primary {
    background: #1A1B1D;
    border-color: transparent;
    color: #ffffff;
  }
  .uh-fpw-btn-primary:hover {
    background: #1a1b1de3;
  }
  .uh-fpw-hint {
    flex-basis: 100%;
    text-align: center;
    font-size: 10px;
    line-height: 1;
    color: rgba(0, 0, 0, 0.45);
  }

  /* ===== 弹窗（遮罩 + 居中卡片，渲染于 shadow DOM 内） ===== */
  .uh-fpw-overlay {
    position: fixed;
    inset: 0;
    z-index: 2147483000;
    display: flex;
    align-items: center;
    justify-content: center;
    /* glass 遮罩：半透明 + 轻微毛玻璃（对齐 app 弹窗遮罩） */
    background: rgba(0, 0, 0, 0.45);
    -webkit-backdrop-filter: blur(4px);
    backdrop-filter: blur(4px);
    padding: 16px;
    box-sizing: border-box;
  }
  .uh-fpw-modal {
    box-sizing: border-box;
    width: 100%;
    max-width: 420px;
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
  .uh-fpw-modal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 14px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  }
  .uh-fpw-modal-title {
    font-size: 15px;
    font-weight: 600;
    color: #1a1a1a;
  }
  .uh-fpw-modal-close {
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    border-radius: 6px;
    border: 1px solid rgba(255,255,255, 0.5);
    background: rgba(255, 255, 255, 0.75);
    box-shadow: 0 0 12px rgba(0, 0, 0, 0.075);
    font-size: 14px;
    color: #999999;
    cursor: pointer;
  }
  .uh-fpw-modal-close:hover {
    color: #333333;
  }
  .uh-fpw-modal-body {
    padding: 14px;
    overflow-y: auto;
  }

  /* ===== 申请表单 ===== */
  .uh-fpw-form {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .uh-fpw-field {
    display: flex;
    flex-direction: column;
    gap: 4px;
    font-size: 12px;
    color: #666666;
  }
  /* 申请弹窗面板内字段上下间距（footer 内验证码区不受影响） */
  .uh-fpw-apply-panel .uh-fpw-field {
    margin-bottom: 10px;
  }
  .uh-fpw-apply-panel .uh-fpw-field:last-child {
    margin-bottom: 0;
  }
  .uh-fpw-field input[type="text"] {
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
  .uh-fpw-field input[type="text"]:focus,
  .uh-fpw-field select:focus,
  .uh-fpw-field textarea:focus {
    border-color: #37c2bc;
  }
  .uh-fpw-field select,
  .uh-fpw-field textarea {
    box-sizing: border-box;
    width: 100%;
    border: 1px solid rgba(0, 0, 0, 0.12);
    border-radius: 8px;
    font-size: 13px;
    color: #1a1a1a;
    outline: none;
    font-family: inherit;
    background: #ffffff;
  }
  .uh-fpw-field select {
    height: 32px;
    padding: 0 8px;
  }
  .uh-fpw-field textarea {
    padding: 6px 10px;
    resize: vertical;
  }
  .uh-fpw-captcha-input {
    display: flex;
    gap: 8px;
  }
  .uh-fpw-captcha-input input {
    flex: 1;
  }
  .uh-fpw-captcha-img {
    width: 100px;
    height: 32px;
    border-radius: 8px;
    border: 1px solid rgba(0, 0, 0, 0.1);
    cursor: pointer;
    object-fit: cover;
  }
  .uh-fpw-form-actions {
    display: flex;
    gap: 8px;
    margin-top: 12px;
  }

  /* ===== 申请弹窗：分段器 + 面板 + 底部固定操作区 ===== */
  .uh-fpw-modal-apply {
    max-height: 80vh;
  }
  /* shadcn Tabs（radix tabs）风格分段器：track 连体浅灰背景，激活项浮起 */
  .uh-fpw-segmented {
    display: flex;
    gap: 4px;
    margin: 8px 14px 0;
    padding: 4px; /* track 内边距（对齐 tabs-list p-1） */
    background: rgba(0, 0, 0, 0.05);
    border-radius: 8px;
  }
  .uh-fpw-seg-item {
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
  .uh-fpw-seg-active {
    /* 激活块：主色 #1A1B1D + 白字 + 轻投影 */
    background: #1A1B1D;
    color: #ffffff;
    font-weight: 600;
    box-shadow: 0 1px 2px rgba(14, 23, 49, 0.35);
  }
  .uh-fpw-apply-body {
    display: flex;
    flex-direction: column;
    padding: 10px 14px 14px;
    overflow: hidden;
  }
  .uh-fpw-apply-panels {
    flex: 1;
    min-height: 0;
    max-height: 40vh; /* 内容区最大高度 40vh + 滚动 */
    overflow-y: auto;
    scrollbar-width: thin; /* Firefox */
    scrollbar-color: rgba(120, 130, 150, 0.4) transparent;
  }
  .uh-fpw-apply-panels::-webkit-scrollbar {
    width: 6px;
  }
  .uh-fpw-apply-panels::-webkit-scrollbar-track {
    background: transparent;
  }
  .uh-fpw-apply-panels::-webkit-scrollbar-thumb {
    background: rgba(120, 130, 150, 0.4);
    border-radius: 3px;
  }
  .uh-fpw-apply-panels::-webkit-scrollbar-thumb:hover {
    background: rgba(120, 130, 150, 0.6);
  }
  .uh-fpw-apply-footer {
    flex-shrink: 0;
    border-top: 1px solid rgba(0, 0, 0, 0.06);
    padding-top: 10px;
    margin-top: 10px;
  }
  /* 预览图动态行 */
  .uh-fpw-shot-row {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 6px; /* 预览图行之间上下间距 */
  }
  .uh-fpw-shot-input {
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
  .uh-fpw-shot-input:focus {
    border-color: #37c2bc;
  }
  .uh-fpw-shot-remove {
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
  .uh-fpw-shot-remove:hover {
    background: rgba(0, 0, 0, 0.1);
    color: #333333;
  }
  .uh-fpw-shot-add {
    margin-top: 2px;
  }

  /* ===== 友链信息（小程序信息 + 博主信息，输入框行 + 复制） ===== */
  .uh-fpw-info-card {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 12px;
    border-radius: 10px;
    background: rgba(0, 0, 0, 0.03);
    margin-bottom: 10px;
  }
  .uh-fpw-info-card-title {
    font-size: 13px;
    font-weight: 600;
    color: #1a1a1a;
    margin-bottom: 2px;
  }
  .uh-fpw-copy-row {
    display: flex;
    align-items: center;
    gap: 6px;
  }
  .uh-fpw-copy-label {
    flex-shrink: 0;
    font-size: 12px;
    color: #000000; /* label 正常黑色 */
    min-width: 64px;
    text-align: right;
  }
  .uh-fpw-copy-input {
    flex: 1;
    min-width: 0;
    box-sizing: border-box;
    height: 28px;
    padding: 0 8px;
    /* 只读输入框：浅白背景 + 白色边框（与玻璃弹窗背景区分） */
    border: 1px solid #ffffff;
    border-radius: 6px;
    font-size: 12px;
    color: #1a1a1a;
    background: rgba(255, 255, 255, 0.75);
    outline: none;
    font-family: inherit;
  }
  .uh-fpw-copy-input:focus {
    border-color: #37c2bc;
  }
  .uh-fpw-copy-textarea {
    height: auto;
    min-height: 40px;
    padding: 5px 8px;
    line-height: 1.4;
    resize: none;
    font-family: inherit;
  }
  .uh-fpw-copy-btn {
    flex-shrink: 0;
    flex: none;
    width: 60px;
    padding: 8px 0;
  }
  .uh-fpw-copy-all {
    width: 100%;
  }
  .uh-fpw-loading,
  .uh-fpw-empty {
    padding: 20px 0;
    text-align: center;
    font-size: 13px;
    color: #999999;
  }
`;
