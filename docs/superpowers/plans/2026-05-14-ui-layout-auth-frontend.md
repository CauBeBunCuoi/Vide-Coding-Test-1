# UI Layout + Auth — Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the full frontend auth flow — AppLayout, AuthLayout, Navbar, Axios client with token-refresh interceptors, Zustand auth store, ProtectedRoute/PublicOnlyRoute guards, and Login/Register forms — in `taskflow-fe-sample/`.

**Architecture:** Access token lives in Zustand memory only. Refresh token is an httpOnly cookie managed by the backend. Axios intercepts 401s, attempts one refresh, drains a queue of concurrent failed requests on success, and redirects to `/login` on failure. Route guards (`ProtectedRoute`, `PublicOnlyRoute`) redirect unauthenticated or already-authenticated users.

**Tech Stack:** React 19, TypeScript 5 (strict), Vite 6, Tailwind CSS 4, React Router 7, TanStack Query 5, Zustand 5, Axios, React Hook Form + Zod, Vitest + React Testing Library

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `src/index.css` | Modify | Add all CSS design tokens |
| `src/types/api.ts` | Modify | `UserResponse`, `TokenResponse` types |
| `src/stores/authStore.ts` | Modify | Zustand: token + user + isAuthenticated |
| `src/hooks/useClickOutside.ts` | Modify | Close dropdown on outside click |
| `src/api/client.ts` | Create | Axios instance + request/response interceptors + refresh queue |
| `src/api/auth.ts` | Modify | `register`, `login`, `refreshToken`, `logout` |
| `src/api/users.ts` | Modify | `getMe` |
| `src/features/auth/useAuth.ts` | Modify | `useLogin`, `useRegister`, `useLogout` mutations |
| `src/features/auth/PasswordStrength.tsx` | Modify | 5-requirement password checklist |
| `src/features/auth/LoginForm.tsx` | Modify | RHF + Zod login form |
| `src/features/auth/RegisterForm.tsx` | Modify | RHF + Zod register form + strength indicator |
| `src/features/auth/index.ts` | Modify | Re-exports |
| `src/layouts/AuthLayout.tsx` | Modify | Centered card wrapper for auth pages |
| `src/layouts/Navbar.tsx` | Modify | Fixed top bar with user avatar dropdown |
| `src/layouts/AppLayout.tsx` | Modify | Navbar + scrollable content area |
| `src/pages/LoginPage.tsx` | Modify | Renders `LoginForm` inside auth layout |
| `src/pages/RegisterPage.tsx` | Modify | Renders `RegisterForm` inside auth layout |
| `src/pages/DashboardPage.tsx` | Modify | Stub — "Projects" heading so routing is testable |
| `src/App.tsx` | Modify | Router + `ProtectedRoute` + `PublicOnlyRoute` + all routes |

---

### Task 1: Design tokens + type definitions (3 min)

**Files:**
- Modify: `src/index.css`
- Modify: `src/types/api.ts`

- [ ] **Step 1: Add all color tokens to `src/index.css`**

Replace existing content with:
```css
@import "tailwindcss";

@theme {
  --font-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  --color-primary: #2563EB;
  --color-primary-hover: #1D4ED8;
  --color-danger: #DC2626;
  --color-danger-hover: #B91C1C;
  --color-success: #16A34A;
  --color-warning: #D97706;
  --color-gray-50: #F9FAFB;
  --color-gray-100: #F3F4F6;
  --color-gray-200: #E5E7EB;
  --color-gray-500: #6B7280;
  --color-gray-700: #374151;
  --color-gray-900: #111827;
}

body {
  font-family: var(--font-sans);
  background-color: var(--color-gray-50);
  color: var(--color-gray-700);
}
```

- [ ] **Step 2: Define `UserResponse` and `TokenResponse` in `src/types/api.ts`**

```typescript
export interface ApiError {
  error: string
  message: string
  details?: {
    fieldErrors?: Array<{ field: string; message: string }>
  }
}

export interface UserResponse {
  id: number
  username: string
  email: string
  createdAt: string
}

export interface TokenResponse {
  accessToken: string
  expiresIn: number
}

export interface User {}
export interface Project {}
export interface ProjectDetail {}
export interface ProjectMember {}
export interface Task {}
export interface Label {}
export interface Comment {}
```

