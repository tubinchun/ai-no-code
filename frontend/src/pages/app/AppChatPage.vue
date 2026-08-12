<template>
  <div id="appChatPage">
    <header class="chat-header">
      <div class="chat-header__title">
        <img src="@/assets/logo.png" alt="logo" />
        <span>{{ appInfo?.appName || '未命名应用' }}</span>
        <a-tag v-if="appInfo?.codeGenType" color="blue">{{ codeGenTypeText }}</a-tag>
      </div>
      <a-space>
        <a-button @click="detailVisible = true">应用详情</a-button>
        <a-button v-if="previewReady && previewUrl" :href="previewUrl" target="_blank">打开预览</a-button>
        <a-button :loading="downloading" :disabled="!canChat" @click="doDownloadCode">下载代码</a-button>
        <a-button type="primary" :loading="deploying" @click="doDeployApp">部署</a-button>
      </a-space>
    </header>

    <main class="chat-main">
      <section class="conversation">
        <div ref="messageListRef" class="message-list">
          <div v-if="hasMoreHistory" class="history-load-more">
            <a-button type="link" :loading="historyLoading" @click="loadMoreHistory">加载更多</a-button>
          </div>
          <div
            v-for="item in messages"
            :key="item.id"
            class="message-item"
            :class="`message-item--${item.role}`"
          >
            <a-avatar v-if="item.role === 'assistant'" :src="logoUrl" />
            <div class="message-bubble">
              <div
                v-if="item.role === 'assistant'"
                class="message-content markdown-body"
                v-html="renderMarkdown(item.content)"
              />
              <div v-else class="message-content">{{ item.content }}</div>
              <div v-if="item.status" class="message-status">{{ item.status }}</div>
            </div>
          </div>
          <a-empty v-if="messages.length === 0" description="输入需求后开始生成网站" />
        </div>

        <a-tooltip :title="canChat ? '' : '无法在别人的作品下对话哦~'">
          <div class="message-input">
            <a-alert
              v-if="selectedElement"
              class="selected-element-alert"
              type="info"
              show-icon
              closable
              :message="`已选择元素：${selectedElementSummary}`"
              :description="selectedElement.text ? `文本：${selectedElement.text}` : undefined"
              @close="clearSelectedElement"
            />
            <a-textarea
              v-model:value="userMessage"
              :auto-size="{ minRows: 4, maxRows: 6 }"
              :disabled="generating || !canChat"
              placeholder="请描述你想生成的网站，越详细效果越好哦"
              @press-enter.ctrl="sendUserMessage"
            />
            <div class="message-input__footer">
              <a-space>
                <a-button :disabled="!canChat">上传</a-button>
                <a-button
                  :type="visualEditMode ? 'primary' : 'default'"
                  :disabled="!canChat || !canShowPreview"
                  @click="toggleVisualEditMode"
                >
                  {{ visualEditMode ? '退出编辑' : '编辑' }}
                </a-button>
              </a-space>
              <a-button
                type="primary"
                shape="circle"
                :loading="generating"
                :disabled="!userMessage.trim() || !canChat"
                @click="sendUserMessage"
              >
                ↑
              </a-button>
            </div>
          </div>
        </a-tooltip>
      </section>

      <section class="preview-panel" :class="{ 'preview-panel--editing': visualEditMode }">
        <iframe
          v-if="canShowPreview && previewDisplayUrl"
          ref="previewIframeRef"
          :key="previewKey"
          :src="previewDisplayUrl"
          title="网页预览"
          @load="handlePreviewLoad"
        />
        <a-empty v-else class="preview-empty" :description="previewEmptyText" />
      </section>
    </main>

    <a-modal v-model:open="detailVisible" title="应用详情" :footer="null" width="560px">
      <div class="app-detail">
        <h3>应用基础信息</h3>
        <div class="detail-row">
          <span class="detail-label">创建者</span>
          <a-space>
            <a-avatar :src="appInfo?.user?.userAvatar" />
            <span>{{ appInfo?.user?.userName || appInfo?.user?.userAccount || '无名' }}</span>
          </a-space>
        </div>
        <div class="detail-row">
          <span class="detail-label">创建时间</span>
          <span>{{ formatDateTime(appInfo?.createTime) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">生成类型</span>
          <a-tag color="blue">{{ codeGenTypeText }}</a-tag>
        </div>

        <template v-if="canManageApp">
          <a-divider />
          <h3>操作栏</h3>
          <a-space>
            <a-button type="primary" @click="goEditApp">修改</a-button>
            <a-popconfirm title="确定删除这个应用吗？" @confirm="doDeleteApp">
              <a-button danger :loading="deleting">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </div>
    </a-modal>

    <a-modal v-model:open="deploySuccessVisible" title="部署成功" :footer="null" width="620px">
      <div class="deploy-success">
        <div class="deploy-success__icon">✓</div>
        <h2>网站部署成功！</h2>
        <p>你的网站已经成功部署，可以通过以下链接访问：</p>
        <a-input-group compact class="deploy-success__link">
          <a-input :value="deployUrl" readonly />
          <a-tooltip title="复制链接">
            <a-button @click="copyDeployUrl">复制</a-button>
          </a-tooltip>
        </a-input-group>
        <a-space>
          <a-button type="primary" @click="openDeployUrl">访问网站</a-button>
          <a-button @click="deploySuccessVisible = false">关闭</a-button>
        </a-space>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import logoUrl from '@/assets/logo.png'
import { deleteApp, deleteAppByAdmin, deployApp, getAppVoById } from '@/api/appController.ts'
import { listAppChatHistoryVoByPage } from '@/api/chatHistoryController.ts'
import { formatDateTime, getAppPreviewUrl } from '@/utils/app'
import { activeChatStreams, startChatStream } from '@/utils/chatStream'
import {
  createVisualEditor,
  formatSelectedElementPrompt,
  formatSelectedElementSummary,
  type SelectedElementInfo,
  type VisualEditorController,
} from '@/utils/visualEditor'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import request from '@/request'

type ChatMessage = {
  id: string
  role: 'user' | 'assistant'
  content: string
  status?: string
}

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const appInfo = ref<API.AppVO>()
const userMessage = ref('')
const messages = ref<ChatMessage[]>([])
const historyLoading = ref(false)
const hasMoreHistory = ref(false)
const activeStream = computed(() => activeChatStreams[appId.value])
const generating = computed(() => Boolean(activeStream.value?.generating))
const downloading = ref(false)
const deploying = ref(false)
const deleting = ref(false)
const detailVisible = ref(false)
const deploySuccessVisible = ref(false)
const deployUrl = ref('')
const showPreview = ref(false)
const previewReady = ref(false)
const previewChecking = ref(false)
const previewKey = ref(0)
const messageListRef = ref<HTMLDivElement>()
const previewIframeRef = ref<HTMLIFrameElement>()
const visualEditMode = ref(false)
const selectedElement = ref<SelectedElementInfo>()
const historyCursor = ref<string>()
let messageId = 1
let scrollTimer: ReturnType<typeof window.requestAnimationFrame> | undefined
let previewCheckTimer: ReturnType<typeof window.setTimeout> | undefined
let visualEditor: VisualEditorController | undefined

const escapeHtml = (content: string) =>
  content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight: (str: string, lang: string): string => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang, ignoreIllegals: true }).value}</code></pre>`
      } catch {
        return ''
      }
    }
    return `<pre class="hljs"><code>${escapeHtml(str)}</code></pre>`
  },
})

const appId = computed(() => String(route.params.id || ''))
const previewUrl = computed(() => getAppPreviewUrl(appInfo.value))
const previewDisplayUrl = computed(() => {
  if (!previewUrl.value) {
    return ''
  }
  const separator = previewUrl.value.includes('?') ? '&' : '?'
  return `${previewUrl.value}${separator}t=${previewKey.value}`
})
const canShowPreview = computed(() => Boolean(showPreview.value && previewReady.value && previewUrl.value))
const previewEmptyText = computed(() => {
  if (previewChecking.value) {
    return '网站正在构建中，完成后会自动刷新预览'
  }
  return '网站文件生成完成后将在这里展示'
})
const isOwner = computed(() => {
  if (!appInfo.value?.userId || !loginUserStore.loginUser.id) {
    return false
  }
  return String(appInfo.value.userId) === String(loginUserStore.loginUser.id)
})
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')
const canChat = computed(() => isOwner.value)
const canManageApp = computed(() => isOwner.value || isAdmin.value)
const codeGenTypeText = computed(() => {
  const codeGenType = appInfo.value?.codeGenType
  const codeGenTypeMap: Record<string, string> = {
    html: '原生 HTML',
    multi_file: '多文件',
    vue_project: 'Vue 工程',
  }
  return codeGenType ? codeGenTypeMap[codeGenType] || codeGenType : '-'
})
const selectedElementSummary = computed(() =>
  selectedElement.value ? formatSelectedElementSummary(selectedElement.value) : ''
)

const renderMarkdown = (content: string) => markdown.render(content || '')

const toChatMessage = (item: API.ChatHistoryVO): ChatMessage => ({
  id: item.id ? String(item.id) : `history-${messageId++}`,
  role: item.messageType === 'user' ? 'user' : 'assistant',
  content: item.message || '',
})

const scrollToBottom = async () => {
  await nextTick()
  const el = messageListRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

const scheduleScrollToBottom = () => {
  if (scrollTimer) {
    return
  }
  scrollTimer = window.requestAnimationFrame(() => {
    scrollTimer = undefined
    scrollToBottom()
  })
}

const fetchAppInfo = async () => {
  if (!appId.value) {
    message.error('应用不存在')
    return
  }
  const res = await getAppVoById({ id: appId.value } as unknown as API.getAppVOByIdParams)
  if (res.data.code === 0 && res.data.data) {
    appInfo.value = res.data.data
  } else {
    message.error('获取应用详情失败，' + res.data.message)
  }
}

const fetchHistory = async (loadMore = false) => {
  if (!appId.value || historyLoading.value || !canManageApp.value) {
    return
  }
  historyLoading.value = true
  const res = await listAppChatHistoryVoByPage({
    appId: appId.value,
    pageSize: 10,
    lastCreateTime: loadMore ? historyCursor.value : undefined,
  } as unknown as API.ChatHistoryQueryRequest)
  historyLoading.value = false
  if (res.data.code !== 0 || !res.data.data) {
    message.error('获取对话历史失败，' + res.data.message)
    return
  }
  const records = res.data.data.records ?? []
  const orderedRecords = [...records].reverse()
  const historyMessages = orderedRecords.map(toChatMessage)
  messages.value = loadMore ? [...historyMessages, ...messages.value] : historyMessages
  hasMoreHistory.value = (res.data.data.totalRow ?? 0) > records.length
  historyCursor.value = orderedRecords[0]?.createTime
  showPreview.value = Boolean(appInfo.value?.codeGenType && messages.value.length >= 2)
  if (!loadMore) {
    await scrollToBottom()
  }
}

const loadMoreHistory = async () => {
  const el = messageListRef.value
  const oldScrollHeight = el?.scrollHeight ?? 0
  await fetchHistory(true)
  await nextTick()
  if (el) {
    el.scrollTop = el.scrollHeight - oldScrollHeight
  }
}

const clearStreamTimers = () => {
  if (scrollTimer) {
    window.cancelAnimationFrame(scrollTimer)
    scrollTimer = undefined
  }
  if (previewCheckTimer) {
    window.clearTimeout(previewCheckTimer)
    previewCheckTimer = undefined
  }
}

const setupVisualEditor = () => {
  if (!previewIframeRef.value) {
    return
  }
  visualEditor?.destroy()
  visualEditor = createVisualEditor({
    iframe: previewIframeRef.value,
    onSelect: (info) => {
      selectedElement.value = info
    },
  })
  if (visualEditMode.value) {
    visualEditor.enable()
  }
}

const handlePreviewLoad = () => {
  setupVisualEditor()
}

const clearSelectedElement = () => {
  selectedElement.value = undefined
  visualEditor?.clearSelection()
}

const exitVisualEditMode = () => {
  visualEditMode.value = false
  visualEditor?.disable()
}

const toggleVisualEditMode = () => {
  if (!visualEditMode.value && !canShowPreview.value) {
    message.warning('请等待网站预览加载完成后再编辑')
    return
  }
  visualEditMode.value = !visualEditMode.value
  if (visualEditMode.value) {
    setupVisualEditor()
    visualEditor?.enable()
    message.info('已进入可视化编辑模式，请在右侧页面中选择元素')
    return
  }
  exitVisualEditMode()
}

const checkPreviewReady = async (retryCount = 0) => {
  if (!previewUrl.value || (!showPreview.value && !activeStream.value?.assistantContent)) {
    previewReady.value = false
    previewChecking.value = false
    return
  }
  previewChecking.value = true
  try {
    const checkUrl = previewUrl.value.endsWith('/') ? `${previewUrl.value}index.html` : previewUrl.value
    const separator = checkUrl.includes('?') ? '&' : '?'
    const res = await fetch(`${checkUrl}${separator}t=${Date.now()}`, {
      method: 'GET',
      cache: 'no-store',
    })
    if (res.ok) {
      previewReady.value = true
      previewChecking.value = false
      previewKey.value += 1
      return
    }
  } catch {
    // 静态资源未就绪时继续轮询
  }
  previewReady.value = false
  if (retryCount >= 60 || (!showPreview.value && !activeStream.value?.assistantContent)) {
    previewChecking.value = false
    return
  }
  previewCheckTimer = window.setTimeout(() => {
    checkPreviewReady(retryCount + 1)
  }, 1000)
}

const sendMessage = async (content: string) => {
  if (!content.trim() || generating.value || !appId.value || !canChat.value) {
    return
  }
  const text = content.trim()
  const prompt = selectedElement.value ? `${text}${formatSelectedElementPrompt(selectedElement.value)}` : text
  userMessage.value = ''
  showPreview.value = false
  previewReady.value = false
  previewChecking.value = false
  startChatStream(appId.value, prompt)
  clearSelectedElement()
  exitVisualEditMode()
  syncActiveStream()
  await scrollToBottom()
}

const sendUserMessage = () => {
  sendMessage(userMessage.value)
}

const getDownloadFileName = (contentDisposition?: string) => {
  if (!contentDisposition) {
    return `${appId.value}.zip`
  }
  const utf8FileNameMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8FileNameMatch?.[1]) {
    return decodeURIComponent(utf8FileNameMatch[1])
  }
  const fileNameMatch = contentDisposition.match(/filename="?([^"]+)"?/i)
  if (fileNameMatch?.[1]) {
    return decodeURIComponent(fileNameMatch[1])
  }
  return `${appId.value}.zip`
}

const doDownloadCode = async () => {
  if (!appId.value || downloading.value) {
    return
  }
  downloading.value = true
  try {
    const res = await request.get<Blob>(`/app/download/${appId.value}`, {
      responseType: 'blob',
    })
    const contentType = String(res.headers['content-type'] || '')
    if (contentType.includes('application/json')) {
      const errorText = await res.data.text()
      const errorData = JSON.parse(errorText)
      message.error('下载失败，' + (errorData.message || '请稍后重试'))
      return
    }
    const blob = new Blob([res.data], { type: contentType || 'application/zip' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = getDownloadFileName(String(res.headers['content-disposition'] || ''))
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    message.success('下载已开始')
  } catch (error: any) {
    message.error('下载失败，' + (error?.response?.data?.message || error?.message || '请稍后重试'))
  } finally {
    downloading.value = false
  }
}

const doDeployApp = async () => {
  if (!appId.value) {
    return
  }
  deploying.value = true
  const res = await deployApp({ appId: appId.value } as unknown as API.AppDeployRequest)
  deploying.value = false
  if (res.data.code === 0 && res.data.data) {
    deployUrl.value = res.data.data
    deploySuccessVisible.value = true
    message.success('部署成功')
  } else {
    message.error('部署失败，' + res.data.message)
  }
}

const copyDeployUrl = async () => {
  if (!deployUrl.value) {
    return
  }
  await navigator.clipboard.writeText(deployUrl.value)
  message.success('链接已复制')
}

const openDeployUrl = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

const goEditApp = () => {
  if (!appId.value) {
    return
  }
  router.push(isAdmin.value ? `/admin/app/edit/${appId.value}` : `/app/edit/${appId.value}`)
}

const doDeleteApp = async () => {
  if (!appId.value) {
    return
  }
  deleting.value = true
  const deleteRequest = { id: appId.value } as unknown as API.DeleteRequest
  const res = isAdmin.value ? await deleteAppByAdmin(deleteRequest) : await deleteApp(deleteRequest)
  deleting.value = false
  if (res.data.code === 0) {
    message.success('删除成功')
    await router.push('/')
  } else {
    message.error('删除失败，' + res.data.message)
  }
}

const upsertMessage = (chatMessage: ChatMessage) => {
  const index = messages.value.findIndex((item) => item.id === chatMessage.id)
  if (index >= 0) {
    messages.value[index] = chatMessage
    return
  }
  messages.value.push(chatMessage)
}

const syncActiveStream = () => {
  const stream = activeStream.value
  if (!stream) {
    return
  }
  const hasSameUserHistory = messages.value.some(
    (item) => item.role === 'user' && item.content === stream.userMessage
  )
  if (!hasSameUserHistory) {
    upsertMessage({
      id: stream.userMessageId,
      role: 'user',
      content: stream.userMessage,
    })
  }
  const streamContent = stream.error || stream.assistantContent
  const hasSameAssistantHistory = Boolean(
    streamContent && messages.value.some((item) => item.role === 'assistant' && item.content === streamContent)
  )
  if (!stream.generating && hasSameAssistantHistory) {
    const assistantIndex = messages.value.findIndex((item) => item.id === stream.assistantMessageId)
    if (assistantIndex >= 0) {
      messages.value[assistantIndex] = {
        ...messages.value[assistantIndex],
        content: streamContent,
        status: undefined,
      }
    }
    showPreview.value = Boolean(appInfo.value?.codeGenType && messages.value.length >= 2)
    return
  }
  upsertMessage({
    id: stream.assistantMessageId,
    role: 'assistant',
    content: streamContent,
    status: stream.generating ? '生成中...' : undefined,
  })
  showPreview.value = Boolean(appInfo.value?.codeGenType && streamContent)
  scheduleScrollToBottom()
}

watch(
  () => [
    activeStream.value?.assistantContent,
    activeStream.value?.generating,
    activeStream.value?.error,
    activeStream.value?.userMessage,
  ],
  () => {
    syncActiveStream()
    if (
      activeStream.value?.assistantContent &&
      !activeStream.value.generating &&
      previewUrl.value &&
      !previewReady.value &&
      !previewChecking.value
    ) {
      checkPreviewReady()
    }
  }
)

watch(
  () => [showPreview.value, previewUrl.value],
  () => {
    if (previewCheckTimer) {
      window.clearTimeout(previewCheckTimer)
      previewCheckTimer = undefined
    }
    previewReady.value = false
    if (
      (showPreview.value || activeStream.value?.assistantContent) &&
      !activeStream.value?.generating &&
      previewUrl.value
    ) {
      checkPreviewReady()
    } else {
      previewChecking.value = false
    }
  }
)

onMounted(async () => {
  await fetchAppInfo()
  await fetchHistory()
  syncActiveStream()
  if (messages.value.length === 0 && appInfo.value?.initPrompt && canChat.value) {
    sendMessage(appInfo.value.initPrompt)
  }
})

onBeforeUnmount(() => {
  clearStreamTimers()
  visualEditor?.destroy()
})
</script>

<style scoped>
#appChatPage {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 160px);
  min-height: 560px;
  overflow: hidden;
}

.chat-header {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  height: 56px;
  margin-bottom: 10px;
  padding: 0 12px;
  background: #ffffff;
  border: 1px solid #edf0f5;
  border-radius: 10px;
}

.chat-header__title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  color: #101828;
  font-size: 18px;
  font-weight: 600;
}

.chat-header__title img {
  width: 32px;
  height: 32px;
}

.chat-header__title span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-main {
  display: grid;
  grid-template-columns: minmax(340px, 2fr) minmax(520px, 3fr);
  gap: 10px;
  flex: 1 1 auto;
  min-height: 0;
}

.conversation,
.preview-panel {
  min-height: 0;
  background: #ffffff;
  border: 1px solid #edf0f5;
  border-radius: 12px;
}

.conversation {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.message-list {
  flex: 1 1 auto;
  min-height: 0;
  padding: 12px;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.history-load-more {
  margin-bottom: 12px;
  text-align: center;
}

.message-item {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.message-item--user {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 82%;
  padding: 12px 14px;
  color: #1f2937;
  line-height: 1.7;
  white-space: pre-wrap;
  background: #f6f7f9;
  border-radius: 12px;
}

.message-item--user .message-bubble {
  color: #ffffff;
  background: #1677ff;
}

.message-status {
  margin-top: 8px;
  color: #98a2b3;
  font-size: 12px;
}

.markdown-body {
  white-space: normal;
}

.markdown-body :deep(p) {
  margin: 0 0 10px;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 22px;
  margin: 8px 0 12px;
}

.markdown-body :deep(li) {
  margin: 4px 0;
}

.markdown-body :deep(pre.hljs) {
  margin: 12px 0;
  padding: 14px 16px;
  overflow-x: auto;
  border-radius: 8px;
}

.markdown-body :deep(code) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

.markdown-body :deep(:not(pre) > code) {
  padding: 2px 6px;
  color: #d63384;
  background: #fff0f6;
  border-radius: 4px;
}

.markdown-body :deep(blockquote) {
  padding-left: 12px;
  margin: 10px 0;
  color: #667085;
  border-left: 3px solid #d0d5dd;
}

.markdown-body :deep(table) {
  width: 100%;
  margin: 12px 0;
  border-collapse: collapse;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 8px 10px;
  border: 1px solid #eaecf0;
}

.markdown-body :deep(a) {
  color: #1677ff;
}

.message-input {
  flex: 0 0 auto;
  margin: 0 12px 12px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #edf0f5;
  border-radius: 12px;
}

.message-input :deep(textarea.ant-input) {
  background: transparent;
  border: 0;
  box-shadow: none;
  resize: none;
}

.selected-element-alert {
  margin-bottom: 10px;
}

.message-input__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}

.preview-panel {
  overflow: hidden;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.preview-panel--editing {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
}

.preview-panel iframe {
  width: 100%;
  height: 100%;
  min-height: 0;
  border: 0;
}

.preview-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 0;
}

.app-detail h3 {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
}

.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.detail-label {
  color: #667085;
}

.deploy-success {
  padding: 16px 24px 8px;
  text-align: center;
}

.deploy-success__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  margin-bottom: 18px;
  color: #52c41a;
  font-size: 34px;
  font-weight: 700;
  border: 4px solid #52c41a;
  border-radius: 50%;
}

.deploy-success h2 {
  margin: 0 0 16px;
  color: #101828;
  font-size: 22px;
  font-weight: 700;
}

.deploy-success p {
  margin: 0 0 20px;
  color: #667085;
}

.deploy-success__link {
  display: flex;
  margin: 0 auto 24px;
  max-width: 480px;
}

.deploy-success__link :deep(.ant-input) {
  flex: 1;
}

@media (max-width: 1024px) {
  #appChatPage {
    height: auto;
    min-height: calc(100vh - 160px);
    overflow: visible;
  }

  .chat-main {
    grid-template-columns: 1fr;
  }

  .message-list {
    max-height: 52vh;
  }

  .preview-panel iframe {
    min-height: 520px;
  }
}
</style>
