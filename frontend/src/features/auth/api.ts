import { apiRequest } from '../../lib/api/client'
import type { AppUser } from '../../lib/types/common'
import type {
  AuthResponse,
  LoginFormValues,
  SignupFormValues,
} from './types'

export function login(values: LoginFormValues) {
  return apiRequest<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(values),
  })
}

export function signup(values: SignupFormValues) {
  return apiRequest<AuthResponse>('/api/auth/signup', {
    method: 'POST',
    body: JSON.stringify(values),
  })
}

export function getMe(token: string) {
  return apiRequest<AppUser>('/api/auth/me', {
    token,
  })
}

export function deleteMe(token: string) {
  return apiRequest<void>('/api/auth/me', {
    method: 'DELETE',
    token,
  })
}
