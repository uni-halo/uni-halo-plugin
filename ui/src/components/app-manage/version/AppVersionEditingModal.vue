<script lang="ts" setup>
import { Toast, VButton, VModal, VSpace } from "@halo-dev/components";
import { submitForm } from "@formkit/core";
import { cloneDeep } from "lodash-es";
import { computed, ref, watch } from "vue";
import SubmitButton from "../../button/SubmitButton.vue";
import { appVersionsApi } from "@/api";
import type { AppInfo, AppVersion, StoreChannel } from "@/types";

// 商店渠道预设：scheme 需管理员按各商店规则填写（含安卓包名），仅预填名称/标识/优先级
const STORE_PRESETS: Omit<StoreChannel, "enable">[] = [
  { id: "huawei", name: "华为应用市场", priority: 100 },
  { id: "xiaomi", name: "小米应用商店", priority: 90 },
  { id: "oppo", name: "OPPO 软件商店", priority: 80 },
  { id: "vivo", name: "vivo 应用商店", priority: 70 },
  { id: "tencent", name: "应用宝", priority: 60 },
];

// 已添加的预设 id（chip 激活态判断）
const addedStoreIds = computed(() =>
  new Set((formState.value.spec.storeList || []).map((c) => c.id || ""))
);

// 预设 chip 点击切换:未添加则添加,已添加则移除
const toggleStoreChannel = (preset: Omit<StoreChannel, "enable">) => {
  const list = formState.value.spec.storeList || (formState.value.spec.storeList = []);
  const index = list.findIndex((c) => c.id === preset.id);
  if (index >= 0) {
    removeStoreChannel(index);
  } else {
    addStoreChannel(preset);
  }
};

const addStoreChannel = (preset?: Omit<StoreChannel, "enable">) => {
  if (!formState.value.spec.storeList) {
    formState.value.spec.storeList = [];
  }
  const channel: StoreChannel = preset
    ? { enable: true, ...preset }
    : { enable: true, id: "", name: "", scheme: "", priority: 10 };
  formState.value.spec.storeList.push(channel);
};

const removeStoreChannel = (index: number) => {
  formState.value.spec.storeList?.splice(index, 1);
};

const props = withDefaults(
  defineProps<{
    appVersion?: AppVersion;
    apps: AppInfo[];
    initialAppid?: string;
  }>(),
  {
    appVersion: undefined,
    initialAppid: "",
  }
);

const emit = defineEmits<{
  (event: "close"): void;
}>();

const modal = ref<InstanceType<typeof VModal> | null>(null);

const saving = ref(false);

const isUpdateMode = computed(() => !!props.appVersion);

const modalTitle = computed(() => (isUpdateMode.value ? "编辑版本" : "发布新版"));

const selectedAppid = ref("");

const appOptions = computed(() => {
  return props.apps.map((app) => ({
    label: `${app.spec.name || ""}（${app.spec.appid || ""}）`,
    value: app.spec.appid || "",
  }));
});

const formState = ref<AppVersion>({
  metadata: {
    name: "",
  },
  spec: {
    appid: "",
    name: "",
    title: "",
    contents: "",
    platform: ["Android"],
    type: "native_app",
    version: "",
    versionCode: undefined,
    minUniVersion: "",
    url: "",
    downloadType: "direct",
    storeList: [],
    externalUrl: "",
    externalName: "",
    stablePublish: true,
    isSilently: false,
    isMandatory: false,
  },
});

// wgt 包仅支持应用内自更新，下载方式锁定直链
watch(
  () => formState.value.spec.type,
  (type) => {
    if (type === "wgt") {
      formState.value.spec.downloadType = "direct";
    }
  }
);

// 下载地址的 label/help 按下载方式切换（三种形态均必填：
// direct 为主要地址；store 为商店全部跳转失败后的默认地址；
// external 为旧版客户端兜底地址——旧版不识别 download_type，拿 url 直接下载）
const urlLabel = computed(() => {
  if (formState.value.spec.downloadType === "direct") {
    return "下载地址";
  }
  return "默认下载地址";
});

