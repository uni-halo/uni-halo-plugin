<script setup lang="ts">
import { computed, inject, ref } from "vue";
import { VButton } from "@halo-dev/components";
import { VueDraggable } from "vue-draggable-plus";
import RiDeleteBinLine from "~icons/ri/delete-bin-6-line";
import RiDragMove2Line from "~icons/ri/drag-move-2-line";
import RiImageLine from "~icons/ri/image-line";
import AuditCandidatesModal from "@/components/audit-config/AuditCandidatesModal.vue";
import { DEFAULT_MY_PAGE_COMMON_KEYS, DEFAULT_MY_PAGE_OTHER_KEYS, DEFAULT_QUICK_NAV_KEYS } from "@/constant/feature-entries";
import type {
  AuditDataRef,
  FeatureConfigCategoryItem,
  FeatureConfigQuickNavigationItem,
} from "@/types";
import { FeatureConfigFormKey } from "../form-context";
import BloggerProfileCard from "../blocks/BloggerProfileCard.vue";
import CommentSwitchGroup from "../blocks/CommentSwitchGroup.vue";
import QuickNavEditor from "../blocks/QuickNavEditor.vue";
import SwitchRow from "../fields/SwitchRow.vue";

/**
 * 外观布局分区（9 个子 tab）：
 * 通用外观 / 博主页（资料卡 + 功能入口）/ 首页 / 笔记页 / 归档页 / 分类页 /
 * 标签页 / 友链页 / 内容页（瞬间、笔记）。
 */
defineProps<{ subTab: string }>();

const { formState } = inject(FeatureConfigFormKey)!;

/** 卡片样式选项（组件 layout 值） */
const CARD_STYLE_OPTIONS = [
  { label: "上图下文", value: "image_top" },
  { label: "左文右图", value: "image_right" },
  { label: "上文下图", value: "image_bottom" },
  { label: "左图右文", value: "image_left" },
];

// ===== 博主页面：常用功能显示方式 =====

/** 常用功能显示方式（grid=宫格 / list=列表；旧数据字段缺失时回显 grid，与 app 端兜底一致） */
const commonFeaturesMode = computed({
  get: () => formState.value.spec.pages.blogger.commonFeaturesMode || "grid",
  set: (value: "grid" | "list") => {
    formState.value.spec.pages.blogger.commonFeaturesMode = value;
  },
});

// ===== 自定义功能入口候选库（spec.pages.customEntries，候选弹窗内维护）=====

const customEntries = computed<FeatureConfigQuickNavigationItem[]>(
  () => formState.value.spec.pages.customEntries || []
);

const setCustomEntries = (value: FeatureConfigQuickNavigationItem[]) => {
  formState.value.spec.pages.customEntries = value;
};

// ===== 首页分类栏选择（固定 3 个，复用审核模式候选弹窗）=====

const categoryModalVisible = ref(false);

/** 分类栏已选快照列表（写回 formState；拖拽排序，顺序 = 展示顺序） */
const homeCategoriesSortable = computed({
  get: () => formState.value.spec.pages.home.categories || [],
  set: (value: FeatureConfigCategoryItem[]) => {
    formState.value.spec.pages.home.categories = value;
  },
});

/** 回显给候选弹窗的 AuditDataRef 形态 */
const categoryModalSelected = computed<AuditDataRef[]>(() =>
  homeCategoriesSortable.value.map((item) => ({
    name: item.name || "",
    title: item.displayName,
    cover: item.cover,
    priority: item.priority,
    postCount: item.postCount,
  }))
);

/** 弹窗确认：映射回快照并关闭（候选弹窗已限制最多 3 个） */
const handleCategoryConfirm = (selected: AuditDataRef[]) => {
  formState.value.spec.pages.home.categories = selected.map((item) => ({
    name: item.name,
    displayName: item.title || item.name,
    cover: item.cover,
    priority: item.priority,
    postCount: item.postCount,
  }));
  categoryModalVisible.value = false;
};

const removeCategory = (item: FeatureConfigCategoryItem) => {
  homeCategoriesSortable.value = homeCategoriesSortable.value.filter(
    (it) => it.name !== item.name
  );
};
</script>

