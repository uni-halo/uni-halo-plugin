<script setup lang="ts">
import { inject } from "vue";
import { GeneralConfigFormKey } from "../form-context";

/**
 * 资源设置分区：
 * 加载占位（加载中/加载失败两图）。
 */
defineProps<{ subTab: string }>();

const { formState } = inject(GeneralConfigFormKey)!;
</script>

<template>
  <!-- 资源与兜底 → 加载占位 -->
  <template v-if="subTab === 'loading'">
    <FormKit v-model="formState.spec.assets.loadingGifUrl" name="assets_loading_gif" label="加载中的图片" type="attachment" :accepts="['image/*']" />
    <FormKit v-model="formState.spec.assets.loadingErrUrl" name="assets_loading_err" label="加载失败图片" type="attachment" :accepts="['image/*']" />
  </template>
</template>
