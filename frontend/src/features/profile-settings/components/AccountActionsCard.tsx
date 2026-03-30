import { useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { useAuth } from '../../../hooks/useAuth'
import { ROUTES } from '../../../lib/constants/routes'

export function AccountActionsCard() {
  const navigate = useNavigate()
  const { deleteAccount, isDemo, logout } = useAuth()
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleDeleteAccount() {
    const confirmed = window.confirm(
      '계정을 비활성화하면 보호된 영역에서 로그아웃됩니다. 계속할까요?',
    )

    if (!confirmed) {
      return
    }

    setIsSubmitting(true)
    setError(null)

    try {
      await deleteAccount()
      navigate(ROUTES.auth, { replace: true })
    } catch {
      setError('계정 비활성화에 실패했습니다. 잠시 후 다시 시도해주세요.')
    } finally {
      setIsSubmitting(false)
    }
  }

  function handleLogout() {
    logout()
    navigate(ROUTES.auth, { replace: true })
  }

  return (
    <Card className="stack-md">
      <h2 className="section-title">계정 액션</h2>
      <p className="muted-copy">
        세션을 종료하거나, 필요 시 현재 계정을 비활성화합니다.
      </p>
      {error ? <p className="meta-line">{error}</p> : null}
      <div className="entity-title">
        <Button variant="secondary" onClick={handleLogout}>
          로그아웃
        </Button>
        <Button
          variant="danger"
          onClick={handleDeleteAccount}
          disabled={isSubmitting || isDemo}
        >
          {isDemo
            ? '데모 계정'
            : isSubmitting
              ? '처리 중...'
              : '계정 비활성화'}
        </Button>
      </div>
    </Card>
  )
}
