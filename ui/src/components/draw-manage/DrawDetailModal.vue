<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useQuery } from "@tanstack/vue-query";
import { VModal, VButton, VLoading } from "@halo-dev/components";
import ImagePreviewModal from "@/components/common/ImagePreviewModal.vue";
import { drawsApi } from "@/api";
import { DRAW_STATUS_LABELS } from "@/types";
import type { Draw } from "@/types";

const props = withDefaults(
  defineProps<{
    /** 当前画作（常驻挂载时可能为空，内部按空值渲染） */
    draw?: Draw;
    visible?: boolean;
  }>(),
  { visible: false }
);

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
}>();

const previewVisible = ref(false);
const previewUrl = ref("");

const spec = computed(() => props.draw?.spec ?? ({} as Draw["spec"]));

const isPuzzle = computed(() => !!spec.value.puzzleStatus);

// 画谜管理信息（谜底原文 + 竞猜记录），仅画谜作品且已选中画作时请求
const { data: puzzleInfo, isLoading: puzzleLoading, refetch: refetchPuzzle } = useQuery({
  queryKey: ["uni-halo:draw-puzzle", () => props.draw?.metadata.name],
  queryFn: () => drawsApi.puzzle(props.draw?.metadata.name || ""),
  enabled: () => !!props.draw && isPuzzle.value,
});

// 弹窗常驻（不卸载）：打开时重取画谜信息，保证谜底与竞猜记录是最新数据
watch(
  () => props.visible,
  (visible) => {
    if (visible && isPuzzle.value) {
      refetchPuzzle();
    }
  }
);

const statusLabel = computed(
  () => DRAW_STATUS_LABELS[spec.value.status || "PENDING"] || "未知"
);

const statusBadgeClass = computed(() => {
  switch (spec.value.status) {
    case "APPROVED":
      return "bg-green-50 text-green-600";
    case "REJECTED":
      return "bg-red-50 text-red-600";
    default:
      return "bg-amber-50 text-amber-600";
  }
});

const typeLabel = computed(() =>
  spec.value.puzzleStatus ? "画谜作品" : "普通作品"
);

const typeClass = computed(() =>
  spec.value.puzzleStatus
    ? ":uno: rounded px-1.5 py-0.5 text-xs text-white bg-orange-500"
    : ":uno: rounded px-1.5 py-0.5 text-xs text-white bg-gray-400"
);

/** 画谜状态（优先取画谜接口实时结果，已揭晓含惰性揭晓） */
const puzzleStatusLabel = computed(() =>
  (puzzleInfo.value?.puzzleStatus || spec.value.puzzleStatus) === "REVEALED"
    ? "已揭晓"
    : "画谜中"
);

/** 参与用户时间：猜中显示猜中时刻，未猜中显示最近作答时间 */
const recordTime = (record: { solvedAt?: string; answeredAt?: string }) =>
  record.solvedAt || record.answeredAt || "";

