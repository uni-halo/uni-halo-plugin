<script setup lang="ts">
import { Dialog, IconAddCircle, Toast, VButton, VModal, VSpace } from "@halo-dev/components";
import { submitForm } from "@formkit/core";
import { useQuery, useQueryClient } from "@tanstack/vue-query";
import { cloneDeep } from "lodash-es";
import { computed, ref, watch } from "vue";
import SubmitButton from "@/components/button/SubmitButton.vue";
import RichTextEditorField from "@/components/common/RichTextEditorField.vue";
import MarkdownImportButton from "@/components/common/MarkdownImportButton.vue";
import NoticeTypeEditingModal from "@/components/notice-manage/NoticeTypeEditingModal.vue";
import { noticeApi, noticeTypeApi } from "@/api";
import { NOTICE_STATUS_OPTIONS, type Notice, type NoticeType } from "@/types";

const props = withDefaults(
  defineProps<{
    item?: Notice;
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
const typeModalVisible = ref(false);

const queryClient = useQueryClient();

const isUpdateMode = computed(() => !!props.item);

const formState = ref<Notice>({
  metadata: { name: "" },
  spec: {
    title: "",
    content: "",
    summary: "",
    cover: "",
    link: "",
    linkType: "none",
    typeName: "",
    status: "draft",
    priority: 0,
  },
});

/** 重置表单：编辑模式回显条目，新建模式回到空表单 */
const resetForm = () => {
  if (props.item) {
    formState.value = cloneDeep(props.item);
    if (!formState.value.spec.content) {
      formState.value.spec.content = "";
    }
    if (!formState.value.spec.cover) {
      formState.value.spec.cover = "";
    }
    // 历史数据兼容：linkType 为空且 link 非空时回退为 external
    if (!formState.value.spec.linkType) {
      formState.value.spec.linkType = formState.value.spec.link ? "external" : "none";
    }
    return;
  }
  formState.value = {
    metadata: { name: "" },
    spec: {
      title: "",
      content: "",
      summary: "",
      cover: "",
      link: "",
      linkType: "none",
      typeName: "",
      status: "draft",
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

// 公告类型选项（类型数量少，一次拉取足够）
const { data: typesData } = useQuery({
  queryKey: ["uni-halo:notice-types-options"],
  queryFn: async () => {
    const result = await noticeTypeApi.list({ page: 1, size: 100 });
    return result.items;
  },
});

const typeOptions = computed(() => {
  const options =
    typesData.value?.map((type) => ({
      label: type.spec.displayName || type.metadata.name,
      value: type.metadata.name,
    })) || [];
  // 编辑回显时若当前类型不在列表中（如已被删除），补一个占位选项避免空白
  const current = formState.value.spec.typeName;
  if (current && !options.some((option) => option.value === current)) {
    options.unshift({ label: current, value: current });
  }
  return options;
});

const handleSubmit = () => {
  submitForm("notice-form");
};

/** 新增类型保存成功后：刷新类型选项并自动选中 */
const handleTypeSaved = (type: NoticeType) => {
  typeModalVisible.value = false;
  formState.value.spec.typeName = type.metadata.name;
  queryClient.invalidateQueries({ queryKey: ["uni-halo:notice-types-options"] });
};

const handleSave = async () => {
  try {
    saving.value = true;
    if (isUpdateMode.value) {
      await noticeApi.update(formState.value.metadata.name, formState.value);
    } else {
      await noticeApi.create(formState.value);
    }
    emit("update:visible", false);
    Toast.success("保存成功");
  } catch (error) {
    Toast.error((error as Error).message);
  } finally {
    saving.value = false;
  }
};

/** 正文 Markdown 导入（内容非空时覆盖需二次确认） */
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
    description: "导入将覆盖当前已编辑的正文，确定继续吗？",
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
  <VModal
    :visible="visible"
    :title="isUpdateMode ? '编辑公告' : '新建公告'"
    :width="1080"
    @update:visible="emit('update:visible', $event)"
    @close="emit('update:visible', false)"
  >
    <!-- 左右布局：左侧基础表单（固定宽），右侧摘要 + 富文本正文（自适应） -->
    <FormKit
      id="notice-form"
      type="form"
      name="notice-form"
      :config="{ validationVisibility: 'submit' }"
      @submit="handleSave"
    >
      <div class=":uno: flex gap-4">
        <div class=":uno: w-80 shrink-0 overflow-y-auto border-r border-gray-100 pr-4">
          <FormKit
            v-model="formState.spec.title"
            name="title"
            label="标题"
            type="text"
            validation="required"
            :validation-messages="{ required: '标题不能为空' }"
            placeholder="例如：国庆假期安排通知"
          />
          <FormKit
            v-model="formState.spec.cover"
            name="cover"
            label="封面图"
            type="attachment"
            width="100%"
            aspect-ratio="3/2"
          />
          <FormKit
            v-model="formState.spec.linkType"
            name="linkType"
            label="链接类型"
            type="select"
            :options="[
              { label: '无', value: 'none' },
              { label: '外部链接', value: 'external' },
              { label: '小程序链接', value: 'miniapp' },
              { label: '内部页面链接', value: 'internal' },
              { label: '微信公众号文章', value: 'wechat-mp' },
            ]"
            help="平台兼容：App（安卓/iOS）不支持小程序链接；小程序不支持外部链接/公众号文章直接跳转（按资质回退为复制链接）"
          />
          <FormKit
            v-if="formState.spec.linkType === 'external' || formState.spec.linkType === 'wechat-mp' || formState.spec.linkType === 'internal'"
            v-model="formState.spec.link"
            name="link"
            :label="formState.spec.linkType === 'internal' ? '页面路径' : '链接地址'"
            type="text"
            :help="formState.spec.linkType === 'internal' ? 'app 内页面路径，如 /pages-blog/notice/notice' : '完整的跳转地址'"
            :placeholder="formState.spec.linkType === 'internal' ? '/pages-...' : 'https://...'"
          />
          <template v-if="formState.spec.linkType === 'miniapp'">
            <FormKit
              v-model="formState.spec.linkAppId"
              name="linkAppId"
              label="小程序 AppID"
              type="text"
              validation="required"
              :validation-messages="{ required: '小程序 AppID 不能为空' }"
              placeholder="wx..."
            />
            <FormKit
              v-model="formState.spec.linkPath"
              name="linkPath"
              label="小程序页面路径"
              type="text"
              help="留空则打开小程序首页"
              placeholder="/pages/index/index"
            />
          </template>
          <FormKit
            v-model="formState.spec.typeName"
            name="typeName"
            label="公告类型"
            type="select"
            :options="typeOptions"
            placeholder="请选择公告类型"
          />
          <!-- 新增公告类型：单独一行显示在类型选择下方 -->
          <div class=":uno: mb-4">
            <VButton
              type="secondary"
              size="sm"
              title="新增公告类型"
              @click="typeModalVisible = true"
            >
              <template #icon>
                <IconAddCircle />
              </template>
              新增公告类型
            </VButton>
          </div>
          <FormKit
            v-model="formState.spec.status"
            name="status"
            label="状态"
            type="select"
            :options="NOTICE_STATUS_OPTIONS"
          />
          <FormKit
            v-model="formState.spec.priority"
            name="priority"
            label="排序"
            type="number"
            help="数值越大越靠前"
          />
        </div>

        <div class=":uno: flex min-w-0 flex-1 flex-col gap-3">
          <FormKit
            v-model="formState.spec.summary"
            name="summary"
            label="摘要"
            type="textarea"
            rows="2"
            help="留空时自动从正文提取"
            placeholder="简要描述公告内容（可选）"
          />
          <div class=":uno: flex items-center gap-3">
            <div class=":uno: text-sm font-medium text-gray-700">正文</div>
            <MarkdownImportButton @imported="handleContentMarkdownImported" @error="handleContentMarkdownError" />
            <span class=":uno: text-xs text-gray-400">导入将覆盖当前内容</span>
          </div>
          <!-- 富文本占满剩余空间，高度最小 400px -->
          <div class=":uno: min-h-[400px] flex-1">
            <RichTextEditorField v-model="formState.spec.content" />
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
  <!-- 新增公告类型弹窗：与主弹窗平级挂载（两者 z-index 均为 2000，DOM 靠后者在上） -->
  <NoticeTypeEditingModal
    v-model:visible="typeModalVisible"
    @saved="handleTypeSaved"
  />
</template>
