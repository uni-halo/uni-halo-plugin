<script setup lang="ts">
import { Toast, VButton, VModal, VSpace } from "@halo-dev/components";
import { useQueryClient } from "@tanstack/vue-query";
import { computed, ref, watch } from "vue";
import SubmissionDetailContent from "@/components/link-manage/SubmissionDetailContent.vue";
import LinkGroupSelectField from "@/components/link-manage/LinkGroupSelectField.vue";
import { miniProgramLinkSubmissionsApi } from "@/api";
import type { MiniProgramLinkSubmission } from "@/types";

const props = withDefaults(
  defineProps<{
    /** 待审核申请（常驻挂载时可能为空，内部按空值渲染） */
    item?: MiniProgramLinkSubmission;
    visible?: boolean;
  }>(),
  { visible: false }
);

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
  (event: "done"): void;
}>();

const reason = ref("");
const groupName = ref(props.item?.spec?.groupName || "");
const saving = ref(false);

const queryClient = useQueryClient();

const isPending = computed(() => (props.item?.spec?.status || "PENDING") === "PENDING");

/** 重置审核表单：打开时清空上次的原因与分组选择 */
watch(
  () => props.visible,
  (visible) => {
    if (!visible) {
      return;
    }
    reason.value = "";
    groupName.value = props.item?.spec?.groupName || "";
  }
);

const handleApprove = async () => {
  try {
    saving.value = true;
    await miniProgramLinkSubmissionsApi.approve(
      props.item!.metadata.name,
      reason.value.trim() || undefined,
      groupName.value
    );
    Toast.success("已通过，链接已公开");
    finish();
  } catch (error) {
    Toast.error((error as Error).message);
  } finally {
    saving.value = false;
  }
};

const handleReject = async () => {
  if (!reason.value.trim()) {
    Toast.error("请填写拒绝原因");
    return;
  }
  try {
    saving.value = true;
    await miniProgramLinkSubmissionsApi.reject(
      props.item!.metadata.name,
      reason.value.trim(),
      groupName.value
    );
    Toast.success("已拒绝");
    finish();
  } catch (error) {
    Toast.error((error as Error).message);
  } finally {
    saving.value = false;
  }
};

const finish = () => {
  queryClient.invalidateQueries({ queryKey: ["uni-halo:mini-program-link-submissions"] });
  emit("update:visible", false);
  emit("done");
};
</script>

<template>
  <VModal
    :visible="visible"
    :title="isPending ? '审核申请' : '审核结果'"
    :width="640"
    @update:visible="emit('update:visible', $event)"
    @close="emit('update:visible', false)"
  >
    <div v-if="item">
      <!-- 申请详情（仅设计字段，纯展示组件避免 VModal 嵌套） -->
      <SubmissionDetailContent :item="item" />

      <!-- 审核操作区：仅待审核状态显示（分组选择 + FormKit textarea 审核说明） -->
      <div v-if="isPending" class=":uno: mt-4 border-t border-gray-100 pt-4">
        <LinkGroupSelectField v-model="groupName" help="选择一个分组" />
        <FormKit
          v-model="reason"
          name="reason"
          label="审核说明"
          type="textarea"
          rows="3"
          :placeholder="'填写审核说明（通过时可选；拒绝时必填）'"
        />
      </div>
    </div>

    <template #footer>
      <VSpace v-if="isPending">
        <VButton :loading="saving" :disabled="saving" type="danger" @click="handleReject">
          拒绝
        </VButton>
        <VButton :loading="saving" :disabled="saving" type="primary" @click="handleApprove">
          通过
        </VButton>
        <VButton @click="emit('update:visible', false)">关闭</VButton>
      </VSpace>
      <VButton v-else @click="emit('update:visible', false)">关闭</VButton>
    </template>
  </VModal>
</template>