- [ ] **Step 3: Commit**
```bash
git add src/index.css src/types/api.ts
git commit -m "feat(fe): add design tokens and core API types"
```

---

### Task 2: Auth store + useClickOutside hook (3 min)

**Files:**
- Modify: `src/stores/authStore.ts`
- Modify: `src/hooks/useClickOutside.ts`

- [ ] **Step 1: Write failing auth store test**

Create `src/stores/__tests__/authStore.test.ts`:
```typescript
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
```

- [ ] **Step 2: Run test — expect failure**
```bash
npm run test -- src/stores/__tests__/authStore.test.ts
```
Expected: FAIL — `setAuth is not a function`

- [ ] **Step 3: Implement auth store in `src/stores/authStore.ts`**

```typescript
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
```

- [ ] **Step 4: Implement `src/hooks/useClickOutside.ts`**

```typescript
import { useEffect, type RefObject } from 'react'

export function useClickOutside<T extends HTMLElement>(
  ref: RefObject<T | null>,
  handler: () => void,
) {
  useEffect(() => {
    function handleClick(event: MouseEvent) {
      if (ref.current && !ref.current.contains(event.target as Node)) {
        handler()
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [ref, handler])
}
```

- [ ] **Step 5: Run test — expect pass**
```bash
npm run test -- src/stores/__tests__/authStore.test.ts
```
Expected: PASS (3 tests)

- [ ] **Step 6: Commit**
```bash
git add src/stores/authStore.ts src/stores/__tests__/authStore.test.ts src/hooks/useClickOutside.ts
git commit -m "feat(fe): implement auth store and useClickOutside hook"
```

---

### Task 3: Axios client with interceptors (5 min)

**Files:**
- Create: `src/api/client.ts`

- [ ] **Step 1: Create `src/api/client.ts`**

```typescript
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
```

- [ ] **Step 2: Commit**
```bash
git add src/api/client.ts
git commit -m "feat(fe): add Axios client with JWT refresh interceptor"
```

---

### Task 4: Auth + user API functions (3 min)

**Files:**
- Modify: `src/api/auth.ts`
- Modify: `src/api/users.ts`

- [ ] **Step 1: Implement `src/api/auth.ts`**

```typescript
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
```

- [ ] **Step 2: Implement `src/api/users.ts`**

```typescript
import client from './client'
import type { UserResponse } from '@/types/api'

export const getMe = (): Promise<UserResponse> =>
  client.get<UserResponse>('/users/me').then((r) => r.data)
```

- [ ] **Step 3: Commit**
```bash
git add src/api/auth.ts src/api/users.ts
git commit -m "feat(fe): add auth and user API functions"
```

---

### Task 5: Auth mutations hook (4 min)

**Files:**
- Modify: `src/features/auth/useAuth.ts`

- [ ] **Step 1: Implement `src/features/auth/useAuth.ts`**

```typescript
import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { login, logout, register } from '@/api/auth'
import { getMe } from '@/api/users'
import { useAuthStore } from '@/stores/authStore'
import client from '@/api/client'
import type { UserResponse } from '@/types/api'

export function useLogin() {
  const navigate = useNavigate()
  const { setAuth } = useAuthStore()

  return useMutation({
    mutationFn: async (data: { email: string; password: string }) => {
      const tokenData = await login(data)
      // Fetch user with token directly — don't rely on store interceptor yet
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
  const loginMutation = useLogin()

  return useMutation({
    mutationFn: async (data: { username: string; email: string; password: string }) => {
      await register(data)
      return data
    },
    onSuccess: (data) => {
      loginMutation.mutate(
        { email: data.email, password: data.password },
        {
          onError: () => navigate('/login?registered=true'),
        },
      )
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
```

- [ ] **Step 2: Commit**
```bash
git add src/features/auth/useAuth.ts
git commit -m "feat(fe): add useLogin, useRegister, useLogout mutations"
```

---

### Task 6: PasswordStrength component (3 min)

**Files:**
- Modify: `src/features/auth/PasswordStrength.tsx`

- [ ] **Step 1: Implement `src/features/auth/PasswordStrength.tsx`**

```tsx
const REQUIREMENTS = [
  { label: 'At least 8 characters', test: (p: string) => p.length >= 8 },
  { label: 'Uppercase letter', test: (p: string) => /[A-Z]/.test(p) },
  { label: 'Lowercase letter', test: (p: string) => /[a-z]/.test(p) },
  { label: 'Number', test: (p: string) => /\d/.test(p) },
  { label: 'Special character', test: (p: string) => /[!@#$%^&*()\-_=+\[\]{}|;:'",.<>?/~`\\]/.test(p) },
]

