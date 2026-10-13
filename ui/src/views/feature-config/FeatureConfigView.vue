<script setup lang="ts">
import { Toast, VCard, VPageHeader, VSpace, VTabbar } from "@halo-dev/components";
import { useQuery, useQueryClient } from "@tanstack/vue-query";
import { cloneDeep } from "lodash-es";
import { computed, provide, ref, watch } from "vue";
import SubmitButton from "@/components/button/SubmitButton.vue";
import { featureConfigApi } from "@/api";
import {
  DEFAULT_MY_PAGE_COMMON_KEYS,
  DEFAULT_MY_PAGE_OTHER_KEYS,
  DEFAULT_QUICK_NAV_KEYS,
  featureEntriesByKeys,
  toQuickNavigationItem,
} from "@/constant/feature-entries";
import type { FeatureConfig, FeatureConfigSpec } from "@/types";
import { FeatureConfigFormKey } from "./form-context";
import ProfileSection from "./sections/ProfileSection.vue";
import AppearanceSection from "./sections/AppearanceSection.vue";
import PageTitlesSection from "./sections/PageTitlesSection.vue";
import LegalSection from "./sections/LegalSection.vue";
import MaintenanceSection from "./sections/MaintenanceSection.vue";

const queryClient = useQueryClient();

type BigGroup = "profile" | "appearance" | "titles" | "legal" | "maintenance";

/** 大分区（一级统一 4 字）；desc 为左列表说明 */
const GROUP_ITEMS: Array<{ id: BigGroup; label: string; desc: string }> = [
  { id: "profile", label: "应用资料", desc: "应用信息 / 博主 / 社交 / 页脚版权" },
  { id: "appearance", label: "外观布局", desc: "通用外观 / 页面布局 / 博主页面" },
  { id: "titles", label: "页面标题", desc: "全站页面标题统一维护" },
  { id: "legal", label: "法律页面", desc: "用户协议 / 隐私政策 / 免责声明" },
  { id: "maintenance", label: "维护模式", desc: "维护页文案 / 排期与开关" },
];

/** 二级子 tab（系列内等长）；空数组 = 无二级（整卡单表） */
const SUB_TABS: Record<BigGroup, Array<{ id: string; label: string }>> = {
  profile: [
    { id: "appInfo", label: "应用信息" },
    { id: "blogger", label: "博主资料" },
    { id: "social", label: "社交信息" },
    { id: "copyright", label: "页脚版权" },
  ],
  appearance: [
    { id: "general", label: "通用外观" },
    { id: "bloggerPage", label: "博主页" },
    { id: "home", label: "首页" },
    { id: "articles", label: "笔记页" },
    { id: "archives", label: "归档页" },
    { id: "categoryArticles", label: "分类页" },
    { id: "tagArticles", label: "标签页" },
    { id: "friendLinks", label: "友链页" },
    { id: "content", label: "内容页（瞬间、笔记）" },
  ],
  titles: [],
  legal: [
    { id: "userAgreement", label: "用户协议" },
    { id: "privacyPolicy", label: "隐私政策" },
    { id: "disclaimer", label: "免责声明" },
  ],
  maintenance: [
    { id: "time", label: "维护时间" },
    { id: "content", label: "维护内容" },
  ],
};

/** 各大分区默认子 tab（titles 无二级，固定 titles） */
const DEFAULT_SUB_TABS: Record<BigGroup, string> = {
  profile: "appInfo",
  appearance: "general",
  titles: "titles",
  legal: "userAgreement",
  maintenance: "time",
};

const bigGroup = ref<BigGroup>("profile");
const subTab = ref<string>(DEFAULT_SUB_TABS.profile);

watch(bigGroup, (group) => {
  subTab.value = DEFAULT_SUB_TABS[group];
});

const subTabItems = computed(() => SUB_TABS[bigGroup.value]);

const { data: config, isLoading } = useQuery({
  queryKey: ["uni-halo:feature-config"],
  queryFn: () => featureConfigApi.get(),
});

const formState = ref<FeatureConfig>(defaultConfig());

