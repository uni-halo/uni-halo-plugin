<script setup lang="ts">
import {
  IconAddCircle,
  IconRefreshLine,
  Toast,
  VButton,
  VCard,
  VEmpty,
  VEntity,
  VEntityContainer,
  VEntityField,
  VLoading,
  VPageHeader,
  VPagination,
  VSpace,
  VStatusDot,
  VDropdownItem,
} from "@halo-dev/components";
import RiEyeLine from "~icons/ri/eye-line";
import RiEyeOffLine from "~icons/ri/eye-off-line";
import { useQuery, useQueryClient } from "@tanstack/vue-query";
import { useDeletionFlow } from "@/composables/useDeletionFlow";
import { deletingRefetchInterval } from "@/utils/query";
import { computed, ref, watch } from "vue";
import FaqEditingModal from "@/components/faq-manage/FaqEditingModal.vue";
import FilterDropdown from "@/components/common/FilterDropdown.vue";
import { faqApi } from "@/api";
import { FAQ_LINK_TYPE_LABELS, type Faq } from "@/types";

const queryClient = useQueryClient();
const { confirmDelete } = useDeletionFlow(["uni-halo:faqs"]);

const page = ref(1);
const size = ref(20);
const keyword = ref("");
const visible = ref<string>();
const total = ref(0);

/** 多选（批量删除） */
const selectedNames = ref<string[]>([]);
const checkAll = ref(false);

const editingModal = ref(false);
const selectedFaq = ref<Faq>();

const { data: faqs, isLoading, isFetching, refetch } = useQuery({
  queryKey: ["uni-halo:faqs", page, size, visible, keyword],
  queryFn: async () => {
    const result = await faqApi.list({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      visible: visible.value || undefined,
    });
    total.value = result.total;
    return result;
  },
  // 删除中对象存在时每 1s 自动重取，直到对象消失（删除语义统一）
  refetchInterval: (data) => deletingRefetchInterval(data),
});

/** 分类选项（从列表数据聚合，供筛选；数据量小一次取全） */
const { data: allFaqs } = useQuery({
  queryKey: ["uni-halo:faqs-all"],
  queryFn: async () => {
    const result = await faqApi.list({ page: 1, size: 0 });
    return result.items;
  },
});

const categoryOptions = computed(() => {
  const set = new Set<string>();
  allFaqs.value?.forEach((faq) => {
    if (faq.spec.category) {
      set.add(faq.spec.category);
    }
  });
  return [...set].map((c) => ({ label: c, value: c }));
});

/** 当前分类筛选（按分类名过滤，服务端 category 参数亦可，这里前端过滤保持简单） */
const activeCategory = ref("");

const filteredItems = computed(() => {
  const items = faqs.value?.items || [];
  if (!activeCategory.value) {
    return items;
  }
  return items.filter((faq) => faq.spec.category === activeCategory.value);
});

watch(
  () => [keyword.value, visible.value, activeCategory.value],
  () => {
    page.value = 1;
    selectedNames.value = [];
    checkAll.value = false;
  }
);

watch(
  () => selectedNames.value,
  (value) => {
    checkAll.value = value.length === filteredItems.value.length
      && filteredItems.value.length > 0;
  }
);

const handleCheckAllChange = (e: Event) => {
  const { checked } = e.target as HTMLInputElement;
  selectedNames.value = checked
    ? filteredItems.value.map((faq) => faq.metadata.name)
    : [];
};

const checkSelection = (faq: Faq) =>
  selectedNames.value.includes(faq.metadata.name);

const handleOpenEditingModal = (faq?: Faq) => {
  selectedFaq.value = faq;
  editingModal.value = true;
};

const handleDelete = (faq: Faq) => {
  confirmDelete({
    title: "确定要删除这个常见问题吗？",
    description: "删除后 app 端将不再展示，该操作不可恢复。",
    names: [faq.metadata.name],
    doDelete: (name) => faqApi.delete(name),
    onSuccess: () => {
      // 删除当前页最后一条后回退页码，避免停留在空页
      if (page.value > 1 && faqs.value?.items.length === 1) {
        page.value -= 1;
      }
    },
  });
};

const handleDeleteInBatch = () => {
  const names = [...selectedNames.value];
  confirmDelete({
    title: "确定要删除选中的常见问题吗？",
    description: "删除后 app 端将不再展示，该操作不可恢复。",
    names,
    doDelete: (name) => faqApi.delete(name),
    onSuccess: () => {
      // 删除后当前页可能为空，回退页码
      if (page.value > 1 && (faqs.value?.items.length || 0) <= names.length) {
        page.value -= 1;
      }
      selectedNames.value = [];
    },
  });
};

/** 显隐切换：整体更新仅翻转 spec.visible（undefined 视为显示） */
const handleToggleVisible = async (faq: Faq) => {
  try {
    await faqApi.update(faq.metadata.name, {
      metadata: faq.metadata,
      spec: { ...faq.spec, visible: faq.spec.visible === false },
    });
    refetch();
    queryClient.invalidateQueries({ queryKey: ["uni-halo:faqs-all"] });
  } catch (error) {
    Toast.error((error as Error).message);
  }
};

// 编辑弹窗常驻（不卸载）：关闭时释放选中条目并刷新列表
watch(editingModal, (visible) => {
  if (!visible) {
    selectedFaq.value = undefined;
    refetch();
    queryClient.invalidateQueries({ queryKey: ["uni-halo:faqs-all"] });
  }
});