<template>
  <!-- 通用外观 -->
  <template v-if="subTab === 'general'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      以下为通用外观默认偏好，与小程序端「偏好设置-功能-通用功能」分组对齐；用户可在小程序端覆盖。
    </p>
    <FormKit v-model="formState.spec.preferences.avatarShape" name="pref_avatar_shape" label="头像外观" type="select"
      :options="[
        { label: '方形', value: 'square' },
        { label: '圆形', value: 'circle' },
      ]" help="应用于文章卡片（上文下图）与瞬间卡片的用户头像" />
  </template>

  <!-- 博主页（资料卡视觉 + 功能入口，mine 对标博主页） -->
  <template v-if="subTab === 'bloggerPage'">
    <BloggerProfileCard v-model="formState.spec.pages.blogger" />

    <div class=":uno: mt-6 space-y-4">
      <div class=":uno: rounded-lg bg-gray-50 p-3">
        <FormKit v-model="commonFeaturesMode" name="about_common_features_mode" label="常用功能显示方式" type="radio"
          :options="[
            { label: '网格（宫格图标）', value: 'grid' },
            { label: '列表（分行条目）', value: 'list' },
          ]" />
      </div>

      <!-- 常用功能入口 -->
      <QuickNavEditor v-model="formState.spec.pages.mine!.commonFeatures!" group-label="常用功能入口"
        :default-keys="DEFAULT_MY_PAGE_COMMON_KEYS" :custom-entries="customEntries"
        @create-custom="(entry) => setCustomEntries([...customEntries, entry])"
        @update-custom="(entry) => setCustomEntries(customEntries.map((it) => (it.key === entry.key ? { ...entry } : it)))"
        @delete-custom="(entry) => setCustomEntries(customEntries.filter((it) => it.key !== entry.key))" />

      <!-- 其他功能入口 -->
      <QuickNavEditor v-model="formState.spec.pages.mine!.otherFeatures!" group-label="其他功能入口"
        :default-keys="DEFAULT_MY_PAGE_OTHER_KEYS" :custom-entries="customEntries"
        @create-custom="(entry) => setCustomEntries([...customEntries, entry])"
        @update-custom="(entry) => setCustomEntries(customEntries.map((it) => (it.key === entry.key ? { ...entry } : it)))"
        @delete-custom="(entry) => setCustomEntries(customEntries.filter((it) => it.key !== entry.key))" />
    </div>
  </template>

  <!-- 首页（布局偏好 + 功能开关 + 快捷导航 + 精选分类） -->
  <template v-if="subTab === 'home'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      首页默认展示偏好与功能开关；布局偏好与小程序端「偏好设置-布局」首页分组对齐，用户可在小程序端覆盖。
    </p>
    <FormKit v-model="formState.spec.preferences.homeListLayout" name="pref_home_layout" label="首页列表布局"
      type="select" :options="[
        { label: '单列', value: 'single' },
        { label: '双列', value: 'double' },
      ]" help="首页笔记列表默认展示方式" />
    <FormKit v-model="formState.spec.preferences.homeCardType" name="pref_home_card_type" label="首页卡片样式"
      type="select" :options="CARD_STYLE_OPTIONS" help="封面图与文字的位置关系" />

    <FormKit v-model="formState.spec.pages.home.useHitokoto" name="home_use_hitokoto" label="显示一言"
      type="checkbox" help="依赖【轻言】插件" />
    <FormKit v-model="formState.spec.pages.home.useQuickNavigation" name="home_use_quick_navigation"
      label="显示快捷导航" type="checkbox" help="首页顶部快捷入口" />

    <div v-if="formState.spec.pages.home.useQuickNavigation" class=":uno: mt-4">
      <QuickNavEditor v-model="formState.spec.pages.home.quickNavigation!" group-label="快捷导航项"
        :default-keys="DEFAULT_QUICK_NAV_KEYS" :custom-entries="customEntries"
        @create-custom="(entry) => setCustomEntries([...customEntries, entry])"
        @update-custom="(entry) => setCustomEntries(customEntries.map((it) => (it.key === entry.key ? { ...entry } : it)))"
        @delete-custom="(entry) => setCustomEntries(customEntries.filter((it) => it.key !== entry.key))" />
    </div>

    <FormKit v-model="formState.spec.pages.home.useCategory" name="home_use_category" label="显示精选分类"
      type="checkbox" help="首页是否展示精选分类栏" />

    <!-- 精选分类栏（固定 3 个，快照含封面/名称，拖拽排序） -->
    <div v-if="formState.spec.pages.home.useCategory" class=":uno: mt-4 rounded-lg bg-gray-50 p-4">
      <div class="flex items-center justify-between">
        <div class="flex-1">
          <div class=":uno: mb-2 text-sm font-medium text-gray-700">分类栏展示（固定 3 个）</div>
          <p class=":uno: mb-3 text-xs text-gray-400">首页显示的分类，设置3个分类最佳。</p>
        </div>
        <div class="shrink-0">
          <VButton size="sm" type="secondary" @click="categoryModalVisible = true">选择分类</VButton>
        </div>
      </div>
      <div v-if="homeCategoriesSortable.length" class=":uno: mb-3">
        <VueDraggable v-model="homeCategoriesSortable" handle=".home-category-drag-handle">
          <div v-for="item in homeCategoriesSortable" :key="item.name"
            class=":uno: mb-2 flex items-center gap-3 rounded-md border border-gray-100 bg-white px-3 py-2 last:mb-0">
            <span class=":uno: home-category-drag-handle cursor-move text-gray-300 hover:text-gray-500">
              <RiDragMove2Line class=":uno: h-4 w-4" />
            </span>
            <div
              class=":uno: flex h-10 w-10 shrink-0 items-center justify-center overflow-hidden rounded-md bg-gray-100 text-base">
              <img v-if="item.cover" :src="item.cover" class=":uno: h-full w-full object-cover" alt="" />
              <RiImageLine v-else class=":uno: h-5 w-5 text-gray-300" />
            </div>
            <div class=":uno: min-w-0 flex-1">
              <div class=":uno: truncate text-sm font-medium text-gray-700">
                {{ item.displayName || item.name }}
              </div>
            </div>
            <button class=":uno: rounded p-1 text-gray-400 hover:bg-red-50 hover:text-red-500" title="移除该分类"
              @click="removeCategory(item)">
              <RiDeleteBinLine class=":uno: h-4 w-4" />
            </button>
          </div>
        </VueDraggable>
      </div>
    </div>
  </template>

  <!-- 笔记页 -->
  <template v-if="subTab === 'articles'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      笔记列表页默认展示偏好，与小程序端「偏好设置-布局」笔记列表分组对齐；用户可在小程序端覆盖。
    </p>
    <FormKit v-model="formState.spec.preferences.articlesListLayout" name="pref_articles_layout" label="笔记列表布局"
      type="select" :options="[
        { label: '单列', value: 'single' },
        { label: '双列', value: 'double' },
      ]" />
    <FormKit v-model="formState.spec.preferences.articlesCardType" name="pref_articles_card_type" label="笔记列表卡片样式"
      type="select" :options="CARD_STYLE_OPTIONS" />
  </template>

  <!-- 归档页 -->
  <template v-if="subTab === 'archives'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      笔记归档页默认展示偏好，与小程序端「偏好设置-布局」笔记归档分组对齐；用户可在小程序端覆盖。
    </p>
    <FormKit v-model="formState.spec.preferences.archivesListLayout" name="pref_archives_layout" label="笔记归档布局"
      type="select" :options="[
        { label: '单列', value: 'single' },
        { label: '双列', value: 'double' },
      ]" />
    <FormKit v-model="formState.spec.preferences.archivesCardType" name="pref_archives_card_type" label="笔记归档卡片样式"
      type="select" :options="CARD_STYLE_OPTIONS" />
  </template>

  <!-- 分类页 -->
  <template v-if="subTab === 'categoryArticles'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      分类笔记页默认展示偏好，与小程序端「偏好设置-布局」分类笔记分组对齐；用户可在小程序端覆盖。
    </p>
    <FormKit v-model="formState.spec.preferences.categoryArticlesListLayout" name="pref_category_articles_layout"
      label="分类笔记布局" type="select" :options="[
        { label: '单列', value: 'single' },
        { label: '双列', value: 'double' },
      ]" />
    <FormKit v-model="formState.spec.preferences.categoryArticlesCardType" name="pref_category_articles_card_type"
      label="分类笔记卡片样式" type="select" :options="CARD_STYLE_OPTIONS" />
  </template>

  <!-- 标签页 -->
  <template v-if="subTab === 'tagArticles'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      标签笔记页默认展示偏好，与小程序端「偏好设置-布局」标签笔记分组对齐；用户可在小程序端覆盖。
    </p>
    <FormKit v-model="formState.spec.preferences.tagArticlesListLayout" name="pref_tag_articles_layout" label="标签笔记布局"
      type="select" :options="[
        { label: '单列', value: 'single' },
        { label: '双列', value: 'double' },
      ]" />
    <FormKit v-model="formState.spec.preferences.tagArticlesCardType" name="pref_tag_articles_card_type"
      label="标签笔记卡片样式" type="select" :options="CARD_STYLE_OPTIONS" />
  </template>

  <!-- 友链页 -->
  <template v-if="subTab === 'friendLinks'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      友情链接页展示偏好；小程序打开模式决定用户点击小程序链接时的打开方式。
    </p>
    <FormKit v-model="formState.spec.preferences.linkPage!.miniProgramOpenMode" name="pref_link_page_open_mode"
      label="小程序友链打开模式" type="select" :options="[
        { label: '全屏', value: 'fullscreen' },
        { label: '半屏', value: 'halfScreen' },
      ]" />
  </template>

  <!-- 内容页 -->
  <template v-if="subTab === 'content'">
    <CommentSwitchGroup v-model:moment="formState.spec.pages.moment!"
      v-model:post-detail="formState.spec.pages.postDetail!" />
    <div class=":uno: mt-4 rounded-lg bg-gray-50 p-4">
      <SwitchRow v-model="formState.spec.pages.postDetail!.copyrightEnabled!" title="显示笔记版权"
        description="笔记底部是否展示版权声明" />
      <div class=":uno: mt-4">
        <FormKit v-model="formState.spec.pages.postDetail!.copyrightAuthor" name="post_copyright_author" label="笔记版权作者"
          type="text" />
        <FormKit v-model="formState.spec.pages.postDetail!.copyrightDesc" name="post_copyright_desc" label="笔记版权描述"
          type="textarea" />
        <FormKit v-model="formState.spec.pages.postDetail!.copyrightViolation" name="post_copyright_violation"
          label="笔记侵权说明" type="textarea" />
      </div>
    </div>
  </template>

  <!-- 精选分类候选弹窗（固定 3 个） -->
  <AuditCandidatesModal v-model:visible="categoryModalVisible" type="category" :selected="categoryModalSelected"
    :max="3" @confirm="handleCategoryConfirm" />
</template>
