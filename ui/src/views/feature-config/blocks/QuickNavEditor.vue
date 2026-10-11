<script setup lang="ts">
import { Dialog, Toast, VButton, VEmpty, VSpace, VSwitch } from "@halo-dev/components";
import { computed, ref } from "vue";
import { VueDraggable } from "vue-draggable-plus";
import RiDragMove2Line from "~icons/ri/drag-move-2-line";
import RiDeleteBinLine from "~icons/ri/delete-bin-6-line";
import RiEdit2Line from "~icons/ri/edit-2-line";
import FeatureEntryCandidatesModal from "@/components/feature-config/FeatureEntryCandidatesModal.vue";
import FeatureEntryEditModal from "@/components/feature-config/FeatureEntryEditModal.vue";
import ColorFieldRow from "../fields/ColorFieldRow.vue";
import {
  featureEntriesByKeys,
  toQuickNavigationItem,
  type FeatureEntry,
} from "@/constant/feature-entries";
import type { FeatureConfigQuickNavigationItem } from "@/types";

/**
 * 快捷导航条目编辑器（唯一实现，首页快捷导航 / 博主页常用功能 / 其他功能复用）：
 * 拖拽排序（顺序即展示顺序）+ 名称/文字颜色/图标背景色行内编辑 +
 * 显隐开关 + 全字段编辑弹窗 + 候选弹窗添加 + 恢复默认。
 * 只认 QuickNavigationItem[]，不知道列表属于哪个页面。
 */
const props = defineProps<{
  /** 条目列表（v-model，父组件绑定 formState 内数组） */
  modelValue: FeatureConfigQuickNavigationItem[];
  /** 「恢复默认」使用的注册表 key 列表 */
  defaultKeys: string[];
  /** 列表名称（弹窗提示文案用，如「快捷导航」「常用功能」） */
  groupLabel: string;
  /** 自定义功能入口候选库（候选弹窗内新增/编辑/删除的持久化数据源） */
  customEntries?: FeatureConfigQuickNavigationItem[];
}>();

const emit = defineEmits<{
  "update:modelValue": [value: FeatureConfigQuickNavigationItem[]];
  /** 候选弹窗内新增自定义条目（转发给 formState 持有者落库） */
  "create-custom": [entry: FeatureConfigQuickNavigationItem];
  "update-custom": [entry: FeatureConfigQuickNavigationItem];
  "delete-custom": [entry: FeatureConfigQuickNavigationItem];
}>();

/** 列表代理（VueDraggable 需要非 undefined 数组；写回经 emit，不直接改 prop） */
const items = computed({
  get: () => props.modelValue || [],
  set: (value: FeatureConfigQuickNavigationItem[]) => emit("update:modelValue", value),
});

/** 恢复默认用的默认条目（由注册表 key 列表派生，全快照字段） */
const defaultItems = computed(() =>
  featureEntriesByKeys(props.defaultKeys).map(toQuickNavigationItem)
);

/** 候选弹窗（v-if 控制挂载） */
const candidatesVisible = ref(false);

/** 候选确认：注册表条目快照追加到列表尾部 */
const handleCandidatesConfirm = (selected: FeatureEntry[]) => {
  items.value = [...items.value, ...selected.map(toQuickNavigationItem)];
  candidatesVisible.value = false;
};

/** 删除某项 */
const removeItem = (item: FeatureConfigQuickNavigationItem) => {
  items.value = items.value.filter((it) => it !== item && it.key !== item.key);
};

/** 恢复默认（确认后整表恢复注册表默认配置，排序/visible 一并恢复） */
function restoreDefaults() {
  Dialog.warning({
    title: "恢复默认",
    description: `将「${props.groupLabel}」恢复为默认配置，确定继续吗？`,
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: () => {
      items.value = defaultItems.value.map((item) => ({ ...item }));
      Toast.success("已恢复默认");
    },
  });
}

// ===== 全字段编辑弹窗（编辑对象为列表内条目引用，确认后原位替换） =====

const editingEntry = ref<FeatureConfigQuickNavigationItem | null>(null);
const editingIndex = ref<number>();

