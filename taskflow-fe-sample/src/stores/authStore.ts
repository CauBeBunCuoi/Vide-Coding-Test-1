import { create } from 'zustand'

interface AuthStore {
  accessToken: string | null
  user: unknown | null
  isAuthenticated: boolean
  setAccessToken: (token: string) => void
  logout: () => void
}

export const useAuthStore = create<AuthStore>(() => ({
  accessToken: null,
  user: null,
  isAuthenticated: false,
  setAccessToken: () => {},
  logout: () => {},
}))
