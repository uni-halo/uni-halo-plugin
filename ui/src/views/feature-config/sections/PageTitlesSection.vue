<script setup lang="ts">
import { computed, inject, ref } from "vue";
import { Toast } from "@halo-dev/components";
import { FeatureConfigFormKey } from "../form-context";
import PageTitleGroup, { type PageTitleNamingVersion } from "../blocks/PageTitleGroup.vue";

/**
 * 页面标题分区：全站页面标题单表（三分组 + 搜索 + 命名方案）。
 * 自原「页面设置-页面标题」拆出；新增恋爱日记页（titles.loveDiary）。
 */
const { formState } = inject(FeatureConfigFormKey)!;

/** 标题键值对（写回 formState.spec.pages.titles） */
const titles = computed(
  () => (formState.value.spec.pages.titles ??= {}) as Record<string, string | undefined>
);

/** 搜索关键词（过滤各分组标题项） */
const keyword = ref("");

/** 页面标题分组定义（key → 标题项列表；展示顺序即数组顺序） */
const PAGE_TITLE_GROUPS = [
  {
    key: 'tabbar',
    label: '主导航页面',
    desc: '底部主导航页面',
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
      { key: 'loveDiary', label: '恋爱日记页' },
      { key: 'search', label: '搜索页' },
      { key: 'favorites', label: '我的收藏页' },
      { key: 'friendLinks', label: '友情链接页' },
      { key: 'notice', label: '公告中心页' },
      { key: 'noticeDetail', label: '公告详情页' },
      { key: 'votes', label: '投票中心页' },
      { key: 'voteDetail', label: '投票详情页' },
      { key: 'contact', label: '联系博主页' },
      { key: 'portfolio', label: '项目集页' },
      { key: 'portfolioDetail', label: '项目详情页' },
      { key: 'douban', label: '豆瓣页' },
      { key: 'footprint', label: '足迹地图页' },
      { key: 'equipment', label: '装备页' },
      { key: 'drawList', label: '绘画作品页' },
      { key: 'drawCanvas', label: '画布创作页' },
      { key: 'momentDetail', label: '瞬间详情页' },
      { key: 'bannerDetail', label: '轮播详情页' },
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
      { key: 'drawManage', label: '我的绘画页' },
      { key: 'drawReview', label: '绘画审核页' },
      { key: 'userProfile', label: '个人主页' },
      { key: 'login', label: '登录页' },
      { key: 'register', label: '注册页' },
      { key: 'userAgreement', label: '用户协议页' },
      { key: 'privacyPolicy', label: '隐私政策页' },
    ],
  },
];

