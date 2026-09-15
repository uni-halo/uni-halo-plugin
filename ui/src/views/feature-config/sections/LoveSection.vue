<script setup lang="ts">
import { VButton, VSwitch } from "@halo-dev/components";
import { VueDraggable } from "vue-draggable-plus";
import { computed, inject, ref } from "vue";
import RiDragMove2Line from "~icons/ri/drag-move-2-line";
import RiArrowDownSLine from "~icons/ri/arrow-down-s-line";
import RiArrowUpSLine from "~icons/ri/arrow-up-s-line";
import type { FeatureConfigLoveModule } from "@/types";
import { FeatureConfigFormKey } from "../form-context";

/**
 * 恋爱设置分区：
 * 恋爱信息（纪念日 + 恋人信息）/ 页面入口（恋爱日记入口密码，无开关；
 * 入口显隐由页面设置-快捷导航/关于页功能入口注册表控制）/ 模块入口
 * （恋爱故事/相册/清单：开关 + 入口密码 + 入口列表数据
 * title/subTitle/颜色/iconBgColor/path + 拖拽排序，app 端直接消费）。
 * 恋爱页背景图配置于「页面设置-恋爱日记」。
 */
defineProps<{ subTab: string }>();

const { formState } = inject(FeatureConfigFormKey)!;

/** 恋爱模块入口（三模块，模块入口分区渲染数据源；password 语义见 FeatureConfigLoveModule 类型） */
const LOVE_MODULES: Array<{
  key: "ourStory" | "lovePhoto" | "loveDaily";
  label: string;
  desc: string;
}> = [
    { key: "ourStory", label: "恋爱故事", desc: "恋爱页是否展示「恋爱故事」入口" },
    { key: "lovePhoto", label: "恋爱相册", desc: "恋爱页是否展示「恋爱相册」入口" },
    { key: "loveDaily", label: "恋爱清单", desc: "恋爱页是否展示「恋爱清单」入口" },
  ];

/**
 * 模块入口拖拽排序：按 priority 降序展示（对齐后端 loveConfig 输出顺序、
 * app 端渲染顺序）；拖拽后按新顺序降序写回 priority（第一位最大、最后一位最小）。
 */
const moduleSortable = computed({
  get: () => {
    const keys = LOVE_MODULES.map((item) => item.key);
    keys.sort(
      (a, b) =>
        (formState.value.spec.love[b]?.priority ?? 0) -
        (formState.value.spec.love[a]?.priority ?? 0),
    );
    return keys;
  },
  set: (value: Array<"ourStory" | "lovePhoto" | "loveDaily">) => {
    const count = value.length;
    value.forEach((key, index) => {
      const module = formState.value.spec.love[key];
      if (module) {
        module.priority = count - index;
      }
    });
  },
});

/** FormKit type="color" 回显值：空值兜底（与快捷导航/社交色块同一套约定） */
function toColorInput(value?: string): string {
  return value || "#cccccc";
}

/** 各模块卡片折叠状态（key → 是否折叠；默认展开） */
const collapsedModules = ref<Record<string, boolean>>({});

/** 切换某模块卡片的折叠状态 */
function toggleModuleCollapse(key: string) {
  collapsedModules.value[key] = !collapsedModules.value[key];
}

/** FormKit type="color" 选色（format="hex8" 输出 #rrggbbaa 含透明度）写回标题颜色 */
function onModuleTitleColor(module: FeatureConfigLoveModule, value: unknown) {
  if (typeof value === "string") {
    module.titleColor = value;
  }
}

/** FormKit type="color" 选色（format="hex8"）写回副标题颜色 */
function onModuleSubTitleColor(module: FeatureConfigLoveModule, value: unknown) {
  if (typeof value === "string") {
    module.subTitleColor = value;
  }
}

/** FormKit type="color" 选色（format="hex8"）写回图标背景色 */
function onModuleIconBgColor(module: FeatureConfigLoveModule, value: unknown) {
  if (typeof value === "string") {
    module.iconBgColor = value;
  }
}

/** 清除某恋爱入口密码（清空新密码输入并标记 passwordRemoved，保存时后端清除） */
function clearModulePassword(key: "loveDiary" | "ourStory" | "lovePhoto" | "loveDaily") {
  const module = formState.value.spec.love[key];
  if (module) {
    module.password = "";
    module.passwordRemoved = true;
  }
}

/** 新密码输入时自动取消「清除密码」标记（重设优先级高于清除） */
function cancelRemovalOnTyping(module: FeatureConfigLoveModule) {
  if (module?.passwordRemoved) {
    module.passwordRemoved = false;
  }
}
</script>

