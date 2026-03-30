import { createContext } from 'react'
import type { LoginFormValues, SignupFormValues } from '../../features/auth/types'
import type { AppUser } from '../../lib/types/common'

export type AuthContextValue = {
  currentUser: AppUser | null
  isLoading: boolean
  isAuthenticated: boolean
  isDemo: boolean
  sessionMessage: string | null
  loginWithPassword: (values: LoginFormValues) => Promise<void>
  signupWithPassword: (values: SignupFormValues) => Promise<void>
  continueAsDemo: () => void
  logout: () => void
  deleteAccount: () => Promise<void>
  clearSessionMessage: () => void
  syncCurrentUser: (user: AppUser) => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
