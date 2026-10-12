<script setup lang="ts">
import { Sketch } from "@ckpack/vue-color";
import { Toast, VButton, VModal, VSpace } from "@halo-dev/components";
import { submitForm } from "@formkit/core";
import { cloneDeep } from "lodash-es";
import { computed, ref, watch } from "vue";
import SubmitButton from "@/components/button/SubmitButton.vue";
import { noticeTypeApi } from "@/api";
import type { NoticeType } from "@/types";

const props = withDefaults(
  defineProps<{
    visible?: boolean;
    item?: NoticeType;
  }>(),
  {
    visible: false,
    item: undefined,
  }
);

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
  /** 保存成功（供父组件刷新类型选项并自动选中） */
  (event: "saved", type: NoticeType): void;
}>();

const saving = ref(false);
const colorPickerVisible = ref(false);

const isUpdateMode = computed(() => !!props.item);

const emptyForm = (): NoticeType => ({
  metadata: { name: "" },
  spec: {
    displayName: "",
    color: "#10B981",
    priority: 0,
  },
});

const formState = ref<NoticeType>(emptyForm());

/** 打开弹窗时按传入条目回填草稿（新建时重置为空表单） */
const syncDraft = () => {
  if (props.item) {
    formState.value = cloneDeep(props.item);
    if (!formState.value.spec.color) {
      formState.value.spec.color = "#10B981";
    }
  } else {
    formState.value = emptyForm();
  }
  colorPickerVisible.value = false;
};

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      syncDraft();
    }
  }
);

const colorValue = computed(() => {
  const color = formState.value.spec.color;
  return color && /^#[0-9a-fA-F]{3,8}$/.test(color) ? color : "#cccccc";
});

// SketchPicker 双向绑定对象（内部含 hex/rgba/hsv）
const colorObj = ref<{ hex: string }>({ hex: colorValue.value });

watch(
  () => formState.value.spec.color,
  (value) => {
    const next = value && /^#[0-9a-fA-F]{3,8}$/.test(value) ? value : "#cccccc";
    if (colorObj.value.hex !== next) {
      colorObj.value = { hex: next };
    }
  }
);

const handleColorChange = (value: { hex: string }) => {
  formState.value.spec.color = value.hex;
};

/** 根据背景色亮度返回可读的文字颜色（深底白字 / 浅底深字） */
const textColorOn = (value: string) => {
  const hex = (value || "").replace(/^#/, "");
  const full =
    hex.length === 3
      ? hex
          .split("")
          .map((c) => c + c)
          .join("")
      : hex.slice(0, 6);
  if (!/^[0-9a-fA-F]{6}$/.test(full)) {
    return "#333333";
  }
  const r = parseInt(full.slice(0, 2), 16);
  const g = parseInt(full.slice(2, 4), 16);
  const b = parseInt(full.slice(4, 6), 16);
  return (r * 299 + g * 587 + b * 114) / 1000 > 150 ? "#333333" : "#ffffff";
};

const handleSubmit = () => {
  submitForm("notice-type-form");
};

const handleSave = async () => {
  try {
    saving.value = true;
    let saved: NoticeType;
    if (isUpdateMode.value) {
      saved = await noticeTypeApi.update(formState.value.metadata.name, formState.value);
    } else {
      saved = await noticeTypeApi.create(formState.value);
    }
    emit("update:visible", false);
    emit("saved", saved);
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
    :title="isUpdateMode ? '编辑公告类型' : '新建公告类型'"
    :width="480"
    @update:visible="emit('update:visible', $event)"
    @close="emit('update:visible', false)"
  >
    <FormKit
      id="notice-type-form"
      type="form"
      name="notice-type-form"
      :config="{ validationVisibility: 'submit' }"
      @submit="handleSave"
    >
      <FormKit
        v-model="formState.spec.displayName"
        name="displayName"
        label="类型名称"
        type="text"
        validation="required"
        :validation-messages="{ required: '类型名称不能为空' }"
        placeholder="例如：活动、维护、更新"
      />
      <FormKit
        v-model="formState.spec.color"
        name="color"
        label="标签颜色"
        type="text"
        help="支持直接输入颜色代码（如 #10B981），或点击「选择颜色」从色板选取"
        placeholder="#10B981"
      />
      <!-- 颜色预览 + 操作按钮独立一行 -->
      <div class=":uno: mb-4 flex items-center gap-2">
        <span
          class=":uno: inline-flex h-7 items-center rounded border border-gray-300 px-2 text-xs font-medium"
          :style="{ backgroundColor: colorValue, color: textColorOn(colorValue) }"
          title="当前颜色"
        >
          {{ formState.spec.color || "未设置" }}
        </span>
        <VButton
          type="secondary"
          size="sm"
          @click="colorPickerVisible = !colorPickerVisible"
        >
          选择颜色
        </VButton>
      </div>
      <!-- 色板在下方展开/收起（正常文档流，不遮挡） -->
      <div
        v-if="colorPickerVisible"
        class=":uno: mb-4 rounded-md border border-gray-200 bg-white p-3"
      >
        <Sketch
          :model-value="colorObj"
          @update:model-value="handleColorChange"
        />
      </div>
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
