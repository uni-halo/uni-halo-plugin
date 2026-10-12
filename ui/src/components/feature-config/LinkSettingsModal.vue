<script setup lang="ts">
import { Toast, VButton, VLoading, VModal, VSwitch, VTabbar } from "@halo-dev/components";
import { useQuery, useQueryClient } from "@tanstack/vue-query";
import { cloneDeep } from "lodash-es";
import { computed, ref, watch } from "vue";
import SubmitButton from "@/components/button/SubmitButton.vue";
import { featureConfigApi } from "@/api";
import type { FeatureConfigPatch } from "@/types";
import type { FeatureConfigLinkInfo, FeatureConfigSiteInfo } from "@/types";

/**
 * 友链设置弹窗（友链管理页 ⚙ 入口）：
 * 顶部分段器切换「提交申请 / 小程序信息 / 站点信息」三段。
 * 仅保存 spec.linkInfo（后端 merge 对 null/缺省字段不动其它分区）。
 */
const props = withDefaults(defineProps<{ visible?: boolean }>(), { visible: false });

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
}>();

/** 顶部分段器（当前分段） */
const SEGMENTS = [
  { id: "submission", label: "提交申请" },
  { id: "miniInfo", label: "小程序信息" },
  { id: "siteInfo", label: "站点信息" },
];
const segment = ref("submission");

const queryClient = useQueryClient();

const { data: config, isLoading, refetch } = useQuery({
  queryKey: ["uni-halo:feature-config"],
  queryFn: () => featureConfigApi.get(),
});

/** 草稿（仅 linkInfo 分区；加载后深拷贝，保存时整块写回） */
const draft = ref<FeatureConfigLinkInfo>({});

