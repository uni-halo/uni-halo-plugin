<script setup lang="ts">
import { Dialog, Toast, VButton, VModal } from "@halo-dev/components";
import { submitForm } from "@formkit/core";
import { cloneDeep } from "lodash-es";
import { computed, ref, watch } from "vue";
import SubmitButton from "@/components/button/SubmitButton.vue";
import RichTextEditorField from "@/components/common/RichTextEditorField.vue";
import MarkdownImportButton from "@/components/common/MarkdownImportButton.vue";
import { faqApi } from "@/api";
import {
  FAQ_LINK_TYPE_OPTIONS,
  type Faq,
  type FaqLink,
} from "@/types";

const props = withDefaults(
  defineProps<{
    item?: Faq;
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

const emptyForm = (): Faq => ({
  metadata: { name: "" },
  spec: {
    title: "",
    description: "",
    category: "",
    content: "",
    links: [],
    visible: true,
    priority: 0,
  },
});

const formState = ref<Faq>(emptyForm());

/** 重置表单：编辑模式回显条目，新建模式回到空表单 */
const resetForm = () => {
  if (props.item) {
    formState.value = cloneDeep(props.item);
    formState.value.spec.links = formState.value.spec.links || [];
    formState.value.spec.content = formState.value.spec.content || "";
    return;
  }
  formState.value = emptyForm();
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

/* ---------------- 相关链接编辑 ---------------- */

const addLink = () => {
  formState.value.spec.links = [
    ...(formState.value.spec.links || []),
    { title: "", type: "external", url: "" },
  ];
};

const removeLink = (index: number) => {
  formState.value.spec.links = (formState.value.spec.links || []).filter(
    (_, i) => i !== index
  );
};

/** 切换为客服按钮时清空 url（客服按钮无需地址） */
const handleLinkTypeChange = (link: FaqLink, value: string) => {
  link.type = value as FaqLink["type"];
  if (link.type === "contact") {
    link.url = "";
  }
};

const handleSubmit = () => {
  submitForm("faq-form");
};

/** 详细内容 Markdown 导入（内容非空时覆盖需二次确认） */
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
    description: "导入将覆盖当前已编辑的详细内容，确定继续吗？",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: apply,
  });
}

function handleContentMarkdownError(message: string) {
  Toast.error(`Markdown 解析失败：${message}`);
}

