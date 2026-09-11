<script setup lang="ts">
import {VButton, VSwitch} from "@halo-dev/components";
import { inject } from "vue";
import type { GeneralConfigLove } from "@/types";
import { GeneralConfigFormKey } from "../form-context";

/**
 * 恋爱设置分区：
 * 恋爱信息（纪念日 + 恋人信息，2026-09-11 起由「恋爱管理-恋爱配置」迁入）/
 * 模块入口（恋爱日记 + 三模块的开关与入口密码；恋爱日记密码 2026-09-11 起
 * 由基本设置迁入此处顶部）。恋爱页背景图已迁至「页面设置-恋爱日记」。
 */
defineProps<{ subTab: string }>();

const { formState } = inject(GeneralConfigFormKey)!;

/** 恋爱入口（恋爱日记 + 三模块，模块入口分区渲染数据源；password 语义见 GeneralConfigLoveModule 类型） */
const LOVE_MODULES: Array<{
  key: "loveDiary" | "ourStory" | "lovePhoto" | "loveDaily";
  label: string;
  desc: string;
}> = [
  {key: "loveDiary", label: "恋爱日记", desc: "恋爱页（恋爱日记）本身入口，设置密码后进入恋爱页前需先验证"},
  {key: "ourStory", label: "恋爱故事", desc: "恋爱页是否展示「恋爱故事」入口"},
  {key: "lovePhoto", label: "恋爱相册", desc: "恋爱页是否展示「恋爱相册」入口"},
  {key: "loveDaily", label: "恋爱清单", desc: "恋爱页是否展示「恋爱清单」入口"},
];

/** 清除某恋爱入口密码（清空新密码输入并标记 passwordRemoved，保存时后端清除） */
function clearModulePassword(key: "loveDiary" | "ourStory" | "lovePhoto" | "loveDaily") {
  const module = formState.value.spec.love[key];
  if (module) {
    module.password = "";
    module.passwordRemoved = true;
  }
}

/** 新密码输入时自动取消「清除密码」标记（重设优先级高于清除） */
function cancelRemovalOnTyping(module: GeneralConfigLove["ourStory"]) {
  if (module?.passwordRemoved) {
    module.passwordRemoved = false;
  }
}
</script>

<template>
  <!-- 恋爱 → 恋爱信息（纪念日 + 恋人信息；2026-09-11 起由「恋爱管理-恋爱配置」迁入） -->
  <template v-if="subTab === 'info'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      恋爱纪念日与恋人信息展示在恋爱页顶部，数据迁移自原「恋爱管理-恋爱配置」。
    </p>
    <div class=":uno: mb-4">
      <div class=":uno: mb-2 text-sm font-semibold text-gray-700">纪念日</div>
      <FormKit
        v-model="formState.spec.love.loveInfo!.loveDateTitle"
        name="love_info_date_title"
        label="纪念日标题"
        type="text"
        placeholder="例如：我们在一起的那天"
      />
      <FormKit
        v-model="formState.spec.love.loveInfo!.loveDate"
        name="love_info_date"
        label="恋爱纪念日"
        type="date"
        help="用于计算恋爱天数，同时这可是一个非常重要的节日呢，可不能忘记哦~"
      />
    </div>

    <div class=":uno: mb-4">
      <div class=":uno: mb-2 text-sm font-semibold text-gray-700">恋人信息</div>
      <div class=":uno: grid grid-cols-1 gap-4 md:grid-cols-2">
        <div class="flex flex-col">
          <FormKit
            v-model="formState.spec.love.loveInfo!.boyAvatar"
            name="love_info_boy_avatar"
            label="男生头像"
            type="attachment"
            :accepts="['image/*']"
          />
          <FormKit
            v-model="formState.spec.love.loveInfo!.boyNickname"
            name="love_info_boy_nickname"
            label="男生昵称"
            type="text"
            placeholder="男生的昵称"
          />
        </div>
        <div class="flex flex-col">
          <FormKit
            v-model="formState.spec.love.loveInfo!.girlAvatar"
            name="love_info_girl_avatar"
            label="女生头像"
            type="attachment"
            :accepts="['image/*']"
          />
          <FormKit
            v-model="formState.spec.love.loveInfo!.girlNickname"
            name="love_info_girl_nickname"
            label="女生昵称"
            type="text"
            placeholder="女生的昵称"
          />
        </div>
      </div>
    </div>
  </template>

  <!-- 恋爱 → 模块入口（恋爱日记 + 三模块：开关 + 入口密码） -->
  <template v-if="subTab === 'modules'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      以下为恋爱页各模块入口的展示开关与入口密码；模块数据分别在「恋爱管理-恋爱故事 / 恋爱相册 / 恋爱清单」维护。
      设置密码后，小程序端进入对应模块前需先验证密码。
    </p>
    <div v-for="item in LOVE_MODULES" :key="item.key" class=":uno: mb-4 rounded-lg bg-gray-50 p-4">
      <div class=":uno: flex items-center justify-between gap-4 border-b border-gray-100 pb-3">
        <div>
          <div class=":uno: text-sm text-gray-700">{{ item.label }}</div>
          <div class=":uno: mt-0.5 text-xs text-gray-400">{{ item.desc }}</div>
        </div>
        <VSwitch v-model="formState.spec.love[item.key]!.enabled" />
      </div>
      <div class=":uno: mt-3">
        <div class=":uno: mb-2 flex items-center text-sm text-gray-700">
          入口密码
          <span
            v-if="formState.spec.love[item.key]!.passwordEnabled"
            class=":uno: ml-2 text-xs font-normal text-emerald-600"
          >已设置</span>
          <span v-else class=":uno: ml-2 text-xs font-normal text-gray-400">未设置</span>
        </div>
        <FormKit
          v-if="!formState.spec.love[item.key]!.passwordRemoved"
          v-model="formState.spec.love[item.key]!.password"
          :name="`love_${item.key}_password`"
          label="新密码"
          type="password"
          :help="formState.spec.love[item.key]!.passwordEnabled
            ? '留空表示保持原密码不变'
            : '设置后进入该模块前需先输入密码'"
          placeholder="输入入口密码"
          @input="cancelRemovalOnTyping(formState.spec.love[item.key])"
        />
        <p v-else class=":uno: text-sm text-gray-500">保存后将清除该入口密码。</p>
        <VButton
          v-if="formState.spec.love[item.key]!.passwordEnabled && !formState.spec.love[item.key]!.passwordRemoved"
          size="sm"
          type="danger"
          plain
          class=":uno: mt-2"
          @click="clearModulePassword(item.key)"
        >
          清除密码
        </VButton>
        <VButton
          v-if="formState.spec.love[item.key]!.passwordRemoved"
          size="sm"
          type="secondary"
          plain
          class=":uno: mt-2"
          @click="formState.spec.love[item.key]!.passwordRemoved = false"
        >
          取消清除
        </VButton>
      </div>
    </div>
  </template>
</template>
