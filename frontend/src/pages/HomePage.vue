<template>
  <div id="homePage">
    <section class="hero">
      <div class="hero__brand">
        <span>AI 应用生成平台</span>
      </div>
      <p class="hero__desc">一句话轻松创建网站应用</p>

      <div class="prompt-box">
        <a-textarea
          v-model:value="initPrompt"
          :auto-size="{ minRows: 5, maxRows: 8 }"
          placeholder="帮我创建个人博客网站"
          @press-enter.ctrl="doCreateApp"
        />
        <div class="prompt-box__footer">
          <a-space wrap>
            <a-button v-for="item in promptExamples" :key="item.label" @click="initPrompt = item.prompt">
              {{ item.label }}
            </a-button>
          </a-space>
          <a-button type="primary" shape="circle" :loading="creating" @click="doCreateApp">↑</a-button>
        </div>
      </div>
    </section>

    <section class="app-section">
      <div class="section-header">
        <h2>我的作品</h2>
        <a-input-search
          v-model:value="mySearchParams.appName"
          class="section-search"
          placeholder="搜索我的应用"
          enter-button
          @search="doSearchMyApps"
        />
      </div>
      <a-list
        :grid="{ gutter: 24, xs: 1, sm: 2, md: 2, lg: 3, xl: 3, xxl: 3 }"
        :data-source="myApps"
        :loading="myLoading"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <article class="app-card">
              <div class="app-card__cover">
                <img v-if="item.cover" :src="item.cover" :alt="item.appName" />
                <div v-else class="app-card__placeholder">{{ item.appName || '未命名应用' }}</div>
                <div class="app-card__mask">
                  <a-space size="middle">
                    <a-button type="primary" size="large" @click="goChat(item)">查看对话</a-button>
                    <a-button v-if="item.deployKey" size="large" :href="getAppDeployUrl(item)" target="_blank">
                      查看作品
                    </a-button>
                  </a-space>
                </div>
              </div>
              <div class="app-card__body">
                <a-avatar class="app-card__avatar" :src="getAppUserAvatar(item)" />
                <div class="app-card__meta">
                  <h3>{{ item.appName || '未命名应用' }}</h3>
                  <p>{{ getAppUserName(item) }}</p>
                </div>
              </div>
            </article>
          </a-list-item>
        </template>
      </a-list>
      <a-pagination
        v-model:current="mySearchParams.pageNum"
        v-model:page-size="mySearchParams.pageSize"
        :total="myTotal"
        :page-size-options="['6', '9', '12', '20']"
        show-size-changer
        @change="fetchMyApps"
      />
    </section>

    <section class="app-section">
      <div class="section-header">
        <h2>精选案例</h2>
        <a-input-search
          v-model:value="goodSearchParams.appName"
          class="section-search"
          placeholder="搜索精选应用"
          enter-button
          @search="doSearchGoodApps"
        />
      </div>
      <a-list
        :grid="{ gutter: 24, xs: 1, sm: 2, md: 2, lg: 3, xl: 3, xxl: 3 }"
        :data-source="goodApps"
        :loading="goodLoading"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <article class="app-card">
              <div class="app-card__cover">
                <img v-if="item.cover" :src="item.cover" :alt="item.appName" />
                <div v-else class="app-card__placeholder">{{ item.appName || '精选应用' }}</div>
                <div class="app-card__mask">
                  <a-space size="middle">
                    <a-button type="primary" size="large" @click="goChat(item)">查看对话</a-button>
                    <a-button v-if="item.deployKey" size="large" :href="getAppDeployUrl(item)" target="_blank">
                      查看作品
                    </a-button>
                  </a-space>
                </div>
              </div>
              <div class="app-card__body">
                <a-avatar class="app-card__avatar" :src="getAppUserAvatar(item)" />
                <div class="app-card__meta">
                  <h3>{{ item.appName || '未命名应用' }}</h3>
                  <p>{{ getAppUserName(item) }}</p>
                </div>
              </div>
            </article>
          </a-list-item>
        </template>
      </a-list>
      <a-pagination
        v-model:current="goodSearchParams.pageNum"
        v-model:page-size="goodSearchParams.pageSize"
        :total="goodTotal"
        :page-size-options="['6', '9', '12', '20']"
        show-size-changer
        @change="fetchGoodApps"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { addApp, listGoodAppVoByPage, listMyAppVoByPage } from '@/api/appController.ts'
import { getAppDeployUrl } from '@/utils/app'

const router = useRouter()
const initPrompt = ref('')
const creating = ref(false)
const myLoading = ref(false)
const goodLoading = ref(false)
const myApps = ref<API.AppVO[]>([])
const goodApps = ref<API.AppVO[]>([])
const myTotal = ref(0)
const goodTotal = ref(0)

const promptExamples = [
  {
    label: '个人博客网站',
    prompt:
      '帮我创建一个个人博客网站，整体风格简洁高级，首页展示个人介绍、技术文章列表、项目作品和联系方式，支持响应式布局，适合程序员展示技术能力和持续更新文章。',
  },
  {
    label: '科技企业官网',
    prompt:
      '帮我创建一个企业官网，面向科技服务公司，包含首页横幅、核心服务、解决方案、客户案例、关于我们和联系表单，视觉风格专业可信，适合展示品牌实力。',
  },
  {
    label: '在线课程页面',
    prompt:
      '帮我创建一个在线课程介绍网站，用于销售前端开发课程，包含课程亮点、讲师介绍、课程大纲、学员评价、价格套餐和报名入口，页面要有转化率设计。',
  },
  {
    label: '门店展示官网',
    prompt:
      '帮我创建一个本地生活门店官网，适合咖啡店或轻食店使用，包含品牌故事、菜单展示、环境图片、营业时间、门店地址和预约按钮，整体温暖精致。',
  },
]

const mySearchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 6,
  sortField: 'createTime',
  sortOrder: 'descend',
})

const goodSearchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 6,
  sortField: 'createTime',
  sortOrder: 'descend',
})

const fetchMyApps = async () => {
  myLoading.value = true
  const res = await listMyAppVoByPage({
    ...mySearchParams,
    pageSize: Math.min(mySearchParams.pageSize || 6, 20),
  })
  if (res.data.code === 0 && res.data.data) {
    myApps.value = res.data.data.records ?? []
    myTotal.value = res.data.data.totalRow ?? 0
  } else {
    message.error('获取我的应用失败，' + res.data.message)
  }
  myLoading.value = false
}

const fetchGoodApps = async () => {
  goodLoading.value = true
  const res = await listGoodAppVoByPage({
    ...goodSearchParams,
    pageSize: Math.min(goodSearchParams.pageSize || 6, 20),
  })
  if (res.data.code === 0 && res.data.data) {
    goodApps.value = res.data.data.records ?? []
    goodTotal.value = res.data.data.totalRow ?? 0
  } else {
    message.error('获取精选应用失败，' + res.data.message)
  }
  goodLoading.value = false
}

const doCreateApp = async () => {
  if (!initPrompt.value.trim()) {
    message.warning('请输入应用需求')
    return
  }
  creating.value = true
  const res = await addApp({ initPrompt: initPrompt.value.trim() })
  creating.value = false
  if (res.data.code === 0 && res.data.data) {
    router.push({
      path: `/app/chat/${res.data.data}`,
      query: { auto: '1' },
    })
  } else {
    message.error('创建应用失败，' + res.data.message)
  }
}

const doSearchMyApps = () => {
  mySearchParams.pageNum = 1
  fetchMyApps()
}

const doSearchGoodApps = () => {
  goodSearchParams.pageNum = 1
  fetchGoodApps()
}

const goChat = (app: API.AppVO) => {
  if (app.id) {
    router.push(`/app/chat/${app.id}`)
  }
}

const getAppUserName = (app: API.AppVO) => {
  return app.user?.userName || app.user?.userAccount || '无名用户'
}

const getAppUserAvatar = (app: API.AppVO) => {
  return app.user?.userAvatar
}

onMounted(() => {
  fetchMyApps()
  fetchGoodApps()
})
</script>

<style scoped>
#homePage {
  width: 100vw;
  min-height: calc(100vh - 112px);
  padding: 56px max(24px, calc((100vw - 1200px) / 2)) 48px;
  margin: -24px calc(50% - 50vw) -48px;
  background:
    radial-gradient(circle at 18% 14%, rgba(64, 224, 208, 0.34), transparent 28%),
    radial-gradient(circle at 82% 8%, rgba(47, 111, 237, 0.36), transparent 30%),
    radial-gradient(circle at 72% 62%, rgba(113, 76, 255, 0.2), transparent 34%),
    linear-gradient(135deg, #eefcff 0%, #f7f9ff 42%, #eef4ff 100%);
}

.hero {
  max-width: 1040px;
  min-height: auto;
  padding: 48px 16px 36px;
  margin: 0 auto;
  text-align: center;
}

.hero__brand {
  color: #0f172a;
  font-size: 56px;
  font-weight: 800;
  line-height: 1.12;
}

.hero__desc {
  margin: 18px 0 36px;
  color: #475467;
  font-size: 22px;
}

.prompt-box {
  max-width: 900px;
  margin: 0 auto;
  padding: 18px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 24px;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(18px);
}

.prompt-box :deep(textarea.ant-input) {
  border: 0;
  background: transparent;
  box-shadow: none;
  font-size: 18px;
  resize: none;
}

.prompt-box__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
}

.app-section {
  margin-top: 48px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.section-header h2 {
  margin: 0;
  color: #101828;
  font-size: 32px;
  font-weight: 700;
}

.section-search {
  max-width: 320px;
}

.app-card {
  overflow: hidden;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 16px;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.1);
  backdrop-filter: blur(16px);
}

.app-card__cover {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: #f2f4f7;
}

.app-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-card__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 24px;
  color: #667085;
  font-size: 20px;
  font-weight: 600;
  background: linear-gradient(135deg, #f7f7f8, #eef8f7);
}

.app-card__mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.42);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.app-card__cover:hover .app-card__mask {
  opacity: 1;
}

.app-card__body {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 20px 20px;
}

.app-card__avatar {
  flex: 0 0 auto;
  width: 42px;
  height: 42px;
}

.app-card__meta {
  min-width: 0;
}

.app-card__meta h3 {
  margin: 0 0 4px;
  overflow: hidden;
  color: #101828;
  font-size: 20px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-card__meta p {
  margin: 0;
  overflow: hidden;
  color: #667085;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .hero {
    min-height: auto;
    padding: 32px 0 28px;
    border-radius: 20px;
  }

  .hero__brand {
    font-size: 32px;
  }

  .hero__desc {
    margin: 16px 0 28px;
    font-size: 16px;
  }

  .prompt-box {
    padding: 16px;
    border-radius: 20px;
  }

  .prompt-box__footer,
  .section-header {
    align-items: stretch;
    flex-direction: column;
  }

  .app-card__mask {
    opacity: 1;
  }

  .section-search {
    max-width: none;
  }
}
</style>
