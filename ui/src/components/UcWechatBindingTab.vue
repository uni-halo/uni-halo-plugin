<script setup lang="ts">
// UC 个人中心「微信绑定」选项卡：由 uc:user:profile:tabs:create 扩展点挂载。
//
// 展示当前登录用户的微信绑定状态；未绑定时可弹出二维码，用微信小程序扫码确认后
// 完成绑定。二维码内容为 uh-bindwx-{ticket}（服务端签发，前缀约定见 Constants），
// 弹窗每 2 秒轮询票据状态（短时低频场景，轮询优于 WebSocket）。
import {
  Dialog,
  IconCheckboxCircle,
  IconErrorWarning,
  IconShieldUser,
  IconTimerLine,
  Toast,
  VButton,
  VEmpty,
  VLoading,
  VModal,
  VSpace,
  VTag,
} from "@halo-dev/components";
import { useQuery, useQueryClient } from "@tanstack/vue-query";
import { computed, onUnmounted, ref } from "vue";
import QRCode from "qrcode";
import {
  ucWechatBindingApi,
} from "@/api";

const BIND_TICKET_POLL_INTERVAL = 2000;
const COUNTDOWN_INTERVAL = 1000;

const queryClient = useQueryClient();

const { data: binding, isLoading } = useQuery({
  queryKey: ["uni-halo:uc-wechat-binding"],
  queryFn: () => ucWechatBindingApi.getMyBinding(),
});

// ----- 扫码绑定弹窗状态机 -----
// idle → loading → scanning(PENDING) → scanned(SCANNED) → confirmed / failed / expired
//
// scanned 是两阶段确认的「待确认」阶段：二维码必然会被屏幕共享、截图外传看到，
// 扫码即绑定等于把「拿到二维码」当成「可以绑走这个账号」。所以扫码只登记微信
// 身份，必须本人在弹窗里再点一次确认，才真正建立绑定。
const modalVisible = ref(false);
// failed：绑定失败或被本人拒绝（如该微信已绑其他账号），与 expired 分开提示
const phase = ref<
  | "idle"
  | "loading"
  | "scanning"
  | "scanned"
  | "binding"
  | "confirmed"
  | "failed"
  | "expired"
>("idle");
const qrDataUrl = ref("");
const qrError = ref("");
const bindError = ref("");
/** 当前票据号（scanned 阶段确认/拒绝时要带回服务端） */
const currentTicket = ref("");
/** 扫码方微信标识的脱敏尾号，供本人核对「是谁在扫」 */
const scanHint = ref("");
const approving = ref(false);
let pollTimer: ReturnType<typeof setInterval> | null = null;

// ----- 有效期倒计时 -----
// 票据 TTL 3 分钟：只靠轮询发现过期，用户会在扫码后才发现白扫一趟。
// 本地倒计时让剩余时间可见（越接近过期越醒目），归零立即落过期态，
// 与服务端 status() 的 EXPIRED 判定一致（以服务端返回为准，这里只是提前提示）。
const remainingSeconds = ref(0);
let countdownTimer: ReturnType<typeof setInterval> | null = null;

const remainingText = computed(() => {
  if (remainingSeconds.value <= 0) {
    return "";
  }
  const m = Math.floor(remainingSeconds.value / 60);
  const s = remainingSeconds.value % 60;
  return `${m}:${String(s).padStart(2, "0")}`;
});

/** 遮罩覆盖二维码并显示当前状态：扫码后/处理中/已失效/已过期都不需要再看码 */
const showMask = computed(() =>
  ["loading", "scanned", "binding", "failed", "expired"].includes(phase.value)
);

/** 重新生成按钮可见状态：生成中/已成功时不需要（前者自动创建、后者应关闭弹窗） */
const canRegenerate = computed(() =>
  ["scanning", "scanned", "failed", "expired"].includes(phase.value)
);

function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer);
    countdownTimer = null;
  }
}

function startCountdown(expiresAt: string) {
  stopCountdown();
  const target = Date.parse(expiresAt);
  if (Number.isNaN(target)) {
    return;
  }
  const tick = () => {
    remainingSeconds.value = Math.max(
      0,
      Math.ceil((target - Date.now()) / 1000)
    );
    // 本地倒计时归零：直接落过期态，不等下一次轮询（服务端判定以 EXPIRED 为准）
    if (remainingSeconds.value === 0 && phase.value === "scanning") {
      stopPolling();
      stopCountdown();
      phase.value = "expired";
    }
  };
  tick();
  countdownTimer = setInterval(tick, COUNTDOWN_INTERVAL);
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
}

