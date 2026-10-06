/**
 * 应用展示面板独立构建
 * vite lib mode（iife）产出压缩版 app-showcase-widget.js，
 * closeBundle 时自动拷贝到插件静态目录 src/main/resources/static/widgets/app-showcase-widget/
 * （文件名与插件注入 URL 保持一致）。
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
      new URL("../../src/main/resources/static/widgets/app-showcase-widget", import.meta.url),
    );

    if (!existsSync(staticDir)) {
      mkdirSync(staticDir, { recursive: true });
    }

    const srcPath = join(distDir, "app-showcase-widget.js");
    if (existsSync(srcPath)) {
      copyFileSync(srcPath, join(staticDir, "app-showcase-widget.js"));
      console.log(`✓ Copied app-showcase-widget.js -> ${staticDir}`);
    }
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
      name: "AppShowcaseWidget",
      // 固定产物文件名（含扩展名），避免 vite 为 iife 追加 .iife 后缀，
      // 与插件静态目录/注入 URL 保持一致（app-showcase-widget.js）
      fileName: () => "app-showcase-widget.js",
      formats: ["iife"],
    },
    cssCodeSplit: false,
    rollupOptions: {
      output: {
        extend: true,
        assetFileNames: "app-showcase-widget.[ext]",
      },
    },
    // minify 默认 esbuild，产物即压缩版
  },
});
