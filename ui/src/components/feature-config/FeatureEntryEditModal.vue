<script setup lang="ts">
import { Toast, VButton, VModal, VSpace, VSwitch } from "@halo-dev/components";
import { computed, ref, watch } from "vue";
import type { FeatureConfigNavIconStyle, FeatureConfigQuickNavigationItem } from "@/types";

/**
 * 功能入口全字段编辑弹窗。
 *
 * <p>首页快捷导航 / 我的页面两组功能入口共用。编辑对象为外部传入的条目快照，
 * 确认时通过 emit("confirm", draft) 写回，取消不落盘。key 仅新增时可编辑，
 * 编辑已有条目时锁定（列表去重与删除过滤依赖 key）。</p>
 */
const props = withDefaults(
  defineProps<{
    /** 编辑中的条目（null = 关闭态） */
    entry: FeatureConfigQuickNavigationItem | null;
    /** 新增模式：key 可编辑 */
    creating?: boolean;
  }>(),
  { creating: false }
);

const emit = defineEmits<{
  (event: "update:entry", value: FeatureConfigQuickNavigationItem | null): void;
  (event: "confirm", draft: FeatureConfigQuickNavigationItem): void;
}>();

/** 编辑草稿（深拷贝传入条目，确认前不污染原数据） */
const draft = ref<FeatureConfigQuickNavigationItem>({});

watch(
  () => props.entry,
  (value) => {
    draft.value = value ? JSON.parse(JSON.stringify(value)) : {};
    // 无 icons 的旧数据补默认风格（emoji-font），确保编辑表单可用
    if (value && !draft.value.icons?.length) {
      draft.value.icons = [{ key: "emoji-font", prefix: "uhemoji2-icon", iconName: draft.value.icon || "" }];
    }
    if (value && !draft.value.iconMode) {
      draft.value.iconMode = draft.value.icons?.[0]?.key;
    }
    // iconColor 缺省时取 color 的值
    if (value && !draft.value.iconColor) {
      draft.value.iconColor = draft.value.color;
    }
  },
  { immediate: true }
);

const icons = computed<FeatureConfigNavIconStyle[]>(
  () => draft.value.icons || []
);

/** icons 内 key 集合变化（重命名/删除/新增）时，iconMode 失配则回落第一项 */
watch(
  icons,
  (list) => {
    const mode = draft.value.iconMode;
    if (!mode || !list.some((i) => i.key && i.key === mode)) {
      draft.value.iconMode = list.find((i) => i.key)?.key;
    }
  },
  { deep: true }
);

/** 生成不与现有 icons 冲突的默认风格 key（冲突自动加序号） */
const nextDefaultIconKey = (): string => {
  const base = "emoji-font";
  const keys = new Set(icons.value.map((i) => i.key));
  if (!keys.has(base)) {
    return base;
  }
  let n = 2;
  while (keys.has(`${base}-${n}`)) {
    n += 1;
  }
  return `${base}-${n}`;
};

const addIcon = () => {
  draft.value.icons = [...icons.value, { key: nextDefaultIconKey(), prefix: "", iconName: "" }];
};

const removeIcon = (index: number) => {
  const next = icons.value.filter((_, i) => i !== index);
  draft.value.icons = next;
  if (draft.value.iconMode && !next.some((i) => i.key === draft.value.iconMode)) {
    draft.value.iconMode = next[0]?.key;
  }
};

/** FormKit type="color" 回显兜底 */
const toColorInput = (value?: string) => value || "#cccccc";

const onColor = (value: unknown) => {
  if (typeof value === "string") draft.value.color = value;
};

const onIconColor = (value: unknown) => {
  if (typeof value === "string") draft.value.iconColor = value;
};

const onBgColor = (value: unknown) => {
  if (typeof value === "string") draft.value.bgColor = value;
};

const handleConfirm = () => {
  // 校验：title 必填
  if (!draft.value.title?.trim()) {
    Toast.warning("请填写标题");
    return;
  }
  // 过滤全空风格行
  const cleaned = (draft.value.icons || []).filter(
    (i) => i.key?.trim() || i.prefix?.trim() || i.iconName?.trim()
  );
  // key 必填且唯一
  const keys = cleaned.map((i) => i.key?.trim() || "");
  if (cleaned.length && keys.some((k) => !k)) {
    Toast.warning("图标风格的 key 不能为空");
    return;
  }
  if (new Set(keys).size !== keys.length) {
    Toast.warning("图标风格的 key 不能重复");
    return;
  }
  draft.value.icons = cleaned;
  emit("confirm", JSON.parse(JSON.stringify(draft.value)));
  emit("update:entry", null);
};

