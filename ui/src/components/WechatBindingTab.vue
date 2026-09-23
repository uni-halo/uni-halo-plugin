<script setup lang="ts">
// 用户详情「微信绑定」选项卡：由 user:detail:tabs:create 扩展点挂载。
//
// 只展示与「绑定关系」有关的信息和操作。用户本体的禁用、删号、改角色一律走 Halo
// 原生用户管理，这里不重复实现，避免维护两套用户管理逻辑。

import {
  Dialog,
  Toast,
  VButton,
  VEmpty,
  VLoading,
  VSpace,
  VTag,
} from "@halo-dev/components";
import { useQuery, useQueryClient } from "@tanstack/vue-query";
import { computed, ref } from "vue";
import type { DetailedUser } from "@halo-dev/api-client";
import { wechatUserApi } from "@/api";

const props = defineProps<{ user: DetailedUser }>();

const queryClient = useQueryClient();

const username = computed(() => props.user?.user?.metadata?.name ?? "");

/**
 * 完整 openid 默认不下发（服务端只给脱敏值），排查需要时由管理员显式点开 ——
 * 每次查看服务端都会记审计日志，所以不要默认开着。
 */
const reveal = ref(false);

// key 用原始值（username/reveal 的 .value），解绑后 setQueryData 才能命中同一份缓存；
// 整体包成 computed 让 key 随 username / reveal 变化自动重新查询。
const bindingQueryKey = computed(() => ["uni-halo:wechat-binding", username.value, reveal.value]);

const { data: binding, isLoading } = useQuery({
  queryKey: bindingQueryKey,
  queryFn: () => wechatUserApi.getBinding(username.value, reveal.value),
  enabled: computed(() => !!username.value),
});

const identityText = computed(() => {
  if (!binding.value?.bound) {
    return "-";
  }
  return binding.value.providerUserIdFull || binding.value.providerUserId || "-";
});

const boundAtText = computed(() => {
  const raw = binding.value?.boundAt;
  if (!raw) return "-";
  const date = new Date(raw);
  return Number.isNaN(date.getTime()) ? raw : date.toLocaleString();
});

const handleUnbind = () => {
  Dialog.warning({
    title: "解除微信绑定",
    description:
      "解绑后该用户将无法使用微信一键登录，需要重新绑定或改用账号密码登录。Halo 账号本身不会被删除，解绑后会向该用户发送站内通知。",
    confirmText: "确定解绑",
    cancelText: "取消",
    onConfirm: async () => {
      await wechatUserApi.unbind(username.value);
      Toast.success("已解除绑定，已通知该用户");
      // 本地直接置为未绑定，不立即重查：Halo 删除扩展是两阶段异步，
      // 删除指令返回后残留的 deleting 记录仍可能让重查拿到 bound=true，
      // 管理员会以为解绑没生效。下次进入用户详情即为最终状态。
      queryClient.setQueryData(bindingQueryKey.value, {
        username: username.value,
        bound: false,
        providerUserId: undefined,
        boundAt: undefined,
      });
    },
  });
};
</script>

<template>
  <div class="p-4">
    <VLoading v-if="isLoading" />

    <div v-else-if="!binding">
      <VEmpty title="加载失败" description="无法读取微信绑定信息" />
    </div>

    <div v-else-if="binding.bound" class="flex flex-col gap-4">
      <div class="flex items-center gap-2">
        <VTag type="success">已绑定</VTag>
        <span class="text-sm text-gray-600">该用户可以使用微信小程序一键登录</span>
      </div>

      <dl class="grid gap-y-3 text-sm" style="grid-template-columns: 96px 1fr">
        <dt class="text-gray-600">微信标识</dt>
        <dd class="flex flex-wrap items-center gap-2">
          <span class="break-all font-mono text-gray-900">{{ identityText }}</span>
          <VButton size="sm" type="secondary" @click="reveal = !reveal">
            {{ reveal ? "隐藏" : "显示完整" }}
          </VButton>
        </dd>
        <dt class="text-gray-600">绑定时间</dt>
        <dd class="text-gray-900">{{ boundAtText }}</dd>
        <dt class="text-gray-600">Halo 账号</dt>
        <dd class="text-gray-900">{{ username }}</dd>
      </dl>

      <VSpace>
        <VButton type="danger" size="sm" @click="handleUnbind">解除绑定</VButton>
      </VSpace>
    </div>

    <div v-else class="flex flex-col gap-3">
      <div class="flex items-center gap-2">
        <VTag>未绑定</VTag>
        <span class="text-sm text-gray-600">
          该用户尚未关联微信。使用微信一键登录时会自动创建新的 Halo 账号。
        </span>
      </div>
      <p class="text-sm text-gray-500">
        若希望该用户用微信登录进这个已有账号，需在小程序端扫码绑定。
      </p>
    </div>
  </div>
</template>