const handleSave = async () => {
  try {
    saving.value = true;
    // 过滤掉未填写标题的链接行
    formState.value.spec.links = (formState.value.spec.links || []).filter(
      (link) => link.title && link.title.trim()
    );
    if (isUpdateMode.value) {
      await faqApi.update(formState.value.metadata.name, formState.value);
    } else {
      await faqApi.create(formState.value);
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
    :title="isUpdateMode ? '编辑常见问题' : '新建常见问题'"
    :width="1080"
    @update:visible="emit('update:visible', $event)"
    @close="emit('update:visible', false)"
  >
    <FormKit
      id="faq-form"
      type="form"
      name="faq-form"
      :config="{ validationVisibility: 'submit' }"
      @submit="handleSave"
    >
      <div class=":uno: flex gap-4">
        <!-- 左侧基础表单 -->
        <div class=":uno: w-80 shrink-0 overflow-y-auto border-r border-gray-100 pr-4">
          <FormKit
            v-model="formState.spec.title"
            name="title"
            label="问题标题"
            type="text"
            validation="required"
            :validation-messages="{ required: '问题标题不能为空' }"
            placeholder="例如：如何绑定微信账号？"
          />
          <FormKit
            v-model="formState.spec.description"
            name="description"
            label="摘要描述"
            type="textarea"
            rows="3"
            help="折叠展开时显示的一句话摘要"
            placeholder="简要描述解决方案（必填，详情入口引导）"
            validation="required"
            :validation-messages="{ required: '摘要描述不能为空' }"
          />
          <FormKit
            v-model="formState.spec.category"
            name="category"
            label="分类"
            type="text"
            help="同一分类的问题在 app 端归为一组展示，如：账号相关"
            placeholder="例如：账号相关"
          />
          <FormKit
            v-model="formState.spec.visible"
            name="visible"
            label="显示状态"
            type="checkbox"
            :label-class="':uno: font-medium'"
            help="关闭后 app 端不展示，仅控制台可见"
          />
          <FormKit
            v-model="formState.spec.priority"
            name="priority"
            label="排序权重"
            type="number"
            help="数值越小越靠前"
          />
        </div>

        <!-- 右侧：富文本详细内容 + 相关链接 -->
        <div class=":uno: flex min-w-0 flex-1 flex-col gap-3">
          <div class=":uno: flex items-center gap-3">
            <div class=":uno: text-sm font-medium text-gray-700">
              详细内容
              <span class=":uno: text-xs font-normal text-gray-400">
                （点击「查看详情」后在弹窗中展示）
              </span>
            </div>
            <MarkdownImportButton @imported="handleContentMarkdownImported" @error="handleContentMarkdownError" />
            <span class=":uno: text-xs text-gray-400">导入将覆盖当前内容</span>
          </div>
          <!-- 富文本占满剩余空间，高度最小 320px -->
          <div class=":uno: min-h-[320px] flex-1">
            <RichTextEditorField v-model="formState.spec.content" />
          </div>

          <!-- 相关链接（详情弹窗底部按钮，可添加多条） -->
          <div class=":uno: border-t border-gray-100 pt-3">
            <div class=":uno: mb-2 flex items-center justify-between">
              <div class=":uno: text-sm font-medium text-gray-700">
                相关链接
                <span class=":uno: text-xs font-normal text-gray-400">
                  （展示为按钮；客服按钮无需填写地址，仅小程序端生效）
                </span>
              </div>
              <VButton size="sm" type="default" @click="addLink">
                + 添加链接
              </VButton>
            </div>
            <div class=":uno: flex flex-col gap-2">
              <div
                v-for="(link, index) in formState.spec.links"
                :key="index"
                class=":uno: flex items-start gap-2 rounded-lg bg-gray-50 p-2"
              >
                <FormKit
                  v-model="link.title"
                  :name="`link-title-${index}`"
                  type="text"
                  placeholder="按钮标题"
                  outer-class=":uno: flex-1 !py-0"
                />
                <FormKit
                  :model-value="link.type"
                  :name="`link-type-${index}`"
                  type="select"
                  :options="FAQ_LINK_TYPE_OPTIONS"
                  outer-class=":uno: w-36 !py-0"
                  @update:model-value="(value: string) => handleLinkTypeChange(link, value)"
                />
                <FormKit
                  v-if="link.type !== 'contact'"
                  v-model="link.url"
                  :name="`link-url-${index}`"
                  type="text"
                  :placeholder="link.type === 'internal' ? '/pages-blog/...' : 'https://...'"
                  outer-class=":uno: flex-1 !py-0"
                />
                <FormKit
                  v-else
                  value="无需填写地址，拉起小程序客服会话"
                  :name="`link-contact-${index}`"
                  type="text"
                  disabled
                  outer-class=":uno: flex-1 !py-0"
                />
                <VButton size="sm" type="danger" @click="removeLink(index)">
                  删除
                </VButton>
              </div>
              <div
                v-if="!formState.spec.links?.length"
                class=":uno: rounded-lg bg-gray-50 py-3 text-center text-xs text-gray-400"
              >
                暂无相关链接，点击右上角「添加链接」新增
              </div>
            </div>
          </div>
        </div>
      </div>
    </FormKit>

    <template #footer>
      <div class=":uno: flex items-center gap-3">
        <SubmitButton
          :loading="saving"
          :disabled="saving"
          type="secondary"
          text="提交"
          @submit="handleSubmit"
        />
        <VButton @click="emit('update:visible', false)">关闭</VButton>
      </div>
    </template>
  </VModal>
</template>
