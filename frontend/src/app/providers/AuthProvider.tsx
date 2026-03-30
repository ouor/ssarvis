import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { deleteMe, getMe, login, signup } from '../../features/auth/api'
import { ApiError } from '../../lib/api/client'
import type {
  LoginFormValues,
  SignupFormValues,
} from '../../features/auth/types'
import { getDemoUser } from '../../lib/demo/adapters'
import { STORAGE_KEYS } from '../../lib/constants/storage'
import type { AppUser } from '../../lib/types/common'
import {
  readStorage,
  removeStorage,
  writeStorage,
} from '../../lib/utils/storage'
import { AuthContext, type AuthContextValue } from './auth-context'

type AuthProviderProps = {
  children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [currentUser, setCurrentUser] = useState<AppUser | null>(null)
  const [isLoading, setIsLoading] = useState(() =>
    Boolean(readStorage(STORAGE_KEYS.accessToken)),
  )
  const [isDemo, setIsDemo] = useState(false)
  const [sessionMessage, setSessionMessage] = useState<string | null>(null)

  useEffect(() => {
    const token = readStorage(STORAGE_KEYS.accessToken)

    if (!token) {
      return
    }

    getMe(token)
      .then((user) => {
        setCurrentUser(user)
      })
      .catch((error) => {
        removeStorage(STORAGE_KEYS.accessToken)
        setSessionMessage(
          error instanceof ApiError && error.status !== 401
            ? '세션을 확인하지 못했습니다. 다시 로그인해주세요.'
            : '세션이 만료되어 다시 로그인해야 합니다.',
        )
      })
      .finally(() => {
        setIsLoading(false)
      })
  }, [])

  const loginWithPassword = useCallback(async (values: LoginFormValues) => {
    const response = await login(values)
    writeStorage(STORAGE_KEYS.accessToken, response.accessToken)
    setIsDemo(false)
    setSessionMessage(null)
    setCurrentUser(response)
  }, [])

  const signupWithPassword = useCallback(async (values: SignupFormValues) => {
    const response = await signup(values)
    writeStorage(STORAGE_KEYS.accessToken, response.accessToken)
    setIsDemo(false)
    setSessionMessage(null)
    setCurrentUser(response)
  }, [])

  const continueAsDemo = useCallback(() => {
    setCurrentUser(getDemoUser())
    setIsDemo(true)
    setSessionMessage(null)
  }, [])

  const logout = useCallback(() => {
    removeStorage(STORAGE_KEYS.accessToken)
    setCurrentUser(null)
    setIsDemo(false)
  }, [])

  const deleteAccount = useCallback(async () => {
    const token = readStorage(STORAGE_KEYS.accessToken)

    if (token) {
      await deleteMe(token)
    }

    removeStorage(STORAGE_KEYS.accessToken)
    setCurrentUser(null)
    setIsDemo(false)
    setSessionMessage('계정이 비활성화되었습니다.')
  }, [])

  const clearSessionMessage = useCallback(() => {
    setSessionMessage(null)
  }, [])

  const syncCurrentUser = useCallback((user: AppUser) => {
    setCurrentUser(user)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      currentUser,
      isLoading,
      isAuthenticated: Boolean(currentUser),
      isDemo,
      sessionMessage,
      loginWithPassword,
      signupWithPassword,
      continueAsDemo,
      logout,
      deleteAccount,
      clearSessionMessage,
      syncCurrentUser,
    }),
    [
      clearSessionMessage,
      continueAsDemo,
      currentUser,
      deleteAccount,
      isDemo,
      isLoading,
      loginWithPassword,
      logout,
      sessionMessage,
      signupWithPassword,
      syncCurrentUser,
    ],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