const handleClose = () => {
  emit("update:entry", null);
};
</script>

<template>
  <VModal :title="creating ? '新增功能入口' : '编辑功能入口'" :width="640" @close="handleClose">
    <div class=":uno: flex flex-col gap-4">
      <!-- 基础信息 -->
      <FormKit
        v-model="draft.key"
        type="text"
        label="标识 key"
        :disabled="!creating"
        help="唯一标识，编辑模式下不可修改"
        validation="required"
      />
      <FormKit v-model="draft.title" type="text" label="标题" validation="required" />
      <FormKit v-model="draft.subTitle" type="text" label="副标题" />
      <FormKit v-model="draft.path" type="text" label="跳转路径" help="小程序页面路径，如 /pages-blog/contact/contact" />
      <div class=":uno: flex items-center gap-2">
        <span class=":uno: text-sm font-medium text-gray-700">显示入口</span>
        <VSwitch v-model="draft.visible" />
      </div>

      <!-- 配色 -->
      <div class=":uno: flex gap-4">
        <div class=":uno: flex flex-1 items-center gap-2">
          <span class=":uno: shrink-0 text-sm text-gray-600">图标颜色</span>
          <FormKit type="color" format="hex8" outer-class=":uno: flex-1 !py-0"
            :value="toColorInput(draft.iconColor || draft.color)" @input="onIconColor" />
        </div>
        <div class=":uno: flex flex-1 items-center gap-2">
          <span class=":uno: shrink-0 text-sm text-gray-600">文字颜色</span>
          <FormKit type="color" format="hex8" outer-class=":uno: flex-1 !py-0"
            :value="toColorInput(draft.color)" @input="onColor" />
        </div>
        <div class=":uno: flex flex-1 items-center gap-2">
          <span class=":uno: shrink-0 text-sm text-gray-600">背景颜色</span>
          <FormKit type="color" format="hex8" outer-class=":uno: flex-1 !py-0"
            :value="toColorInput(draft.bgColor)" @input="onBgColor" />
        </div>
      </div>

      <!-- 图标风格集合 -->
      <div class=":uno: flex flex-col gap-2">
        <div class=":uno: flex items-center justify-between">
          <div>
            <span class=":uno: text-sm font-medium text-gray-700">图标风格集合</span>
            <span class=":uno: ml-2 text-xs text-gray-400">（需要配合 UniHalo 端开发）</span>
          </div>
          <VButton size="sm" type="default" @click="addIcon">添加风格</VButton>
        </div>
        <div
          v-for="(style, index) in icons"
          :key="index"
          class=":uno: flex items-center gap-6 rounded-md border border-gray-100 p-2"
        >
          <span class=":uno: shrink-0 text-sm text-gray-600">风格 key</span>
          <FormKit v-model="style.key" type="text" outer-class=":uno: min-w-0 flex-1 !py-0" />
          <span class=":uno: ml-4 shrink-0 text-sm text-gray-600">前缀</span>
          <FormKit v-model="style.prefix" type="text" outer-class=":uno: min-w-0 flex-1 !py-0" />
          <span class=":uno: ml-4 shrink-0 text-sm text-gray-600">图标名</span>
          <FormKit v-model="style.iconName" type="text" outer-class=":uno: min-w-0 flex-1 !py-0" />
          <VButton size="sm" type="danger" plain @click="removeIcon(index)">删除</VButton>
        </div>
        <FormKit
          v-model="draft.iconMode"
          type="select"
          label="当前生效风格"
          :options="icons.map((i) => ({ label: i.key || '(未命名)', value: i.key }))"
          help="app 端按该风格渲染，缺省取第一项"
        />
        <!-- 实时预览 -->
        <div class=":uno: flex items-center gap-3 rounded-md bg-gray-50 p-3">
          <div
            class=":uno: flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-gray-200 text-xl"
            :style="{ backgroundColor: draft.bgColor, color: draft.iconColor || draft.color }"
          >
            <span
              class=":uno: h-4 w-4 rounded-full"
              :style="{ backgroundColor: draft.iconColor || draft.color }"
            />
          </div>
          <div class=":uno: min-w-0">
            <div class=":uno: text-sm font-medium" :style="{ color: draft.color }">
              {{ draft.title || "标题预览" }}
            </div>
            <div class=":uno: truncate text-xs text-gray-400">
              {{ draft.subTitle || "副标题预览" }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <VSpace>
        <VButton @click="handleClose">取消</VButton>
        <VButton type="secondary" @click="handleConfirm">确定</VButton>
      </VSpace>
    </template>
  </VModal>
</template>