function defaultConfig(): FeatureConfig {
  return {
    metadata: { name: "feature-config" },
    spec: defaultSpec(),
  };
}

function defaultSpec(): FeatureConfigSpec {
  return {
    profile: {
      appInfo: { name: "uni-halo", logo: "/plugins/uni-halo/assets/static/logo.png" },
      blogger: { nickname: "uni-halo", avatar: "", email: "", description: "", website: "", intro: "" },
      // 页脚版权（显示于关于页页脚，与后端 profile.copyrightConfig 默认一致）
      copyrightConfig: { enabled: true, content: "「 2022 uni-halo 丨 开源项目@小莫唐尼 」" },
      // 社交信息（动态列表：qq/wechat/email/github 四项默认；图标由 app 端按颜色/背景色色块渲染）
      social: {
        items: [
          { name: "企鹅号", content: "", color: "#12b7f5", bgColor: "#12b7f51A", priority: 1, visible: true },
          { name: "微信号", content: "", color: "#07c160", bgColor: "#07c1601A", priority: 2, visible: true },
          { name: "邮箱地址", content: "", color: "#f57c00", bgColor: "#f57c001A", priority: 3, visible: true },
          { name: "Github", content: "", color: "#24292f", bgColor: "#24292f1A", priority: 4, visible: true },
        ],
      },
    },
    pages: {
      // 全站页面标题（外观-页面标题单表统一维护；留空时客户端回退内置标题）
      titles: {
        home: "首页",
        gallery: "图库",
        category: "分类",
        moments: "瞬间",
        blogger: "博主",
        articles: "笔记",
        archives: "笔记归档",
        postDetail: "笔记详情",
        categoryArticles: "分类笔记",
        tags: "标签",
        tagArticles: "标签笔记",
        loveDiary: "恋爱日记",
        search: "搜索",
        favorites: "我的收藏",
        friendLinks: "友情链接",
        notice: "公告中心",
        noticeDetail: "公告详情",
        votes: "投票中心",
        voteDetail: "投票详情",
        contact: "联系博主",
        portfolio: "项目集",
        portfolioDetail: "项目详情",
        douban: "豆瓣",
        footprint: "足迹地图",
        momentDetail: "瞬间详情",
        bannerDetail: "内容详情",
        userProfile: "个人主页",
        setting: "偏好设置",
        aboutProject: "关于项目",
        disclaimer: "免责声明",
        dataVisual: "数据看板",
        login: "登录",
        register: "注册",
        userAgreement: "用户协议",
        privacyPolicy: "隐私政策",
        drawList: "绘画作品",
        drawCanvas: "画一幅",
        drawManage: "我的绘画",
        drawReview: "绘画审核",
      },
      home: {
        useQuickNavigation: true,
        // 快捷导航默认 5 项（对齐客户端 uh-home-quick-nav 默认 navList；由注册表显式 key 列表派生）
        quickNavigation: featureEntriesByKeys(DEFAULT_QUICK_NAV_KEYS).map(toQuickNavigationItem),
        useCategory: true,
        useHitokoto: true,
        categories: [],
      },
      blogger: {
        bgImageUrl: "/plugins/uni-halo/assets/static/uni_halo_profile_bg.jpeg",
        waveImageUrl: "/plugins/uni-halo/assets/static/uni_halo_about_wave.gif",
        commonFeaturesMode: "grid",
      },
      // 博主页功能入口（mine 对标博主页）：默认填充注册表条目（与后端默认一致）
      mine: {
        commonFeatures: featureEntriesByKeys(DEFAULT_MY_PAGE_COMMON_KEYS).map(toQuickNavigationItem),
        otherFeatures: featureEntriesByKeys(DEFAULT_MY_PAGE_OTHER_KEYS).map(toQuickNavigationItem),
      },
      disclaimer: { content: "" },
      moment: {
        showCommentList: true,
      },
      // 用户协议页/隐私政策页（独立两对象；内容留空 = app 端注册页回退静态提示文案）
      userAgreement: { content: "" },
      privacyPolicy: { content: "" },
      // 绘画配置（审核开关入口在「绘画作品管理-设置」弹窗）
      draw: { auditEnabled: true },
      postDetail: {
        showComment: true,
        copyrightEnabled: true,
        copyrightAuthor: "uni-halo",
        copyrightDesc:
          "使用《非商业性使用-相同方式共享 4.0 国际 (CC BY-NC-SA 4.0)》协议授权，笔记来源于网上收集或者原创，若未在笔记内说明的均为原创笔记",
        copyrightViolation:
          "若侵害到您的权利，请您及时联系我，在收到通知后第一时间处理，邮箱：xxxx@xx.com",
      },
    },
    assets: {
      loadingGifUrl: "/plugins/uni-halo/assets/static/uni_halo_img_lazyload.gif",
      loadingErrUrl: "",
    },
    preferences: {
      homeListLayout: "single",
      homeCardType: "image_bottom",
      articlesListLayout: "single",
      articlesCardType: "image_bottom",
      archivesListLayout: "single",
      archivesCardType: "image_bottom",
      categoryArticlesListLayout: "single",
      categoryArticlesCardType: "image_bottom",
      tagArticlesListLayout: "single",
      tagArticlesCardType: "image_bottom",
      avatarShape: "square",
      linkPage: {
        miniProgramOpenMode: "fullscreen",
      },
    },
    love: {
      // 恋爱模块默认：恋爱日记入口仅密码（无开关，入口显隐由快捷导航/功能入口注册表控制）；
      // 三模块入口默认值对齐 app 端 love.vue 硬编码
      // （title/subTitle/颜色（hex8）/iconBgColor/path/priority）
      loveDiary: { passwordEnabled: false, password: "", passwordRemoved: false },
      ourStory: {
        enabled: true, title: "恋爱故事", subTitle: "我们一起度过的那些经历",
        titleColor: "#f83856", subTitleColor: "#f8385699", iconBgColor: "#fce7f3",
        path: "/pages-blog/love/stories", priority: 1,
        passwordEnabled: false, password: "", passwordRemoved: false,
      },
      lovePhoto: {
        enabled: true, title: "恋爱相册", subTitle: "定格了我们的那些小美好",
        titleColor: "#60a5fa", subTitleColor: "#93c5fd", iconBgColor: "#dbeafe",
        path: "/pages-blog/love/album", priority: 2,
        passwordEnabled: false, password: "", passwordRemoved: false,
      },
      loveDaily: {
        enabled: true, title: "恋爱清单", subTitle: "你我之间的约定我们都在努力实现",
        titleColor: "#f83856", subTitleColor: "#f8385699", iconBgColor: "#fce7f3",
        path: "/pages-blog/love/list", priority: 3,
        passwordEnabled: false, password: "", passwordRemoved: false,
      },
      // 恋爱页背景图（默认留空客户端内置回退）
      diaryPage: { bgImageUrl: "" },
    },
    linkInfo: {
      // 友链设置默认：开放公开提交申请；审核邮件通知默认关闭
      submissionEnabled: true,
      sendEmail: false,
      miniInfo: {
        displayName: "",
        miniProgramCode: "",
        appId: "",
        path: "",
        link: "",
        description: "",
        applyRemark: "",
      },
      siteInfo: {
        displayName: "",
        url: "",
        logo: "",
        description: "",
        backlink: "",
        feedUrls: [],
      },
    },
    maintenance: {
      enabled: false,
      title: "站点维护中",
      notice: "",
      description: "",
    },
  };
}