/** 页面标题命名版本（「默认」= 原始标题；其余为风格化命名方案，点击批量填充） */
const TITLE_NAMING_VERSIONS: PageTitleNamingVersion[] = [
  {
    key: 'original',
    label: '默认',
    values: {
      home: '首页', gallery: '图库', category: '分类', moments: '瞬间', blogger: '博主',
      articles: '笔记', archives: '笔记归档', postDetail: '笔记详情', categoryArticles: '分类笔记',
      tags: '标签', tagArticles: '标签笔记', loveDiary: '恋爱日记', search: '搜索', favorites: '我的收藏',
      friendLinks: '友情链接', notice: '公告中心', noticeDetail: '公告详情',
      votes: '投票中心', voteDetail: '投票详情', contact: '联系博主', setting: '偏好设置',
      portfolio: '项目集', portfolioDetail: '项目详情', douban: '豆瓣', footprint: '足迹地图', equipment: '装备',
      drawList: '绘画作品', drawCanvas: '画一幅', drawManage: '我的绘画', drawReview: '绘画审核',
      momentDetail: '瞬间详情', bannerDetail: '内容详情', userProfile: '个人主页',
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
      tags: '文印标记', tagArticles: '印记札记', loveDiary: '情笺日记', search: '寻文觅迹', favorites: '撷珍藏录',
      friendLinks: '友邻书驿', notice: '公启通告', noticeDetail: '通告细阅',
      votes: '众议征询', voteDetail: '众议细览', contact: '尺素传书', setting: '雅趣自调',
      portfolio: '匠作陈列', portfolioDetail: '匠作细览', douban: '书影札记', footprint: '游踪印雪', equipment: '行囊藏珍',
      drawList: '丹青画廊', drawCanvas: '泼墨挥毫', drawManage: '画匣私藏', drawReview: '画评核阅',
      momentDetail: '流光细品', bannerDetail: '藏卷细读', userProfile: '访客芳鉴',
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
      tags: '笺上留痕', tagArticles: '随痕阅文', loveDiary: '恋爱手记', search: '搜览寻踪', favorites: '心藏好物',
      friendLinks: '远友相望', notice: '布告通知', noticeDetail: '阅览告示',
      votes: '民意共商', voteDetail: '共商详情', contact: '寄言留言', setting: '个性定制',
      portfolio: '作品一览', portfolioDetail: '作品详览', douban: '书影记录', footprint: '行迹所至', equipment: '装备一览',
      drawList: '画作一览', drawCanvas: '随笔一画', drawManage: '我的画作', drawReview: '画作审阅',
      momentDetail: '时光详览', bannerDetail: '内容详览', userProfile: '个人小页',
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
      tags: '叶脉题字', tagArticles: '叶上观书', loveDiary: '花开有信', search: '探径寻书', favorites: '拾珍入匣',
      friendLinks: '遥灯相映', notice: '庭前传讯', noticeDetail: '讯笺细读',
      votes: '衡议听言', voteDetail: '衡论细读', contact: '飞雁传笺', setting: '四时调序',
      portfolio: '山房陈物', portfolioDetail: '陈物细观', douban: '书影偶记', footprint: '屐痕处处', equipment: '行囊藏器',
      drawList: '田园涂鸦', drawCanvas: '山野拾笔', drawManage: '自家画册', drawReview: '山舍品画',
      momentDetail: '风露细看', bannerDetail: '笺上细读', userProfile: '客至小坐',
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
      tags: '内容标签', tagArticles: '标签随笔', loveDiary: '恋爱日记', search: '全站检索', favorites: '个人收藏',
      friendLinks: '友链站点', notice: '公告中心', noticeDetail: '公告详情',
      votes: '投票专区', voteDetail: '投票详情', contact: '联络博主', setting: '偏好设置',
      portfolio: '项目展示', portfolioDetail: '项目详情', douban: '书影音记录', footprint: '足迹地图', equipment: '装备展示',
      drawList: '绘画作品', drawCanvas: '创作画板', drawManage: '我的绘画', drawReview: '绘画审核',
      momentDetail: '动态详情', bannerDetail: '内容详情', userProfile: '个人主页',
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
      tags: '印记标识', tagArticles: '印记寻文', loveDiary: '相恋备忘', search: '觅迹寻章', favorites: '所爱存藏',
      friendLinks: '四方同好', notice: '讯息通告', noticeDetail: '讯息详读',
      votes: '众言表决', voteDetail: '表决详阅', contact: '寄语相询', setting: '私趣自定',
      portfolio: '作品陈列', portfolioDetail: '作品细读', douban: '书影拾遗', footprint: '行止留痕', equipment: '行囊之物',
      drawList: '笔迹存览', drawCanvas: '落笔成画', drawManage: '己作存览', drawReview: '己作甄别',
      momentDetail: '瞬念细读', bannerDetail: '篇章详读', userProfile: '访客之页',
      aboutProject: '初心述记', disclaimer: '条例申明', dataVisual: '万象数观',
      login: '凭籍登入', register: '开户结缘', userAgreement: '缔约守则', privacyPolicy: '私权明叙',
    },
  },
];

/** 各分组当前选中的命名版本（group key → version key；仅前端记忆，不落库） */
const activeTitleVersions = ref<Record<string, string>>(
  Object.fromEntries(PAGE_TITLE_GROUPS.map((group) => [group.key, 'original']))
);

/** 各分组折叠状态（group key → 是否折叠；默认全部折叠） */
const collapsedTitleGroups = ref<Record<string, boolean>>(
  Object.fromEntries(PAGE_TITLE_GROUPS.map((group) => [group.key, true]))
);

/** 应用命名版本成功提示（实际填充逻辑在 PageTitleGroup 块内） */
function onApplyVersion(groupKey: string) {
  const label =
    TITLE_NAMING_VERSIONS.find((v) => v.key === activeTitleVersions.value[groupKey])?.label || "";
  if (label) {
    Toast.success(`已应用「${label}」命名`);
  }
}
</script>

<template>
  <div class=":uno: space-y-3">
    <!-- 搜索 -->
    <div class=":uno: max-w-xs">
      <FormKit v-model="keyword" name="page_title_search" placeholder="搜索页面名…" type="text" outer-class=":uno: !mb-0" />
    </div>

    <PageTitleGroup v-for="group in PAGE_TITLE_GROUPS" :key="group.key" :group="group" :titles="titles"
      :keyword="keyword" :versions="TITLE_NAMING_VERSIONS" v-model:active-version="activeTitleVersions[group.key]"
      v-model:collapsed="collapsedTitleGroups[group.key]" @update:active-version="() => onApplyVersion(group.key)" />
  </div>
</template>
