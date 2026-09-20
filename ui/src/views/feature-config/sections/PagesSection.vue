<script setup lang="ts">
import {Dialog, Toast, VButton, VEmpty, VSpace, VSwitch} from "@halo-dev/components";
import {computed, inject, ref} from "vue";
import {VueDraggable} from "vue-draggable-plus";
import RiDragMove2Line from "~icons/ri/drag-move-2-line";
import RiArrowDownSLine from "~icons/ri/arrow-down-s-line";
import RiArrowUpSLine from "~icons/ri/arrow-up-s-line";
import RiDeleteBinLine from "~icons/ri/delete-bin-6-line";
import RiImageLine from "~icons/ri/image-line";
import RichTextEditorField from "@/components/common/RichTextEditorField.vue";
import AuditCandidatesModal from "@/components/audit-config/AuditCandidatesModal.vue";
import FeatureEntryCandidatesModal from "@/components/feature-config/FeatureEntryCandidatesModal.vue";
import {FeatureConfigFormKey} from "@/views/feature-config/form-context";
import {
  DEFAULT_MY_PAGE_COMMON_KEYS,
  DEFAULT_MY_PAGE_OTHER_KEYS,
  DEFAULT_QUICK_NAV_KEYS,
  featureEntriesByKeys,
  toQuickNavigationItem,
  type FeatureEntry,
} from "@/constant/feature-entries";
import type {
  AuditDataRef,
  FeatureConfigCategoryItem,
  FeatureConfigMinePage,
  FeatureConfigQuickNavigationItem,
} from "@/types";

/**
 * 页面设置分区
 * 页面标题（全站页面标题统一维护）/ 首页（快捷导航逐项配置 + 分类栏固定 3 个）/
 * 博主页（资料卡视觉与功能入口两组）/ 文章详情页 / 免责声明页。
 */
defineProps<{ subTab: string }>();

const { formState } = inject(FeatureConfigFormKey)!;

/** 页面标题分组定义（key → 标题项列表；group=分组名，展示顺序即数组顺序） */
const PAGE_TITLE_GROUPS: Array<{ key: string; label: string; desc: string; items: Array<{ key: keyof typeof pageTitleBindings; label: string }> }> = [
  {
    key: 'tabbar',
    label: '主导航页面',
    desc: '底部主导航 5 个页面',
    items: [
      { key: 'home', label: '首页' },
      { key: 'gallery', label: '图库页' },
      { key: 'category', label: '分类页' },
      { key: 'moments', label: '瞬间页' },
      { key: 'blogger', label: '博主页' },
    ],
  },
  {
    key: 'blog',
    label: '博客页面',
    desc: '博客内容相关页面',
    items: [
      { key: 'articles', label: '笔记列表页' },
      { key: 'archives', label: '笔记归档页' },
      { key: 'postDetail', label: '笔记详情页' },
      { key: 'categoryArticles', label: '分类笔记页' },
      { key: 'tags', label: '标签列表页' },
      { key: 'tagArticles', label: '标签笔记页' },
      { key: 'search', label: '搜索页' },
      { key: 'favorites', label: '我的收藏页' },
      { key: 'friendLinks', label: '友情链接页' },
      { key: 'notice', label: '公告中心页' },
      { key: 'noticeDetail', label: '公告详情页' },
      { key: 'votes', label: '投票中心页' },
      { key: 'voteDetail', label: '投票详情页' },
      { key: 'contact', label: '联系博主页' },
    ],
  },
  {
    key: 'admin',
    label: '管理页面',
    desc: '偏好、数据与项目信息等管理类页面',
    items: [
      { key: 'setting', label: '偏好设置页' },
      { key: 'aboutProject', label: '关于项目页' },
      { key: 'disclaimer', label: '免责声明页' },
      { key: 'dataVisual', label: '数据看板页' },
      { key: 'login', label: '登录页' },
      { key: 'register', label: '注册页' },
      { key: 'userAgreement', label: '用户协议页' },
      { key: 'privacyPolicy', label: '隐私政策页' },
    ],
  },
];

/** 页面标题绑定取值器（key → spec.pages.titles 字段访问路径；formState 为 ref，取值须经 .value） */
const pageTitleBindings = {
  home: () => formState.value.spec.pages.titles?.home,
  gallery: () => formState.value.spec.pages.titles?.gallery,
  category: () => formState.value.spec.pages.titles?.category,
  moments: () => formState.value.spec.pages.titles?.moments,
  blogger: () => formState.value.spec.pages.titles?.blogger,
  articles: () => formState.value.spec.pages.titles?.articles,
  archives: () => formState.value.spec.pages.titles?.archives,
  postDetail: () => formState.value.spec.pages.titles?.postDetail,
  categoryArticles: () => formState.value.spec.pages.titles?.categoryArticles,
  tags: () => formState.value.spec.pages.titles?.tags,
  tagArticles: () => formState.value.spec.pages.titles?.tagArticles,
  search: () => formState.value.spec.pages.titles?.search,
  favorites: () => formState.value.spec.pages.titles?.favorites,
  friendLinks: () => formState.value.spec.pages.titles?.friendLinks,
  notice: () => formState.value.spec.pages.titles?.notice,
  noticeDetail: () => formState.value.spec.pages.titles?.noticeDetail,
  votes: () => formState.value.spec.pages.titles?.votes,
  voteDetail: () => formState.value.spec.pages.titles?.voteDetail,
  contact: () => formState.value.spec.pages.titles?.contact,
  setting: () => formState.value.spec.pages.titles?.setting,
  aboutProject: () => formState.value.spec.pages.titles?.aboutProject,
  disclaimer: () => formState.value.spec.pages.titles?.disclaimer,
  dataVisual: () => formState.value.spec.pages.titles?.dataVisual,
  login: () => formState.value.spec.pages.titles?.login,
  register: () => formState.value.spec.pages.titles?.register,
  userAgreement: () => formState.value.spec.pages.titles?.userAgreement,
  privacyPolicy: () => formState.value.spec.pages.titles?.privacyPolicy,
} as const;

