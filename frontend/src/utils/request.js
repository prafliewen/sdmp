import axios from 'axios'
import { useUserStore } from '../store/user'

const request = axios.create({
  baseURL: '',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json; charset=UTF-8' }
})

// Request interceptor - add X-User header
request.interceptors.request.use(config => {
  try {
    const userStore = useUserStore()
    if (userStore.operator) {
      config.headers['X-User'] = userStore.operator
    }
  } catch (e) {
    // store not available during setup
  }
  return config
})

// Response interceptor - uniform error handling.
// On success the wrapper returns the unwrapped `data` field so that callers
// can read `data.records` / `data.total` / `data.id` directly without going
// through a `data.data` chain.
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 0) {
      showToast(res.message || '请求失败', 'error')
      return Promise.reject(new Error(res.message))
    }
    return res.data
  },
  error => {
    const msg = error.response?.data?.message || error.message || '网络错误'
    showToast(msg, 'error')
    return Promise.reject(error)
  }
)

function showToast(message, type) {
  let container = document.getElementById('app-toast-container')
  if (!container) {
    container = document.createElement('div')
    container.id = 'app-toast-container'
    container.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 9999; display: flex; flex-direction: column; gap: 8px; pointer-events: none;'
    document.body.appendChild(container)
  }
  const toast = document.createElement('div')
  toast.className = `toast toast-${type}`
  toast.textContent = message
  toast.style.pointerEvents = 'auto'
  container.appendChild(toast)
  setTimeout(() => toast.remove(), 3000)
}

export function showSuccess(message) {
  showToast(message, 'success')
}

export function showError(message) {
  showToast(message, 'error')
}

export default request