watch(
  () => config.value,
  (value) => {
    if (!value) {
      return;
    }
    const loaded = cloneDeep(value);
    loaded.spec = deepMerge(defaultSpec(), loaded.spec || {});
    loaded.metadata = { name: "feature-config", ...loaded.metadata };
    formState.value = loaded;
  },
  { immediate: true }
);

function deepMerge<T>(base: T, overlay: object): T {
  const out = cloneDeep(base) as unknown as Record<string, unknown>;
  for (const [key, value] of Object.entries(overlay)) {
    if (value === null || value === undefined) {
      continue;
    }
    if (
      value &&
      typeof value === "object" &&
      !Array.isArray(value) &&
      out[key] &&
      typeof out[key] === "object"
    ) {
      out[key] = deepMerge(out[key], value);
    } else {
      out[key] = cloneDeep(value);
    }
  }
  return out as unknown as T;
}

/** 保存前预检：维护时间窗口 startTime >= endTime 拒绝（后端 400 兜底） */
function validateMaintenanceWindow(): string | null {
  const m = formState.value.spec.maintenance;
  if (!m) {
    return null;
  }
  const start = m.startTime ? Date.parse(m.startTime) : Number.NaN;
  const end = m.endTime ? Date.parse(m.endTime) : Number.NaN;
  if (Number.isFinite(start) && Number.isFinite(end) && start >= end) {
    return "维护结束时间必须晚于开始时间";
  }
  return null;
}

