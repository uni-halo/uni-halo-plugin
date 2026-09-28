<script setup lang="ts">
import { Dialog, VButton, VEmpty, VModal, VSpace } from "@halo-dev/components";
import { computed, onMounted, ref } from "vue";
import FeatureEntryEditModal from "@/components/feature-config/FeatureEntryEditModal.vue";
import {
  FEATURE_ENTRY_REGISTRY,
  type FeatureEntry,
} from "@/constant/feature-entries";
import RiDeleteBinLine from "~icons/ri/delete-bin-6-line";
import RiEdit2Line from "~icons/ri/edit-2-line";
import type { FeatureConfigQuickNavigationItem } from "@/types";

/**
 * 功能入口候选弹窗。
 *
 * <p>数据源 = 内置静态注册表 {@link FEATURE_ENTRY_REGISTRY}（只读） +
 * 自定义条目库（props.customEntries，可新增/编辑/删除，由父组件持久化到
 * spec.pages.customEntries）。语义为「追加选择」：已配置条目（selectedKeys）
 * 置灰禁选，确认后返回新选中条目，由父组件追加到目标列表。</p>
 */
const props = withDefaults(
  defineProps<{
    /** 已配置条目的 key 列表（置灰禁用，不可再选） */
    selectedKeys?: string[];
    /** 自定义条目库（内置注册表之外的候选，可维护） */
    customEntries?: FeatureConfigQuickNavigationItem[];
  }>(),
  { selectedKeys: () => [], customEntries: () => [] }
);

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
  (event: "confirm", selected: FeatureEntry[]): void;
  (event: "create", entry: FeatureConfigQuickNavigationItem): void;
  (event: "update", entry: FeatureConfigQuickNavigationItem): void;
  (event: "delete", entry: FeatureConfigQuickNavigationItem): void;
}>();

const keyword = ref("");
const localSelected = ref<FeatureEntry[]>([]);

// ===== 自定义条目维护（新增/编辑共用内部编辑弹窗）=====

const editingCustom = ref<FeatureConfigQuickNavigationItem | null>(null);
const creatingCustom = ref(false);

const openCreateCustom = () => {
  creatingCustom.value = true;
  editingCustom.value = { key: `custom-${Date.now()}`, visible: true };
};

const openEditCustom = (entry: FeatureConfigQuickNavigationItem) => {
  creatingCustom.value = false;
  editingCustom.value = entry;
};

const handleCustomConfirm = (draft: FeatureConfigQuickNavigationItem) => {
  if (creatingCustom.value) {
    emit("create", draft);
  } else {
    emit("update", draft);
  }
  editingCustom.value = null;
};

const removeCustom = (entry: FeatureConfigQuickNavigationItem) => {
  Dialog.warning({
    title: "删除自定义条目",
    description: `确定删除「${entry.title || entry.key}」吗？已添加到功能列表的快照不受影响。`,
    confirmText: "删除",
    cancelText: "取消",
    onConfirm: () => {
      emit("delete", entry);
    },
  });
};

// 父组件 v-if 挂载，挂载即打开：重置搜索与已选
onMounted(() => {
  keyword.value = "";
  localSelected.value = [];
});

/** 统一候选清单 = 内置注册表（只读）+ 自定义条目；关键字过滤（key/title/subTitle 命中） */
const candidates = computed<FeatureEntry[]>(() => {
  const kw = keyword.value.trim().toLowerCase();
  const all: FeatureEntry[] = [
    ...FEATURE_ENTRY_REGISTRY,
    ...props.customEntries.map((entry) => ({ ...entry, group: "common" as const })),
  ];
  return all.filter(
    (entry) =>
      !kw ||
      entry.title?.toLowerCase().includes(kw) ||
      entry.subTitle?.toLowerCase().includes(kw) ||
      entry.key?.toLowerCase().includes(kw)
  );
});

const isCustom = (candidate: FeatureEntry) =>
  props.customEntries.some((entry) => entry.key === candidate.key);

/** 已在目标列表中的条目（置灰禁选） */
const isTaken = (candidate: FeatureEntry) =>
  props.selectedKeys.includes(candidate.key || "");

const isChecked = (candidate: FeatureEntry) =>
  localSelected.value.some((item) => item.key === candidate.key);

const toggleItem = (candidate: FeatureEntry) => {
  if (isTaken(candidate)) {
    return;
  }
  const index = localSelected.value.findIndex((item) => item.key === candidate.key);
  if (index >= 0) {
    localSelected.value.splice(index, 1);
    return;
  }
  localSelected.value.push({ ...candidate });
};

