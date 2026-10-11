<script setup lang="ts">
/**
 * 颜色选择行表单原语：FormKit color(hex8) 封装，含空值兜底与写回。
 * 零业务知识：只吃 v-model。
 */
const props = withDefaults(
  defineProps<{
    /** 颜色用途说明（如「标题颜色」，让用户知道改的是什么颜色） */
    label?: string;
  }>(),
  { label: "颜色" }
);

const model = defineModel<string>({ default: "" });

/** FormKit type="color" 回显值：空值兜底，rgba/hex 原样透传（保留透明度供滑块回显） */
function toColorInput(value?: string): string {
  return value || "#cccccc";
}

/** 选色（format="hex8" 输出 #rrggbbaa 含透明度）写回 */
function onInput(value: unknown) {
  if (typeof value === "string") {
    model.value = value;
  }
}
</script>

<template>
  <div class=":uno: flex shrink-0 items-center gap-2">
    <slot name="label">
      <span class=":uno: w-[4.75rem] shrink-0 text-xs text-gray-700">{{ props.label }}</span>
    </slot>
    <FormKit type="color" format="hex8" :model-value="toColorInput(model)" outer-class=":uno: w-12 shrink-0 !pt-0"
      @update:model-value="onInput" />
  </div>
</template>
