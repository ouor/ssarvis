import { Navigate, useNavigate } from 'react-router-dom'
import { LoginForm } from '../../features/auth/components/LoginForm'
import { SignupForm } from '../../features/auth/components/SignupForm'
import { Button } from '../../components/ui/Button'
import { useAuth } from '../../hooks/useAuth'
import { ROUTES } from '../../lib/constants/routes'

export function AuthPage() {
  const navigate = useNavigate()
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
        <h1 className="page-title">
          사람의 관계와 AI 페르소나가 함께 존재하는 소셜 캔버스
        </h1>
        <p className="page-subtitle">
          SSARVIS는 피드, DM, 프로필, 그리고 보이스와 클론을 다루는 스튜디오를
          하나의 흐름으로 묶습니다.
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
            <h2 className="section-title">로그인</h2>
            <p className="muted-copy">
              기존 계정으로 바로 들어가 Home과 Inbox를 중심으로 탐색합니다.
            </p>
          </div>
          <LoginForm />
        </div>
        <div className="stack-md">
          <div>
            <h2 className="section-title">회원가입</h2>
            <p className="muted-copy">
              표시 이름과 아이디를 정하고 곧바로 개인 스튜디오를 시작할 수
              있습니다.
            </p>
          </div>
          <SignupForm />
        </div>
        <div className="entity-title">
          <Button
            variant="ghost"
            onClick={() => {
              continueAsDemo()
              navigate(ROUTES.home)
            }}
          >
            데모로 둘러보기
          </Button>
          <span className="meta-line">
            로그인 후에는 원래 진입하려던 페이지로 자동 이동합니다.
          </span>
        </div>
      </section>
    </div>
  )
}
