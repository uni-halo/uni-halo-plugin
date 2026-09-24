<script setup lang="ts">
import { Toast, VCard, VPageHeader, VSpace } from "@halo-dev/components";
import { useQuery, useQueryClient } from "@tanstack/vue-query";
import { cloneDeep } from "lodash-es";
import { ref, watch } from "vue";
import SubmitButton from "@/components/button/SubmitButton.vue";
import { loveInfoApi } from "@/api";
import type { LoveInfo } from "@/types";

const queryClient = useQueryClient();

const { data: loveInfo, isLoading } = useQuery({
  queryKey: ["uni-halo:love-info"],
  queryFn: () => loveInfoApi.get(),
});

function defaultLoveInfo(): LoveInfo {
  return {
    metadata: { name: "love-info" },
    spec: {
      loveDateTitle: "",
      loveDate: "",
      boyNickname: "",
      boyAvatar: "",
      girlNickname: "",
      girlAvatar: "",
    },
  };
}

const formState = ref<LoveInfo>(defaultLoveInfo());

watch(
  () => loveInfo.value,
  (value) => {
    if (!value) {
      return;
    }
    const loaded = cloneDeep(value);
    loaded.metadata = { ...loaded.metadata, name: "love-info" };
    formState.value = loaded;
  },
  { immediate: true }
);

const handleSave = async () => {
  try {
    await loveInfoApi.save(formState.value);
    Toast.success("保存成功");
    queryClient.invalidateQueries({ queryKey: ["uni-halo:love-info"] });
  } catch (error) {
    Toast.error((error as Error).message);
  }
};
</script>

<template>
  <VPageHeader title="UniHalo-恋爱信息">
    <template #actions>
      <VSpace>
        <SubmitButton type="secondary" :loading="isLoading" text="保存修改" @submit="handleSave" />
      </VSpace>
    </template>
  </VPageHeader>

  <div class=":uno: m-4">
    <VCard :body-class="[':uno: !p-4']">
      <template #header>
        <div class=":uno: border-b border-gray-100 px-4 py-3 w-full">
          <span class=":uno: text-sm font-semibold text-gray-700">纪念日</span>
        </div>
      </template>
      <div class=":uno: max-w-2xl">
        <FormKit v-model="formState.spec.loveDateTitle" name="love_date_title" label="纪念日标题" type="text"
          placeholder="例如：我们在一起的那天" help="展示在恋爱页顶部，留空使用默认文案「这是我们一起走过的」" />
        <FormKit v-model="formState.spec.loveDate" name="love_date" label="恋爱纪念日" type="date"
          help="用于计算恋爱天数，同时这可是一个非常重要的节日呢，可不能忘记哦~" />
      </div>
    </VCard>

    <VCard class=":uno: mt-4" :body-class="[':uno: !p-4']">
      <template #header>
        <div class=":uno: border-b border-gray-100 px-4 py-3 w-full">
          <span class=":uno: text-sm font-semibold text-gray-700">恋人信息</span>
        </div>
      </template>
      <div class=":uno: max-w-2xl">
        <FormKit v-model="formState.spec.boyAvatar" name="boy_avatar" label="男生头像" type="attachment"
          :accepts="['image/*']" />
        <FormKit v-model="formState.spec.boyNickname" name="boy_nickname" label="男生昵称" type="text"
          placeholder="男生的昵称" />
        <FormKit v-model="formState.spec.girlAvatar" name="girl_avatar" label="女生头像" type="attachment"
          :accepts="['image/*']" />
        <FormKit v-model="formState.spec.girlNickname" name="girl_nickname" label="女生昵称" type="text"
          placeholder="女生的昵称" />
      </div>
    </VCard>
  </div>
</template>
