import client from './client'
import type { TokenResponse, UserResponse } from '@/types/api'

export interface RegisterPayload {
  username: string
  email: string
  password: string
}

export interface LoginPayload {
  email: string
  password: string
}

export const register = (data: RegisterPayload): Promise<UserResponse> =>
  client.post<UserResponse>('/auth/register', data).then((r) => r.data)

export const login = (data: LoginPayload): Promise<TokenResponse> =>
  client.post<TokenResponse>('/auth/login', data).then((r) => r.data)

export const logout = (): Promise<void> =>
  client.post('/auth/logout').then(() => undefined)
