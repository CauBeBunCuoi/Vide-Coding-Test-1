import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { login, logout, register } from '@/api/auth'
import { useAuthStore } from '@/stores/authStore'
import client from '@/api/client'
import type { UserResponse } from '@/types/api'

export function useLogin() {
  const navigate = useNavigate()
  const { setAuth } = useAuthStore()

  return useMutation({
    mutationFn: async (data: { email: string; password: string }) => {
      const tokenData = await login(data)
      const user = await client
        .get<UserResponse>('/users/me', {
          headers: { Authorization: `Bearer ${tokenData.accessToken}` },
        })
        .then((r) => r.data)
      return { tokenData, user }
    },
    onSuccess: ({ tokenData, user }) => {
      setAuth(tokenData.accessToken, user)
      navigate('/')
    },
  })
}

export function useRegister() {
  const navigate = useNavigate()
  const { setAuth } = useAuthStore()

  return useMutation({
    mutationFn: async (data: { username: string; email: string; password: string }) => {
      await register(data)
      const tokenData = await login({ email: data.email, password: data.password })
      const user = await client
        .get<UserResponse>('/users/me', {
          headers: { Authorization: `Bearer ${tokenData.accessToken}` },
        })
        .then((r) => r.data)
      return { tokenData, user }
    },
    onSuccess: ({ tokenData, user }) => {
      setAuth(tokenData.accessToken, user)
      navigate('/')
    },
    onError: () => {
      navigate('/login?registered=true')
    },
  })
}

export function useLogout() {
  const navigate = useNavigate()
  const { logout: clearAuth } = useAuthStore()

  return useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearAuth()
      navigate('/login')
    },
  })
}
