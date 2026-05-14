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
