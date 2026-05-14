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
