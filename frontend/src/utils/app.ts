export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
export const DEPLOY_DOMAIN = (import.meta.env.VITE_DEPLOY_DOMAIN || window.location.origin).replace(/\/$/, '')
export const STATIC_BASE_URL = `${API_BASE_URL}/static/code_output`
export const CODE_GEN_TYPE = {
  HTML: 'html',
  MULTI_FILE: 'multi_file',
  VUE_PROJECT: 'vue_project',
} as const

export const getStaticPreviewUrl = (codeGenType?: string, appId?: string) => {
  if (!appId) {
    return ''
  }
  const type = codeGenType || CODE_GEN_TYPE.HTML
  const baseUrl = `${STATIC_BASE_URL}/${type}_${appId}/`
  if (type === CODE_GEN_TYPE.VUE_PROJECT) {
    return `${baseUrl}dist/index.html`
  }
  return `${baseUrl}index.html`
}

export const getAppPreviewUrl = (app?: API.AppVO) => {
  if (!app?.id) {
    return ''
  }
  return getStaticPreviewUrl(app.codeGenType, app.id as unknown as string)
}

export const getAppDeployUrl = (app?: API.AppVO) => {
  if (!app?.deployKey) {
    return ''
  }
  return `${DEPLOY_DOMAIN}/${app.deployKey}`
}

export const formatDateTime = (value?: string) => {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString()
}
