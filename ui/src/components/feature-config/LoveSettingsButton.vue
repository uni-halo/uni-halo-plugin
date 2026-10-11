<script setup lang="ts">
import { VButton } from "@halo-dev/components";
import { ref } from "vue";
import RiSettings3Line from "~icons/ri/settings-3-line";
import LoveSettingsModal from "./LoveSettingsModal.vue";

/**
 * 恋爱设置入口按钮（恋爱管理各页头 ⚙）：内含设置弹窗，挂载即用。
 * default-segment 使弹窗打开时定位到当前页对应的分段。
 * 读写 feature-config 复用既有 feature-config 角色权限。
 */
const props = defineProps<{
  /** 打开弹窗时的默认分段（diary/stories/albums/daily） */
  defaultSegment?: string;
}>();

const settingsVisible = ref(false);
</script>

<template>
  <VButton @click="settingsVisible = true">
    <template #icon>
      <RiSettings3Line class="w-4 h-4" />
    </template>
    设置
  </VButton>
  <LoveSettingsModal v-if="settingsVisible" :initial-segment="props.defaultSegment"
    @close="settingsVisible = false" />
</template>
