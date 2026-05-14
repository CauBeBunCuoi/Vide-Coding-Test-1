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