/** 页面标题命名版本（「原始标题」= 默认值；其余为风格化命名方案，点击批量填充 titles） */
const TITLE_NAMING_VERSIONS: Array<{ key: string; label: string; values: Record<string, string> }> = [
  {
    key: 'original',
    label: '默认',
    values: {
      home: '首页', gallery: '图库', category: '分类', moments: '瞬间', blogger: '博主',
      articles: '笔记', archives: '笔记归档', postDetail: '笔记详情', categoryArticles: '分类笔记',
      tags: '标签', tagArticles: '标签笔记', search: '搜索', favorites: '我的收藏',
      friendLinks: '友情链接', notice: '公告中心', noticeDetail: '公告详情',
      votes: '投票中心', voteDetail: '投票详情', contact: '联系博主', setting: '偏好设置',
      aboutProject: '关于项目', disclaimer: '免责声明', dataVisual: '数据看板',
      login: '登录', register: '注册', userAgreement: '用户协议', privacyPolicy: '隐私政策',
    },
  },
  {
    key: 'shuju',
    label: '书卷雅集',
    values: {
      home: '开篇叙章', gallery: '影绘藏珍', category: '篇目分辑', moments: '流光碎影', blogger: '执笔者叙',
      articles: '随笔札记', archives: '旧卷存藏', postDetail: '札记细览', categoryArticles: '分辑札记',
      tags: '文印标记', tagArticles: '印记札记', search: '寻文觅迹', favorites: '撷珍藏录',
      friendLinks: '友邻书驿', notice: '公启通告', noticeDetail: '通告细阅',
      votes: '众议征询', voteDetail: '众议细览', contact: '尺素传书', setting: '雅趣自调',
      aboutProject: '缘起叙记', disclaimer: '规约叙言', dataVisual: '观数览势',
      login: '入阁登览', register: '入籍结缘', userAgreement: '共守约章', privacyPolicy: '私籍守章',
    },
  },
  {
    key: 'qingjian',
    label: '清简静致',
    values: {
      home: '万象始见', gallery: '光影留痕', category: '物以类归', moments: '刹那时光', blogger: '执笔之人',
      articles: '书录闲言', archives: '岁月存笺', postDetail: '阅览全文', categoryArticles: '类目阅文',
      tags: '笺上留痕', tagArticles: '随痕阅文', search: '搜览寻踪', favorites: '心藏好物',
      friendLinks: '远友相望', notice: '布告通知', noticeDetail: '阅览告示',
      votes: '民意共商', voteDetail: '共商详情', contact: '寄言留言', setting: '个性定制',
      aboutProject: '本站小叙', disclaimer: '权责申明', dataVisual: '数览概观',
      login: '登入本站', register: '新客登记', userAgreement: '用者之约', privacyPolicy: '私权规约',
    },
  },
  {
    key: 'fengwu',
    label: '风物诗意',
    values: {
      home: '山舍门庭', gallery: '山野撷影', category: '溪路分途', moments: '风露一瞬', blogger: '守园书斋',
      articles: '林间书简', archives: '年轮藏册', postDetail: '书简细读', categoryArticles: '溪径书简',
      tags: '叶脉题字', tagArticles: '叶上观书', search: '探径寻书', favorites: '拾珍入匣',
      friendLinks: '遥灯相映', notice: '庭前传讯', noticeDetail: '讯笺细读',
      votes: '衡议听言', voteDetail: '衡论细读', contact: '飞雁传笺', setting: '四时调序',
      aboutProject: '种源溯源', disclaimer: '约言明戒', dataVisual: '星野观览',
      login: '推门入舍', register: '筑舍安家', userAgreement: '同心之契', privacyPolicy: '清规护隐',
    },
  },
  {
    key: 'xiandai',
    label: '现代雅致',
    values: {
      home: '站点首页', gallery: '影像图库', category: '内容分类', moments: '片刻纪事', blogger: '作者主页',
      articles: '随笔笔记', archives: '资料归档', postDetail: '笔记详情', categoryArticles: '分类随笔',
      tags: '内容标签', tagArticles: '标签随笔', search: '全站检索', favorites: '个人收藏',
      friendLinks: '友链站点', notice: '公告中心', noticeDetail: '公告详情',
      votes: '投票专区', voteDetail: '投票详情', contact: '联络博主', setting: '偏好设置',
      aboutProject: '项目简介', disclaimer: '免责声明', dataVisual: '数据总览',
      login: '账号登录', register: '账号注册', userAgreement: '用户规约', privacyPolicy: '隐私规约',
    },
  },
  {
    key: 'jiansu',
    label: '简素哲思',
    values: {
      home: '归处初临', gallery: '万象留观', category: '万象分门', moments: '瞬念存记', blogger: '执笔者言',
      articles: '片言存录', archives: '往迹留存', postDetail: '片语深读', categoryArticles: '分门阅录',
      tags: '印记标识', tagArticles: '印记寻文', search: '觅迹寻章', favorites: '所爱存藏',
      friendLinks: '四方同好', notice: '讯息通告', noticeDetail: '讯息详读',
      votes: '众言表决', voteDetail: '表决详阅', contact: '寄语相询', setting: '私趣自定',
      aboutProject: '初心述记', disclaimer: '条例申明', dataVisual: '万象数观',
      login: '凭籍登入', register: '开户结缘', userAgreement: '缔约守则', privacyPolicy: '私权明叙',
    },
  },
];

