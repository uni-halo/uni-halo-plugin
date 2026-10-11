<script setup lang="ts">
import { inject } from "vue";
import { FeatureConfigFormKey } from "../form-context";
import RichTextImportEditor from "../blocks/RichTextImportEditor.vue";

/**
 * 法律页面分区：用户协议 / 隐私政策 / 免责声明。
 * 内容支持手动编辑（默认）、从已发布笔记导入、上传 Markdown 文件解析。
 */
defineProps<{ subTab: string }>();

const { formState } = inject(FeatureConfigFormKey)!;
</script>

<template>
  <!-- 用户协议页 -->
  <template v-if="subTab === 'userAgreement'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      UniHalo端「用户协议」页面展示的内容。
    </p>
    <RichTextImportEditor v-model="formState.spec.pages.userAgreement!.content" name="legal_user_agreement"
      placeholder="输入用户协议内容，支持图文混排……留空时 app 端仅展示静态提示" />
  </template>

  <!-- 隐私政策页 -->
  <template v-if="subTab === 'privacyPolicy'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      UniHalo端「隐私政策」页面展示的内容。
    </p>
    <RichTextImportEditor v-model="formState.spec.pages.privacyPolicy!.content" name="legal_privacy_policy"
      placeholder="输入隐私政策内容，支持图文混排……留空时 app 端仅展示静态提示" />
  </template>

  <!-- 免责声明页 -->
  <template v-if="subTab === 'disclaimer'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      UniHalo端「免责声明」页面展示的内容。
    </p>
    <RichTextImportEditor v-model="formState.spec.pages.disclaimer!.content" name="legal_disclaimer"
      placeholder="输入免责声明内容，支持图文混排……留空则不展示免责声明页" />
  </template>
</template>
