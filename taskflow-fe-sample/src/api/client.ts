import axios from 'axios'
import { useAuthStore } from '@/stores/authStore'
import type { TokenResponse } from '@/types/api'

const client = axios.create({
  baseURL: import.meta.env.VITE_BACKEND_REST_API_URL,
  withCredentials: true,
})

// Attach Bearer token to every request
client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// On 401: refresh once, retry queued requests, redirect on failure
let isRefreshing = false
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (err: unknown) => void
}> = []

function drainQueue(error: unknown, token: string | null = null) {
  failedQueue.forEach((p) => (error ? p.reject(error) : p.resolve(token!)))
  failedQueue = []
}

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config as typeof error.config & { _retry?: boolean }

    if (error.response?.status !== 401 || original._retry) {
      return Promise.reject(error)
    }

    if (isRefreshing) {
      return new Promise<string>((resolve, reject) => {
        failedQueue.push({ resolve, reject })
      }).then((token) => {
        original.headers.Authorization = `Bearer ${token}`
        return client(original)
      })
    }

    original._retry = true
    isRefreshing = true

    try {
      const { data } = await client.post<TokenResponse>('/auth/refresh')
      useAuthStore.getState().setAccessToken(data.accessToken)
      drainQueue(null, data.accessToken)
      original.headers.Authorization = `Bearer ${data.accessToken}`
      return client(original)
    } catch (refreshError) {
      drainQueue(refreshError)
      useAuthStore.getState().logout()
      window.location.href = '/login'
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  },
)

export default client
