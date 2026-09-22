<script setup lang="ts">
// UC 个人中心「微信绑定」选项卡：由 uc:user:profile:tabs:create 扩展点挂载。
//
// 展示当前登录用户的微信绑定状态；未绑定时可弹出二维码，用微信小程序扫码确认后
// 完成绑定。二维码内容为 uh-bindwx-{ticket}（服务端签发，前缀约定见 Constants），
// 弹窗每 2 秒轮询票据状态（短时低频场景，轮询优于 WebSocket）。
import {
  Toast,
  VButton,
  VEmpty,
  VLoading,
  VModal,
  VSpace,
  VTag,
} from "@halo-dev/components";
import { useQuery, useQueryClient } from "@tanstack/vue-query";
import { onUnmounted, ref } from "vue";
import QRCode from "qrcode";
import {
  ucWechatBindingApi,
} from "@/api";

const BIND_TICKET_POLL_INTERVAL = 2000;

const queryClient = useQueryClient();

const { data: binding, isLoading, refetch } = useQuery({
  queryKey: ["uni-halo:uc-wechat-binding"],
  queryFn: () => ucWechatBindingApi.getMyBinding(),
});

// ----- 扫码绑定弹窗状态机：idle → loading → scanning(PENDING) → confirmed / expired -----
const modalVisible = ref(false);
const phase = ref<"idle" | "loading" | "scanning" | "confirmed" | "expired">("idle");
const qrDataUrl = ref("");
const qrError = ref("");
let pollTimer: ReturnType<typeof setInterval> | null = null;

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
}

async function pollTicket(ticket: string) {
  try {
    const { status } = await ucWechatBindingApi.getTicketStatus(ticket);
    if (status === "CONFIRMED") {
      stopPolling();
      phase.value = "confirmed";
      queryClient.invalidateQueries({ queryKey: ["uni-halo:uc-wechat-binding"] });
      Toast.success("微信绑定成功");
    } else if (status === "EXPIRED") {
      stopPolling();
      phase.value = "expired";
    }
    // PENDING：继续等待
  } catch {
    // 单次轮询失败不终止流程（网络抖动），下一轮继续
  }
}

async function openModal() {
  modalVisible.value = true;
  phase.value = "loading";
  qrError.value = "";
  try {
    const ticket = await ucWechatBindingApi.createBindTicket();
    // 二维码内容即 qrContent（uh-bindwx-{ticket}），扫码后由小程序端识别前缀并处理
    qrDataUrl.value = await QRCode.toDataURL(ticket.qrContent, {
      width: 220,
      margin: 1,
    });
    phase.value = "scanning";
    stopPolling();
    pollTimer = setInterval(() => pollTicket(ticket.ticket), BIND_TICKET_POLL_INTERVAL);
  } catch (e) {
    stopPolling();
    phase.value = "expired";
    qrError.value = e instanceof Error ? e.message : "创建绑定请求失败";
  }
}

function closeModal() {
  modalVisible.value = false;
  stopPolling();
  phase.value = "idle";
}

const handleUnbindFailedNote = () => {
  // 解绑走 Console 管理端（wechat-users 接口），UC 侧普通用户无权限，
  // 引导联系管理员即可——UC 只提供「绑定」与状态查看。
  Toast.info("如需解绑，请联系站点管理员在用户管理中操作");
};

onUnmounted(stopPolling);
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
        <span class="text-sm text-gray-600">当前账号已关联微信，可以使用微信小程序一键登录</span>
      </div>

      <dl class="grid gap-y-3 text-sm" style="grid-template-columns: 96px 1fr">
        <dt class="text-gray-600">微信标识</dt>
        <dd class="break-all font-mono text-gray-900">
          {{ binding.providerUserId || "-" }}
        </dd>
        <dt class="text-gray-600">绑定时间</dt>
        <dd class="text-gray-900">
          {{
            binding.boundAt && !Number.isNaN(new Date(binding.boundAt).getTime())
              ? new Date(binding.boundAt).toLocaleString()
              : (binding.boundAt ?? "-")
          }}
        </dd>
      </dl>

      <VSpace>
        <VButton size="sm" @click="handleUnbindFailedNote">解除绑定</VButton>
      </VSpace>
    </div>

    <div v-else class="flex flex-col gap-3">
      <div class="flex items-center gap-2">
        <VTag>未绑定</VTag>
        <span class="text-sm text-gray-600">绑定后可以在 UniHalo 客户端使用微信一键登录本账号。</span>
      </div>
      <VSpace>
        <VButton type="secondary" size="sm" @click="openModal">扫码绑定微信</VButton>
      </VSpace>
    </div>

    <VModal
      :visible="modalVisible"
      :width="420"
      title="扫码绑定微信"
      @close="closeModal"
    >
      <div class="flex flex-col items-center gap-4 py-2">
        <template v-if="phase === 'loading'">
          <VLoading />
          <p class="text-sm text-gray-500">正在生成绑定二维码…</p>
        </template>

        <template v-else-if="phase === 'scanning'">
          <img
            :src="qrDataUrl"
            alt="绑定二维码"
            class="h-[220px] w-[220px] rounded border border-gray-100"
          />
          <p class="text-center text-sm text-gray-600">
            请打开微信，使用
            <span class="font-medium text-gray-900">「扫一扫」</span>
            扫描二维码，<br />在小程序中确认后即可完成绑定
          </p>
        </template>

        <template v-else-if="phase === 'confirmed'">
          <span class="text-4xl">✅</span>
          <p class="text-sm font-medium text-gray-900">绑定成功</p>
          <p class="text-sm text-gray-500">现在可以使用微信小程序一键登录了</p>
        </template>

        <template v-else>
          <span class="text-4xl">⏰</span>
          <p class="text-sm font-medium text-gray-900">
            {{ qrError || "二维码已过期" }}
          </p>
          <VButton type="secondary" size="sm" @click="openModal">重新生成</VButton>
        </template>
      </div>

      <template #footer>
        <VSpace>
          <VButton @click="closeModal">{{ phase === "confirmed" ? "完成" : "取消" }}</VButton>
        </VSpace>
      </template>
    </VModal>
  </div>
</template>
