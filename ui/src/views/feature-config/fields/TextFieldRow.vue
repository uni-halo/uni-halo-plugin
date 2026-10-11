<script setup lang="ts">
/**
 * 单行输入行表单原语：FormKit text 封装。
 * 零业务知识：只吃 v-model。
 */
defineProps<{
  /** 表单 label */
  label?: string;
  /** 帮助文案 */
  help?: string;
  /** 占位提示 */
  placeholder?: string;
  /** FormKit name（同表单内需唯一） */
  name: string;
}>();

const model = defineModel<string>({ default: "" });
</script>

<template>
  <FormKit v-model="model" :name="name" :label="label" :help="help" :placeholder="placeholder" type="text" />
</template>