const openEntryEdit = (index: number, item: FeatureConfigQuickNavigationItem) => {
  editingIndex.value = index;
  editingEntry.value = item;
};

const handleEntryEditConfirm = (draft: FeatureConfigQuickNavigationItem) => {
  const index = editingIndex.value;
  if (index != null && index < items.value.length) {
    const next = [...items.value];
    next[index] = { ...draft };
    items.value = next;
  }
  editingEntry.value = null;
  editingIndex.value = undefined;
};
</script>

<template>
  <div class=":uno: rounded-lg bg-gray-50 p-4">
    <div class=":uno: mb-2 flex items-center justify-between">
      <div class=":uno: text-sm font-medium text-gray-700">{{ groupLabel }}</div>
      <VSpace>
        <VButton size="sm" type="secondary" @click="candidatesVisible = true">添加</VButton>
        <VButton size="sm" type="secondary" @click="restoreDefaults">恢复默认</VButton>
      </VSpace>
    </div>
    <p class=":uno: mb-3 text-xs text-gray-400">拖拽排序，顺序即展示顺序。</p>

    <VueDraggable v-model="items" handle=".nav-drag-handle">
      <div v-for="(item, index) in items" :key="index" class=":uno: mb-2 last:mb-0">
        <div
          class=":uno: flex flex-wrap items-center gap-x-3 gap-y-2 rounded-lg border border-gray-100 bg-white px-3 py-2">
          <span class=":uno: nav-drag-handle cursor-move shrink-0 text-gray-400 hover:text-gray-600">
            <RiDragMove2Line class=":uno: h-4 w-4" />
          </span>
          <!-- 名称 -->
          <div class=":uno: flex min-w-0 flex-1 items-center gap-2">
            <span class=":uno: w-10 shrink-0 text-xs text-gray-700">名称</span>
            <FormKit v-model="item.title" :name="`nav_title_${index}`" type="text" placeholder="导航名称"
              outer-class=":uno: min-w-0 flex-1 !pt-0" />
          </div>
          <!-- 文字颜色 / 图标背景色 -->
          <ColorFieldRow v-model="item.color!" label="文字颜色" class=":uno: pr-24" />
          <ColorFieldRow v-model="item.bgColor!" label="图标背景色" class=":uno: pr-24" />
          <!-- 显示开关 -->
          <div class=":uno: flex shrink-0 items-center gap-2 text-xs text-gray-500">
            <span>显示</span>
            <VSwitch v-model="item.visible" />
          </div>
          <!-- 编辑 -->
          <button type="button" class=":uno: shrink-0 text-gray-400 transition-all hover:text-primary" title="编辑"
            @click="openEntryEdit(index, item)">
            <RiEdit2Line class=":uno: h-4 w-4" />
          </button>
          <!-- 删除 -->
          <button type="button" class=":uno: shrink-0 text-gray-400 transition-all hover:text-red-600" title="删除"
            @click="removeItem(item)">
            <RiDeleteBinLine class=":uno: h-4 w-4" />
          </button>
        </div>
      </div>
    </VueDraggable>
    <VEmpty v-if="!items.length" title="暂无条目" message="点击「添加」选择功能" :class="':uno: py-6'" />

    <!-- 候选弹窗：统一清单（内置注册表 + 自定义条目），已配置置灰禁选 -->
    <FeatureEntryCandidatesModal v-if="candidatesVisible"
      :selected-keys="items.map((i) => i.key || '')" :custom-entries="customEntries || []"
      @update:visible="candidatesVisible = false" @confirm="handleCandidatesConfirm"
      @create="(entry) => emit('create-custom', entry)"
      @update="(entry) => emit('update-custom', entry)"
      @delete="(entry) => emit('delete-custom', entry)" />

    <!-- 全字段编辑弹窗 -->
    <FeatureEntryEditModal v-if="editingEntry" :entry="editingEntry" @update:entry="editingEntry = $event"
      @confirm="handleEntryEditConfirm" />
  </div>
</template>
