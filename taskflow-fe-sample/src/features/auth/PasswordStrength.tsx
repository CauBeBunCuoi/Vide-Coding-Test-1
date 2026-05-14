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
