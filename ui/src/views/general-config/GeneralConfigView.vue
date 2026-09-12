<script setup lang="ts">
import {Toast, VCard, VPageHeader, VSpace, VTabbar} from "@halo-dev/components";
import {useQuery, useQueryClient} from "@tanstack/vue-query";
import {cloneDeep} from "lodash-es";
import {computed, nextTick, provide, ref, watch} from "vue";
import SubmitButton from "@/components/button/SubmitButton.vue";
import {generalConfigApi} from "@/api";
import {
  DEFAULT_MY_PAGE_COMMON_KEYS,
  DEFAULT_MY_PAGE_OTHER_KEYS,
  DEFAULT_QUICK_NAV_KEYS,
  featureEntriesByKeys,
  toQuickNavigationItem,
} from "@/constant/feature-entries";
import type {GeneralConfig, GeneralConfigSpec} from "@/types";
import {GeneralConfigFormKey} from "./form-context";
import ProfileSection from "./sections/ProfileSection.vue";
import PreferencesSection from "./sections/PreferencesSection.vue";
import PagesSection from "./sections/PagesSection.vue";
import AssetsSection from "./sections/AssetsSection.vue";
import LoveSection from "./sections/LoveSection.vue";
import LinkInfoSection from "./sections/LinkInfoSection.vue";
import MaintenanceSection from "./sections/MaintenanceSection.vue";

const queryClient = useQueryClient();

type BigGroup = "profile" | "preferences" | "pages" | "assets" | "love" | "linkInfo" | "maintenance";

const GROUP_ITEMS: Array<{id: BigGroup; label: string; desc: string}> = [
  {id: "profile", label: "应用设置", desc: "应用信息 / 博主 / 社交"},
  {id: "preferences", label: "偏好设置", desc: "首页/列表/归档布局与卡片样式"},
  {id: "pages", label: "页面设置", desc: "各页面标题 / 首页 / 图库 / 关于页 / 文章详情"},
  {id: "assets", label: "资源设置", desc: "加载占位"},
  {id: "love", label: "恋爱设置", desc: "恋爱页图片与模块入口"},
  {id: "linkInfo", label: "友链信息", desc: "站点信息 / 小程序信息"},
  {id: "maintenance", label: "维护设置", desc: "维护页文案 / 排期与开关"},
];

const SUB_TABS: Record<BigGroup, Array<{id: string; label: string}>> = {
  profile: [
    {id: "appInfo", label: "应用信息"},
    {id: "blogger", label: "博主资料"},
    {id: "social", label: "社交信息"},
    {id: "auditMode", label: "审核模式"},
    {id: "copyright", label: "页脚版权"},
  ],
  preferences: [
    {id: "home", label: "首页"},
    {id: "articles", label: "文章页面"},
    {id: "archives", label: "归档页面"},
  ],
  pages: [
    {id: "home", label: "首页"},
    {id: "gallery", label: "图库页"},
    {id: "categoryPage", label: "分类页"},
    {id: "momentPage", label: "瞬间页"},
    {id: "aboutPage", label: "关于页"},
    {id: "postDetail", label: "文章详情页"},
    {id: "disclaimersPage", label: "免责声明页"},
    {id: "loveDiary", label: "恋爱日记"},
    {id: "contactBlogger", label: "联系博主"},
    {id: "favorites", label: "我的收藏"},
    {id: "friendLinks", label: "友情链接"},
    {id: "archives", label: "文章归档"},
    {id: "vote", label: "投票中心"},
    {id: "dataVisual", label: "数据看板"},
    {id: "setting", label: "偏好设置"},
    {id: "aboutProject", label: "关于项目"},
    {id: "notice", label: "公告中心"},
    {id: "search", label: "搜索页面"},
  ],
  assets: [
    {id: "loading", label: "加载占位"},
  ],
  love: [
    {id: "info", label: "恋爱信息"},
    {id: "pageEntry", label: "页面入口"},
    {id: "modules", label: "模块入口"},
  ],
  linkInfo: [
    {id: "basic", label: "基本配置"},
    {id: "site", label: "站点信息"},
    {id: "info", label: "小程序信息"},
  ],
  maintenance: [
    {id: "time", label: "维护时间"},
    {id: "content", label: "维护内容"},
  ],
};

const bigGroup = ref<BigGroup>("profile");
const subTab = ref<string>("appInfo");

watch(bigGroup, (group) => {
  const first = SUB_TABS[group][0];
  if (first) {
    subTab.value = first.id;
  }
});

const subTabItems = computed(() => SUB_TABS[bigGroup.value]);

const {data: config, isLoading} = useQuery({
  queryKey: ["uni-halo:general-config"],
  queryFn: () => generalConfigApi.get(),
});

const formState = ref<GeneralConfig>(defaultConfig());

function defaultConfig(): GeneralConfig {
  return {
    metadata: {name: "general-config"},
    spec: defaultSpec(),
  };
}

