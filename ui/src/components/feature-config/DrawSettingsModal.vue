<script setup lang="ts">
import { Toast, VButton, VLoading, VModal, VSwitch } from "@halo-dev/components";
import { useQuery, useQueryClient } from "@tanstack/vue-query";
import { ref, watch } from "vue";
import SubmitButton from "@/components/button/SubmitButton.vue";
import { featureConfigApi } from "@/api";
import type { FeatureConfigPatch } from "@/types";

/**
 * 绘画设置弹窗（绘画作品管理页 ⚙ 入口）：
 * 发布审核开关（spec.pages.draw.auditEnabled）。仅保存 spec.pages.draw（后端 merge 不动其它分区）。
 */
const props = withDefaults(defineProps<{ visible?: boolean }>(), { visible: false });

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
}>();

const queryClient = useQueryClient();

const { data: config, isLoading, refetch } = useQuery({
  queryKey: ["uni-halo:feature-config"],
  queryFn: () => featureConfigApi.get(),
});

/** 审核开关草稿（缺省 true，与旧 drawConfig 默认一致） */
const auditEnabled = ref(true);

/** 猜画配置草稿（缺省与后端默认一致） */
const guessEnabled = ref(true);
const guessRankTopN = ref(20);
const guessMinRevealWindowHours = ref(1);
const guessRevealPostponeHours = ref(24);

/** 用后端配置覆盖本地草稿（缺省值与后端默认保持一致） */
const syncDraft = () => {
  if (!config.value) {
    return;
  }
  const draw = config.value.spec?.pages?.draw;
  auditEnabled.value = draw?.auditEnabled ?? true;
  guessEnabled.value = draw?.guessEnabled ?? true;
  guessRankTopN.value = draw?.guessRankTopN ?? 20;
  guessMinRevealWindowHours.value = draw?.guessMinRevealWindowHours ?? 1;
  guessRevealPostponeHours.value = draw?.guessRevealPostponeHours ?? 24;
};

watch(() => config.value, () => syncDraft(), { immediate: true });

// 弹窗常驻（不卸载）：打开时重取配置并覆盖草稿，避免保留上次未保存的修改
watch(
  () => props.visible,
  async (visible) => {
    if (!visible) {
      return;
    }
    await refetch();
    syncDraft();
  }
);

const saving = ref(false);

const handleSave = async () => {
  saving.value = true;
  try {
    await featureConfigApi.save(<FeatureConfigPatch>{
      metadata: { name: "feature-config" },
      spec: {
        pages: {
          draw: {
            auditEnabled: auditEnabled.value,
            guessEnabled: guessEnabled.value,
            guessRankTopN: guessRankTopN.value,
            guessMinRevealWindowHours: guessMinRevealWindowHours.value,
            guessRevealPostponeHours: guessRevealPostponeHours.value,
          },
        },
      },
    });
    Toast.success("保存成功");
    queryClient.invalidateQueries({ queryKey: ["uni-halo:feature-config"] });
    emit("update:visible", false);
  } catch (error) {
    Toast.error((error as Error).message);
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <VModal
    :visible="visible"
    title="绘画设置"
    :layer-closable="true"
    @update:visible="emit('update:visible', $event)"
    @close="emit('update:visible', false)"
  >
    <div v-if="isLoading" class=":uno: py-2"><VLoading /></div>
    <div v-else>
      <p class=":uno: mb-3 text-xs text-gray-400">
        绘画发布流程配置：控制 App 端用户在画布创作页发布绘画后的审核流程。
      </p>
      <div class=":uno: flex items-center justify-between gap-4 border-b border-gray-100 py-3">
        <div>
          <div class=":uno: text-sm text-gray-700">发布绘画需要审核</div>
          <div class=":uno: mt-0.5 text-xs text-gray-400">
            开启后，App 端发布的绘画需管理员在「绘画作品」中审核通过后才公开展示；关闭后发布的绘画直接公开展示（不影响历史待审核数据）。
          </div>
        </div>
        <VSwitch v-model="auditEnabled" />
      </div>
      <div class=":uno: flex items-center justify-between gap-4 border-b border-gray-100 py-3">
        <div>
          <div class=":uno: text-sm text-gray-700">开启画谜玩法</div>
          <div class=":uno: mt-0.5 text-xs text-gray-400">
            开启后，App 端发布绘画时可设置谜底供其他用户猜一猜，并开放画谜排行榜；关闭后隐藏画谜入口（历史数据保留）。
          </div>
        </div>
        <VSwitch v-model="guessEnabled" />
      </div>
      <div v-if="guessEnabled" class=":uno: flex flex-col gap-3 py-3">
        <FormKit
          v-model="guessRankTopN"
          type="number"
          name="guessRankTopN"
          label="排行榜条数"
          :min="5"
          :max="100"
        />
        <FormKit
          v-model="guessMinRevealWindowHours"
          type="number"
          name="guessMinRevealWindowHours"
            label="最短画谜窗口（小时）"
          help="画谜至少展示这么多小时才揭晓谜底；作者设置的揭晓时间早于该窗口时自动顺延"
          :min="0"
          :max="72"
        />
        <FormKit
          v-model="guessRevealPostponeHours"
          type="number"
          name="guessRevealPostponeHours"
          label="审核顺延时长（小时）"
          help="审核通过后，谜底再过这么多小时才揭晓"
          :min="1"
          :max="168"
        />
      </div>
    </div>

    <template #footer>
      <div class=":uno: flex items-center gap-3">
        <SubmitButton type="secondary" :loading="saving" text="保存" @submit="handleSave" />
        <VButton @click="emit('update:visible', false)">取消</VButton>
      </div>
    </template>
  </VModal>
</template>