const preview = (url?: string) => {
  if (!url) {
    return;
  }
  previewUrl.value = url;
  previewVisible.value = true;
};

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
  <VModal
    :visible="visible"
    title="画作详情"
    :width="640"
    @update:visible="emit('update:visible', $event)"
    @close="emit('update:visible', false)"
  >
    <div v-if="draw" class=":uno: space-y-4">
      <!-- 画作图片 + 基础信息 -->
      <div class=":uno: flex gap-4">
        <img
          v-if="spec.cover"
          :src="spec.cover"
          :alt="spec.title || '画作图片'"
          class=":uno: h-24 w-24 cursor-pointer rounded-lg border border-solid border-gray-100 object-cover"
          @click="preview(spec.cover)"
        />
        <div class=":uno: min-w-0 flex-1">
          <div class=":uno: flex flex-wrap items-center gap-2">
            <span class=":uno: text-base font-semibold text-gray-900">
              {{ spec.title || draw?.metadata.name }}
            </span>
            <span class=":uno: rounded px-1.5 py-0.5 text-xs" :class="statusBadgeClass">
              {{ statusLabel }}
            </span>
            <span class=":uno: rounded px-1.5 py-0.5 text-xs" :class="typeClass">
              {{ typeLabel }}
            </span>
            <span
              v-if="spec.hidden"
              class=":uno: rounded bg-orange-50 px-1.5 py-0.5 text-xs text-orange-600"
            >
              已隐藏
            </span>
          </div>
          <div v-if="spec.description" class=":uno: mt-1 text-sm text-gray-600">
            <span class=":uno: text-gray-500">画作简介：</span>{{ spec.description }}
          </div>
          <div class=":uno: mt-1 text-sm text-gray-600">
            <span class=":uno: text-gray-500">提交时间：</span>
            <span class=":uno: text-gray-800">{{ formatTime(draw?.metadata.creationTimestamp) }}</span>
          </div>
          <div class=":uno: mt-1 flex items-center gap-1.5 text-sm text-gray-600">
            <img
              v-if="draw.owner?.avatar"
              :src="draw.owner.avatar"
              :alt="draw.owner.displayName || '作者头像'"
              class=":uno: h-5 w-5 flex-shrink-0 rounded-full object-cover"
            />
            <span v-else class=":uno: h-5 w-5 flex-shrink-0 rounded-full bg-gray-100" />
            <span class=":uno: text-gray-500">作者：</span>
            {{ draw.owner?.displayName || draw.owner?.username || "未知作者" }}
            <span v-if="draw.owner?.username" class=":uno: text-xs text-gray-400">
              （{{ draw.owner.username }}）
            </span>
          </div>
        </div>
      </div>

      <!-- 画谜信息（仅画谜作品，浅灰卡片 + 橙色标题） -->
      <div
        v-if="isPuzzle"
        class=":uno: space-y-1 rounded-md border border-gray-100 bg-gray-50 p-3"
      >
        <div class=":uno: mb-3 text-xs text-orange-500">画谜信息</div>
        <template v-if="puzzleLoading">
          <VLoading />
        </template>
        <template v-else>
          <div class=":uno: text-sm">
            <span class=":uno: text-gray-500">竞猜状态：</span>
            <span class=":uno: text-gray-800">{{ puzzleStatusLabel }}</span>
          </div>
          <div v-if="puzzleInfo?.puzzleHint || spec.puzzleHint" class=":uno: text-sm">
            <span class=":uno: text-gray-500">谜底提示：</span>
            <span class=":uno: text-gray-800">{{ puzzleInfo?.puzzleHint || spec.puzzleHint }}</span>
          </div>
          <div class=":uno: text-sm">
            <span class=":uno: text-gray-500">画谜谜底：</span>
            <span class=":uno: text-gray-800">{{ puzzleInfo?.puzzleAnswer || spec.puzzleAnswer || "未设置" }}</span>
          </div>
          <div v-if="puzzleInfo?.autoRevealAt || spec.autoRevealAt" class=":uno: text-sm">
            <span class=":uno: text-gray-500">自动揭晓：</span>
            <span class=":uno: text-gray-800">
              {{ formatTime(puzzleInfo?.autoRevealAt || spec.autoRevealAt) }}
            </span>
          </div>
          <div v-if="puzzleInfo?.revealedAt || spec.revealedAt" class=":uno: text-sm">
            <span class=":uno: text-gray-500">揭晓时间：</span>
            <span class=":uno: text-gray-800">
              {{ formatTime(puzzleInfo?.revealedAt || spec.revealedAt) }}
            </span>
          </div>
        </template>
      </div>

      <!-- 参与用户（仅画谜作品，浅灰卡片 + 橙色标题） -->
      <div
        v-if="isPuzzle"
        class=":uno: space-y-2 rounded-md border border-gray-100 bg-gray-50 p-3"
      >
        <div class=":uno: text-xs text-orange-500">
          参与用户
          <span v-if="puzzleInfo" class=":uno: ml-1 text-gray-400">
            （参与 {{ puzzleInfo.guessCount }} 人 · 猜中 {{ puzzleInfo.solvedCount }} 人）
          </span>
        </div>
        <div v-if="puzzleLoading">
          <VLoading />
        </div>
        <template v-else-if="puzzleInfo?.records?.length">
          <div
            v-for="record in puzzleInfo.records"
            :key="record.username"
            class=":uno: flex items-center gap-2 text-sm"
          >
            <img
              v-if="record.avatar"
              :src="record.avatar"
              :alt="record.displayName || '用户头像'"
              class=":uno: h-6 w-6 flex-shrink-0 rounded-full object-cover"
            />
            <span v-else class=":uno: h-6 w-6 flex-shrink-0 rounded-full bg-gray-100" />
            <span class=":uno: min-w-0 flex-1 truncate text-gray-800">
              {{ record.displayName || record.username }}
            </span>
            <span
              v-if="record.correct"
              class=":uno: flex-shrink-0 rounded bg-green-50 px-1.5 py-0.5 text-xs text-green-600"
            >
              已猜中
            </span>
            <span class=":uno: flex-shrink-0 text-xs text-gray-400">
              {{ formatTime(recordTime(record)) }}
            </span>
          </div>
        </template>
        <div v-else class=":uno: text-sm text-gray-400">还没有用户参与竞猜</div>
      </div>

      <!-- 时间信息 -->
      <div v-if="spec.reviewedAt" class=":uno: text-sm">
        <span class=":uno: text-gray-500">审核时间：</span>
        <span class=":uno: text-gray-800">{{ formatTime(spec.reviewedAt) }}</span>
      </div>

      <!-- 审核信息区：已审核时显示 -->
      <div
        v-if="spec.status && spec.status !== 'PENDING'"
        class=":uno: space-y-1 rounded-md border border-gray-100 bg-gray-50 p-3"
      >
        <div class=":uno: mb-3 text-xs text-gray-500">审核结果</div>
        <div class=":uno: text-sm">
          <span class=":uno: text-gray-500">审核状态：</span>
          <span class=":uno: box-border rounded px-1.5 py-0.5 text-xs" :class="statusBadgeClass">
            {{ statusLabel }}
          </span>
        </div>
        <div v-if="spec.reviewedBy" class=":uno: text-sm">
          <span class=":uno: text-gray-500">审核人：</span>
          <span class=":uno: text-gray-800">{{ spec.reviewedBy }}</span>
        </div>
        <div v-if="spec.reason" class=":uno: text-sm">
          <span class=":uno: text-gray-500">{{ spec.status === "REJECTED" ? "拒绝原因：" : "审核说明：" }}</span>
          <span class=":uno: text-gray-800">{{ spec.reason }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <VButton @click="emit('update:visible', false)">关闭</VButton>
    </template>
  </VModal>

  <ImagePreviewModal
    v-model:visible="previewVisible"
    :images="previewUrl ? [previewUrl] : []"
    title="画作预览"
  />
</template>