<template>
  <!-- 恋爱 → 页面设置（恋爱日记页标题 + 恋爱页背景图，2026-09-15 自页面设置-恋爱日记迁入） -->
  <template v-if="subTab === 'page'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      恋爱日记页（app 端恋爱页）的展示标题与背景图；留空分别回退内置标题与内置背景。
    </p>
    <FormKit v-model="formState.spec.love.diaryPage!.pageTitle" name="love_diary_page_title" label="页面标题" type="text"
      help="恋爱日记页展示标题，留空使用默认" />
    <FormKit v-model="formState.spec.love.diaryPage!.bgImageUrl" name="love_diary_bg_image" label="恋爱页背景图"
      type="attachment" :accepts="['image/*']" help="恋爱页（恋爱日记）顶部背景图，留空使用内置回退" />
  </template>

  <!-- 恋爱 → 恋爱信息（纪念日 + 恋人信息） -->
  <template v-if="subTab === 'info'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      恋爱纪念日与恋人信息展示在恋爱页顶部。
    </p>
    <div class=":uno: mb-6">
      <div class=":uno: mb-2 text-sm font-semibold text-gray-700">纪念日</div>
      <FormKit v-model="formState.spec.love.loveInfo!.loveDateTitle" name="love_info_date_title" label="纪念日标题"
        type="text" placeholder="例如：我们在一起的那天" />
      <FormKit v-model="formState.spec.love.loveInfo!.loveDate" name="love_info_date" label="恋爱纪念日" type="date"
        help="用于计算恋爱天数，同时这可是一个非常重要的节日呢，可不能忘记哦~" />
    </div>

    <div class=":uno: mb-4">
      <div class=":uno: mb-2 text-sm font-semibold text-gray-700">恋人信息</div>
      <FormKit v-model="formState.spec.love.loveInfo!.boyAvatar" name="love_info_boy_avatar" label="男生头像"
        type="attachment" :accepts="['image/*']" />
      <FormKit v-model="formState.spec.love.loveInfo!.boyNickname" name="love_info_boy_nickname" label="男生昵称"
        type="text" placeholder="男生的昵称" />
      <FormKit v-model="formState.spec.love.loveInfo!.girlAvatar" name="love_info_girl_avatar" label="女生头像"
        type="attachment" :accepts="['image/*']" />
      <FormKit v-model="formState.spec.love.loveInfo!.girlNickname" name="love_info_girl_nickname" label="女生昵称"
        type="text" placeholder="女生的昵称" />
    </div>
  </template>

  <!-- 恋爱 → 页面入口（恋爱日记入口密码，无开关；入口显隐由页面设置 快捷导航/关于页功能入口注册表控制） -->
  <template v-if="subTab === 'pageEntry'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      恋爱日记页（app 端恋爱页）本身的入口密码；设置密码后，进入恋爱页前需先验证密码。
      入口的展示/显隐由「页面设置-快捷导航」或「关于页-功能入口」控制，此处无需开关。
    </p>
    <div class=":uno: mb-4 rounded-lg bg-gray-50 p-4">
      <div class=":uno: mb-2 flex items-center text-sm text-gray-700">
        入口密码
        <span v-if="formState.spec.love.loveDiary!.passwordEnabled"
          class=":uno: ml-2 text-xs font-normal text-emerald-600">已设置</span>
        <span v-else class=":uno: ml-2 text-xs font-normal text-gray-400">未设置</span>
      </div>
      <FormKit v-if="!formState.spec.love.loveDiary!.passwordRemoved" v-model="formState.spec.love.loveDiary!.password"
        name="love_diary_password" label="新密码" type="password" :help="formState.spec.love.loveDiary!.passwordEnabled
          ? '留空表示保持原密码不变'
          : '设置后进入恋爱页前需先输入密码'" placeholder="输入入口密码" @input="cancelRemovalOnTyping(formState.spec.love.loveDiary!)" />
      <p v-else class=":uno: text-sm text-gray-500">保存后将清除该入口密码。</p>
      <VButton v-if="formState.spec.love.loveDiary!.passwordEnabled && !formState.spec.love.loveDiary!.passwordRemoved"
        size="sm" type="danger" plain class=":uno: mt-2" @click="clearModulePassword('loveDiary')">
        清除密码
      </VButton>
      <VButton v-if="formState.spec.love.loveDiary!.passwordRemoved" size="sm" type="secondary" plain class=":uno: mt-2"
        @click="formState.spec.love.loveDiary!.passwordRemoved = false">
        取消清除
      </VButton>
    </div>
  </template>

  <!-- 恋爱 → 模块入口（恋爱故事/相册/清单：开关 + 入口列表数据 + 入口密码 + 拖拽排序；
       入口列表数据供 app 端直接渲染，模块数据分别在「恋爱管理」对应菜单维护） -->
  <template v-if="subTab === 'modules'">
    <p class=":uno: mb-3 text-xs text-gray-400 flex flex-col gap-y-2">
      以下为恋爱页各模块入口的展示开关,拖拽卡片可调整展示顺序。 模块数据分别在「恋爱管理-恋爱故事 / 恋爱相册 / 恋爱清单」维护。 设置密码后，小程序端进入对应模块前需先验证密码。
    </p>
    <VueDraggable v-model="moduleSortable" handle=".love-module-drag-handle" class=":uno: space-y-3">
      <div v-for="itemKey in moduleSortable" :key="itemKey" class=":uno: rounded-lg bg-gray-50 p-4">
        <div class=":uno: flex items-center justify-between gap-4 border-b border-gray-100 pb-3">
          <div class=":uno: flex items-center gap-2">
            <RiDragMove2Line class="love-module-drag-handle cursor-move text-gray-400" />
            <div>
              <div class=":uno: text-sm text-gray-700 font-bold"
                :style="{ color: formState.spec.love[itemKey]!.titleColor }">
                {{LOVE_MODULES.find((item) => item.key === itemKey)!.label}}
              </div>
              <div class=":uno: mt-0.5 text-xs text-gray-400">
                {{LOVE_MODULES.find((item) => item.key === itemKey)!.desc}}
              </div>
            </div>
          </div>
          <div class=":uno: flex items-center gap-4">
            <VSwitch v-model="formState.spec.love[itemKey]!.enabled" />
            <VButton size="sm" type="secondary" plain class=":uno: !py-1.5 !px-3 rounded-full"
              :title="collapsedModules[itemKey] ? '展开配置' : '折叠配置'" @click="toggleModuleCollapse(itemKey)">
              <span class=":uno: flex items-center justify-center gap-x-1 my-auto">
                <span>{{ collapsedModules[itemKey] ? '展开配置' : '折叠配置' }} </span>
                <RiArrowDownSLine v-if="collapsedModules[itemKey]" class=":uno: text-base" />
                <RiArrowUpSLine v-else class=":uno: text-base" />
              </span>
            </VButton>
          </div>
        </div>

        <div v-show="!collapsedModules[itemKey]" class=":uno: pt-1">
          <div class=":uno: mt-3 grid grid-cols-1 gap-3">
            <FormKit v-model="formState.spec.love[itemKey]!.title" :name="`love_${itemKey}_title`" label="入口名称"
              type="text" placeholder="入口名称（app 端入口列表标题）" />
            <FormKit v-model="formState.spec.love[itemKey]!.subTitle" :name="`love_${itemKey}_sub_title`" label="副标题"
              type="text" placeholder="入口副标题" />
          </div>

          <!-- 颜色 -->
          <div class=":uno: mt-6 space-y-6">
            <div class=":uno: flex items-center gap-3">
              <span class=":uno: w-20 shrink-0 text-xs text-gray-700">标题颜色</span>
              <FormKit type="color" format="hex8" :model-value="toColorInput(formState.spec.love[itemKey]!.titleColor)"
                @update:model-value="onModuleTitleColor(formState.spec.love[itemKey]!, $event)"
                outer-class=":uno: w-12 shrink-0 !pt-0" />
            </div>
            <div class=":uno: flex items-center gap-3">
              <span class=":uno: w-20 shrink-0 text-xs text-gray-700">副标题颜色</span>
              <FormKit type="color" format="hex8"
                :model-value="toColorInput(formState.spec.love[itemKey]!.subTitleColor)"
                @update:model-value="onModuleSubTitleColor(formState.spec.love[itemKey]!, $event)"
                outer-class=":uno: w-12 shrink-0 !pt-0" />
            </div>
            <div class=":uno: flex items-center gap-3">
              <span class=":uno: w-20 shrink-0 text-xs text-gray-700">图标背景色</span>
              <FormKit type="color" format="hex8" :model-value="toColorInput(formState.spec.love[itemKey]!.iconBgColor)"
                @update:model-value="onModuleIconBgColor(formState.spec.love[itemKey]!, $event)"
                outer-class=":uno: w-12 shrink-0 !pt-0" />
            </div>
            <FormKit v-model="formState.spec.love[itemKey]!.path" :name="`love_${itemKey}_path`" label="跳转路径"
              type="text" placeholder="app 端进入该模块页面的路径" />
          </div>

          <div class=":uno: mt-6">
            <div class=":uno: mb-2 flex items-center text-sm text-gray-700">
              入口密码
              <span v-if="formState.spec.love[itemKey]!.passwordEnabled"
                class=":uno: ml-2 text-xs font-normal text-emerald-600">已设置</span>
              <span v-else class=":uno: ml-2 text-xs font-normal text-gray-400">未设置</span>
            </div>
            <FormKit v-if="!formState.spec.love[itemKey]!.passwordRemoved"
              v-model="formState.spec.love[itemKey]!.password" :name="`love_${itemKey}_password`" label="新密码"
              type="password" :help="formState.spec.love[itemKey]!.passwordEnabled
                ? '留空表示保持原密码不变'
                : '设置后进入该模块前需先输入密码'" placeholder="输入入口密码"
              @input="cancelRemovalOnTyping(formState.spec.love[itemKey]!)" />
            <p v-else class=":uno: text-sm text-gray-500">保存后将清除该入口密码。</p>
            <VButton
              v-if="formState.spec.love[itemKey]!.passwordEnabled && !formState.spec.love[itemKey]!.passwordRemoved"
              size="sm" type="danger" plain class=":uno: mt-2" @click="clearModulePassword(itemKey)">
              清除密码
            </VButton>
            <VButton v-if="formState.spec.love[itemKey]!.passwordRemoved" size="sm" type="secondary" plain
              class=":uno: mt-2" @click="formState.spec.love[itemKey]!.passwordRemoved = false">
              取消清除
            </VButton>
          </div>
        </div>
      </div>
    </VueDraggable>
  </template>
</template>