async function pollTicket(ticket: string) {
  try {
    const { status, reason, hint } = await ucWechatBindingApi.getTicketStatus(ticket);
    if (status === "CONFIRMED") {
      stopPolling();
      phase.value = "confirmed";
      queryClient.invalidateQueries({ queryKey: ["uni-halo:uc-wechat-binding"] });
      Toast.success("微信绑定成功，站内通知已发送");
    } else if (status === "SCANNED") {
      // 已扫码，等本人确认：继续轮询（确认后状态会推进到处理中 → CONFIRMED/FAILED）
      phase.value = "scanned";
      scanHint.value = hint || "";
    } else if (status === "FAILED") {
      // 已扫码但绑定失败：直接展示服务端原因，别让用户干等到二维码过期
      stopPolling();
      phase.value = "failed";
      bindError.value = reason || "绑定失败，请稍后重试";
    } else if (status === "EXPIRED") {
      stopPolling();
      phase.value = "expired";
    }
    // PENDING：继续等待（含「已扫码、绑定处理中」）
  } catch {
    // 单次轮询失败不终止流程（网络抖动），下一轮继续
  }
}

async function createTicket() {
  phase.value = "loading";
  qrDataUrl.value = "";
  qrError.value = "";
  bindError.value = "";
  scanHint.value = "";
  remainingSeconds.value = 0;
  try {
    const ticket = await ucWechatBindingApi.createBindTicket();
    currentTicket.value = ticket.ticket;
    // 二维码内容即 qrContent（uh-bindwx-{ticket}），扫码后由小程序端识别前缀并处理
    qrDataUrl.value = await QRCode.toDataURL(ticket.qrContent, {
      width: 220,
      margin: 1,
    });
    phase.value = "scanning";
    stopPolling();
    pollTimer = setInterval(() => pollTicket(ticket.ticket), BIND_TICKET_POLL_INTERVAL);
    startCountdown(ticket.expiresAt);
  } catch (e) {
    stopPolling();
    phase.value = "expired";
    qrError.value = e instanceof Error ? e.message : "创建绑定请求失败";
  }
}

async function openModal() {
  modalVisible.value = true;
  await createTicket();
}

/**
 * 重新生成二维码：无需等旧票过期，随时可点。
 * 若旧票已被扫码（SCANNED），先 reject 让手机端立即收到失效反馈，
 * 否则对方会在确认页干等到过期。旧票的其他状态无需处理——
 * PENDING 会自然过期，终态本来就不可再用。
 */
function regenerate() {
  if (phase.value === "scanned" && currentTicket.value) {
    void ucWechatBindingApi.rejectTicket(currentTicket.value).catch(() => {
      // 拒绝失败不影响重新生成：旧票最多 3 分钟后自行过期
    });
  }
  stopPolling();
  stopCountdown();
  void createTicket();
}

function closeModal() {
  // 待确认阶段关窗 = 拒绝本次扫码：否则小程序端会一直干等到票据过期
  if (phase.value === "scanned" && currentTicket.value) {
    void ucWechatBindingApi.rejectTicket(currentTicket.value).catch(() => {
      // 拒绝失败不影响关窗：票据最多 3 分钟后自行过期
    });
  }
  modalVisible.value = false;
  stopPolling();
  stopCountdown();
  phase.value = "idle";
  currentTicket.value = "";
}

/** 确认绑定（两阶段第二阶段）：票据归属校验在服务端，他人无法代确认 */
async function confirmScan() {
  if (approving.value || !currentTicket.value) {
    return;
  }
  approving.value = true;
  try {
    await ucWechatBindingApi.approveTicket(currentTicket.value);
    // 继续轮询：服务端落 CONFIRMED / FAILED 后弹窗给出结果
    phase.value = "binding";
  } catch (e) {
    stopPolling();
    phase.value = "failed";
    bindError.value = e instanceof Error ? e.message : "确认失败，请稍后重试";
  } finally {
    approving.value = false;
  }
}

/** 拒绝本次扫码：票据落 FAILED 并回传原因，二维码立即失效 */
async function rejectScan() {
  if (!currentTicket.value) {
    return;
  }
  try {
    await ucWechatBindingApi.rejectTicket(currentTicket.value);
    stopPolling();
    phase.value = "failed";
    bindError.value = "你已取消本次绑定，二维码已失效";
  } catch {
    stopPolling();
    phase.value = "failed";
    bindError.value = "取消失败，请关闭弹窗后重新生成二维码";
  }
}

// ----- 自助解绑 -----
// 用户本人即可解绑（服务端只认登录身份，无需管理员介入）；管理员侧
// 的 wechat-users 解绑仍保留，用于用户无法自助操作的兜底场景。
// 解绑影响登录方式，必须先二次确认，避免误点导致微信登录失效。
const unbinding = ref(false);

