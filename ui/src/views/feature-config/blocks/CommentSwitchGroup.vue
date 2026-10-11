<script setup lang="ts">
import SwitchRow from "../fields/SwitchRow.vue";

/**
 * 评论开关组块：瞬间页 / 笔记详情页评论显隐（评论总开关跟随 Halo 系统设置，
 * 服务端注入 enableComment，此处无开关）。
 */
defineProps<{
  /** 顶部说明（缺失时用默认文案） */
  tip?: string;
}>();

const moment = defineModel<{ showCommentList?: boolean }>("moment", { required: true });
const postDetail = defineModel<{ showComment?: boolean; copyrightEnabled?: boolean }>("postDetail",
  { required: true });
</script>

<template>
  <p class=":uno: mb-3 text-xs text-gray-400">
    {{ tip || '评论开关：「开启评论」跟随 Halo 官方后台「系统 → 评论设置 → 启用评论」；「显示评论列表」仅控制对应页面评论列表区域的显隐。' }}
  </p>
  <SwitchRow v-model="moment.showCommentList!" title="瞬间页：显示评论列表"
    description="瞬间详情页是否展示评论列表区域" />
  <SwitchRow v-model="postDetail.showComment!" title="笔记详情：显示评论列表"
    description="笔记详情页是否展示评论相关功能" />
</template>