/** 用后端配置覆盖本地草稿（缺分区段补空对象，保证深层字段可写） */
const syncDraft = () => {
  if (!config.value) {
    return;
  }
  const linkInfo = cloneDeep(config.value.spec?.linkInfo || {});
  draft.value = {
    sendEmail: false,
    sendEmailSiteName: "uni-halo",
    ...linkInfo,
    miniInfo: linkInfo.miniInfo || {},
    siteInfo: linkInfo.siteInfo || {},
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

/** 站点订阅地址（textarea 换行分隔 ↔ 数组，对齐官方 feedUrls 字段） */
const feedUrlsText = computed({
  get: () => (draft.value.siteInfo?.feedUrls || []).join("\n"),
  set: (value: string) => {
    if (!draft.value.siteInfo) {
      draft.value.siteInfo = {} as FeatureConfigSiteInfo;
    }
    draft.value.siteInfo.feedUrls = value
      .split("\n")
      .map((s) => s.trim())
      .filter(Boolean);
  },
});

const saving = ref(false);

const handleSave = async () => {
  saving.value = true;
  try {
    await featureConfigApi.save(<FeatureConfigPatch>{
      metadata: { name: "feature-config" },
      spec: { linkInfo: draft.value },
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
    title="友链设置"
    :layer-closable="true"
    mount-to-body
    @update:visible="emit('update:visible', $event)"
    @close="emit('update:visible', false)"
  >
    <VTabbar v-model:active-id="segment" :items="SEGMENTS" class=":uno: mb-4" />

    <div v-if="isLoading" class=":uno: py-2"><VLoading /></div>
    <div v-else class=":uno: space-y-2">
      <!-- 提交申请 -->
      <template v-if="segment === 'submission'">
      <div class=":uno: flex items-center justify-between gap-4 border-b border-gray-100 py-3">
        <div>
          <div class=":uno: text-sm text-gray-700">开放友链申请</div>
          <div class=":uno: mt-0.5 text-xs text-gray-400">
            开启后 UniHalo 端可提交友链申请，后台「友链管理-友链审核」进行审核
          </div>
        </div>
        <VSwitch v-model="draft.submissionEnabled" />
      </div>
      <div class=":uno: flex items-center justify-between gap-4 border-b border-gray-100 py-3">
        <div>
          <div class=":uno: text-sm text-gray-700">审核结果邮件通知</div>
          <div class=":uno: mt-0.5 text-xs text-gray-400">
            开启后，审核通过/拒绝时向申请人邮箱发送通知（复用 Halo 系统设置的邮件通知器 SMTP 配置；申请未填邮箱则不发送）
          </div>
        </div>
        <VSwitch v-model="draft.sendEmail" />
      </div>
      <FormKit v-if="draft.sendEmail" v-model="draft.sendEmailSiteName" name="link_send_email_site_name"
        label="邮件署名" type="text" help="审核邮件标题与正文中的站点名称" />
      </template>

      <!-- 站点信息（本站站点名片，对齐官方友链提交 API） -->
      <template v-if="segment === 'siteInfo'">
      <p class=":uno: mb-3 text-xs text-gray-400">
        配置本站站点名片信息，字段对齐 Halo 官方链接管理插件，供展示与友链申请使用。
      </p>
      <FormKit v-model="draft.siteInfo!.displayName" name="link_site_display_name" label="网站名称" type="text"
        placeholder="如「UniHalo 博客」" />
      <FormKit v-model="draft.siteInfo!.url" name="link_site_url" label="网站地址" type="text"
        placeholder="如 https://your-site.com" help="HTTP/HTTPS 地址" />
      <FormKit v-model="draft.siteInfo!.logo" name="link_site_logo" label="网站 Logo" type="attachment"
        :accepts="['image/*']" placeholder="选择或粘贴 Logo 图片地址" />
      <FormKit v-model="draft.siteInfo!.description" name="link_site_description" label="网站描述" type="textarea"
        placeholder="一句话介绍你的网站" />
      <FormKit v-model="draft.siteInfo!.backlink" name="link_site_backlink" label="反链地址" type="text"
        placeholder="如 https://your-site.com/links" help="友链页面地址（反链）" />
      <FormKit v-model="feedUrlsText" name="link_site_feed_urls" label="订阅地址" type="textarea"
        placeholder="每行一个 RSS/Atom 地址" help="RSS/Atom 订阅地址，每行一个（存储为数组）" />
      </template>

      <!-- 小程序信息（app 端「申请信息」弹窗展示） -->
      <template v-if="segment === 'miniInfo'">
      <p class=":uno: mb-3 text-xs text-gray-400">
        配置你自己的小程序展示信息，用于 app 小程序端「申请信息」弹窗展示。
      </p>
      <FormKit v-model="draft.miniInfo!.displayName" name="link_display_name" label="小程序名称" type="text"
        placeholder="如「UniHalo 博客」" />
      <FormKit v-model="draft.miniInfo!.miniProgramCode" name="link_mini_program_code" label="太阳码/小程序码"
        type="attachment" :accepts="['image/*']" placeholder="选择或粘贴小程序码图片地址" help="小程序码图片，弹窗中点击可预览" />
      <FormKit v-model="draft.miniInfo!.appId" name="link_app_id" label="小程序 AppID" type="text"
        placeholder="小程序 AppID，如 wx1234567890abcdef" />
      <FormKit v-model="draft.miniInfo!.path" name="link_path" label="跳转页面路径" type="text"
        placeholder="如 pages/index/index" help="跳转页面路径" />
      <FormKit v-model="draft.miniInfo!.link" name="link_url" label="跳转地址" type="text"
        placeholder="如 #小程序://小莫唐尼/AGLiOpse2vi6QJC"
        help="说明：微信打开小程序，点击右上角三个点找到复制链接。" />
      <FormKit v-model="draft.miniInfo!.description" name="link_description" label="小程序描述" type="textarea"
        placeholder="一句话介绍你的小程序，如「记录生活与技术的个人博客」" />
      <FormKit v-model="draft.miniInfo!.applyRemark" name="link_apply_remark" label="申请说明" type="textarea"
        placeholder="如「欢迎友链互换，请附上你的网站信息」" help="弹窗中「申请说明」栏展示的文案" />
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
