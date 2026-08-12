<template>
  <div id="appEditPage">
    <div class="edit-header">
      <a-button @click="goBack">返回</a-button>
      <h2>{{ isAdminEdit ? '管理应用信息' : '修改应用信息' }}</h2>
    </div>
    <a-card :loading="loading">
      <a-form
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 14 }"
        :model="formState"
        @finish="handleSubmit"
      >
        <a-form-item name="appName" label="应用名称" :rules="[{ required: true, message: '请输入应用名称' }]">
          <a-input v-model:value="formState.appName" placeholder="请输入应用名称" />
        </a-form-item>
        <template v-if="isAdminEdit">
          <a-form-item name="cover" label="应用封面">
            <a-input v-model:value="adminFormState.cover" placeholder="请输入封面 URL" />
          </a-form-item>
          <a-form-item name="priority" label="优先级">
            <a-input-number v-model:value="adminFormState.priority" :min="0" style="width: 100%" />
          </a-form-item>
        </template>
        <a-form-item label="创建提示词">
          <a-textarea :value="appInfo?.initPrompt" disabled :auto-size="{ minRows: 3, maxRows: 6 }" />
        </a-form-item>
        <a-form-item :wrapper-col="{ offset: 4, span: 14 }">
          <a-space>
            <a-button type="primary" html-type="submit" :loading="submitting">保存</a-button>
            <a-button @click="goBack">取消</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  getAppVoById,
  getAppVoByIdByAdmin,
  updateApp,
  updateAppByAdmin,
} from '@/api/appController.ts'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const appInfo = ref<API.AppVO>()

const appId = computed(() => String(route.params.id || ''))
const isAdminEdit = computed(() => route.path.startsWith('/admin'))

const formState = reactive<API.AppUpdateRequest>({
  id: undefined,
  appName: '',
})

const adminFormState = reactive<API.AppAdminUpdateRequest>({
  id: undefined,
  appName: '',
  cover: '',
  priority: 0,
})

const fetchAppInfo = async () => {
  if (!appId.value) {
    message.error('应用不存在')
    return
  }
  loading.value = true
  const res = isAdminEdit.value
    ? await getAppVoByIdByAdmin({ id: appId.value } as unknown as API.getAppVOByIdByAdminParams)
    : await getAppVoById({ id: appId.value } as unknown as API.getAppVOByIdParams)
  loading.value = false
  if (res.data.code === 0 && res.data.data) {
    appInfo.value = res.data.data
    formState.id = res.data.data.id as any
    formState.appName = res.data.data.appName || ''
    adminFormState.id = res.data.data.id as any
    adminFormState.appName = res.data.data.appName || ''
    adminFormState.cover = res.data.data.cover || ''
    adminFormState.priority = res.data.data.priority ?? 0
  } else {
    message.error('获取应用详情失败，' + res.data.message)
  }
}

const handleSubmit = async () => {
  if (!appId.value) {
    return
  }
  submitting.value = true
  const res = isAdminEdit.value
    ? await updateAppByAdmin({
        ...adminFormState,
        appName: formState.appName,
      })
    : await updateApp({
        id: appId.value,
        appName: formState.appName,
      } as unknown as API.AppUpdateRequest)
  submitting.value = false
  if (res.data.code === 0) {
    message.success('保存成功')
    goBack()
  } else {
    message.error('保存失败，' + res.data.message)
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchAppInfo()
})
</script>

<style scoped>
#appEditPage {
  max-width: 920px;
  margin: 16px auto 0;
}

.edit-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.edit-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
}
</style>
