import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Input } from '../../../components/ui/Input'
import { ROUTES } from '../../../lib/constants/routes'
import { useAuth } from '../../../hooks/useAuth'
import { validateLogin } from '../validation'

export function LoginForm() {
  const navigate = useNavigate()
  const location = useLocation()
  const { loginWithPassword } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const values = {
      username: username.trim(),
      password,
    }
    const validationError = validateLogin(values)

    if (validationError) {
      setError(validationError)
      return
    }

    setIsSubmitting(true)
    setError(null)

    try {
      await loginWithPassword(values)
      const nextPath =
        (location.state as { from?: { pathname?: string } } | null)?.from
          ?.pathname ?? ROUTES.home
      navigate(nextPath, { replace: true })
    } catch {
      setError('로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <div className="field">
        <label htmlFor="login-username">아이디</label>
        <Input
          id="login-username"
          placeholder="haru"
          value={username}
          onChange={(event) => setUsername(event.target.value)}
        />
      </div>
      <div className="field">
        <label htmlFor="login-password">비밀번호</label>
        <Input
          id="login-password"
          type="password"
          placeholder="••••••••"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
      </div>
      {error ? <p className="meta-line">{error}</p> : null}
      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting ? '로그인 중...' : '로그인'}
      </Button>
    </form>
  )
}