export function allPasswordRequirementsMet(password: string): boolean {
  return REQUIREMENTS.every(({ test }) => test(password))
}

interface PasswordStrengthProps {
  password: string
}

export function PasswordStrength({ password }: PasswordStrengthProps) {
  return (
    <ul className="flex flex-col gap-1 mt-1">
      {REQUIREMENTS.map(({ label, test }) => {
        const met = password.length > 0 && test(password)
        return (
          <li
            key={label}
            className={`text-xs flex items-center gap-1.5 ${met ? 'text-green-600' : 'text-gray-400'}`}
          >
            <span className="font-mono">{met ? '✓' : '✗'}</span>
            {label}
          </li>
        )
      })}
    </ul>
  )
}
```

- [ ] **Step 2: Write test**

Create `src/features/auth/__tests__/PasswordStrength.test.tsx`:
```typescript
import { describe, it, expect } from 'vitest'
import { allPasswordRequirementsMet } from '../PasswordStrength'

describe('allPasswordRequirementsMet', () => {
  it('returns false for empty string', () => {
    expect(allPasswordRequirementsMet('')).toBe(false)
  })

  it('returns false when missing special char', () => {
    expect(allPasswordRequirementsMet('Test1234')).toBe(false)
  })

  it('returns true for valid password', () => {
    expect(allPasswordRequirementsMet('Test1234!')).toBe(true)
  })
})
```

- [ ] **Step 3: Run test**
```bash
npm run test -- src/features/auth/__tests__/PasswordStrength.test.tsx
```
Expected: PASS (3 tests)

- [ ] **Step 4: Commit**
```bash
git add src/features/auth/PasswordStrength.tsx src/features/auth/__tests__/PasswordStrength.test.tsx
git commit -m "feat(fe): add PasswordStrength component"
```

---

### Task 7: AuthLayout (2 min)

**Files:**
- Modify: `src/layouts/AuthLayout.tsx`

- [ ] **Step 1: Implement `src/layouts/AuthLayout.tsx`**

```tsx
import { Outlet } from 'react-router-dom'

export default function AuthLayout() {
  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4 py-8">
      <div className="w-full max-w-[400px]">
        <div className="text-center mb-8">
          <span className="text-2xl font-bold text-gray-900">TaskFlow</span>
        </div>
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-8">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Commit**
```bash
git add src/layouts/AuthLayout.tsx
git commit -m "feat(fe): add AuthLayout centered card"
```

---

### Task 8: LoginForm (5 min)

**Files:**
- Modify: `src/features/auth/LoginForm.tsx`

- [ ] **Step 1: Implement `src/features/auth/LoginForm.tsx`**

```tsx
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link } from 'react-router-dom'
import { Eye, EyeOff, Loader2 } from 'lucide-react'
import { useLogin } from './useAuth'
import type { AxiosError } from 'axios'
import type { ApiError } from '@/types/api'

const schema = z.object({
  email: z.string().email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
})

type FormData = z.infer<typeof schema>

export function LoginForm() {
  const [showPassword, setShowPassword] = useState(false)
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) })
  const loginMutation = useLogin()

  const apiError = loginMutation.error as AxiosError<ApiError> | null
  const errorMessage = apiError?.response?.data?.message
    ?? (apiError ? 'Unable to connect. Please check your connection and try again.' : null)

  const onSubmit = (data: FormData) => loginMutation.mutate(data)

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold text-gray-900 text-center">Log in</h1>

      {errorMessage && (
        <div
          role="alert"
          className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded text-sm"
        >
          {errorMessage}
        </div>
      )}

      <div className="flex flex-col gap-1">
        <label className="text-xs font-medium text-gray-700" htmlFor="email">
          Email
        </label>
        <input
          id="email"
          type="email"
          autoComplete="email"
          className="border border-gray-200 rounded px-3 py-2 text-sm bg-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white"
          {...register('email')}
        />
        {errors.email && (
          <span className="text-xs text-red-600">{errors.email.message}</span>
        )}
      </div>

      <div className="flex flex-col gap-1">
        <label className="text-xs font-medium text-gray-700" htmlFor="password">
          Password
        </label>
        <div className="relative">
          <input
            id="password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            className="w-full border border-gray-200 rounded px-3 py-2 pr-10 text-sm bg-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white"
            {...register('password')}
          />
          <button
            type="button"
            onClick={() => setShowPassword((v) => !v)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            aria-label={showPassword ? 'Hide password' : 'Show password'}
          >
            {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
          </button>
        </div>
        {errors.password && (
          <span className="text-xs text-red-600">{errors.password.message}</span>
        )}
      </div>

      <button
        type="submit"
        disabled={loginMutation.isPending}
        className="flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-60 text-white font-medium py-2 px-4 rounded text-sm transition-colors"
      >
        {loginMutation.isPending && <Loader2 size={14} className="animate-spin" />}
        Log in
      </button>

      <p className="text-center text-sm text-gray-500">
        Don't have an account?{' '}
        <Link to="/register" className="text-blue-600 hover:underline">
          Register
        </Link>
      </p>
    </form>
  )
}
```

- [ ] **Step 2: Commit**
```bash
git add src/features/auth/LoginForm.tsx
git commit -m "feat(fe): add LoginForm with RHF + Zod validation"
```

---

### Task 9: RegisterForm (5 min)

**Files:**
- Modify: `src/features/auth/RegisterForm.tsx`

- [ ] **Step 1: Implement `src/features/auth/RegisterForm.tsx`**

```tsx
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link } from 'react-router-dom'
import { Eye, EyeOff, Loader2 } from 'lucide-react'
import { useRegister } from './useAuth'
import { PasswordStrength, allPasswordRequirementsMet } from './PasswordStrength'
import type { AxiosError } from 'axios'
import type { ApiError } from '@/types/api'

const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()\-_=+\[\]{}|;:'",.<>?/~`\\]).{8,72}$/
const USERNAME_REGEX = /^[a-zA-Z][a-zA-Z0-9_]{2,49}$/

