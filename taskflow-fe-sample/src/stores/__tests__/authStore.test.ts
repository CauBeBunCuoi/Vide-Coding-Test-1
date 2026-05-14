import { describe, it, expect, beforeEach } from 'vitest'
import { useAuthStore } from '../authStore'

describe('authStore', () => {
  beforeEach(() => {
    useAuthStore.getState().logout()
  })

  it('starts unauthenticated', () => {
    const { isAuthenticated, accessToken, user } = useAuthStore.getState()
    expect(isAuthenticated).toBe(false)
    expect(accessToken).toBeNull()
    expect(user).toBeNull()
  })

  it('setAuth sets token + user + isAuthenticated', () => {
    const fakeUser = { id: 1, username: 'alex_lead', email: 'alex@example.com', createdAt: '2026-01-01T00:00:00Z' }
    useAuthStore.getState().setAuth('token123', fakeUser)
    const { isAuthenticated, accessToken, user } = useAuthStore.getState()
    expect(isAuthenticated).toBe(true)
    expect(accessToken).toBe('token123')
    expect(user).toEqual(fakeUser)
  })

  it('logout clears all state', () => {
    const fakeUser = { id: 1, username: 'alex_lead', email: 'alex@example.com', createdAt: '2026-01-01T00:00:00Z' }
    useAuthStore.getState().setAuth('token123', fakeUser)
    useAuthStore.getState().logout()
    const { isAuthenticated, accessToken, user } = useAuthStore.getState()
    expect(isAuthenticated).toBe(false)
    expect(accessToken).toBeNull()
    expect(user).toBeNull()
  })
})
