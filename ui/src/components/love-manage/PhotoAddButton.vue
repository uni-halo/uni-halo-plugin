<script setup lang="ts">
import {
  IconAddCircle,
  Toast,
  VButton,
  VDropdown,
  VDropdownItem,
  VModal,
} from "@halo-dev/components";
import { utils, type AttachmentLike } from "@halo-dev/ui-shared";
import { ref } from "vue";
import { loveAlbumsApi } from "@/api";
import type { LoveAlbum } from "@/types";

/**
 * 照片「新增」按钮（交互对齐图库插件 plugin-photos 的 AddButton）：
 * 下拉多入口——从附件库选择（官方 AttachmentSelectorModal，含本地上传）/
 * 手动添加（单个图片地址）/ 批量添加外链（每行一个链接）。
 * 不填写照片信息。
 */
const props = defineProps<{
  album: LoveAlbum;
}>();

const emit = defineEmits<{
  (event: "imported"): void;
}>();

const selectorVisible = ref(false);
const externalVisible = ref(false);

/** 批量写入相册；返回是否全部成功，由调用方决定后续交互 */
async function importPhotos(
  items: Array<{ url: string; title?: string }>
): Promise<boolean> {
  if (!items.length) {
    Toast.warning("请选择至少一张图片");
    return false;
  }
  try {
    await loveAlbumsApi.addPhotos(props.album.metadata.name, items);
    Toast.success(`已导入 ${items.length} 张照片`);
    emit("imported");
    return true;
  } catch (error) {
    Toast.error((error as Error).message);
    return false;
  }
}

/** 官方附件选择器确认（支持多选，含本地上传结果） */
const handleAttachmentsSelect = async (attachments: AttachmentLike[]) => {
  const urls = attachments
    .map((attachment) => utils.attachment.convertToSimple(attachment)?.url)
    .filter((url): url is string => !!url);
  const ok = await importPhotos(urls.map((url) => ({ url })));
  if (ok) {
    selectorVisible.value = false;
  }
};

// ===== 批量添加外链（每行一个链接） =====

const externalUrlText = ref("");
const externalSaving = ref(false);

/** 从链接地址推导展示名（对齐 plugin-photos 的 deriveNameFromUrl） */
function deriveNameFromUrl(url: string): string {
  try {
    const filename = new URL(url).pathname.split("/").pop();
    return filename && filename !== "" ? decodeURIComponent(filename) : "";
  } catch {
    return "";
  }
}

/** 批量添加外链：每行一个链接 */
const handleExternalSubmit = async () => {
  const urls = externalUrlText.value
    .split("\n")
    .map((line) => line.trim())
    .filter((line) => line.length > 0);
  if (!urls.length) {
    Toast.warning("请输入至少一个有效的图片链接");
    return;
  }
  externalSaving.value = true;
  // 外链无附件实体，名称按链接文件名推导存入 title
  const ok = await importPhotos(
    urls.map((url) => ({ url, title: deriveNameFromUrl(url) }))
  );
  externalSaving.value = false;
  if (ok) {
    externalUrlText.value = "";
    externalVisible.value = false;
  }
};
</script>

<template>
  <VDropdown>
    <VButton size="sm" type="secondary">
      <template #icon>
        <IconAddCircle class="w-3.5 h-3.5" />
      </template>
      新增
    </VButton>
    <template #popper>
      <VDropdownItem @click="selectorVisible = true">从附件库选择</VDropdownItem>
      <VDropdownItem @click="externalVisible = true">批量添加外链</VDropdownItem>
    </template>
  </VDropdown>

  <!-- 官方附件选择器（全局组件，含本地上传 / 附件库，多选） -->
  <AttachmentSelectorModal v-if="selectorVisible" :accepts="['image/*']" @select="handleAttachmentsSelect"
    @close="selectorVisible = false" />

  <!-- 批量添加外链：每行一个链接（弹窗常驻，不随显隐卸载） -->
  <VModal :visible="externalVisible" title="批量添加外链" :width="600" :layer-closable="true"
    @update:visible="externalVisible = $event" @close="externalVisible = false">
    <FormKit v-model="externalUrlText" name="external_photo_urls" label="链接列表" type="textarea" :rows="10"
      placeholder="每行一个图片链接地址" validation="required" />
    <template #footer>
      <div class=":uno: flex items-center gap-3">
        <VButton type="secondary" :loading="externalSaving" @click="handleExternalSubmit">导入</VButton>
        <VButton @click="externalVisible = false">取消</VButton>
      </div>
    </template>
  </VModal>
</template>
