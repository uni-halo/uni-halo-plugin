/**
 * 悬浮卡片独立构建
 * vite lib mode（iife）产出压缩版 float-profile-widget.js + float-profile-widget.css，
 * closeBundle 时自动拷贝到插件静态目录 src/main/resources/static/widgets/float-profile-widget/
 * （文件名与手写版一致，插件注入 URL 无需改动）。
 */
import { copyFileSync, existsSync, mkdirSync } from "fs";
import { join } from "path";
import { fileURLToPath } from "url";
import { defineConfig, type Plugin } from "vite";

const copyToStatic = (): Plugin => ({
  name: "copy-to-static",
  closeBundle() {
    // outDir 已对齐 bundler-kit 约定为 build/dist，拷贝源须同步
    const distDir = join(process.cwd(), "build", "dist");
    const staticDir = fileURLToPath(
      new URL("../../src/main/resources/static/widgets/float-profile-widget", import.meta.url),
    );

    if (!existsSync(staticDir)) {
      mkdirSync(staticDir, { recursive: true });
    }

    const targets: Array<[string, string]> = [
      ["float-profile-widget.js", "float-profile-widget.js"],
      ["float-profile-widget.css", "float-profile-widget.css"],
    ];
    targets.forEach(([src, dest]) => {
      const srcPath = join(distDir, src);
      const destPath = join(staticDir, dest);
      if (existsSync(srcPath)) {
        copyFileSync(srcPath, destPath);
        console.log(`✓ Copied ${src} -> ${staticDir}`);
      }
    });
  },
});

export default defineConfig({
  plugins: [copyToStatic()],
  build: {
    // 对齐 ui 工程 bundler-kit 约定：产物输出到 Gradle build/dist
    outDir: "build/dist",
    emptyOutDir: true,
    lib: {
      entry: "src/index.ts",
      name: "FloatProfileWidget",
      // 固定产物文件名（含扩展名），避免 vite 为 iife 追加 .iife 后缀，
      // 与插件静态目录/注入 URL 保持一致（float-profile-widget.js）
      fileName: () => "float-profile-widget.js",
      formats: ["iife"],
    },
    cssCodeSplit: false,
    rollupOptions: {
      output: {
        extend: true,
        assetFileNames: "float-profile-widget.[ext]",
      },
    },
    // minify 默认 esbuild，产物即压缩版
  },
});
