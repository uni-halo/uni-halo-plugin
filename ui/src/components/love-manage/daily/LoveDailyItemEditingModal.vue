<script setup lang="ts">
import { Toast, VButton, VModal, VSpace } from "@halo-dev/components";
import { submitForm } from "@formkit/core";
import { cloneDeep } from "lodash-es";
import { computed, ref, watch } from "vue";
import SubmitButton from "../../button/SubmitButton.vue";
import { loveDailyApi } from "@/api";
import { LOVE_STATUS_OPTIONS, type LoveDailyItem } from "@/types";

const props = withDefaults(
  defineProps<{
    item?: LoveDailyItem;
    visible?: boolean;
  }>(),
  {
    item: undefined,
    visible: false,
  }
);

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
}>();

const saving = ref(false);

const isUpdateMode = computed(() => !!props.item);

const formState = ref<LoveDailyItem>({
  metadata: { name: "" },
  spec: {
    title: "",
    content: "",
    status: "wait",
    planDate: "",
    completeDate: "",
    completeRemark: "",
    images: [],
    priority: 0,
  },
});

/** 重置表单：编辑模式回显清单项，新建模式回到空表单 */
const resetForm = () => {
  formState.value = props.item
    ? { ...cloneDeep(props.item), spec: { ...cloneDeep(props.item.spec), images: props.item.spec.images || [] } }
    : {
        metadata: { name: "" },
        spec: {
          title: "",
          content: "",
          status: "wait",
          planDate: "",
          completeDate: "",
          completeRemark: "",
          images: [],
          priority: 0,
        },
      };
};

watch(() => props.item, () => resetForm(), { immediate: true });

// 弹窗常驻（不卸载）：打开时重置表单，避免保留上次未保存的内容
watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      resetForm();
    }
  }
);

// 状态改为"已完成"且未填完成时间时，自动填充当天日期
watch(
  () => formState.value.spec.status,
  (status) => {
    if (status === "complete" && !formState.value.spec.completeDate) {
      const today = new Date();
      const pad = (n: number) => String(n).padStart(2, "0");
      formState.value.spec.completeDate = `${today.getFullYear()}-${pad(
        today.getMonth() + 1
      )}-${pad(today.getDate())}`;
    }
  }
);

const handleSubmit = () => {
  submitForm("love-daily-form");
};

const handleSave = async () => {
  try {
    saving.value = true;
    if (isUpdateMode.value) {
      await loveDailyApi.update(formState.value.metadata.name, formState.value);
    } else {
      await loveDailyApi.create(formState.value);
    }
    emit("update:visible", false);
    Toast.success("保存成功");
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
    :title="isUpdateMode ? '编辑清单项' : '新建清单项'"
    :width="640"
    @update:visible="emit('update:visible', $event)"
    @close="emit('update:visible', false)"
  >
    <FormKit
      id="love-daily-form"
      type="form"
      name="love-daily-form"
      :config="{ validationVisibility: 'submit' }"
      @submit="handleSave"
    >
      <FormKit
        v-model="formState.spec.title"
        name="title"
        label="标题"
        type="text"
        validation="required"
        :validation-messages="{ required: '标题不能为空' }"
        placeholder="例如：一起去看海"
      />
      <FormKit
        v-model="formState.spec.content"
        name="content"
        label="内容"
        type="textarea"
        rows="3"
        placeholder="补充说明（可选）"
      />
      <FormKit
        v-model="formState.spec.status"
        name="status"
        label="状态"
        type="select"
        :options="LOVE_STATUS_OPTIONS"
      />
      <FormKit
        v-model="formState.spec.planDate"
        name="planDate"
        label="计划时间"
        type="date"
        help="计划什么时候完成这件事"
        placeholder="选择计划日期"
      />
      <FormKit
        v-if="formState.spec.status === 'complete'"
        v-model="formState.spec.completeDate"
        name="completeDate"
        label="完成时间"
        type="date"
        validation="required"
        :validation-messages="{ required: '已完成状态必须填写完成时间' }"
        placeholder="选择完成日期"
      />
      <FormKit
        v-if="formState.spec.status === 'complete'"
        v-model="formState.spec.completeRemark"
        name="completeRemark"
        label="完成感想"
        type="textarea"
        rows="3"
        help="记录完成这一刻的心情或回忆（可选）"
        placeholder="例如：这一天我们一起看了海，很幸福"
      />
      <FormKit
        v-model="formState.spec.images"
        name="images"
        label="图片"
        type="attachment"
        multiple
        :accepts="['image/*']"
        help="可添加多张图片，从附件库选择或直接输入图片地址"
      />
      <FormKit
        v-model="formState.spec.priority"
        name="priority"
        label="排序"
        type="number"
        help="数值越大越靠前"
      />
    </FormKit>

    <template #footer>
      <VSpace>
        <SubmitButton
          :loading="saving"
          :disabled="saving"
          type="secondary"
          text="提交"
          @submit="handleSubmit"
        />
        <VButton @click="emit('update:visible', false)">关闭</VButton>
      </VSpace>
    </template>
  </VModal>
</template>
