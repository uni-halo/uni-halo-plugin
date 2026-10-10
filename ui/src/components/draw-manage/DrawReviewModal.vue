<script setup lang="ts">
import { Toast, VButton, VModal, VSpace } from "@halo-dev/components";
import { submitForm } from "@formkit/core";
import { computed, ref } from "vue";
import SubmitButton from "@/components/button/SubmitButton.vue";
import { drawsApi } from "@/api";

const props = defineProps<{
  /** approve=通过 / reject=拒绝 */
  action: "approve" | "reject";
  /** 目标绘画 name */
  name: string;
  /** 画作标题（弹窗描述展示） */
  title: string;
}>();

const emit = defineEmits<{
  (event: "close"): void;
  (event: "done"): void;
}>();

const modal = ref<InstanceType<typeof VModal> | null>(null);
const reason = ref("");
const saving = ref(false);

const isReject = computed(() => props.action === "reject");
const modalTitle = computed(() => (isReject.value ? "拒绝绘画" : "通过绘画"));

const handleSubmit = () => {
  submitForm("draw-review-form");
};

const handleConfirm = async () => {
  try {
    saving.value = true;
    if (isReject.value) {
      await drawsApi.reject(props.name, reason.value.trim());
    } else {
      await drawsApi.approve(props.name, reason.value.trim() || undefined);
    }
    Toast.success(isReject.value ? "已拒绝" : "已通过");
    modal.value?.close();
    emit("done");
  } catch (error) {
    Toast.error((error as Error).message);
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <VModal ref="modal" :title="modalTitle" :width="480" @close="emit('close')">
    <FormKit
      id="draw-review-form"
      type="form"
      name="draw-review-form"
      :config="{ validationVisibility: 'submit' }"
      @submit="handleConfirm"
    >
      <div class=":uno: mb-4 text-sm text-gray-600">
        画作「{{ title }}」
        {{ isReject ? "拒绝后不会公开展示，需填写拒绝原因。" : "通过后将在站点公开展示，备注选填。" }}
      </div>
      <FormKit
        v-model="reason"
        name="reason"
        :label="isReject ? '拒绝原因' : '审核备注'"
        type="textarea"
        :validation="isReject ? 'required' : undefined"
        :validation-messages="{ required: '拒绝原因不能为空' }"
        :placeholder="isReject ? '请填写拒绝原因（必填）' : '备注（可选）'"
        rows="3"
      />
    </FormKit>

    <template #footer>
      <VSpace>
        <SubmitButton
          :loading="saving"
          :disabled="saving"
          :type="isReject ? 'danger' : 'primary'"
          :text="modalTitle"
          @submit="handleSubmit"
        />
        <VButton @click="modal?.close()">取消</VButton>
      </VSpace>
    </template>
  </VModal>
</template>
