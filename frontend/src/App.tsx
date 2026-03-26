import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import CloneStudioPage from './pages/CloneStudioPage'
import AuthPage from './features/auth/AuthPage'
import { apiBaseUrl, apiFetch, authExpiredEventName, clearStoredAccessToken, readErrorMessage, storeAccessToken } from './features/clone-studio/api'
import type { AuthResponse, CurrentUser } from './features/clone-studio/types'

function App() {
  const [authMode, setAuthMode] = useState<'login' | 'signup'>('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [authLoading, setAuthLoading] = useState(true)
  const [authSubmitting, setAuthSubmitting] = useState(false)
  const [authError, setAuthError] = useState('')
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null)

  useEffect(() => {
    async function restoreSession() {
      setAuthLoading(true)
      try {
        const response = await apiFetch(`${apiBaseUrl}/api/auth/me`)
        if (!response.ok) {
          if (response.status !== 401) {
            throw new Error(await readErrorMessage(response, '로그인 상태를 확인하지 못했습니다.'))
          }
          clearStoredAccessToken()
          setCurrentUser(null)
          return
        }

        const me: CurrentUser = await response.json()
        setCurrentUser(me)
        setAuthError('')
      } catch (error) {
        clearStoredAccessToken()
        setCurrentUser(null)
        setAuthError(error instanceof Error ? error.message : '로그인 상태를 확인하지 못했습니다.')
      } finally {
        setAuthLoading(false)
      }
    }

    function handleAuthExpired() {
      setCurrentUser(null)
      setPassword('')
      setAuthMode('login')
      setAuthError('세션이 만료되어 다시 로그인해야 합니다.')
    }

    window.addEventListener(authExpiredEventName, handleAuthExpired)
    void restoreSession()

    return () => {
      window.removeEventListener(authExpiredEventName, handleAuthExpired)
    }
  }, [])

  async function handleAuthSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setAuthSubmitting(true)
    setAuthError('')

    try {
      const endpoint = authMode === 'login' ? '/api/auth/login' : '/api/auth/signup'
      const response = await fetch(`${apiBaseUrl}${endpoint}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(
          authMode === 'login'
            ? { username, password }
            : { username, password, displayName }
        ),
      })

      if (!response.ok) {
        throw new Error(await readErrorMessage(response, authMode === 'login' ? '로그인에 실패했습니다.' : '회원가입에 실패했습니다.'))
      }

      const session: AuthResponse = await response.json()
      storeAccessToken(session.accessToken)
      setCurrentUser({
        userId: session.userId,
        username: session.username,
        displayName: session.displayName,
      })
      setPassword('')
      setAuthError('')
    } catch (error) {
      setAuthError(error instanceof Error ? error.message : '인증 처리 중 오류가 발생했습니다.')
    } finally {
      setAuthSubmitting(false)
      setAuthLoading(false)
    }
  }

  function handleModeChange(mode: 'login' | 'signup') {
    setAuthMode(mode)
    setAuthError('')
  }

  function handleLogout() {
    clearStoredAccessToken()
    setCurrentUser(null)
    setPassword('')
    setAuthMode('login')
    setAuthError('')
  }

  if (authLoading) {
    return (
      <main className="auth-loading-shell">
        <section className="auth-loading-card">
          <p className="auth-kicker">SSARVIS</p>
          <h1>세션을 확인하는 중입니다.</h1>
          <p>저장된 로그인 정보를 바탕으로 작업 공간을 준비하고 있습니다.</p>
        </section>
      </main>
    )
  }

  if (!currentUser) {
    return (
      <AuthPage
        displayName={displayName}
        error={authError}
        loading={authSubmitting}
        mode={authMode}
        onDisplayNameChange={setDisplayName}
        onModeChange={handleModeChange}
        onPasswordChange={setPassword}
        onSubmit={handleAuthSubmit}
        onUsernameChange={setUsername}
        password={password}
        username={username}
      />
    )
  }

  return <CloneStudioPage currentUser={currentUser} onLogout={handleLogout} />
}

export default App
