<template>
  <div id="appManagePage">
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="ID">
        <a-input v-model:value="searchParams.id" placeholder="应用 ID" />
      </a-form-item>
      <a-form-item label="名称">
        <a-input v-model:value="searchParams.appName" placeholder="输入应用名称" />
      </a-form-item>
      <a-form-item label="类型">
        <a-input v-model:value="searchParams.codeGenType" placeholder="生成类型" />
      </a-form-item>
      <a-form-item label="用户 ID">
        <a-input-number v-model:value="searchParams.userId" placeholder="用户 ID" />
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
        <template v-if="column.dataIndex === 'cover'">
          <a-image v-if="record.cover" :src="record.cover" :width="120" />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.dataIndex === 'priority'">
          <a-tag :color="record.priority >= 99 ? 'gold' : 'blue'">{{ record.priority ?? 0 }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'user'">
          {{ record.user?.userName || record.user?.userAccount || record.userId || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatDateTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="goEdit(record)">编辑</a-button>
            <a-button size="small" type="primary" @click="doSetGood(record)">精选</a-button>
            <a-popconfirm title="确定删除这个应用吗？" @confirm="doDelete(record.id)">
              <a-button size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  deleteAppByAdmin,
  listAppVoByPageByAdmin,
  updateAppByAdmin,
} from '@/api/appController.ts'
import { formatDateTime } from '@/utils/app'

const router = useRouter()

const columns = [
  { title: 'ID', dataIndex: 'id' },
  { title: '名称', dataIndex: 'appName' },
  { title: '封面', dataIndex: 'cover' },
  { title: '类型', dataIndex: 'codeGenType' },
  { title: '部署标识', dataIndex: 'deployKey' },
  { title: '优先级', dataIndex: 'priority' },
  { title: '创建用户', dataIndex: 'user' },
  { title: '创建时间', dataIndex: 'createTime' },
  { title: '操作', key: 'action' },
]

const data = ref<API.AppVO[]>([])
const total = ref(0)

const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
})

const fetchData = async () => {
  const res = await listAppVoByPageByAdmin({ ...searchParams })
  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
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

const goEdit = (record: API.AppVO) => {
  if (record.id) {
    router.push(`/admin/app/edit/${record.id}`)
  }
}

const doSetGood = async (record: API.AppVO) => {
  if (!record.id) {
    return
  }
  const res = await updateAppByAdmin({
    id: record.id,
    appName: record.appName,
    cover: record.cover,
    priority: 99,
  })
  if (res.data.code === 0) {
    message.success('设置精选成功')
    fetchData()
  } else {
    message.error('设置精选失败，' + res.data.message)
  }
}

const doDelete = async (id?: string | number) => {
  if (!id) {
    return
  }
  const res = await deleteAppByAdmin({ id } as unknown as API.DeleteRequest)
  if (res.data.code === 0) {
    message.success('删除成功')
    fetchData()
  } else {
    message.error('删除失败，' + res.data.message)
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
#appManagePage {
  padding: 24px;
  margin-top: 16px;
  background: #ffffff;
  border-radius: 8px;
}
</style>
