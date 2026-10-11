<script setup lang="ts">
import {
  IconRefreshLine,
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
import { Toast } from "@halo-dev/components";
import { useQuery } from "@tanstack/vue-query";
import { useDeletionFlow } from "@/composables/useDeletionFlow";
import { deletingRefetchInterval } from "@/utils/query";
import { computed, ref, watch } from "vue";
import RiEyeLine from "~icons/ri/eye-line";
import RiEyeOffLine from "~icons/ri/eye-off-line";
import RiSettings3Line from "~icons/ri/settings-3-line";
import DrawReviewModal from "@/components/draw-manage/DrawReviewModal.vue";
import DrawSettingsModal from "@/components/feature-config/DrawSettingsModal.vue";
import ImagePreviewModal from "@/components/common/ImagePreviewModal.vue";
import FilterDropdown from "@/components/common/FilterDropdown.vue";
import FilterCleanButton from "@/components/common/FilterCleanButton.vue";
import SortDropdowns from "@/components/common/SortDropdowns.vue";
import { drawsApi } from "@/api";
import { DRAW_STATUS_LABELS, DRAW_STATUS_OPTIONS } from "@/types";
import type { Draw } from "@/types";
import type { SortField } from "@/components/common/SortDropdowns.vue";

/** 排序维度（每个维度独立下拉，互斥单值；选项 value 对应后端 sort 参数）
 * 「默认」= 待审核优先 + 提交时间倒序（后端默认排序） */
const SORT_FIELDS: SortField[] = [
  { key: "createdAt", label: "提交时间" },
  { key: "reviewedAt", label: "审核时间" },
];

const { confirmDelete } = useDeletionFlow(["uni-halo:draws"]);

const page = ref(1);
const size = ref(20);
const keyword = ref("");
const status = ref<string>();
const sortBy = ref<string>();
const total = ref(0);

// 行内/批量审核弹窗
const reviewAction = ref<"approve" | "reject">("approve");
const reviewNames = ref<string[]>([]);
const reviewTitle = ref<string>();
const reviewVisible = ref(false);

const previewVisible = ref(false);
const previewUrl = ref("");

// 模块设置弹窗（发布审核开关）
const settingsVisible = ref(false);

const checkAll = ref(false);
const selectedNames = ref<string[]>([]);

const { data: draws, isLoading, isFetching, refetch } = useQuery({
  queryKey: ["uni-halo:draws", page, size, status, sortBy, keyword],
  queryFn: async () => {
    const result = await drawsApi.list({
      page: page.value,
      size: size.value,
      status: status.value,
      sort: sortBy.value,
      keyword: keyword.value || undefined,
    });
    total.value = result.total;
    return result;
  },
  // 删除中对象存在时每 1s 自动重取，直到对象消失（删除语义统一）
  refetchInterval: (data) => deletingRefetchInterval(data),
});

watch(
  () => [keyword.value, status.value, sortBy.value],
  () => {
    page.value = 1;
  }
);

/** 筛选/排序是否有激活条件（决定是否显示「清除」按钮） */
const hasFilters = computed(() => !!status.value || !!sortBy.value);

/** 清除操作：重置状态筛选与排序（keyword 搜索框独立保留） */
const handleClearFilters = () => {
  status.value = undefined;
  sortBy.value = undefined;
};

watch(
  () => selectedNames.value,
  (newValue) => {
    checkAll.value = newValue.length === (draws.value?.items.length || 0);
  }
);

const handleCheckAllChange = (e: Event) => {
  const { checked } = e.target as HTMLInputElement;
  if (checked) {
    selectedNames.value = draws.value?.items.map((draw) => draw.metadata.name) || [];
  } else {
    selectedNames.value = [];
  }
};

const checkSelection = (draw: Draw) => selectedNames.value.includes(draw.metadata.name);

/** 行内快速审核：打开确认弹窗（单条） */
const handleOpenReview = (action: "approve" | "reject", draw: Draw) => {
  reviewAction.value = action;
  reviewNames.value = [draw.metadata.name];
  reviewTitle.value = draw.spec.title;
  reviewVisible.value = true;
};

/** 切换显隐（仅已通过的画作；隐藏后公开列表/详情不展示） */
const handleToggleHidden = async (draw: Draw) => {
  const target = !draw.spec.hidden;
  try {
    await drawsApi.setHidden(draw.metadata.name, target);
    Toast.success(target ? "已隐藏" : "已公开展示");
    refetch();
  } catch (e) {
    Toast.error("操作失败，请重试");
    console.error("切换绘画显隐失败", e);
  }
};

/** 批量审核：打开确认弹窗（选中项） */
const handleBatchReview = (action: "approve" | "reject") => {
  if (!selectedNames.value.length) {
    return;
  }
  reviewAction.value = action;
  reviewNames.value = [...selectedNames.value];
  reviewTitle.value = undefined;
  reviewVisible.value = true;
};

const handleDelete = (draw: Draw) => {
  confirmDelete({
    title: "确定要删除这幅绘画吗？",
    description: "仅删除绘画记录，图片附件保留，该操作不可恢复。",
    names: [draw.metadata.name],
    doDelete: (name) => drawsApi.delete(name),
  });
};

const handleDeleteInBatch = () => {
  const names = [...selectedNames.value];
  confirmDelete({
    title: "确定要删除选中的绘画吗？",
    description: "仅删除绘画记录，图片附件保留，该操作不可恢复。",
    names,
    doDelete: (name) => drawsApi.delete(name),
    onSuccess: () => {
      selectedNames.value = [];
    },
  });
};

const onReviewDone = () => {
  reviewVisible.value = false;
  selectedNames.value = [];
  refetch();
};

const statusColorClass = computed(() => (draw: Draw) => {
  switch (draw.spec.status) {
    case "APPROVED":
      return "text-green-600";
    case "REJECTED":
      return "text-red-500";
    default:
      return "text-amber-500";
  }
});

const formatTime = (value?: string | null) => {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}`;
};
</script>

<template>
  <!--
    单根包裹：多根 fragment 中混挂 body Teleport 弹窗时，Vue 插入新节点
    可能触发 "Failed to execute 'insertBefore'"，导致弹窗内容挂载中断
  -->
  <div>
  <DrawReviewModal
    v-if="reviewVisible"
    :action="reviewAction"
    :names="reviewNames"
    :title="reviewTitle"
    @close="reviewVisible = false"
    @done="onReviewDone"
  />

  <VPageHeader title="UniHalo-绘画作品管理">
    <template #actions>
      <VSpace>
        <VButton @click="refetch">
          <template #icon>
            <IconRefreshLine class="w-4 h-4" :class="{ 'animate-spin text-gray-900': isFetching }" />
          </template>
          刷新
        </VButton>
        <VButton @click="settingsVisible = true">
          <template #icon>
            <RiSettings3Line class="w-4 h-4" />
          </template>
          设置
        </VButton>
      </VSpace>
    </template>
  </VPageHeader>

  <div class=":uno: m-0 flex flex-col gap-4 md:m-4">
    <VCard :body-class="[':uno: !p-0']">
      <template #header>
        <div class=":uno: block w-full bg-gray-50 px-4 py-3">
          <div class=":uno: relative flex flex-col flex-wrap items-start gap-4 sm:flex-row sm:items-center">
            <div class=":uno: hidden items-center sm:flex">
              <input v-model="checkAll" type="checkbox" @change="handleCheckAllChange" />
            </div>
            <div class=":uno: flex w-full flex-1 items-center sm:w-auto">
              <template v-if="!selectedNames.length">
                <SearchInput v-model="keyword" placeholder="标题/用户名（回车搜索）" />
              </template>
              <VSpace v-else spacing="sm">
                <VButton size="sm" type="primary" @click="handleBatchReview('approve')">
                  通过
                </VButton>
                <VButton size="sm" type="danger" @click="handleBatchReview('reject')">
                  拒绝
                </VButton>
                <VButton size="sm" type="danger" plain @click="handleDeleteInBatch">
                  删除
                </VButton>
              </VSpace>
            </div>
            <VSpace spacing="lg" class=":uno: flex-wrap">
              <FilterCleanButton v-if="hasFilters" @click="handleClearFilters" />
              <FilterDropdown v-model="status" label="状态" :items="DRAW_STATUS_OPTIONS" />
              <SortDropdowns v-model="sortBy" :fields="SORT_FIELDS" />
            </VSpace>
          </div>
        </div>
      </template>

      <VLoading v-if="isLoading" />

      <Transition v-else-if="!draws?.items.length" appear name="fade">
        <VEmpty message="暂无绘画作品，可引导用户在 App 端绘画并发布" title="还没有绘画作品" />
      </Transition>

      <Transition v-else appear name="fade">
        <VEntityContainer>
          <VEntity v-for="draw in draws?.items" :key="draw.metadata.name" :is-selected="checkSelection(draw)">
            <template #checkbox>
              <input v-model="selectedNames" :value="draw.metadata.name" name="draw-checkbox" type="checkbox" />
            </template>
            <template #start>
              <VEntityField v-if="draw.spec.cover" width="42px">
                <template #description>
                  <img
                    :src="draw.spec.cover"
                    :alt="draw.spec.title || '画作封面'"
                    loading="lazy"
                    class=":uno: h-10 w-10 cursor-pointer rounded object-cover hover:opacity-80"
                    @click="() => { previewUrl = draw.spec.cover || ''; previewVisible = true; }"
                  />
                </template>
              </VEntityField>
              <VEntityField width="16rem">
                <template #title>
                  <span class=":uno: font-semibold text-gray-900">
                    {{ draw.spec.title || draw.metadata.name }}
                  </span>
                </template>
                <template #description>
                  <span class=":uno: text-xs text-gray-500">
                    {{ draw.spec.description || "暂无简介" }}
                  </span>
                </template>
              </VEntityField>
            </template>
            <template #end>
              <VEntityField v-if="draw.metadata.deletionTimestamp">
                <template #description>
                  <VStatusDot v-tooltip="'删除中'" state="warning" text="删除中" />
                </template>
              </VEntityField>
              <VEntityField>
                <template #description>
                  <div class=":uno: flex items-center gap-1.5">
                    <img
                      v-if="draw.owner?.avatar"
                      :src="draw.owner.avatar"
                      :alt="draw.owner.displayName || '作者头像'"
                      class=":uno: h-5 w-5 flex-shrink-0 rounded-full object-cover"
                    />
                    <span v-else class=":uno: h-5 w-5 flex-shrink-0 rounded-full bg-gray-100" />
                    <span class=":uno: truncate text-xs text-gray-600" style="max-width: 8rem">
                      {{ draw.owner?.displayName || draw.owner?.username || "未知作者" }}
                    </span>
                  </div>
                </template>
              </VEntityField>
              <VEntityField>
                <template #description>
                  <span class=":uno: text-xs font-medium" :class="statusColorClass(draw)">
                    {{ DRAW_STATUS_LABELS[draw.spec.status || "PENDING"] }}
                  </span>
                </template>
              </VEntityField>
              <VEntityField v-if="draw.spec.status === 'APPROVED'">
                <template #description>
                  <button
                    v-tooltip="draw.spec.hidden ? '恢复公开展示' : '隐藏（公开列表/详情不展示）'"
                    type="button"
                    class=":uno: flex cursor-pointer items-center border-none bg-transparent p-0"
                    @click="handleToggleHidden(draw)"
                  >
                    <RiEyeOffLine v-if="draw.spec.hidden" class=":uno: h-4 w-4 text-orange-400" />
                    <RiEyeLine v-else class=":uno: h-4 w-4 text-gray-400" />
                  </button>
                </template>
              </VEntityField>
              <VEntityField>
                <template #description>
                  <div class=":uno: flex flex-col gap-0.5 text-xs text-gray-500">
                    <span>
                      提交时间：{{ formatTime(draw.metadata.creationTimestamp) }}
                    </span>
                    <span v-if="draw.spec.reviewedAt">
                      审核时间：{{ formatTime(draw.spec.reviewedAt) }}
                    </span>
                  </div>
                </template>
              </VEntityField>
              <VEntityField v-if="(draw.spec.status || 'PENDING') === 'PENDING'">
                <template #description>
                  <VSpace spacing="sm">
                    <VButton size="sm" type="primary" @click="handleOpenReview('approve', draw)">
                      通过
                    </VButton>
                    <VButton size="sm" type="danger" plain @click="handleOpenReview('reject', draw)">
                      拒绝
                    </VButton>
                  </VSpace>
                </template>
              </VEntityField>
              <VEntityField v-else-if="draw.spec.status === 'REJECTED' && draw.spec.reason">
                <template #description>
                  <span class=":uno: truncate text-xs text-gray-400" style="max-width: 12rem">
                    拒绝原因：{{ draw.spec.reason }}
                  </span>
                </template>
              </VEntityField>
            </template>
            <template #dropdownItems>
              <VDropdownItem @click="handleOpenReview('approve', draw)">
                通过
              </VDropdownItem>
              <VDropdownItem @click="handleOpenReview('reject', draw)">
                拒绝
              </VDropdownItem>
              <VDropdownItem v-if="draw.spec.status === 'APPROVED'" @click="handleToggleHidden(draw)">
                {{ draw.spec.hidden ? "恢复公开展示" : "隐藏" }}
              </VDropdownItem>
              <VDropdownItem type="danger" @click="handleDelete(draw)">
                删除
              </VDropdownItem>
            </template>
          </VEntity>
        </VEntityContainer>
      </Transition>

      <template #footer>
        <VPagination
          v-model:page="page"
          v-model:size="size"
          page-label="页"
          size-label="条 / 页"
          :total-label="`共 ${total} 项数据`"
          :total="total"
          :size-options="[20, 30, 50, 100]"
        />
      </template>
    </VCard>
  </div>

  <ImagePreviewModal v-model:visible="previewVisible" :images="previewUrl ? [previewUrl] : []" title="画作预览" />

  <!-- 绘画设置弹窗（发布审核开关） -->
  <DrawSettingsModal v-if="settingsVisible" @close="settingsVisible = false" />
  </div>
</template>
