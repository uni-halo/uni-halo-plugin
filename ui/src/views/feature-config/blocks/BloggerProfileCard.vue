<script setup lang="ts">
import AttachmentRow from "../fields/AttachmentRow.vue";

/**
 * 博主页资料卡视觉块：背景图 + 波浪图（宽度对齐 App 端 375px 设计稿）。
 * 只认 BloggerPage 资料卡字段，不知道它渲染在哪个分区。
 */
const model = defineModel<{ bgImageUrl?: string; waveImageUrl?: string }>({ required: true });
</script>

<template>
  <div class=":uno: space-y-4">
    <AttachmentRow v-model="model.bgImageUrl!" name="blogger_bg_image" label="资料卡背景图" width="375px" />
    <AttachmentRow v-model="model.waveImageUrl!" name="blogger_wave_image" label="资料卡波浪图" width="375px"
      aspect-ratio="375/50" />
  </div>
</template>