/** 各分组当前选中的命名版本（group key → version key；仅前端记忆，不落库） */
const activeTitleVersions = ref<Record<string, string>>(
  Object.fromEntries(PAGE_TITLE_GROUPS.map((group) => [group.key, 'original']))
);

/** 应用命名版本到指定分组：仅填充该分组内的 titles 项（空串 = 恢复留空回退） */
const applyTitleVersion = (
  group: { key: string; label: string; items: Array<{ key: keyof typeof pageTitleBindings; label: string }> },
  version: { key: string; label: string; values: Record<string, string> }
) => {
  const titles = (formState.value.spec.pages.titles ??= {} as Record<string, string | undefined>) as Record<string, string | undefined>;
  group.items.forEach((item) => {
    titles[item.key] = version.values[item.key] ?? '';
  });
  activeTitleVersions.value[group.key] = version.key;
  Toast.success(`已应用「${version.label}」命名`);
};

/** 各分组折叠状态（group key → 是否折叠；默认全部折叠） */
const collapsedTitleGroups = ref<Record<string, boolean>>(
  Object.fromEntries(PAGE_TITLE_GROUPS.map((group) => [group.key, true]))
);

/** 切换某分组卡片的折叠状态 */
function toggleTitleGroupCollapse(key: string) {
  collapsedTitleGroups.value[key] = !collapsedTitleGroups.value[key];
}

/**
 * 快捷导航默认 5 项（由注册表显式 key 列表派生——
 * love/contact-blogger/favorites/friend-links/about，与 app 端 uh-home-quick-nav 默认一致）
 */
const DEFAULT_QUICK_NAVIGATION: FeatureConfigQuickNavigationItem[] =
  featureEntriesByKeys(DEFAULT_QUICK_NAV_KEYS).map(toQuickNavigationItem);

/** 首页快捷导航「添加」候选弹窗（统一清单：展示全部注册表条目，已配置置灰禁选，确认后追加） */
const quickNavModalVisible = ref(false);

/** 快捷导航项列表（写回 formState；VueDraggable 需要非 undefined 数组） */
const homeQuickNavigation = computed({
  get: () => formState.value.spec.pages.home.quickNavigation || [],
  set: (value: FeatureConfigQuickNavigationItem[]) => {
    formState.value.spec.pages.home.quickNavigation = value;
  },
});

/** 候选弹窗确认：选中的注册表条目快照追加到快捷导航列表尾部 */
const handleQuickNavConfirm = (selected: FeatureEntry[]) => {
  const list = formState.value.spec.pages.home.quickNavigation || [];
  formState.value.spec.pages.home.quickNavigation = [
    ...list,
    ...selected.map(toQuickNavigationItem),
  ];
  quickNavModalVisible.value = false;
};

/** 删除某项快捷导航 */
const removeQuickNavItem = (item: FeatureConfigQuickNavigationItem) => {
  const list = formState.value.spec.pages.home.quickNavigation || [];
  formState.value.spec.pages.home.quickNavigation = list.filter(
    (it) => it.key !== item.key
  );
};

/** 「恢复默认」：确认后恢复为候选弹窗注册表 home 组的默认配置（全部快照字段、排序/visible 一并恢复） */
function restoreQuickNavigationDefaults() {
  Dialog.warning({
    title: "恢复默认",
    description: "将全部快捷导航项恢复为默认配置，确定继续吗？",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: () => {
      formState.value.spec.pages.home.quickNavigation = DEFAULT_QUICK_NAVIGATION.map(
        (item) => ({...item})
      );
      Toast.success("已恢复默认");
    },
  });
}

// ===== 首页分类栏选择（固定 3 个，复用审核模式候选弹窗 AuditCandidatesModal）=====

const categoryModalVisible = ref(false);

/** 首页分类栏已选引用（FeatureConfigCategoryItem：name + displayName + cover 快照） */
const homeCategories = computed(
  () => formState.value.spec.pages.home.categories || []
);

/** 回显给候选弹窗的 AuditDataRef 形态（title ← displayName、cover ← cover、priority/postCount ← 快照） */
const categoryModalSelected = computed<AuditDataRef[]>(() =>
  homeCategories.value.map((item) => ({
    name: item.name || "",
    title: item.displayName,
    cover: item.cover,
    priority: item.priority,
    postCount: item.postCount,
  }))
);

/** 分类栏已选快照列表（写回 formState；VueDraggable 拖拽排序，顺序 = 展示顺序） */
const homeCategoriesSortable = computed({
  get: () => formState.value.spec.pages.home.categories || [],
  set: (value: FeatureConfigCategoryItem[]) => {
    formState.value.spec.pages.home.categories = value;
  },
});

/** 弹窗确认：映射回 {name, displayName, cover, priority, postCount} 快照并关闭（候选弹窗已限制最多 3 个） */
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
  const list = formState.value.spec.pages.home.categories || [];
  formState.value.spec.pages.home.categories = list.filter(
    (it) => it.name !== item.name
  );
};

// ===== 快捷导航项/功能入口共用编辑工具（仅维护名称/背景色/显示，key 固定、拖拽排序） =====

/** FormKit type="color" 回显值：空值兜底，rgba/hex 原样透传（保留透明度供 Sketch 滑块回显） */
function toColorInput(value?: string): string {
  return value || "#cccccc";
}

/** FormKit type="color" 选色（format="hex8" 输出 #rrggbbaa 含透明度）写回 color（文字颜色，app 端直接读该色值渲染文字/图标） */
function onNavColor(item: FeatureConfigQuickNavigationItem, value: unknown) {
  if (typeof value === "string") {
    item.color = value;
  }
}

