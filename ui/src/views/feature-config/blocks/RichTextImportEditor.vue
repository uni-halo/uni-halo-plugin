<script setup lang="ts">
import { Dialog, Toast, VButton } from "@halo-dev/components";
import { consoleApiClient } from "@halo-dev/api-client";
import { ref } from "vue";
import type { BannerCandidate } from "@/types";
import RichTextRow from "../fields/RichTextRow.vue";
import MarkdownImportButton from "@/components/common/MarkdownImportButton.vue";
import BannerCandidatesModal from "@/components/banner-manage/BannerCandidatesModal.vue";

/**
 * 富文本内容编辑块（通用）：富文本编辑 + 内容导入
 * （从已发布笔记导入渲染后的 HTML / 上传 Markdown 文件解析）。
 * 导入前内容非空时二次确认；确认后整体覆盖当前富文本。
 * 文章选择复用轮播管理的文章候选弹窗（后端仅返回已发布且公开的文章）。
 */
defineProps<{
  /** FormKit name（同表单内需唯一） */
  name: string;
  /** 占位提示 */
  placeholder?: string;
}>();

const model = defineModel<string | undefined>({ required: true });

/** 文章候选弹窗（复用轮播管理组件，单选模式） */
const pickerVisible = ref(false);

/** 导入前确认（内容非空时覆盖需二次确认） */
function confirmOverwrite(onConfirm: () => void) {
  if (!model.value) {
    onConfirm();
    return;
  }
  Dialog.warning({
    title: "覆盖当前内容",
    description: "导入将覆盖当前已编辑的内容，确定继续吗？",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm,
  });
}

/** 文章候选确认：拉取已发布渲染内容（HTML）覆盖当前富文本 */
async function handlePostPicked(candidates: BannerCandidate[]) {
  const candidate = candidates[0];
  if (!candidate) {
    return;
  }
  confirmOverwrite(async () => {
    try {
      const { data } = await consoleApiClient.content.post.fetchPostReleaseContent({
        name: candidate.name,
      });
      const html = data.content;
      if (!html) {
        Toast.warning("该文章暂无可导入的内容");
        return;
      }
      model.value = html;
      Toast.success("已导入文章内容");
    } catch (error) {
      Toast.error((error as Error).message);
    }
  });
}

/** Markdown 解析结果写入 */
function handleMarkdownImported(html: string, fileName: string) {
  confirmOverwrite(() => {
    model.value = html;
    Toast.success(`已导入「${fileName}」`);
  });
}

function handleMarkdownError(message: string) {
  Toast.error(`Markdown 解析失败：${message}`);
}
</script>

<template>
  <div class=":uno: mb-3 flex items-center gap-3">
    <VButton size="sm" @click="pickerVisible = true">从文章导入</VButton>
    <MarkdownImportButton @imported="handleMarkdownImported" @error="handleMarkdownError" />
    <span class=":uno: text-xs text-gray-400">导入内容将覆盖当前富文本；也可直接在下方手动编辑</span>
  </div>

  <RichTextRow v-model="model" :placeholder="placeholder" />

  <BannerCandidatesModal v-model:visible="pickerVisible" :multi-select="false"
    tip="导入后将以文章当前已发布内容覆盖富文本，文章后续修改不会同步"
    @confirm="handlePostPicked" />
</template>
