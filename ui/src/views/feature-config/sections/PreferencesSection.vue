<script setup lang="ts">
import { inject } from "vue";
import { FeatureConfigFormKey } from "../form-context";

/**
 * 偏好设置分区：
 * 通用功能 / 首页 / 文章页面 / 归档页面 / 友情链接页 子 tab（L0 站点默认偏好，用户可在小程序端覆盖）。
 */
defineProps<{ subTab: string }>();

const { formState } = inject(FeatureConfigFormKey)!;

/** 卡片样式选项（组件 layout 值） */
const CARD_STYLE_OPTIONS = [
  {label: "上图下文", value: "image_top"},
  {label: "左文右图", value: "image_right"},
  {label: "上文下图", value: "image_bottom"},
  {label: "左图右文", value: "image_left"},
];
</script>

<template>
  <!-- 偏好设置 → 通用功能 -->
  <template v-if="subTab === 'general'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      以下为通用功能默认偏好，与小程序端「偏好设置-功能-通用功能」分组对齐；用户可在小程序端覆盖。
    </p>
    <FormKit
      v-model="formState.spec.preferences.avatarShape"
      name="pref_avatar_shape"
      label="头像外观"
      type="select"
      :options="[
        {label: '方形', value: 'square'},
        {label: '圆形', value: 'circle'},
      ]"
      help="应用于文章卡片（上文下图）与瞬间卡片的用户头像"
    />
  </template>

  <!-- 偏好设置 → 首页 -->
  <template v-if="subTab === 'home'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      以下为首页默认展示偏好，与小程序端「偏好设置-布局」首页分组对齐；用户可在「我的-设置」中按个人偏好覆盖。
    </p>
    <FormKit
      v-model="formState.spec.preferences.homeListLayout"
      name="pref_home_layout"
      label="首页列表布局"
      type="select"
      :options="[
        {label: '单列', value: 'single'},
        {label: '双列', value: 'double'},
      ]"
      help="首页笔记列表默认展示方式"
    />
    <FormKit
      v-model="formState.spec.preferences.homeCardType"
      name="pref_home_card_type"
      label="首页卡片样式"
      type="select"
      :options="CARD_STYLE_OPTIONS"
      help="首页笔记卡片中封面图与文字的位置关系"
    />
  </template>

  <!-- 偏好设置 → 文章列表 -->
  <template v-if="subTab === 'articles'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      以下为笔记列表页默认展示偏好，与小程序端「偏好设置-布局」笔记列表分组对齐；用户可在「我的-设置」中按个人偏好覆盖。
    </p>
    <FormKit
      v-model="formState.spec.preferences.articlesListLayout"
      name="pref_articles_layout"
      label="笔记列表布局"
      type="select"
      :options="[
        {label: '单列', value: 'single'},
        {label: '双列', value: 'double'},
      ]"
      help="笔记列表页默认展示方式"
    />
    <FormKit
      v-model="formState.spec.preferences.articlesCardType"
      name="pref_articles_card_type"
      label="笔记列表卡片样式"
      type="select"
      :options="CARD_STYLE_OPTIONS"
      help="笔记列表卡片中封面图与文字的位置关系"
    />
  </template>

  <!-- 偏好设置 → 文章归档 -->
  <template v-if="subTab === 'archives'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      以下为笔记归档页默认展示偏好，与小程序端「偏好设置-布局」笔记归档分组对齐；用户可在「我的-设置」中按个人偏好覆盖。
    </p>
    <FormKit
      v-model="formState.spec.preferences.archivesListLayout"
      name="pref_archives_layout"
      label="笔记归档布局"
      type="select"
      :options="[
        {label: '单列', value: 'single'},
        {label: '双列', value: 'double'},
      ]"
      help="笔记归档页默认展示方式"
    />
    <FormKit
      v-model="formState.spec.preferences.archivesCardType"
      name="pref_archives_card_type"
      label="笔记归档卡片样式"
      type="select"
      :options="CARD_STYLE_OPTIONS"
      help="笔记归档卡片中封面图与文字的位置关系"
    />
  </template>

  <!-- 偏好设置 → 友情链接页 -->
  <template v-if="subTab === 'linkPage'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      以下为友情链接页展示偏好；小程序打开模式决定用户点击小程序链接时的打开方式。
    </p>
    <FormKit
      v-model="formState.spec.preferences.linkPage!.miniProgramOpenMode"
      name="pref_link_page_open_mode"
      label="小程序友链打开模式"
      type="select"
      :options="[
        {label: '全屏', value: 'fullscreen'},
        {label: '半屏', value: 'halfScreen'},
      ]"
    />
  </template>
</template>