/** FormKit type="color" 选色（format="hex8" 输出 #rrggbbaa 含透明度）写回 bgColor（图标背景色，app 端直接读该色值渲染） */
function onNavBgColor(item: FeatureConfigQuickNavigationItem, value: unknown) {
  if (typeof value === "string") {
    item.bgColor = value;
  }
}

// ===== 我的页面功能入口（常用功能/其他功能两组） =====

/** 我的页面 mine 安全访问（defaultSpec 已含两组默认，旧数据可能缺失） */
const mine = computed<FeatureConfigMinePage>(
  () => formState.value.spec.pages.mine || {commonFeatures: [], otherFeatures: []}
);

/** 常用功能显示方式（grid=宫格 / list=列表；旧数据字段缺失时表单回显 grid，与 app 端兜底一致） */
const commonFeaturesMode = computed({
  get: () => formState.value.spec.pages.blogger.commonFeaturesMode || "grid",
  set: (value: "grid" | "list") => {
    formState.value.spec.pages.blogger.commonFeaturesMode = value;
  },
});

/** 两组列表（写回 formState；VueDraggable 需要非 undefined 数组） */
const mineCommonFeatures = computed({
  get: () => mine.value.commonFeatures || [],
  set: (value: FeatureConfigQuickNavigationItem[]) => {
    mine.value.commonFeatures = value;
  },
});

const mineOtherFeatures = computed({
  get: () => mine.value.otherFeatures || [],
  set: (value: FeatureConfigQuickNavigationItem[]) => {
    mine.value.otherFeatures = value;
  },
});

/** 候选弹窗：当前打开的分组（common=常用功能 / other=其他功能；null=关闭） */
const mineModalGroup = ref<"common" | "other" | null>(null);

/** 候选弹窗确认：选中的注册表条目快照追加到对应分组列表尾部 */
const handleMineConfirm = (selected: FeatureEntry[], group: "common" | "other") => {
  const list = group === "common"
    ? mineCommonFeatures.value
    : mineOtherFeatures.value;
  const appended = [...list, ...selected.map(toQuickNavigationItem)];
  if (group === "common") {
    mineCommonFeatures.value = appended;
  } else {
    mineOtherFeatures.value = appended;
  }
  mineModalGroup.value = null;
};

/** 删除对应分组中的某项 */
const removeMineFeature = (group: "common" | "other", item: FeatureConfigQuickNavigationItem) => {
  const list = group === "common"
    ? mineCommonFeatures.value
    : mineOtherFeatures.value;
  const filtered = list.filter((it) => it.key !== item.key);
  if (group === "common") {
    mineCommonFeatures.value = filtered;
  } else {
    mineOtherFeatures.value = filtered;
  }
};

/** 恢复默认：恢复为注册表对应组的默认配置（全部快照字段、排序/visible 一并恢复；
 * 对齐 app 端 about.vue navList：常用 8 项 / 其他 3 项，由显式 key 列表派生） */
function restoreMineDefaults(group: "common" | "other") {
  const label = group === "common" ? "常用功能" : "其他功能";
  Dialog.warning({
    title: "恢复默认",
    description: `将「${label}」恢复为默认配置，确定继续吗？`,
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: () => {
      const defaults = featureEntriesByKeys(
        group === "common" ? DEFAULT_MY_PAGE_COMMON_KEYS : DEFAULT_MY_PAGE_OTHER_KEYS
      ).map(toQuickNavigationItem);
      if (group === "common") {
        mineCommonFeatures.value = defaults;
      } else {
        mineOtherFeatures.value = defaults;
      }
      Toast.success("已恢复默认");
    },
  });
}
</script>

