<template>
  <div id="chatHistoryManagePage">
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="ID">
        <a-input v-model:value="searchParams.id" placeholder="对话 ID" />
      </a-form-item>
      <a-form-item label="应用 ID">
        <a-input v-model:value="searchParams.appId" placeholder="应用 ID" />
      </a-form-item>
      <a-form-item label="用户 ID">
        <a-input-number v-model:value="searchParams.userId" placeholder="用户 ID" />
      </a-form-item>
      <a-form-item label="消息类型">
        <a-select
          v-model:value="searchParams.messageType"
          allow-clear
          placeholder="请选择消息类型"
          style="width: 140px"
        >
          <a-select-option value="user">用户</a-select-option>
          <a-select-option value="ai">AI</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="消息内容">
        <a-input v-model:value="searchParams.message" placeholder="搜索消息内容" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>

    <a-divider />

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'messageType'">
          <a-tag :color="record.messageType === 'user' ? 'blue' : 'green'">
            {{ record.messageType === 'user' ? '用户' : 'AI' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'message'">
          <div class="message-cell">
            {{ record.message || '-' }}
          </div>
          <a-button v-if="record.message" type="link" size="small" @click="showMessageDetail(record)">
            查看详情
          </a-button>
        </template>
        <template v-else-if="column.dataIndex === 'user'">
          {{ record.user?.userName || record.user?.userAccount || record.userId || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatDateTime(record.createTime) }}
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="detailVisible" title="消息详情" :footer="null" width="760px">
      <pre class="message-detail">{{ currentMessage }}</pre>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { listAllChatHistoryByPageForAdmin } from '@/api/chatHistoryController.ts'
import { formatDateTime } from '@/utils/app'

const columns = [
  { title: 'ID', dataIndex: 'id', width: 180 },
  { title: '应用 ID', dataIndex: 'appId', width: 180 },
  { title: '用户', dataIndex: 'user', width: 160 },
  { title: '消息类型', dataIndex: 'messageType', width: 120 },
  { title: '消息内容', dataIndex: 'message' },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
]

const data = ref<API.ChatHistoryVO[]>([])
const total = ref(0)
const detailVisible = ref(false)
const currentMessage = ref('')

const searchParams = reactive<API.ChatHistoryQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
})

const fetchData = async () => {
  const res = await listAllChatHistoryByPageForAdmin({ ...searchParams })
  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error('获取对话历史失败，' + res.data.message)
  }
}

const pagination = computed(() => ({
  current: searchParams.pageNum ?? 1,
  pageSize: searchParams.pageSize ?? 10,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 条`,
}))

const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

const showMessageDetail = (record: API.ChatHistoryVO) => {
  currentMessage.value = record.message || ''
  detailVisible.value = true
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
#chatHistoryManagePage {
  padding: 24px;
  margin-top: 16px;
  background: #ffffff;
  border-radius: 8px;
}

.message-cell {
  max-width: 560px;
  max-height: 72px;
  margin-bottom: 0;
  overflow: hidden;
  color: #1f2937;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-detail {
  max-height: 60vh;
  padding: 16px;
  margin: 0;
  overflow: auto;
  color: #1f2937;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  background: #f8fafc;
  border: 1px solid #edf0f5;
  border-radius: 8px;
}
</style>