const urlHelp = computed(() => {
  if (formState.value.spec.downloadType === "store") {
    return "全部商店跳转失败后使用此地址下载，仅支持上传 .apk 格式文件，或直接输入下载地址";
  }
  if (formState.value.spec.downloadType === "external") {
    return "旧版客户端将通过此地址直接下载，仅支持上传 .apk 格式文件，或直接输入下载地址";
  }
  return formState.value.spec.type === "wgt"
    ? "仅支持上传 .wgt 格式文件，或直接输入下载地址"
    : "仅支持上传 .apk 格式文件，或直接输入下载地址";
});

// 简单版本号比较（按 . 分段数字比较）
const compareVersions = (a: string, b: string): number => {
  if (!b) {
    return a ? 1 : 0;
  }
  const pa = a.split(".").map(Number);
  const pb = b.split(".").map(Number);
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const na = pa[i] || 0;
    const nb = pb[i] || 0;
    if (na !== nb) {
      return na - nb;
    }
  }
  return 0;
};

// 当前选中应用上次发布的信息（名称 + 版本号，用于填写提示）
const latestVersion = ref("");
const latestVersionCode = ref<number>();

// 下载地址按包类型约束文件格式：整包仅 apk，wgt 仅 wgt。
// 注意：Halo attachment 的 accepts 按 MIME 类型匹配（附件库 mediaType），
// 不能用扩展名（.apk/.wgt），否则附件库中对应格式文件无法显示。
// 整包不能包含 application/octet-stream：wgt 文件在附件库中的 mediaType 就是
// application/octet-stream，会导致整包时 apk 与 wgt 同时被匹配。
const urlAccepts = computed(() => {
  return formState.value.spec.type === "wgt"
    ? ["application/octet-stream", "application/zip"]
    : ["application/vnd.android.package-archive"];
});

watch(
  [selectedAppid],
  async () => {
    latestVersion.value = "";
    latestVersionCode.value = undefined;
    if (!selectedAppid.value) {
      return;
    }
    try {
      const result = await appVersionsApi.list({
        appid: selectedAppid.value,
        stablePublish: "true",
        page: 1,
        size: 50,
      });
      let maxName = "";
      let maxCode: number | undefined;
      for (const item of result.items || []) {
        const name = item.spec?.version;
        const code = item.spec?.versionCode;
        if (!name && code === undefined) {
          continue;
        }
        const currentCode = code ?? -1;
        const bestCode = maxCode ?? -1;
        if (currentCode > bestCode
            || (currentCode === bestCode && compareVersions(name || "", maxName) > 0)) {
          maxName = name || "";
          maxCode = code;
        }
      }
      latestVersion.value = maxName;
      latestVersionCode.value = maxCode;
    } catch (error) {
      console.error("Failed to fetch latest version", error);
    }
  },
  {
    immediate: false,
  }
);

// 回填 / 初始化时同步所属应用下拉
watch(
  () => props.appVersion,
  (appVersion) => {
    if (appVersion) {
      formState.value = cloneDeep(appVersion);
      // 存量记录无 downloadType，回填缺省直链
      formState.value.spec.downloadType = formState.value.spec.downloadType || "direct";
      // 平台仅保留 Android（iOS / Harmony 已隐藏）
      if (formState.value.spec.platform?.length) {
        formState.value.spec.platform = formState.value.spec.platform.filter(
          (p) => p === "Android"
        );
      }
      selectedAppid.value = appVersion.spec.appid || "";
    } else if (props.initialAppid) {
      // 发布新版：预选所属应用
      selectedAppid.value = props.initialAppid;
    }
  },
  {
    immediate: true,
  }
);

watch(selectedAppid, (appid) => {
  const app = props.apps.find((item) => item.spec.appid === appid);
  formState.value.spec.appid = appid;
  formState.value.spec.name = app?.spec.name || "";
});

const handleSubmit = () => {
  submitForm("app-version-form");
};

