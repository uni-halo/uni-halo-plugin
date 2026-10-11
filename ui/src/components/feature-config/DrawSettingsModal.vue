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
const emit = defineEmits<{
  (event: "close"): void;
}>();

const queryClient = useQueryClient();

const { data: config, isLoading } = useQuery({
  queryKey: ["uni-halo:feature-config"],
  queryFn: () => featureConfigApi.get(),
});

/** 审核开关草稿（缺省 true，与旧 drawConfig 默认一致） */
const auditEnabled = ref(true);

watch(
  () => config.value,
  (value) => {
    if (!value) {
      return;
    }
    auditEnabled.value = value.spec?.pages?.draw?.auditEnabled ?? true;
  },
  { immediate: true }
);

const saving = ref(false);

const handleSave = async () => {
  saving.value = true;
  try {
    await featureConfigApi.save(<FeatureConfigPatch>{
      metadata: { name: "feature-config" },
      spec: { pages: { draw: { auditEnabled: auditEnabled.value } } },
    });
    Toast.success("保存成功");
    queryClient.invalidateQueries({ queryKey: ["uni-halo:feature-config"] });
    emit("close");
  } catch (error) {
    Toast.error((error as Error).message);
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <VModal title="绘画设置" :layer-closable="true" mount-to-body @close="emit('close')">
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
    </div>

    <template #footer>
      <div class=":uno: flex items-center gap-3">
        <SubmitButton type="secondary" :loading="saving" text="保存" @submit="handleSave" />
        <VButton @click="emit('close')">取消</VButton>
      </div>
    </template>
  </VModal>
</template>
