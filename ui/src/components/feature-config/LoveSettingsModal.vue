<script setup lang="ts">
import { Toast, VButton, VLoading, VModal, VTabbar } from "@halo-dev/components";
import { useQuery, useQueryClient } from "@tanstack/vue-query";
import { cloneDeep } from "lodash-es";
import { ref, watch } from "vue";
import SubmitButton from "@/components/button/SubmitButton.vue";
import AttachmentRow from "@/views/feature-config/fields/AttachmentRow.vue";
import ModuleEntryEditor from "@/views/feature-config/blocks/ModuleEntryEditor.vue";
import { featureConfigApi } from "@/api";
import type { FeatureConfigLove, FeatureConfigPatch } from "@/types";

/**
 * 恋爱设置弹窗（恋爱管理页 ⚙ 入口）：
 * 顶部分段器切换「恋爱日记 / 恋爱故事 / 恋爱相册 / 恋爱清单 / 恋爱页」，
 * initialSegment 按进入入口页定位默认分段。
 * 仅保存 spec.love（后端 merge 不动其它分区；密码 BCrypt 语义由后端 save 处理）。
 */
const props = withDefaults(
  defineProps<{
    /** 默认分段 id（从不同管理页进入时定位到对应模块） */
    initialSegment?: string;
    visible?: boolean;
  }>(),
  { initialSegment: "diary", visible: false }
);

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
}>();

/** 顶部分段器 */
const SEGMENTS = [
  { id: "diary", label: "恋爱日记" },
  { id: "stories", label: "恋爱故事" },
  { id: "albums", label: "恋爱相册" },
  { id: "daily", label: "恋爱清单" },
];
const segment = ref(props.initialSegment);

const queryClient = useQueryClient();

const { data: config, isLoading, refetch } = useQuery({
  queryKey: ["uni-halo:feature-config"],
  queryFn: () => featureConfigApi.get(),
});

/** 草稿（仅 love 分区；加载后深拷贝，保存时整块写回） */
const draft = ref<FeatureConfigLove>({});

/** 用后端配置覆盖本地草稿（各分区缺省为空对象，保证深层字段可写） */
const syncDraft = () => {
  if (!config.value) {
    return;
  }
  const love = cloneDeep(config.value.spec?.love || {});
  draft.value = {
    ...love,
    loveDiary: love.loveDiary || { passwordEnabled: false },
    ourStory: love.ourStory || {},
    lovePhoto: love.lovePhoto || {},
    loveDaily: love.loveDaily || {},
    diaryPage: love.diaryPage || {},
  };
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

/** 三模块入口定义（key → 分段 id 一致，password 语义见 FeatureConfigLoveModule 类型） */
const LOVE_MODULES = {
  stories: {
    key: "ourStory" as const,
    label: "恋爱故事",
    desc: "恋爱页是否展示「恋爱故事」入口",
  },
  albums: {
    key: "lovePhoto" as const,
    label: "恋爱相册",
    desc: "恋爱页是否展示「恋爱相册」入口",
  },
  daily: {
    key: "loveDaily" as const,
    label: "恋爱清单",
    desc: "恋爱页是否展示「恋爱清单」入口",
  },
};

const saving = ref(false);

const handleSave = async () => {
  saving.value = true;
  try {
    await featureConfigApi.save(<FeatureConfigPatch>{
      metadata: { name: "feature-config" },
      spec: { love: draft.value },
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
    title="恋爱设置"
    :layer-closable="true"
    mount-to-body
    @update:visible="emit('update:visible', $event)"
    @close="emit('update:visible', false)"
  >
    <VTabbar v-model:active-id="segment" :items="SEGMENTS" class=":uno: mb-4" />

    <div v-if="isLoading" class=":uno: py-2">
      <VLoading />
    </div>
    <div v-else class=":uno: space-y-4">
      <!-- 恋爱日记段：背景图置顶（通栏大图预览），其后为入口密码 -->
      <template v-if="segment === 'diary'">
        <AttachmentRow v-model="draft.diaryPage!.bgImageUrl!" name="love_diary_bg_image" label="恋爱页背景图"
          help="恋爱页（恋爱日记）顶部背景图，留空时使用内置默认图" width="100%" aspect-ratio="16/9" />
        <ModuleEntryEditor v-model="draft.loveDiary!" label="恋爱日记入口" name-prefix="love_diary" :show-entry-fields="false"
          description="恋爱页（app 端恋爱页）本身的入口密码；设置后进入恋爱页前需先验证密码" />
      </template>

      <!-- 模块入口（分段与各管理页对应） -->
      <template v-for="(module, segmentId) in LOVE_MODULES" :key="segmentId">
        <div v-if="segment === segmentId">
          <ModuleEntryEditor v-model="draft[module.key]!" :label="module.label" :description="module.desc"
            :name-prefix="`love_${module.key}`" show-enabled-switch />
          <p class=":uno: mt-3 text-xs text-gray-400">
            设置密码后，小程序端进入「{{ module.label }}」前需先验证密码；模块数据在「恋爱管理-{{ module.label }}」维护。
          </p>
        </div>
      </template>

    </div>

    <template #footer>
      <div class=":uno: flex items-center gap-3">
        <SubmitButton type="secondary" :loading="saving" text="保存" @submit="handleSave" />
        <VButton @click="emit('update:visible', false)">取消</VButton>
      </div>
    </template>
  </VModal>
</template>