const handleSave = async () => {
  try {
    saving.value = true;
    if (isUpdateMode.value) {
      await appVersionsApi.update(formState.value.metadata.name, formState.value);
    } else {
      await appVersionsApi.create(formState.value);
    }
    modal.value?.close();
    Toast.success("保存成功");
  } catch (error) {
    Toast.error((error as Error).message);
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <VModal ref="modal" :title="modalTitle" :width="720" @close="emit('close')">
    <FormKit
      id="app-version-form"
      type="form"
      name="app-version-form"
      :config="{ validationVisibility: 'submit' }"
      @submit="handleSave"
    >
      <FormKit
        v-model="selectedAppid"
        name="appid"
        label="所属应用"
        type="select"
        validation="required"
        :validation-messages="{ required: '请选择所属应用' }"
        :options="[{ label: '请选择应用', value: '' }, ...appOptions]"
      />
      <FormKit
        v-model="formState.spec.title"
        name="title"
        label="更新标题"
        type="text"
        validation="required"
        :validation-messages="{ required: '更新标题不能为空' }"
        placeholder="例如：v1.1.0 新版本"
      />
      <FormKit
        v-model="formState.spec.contents"
        name="contents"
        label="更新内容"
        type="textarea"
        rows="4"
        placeholder="更新内容（可换行）"
      />
      <FormKit
        v-model="formState.spec.platform"
        name="platform"
        label="更新平台"
        type="checkbox"
        :options="[{ label: 'Android', value: 'Android' }]"
      />
      <FormKit
        v-model="formState.spec.type"
        name="type"
        label="包类型"
        type="select"
        :options="[
          { label: '整包', value: 'native_app' },
          { label: 'wgt 资源包', value: 'wgt' },
        ]"
      />
      <FormKit
        v-if="formState.spec.type !== 'wgt'"
        v-model="formState.spec.downloadType"
        name="downloadType"
        label="下载方式"
        type="radio"
        :options="[
          { label: '直链下载', value: 'direct' },
          { label: '商店分发', value: 'store' },
          { label: '外部链接', value: 'external' },
        ]"
      />

      <!-- 商店分发：渠道编辑器（预设一键添加 + 行内编辑） -->
      <div v-if="formState.spec.type !== 'wgt' && formState.spec.downloadType === 'store'" class="box-border pt-3">
        <div class="store-section-head">
          <span>应用商店分发</span>
          <span class="store-section-tip">按优先级降序依次尝试跳转，全部失败后使用默认下载地址</span>
        </div>
        <div class="store-chips">
          <button
            v-for="preset in STORE_PRESETS"
            :key="preset.id"
            type="button"
            class="store-chip"
            :class="{ 'store-chip-active': addedStoreIds.has(preset.id || '') }"
            @click="toggleStoreChannel(preset)"
          >
            + {{ preset.name }}
          </button>
          <button type="button" class="store-chip store-chip-custom" @click="addStoreChannel()">
            + 自定义渠道
          </button>
        </div>
        <div
          v-for="(channel, index) in formState.spec.storeList"
          :key="index"
          class="store-row"
        >
          <label class="store-enable">
            <input v-model="channel.enable" type="checkbox" /> 启用
          </label>
          <FormKit
            v-model="channel.name"
            :name="`store-name-${index}`"
            type="text"
            placeholder="渠道名称"
            :outer-class="'store-cell'"
          />
          <FormKit
            v-model="channel.scheme"
            :name="`store-scheme-${index}`"
            type="text"
            placeholder="跳转 scheme，如 himarket://appmarket-v3/detail?appId=C12345678"
            :outer-class="'store-cell'"
          />
          <FormKit
            v-model="channel.priority"
            :name="`store-priority-${index}`"
            type="number"
            placeholder="优先级"
            :outer-class="'store-cell'"
          />
          <button type="button" class="store-remove" @click="removeStoreChannel(index)">✕</button>
        </div>
        <div v-if="!formState.spec.storeList?.length" class="store-empty">
          尚未添加渠道，未添加时客户端将直接使用默认下载地址
        </div>
      </div>

      <!-- 外部链接：网盘/落地页跳转 -->
      <template v-if="formState.spec.type !== 'wgt' && formState.spec.downloadType === 'external'">
        <FormKit
          v-model="formState.spec.externalUrl"
          name="externalUrl"
          label="外部链接"
          type="url"
          validation="required"
          :validation-messages="{ required: '外部链接不能为空' }"
          placeholder="网盘或落地页地址，例如 https://pan.example.com/s/xxx"
          help="客户端将跳转系统浏览器打开此链接，不做应用内下载"
        />
        <FormKit
          v-model="formState.spec.externalName"
          name="externalName"
          label="按钮文案"
          type="text"
          placeholder="前往网盘下载"
          help="升级弹窗主按钮文案，留空时显示「前往下载」"
        />
      </template>

      <FormKit
        v-model="formState.spec.version"
        name="version"
        label="应用版本名称"
        type="text"
        validation="required"
        :validation-messages="{ required: '应用版本名称不能为空' }"
        placeholder="例如：1.1.0"
        :help="latestVersion
          ? `上次发布：${latestVersion}${latestVersionCode !== undefined ? `（版本号 ${latestVersionCode}）` : ''}，须大于该值`
          : '当前暂无已上线的版本'"
      />
      <FormKit
        v-model="formState.spec.versionCode"
        name="versionCode"
        label="应用版本号"
        type="number"
        validation="required"
        :validation-messages="{ required: '应用版本号不能为空' }"
        placeholder="整数，例如：110"
        help="整数数值，须大于该应用已发布的最大版本号"
      />
      <FormKit
        v-if="formState.spec.type === 'wgt'"
        v-model="formState.spec.minUniVersion"
        name="minUniVersion"
        label="最低原生版本"
        type="text"
        placeholder="例如：3.0.0"
        help="指支持该 wgt 资源包的最低原生 App 版本号。客户端原生 App 版本低于此值时无法应用此 wgt 更新，需通过整包更新升级。"
      />
      <FormKit
        v-model="formState.spec.url"
        name="url"
        :label="urlLabel"
        type="attachment"
        validation="required"
        :validation-messages="{ required: `${urlLabel}不能为空` }"
        :accepts="urlAccepts"
        :help="urlHelp"
      />
      <FormKit
        v-model="formState.spec.stablePublish"
        name="stablePublish"
        label="上线发行（同应用同平台仅一个上线版本）"
        type="checkbox"
      />
      <FormKit
        v-model="formState.spec.isMandatory"
        name="isMandatory"
        label="强制更新"
        type="checkbox"
      />
      <FormKit
        v-if="formState.spec.type === 'wgt'"
        v-model="formState.spec.isSilently"
        name="isSilently"
        label="静默更新（仅 wgt）"
        type="checkbox"
      />
    </FormKit>

    <template #footer>
      <VSpace>
        <SubmitButton
          :loading="saving"
          :disabled="saving"
          type="secondary"
          text="提交"
          @submit="handleSubmit"
        />
        <VButton @click="modal?.close()">关闭</VButton>
      </VSpace>
    </template>
  </VModal>
