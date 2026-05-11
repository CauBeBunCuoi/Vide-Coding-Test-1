import axios from 'axios'

export const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_BACKEND_REST_API_URL ?? '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})