/** 链接摘要（列表展示：按钮标题 x 数量） */
const linksSummary = (faq: Faq) => {
  const links = faq.spec.links || [];
  if (!links.length) {
    return "无链接";
  }
  return links
    .slice(0, 2)
    .map((link) => `${link.title || "未命名"}(${FAQ_LINK_TYPE_LABELS[link.type || ""] || link.type})`)
    .join("、") + (links.length > 2 ? ` 等 ${links.length} 条` : "");
};
</script>

<template>
  <FaqEditingModal v-model:visible="editingModal" :item="selectedFaq" />

  <VPageHeader title="UniHalo-问题管理">
    <template #actions>
      <VSpace>
        <VButton @click="refetch">
          <template #icon>
            <IconRefreshLine class="w-4 h-4" :class="{ 'animate-spin text-gray-900': isFetching }" />
          </template>
          刷新
        </VButton>
        <VButton v-permission="['plugin:uni-halo:faq:manage']" type="secondary" @click="handleOpenEditingModal()">
          <template #icon>
            <IconAddCircle class="w-5 h-5" />
          </template>
          新建问题
        </VButton>
      </VSpace>
    </template>
  </VPageHeader>

  <div class=":uno: m-0 flex flex-col gap-4 md:m-4">
    <div class=":uno: min-w-0 flex-1">
      <VCard :body-class="[':uno: !p-0']">
        <template #header>
          <div class=":uno: block w-full bg-gray-50 px-4 py-3">
            <div class=":uno: relative flex flex-col flex-wrap items-start gap-4 sm:flex-row sm:items-center">
              <div class=":uno: hidden items-center sm:flex">
                <input v-model="checkAll" type="checkbox" @change="handleCheckAllChange" />
              </div>
              <div class=":uno: flex w-full flex-1 items-center sm:w-auto">
                <template v-if="!selectedNames.length">
                  <SearchInput v-model="keyword" placeholder="问题标题/摘要/分类（回车搜索）" />
                </template>
                <VButton v-else size="sm" type="danger" @click="handleDeleteInBatch">
                  删除
                </VButton>
              </div>
              <VSpace spacing="lg" class=":uno: flex-wrap">
                <FilterDropdown v-model="visible" label="状态" :items="[
                  { label: '全部', value: '' },
                  { label: '显示', value: 'true' },
                  { label: '隐藏', value: 'false' },
                ]" @update:model-value="() => refetch()" />
                <FilterDropdown v-if="categoryOptions.length" v-model="activeCategory" label="分类" :items="[
                  { label: '全部', value: '' },
                  ...categoryOptions,
                ]" />
              </VSpace>
            </div>
          </div>
        </template>

        <VLoading v-if="isLoading" />

        <Transition v-else-if="!filteredItems.length" appear name="fade">
          <VEmpty message="维护常见问题，帮助 app 端用户快速解决疑问" title="还没有常见问题">
            <template #actions>
              <VButton v-permission="['plugin:uni-halo:faq:manage']" type="secondary" @click="handleOpenEditingModal()">
                <template #icon>
                  <IconAddCircle />
                </template>
                新建问题
              </VButton>
            </template>
          </VEmpty>
        </Transition>

        <Transition v-else appear name="fade">
          <VEntityContainer>
            <VEntity v-for="faq in filteredItems" :key="faq.metadata.name" :is-selected="checkSelection(faq)">
              <template #checkbox>
                <input v-model="selectedNames" :value="faq.metadata.name" name="faq-checkbox" type="checkbox" />
              </template>
              <template #start>
                <VEntityField :title="faq.spec.title || faq.metadata.name" width="16rem">
                  <template #description>
                    <span class=":uno: text-xs text-gray-500">
                      {{ faq.spec.category || "未分类" }}
                    </span>
                  </template>
                </VEntityField>
              </template>
              <template #end>
                <VEntityField v-if="faq.metadata.deletionTimestamp">
                  <template #description>
                    <VStatusDot v-tooltip="'删除中'" state="warning" text="删除中" />
                  </template>
                </VEntityField>
                <VEntityField>
                  <template #description>
                    <span class=":uno: truncate text-xs text-gray-500" style="max-width: 20rem">
                      {{ faq.spec.description || "暂无摘要" }}
                    </span>
                  </template>
                </VEntityField>
                <VEntityField>
                  <template #description>
                    <span class=":uno: truncate text-xs text-gray-400" style="max-width: 14rem">
                      {{ linksSummary(faq) }}
                    </span>
                  </template>
                </VEntityField>
                <VEntityField>
                  <template #description>
                    <button
                      class=":uno: rounded p-1 hover:bg-gray-100"
                      :title="faq.spec.visible === false ? '点击显示' : '点击隐藏'"
                      @click="handleToggleVisible(faq)"
                    >
                      <RiEyeOffLine v-if="faq.spec.visible === false" class=":uno: h-4 w-4 text-orange-400" />
                      <RiEyeLine v-else class=":uno: h-4 w-4 text-gray-400" />
                    </button>
                  </template>
                </VEntityField>
              </template>
              <template #dropdownItems>
                <VDropdownItem @click="handleOpenEditingModal(faq)">
                  编辑
                </VDropdownItem>
                <VDropdownItem type="danger" @click="handleDelete(faq)">
                  删除
                </VDropdownItem>
              </template>
            </VEntity>
          </VEntityContainer>
        </Transition>

        <template #footer>
          <VPagination v-model:page="page" v-model:size="size" page-label="页" size-label="条 / 页"
            :total-label="`共 ${total} 项数据`" :total="total" :size-options="[20, 30, 50, 100]" />
        </template>
      </VCard>
    </div>
  </div>
</template>