const handleSave = async () => {
  const maintenanceError = validateMaintenanceWindow();
  if (maintenanceError) {
    Toast.error(maintenanceError);
    return;
  }
  try {
    await featureConfigApi.save(formState.value);
    Toast.success("保存成功");
    queryClient.invalidateQueries({ queryKey: ["uni-halo:feature-config"] });
  } catch (error) {
    Toast.error((error as Error).message);
  }
};

// 共享表单上下文：子组件（各 Section）直接改 formState 嵌套属性；
// save 供子组件触发整表单保存（如维护「提前结束维护」）
provide(FeatureConfigFormKey, { formState, save: handleSave });
</script>

<template>
  <VPageHeader title="UniHalo-功能设置">
    <template #actions>
      <div class=":uno: flex items-center gap-x-4">
        <div class="text-xs text-orange-500"> 提示：该页面所有的操作都需要点击 保存设置 按钮才会保存成功 </div>
        <VSpace>
          <SubmitButton type="secondary" :loading="isLoading" text="保存设置" @submit="handleSave" />
        </VSpace>
      </div>
    </template>
  </VPageHeader>

  <div class=":uno: flex flex-col gap-4 md:m-4 lg:flex-row">
    <!-- 左列表：切换大分区（VCard，参考公告管理左侧类型栏） -->
    <aside class=":uno: w-full flex-shrink-0 lg:w-64">
      <VCard :body-class="[':uno: !p-0']">
        <div class=":uno: flex items-center justify-between border-b border-gray-100 px-4 py-3">
          <span class=":uno: text-sm font-semibold text-gray-700">配置分类</span>
        </div>
        <div v-for="item in GROUP_ITEMS" :key="item.id" class=":uno: cursor-pointer px-4 py-3" :class="bigGroup === item.id
            ? ':uno: bg-gray-50 font-medium text-gray-900'
            : ':uno: text-gray-700 hover:bg-gray-50'
          " @click="bigGroup = item.id">
          <div class=":uno: text-sm">{{ item.label }}</div>
          <div class=":uno: mt-0.5 text-xs text-gray-400">{{ item.desc }}</div>
        </div>
      </VCard>
    </aside>

    <!-- 右侧：VTabbar 子切换 + 分区组件（titles 无二级，整卡单表） -->
    <div class=":uno: min-w-0 flex-1">
      <VCard :loading="isLoading">
        <template v-if="subTabItems.length" #header>
          <!-- min-w-0 + w-full：card-header 是 flex+overflow-hidden，flex 子项默认
               min-width:auto 会被撑开导致 VTabbar 内部滚动失效 -->
          <div class=":uno: w-full min-w-0 p-2 pb-0">
            <VTabbar v-model:active-id="subTab" :items="subTabItems" />
          </div>
        </template>

        <ProfileSection v-if="bigGroup === 'profile'" :sub-tab="subTab" />
        <AppearanceSection v-if="bigGroup === 'appearance'" :sub-tab="subTab" />
        <PageTitlesSection v-if="bigGroup === 'titles'" sub-tab="titles" />
        <LegalSection v-if="bigGroup === 'legal'" :sub-tab="subTab" />
        <MaintenanceSection v-if="bigGroup === 'maintenance'" :sub-tab="subTab" />
      </VCard>
    </div>
  </div>
</template>