function confirmUnbind() {
  Dialog.warning({
    title: "解除微信绑定",
    description:
      "解绑后无法使用微信一键登录本账号，需要重新扫码绑定或改用账号密码登录。Halo 账号本身不会被删除。",
    confirmType: "danger",
    confirmText: "确定解绑",
    cancelText: "取消",
    onConfirm: async () => {
      unbinding.value = true;
      try {
        await ucWechatBindingApi.unbindMyWechat();
        Toast.success("已解除微信绑定，站内通知已发送");
        // 本地直接置为未绑定，不立即重查：Halo 删除扩展是两阶段异步，
        // 删除指令返回后残留的 deleting 记录仍可能让重查拿到 bound=true，
        // 用户会以为没解绑成功。下次进入页面自然收敛到最终状态（与 App 端一致）。
        queryClient.setQueryData(["uni-halo:uc-wechat-binding"], {
          username: binding.value?.username ?? "",
          bound: false,
          providerUserId: undefined,
          boundAt: undefined,
        });
      } catch (e) {
        Toast.error(e instanceof Error ? e.message : "解绑失败，请稍后重试");
      } finally {
        unbinding.value = false;
      }
    },
  });
}

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
        <VButton size="sm" type="danger" :loading="unbinding" @click="confirmUnbind">
          解除绑定
        </VButton>
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

    <VModal :visible="modalVisible" :width="420" title="扫码绑定微信" @close="closeModal">
      <div class="flex flex-col items-center gap-4 py-2">
        <!-- 二维码区：容器常驻，状态变化通过遮罩表达，弹窗高度不跳动 -->
        <div v-if="phase !== 'confirmed'"
          class="relative h-[220px] w-[220px] overflow-hidden rounded border border-gray-100 bg-white">
          <img v-if="qrDataUrl" :src="qrDataUrl" alt="绑定二维码" class="h-full w-full" />
          <div v-else class="h-full w-full flex items-center justify-center">
            <VLoading />
          </div>

          <!-- 状态遮罩：盖在二维码上方，直接显示票据当前状态 -->
          <div v-if="showMask"
            class="absolute inset-0 z-10 flex flex-col items-center justify-center gap-2 bg-white/90">
            <template v-if="phase === 'loading'">
              <VLoading />
              <p class="text-sm text-gray-600">正在生成二维码…</p>
            </template>

            <template v-else-if="phase === 'scanned'">
              <IconShieldUser class=":uno: h-10 w-10 text-green-500" />
              <p class="text-sm font-medium text-gray-900">已扫码，待确认</p>
              <p class="text-center text-xs text-gray-500">请在下方确认或拒绝本次绑定</p>
            </template>

            <template v-else-if="phase === 'binding'">
              <VLoading />
              <p class="text-sm text-gray-600">正在完成绑定…</p>
            </template>

            <template v-else-if="phase === 'failed'">
              <IconErrorWarning class=":uno: h-10 w-10 text-red-500" />
              <p class="text-sm font-medium text-gray-900">二维码已失效</p>
              <p class="text-center text-xs text-gray-500">
                {{ bindError || "本次绑定未能完成" }}
              </p>
            </template>

            <template v-else>
              <IconTimerLine class=":uno: h-10 w-10 text-gray-400" />
              <p class="text-sm font-medium text-gray-900">
                {{ qrError || "二维码已过期" }}
              </p>
              <p class="text-center text-xs text-gray-500">请点击「重新生成」获取新二维码</p>
            </template>
          </div>
        </div>

        <p v-if="phase === 'scanning' && remainingText" class="text-xs"
          :class="remainingSeconds <= 30 ? 'text-red-500' : 'text-gray-400'">
          有效期剩余 {{ remainingText }}
        </p>

        <!-- 下方操作区 -->
        <template v-if="phase === 'scanning'">
          <p class="text-center text-sm text-gray-600">
            请打开 UniHalo 微信小程序，使用
            <span class="font-medium text-gray-900">「扫一扫」</span>
            扫描二维码，<br />扫码后需在电脑端确认才会完成绑定
          </p>
        </template>

        <template v-else-if="phase === 'scanned'">
          <p class="text-center text-sm text-gray-500">
            {{ scanHint ? `微信标识 ${scanHint} ` : "" }}请求绑定到当前账号。<br />
            请确认这是你本人的操作 —— 确认后该微信即可一键登录本账号。
          </p>
          <VSpace>
            <VButton type="primary" size="sm" :loading="approving" @click="confirmScan">
              确认绑定
            </VButton>
            <VButton size="sm" @click="rejectScan">拒绝</VButton>
          </VSpace>
        </template>

        <template v-else-if="phase === 'confirmed'">
          <IconCheckboxCircle class=":uno: h-10 w-10 text-green-500" />
          <p class="text-sm font-medium text-gray-900">绑定成功</p>
          <p class="text-sm text-gray-500">现在可以在 UniHalo 客户端使用微信一键登录本账号。</p>
        </template>

        <template v-else-if="phase === 'failed'">
          <VButton type="secondary" size="sm" @click="regenerate">重新生成二维码</VButton>
        </template>

        <template v-else-if="phase === 'expired'">
          <VButton type="secondary" size="sm" @click="regenerate">重新生成二维码</VButton>
        </template>
      </div>

      <template #footer>
        <VSpace>
          <VButton v-if="canRegenerate" type="secondary" @click="regenerate">
            重新生成
          </VButton>
          <VButton @click="closeModal">{{ phase === "confirmed" ? "完成" : "取消" }}</VButton>
        </VSpace>
      </template>
    </VModal>
  </div>
</template>
