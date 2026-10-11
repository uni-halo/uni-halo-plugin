<script setup lang="ts">
/**
 * 附件选择行表单原语：FormKit attachment 封装（图片类）。
 * 零业务知识：只吃 v-model；width / aspect-ratio 透传官方附件选择器属性。
 */
const props = withDefaults(
  defineProps<{
    /** 表单 label */
    label: string;
    /** 帮助文案 */
    help?: string;
    /** 占位提示 */
    placeholder?: string;
    /** FormKit name（同表单内需唯一） */
    name: string;
    /** FormKit 类名 */
    classes?: {
      outer?: string;
      input?: string;
    };
    /** 选择器宽度（透传官方附件组件，如 "100%" / "50%"） */
    width?: string;
    /** 预览图宽高比（透传官方附件组件，如 "16/9"） */
    aspectRatio?: string;
  }>(),
  { classes: () => ({}) }
);

const model = defineModel<string>({ default: "" });
</script>

<template>
  <FormKit v-model="model" :name="name" :label="label" :help="help" :placeholder="placeholder" type="attachment"
    :classes="props.classes" :accepts="['image/*']" :width="props.width" :aspect-ratio="props.aspectRatio" />
</template>