const handleConfirm = () => {
  emit("confirm", [...localSelected.value]);
  emit("update:visible", false);
};

const handleClose = () => {
  emit("update:visible", false);
};
</script>

<template>
  <VModal title="选择功能入口" :width="720" @close="handleClose">
    <div class=":uno: flex flex-col gap-3">
      <!-- 搜索 + 已选计数 + 新增自定义 -->
      <div class=":uno: flex items-center gap-3">
        <FormKit
          v-model="keyword"
          type="text"
          placeholder="输入关键字搜索功能入口…"
          outer-class=":uno: !pt-0 !pb-0"
        />
        <span class=":uno: shrink-0 text-sm text-gray-500">
          已选 <b class=":uno: text-primary">{{ localSelected.length }}</b> 条
        </span>
        <VButton size="sm" type="secondary" @click="openCreateCustom">新增自定义</VButton>
      </div>

      <!-- 候选列表（内置注册表只读 + 自定义条目可维护；已配置条目置灰禁选） -->
      <div class=":uno: max-h-96 overflow-y-auto rounded-md border border-gray-100">
        <div
          v-for="candidate in candidates"
          :key="candidate.key"
          class=":uno: flex cursor-pointer items-center gap-3 border-b border-gray-50 px-3 py-2.5 last:border-b-0"
          :class="[
            isTaken(candidate)
              ? ':uno: cursor-not-allowed bg-gray-50 opacity-50'
              : isChecked(candidate)
                ? ':uno: bg-blue-50'
                : ':uno: hover:bg-gray-50',
          ]"
          @click="toggleItem(candidate)"
        >
          <span
            class=":uno: flex h-4 w-4 shrink-0 items-center justify-center rounded border text-[11px] text-white"
            :class="
              isChecked(candidate) ? ':uno: border-primary bg-primary' : ':uno: border-gray-300'
            "
          >
            {{ isChecked(candidate) ? "✓" : "" }}
          </span>
          <span
            class=":uno: flex h-8 w-8 shrink-0 items-center justify-center rounded-md text-sm font-medium text-white"
            :style="{ backgroundColor: candidate.bgColor, color: candidate.color }"
          >
            {{ (candidate.title || "")[0] || "•" }}
          </span>
          <div class=":uno: min-w-0 flex-1">
            <div class=":uno: flex items-baseline gap-2">
              <span class=":uno: truncate text-sm font-medium text-gray-800" :style="{ color: candidate.color}">
                {{ candidate.title }}
              </span>
              <span v-if="candidate.subTitle" class=":uno: truncate text-xs text-gray-400">
                {{ candidate.subTitle }}
              </span>
            </div>
            <div class=":uno: mt-0.5 truncate text-xs text-gray-400">
              <span class=":uno: mr-1 font-mono text-gray-500">{{ candidate.key }}</span>
              {{ candidate.path || "about 页内入口" }}
            </div>
          </div>
          <span v-if="isTaken(candidate)" class=":uno: shrink-0 text-xs text-gray-400">
            已配置
          </span>
          <!-- 自定义条目维护操作 -->
          <template v-if="isCustom(candidate)">
            <button type="button" class=":uno: shrink-0 text-gray-400 transition-all hover:text-primary"
              title="编辑" @click.stop="openEditCustom(candidate)">
                <RiEdit2Line class=":uno: h-4 w-4" />
            </button>
            <button type="button" class=":uno: shrink-0 text-gray-400 transition-all hover:text-red-600"
              title="删除" @click.stop="removeCustom(candidate)">
               <RiDeleteBinLine class=":uno: h-4 w-4" />
            </button>
          </template>
        </div>
        <div v-if="!candidates.length" class=":uno: py-8">
          <VEmpty title="没有匹配的功能入口" message="换个关键字试试" />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="mb-2 text-xs text-orange-500"> 提示：所有的操作都需要通过页面右上角的 保存设置 按钮才会保存成功 </div>
      <VSpace>
        <VButton @click="emit('update:visible', false)">取消</VButton>
        <VButton type="secondary" @click="handleConfirm">
          确认选择（{{ localSelected.length }}）
        </VButton>
      </VSpace>
    </template>
  </VModal>

  <!-- 自定义条目新增/编辑弹窗（须置于 VModal 外：嵌套 VModal 会因 teleport 引发 insertBefore DOM 错误） -->
  <FeatureEntryEditModal v-if="editingCustom" :entry="editingCustom" :creating="creatingCustom"
    @update:entry="editingCustom = $event" @confirm="handleCustomConfirm" />
</template>
