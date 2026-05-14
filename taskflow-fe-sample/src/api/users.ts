import client from './client'
import type { UserResponse } from '@/types/api'

export const getMe = (): Promise<UserResponse> =>
  client.get<UserResponse>('/users/me').then((r) => r.data)
