<script setup lang="ts">
import { VSwitch } from "@halo-dev/components";

/**
 * 开关行表单原语：标题 + 说明 + VSwitch 的通用排版。
 * 零业务知识：只吃 v-model，不读配置、不 import 类型。
 */
defineProps<{
  /** 开关标题 */
  title: string;
  /** 说明文案（标题下方灰字） */
  description?: string;
}>();

const model = defineModel<boolean>({ default: false });
</script>

<template>
  <div class=":uno: flex items-center justify-between gap-4 border-b border-gray-100 py-3">
    <div>
      <div class=":uno: text-sm text-gray-700">{{ title }}</div>
      <div v-if="description" class=":uno: mt-0.5 text-xs text-gray-400">{{ description }}</div>
    </div>
    <VSwitch v-model="model" />
  </div>
</template>
