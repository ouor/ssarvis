import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Input } from '../../../components/ui/Input'
import { ROUTES } from '../../../lib/constants/routes'
import { useAuth } from '../../../hooks/useAuth'
import { validateSignup } from '../validation'

export function SignupForm() {
  const navigate = useNavigate()
  const location = useLocation()
  const { signupWithPassword } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const values = {
      username: username.trim(),
      password,
      displayName: displayName.trim(),
    }
    const validationError = validateSignup(values)

    if (validationError) {
      setError(validationError)
      return
    }

    setIsSubmitting(true)
    setError(null)

    try {
      await signupWithPassword(values)
      const nextPath =
        (location.state as { from?: { pathname?: string } } | null)?.from
          ?.pathname ?? ROUTES.home
      navigate(nextPath, { replace: true })
    } catch {
      setError('회원가입에 실패했습니다. 입력값을 다시 확인해주세요.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <div className="field">
        <label htmlFor="signup-username">아이디</label>
        <Input
          id="signup-username"
          placeholder="haru"
          value={username}
          onChange={(event) => setUsername(event.target.value)}
        />
      </div>
      <div className="field">
        <label htmlFor="signup-password">비밀번호</label>
        <Input
          id="signup-password"
          type="password"
          placeholder="최소 8자"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
      </div>
      <div className="field">
        <label htmlFor="signup-display-name">표시 이름</label>
        <Input
          id="signup-display-name"
          placeholder="하루"
          value={displayName}
          onChange={(event) => setDisplayName(event.target.value)}
        />
      </div>
      {error ? <p className="meta-line">{error}</p> : null}
      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting ? '가입 중...' : '회원가입'}
      </Button>
    </form>
  )
}
