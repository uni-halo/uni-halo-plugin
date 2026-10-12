<script setup lang="ts">
import { VModal, VButton } from "@halo-dev/components";
import SubmissionDetailContent from "@/components/link-manage/SubmissionDetailContent.vue";
import type { MiniProgramLinkSubmission } from "@/types";

withDefaults(
  defineProps<{
    /** 申请详情（常驻挂载时可能为空，内部按空值渲染） */
    item?: MiniProgramLinkSubmission;
    visible?: boolean;
  }>(),
  { visible: false }
);

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
}>();
</script>

<template>
  <VModal
    :visible="visible"
    title="申请详情"
    :width="640"
    @update:visible="emit('update:visible', $event)"
    @close="emit('update:visible', false)"
  >
    <SubmissionDetailContent v-if="item" :item="item" />

    <template #footer>
      <VButton @click="emit('update:visible', false)">关闭</VButton>
    </template>
  </VModal>
</template>
