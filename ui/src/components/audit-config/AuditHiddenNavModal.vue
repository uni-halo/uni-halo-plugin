<script setup lang="ts">
import { VButton, VEmpty, VModal, VSpace } from "@halo-dev/components";
import { computed, onMounted, ref } from "vue";
import { FEATURE_ENTRY_REGISTRY, type FeatureEntry } from "@/constant/feature-entries";
import type { FeatureConfigQuickNavigationItem } from "@/types";

/**
 * 审核模式隐藏功能入口选择弹窗。
 *
 * 候选 = 内置功能入口注册表（FEATURE_ENTRY_REGISTRY）+ 自定义功能入口
 * （props.customEntries，即 spec.pages.customEntries）+ 已选但候选缺失的
 * 孤儿 key（自定义条目删除后遗留，保留展示以便取消勾选）；
 * 语义为「勾选切换」：打开时回显已选 key，弹窗内可反复勾选/取消，
 * 确认后整体返回选中 key 列表，由父组件写入 spec.hiddenNavKeys。
 */
const props = withDefaults(
  defineProps<{
    /** 已隐藏入口的 key 列表（打开时回显） */
    selectedKeys?: string[];
    /** 自定义功能入口候选库（spec.pages.customEntries） */
    customEntries?: FeatureConfigQuickNavigationItem[];
  }>(),
  { selectedKeys: () => [], customEntries: () => [] }
);

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
  (event: "confirm", keys: string[]): void;
}>();

const localSelected = ref<string[]>([]);
const keyword = ref("");

// 父组件 v-if 挂载，挂载即打开：回显已选并重置搜索
onMounted(() => {
  localSelected.value = [...(props.selectedKeys || [])];
  keyword.value = "";
});

/** 统一候选清单 = 内置注册表 + 自定义条目 + 已选孤儿 key（标记「已移除」） */
const candidates = computed<FeatureEntry[]>(() => {
  const custom = props.customEntries.map((entry) => ({
    ...entry,
    group: "common" as const,
  }));
  const knownKeys = new Set([...FEATURE_ENTRY_REGISTRY, ...custom].map((entry) => entry.key));
  const orphans = localSelected.value
    .filter((key) => key && !knownKeys.has(key))
    .map((key) => ({ key, title: key, group: "common" as const, removed: true }));
  return [...FEATURE_ENTRY_REGISTRY, ...custom, ...orphans];
});

/** 关键字过滤（key/title/subTitle 命中） */
const filteredCandidates = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  if (!kw) {
    return candidates.value;
  }
  return candidates.value.filter(
    (entry) =>
      entry.title?.toLowerCase().includes(kw) ||
      entry.subTitle?.toLowerCase().includes(kw) ||
      entry.key?.toLowerCase().includes(kw)
  );
});

/** 自定义条目（非内置注册表、非孤儿 key） */
const isCustom = (entry: FeatureEntry) =>
  props.customEntries.some((item) => item.key === entry.key);

const isChecked = (entry: FeatureEntry) => localSelected.value.includes(entry.key || "");

const toggleItem = (entry: FeatureEntry) => {
  const key = entry.key || "";
  const index = localSelected.value.indexOf(key);
  if (index >= 0) {
    localSelected.value.splice(index, 1);
  } else {
    localSelected.value.push(key);
  }
};

const handleConfirm = () => {
  emit("confirm", [...localSelected.value]);
  emit("update:visible", false);
};
</script>

<template>
  <VModal title="选择要隐藏的功能入口" :width="560" @close="emit('update:visible', false)">
    <div class=":uno: flex flex-col gap-3">
      <div class=":uno: text-xs text-gray-400">
        选中的功能入口在审核模式开启期间不显示（首页快捷导航 / 我的页面），关闭审核模式后自动还原
      </div>

      <FormKit
        v-model="keyword"
        type="text"
        placeholder="输入关键字搜索功能入口…"
        outer-class=":uno: !pt-0 !pb-0"
      />

      <div class=":uno: max-h-96 overflow-y-auto rounded-md border border-gray-100">
        <div v-for="entry in filteredCandidates" :key="entry.key"
          class=":uno: flex cursor-pointer items-center gap-3 border-b border-gray-50 px-3 py-2.5 last:border-b-0"
          :class="isChecked(entry) ? ':uno: bg-blue-50' : ':uno: hover:bg-gray-50'"
          @click="toggleItem(entry)">
          <span
            class=":uno: flex h-4 w-4 shrink-0 items-center justify-center rounded border text-[11px] text-white"
            :class="isChecked(entry) ? ':uno: border-primary bg-primary' : ':uno: border-gray-300'">
            {{ isChecked(entry) ? "✓" : "" }}
          </span>
          <div class=":uno: min-w-0 flex-1">
            <div class=":uno: flex items-baseline gap-2">
              <span class=":uno: truncate text-sm font-medium text-gray-800">
                {{ entry.title }}
              </span>
              <span v-if="entry.subTitle" class=":uno: truncate text-xs text-gray-400">
                {{ entry.subTitle }}
              </span>
            </div>
          </div>
          <span v-if="isCustom(entry)" class=":uno: shrink-0 rounded bg-blue-50 px-1.5 py-0.5 text-xs text-blue-500">
            自定义
          </span>
          <span v-if="'removed' in entry && entry.removed" class=":uno: shrink-0 rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-400">
            已移除
          </span>
        </div>
        <div v-if="!filteredCandidates.length" class=":uno: py-8">
          <VEmpty title="没有匹配的功能入口" message="换个关键字试试" />
        </div>
      </div>
    </div>

    <template #footer>
      <VSpace>
        <VButton @click="emit('update:visible', false)">取消</VButton>
        <VButton type="secondary" @click="handleConfirm">
          确定（{{ localSelected.length }}）
        </VButton>
      </VSpace>
    </template>
  </VModal>
</template>