<template>
  <!-- 页面与排版 → 页面标题（全站页面标题统一维护，app 端传入各页面 uh-navbar default-title，留空回退内置默认；
       分组折叠交互对齐恋爱设置-模块入口） -->
  <template v-if="subTab === 'pageTitles'">
    <div class=":uno: space-y-3">
      <div v-for="group in PAGE_TITLE_GROUPS" :key="group.key" class=":uno: rounded-lg bg-gray-50 p-4">
        <div class=":uno: flex items-center justify-between gap-4 border-b border-gray-100 pb-3">
          <div>
            <div class=":uno: text-sm font-bold text-gray-700">{{ group.label }}</div>
            <div class=":uno: mt-0.5 text-xs text-gray-400">{{ group.desc }}</div>
          </div>
          <VSpace class=":uno: flex-wrap justify-end">
            <VButton
              v-for="version in TITLE_NAMING_VERSIONS"
              :key="version.key"
              size="sm"
              :type="activeTitleVersions[group.key] === version.key ? 'primary' : 'default'"
              class=":uno: !py-1 !px-2.5 rounded-full"
              @click="applyTitleVersion(group, version)"
            >
              {{ version.label }}
            </VButton>
            <VButton size="sm" type="secondary" plain class=":uno: !py-1.5 !px-3 rounded-full"
              :title="collapsedTitleGroups[group.key] ? '展开配置' : '折叠配置'"
              @click="toggleTitleGroupCollapse(group.key)">
              <span class=":uno: flex items-center justify-center gap-x-1 my-auto">
                <span>{{ collapsedTitleGroups[group.key] ? '展开配置' : '折叠配置' }} </span>
                <RiArrowDownSLine v-if="collapsedTitleGroups[group.key]" class=":uno: text-base" />
                <RiArrowUpSLine v-else class=":uno: text-base" />
              </span>
            </VButton>
          </VSpace>
        </div>

        <div v-show="!collapsedTitleGroups[group.key]" class=":uno: pt-1">
          <div
            v-for="item in group.items"
            :key="item.key"
            class=":uno: flex items-center justify-between gap-4 border-b border-gray-100 py-2 last:border-b-0"
          >
            <div class=":uno: w-28 shrink-0 text-sm leading-8 text-gray-700">{{ item.label }}</div>
            <FormKit
              :model-value="pageTitleBindings[item.key]()"
              :name="`titles_${item.key}`"
              type="text"
              :classes="{outer: 'flex-1 min-w-0 !mb-0', input: '!py-1.5'}"
              placeholder="留空使用默认标题"
              @update:model-value="(value: unknown) => { const titles = formState.spec.pages.titles as Record<string, string | undefined> | undefined; if (titles) { titles[item.key] = typeof value === 'string' ? value : undefined } }"
            />
          </div>
        </div>
      </div>
    </div>
  </template>

  <!-- 页面与排版 → 首页 -->
  <template v-if="subTab === 'home'">
    <div class=":uno: flex items-center justify-between gap-4 border-b border-gray-100 py-3">
      <div>
        <div class=":uno: text-sm text-gray-700">显示快捷导航</div>
        <div class=":uno: mt-0.5 text-xs text-gray-400">首页顶部快捷入口</div>
      </div>
      <VSwitch v-model="formState.spec.pages.home.useQuickNavigation" />
    </div>

    <!-- 快捷导航项配置（一行紧凑：标识 key 禁用 + 名称左右 label + 背景色颜色选择 + 显示开关，拖拽排序） -->
    <div
      v-if="formState.spec.pages.home.useQuickNavigation"
      class=":uno: mt-4 rounded-lg bg-gray-50 p-4"
    >
      <div class=":uno: mb-2 flex items-center justify-between">
        <div class=":uno: text-sm font-medium text-gray-700">快捷导航项</div>
        <VSpace>
          <VButton size="sm" type="secondary" @click="quickNavModalVisible = true">添加</VButton>
          <VButton size="sm" type="secondary" @click="restoreQuickNavigationDefaults">恢复默认</VButton>
        </VSpace>
      </div>
      <p class=":uno: mb-3 text-xs text-gray-400">
        拖拽排序，顺序即首页展示顺序。
      </p>
      <VueDraggable
        v-model="homeQuickNavigation"
        handle=".nav-drag-handle"
      >
        <div
          v-for="(item, index) in homeQuickNavigation"
          :key="index"
          class=":uno: mb-2"
        >
          <div class=":uno: flex flex-wrap items-center gap-x-4 gap-y-2 rounded-lg border border-gray-100 bg-white px-3 py-2">
            <span class=":uno: nav-drag-handle cursor-move shrink-0 text-gray-400 hover:text-gray-600">
              <RiDragMove2Line class=":uno: h-4 w-4" />
            </span>
            <!-- 名称 -->
            <div class=":uno: flex min-w-0 flex-1 items-center gap-2">
              <span class=":uno: w-10 shrink-0 text-xs text-gray-700">名称</span>
              <FormKit
                v-model="item.title"
                :name="`nav_title_${index}`"
                type="text"
                placeholder="导航名称"
                outer-class=":uno: min-w-0 flex-1 !pt-0"
              />
            </div>
            <!-- 文字颜色  -->
            <div class=":uno: flex shrink-0 items-center pr-24 gap-2">
              <span class=":uno: w-14 shrink-0 text-xs text-gray-700">文字颜色</span>
              <FormKit
                type="color"
                format="hex8"
                :model-value="toColorInput(item.color)"
                @update:model-value="onNavColor(item, $event)"
                outer-class=":uno: w-12 shrink-0 !pt-0"
              />
            </div>
            <div class=":uno: flex shrink-0 items-center pr-24 gap-2">
              <span class=":uno: w-16 shrink-0 text-xs text-gray-700">图标背景色</span>
              <FormKit
                type="color"
                format="hex8"
                :model-value="toColorInput(item.bgColor)"
                @update:model-value="onNavBgColor(item, $event)"
                outer-class=":uno: w-12 shrink-0 !pt-0"
              />
            </div>
            <!-- 显示开关 -->
            <div class=":uno: flex shrink-0 items-center gap-2 text-xs text-gray-500">
              <span>显示</span>
              <VSwitch v-model="item.visible" />
            </div>
            <!-- 删除 -->
            <button
              type="button"
              class=":uno: shrink-0 text-gray-400 transition-all hover:text-red-600"
              title="删除"
              @click="removeQuickNavItem(item)"
            >
              <RiDeleteBinLine class=":uno: h-4 w-4" />
            </button>
          </div>
        </div>
      </VueDraggable>
    </div>

    <div class=":uno: flex items-center justify-between gap-4 border-b border-gray-100 py-3">
      <div>
        <div class=":uno: text-sm text-gray-700">显示精选分类</div>
        <div class=":uno: mt-0.5 text-xs text-gray-400">首页是否展示精选分类栏</div>
      </div>
      <VSwitch v-model="formState.spec.pages.home.useCategory" />
    </div>

    <!-- 首页分类栏选中（固定 3 个，快照含封面/名称，拖拽排序；复用审核模式候选弹窗 AuditCandidatesModal） -->
    <div
      v-if="formState.spec.pages.home.useCategory"
      class=":uno: mt-4 rounded-lg bg-gray-50 p-4"
    >
      <div class="flex items-center justify-between">
        <div class="flex-1">
          <div class=":uno: mb-2 text-sm font-medium text-gray-700">分类栏展示（固定 3 个）</div>
           <p class=":uno: mb-3 text-xs text-gray-400">
             首页显示的分类，设置3个分类最佳。
           </p>
        </div>
        <div class="shrink-0">
          <VButton size="sm" type="secondary" @click="categoryModalVisible = true">
            选择分类
          </VButton>
        </div>
      </div>
      <div v-if="homeCategoriesSortable.length" class=":uno: mb-3">
        <VueDraggable
          v-model="homeCategoriesSortable"
          handle=".home-category-drag-handle"
        >
          <div
            v-for="item in homeCategoriesSortable"
            :key="item.name"
            class=":uno: mb-2 flex items-center gap-3 rounded-md border border-gray-100 bg-white px-3 py-2 last:mb-0"
          >
            <span class=":uno: home-category-drag-handle cursor-move text-gray-300 hover:text-gray-500">
              <RiDragMove2Line class=":uno: h-4 w-4" />
            </span>
            <div class=":uno: flex h-10 w-10 shrink-0 items-center justify-center overflow-hidden rounded-md bg-gray-100 text-base">
              <img
                v-if="item.cover"
                :src="item.cover"
                class=":uno: h-full w-full object-cover"
                alt=""
              />
              <RiImageLine v-else class=":uno: h-5 w-5 text-gray-300" />
            </div>
            <div class=":uno: min-w-0 flex-1">
              <div class=":uno: truncate text-sm font-medium text-gray-700">
                {{ item.displayName || item.name }}
              </div>
            </div>
            <button
              class=":uno: rounded p-1 text-gray-400 hover:bg-red-50 hover:text-red-500"
              title="移除该分类"
              @click="removeCategory(item)"
            >
              <RiDeleteBinLine class=":uno: h-4 w-4" />
            </button>
          </div>
        </VueDraggable>
      </div>

    </div>
  </template>

  <!-- 页面与排版 → 博主页（资料卡视觉 + 功能入口布局） -->
  <template v-if="subTab === 'blogger'">
    <div class=":uno: flex flex-col gap-y-4 gap-x-12 md:flex-row">
      <div class=":uno: min-w-0 shrink-0">
        <FormKit v-model="formState.spec.pages.blogger.bgImageUrl" name="about_bg_image" label="资料卡背景图" type="attachment" :accepts="['image/*']" />
      </div>
      <div class=":uno: min-w-0 shrink-0">
        <FormKit v-model="formState.spec.pages.blogger.waveImageUrl" name="about_wave_image" label="资料卡波浪图" type="attachment" :accepts="['image/*']" />
      </div>
    </div>

    <!-- 功能入口 -->
    <div class=":uno: mt-6 rounded-lg bg-gray-50 p-4">
      <div class=":uno: mb-2 text-sm font-medium text-gray-700">功能入口</div>
      <p class=":uno: mb-3 text-xs text-gray-400">
        「我的」页面（about）展示的功能入口，分组配置；拖拽排序，顺序即展示顺序。
      </p>

      <!-- 常用功能显示方式（grid=宫格 / list=列表，控制 app 端关于页常用功能布局） -->
      <div class=":uno: mb-4 rounded-lg bg-white p-3">
        <FormKit
          v-model="commonFeaturesMode"
          name="about_common_features_mode"
          label="常用功能显示方式"
          type="radio"
          :options="[
            {label: '网格（宫格图标）', value: 'grid'},
            {label: '列表（分行条目）', value: 'list'},
          ]"
          help="控制 app 端「我的/关于页」常用功能的展示布局；切换为列表时常用于功能较多、需要展示副标题的场景。"
        />
      </div>

      <!-- 常用功能（原「博客功能」改名） -->
      <div class=":uno: rounded-lg bg-white p-3">
        <div class=":uno: mb-2 flex items-center justify-between">
          <div class=":uno: text-sm font-medium text-gray-700">常用功能</div>
          <VSpace>
            <VButton size="sm" type="secondary" @click="mineModalGroup = 'common'">添加</VButton>
            <VButton size="sm" type="secondary" @click="restoreMineDefaults('common')">恢复默认</VButton>
          </VSpace>
        </div>
        <VueDraggable v-model="mineCommonFeatures" handle=".nav-drag-handle">
          <div
            v-for="(item, index) in mineCommonFeatures"
            :key="index"
            class=":uno: mb-2 last:mb-0"
          >
            <div class=":uno: flex flex-wrap items-center gap-x-3 gap-y-2 rounded-lg border border-gray-100 bg-gray-50 px-3 py-2">
              <span class=":uno: nav-drag-handle cursor-move shrink-0 text-gray-400 hover:text-gray-600">
                <RiDragMove2Line class=":uno: h-4 w-4" />
              </span>
              <!-- 名称 -->
              <div class=":uno: flex min-w-0 flex-1 items-center gap-2">
                <span class=":uno: w-10 shrink-0 text-xs text-gray-700">名称</span>
                <FormKit
                  v-model="item.title"
                  :name="`mypage_common_title_${index}`"
                  type="text"
                  placeholder="导航名称"
                  outer-class=":uno: min-w-0 flex-1 !pt-0"
                />
              </div>
              <!-- 提示（subTitle，app 端展示为功能入口副标题） -->
              <div class=":uno: flex min-w-0 flex-1 items-center gap-2">
                <span class=":uno: w-10 shrink-0 text-xs text-gray-700">提示</span>
                <FormKit
                  v-model="item.subTitle"
                  :name="`mypage_common_subtitle_${index}`"
                  type="text"
                  placeholder="如 博主常用联系方式"
                  outer-class=":uno: min-w-0 flex-1 !pt-0"
                />
              </div>
              <!-- 文字颜色 + 图标背景色 -->
              <div class=":uno: flex shrink-0 items-center gap-2 pr-24">
                <span class=":uno: w-14 shrink-0 text-xs text-gray-700">文字颜色</span>
                <FormKit
                  type="color"
                  format="hex8"
                  :model-value="toColorInput(item.color)"
                  @update:model-value="onNavColor(item, $event)"
                  outer-class=":uno: w-12 shrink-0 !pt-0"
                />
              </div>
              <div class=":uno: flex shrink-0 items-center gap-2 pr-24">
                <span class=":uno: w-16 shrink-0 text-xs text-gray-700">图标背景色</span>
                <FormKit
                  type="color"
                  format="hex8"
                  :model-value="toColorInput(item.bgColor)"
                  @update:model-value="onNavBgColor(item, $event)"
                  outer-class=":uno: w-12 shrink-0 !pt-0"
                />
              </div>
              <div class=":uno: flex shrink-0 items-center gap-2 text-xs text-gray-500">
                <span>显示</span>
                <VSwitch v-model="item.visible" />
              </div>
              <button
                type="button"
                class=":uno: shrink-0 text-gray-400 transition-all hover:text-red-600"
                title="删除"
                @click="removeMineFeature('common', item)"
              >
                <RiDeleteBinLine class=":uno: h-4 w-4" />
              </button>
            </div>
          </div>
        </VueDraggable>
        <VEmpty
          v-if="!mineCommonFeatures.length"
          title="暂无功能入口"
          message="点击「添加」选择功能"
          :class="':uno: py-6'"
        />
      </div>

      <!-- 其他功能 -->
      <div class=":uno: mt-4 rounded-lg bg-white p-3">
        <div class=":uno: mb-2 flex items-center justify-between">
          <div class=":uno: text-sm font-medium text-gray-700">其他功能</div>
          <VSpace>
            <VButton size="sm" type="secondary" @click="mineModalGroup = 'other'">添加</VButton>
            <VButton size="sm" type="secondary" @click="restoreMineDefaults('other')">恢复默认</VButton>
          </VSpace>
        </div>
        <VueDraggable v-model="mineOtherFeatures" handle=".nav-drag-handle">
          <div
            v-for="(item, index) in mineOtherFeatures"
            :key="index"
            class=":uno: mb-2 last:mb-0"
          >
            <div class=":uno: flex flex-wrap items-center gap-x-3 gap-y-2 rounded-lg border border-gray-100 bg-gray-50 px-3 py-2">
              <span class=":uno: nav-drag-handle cursor-move shrink-0 text-gray-400 hover:text-gray-600">
                <RiDragMove2Line class=":uno: h-4 w-4" />
              </span>
              <!-- 名称 -->
              <div class=":uno: flex min-w-0 flex-1 items-center gap-2">
                <span class=":uno: w-10 shrink-0 text-xs text-gray-700">名称</span>
                <FormKit
                  v-model="item.title"
                  :name="`mypage_other_title_${index}`"
                  type="text"
                  placeholder="导航名称"
                  outer-class=":uno: min-w-0 flex-1 !pt-0"
                />
              </div>
              <!-- 提示（subTitle，app 端展示为功能入口副标题） -->
              <div class=":uno: flex min-w-0 flex-1 items-center gap-2">
                <span class=":uno: w-10 shrink-0 text-xs text-gray-700">提示</span>
                <FormKit
                  v-model="item.subTitle"
                  :name="`mypage_other_subtitle_${index}`"
                  type="text"
                  placeholder="如 首页布局、卡片样式等本地偏好"
                  outer-class=":uno: min-w-0 flex-1 !pt-0"
                />
              </div>
              <!-- 文字颜色 + 图标背景色 -->
              <div class=":uno: flex shrink-0 items-center gap-2 pr-24">
                <span class=":uno: w-14 shrink-0 text-xs text-gray-700">文字颜色</span>
                <FormKit
                  type="color"
                  format="hex8"
                  :model-value="toColorInput(item.color)"
                  @update:model-value="onNavColor(item, $event)"
                  outer-class=":uno: w-12 shrink-0 !pt-0"
                />
              </div>
              <div class=":uno: flex shrink-0 items-center gap-2 pr-24">
                <span class=":uno: w-16 shrink-0 text-xs text-gray-700">图标背景色</span>
                <FormKit
                  type="color"
                  format="hex8"
                  :model-value="toColorInput(item.bgColor)"
                  @update:model-value="onNavBgColor(item, $event)"
                  outer-class=":uno: w-12 shrink-0 !pt-0"
                />
              </div>
              <div class=":uno: flex shrink-0 items-center gap-2 text-xs text-gray-500">
                <span>显示</span>
                <VSwitch v-model="item.visible" />
              </div>
              <button
                type="button"
                class=":uno: shrink-0 text-gray-400 transition-all hover:text-red-600"
                title="删除"
                @click="removeMineFeature('other', item)"
              >
                <RiDeleteBinLine class=":uno: h-4 w-4" />
              </button>
            </div>
          </div>
        </VueDraggable>
        <VEmpty
          v-if="!mineOtherFeatures.length"
          title="暂无功能入口"
          message="点击「添加」选择功能"
          :class="':uno: py-6'"
        />
      </div>
    </div>
  </template>

  <!-- 页面与排版 → 瞬间页 -->
  <template v-if="subTab === 'moment'">
    <div class=":uno: flex items-center justify-between gap-4 border-b border-gray-100 pb-3">
      <div>
        <div class=":uno: text-sm text-gray-700">显示评论列表</div>
        <div class=":uno: mt-0.5 text-xs text-gray-400">瞬间详情页是否展示评论列表区域</div>
      </div>
      <VSwitch v-model="formState.spec.pages.moment!.showCommentList" />
    </div>
    <div class=":uno: mt-4 flex items-center justify-between gap-4 border-b border-gray-100 pb-3">
      <div>
        <div class=":uno: text-sm text-gray-700">开启评论</div>
        <div class=":uno: mt-0.5 text-xs text-gray-400">瞬间卡片与详情页是否展示评论入口</div>
      </div>
      <VSwitch v-model="formState.spec.pages.moment!.enableComment" />
    </div>
  </template>

  <!-- 页面与排版 -->
  <template v-if="subTab === 'postDetail'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      评论相关开关：「显示评论列表」仅控制详情页评论列表区域的显隐；评论系统总开关在 Halo 官方后台「系统 → 评论设置 → 启用评论」；单篇文章可在「文章编辑 → 设置 → 允许评论」单独设置。
    </p>
    <div class=":uno: flex items-center justify-between gap-4 border-b border-gray-100 pb-3">
      <div>
        <div class=":uno: text-sm text-gray-700">显示评论列表</div>
        <div class=":uno: mt-0.5 text-xs text-gray-400">笔记详情页是否展示评论相关功能</div>
      </div>
      <VSwitch v-model="formState.spec.pages.postDetail!.showComment" />
    </div>
    <div class=":uno: mt-4 flex items-center justify-between gap-4 border-b border-gray-100 pb-3">
      <div>
        <div class=":uno: text-sm text-gray-700">显示笔记版权</div>
        <div class=":uno: mt-0.5 text-xs text-gray-400">笔记底部是否展示版权声明</div>
      </div>
      <VSwitch v-model="formState.spec.pages.postDetail!.copyrightEnabled" />
    </div>
    <div class=":uno: mt-4 rounded-lg bg-gray-50 p-4">
      <FormKit v-model="formState.spec.pages.postDetail!.copyrightAuthor" name="post_copyright_author" label="笔记版权作者" type="text" />
      <FormKit v-model="formState.spec.pages.postDetail!.copyrightDesc" name="post_copyright_desc" label="笔记版权描述" type="textarea" />
      <FormKit v-model="formState.spec.pages.postDetail!.copyrightViolation" name="post_copyright_violation" label="笔记侵权说明" type="textarea" />
    </div>
  </template>

  <!-- 页面与排版 → 免责声明页（不再需要启用开关，仅内容） -->
  <template v-if="subTab === 'disclaimer'">
    <p class=":uno: mb-3 text-xs text-gray-400">
      小程序端「免责声明」页面展示的内容（支持图文混排）；留空则不展示该页面。
    </p>
    <RichTextEditorField v-model="formState.spec.pages.disclaimer!.content" placeholder="输入免责声明内容，支持图文混排……留空则不展示免责声明页" />
  </template>

  <!-- 页面与排版 → 用户协议页（启用开关 + 注册页勾选行/协议弹窗与独立协议页共用内容） -->
  <template v-if="subTab === 'userAgreement'">
    <div class=":uno: mb-3 flex items-center justify-between gap-4 border-b border-gray-100 pb-3">
      <div>
        <div class=":uno: text-sm text-gray-700">启用用户协议页</div>
        <div class=":uno: mt-0.5 text-xs text-gray-400">关闭后 app 端隐藏注册页勾选行与协议入口</div>
      </div>
      <VSwitch v-model="formState.spec.pages.userAgreement!.enabled" />
    </div>
    <RichTextEditorField v-model="formState.spec.pages.userAgreement!.content" placeholder="输入用户协议内容，支持图文混排……留空时 app 端仅展示静态提示" />
  </template>

  <!-- 页面与排版 → 隐私政策页（启用开关 + 与用户协议同套路） -->
  <template v-if="subTab === 'privacyPolicy'">
    <div class=":uno: mb-3 flex items-center justify-between gap-4 border-b border-gray-100 pb-3">
      <div>
        <div class=":uno: text-sm text-gray-700">启用隐私政策页</div>
        <div class=":uno: mt-0.5 text-xs text-gray-400">关闭后 app 端隐藏注册页勾选行与协议入口</div>
      </div>
      <VSwitch v-model="formState.spec.pages.privacyPolicy!.enabled" />
    </div>
    <RichTextEditorField v-model="formState.spec.pages.privacyPolicy!.content" placeholder="输入隐私政策内容，支持图文混排……留空时 app 端仅展示静态提示" />
  </template>

  <!-- 首页分类栏选择（固定 3 个，复用审核模式候选弹窗） -->
  <AuditCandidatesModal
    v-if="categoryModalVisible"
    type="category"
    :selected="categoryModalSelected"
    :max="3"
    @update:visible="categoryModalVisible = false"
    @confirm="handleCategoryConfirm"
  />

  <!-- 首页快捷导航「添加」候选弹窗（统一清单：展示全部注册表条目，已配置置灰禁选，确认后追加） -->
  <FeatureEntryCandidatesModal
    v-if="quickNavModalVisible"
    :selected-keys="(formState.spec.pages.home.quickNavigation || []).map((i) => i.key || '')"
    @update:visible="quickNavModalVisible = false"
    @confirm="(selected) => handleQuickNavConfirm(selected)"
  />

  <!-- 关于页功能入口候选弹窗（common/other 两组共用；统一清单展示全部注册表条目，按组追加） -->
  <FeatureEntryCandidatesModal
    v-if="mineModalGroup"
    :selected-keys="
      (mineModalGroup === 'common'
        ? mineCommonFeatures
        : mineOtherFeatures
      ).map((i) => i.key || '')
    "
    @update:visible="mineModalGroup = null"
    @confirm="(selected) => handleMineConfirm(selected, mineModalGroup!)"
  />
</template>
