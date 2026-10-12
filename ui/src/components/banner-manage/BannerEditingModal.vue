<script setup lang="ts">
import { Dialog, Toast, VButton, VModal } from "@halo-dev/components";
import { submitForm } from "@formkit/core";
import { cloneDeep } from "lodash-es";
import { computed, ref, watch } from "vue";
import SubmitButton from "@/components/button/SubmitButton.vue";
import RichTextEditorField from "@/components/common/RichTextEditorField.vue";
import MarkdownImportButton from "@/components/common/MarkdownImportButton.vue";
import { bannerApi } from "@/api";
import type { Banner } from "@/types";

const props = withDefaults(
  defineProps<{
    visible?: boolean;
    item?: Banner;
  }>(),
  {
    visible: false,
    item: undefined,
  }
);

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
}>();

const saving = ref(false);

const isUpdateMode = computed(() => !!props.item);

// 纯自定义弹窗：作者信息由服务端默认取当前登录用户，不显示/编辑
const emptyForm = (): Banner => ({
  metadata: { name: "" },
  spec: {
    title: "",
    cover: "",
    date: "",
    content: "",
    remark: "",
    link: "",
    priority: 0,
  },
});

const formState = ref<Banner>(emptyForm());

/** 打开弹窗时按传入条目回填草稿（新建时重置为空表单） */
const syncDraft = () => {
  if (props.item) {
    formState.value = cloneDeep(props.item);
    if (!formState.value.spec.content) {
      formState.value.spec.content = "";
    }
    if (!formState.value.spec.cover) {
      formState.value.spec.cover = "";
    }
  } else {
    formState.value = emptyForm();
  }
};

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      syncDraft();
    }
  }
);

const handleSubmit = () => {
  submitForm("banner-form");
};

const handleSave = async () => {
  try {
    saving.value = true;
    if (isUpdateMode.value) {
      await bannerApi.update(formState.value.metadata.name, formState.value);
    } else {
      await bannerApi.create(formState.value);
    }
    emit("update:visible", false);
    Toast.success("保存成功");
  } catch (error) {
    Toast.error((error as Error).message);
  } finally {
    saving.value = false;
  }
};

/** 内容详情 Markdown 导入（内容非空时覆盖需二次确认） */
function handleContentMarkdownImported(html: string, fileName: string) {
  const apply = () => {
    formState.value.spec.content = html;
    Toast.success(`已导入「${fileName}」`);
  };
  if (!formState.value.spec.content) {
    apply();
    return;
  }
  Dialog.warning({
    title: "覆盖当前内容",
    description: "导入将覆盖当前已编辑的内容详情，确定继续吗？",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: apply,
  });
}

function handleContentMarkdownError(message: string) {
  Toast.error(`Markdown 解析失败：${message}`);
}
</script>

<template>
  <VModal :visible="visible" :title="isUpdateMode ? '编辑自定义轮播图' : '新建自定义轮播图'" :width="880"
    @update:visible="emit('update:visible', $event)" @close="emit('update:visible', false)">
    <FormKit id="banner-form" type="form" name="banner-form" :config="{ validationVisibility: 'submit' }"
      @submit="handleSave">
      <!-- 左右布局：左侧基础表单（封面图占满列宽），右侧富文本内容 -->
      <div class=":uno: flex gap-4">
        <div class=":uno: w-80 shrink-0">
          <FormKit v-model="formState.spec.title" name="title" label="标题" type="text" validation="required"
            :validation-messages="{ required: '标题不能为空' }" placeholder="例如：UniHalo v3.x 发布啦" />
          <FormKit v-model="formState.spec.cover" name="cover" label="封面图" type="attachment" validation="required"
            :validation-messages="{ required: '封面图不能为空' }" width="100%" aspect-ratio="3/2" />
          <FormKit v-model="formState.spec.date" name="date" label="日期" type="date" help="留空则不展示" />
          <FormKit v-model="formState.spec.remark" name="remark" label="备注" type="textarea" rows="2"
            help="仅管理端可见，不对外展示" />
          <FormKit v-model="formState.spec.link" name="link" label="外链" type="text" help="点击弹窗中的跳转链接（可选）"
            placeholder="https://..." />
        </div>
        <div class=":uno: flex min-w-0 flex-1 flex-col gap-2">
          <div class=":uno: flex items-center gap-3">
            <div class=":uno: text-sm font-medium text-gray-700">内容详情</div>
            <MarkdownImportButton @imported="handleContentMarkdownImported" @error="handleContentMarkdownError" />
            <span class=":uno: text-xs text-gray-400">导入将覆盖当前内容</span>
          </div>
          <div class=":uno: min-h-[320px] flex-1">
            <RichTextEditorField v-model="formState.spec.content" />
          </div>
        </div>
      </div>
    </FormKit>

    <template #footer>
      <div class=":uno: flex items-center gap-3">
        <SubmitButton :loading="saving" :disabled="saving" type="secondary" text="提交" @submit="handleSubmit" />
        <VButton @click="emit('update:visible', false)">关闭</VButton>
      </div>
    </template>
  </VModal>
</template>
