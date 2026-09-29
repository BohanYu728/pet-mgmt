import axios from 'axios'

const apiBase = import.meta.env?.VITE_API_BASE_URL?.replace(/\/$/, '') || '/api'
export const api = axios.create({ baseURL: apiBase, withCredentials: true, timeout: 20000 })
let csrfRequest = null

export function ensureCsrf() {
  if (!csrfRequest) {
    const request = api.get('/auth/csrf').then(({ data }) => data).finally(() => {
      if (csrfRequest === request) csrfRequest = null
    })
    csrfRequest = request
  }
  return csrfRequest
}

export function invalidateCsrf() { csrfRequest = null }

api.interceptors.request.use(async config => {
  if (!['get', 'head', 'options'].includes((config.method || 'get').toLowerCase())) {
    const value = await ensureCsrf()
    config.headers[value.headerName || 'X-XSRF-TOKEN'] = value.token
  }
  return config
})

api.interceptors.response.use(response => response, error => {
  if (error?.response?.status === 403) invalidateCsrf()
  return Promise.reject(error)
})

export function errorMessage(error) {
  const status = error?.response?.status
  const body = error?.response?.data
  if (body?.error) return body.error
  if (status === 400) return '提交内容有误，请检查必填项。'
  if (status === 401) return '登录已过期，请重新登录。'
  if (status === 403) return '您没有权限执行此操作。'
  if (status === 404) return '请求的内容不存在。'
  if (status === 409) return '数据已发生冲突，请刷新后重试。'
  if (status >= 500 || !error?.response) return '服务暂时不可用，请稍后再试。'
  return body?.message || '操作失败，请重试。'
}

export function unwrap(response) {
  const body = response?.data ?? response
  if (body?.error) throw new Error(body.error)
  return body?.data ?? body
}

export function imageUrl(path) {
  if (!path) return ''
  if (/^(https?:|data:)/.test(path)) return path
  const origin = new URL(apiBase, globalThis.location?.origin || 'http://localhost').origin
  return origin + (path.startsWith('/') ? path : `/${path}`)
}

export async function allPets() {
  const result = []
  for (let page = 1; page <= 100; page++) {
    const data = (await api.get('/pets', { params: { page } })).data
    result.push(...(data.pets || []))
    if (page >= (data.pages || 1)) break
  }
  return result
}
