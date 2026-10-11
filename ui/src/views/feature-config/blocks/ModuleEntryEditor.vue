<script setup lang="ts">
import { VButton, VSwitch } from "@halo-dev/components";
import ColorFieldRow from "../fields/ColorFieldRow.vue";

/**
 * 恋爱模块入口编辑块（恋爱故事/相册/清单三模块复用）：
 * 展示开关 + 入口名称/副标题 + 三色配置 + 入口密码（BCrypt 语义由后端 save 处理）。
 * 只认 FeatureConfigLoveModule，不知道模块归属哪个页面。
 */
const props = withDefaults(
  defineProps<{
    /** 模块名称（卡片标题） */
    label: string;
    /** 模块说明 */
    description?: string;
    /** 是否展示「展示入口」开关（恋爱日记入口无开关） */
    showEnabledSwitch?: boolean;
    /** 是否展示入口名称/副标题/三色配置（默认展示；恋爱日记入口只在导航中，传 false 隐藏） */
    showEntryFields?: boolean;
    /** FormKit name 前缀（多实例同屏时保证 name 唯一） */
    namePrefix: string;
  }>(),
  {
    // Vue 对未传的 Boolean prop 按 false 处理，必须显式给默认值
    showEnabledSwitch: false,
    showEntryFields: true,
  }
);

const model = defineModel<{
  enabled?: boolean;
  title?: string;
  subTitle?: string;
  titleColor?: string;
  subTitleColor?: string;
  iconBgColor?: string;
  passwordEnabled?: boolean;
  password?: string;
  passwordRemoved?: boolean;
}>({ required: true });

/** 新密码输入时自动取消「清除密码」标记（重设优先级高于清除） */
function cancelRemovalOnTyping() {
  if (model.value.passwordRemoved) {
    model.value.passwordRemoved = false;
  }
}

/** 标记清除密码（保存时后端清除哈希并关闭验证） */
function clearPassword() {
  model.value.password = "";
  model.value.passwordRemoved = true;
}

/** 取消清除标记 */
function cancelRemoval() {
  model.value.passwordRemoved = false;
}
</script>

<template>
  <div class=":uno: rounded-lg bg-gray-50 p-4">
    <div class=":uno: flex items-center justify-between gap-4 border-b border-gray-100 pb-3">
      <div>
        <div class=":uno: text-sm font-bold text-gray-700" :style="{ color: model.titleColor }">{{ label }}</div>
        <div v-if="description" class=":uno: mt-0.5 text-xs text-gray-400">{{ description }}</div>
      </div>
      <div v-if="showEnabledSwitch" class=":uno: flex items-center gap-2 text-xs text-gray-500">
        <span>展示入口</span>
        <VSwitch v-model="model.enabled" />
      </div>
    </div>

    <!-- 入口名称/副标题/颜色（恋爱日记入口不展示：其入口在导航中） -->
    <template v-if="showEntryFields !== false">
      <div class=":uno: mt-3 grid grid-cols-1 gap-3">
        <FormKit v-model="model.title" :name="`${namePrefix}_title`" label="入口名称" type="text"
          placeholder="入口名称（app 端入口列表标题）" />
        <FormKit v-model="model.subTitle" :name="`${namePrefix}_sub_title`" label="副标题" type="text"
          placeholder="入口副标题" />
      </div>

      <!-- 颜色 -->
      <div class=":uno: mt-6 space-y-3">
        <ColorFieldRow v-model="model.titleColor!" label="标题颜色" />
        <ColorFieldRow v-model="model.subTitleColor!" label="副标题颜色" />
        <ColorFieldRow v-model="model.iconBgColor!" label="图标背景色" />
      </div>
    </template>

    <!-- 入口密码 -->
    <div class=":uno: mt-6">
      <div class=":uno: mb-2 flex items-center text-sm text-gray-700">
        入口密码
        <span v-if="model.passwordEnabled" class=":uno: ml-2 text-xs font-normal text-emerald-600">已设置</span>
        <span v-else class=":uno: ml-2 text-xs font-normal text-gray-400">未设置</span>
      </div>
      <FormKit v-if="!model.passwordRemoved" v-model="model.password" :name="`${namePrefix}_password`" label="新密码"
        type="password" :help="model.passwordEnabled ? '留空表示保持原密码不变' : '设置后进入该模块前需先输入密码'"
        placeholder="输入入口密码" @input="cancelRemovalOnTyping" />
      <p v-else class=":uno: text-sm text-gray-500">保存后将清除该入口密码。</p>
      <VButton v-if="model.passwordEnabled && !model.passwordRemoved" size="sm" type="danger" plain class=":uno: mt-2"
        @click="clearPassword">
        清除密码
      </VButton>
      <VButton v-if="model.passwordRemoved" size="sm" type="secondary" plain class=":uno: mt-2" @click="cancelRemoval">
        取消清除
      </VButton>
    </div>
  </div>
</template>
