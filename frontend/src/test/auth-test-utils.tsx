import type { ReactNode } from 'react'
import { MemoryRouter } from 'react-router-dom'
import {
  AuthContext,
  type AuthContextValue,
} from '../app/providers/auth-context'

const defaultAuthValue: AuthContextValue = {
  currentUser: null,
  isLoading: false,
  isAuthenticated: true,
  isDemo: true,
  sessionMessage: null,
  loginWithPassword: async () => {},
  signupWithPassword: async () => {},
  continueAsDemo: () => {},
  logout: () => {},
  deleteAccount: async () => {},
  clearSessionMessage: () => {},
  syncCurrentUser: () => {},
}

type WrapperProps = {
  children: ReactNode
}

export function createAuthWrapper(
  overrides: Partial<AuthContextValue> = {},
) {
  const value = {
    ...defaultAuthValue,
    ...overrides,
  }

  return function AuthWrapper({ children }: WrapperProps) {
    return (
      <AuthContext.Provider value={value}>
        <MemoryRouter>{children}</MemoryRouter>
      </AuthContext.Provider>
    )
  }
}
