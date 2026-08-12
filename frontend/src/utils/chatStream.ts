import { reactive } from 'vue'
import { API_BASE_URL } from '@/utils/app'

export type ActiveChatStream = {
  appId: string
  userMessage: string
  assistantContent: string
  generating: boolean
  error?: string
  userMessageId: string
  assistantMessageId: string
}

export const activeChatStreams = reactive<Record<string, ActiveChatStream>>({})

let streamId = 1
const streamBuffers: Record<string, string> = {}
const streamTimers: Record<string, ReturnType<typeof window.setTimeout> | undefined> = {}
const eventSources: Record<string, EventSource | undefined> = {}

const getSseContent = (data: string) => {
  try {
    const parsed = JSON.parse(data)
    if (typeof parsed?.d === 'string') {
      return parsed.d
    }
  } catch {
    return data
  }
  return ''
}

const flushStreamBuffer = (appId: string) => {
  const stream = activeChatStreams[appId]
  const buffer = streamBuffers[appId]
  if (!stream || !buffer) {
    return
  }
  stream.assistantContent += buffer
  streamBuffers[appId] = ''
}

const appendStreamContent = (appId: string, content: string) => {
  streamBuffers[appId] = `${streamBuffers[appId] || ''}${content}`
  if (streamTimers[appId]) {
    return
  }
  streamTimers[appId] = window.setTimeout(() => {
    streamTimers[appId] = undefined
    flushStreamBuffer(appId)
  }, 80)
}

const finishStream = (appId: string) => {
  if (streamTimers[appId]) {
    window.clearTimeout(streamTimers[appId])
    streamTimers[appId] = undefined
  }
  flushStreamBuffer(appId)
  const stream = activeChatStreams[appId]
  if (stream) {
    stream.generating = false
  }
  eventSources[appId]?.close()
  eventSources[appId] = undefined
}

export const getActiveChatStream = (appId: string) => activeChatStreams[appId]

export const markChatStreamFinished = (appId: string) => {
  finishStream(appId)
}

export const startChatStream = (appId: string, message: string) => {
  const currentStream = activeChatStreams[appId]
  if (currentStream?.generating) {
    return currentStream
  }
  const currentStreamId = streamId++
  const stream = {
    appId,
    userMessage: message,
    assistantContent: '',
    generating: true,
    userMessageId: `stream-user-${appId}-${currentStreamId}`,
    assistantMessageId: `stream-ai-${appId}-${currentStreamId}`,
  }
  activeChatStreams[appId] = stream
  streamBuffers[appId] = ''

  const url = `${API_BASE_URL}/app/chat/gen/code?appId=${appId}&message=${encodeURIComponent(message)}`
  const eventSource = new EventSource(url, { withCredentials: true })
  eventSources[appId] = eventSource

  eventSource.onmessage = (event) => {
    if (!event.data || event.data === '[DONE]') {
      finishStream(appId)
      return
    }
    const content = getSseContent(event.data)
    if (content) {
      appendStreamContent(appId, content)
    }
  }

  eventSource.addEventListener('done', () => {
    finishStream(appId)
  })

  eventSource.onerror = () => {
    const stream = activeChatStreams[appId]
    if (streamBuffers[appId] || stream?.assistantContent) {
      finishStream(appId)
      return
    }
    if (stream) {
      stream.error = '生成失败，请稍后重试'
      stream.generating = false
    }
    eventSource.close()
    eventSources[appId] = undefined
  }

  return stream
}