const schema = z.object({
  username: z
    .string()
    .regex(USERNAME_REGEX, 'Username must be 3–50 characters, letters/numbers/underscores, starting with a letter'),
  email: z.string().email('Enter a valid email'),
  password: z.string().regex(PASSWORD_REGEX, 'Password does not meet requirements'),
})

type FormData = z.infer<typeof schema>

export function RegisterForm() {
  const [showPassword, setShowPassword] = useState(false)
  const {
    register,
    handleSubmit,
    watch,
    setError,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) })
  const registerMutation = useRegister()
  const passwordValue = watch('password', '')

  const apiError = registerMutation.error as AxiosError<ApiError> | null
  if (apiError?.response?.data?.error === 'DUPLICATE_USERNAME') {
    // setError for field-level display — deduplicated by RHF
  }

  const onSubmit = (data: FormData) => {
    registerMutation.mutate(data, {
      onError: (err) => {
        const axiosErr = err as AxiosError<ApiError>
        const code = axiosErr.response?.data?.error
        if (code === 'DUPLICATE_USERNAME') {
          setError('username', { message: 'Username is already taken' })
        } else if (code === 'DUPLICATE_EMAIL') {
          setError('email', { message: 'Email is already registered' })
        }
      },
    })
  }

  const passwordMet = allPasswordRequirementsMet(passwordValue)

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold text-gray-900 text-center">Create account</h1>

      <div className="flex flex-col gap-1">
        <label className="text-xs font-medium text-gray-700" htmlFor="username">
          Username
        </label>
        <input
          id="username"
          type="text"
          autoComplete="username"
          className="border border-gray-200 rounded px-3 py-2 text-sm bg-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white"
          {...register('username')}
        />
        {errors.username && (
          <span className="text-xs text-red-600">{errors.username.message}</span>
        )}
      </div>

      <div className="flex flex-col gap-1">
        <label className="text-xs font-medium text-gray-700" htmlFor="reg-email">
          Email
        </label>
        <input
          id="reg-email"
          type="email"
          autoComplete="email"
          className="border border-gray-200 rounded px-3 py-2 text-sm bg-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white"
          {...register('email')}
        />
        {errors.email && (
          <span className="text-xs text-red-600">{errors.email.message}</span>
        )}
      </div>

      <div className="flex flex-col gap-1">
        <label className="text-xs font-medium text-gray-700" htmlFor="reg-password">
          Password
        </label>
        <div className="relative">
          <input
            id="reg-password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="new-password"
            className="w-full border border-gray-200 rounded px-3 py-2 pr-10 text-sm bg-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white"
            {...register('password')}
          />
          <button
            type="button"
            onClick={() => setShowPassword((v) => !v)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            aria-label={showPassword ? 'Hide password' : 'Show password'}
          >
            {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
          </button>
        </div>
        <PasswordStrength password={passwordValue} />
      </div>

      <button
        type="submit"
        disabled={registerMutation.isPending || !passwordMet}
        className="flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-60 text-white font-medium py-2 px-4 rounded text-sm transition-colors"
      >
        {registerMutation.isPending && <Loader2 size={14} className="animate-spin" />}
        Create account
      </button>

      <p className="text-center text-sm text-gray-500">
        Already have an account?{' '}
        <Link to="/login" className="text-blue-600 hover:underline">
          Log in
        </Link>
      </p>
    </form>
  )
}
```

- [ ] **Step 2: Commit**
```bash
git add src/features/auth/RegisterForm.tsx
git commit -m "feat(fe): add RegisterForm with password strength indicator"
```

---

### Task 10: Navbar + AppLayout (5 min)

**Files:**
- Modify: `src/layouts/Navbar.tsx`
- Modify: `src/layouts/AppLayout.tsx`

- [ ] **Step 1: Implement `src/layouts/Navbar.tsx`**

```tsx
import { useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import { useLogout } from '@/features/auth/useAuth'
import { useClickOutside } from '@/hooks/useClickOutside'

export default function Navbar() {
  const { user } = useAuthStore()
  const logoutMutation = useLogout()
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useClickOutside(menuRef, () => setMenuOpen(false))

  const initials = user?.username?.charAt(0)?.toUpperCase() ?? '?'

  return (
    <nav className="fixed top-0 left-0 right-0 h-14 bg-white border-b border-gray-200 flex items-center px-6 z-50">
      <Link to="/" className="text-lg font-bold text-gray-900 mr-auto select-none">
        TaskFlow
      </Link>
      <div className="relative" ref={menuRef}>
        <button
          onClick={() => setMenuOpen((v) => !v)}
          className="w-8 h-8 rounded-full bg-blue-600 text-white text-sm font-medium flex items-center justify-center hover:bg-blue-700 transition-colors"
          aria-label="User menu"
          aria-expanded={menuOpen}
        >
          {initials}
        </button>
        {menuOpen && (
          <div className="absolute right-0 top-10 w-44 bg-white border border-gray-200 rounded shadow-lg z-50 py-1">
            {user && (
              <div className="px-4 py-2 text-xs text-gray-500 border-b border-gray-100">
                {user.username}
              </div>
            )}
            <Link
              to="/profile"
              className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
              onClick={() => setMenuOpen(false)}
            >
              Profile
            </Link>
            <button
              onClick={() => { setMenuOpen(false); logoutMutation.mutate() }}
              className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
              disabled={logoutMutation.isPending}
            >
              Logout
            </button>
          </div>
        )}
      </div>
    </nav>
  )
}
```

- [ ] **Step 2: Implement `src/layouts/AppLayout.tsx`**

```tsx
import { Outlet } from 'react-router-dom'
import Navbar from './Navbar'

export default function AppLayout() {
  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <main className="pt-14">
        <div className="max-w-[1280px] mx-auto px-6 py-6">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
```

- [ ] **Step 3: Commit**
```bash
git add src/layouts/Navbar.tsx src/layouts/AppLayout.tsx
git commit -m "feat(fe): add Navbar with user menu and AppLayout"
```

---

### Task 11: Page wrappers + auth index exports (3 min)

**Files:**
- Modify: `src/pages/LoginPage.tsx`
- Modify: `src/pages/RegisterPage.tsx`
- Modify: `src/pages/DashboardPage.tsx`
- Modify: `src/features/auth/index.ts`

- [ ] **Step 1: Implement page wrappers**

`src/pages/LoginPage.tsx`:
```tsx
import { LoginForm } from '@/features/auth'

export default function LoginPage() {
  return <LoginForm />
}
```

`src/pages/RegisterPage.tsx`:
```tsx
import { RegisterForm } from '@/features/auth'

export default function RegisterPage() {
  return <RegisterForm />
}
```

`src/pages/DashboardPage.tsx`:
```tsx
export default function DashboardPage() {
  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900">Projects</h1>
      <p className="text-gray-500 mt-2">Your projects will appear here.</p>
    </div>
  )
}
```

- [ ] **Step 2: Update `src/features/auth/index.ts`**

```typescript
export { LoginForm } from './LoginForm'
export { RegisterForm } from './RegisterForm'
export { PasswordStrength, allPasswordRequirementsMet } from './PasswordStrength'
export { useLogin, useRegister, useLogout } from './useAuth'
```

- [ ] **Step 3: Commit**
```bash
git add src/pages/LoginPage.tsx src/pages/RegisterPage.tsx src/pages/DashboardPage.tsx src/features/auth/index.ts
git commit -m "feat(fe): add page wrappers and auth index exports"
```

---

### Task 12: App.tsx routing with guards (4 min)

**Files:**
- Modify: `src/App.tsx`

- [ ] **Step 1: Implement `src/App.tsx`**

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import AppLayout from '@/layouts/AppLayout'
import AuthLayout from '@/layouts/AuthLayout'
import LoginPage from '@/pages/LoginPage'
import RegisterPage from '@/pages/RegisterPage'
import DashboardPage from '@/pages/DashboardPage'
import NotFoundPage from '@/pages/NotFoundPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error: unknown) => {
        const status = (error as { response?: { status?: number } })?.response?.status
        if (status === 401 || status === 403 || status === 404) return false
        return failureCount < 2
      },
    },
  },
})

function ProtectedRoute() {
  const { isAuthenticated } = useAuthStore()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <AppLayout />
}

function PublicOnlyRoute() {
  const { isAuthenticated } = useAuthStore()
  if (isAuthenticated) return <Navigate to="/" replace />
  return <AuthLayout />
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route element={<PublicOnlyRoute />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
          </Route>
          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<DashboardPage />} />
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
```

- [ ] **Step 2: Write routing guard test**

Create `src/__tests__/App.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import App from '../App'
import { useAuthStore } from '@/stores/authStore'

// Mock react-router navigate
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return { ...actual }
})

describe('routing guards', () => {
  it('redirects unauthenticated user from / to /login', () => {
    useAuthStore.getState().logout()
    // MemoryRouter sets initial path
    render(<App />)
    expect(screen.queryByText('Log in')).toBeTruthy()
  })
})
```

- [ ] **Step 3: Run test**
```bash
npm run test -- src/__tests__/App.test.tsx
```
Expected: PASS

- [ ] **Step 4: Commit**
```bash
git add src/App.tsx src/__tests__/App.test.tsx
git commit -m "feat(fe): add routing with ProtectedRoute and PublicOnlyRoute"
```

---

### Task 13: Verify end-to-end (5 min)

> Prerequisite: Backend is running on `localhost:8087` (see backend plan).

- [ ] **Step 1: Start the dev server**
```bash
npm run dev
```
Expected output:
```
  VITE v6.x  ready in xxx ms
  ➜  Local:   http://localhost:5177/
```

- [ ] **Step 2: Test unauthenticated redirect**

Open `http://localhost:5177/` in the browser.  
Expected: Redirected to `http://localhost:5177/login` and the login form is visible.

- [ ] **Step 3: Test login**

Enter `alex@example.com` / `Test1234!` and click "Log in".  
Expected: Redirected to `http://localhost:5177/` and see "Projects" heading. User avatar shows "A" in the navbar.

- [ ] **Step 4: Test logout**

Click the "A" avatar → "Logout".  
Expected: Redirected to `/login` and the session is cleared.

- [ ] **Step 5: Test register**

Go to `/register`, fill in a new username/email/password meeting all requirements.  
Expected: "Create account" button becomes enabled once all password requirements are met. On submit, user is logged in and redirected to `/`.

- [ ] **Step 6: Test 409 duplicate error**

Try registering with `alex@example.com`.  
Expected: Inline error under the email field: "Email is already registered".

- [ ] **Step 7: Test invalid login**

Logout, then try logging in with wrong password.  
Expected: Red alert box with "Invalid email or password".

- [ ] **Step 8: Test public-only redirect**

While logged in, navigate to `/login`.  
Expected: Immediately redirected to `/`.

- [ ] **Step 9: Final commit**
```bash
git add -A
git commit -m "feat(fe): complete initial UI layout and auth feature"
```
