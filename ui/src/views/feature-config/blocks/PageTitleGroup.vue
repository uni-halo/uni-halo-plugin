<script setup lang="ts">
import { computed } from "vue";
import { VButton, VSpace } from "@halo-dev/components";
import RiArrowDownSLine from "~icons/ri/arrow-down-s-line";
import RiArrowUpSLine from "~icons/ri/arrow-up-s-line";

/**
 * 页面标题分组表块：分组标题 + 命名方案按钮 + 折叠 + 逐项标题输入。
 * 三分组（主导航/博客/管理）复用；搜索过滤由外部 keyword 驱动。
 */
export interface PageTitleItem {
  key: string;
  label: string;
}

export interface PageTitleNamingVersion {
  key: string;
  label: string;
  values: Record<string, string>;
}

const props = defineProps<{
  /** 分组定义（展示顺序即数组顺序） */
  group: { key: string; label: string; desc: string; items: PageTitleItem[] };
  /** 标题键值对（外部 formState.spec.pages.titles 的引用，输入直接写回） */
  titles: Record<string, string | undefined>;
  /** 搜索关键词（过滤标题项，空 = 全部） */
  keyword?: string;
  /** 命名方案版本列表 */
  versions: PageTitleNamingVersion[];
}>();

/** 当前选中的命名版本（仅前端记忆，不落库） */
const activeVersion = defineModel<string>("activeVersion", { default: "original" });
/** 折叠状态 */
const collapsed = defineModel<boolean>("collapsed", { default: true });

/** 应用命名版本：仅填充版本中定义的键（未覆盖的键保持原值，避免误清空） */
const applyVersion = (version: PageTitleNamingVersion) => {
  props.group.items.forEach((item) => {
    if (item.key in version.values) {
      props.titles[item.key] = version.values[item.key] ?? "";
    }
  });
  activeVersion.value = version.key;
};

/** 搜索过滤后的标题项（label 或 key 命中关键词） */
const filteredItems = computed(() => {
  const kw = (props.keyword || "").trim().toLowerCase();
  if (!kw) {
    return props.group.items;
  }
  return props.group.items.filter(
    (item) => item.label.toLowerCase().includes(kw) || item.key.toLowerCase().includes(kw)
  );
});
</script>

<template>
  <div class=":uno: rounded-lg bg-gray-50 p-4">
    <div class=":uno: flex items-center justify-between gap-4 border-b border-gray-100 pb-3">
      <div>
        <div class=":uno: text-sm font-bold text-gray-700">{{ group.label }}</div>
        <div class=":uno: mt-0.5 text-xs text-gray-400">{{ group.desc }}</div>
      </div>
      <VSpace class=":uno: flex-wrap justify-end">
        <VButton v-for="version in versions" :key="version.key" size="sm"
          :type="activeVersion === version.key ? 'primary' : 'default'" class=":uno: !py-1 !px-2.5 rounded-full"
          @click="applyVersion(version)">
          {{ version.label }}
        </VButton>
        <VButton size="sm" type="secondary" plain class=":uno: !py-1.5 !px-3 rounded-full"
          :title="collapsed ? '展开配置' : '折叠配置'" @click="collapsed = !collapsed">
          <span class=":uno: flex items-center justify-center gap-x-1 my-auto">
            <span>{{ collapsed ? '展开配置' : '折叠配置' }} </span>
            <RiArrowDownSLine v-if="collapsed" class=":uno: text-base" />
            <RiArrowUpSLine v-else class=":uno: text-base" />
          </span>
        </VButton>
      </VSpace>
    </div>

    <div v-show="!collapsed" class=":uno: pt-1">
      <div v-for="item in filteredItems" :key="item.key"
        class=":uno: flex items-center justify-between gap-4 border-b border-gray-100 py-2 last:border-b-0">
        <div class=":uno: w-22 shrink-0 text-sm leading-8 text-gray-700">{{ item.label }}</div>
        <FormKit :model-value="titles[item.key]" :name="`titles_${group.key}_${item.key}`" type="text"
          :classes="{ outer: 'flex-1 min-w-0 !mb-0 !mt-0 !pt-0', input: '!py-1.5' }" placeholder="留空使用默认标题"
          @update:model-value="(value: unknown) => { titles[item.key] = typeof value === 'string' ? value : undefined }" />
      </div>
      <p v-if="!filteredItems.length" class=":uno: py-4 text-center text-xs text-gray-400">没有匹配的页面</p>
    </div>
  </div>
</template>
