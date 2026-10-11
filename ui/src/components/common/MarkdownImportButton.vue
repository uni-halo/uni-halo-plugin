<script setup lang="ts">
import { VButton } from "@halo-dev/components";
import { ref } from "vue";
import { marked } from "marked";

/**
 * Markdown 导入按钮：选择 .md 文件并解析为 HTML，经 emit("imported", html) 交由
 * 调用方写入富文本。仅负责解析，不做覆盖确认（由调用方决定）。
 */
const emit = defineEmits<{
  (event: "imported", html: string, fileName: string): void;
  (event: "error", message: string): void;
}>();

const fileInput = ref<HTMLInputElement>();

const open = () => fileInput.value?.click();

const handleFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  // 重置 value，允许再次选择同一文件
  input.value = "";
  if (!file) {
    return;
  }
  try {
    const markdown = await file.text();
    const html = await marked.parse(markdown);
    emit("imported", html, file.name);
  } catch (error) {
    emit("error", (error as Error).message);
  }
};
</script>

<template>
  <VButton size="sm" @click="open">上传 Markdown</VButton>
  <input ref="fileInput" type="file" accept=".md,.markdown,text/markdown" class=":uno: hidden"
    @change="handleFileChange" />
</template>