function defaultSpec(): GeneralConfigSpec {
  return {
    profile: {
      appInfo: {name: "uni-halo", logo: "/plugins/plugin-uni-halo/assets/static/logo.png"},
      blogger: {nickname: "uni-halo", avatar: "", email: "", description: "", website: "", intro: ""},
      // 社交信息（动态列表：qq/wechat/email/github 四项默认；图标由 app 端按颜色/背景色色块渲染）
      social: {
        items: [
          {name: "企鹅号", content: "", color: "#12b7f5", bgColor: "#12b7f51A", priority: 1, visible: true},
          {name: "微信号", content: "", color: "#07c160", bgColor: "#07c1601A", priority: 2, visible: true},
          {name: "邮箱地址", content: "", color: "#f57c00", bgColor: "#f57c001A", priority: 3, visible: true},
          {name: "Github", content: "", color: "#24292f", bgColor: "#24292f1A", priority: 4, visible: true},
        ],
      },
    },
    pages: {
      homeConfig: {
        pageTitle: "首页",
        useQuickNavigation: true,
        // 快捷导航默认 5 项（对齐客户端 uh-home-quick-nav 默认 navList；由注册表显式 key 列表派生）
        quickNavigation: featureEntriesByKeys(DEFAULT_QUICK_NAV_KEYS).map(toQuickNavigationItem),
        useCategory: true,
        categories: [],
      },
      galleryConfig: {pageTitle: "图库"},
      categoryConfig: {pageTitle: ""},
      momentConfig: {pageTitle: ""},
      aboutConfig: {
        pageTitle: "关于博主",
        bgImageUrl: "/plugins/plugin-uni-halo/assets/static/uni_halo_profile_bg.jpg",
        waveImageUrl: "/plugins/plugin-uni-halo/assets/static/uni_halo_about_wave.gif",
        copyrightConfig: {enabled: true, content: "「 2022 uni-halo 丨 开源项目@小莫唐尼 」"},
      },
      // 我的页面功能入口：默认填充注册表条目，对齐 app 端 about.vue navList
      // （常用 7 项 / 其他 3 项，与后端 GeneralConfigServiceImpl 默认一致）
      myPageConfig: {
        commonFeatures: featureEntriesByKeys(DEFAULT_MY_PAGE_COMMON_KEYS).map(toQuickNavigationItem),
        otherFeatures: featureEntriesByKeys(DEFAULT_MY_PAGE_OTHER_KEYS).map(toQuickNavigationItem),
      },
      disclaimers: {content: ""},
      postDetailConfig: {
        showComment: true,
        copyrightEnabled: true,
        copyrightAuthor: "uni-halo",
        copyrightDesc:
          "使用《非商业性使用-相同方式共享 4.0 国际 (CC BY-NC-SA 4.0)》协议授权，文章来源于网上收集或者原创，若未在文章内说明的均为原创文章",
        copyrightViolation:
          "若侵害到您的权利，请您及时联系我，在收到通知后第一时间处理，邮箱：xxxx@xx.com",
      },
      // 其余功能页面标题（2026-09-11 起新增，默认留空，客户端回退内置标题）
      loveDiaryConfig: {pageTitle: ""},
      contactConfig: {pageTitle: ""},
      favoritesConfig: {pageTitle: ""},
      friendLinksConfig: {pageTitle: ""},
      archivesConfig: {pageTitle: ""},
      voteConfig: {pageTitle: ""},
      dataVisualConfig: {pageTitle: ""},
      settingConfig: {pageTitle: ""},
      aboutProjectConfig: {pageTitle: ""},
      noticeConfig: {pageTitle: ""},
      searchConfig: {pageTitle: ""},
    },
    assets: {
      loadingGifUrl: "/plugins/plugin-uni-halo/assets/static/uni_halo_img_lazyload.gif",
      loadingErrUrl: "",
    },
    preferences: {
      homeListLayout: "single",
      homeCardType: "image_bottom",
      articlesListLayout: "double",
      articleCardType: "image_bottom",
      archivesListLayout: "single",
      archivesCardType: "image_bottom",
      avatarRadius: true,
    },
    love: {
      // 恋爱模块默认：背景图留空（由站长配置或客户端内置回退）；
      // 恋爱日记入口仅密码（无开关，入口显隐由快捷导航/功能入口注册表控制）；
      // 三模块入口默认值对齐 app 端 love.vue 硬编码
      // （title/subTitle/颜色（hex8）/iconBgColor/path/priority）
      pageImages: {
        bgImageUrl: "",
      },
      loveDiary: {passwordEnabled: false, password: "", passwordRemoved: false},
      ourStory: {
        enabled: true, title: "恋爱故事", subTitle: "我们一起度过的那些经历",
        titleColor: "#f83856", subTitleColor: "#f8385699", iconBgColor: "#fce7f3",
        path: "/pages-blog/love/stories", priority: 1,
        passwordEnabled: false, password: "", passwordRemoved: false,
      },
      lovePhoto: {
        enabled: false, title: "恋爱相册", subTitle: "定格了我们的那些小美好",
        titleColor: "#60a5fa", subTitleColor: "#93c5fd", iconBgColor: "#dbeafe",
        path: "/pages-blog/love/album", priority: 2,
        passwordEnabled: false, password: "", passwordRemoved: false,
      },
      loveDaily: {
        enabled: false, title: "恋爱清单", subTitle: "你我之间的约定我们都在努力实现",
        titleColor: "#f83856", subTitleColor: "#f8385699", iconBgColor: "#fce7f3",
        path: "/pages-blog/love/list", priority: 3,
        passwordEnabled: false, password: "", passwordRemoved: false,
      },
      // 恋爱信息（纪念日 + 恋人信息，2026-09-11 起由「恋爱管理-恋爱配置」迁入；
      // 默认留空，前端回退默认标题）
      loveInfo: {
        loveDateTitle: "",
        loveDate: "",
        boyNickname: "",
        boyAvatar: "",
        girlNickname: "",
        girlAvatar: "",
      },
    },
    linkInfo: {
      // 友链信息默认：基本配置开放公开提交申请；miniInfo/siteInfo 留空
      // （站长配置后经 getConfigs 直接下发 pluginConfig.linkInfo）
      submissionEnabled: true,
      miniInfo: {
        displayName: "",
        miniProgramCode: "",
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
    // 审核模式（默认关闭，开启后关闭小程序部分数据展示，小程序提交审核时建议开启）
    auditMode: {
      enabled: false,
    },
  };
}

/** 回显/加载期间抑制脏标记；加载完成后开启变更追踪 */
let suppressDirty = true;

watch(
  () => config.value,
  (value) => {
    if (!value) {
      return;
    }
    const loaded = cloneDeep(value);
    loaded.spec = deepMerge(defaultSpec(), loaded.spec || {});
    loaded.metadata = {name: "general-config", ...loaded.metadata};
    suppressDirty = true;
    formState.value = loaded;
    dirty.value = false;
    nextTick(() => {
      suppressDirty = false;
    });
  },
  {immediate: true}
);

const dirty = ref(false);

watch(
  formState,
  () => {
    if (!suppressDirty) {
      dirty.value = true;
    }
  },
  {deep: true}
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
    await generalConfigApi.save(formState.value);
    Toast.success("保存成功");
    dirty.value = false;
    queryClient.invalidateQueries({queryKey: ["uni-halo:general-config"]});
  } catch (error) {
    Toast.error((error as Error).message);
  }
};

// 共享表单上下文：子组件（各 Section）直接改 formState 嵌套属性触发 deep watch → dirty；
// save 供子组件触发整表单保存（如维护「提前结束维护」）
provide(GeneralConfigFormKey, {formState, save: handleSave});
</script>

<template>
  <VPageHeader title="UniHalo-通用配置">
    <template #actions>
      <div class=":uno: flex items-center">
        <VSpace>
          <SubmitButton type="secondary" :loading="isLoading" :disabled="!dirty" text="保存" @submit="handleSave" />
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
        <div
          v-for="item in GROUP_ITEMS"
          :key="item.id"
          class=":uno: cursor-pointer px-4 py-3"
          :class="
            bigGroup === item.id
              ? ':uno: bg-gray-50 font-medium text-gray-900'
              : ':uno: text-gray-700 hover:bg-gray-50'
          "
          @click="bigGroup = item.id"
        >
          <div class=":uno: text-sm">{{ item.label }}</div>
          <div class=":uno: mt-0.5 text-xs text-gray-400">{{ item.desc }}</div>
        </div>
      </VCard>
    </aside>

    <!-- 右侧：VTabbar 子切换 + 分区组件（各分区独立组件） -->
    <div class=":uno: min-w-0 flex-1">
      <VCard :loading="isLoading">
        <template #header>
          <div class=":uno: p-2 pb-0">
            <VTabbar v-model:active-id="subTab" :items="subTabItems"/>
          </div>
        </template>

        <ProfileSection v-if="bigGroup === 'profile'" :sub-tab="subTab" />
        <PreferencesSection v-if="bigGroup === 'preferences'" :sub-tab="subTab" />
        <PagesSection v-if="bigGroup === 'pages'" :sub-tab="subTab" />
        <AssetsSection v-if="bigGroup === 'assets'" :sub-tab="subTab" />
        <LoveSection v-if="bigGroup === 'love'" :sub-tab="subTab" />
        <LinkInfoSection v-if="bigGroup === 'linkInfo'" :sub-tab="subTab" />
        <MaintenanceSection v-if="bigGroup === 'maintenance'" :sub-tab="subTab" />
      </VCard>
    </div>
  </div>
</template>
