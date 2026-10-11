<script setup lang="ts">
import RichTextEditorField from "@/components/common/RichTextEditorField.vue";

/**
 * 富文本行表单原语：标题 + RichTextEditorField 封装。
 * 零业务知识：只吃 v-model。
 */
defineProps<{
  /** 表单标题（富文本编辑器上方） */
  label?: string;
  /** 占位提示 */
  placeholder?: string;
}>();

const model = defineModel<string | undefined>();
</script>

<template>
  <div>
    <div v-if="label" class=":uno: mb-2 text-sm text-gray-700">{{ label }}</div>
    <RichTextEditorField v-model="model" :placeholder="placeholder" />
  </div>
</template>
