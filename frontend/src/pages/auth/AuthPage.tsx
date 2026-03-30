import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { LoginForm } from '../../features/auth/components/LoginForm'
import { SignupForm } from '../../features/auth/components/SignupForm'
import { Button } from '../../components/ui/Button'
import { useAuth } from '../../hooks/useAuth'
import { ROUTES } from '../../lib/constants/routes'

type AuthMode = 'login' | 'signup'

export function AuthPage() {
  const navigate = useNavigate()
  const [mode, setMode] = useState<AuthMode>('login')
  const {
    continueAsDemo,
    isAuthenticated,
    sessionMessage,
    clearSessionMessage,
  } = useAuth()

  if (isAuthenticated) {
    return <Navigate to={ROUTES.home} replace />
  }

  return (
    <div className="auth-shell">
      <section className="glass-panel auth-hero">
        <span className="page-eyebrow">Welcome</span>
        <h1 className="page-title">대화와 일상을 한곳에서 이어가세요</h1>
        <p className="page-subtitle">
          로그인하면 최근 소식을 확인하고, 메시지를 주고받고, 내 목소리와
          말투를 담은 스튜디오도 바로 이어서 사용할 수 있습니다.
        </p>
      </section>
      <section className="glass-panel auth-panel">
        {sessionMessage ? (
          <div className="glass-card ui-card stack-sm">
            <h2 className="section-title">세션 안내</h2>
            <p className="muted-copy">{sessionMessage}</p>
            <div>
              <Button variant="secondary" onClick={clearSessionMessage}>
                확인
              </Button>
            </div>
          </div>
        ) : null}
        <div className="stack-md">
          <div>
            <h2 className="section-title">
              {mode === 'login' ? '로그인' : '회원가입'}
            </h2>
            <p className="muted-copy">
              {mode === 'login'
                ? '계정이 있다면 바로 로그인해 피드와 메시지를 확인해보세요.'
                : '간단한 정보만 입력하고 계정을 만든 뒤 바로 시작할 수 있습니다.'}
            </p>
          </div>
          {mode === 'login' ? <LoginForm /> : <SignupForm />}
          <Button
            type="button"
            variant="secondary"
            className="auth-switch-button"
            onClick={() =>
              setMode((current) => (current === 'login' ? 'signup' : 'login'))
            }
          >
            {mode === 'login'
              ? '회원가입'
              : '로그인'}
          </Button>
        </div>
        <div className="centered-stack">
          <Button
            variant="ghost"
            onClick={() => {
              continueAsDemo()
              navigate(ROUTES.home)
            }}
          >
            둘러보기
          </Button>
        </div>
      </section>
    </div>
  )
}
