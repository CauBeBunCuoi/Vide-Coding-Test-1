import { create } from 'zustand'
import type { UserResponse } from '@/types/api'

interface AuthState {
  accessToken: string | null
  user: UserResponse | null
  isAuthenticated: boolean
}

interface AuthActions {
  setAuth: (token: string, user: UserResponse) => void
  setAccessToken: (token: string) => void
  logout: () => void
}

export const useAuthStore = create<AuthState & AuthActions>((set) => ({
  accessToken: null,
  user: null,
  isAuthenticated: false,
  setAuth: (token, user) => set({ accessToken: token, user, isAuthenticated: true }),
  setAccessToken: (token) => set({ accessToken: token }),
  logout: () => set({ accessToken: null, user: null, isAuthenticated: false }),
}))
