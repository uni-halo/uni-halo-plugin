<script setup lang="ts">
import { watch } from "vue";
import { useQuery } from "@tanstack/vue-query";
import { VModal, VButton, VLoading, VEmpty } from "@halo-dev/components";
import { drawsApi } from "@/api";

const props = withDefaults(defineProps<{ visible?: boolean }>(), { visible: false });

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
}>();

const { data: ranks, isLoading, refetch } = useQuery({
  queryKey: ["uni-halo:draw-guess-rank"],
  queryFn: () => drawsApi.guessRank(50),
});

// 弹窗常驻（不卸载），打开时重取，保证排行榜是最新数据
watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      refetch();
    }
  }
);

/** 名次徽标：前三名金/银/铜实底，其余灰色数字 */
const rankClass = (rank: number) => {
  switch (rank) {
    case 1:
      return ":uno: flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full bg-amber-400 text-xs font-medium text-white";
    case 2:
      return ":uno: flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full bg-gray-300 text-xs font-medium text-white";
    case 3:
      return ":uno: flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full bg-orange-400 text-xs font-medium text-white";
    default:
      return ":uno: flex h-6 w-6 flex-shrink-0 items-center justify-center text-xs text-gray-400";
  }
};
</script>

<template>
  <VModal
    :visible="visible"
    title="猜画排行榜"
    :width="480"
    @update:visible="emit('update:visible', $event)"
    @close="emit('update:visible', false)"
  >
    <p class=":uno: mb-3 text-xs text-gray-400">
      按累计猜中画作数排名，猜对一幅 +1 分。
    </p>

    <VLoading v-if="isLoading" />

    <VEmpty
      v-else-if="!ranks?.length"
      message="还没有用户猜中画作，暂无竞猜积分"
      title="暂无竞猜积分"
    />

    <div v-else class=":uno: space-y-1">
      <div
        v-for="item in ranks"
        :key="item.username"
        class=":uno: flex items-center gap-3 rounded-md px-2 py-1.5"
        :class="item.rank <= 3 ? 'bg-gray-50' : ''"
      >
        <span :class="rankClass(item.rank)">{{ item.rank }}</span>
        <img
          v-if="item.avatar"
          :src="item.avatar"
          :alt="item.displayName || '用户头像'"
          class=":uno: h-8 w-8 flex-shrink-0 rounded-full object-cover"
        />
        <span v-else class=":uno: h-8 w-8 flex-shrink-0 rounded-full bg-gray-100" />
        <span class=":uno: min-w-0 flex-1 truncate text-sm text-gray-800">
          {{ item.displayName || item.username }}
        </span>
        <span class=":uno: flex-shrink-0 text-sm text-gray-600">猜中 {{ item.score }} 幅</span>
      </div>
    </div>

    <template #footer>
      <VButton @click="emit('update:visible', false)">关闭</VButton>
    </template>
  </VModal>
</template>