</template>

<style scoped>
.store-section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 0.875rem;
  font-weight: 500;
}
.store-section-tip {
  font-size: 0.75rem;
  font-weight: 400;
  color: var(--color-text-tertiary);
}
.store-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}
.store-chip {
  border: 1px solid transparent;
  border-radius: 6px;
  padding: 6px 14px;
  font-size: 0.75rem;
  color: #2fa37b;
  background: #e4f8f1;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s, background-color 0.15s;
}
.store-chip:hover,
.store-chip-active {
  color: #fff;
  border-color: #4ccba0;
  background: #4ccba0;
}
.store-chip-custom {
  color: #4ccba0;
  background: transparent;
  border: 1px dashed #4ccba0;
}
.store-chip-custom:hover {
  color: #fff;
  background: #4ccba0;
  border-style: solid;
}
.store-row {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) minmax(0, 1.6fr) 72px 28px;
  gap: 6px;
  align-items: center;
  margin-bottom: 6px;
}
.store-enable {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.75rem;
  color: var(--color-text-secondary);
  white-space: nowrap;
}
.store-cell {
  margin-bottom: 0;
}
.store-remove {
  border: none;
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
  font-size: 0.875rem;
}
.store-empty {
  font-size: 0.75rem;
  color: var(--color-text-tertiary);
  padding: 6px 0;
}
</style>
