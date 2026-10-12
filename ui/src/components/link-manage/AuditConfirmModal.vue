<script setup lang="ts">
import { Toast, VButton, VModal, VSpace } from "@halo-dev/components";
import { submitForm } from "@formkit/core";
import { computed, ref, watch } from "vue";
import SubmitButton from "@/components/button/SubmitButton.vue";
import LinkGroupSelectField from "@/components/link-manage/LinkGroupSelectField.vue";
import { miniProgramLinkSubmissionsApi } from "@/api";

const props = withDefaults(
  defineProps<{
    visible?: boolean;
    /** approve=通过 / reject=拒绝 */
    action: "approve" | "reject";
    /** 目标申请 name 列表（单条或批量） */
    names: string[];
  }>(),
  {
    visible: false,
  }
);

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
  (event: "done"): void;
}>();

const reason = ref("");
const saving = ref(false);
/** 通过时归入的分组（空字符串=未分组） */
const groupName = ref("");

/** 打开弹窗时重置审核说明与归入分组 */
watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      reason.value = "";
      groupName.value = "";
    }
  }
);

const isReject = computed(() => props.action === "reject");
const isBatch = computed(() => props.names.length > 1);

const title = computed(() => (isReject.value ? "审核拒绝" : "审核通过"));

const description = computed(() => {
  const target = isBatch.value ? `选中的 ${props.names.length} 条申请` : "该条申请";
  return isReject.value
    ? `确定拒绝${target}吗？拒绝后不会生成链接，需填写拒绝原因。`
    : `确定通过${target}吗？通过后会自动生成链接并公开显示。`;
});

const handleSubmit = () => {
  submitForm("audit-confirm-form");
};

const handleConfirm = async () => {
  try {
    saving.value = true;
    // 通过时归入所选分组（空字符串=未分组）；拒绝不涉及分组
    const groupArg = isReject.value ? undefined : groupName.value;
    await Promise.all(
      props.names.map((name) =>
        isReject.value
          ? miniProgramLinkSubmissionsApi.reject(name, reason.value.trim())
          : miniProgramLinkSubmissionsApi.approve(
              name,
              reason.value.trim() || undefined,
              groupArg
            )
      )
    );
    Toast.success(isReject.value ? "已拒绝" : "已通过");
    emit("update:visible", false);
    emit("done");
  } catch (error) {
    Toast.error((error as Error).message);
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <VModal
    :visible="visible"
    :title="title"
    :width="480"
    @update:visible="emit('update:visible', $event)"
    @close="emit('update:visible', false)"
  >
    <FormKit
      id="audit-confirm-form"
      type="form"
      name="audit-confirm-form"
      :config="{ validationVisibility: 'submit' }"
      @submit="handleConfirm"
    >
      <div class=":uno: mb-4 text-sm text-gray-600">
        {{ description }}
      </div>
      <!-- 通过时可选分组（仅未分组+已有分组，默认未分组） -->
      <LinkGroupSelectField
        v-if="!isReject"
        v-model="groupName"
        help="选择通过后链接归入的分组"
      />
      <FormKit
        v-model="reason"
        name="reason"
        label="审核说明"
        type="textarea"
        :validation="isReject ? 'required' : undefined"
        :validation-messages="{ required: '拒绝原因不能为空' }"
        :placeholder="isReject ? '请填写拒绝原因（必填）' : '审核说明（可选）'"
        rows="3"
      />
    </FormKit>

    <template #footer>
      <VSpace>
        <SubmitButton
          :loading="saving"
          :disabled="saving"
          :type="isReject ? 'danger' : 'primary'"
          :text="title"
          @submit="handleSubmit"
        />
        <VButton @click="emit('update:visible', false)">取消</VButton>
      </VSpace>
    </template>
  </VModal>
</template>
