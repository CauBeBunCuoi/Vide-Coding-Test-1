import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import App from '../App'
import { useAuthStore } from '@/stores/authStore'

describe('routing guards', () => {
  it('redirects unauthenticated user from / to /login', () => {
    useAuthStore.getState().logout()
    render(<App />)
    expect(screen.queryAllByText('Log in').length).toBeGreaterThan(0)
  })
})
