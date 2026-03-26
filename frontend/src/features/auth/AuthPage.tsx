import type { FormEvent } from 'react'

type AuthPageProps = {
  mode: 'login' | 'signup'
  username: string
  password: string
  displayName: string
  loading: boolean
  error: string
  onModeChange: (mode: 'login' | 'signup') => void
  onUsernameChange: (value: string) => void
  onPasswordChange: (value: string) => void
  onDisplayNameChange: (value: string) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
}

function AuthPage({
  mode,
  username,
  password,
  displayName,
  loading,
  error,
  onModeChange,
  onUsernameChange,
  onPasswordChange,
  onDisplayNameChange,
  onSubmit,
}: AuthPageProps) {
  const isLogin = mode === 'login'

  return (
    <main className="auth-shell">
      <section className="auth-panel auth-panel-copy">
        <p className="auth-kicker">SSARVIS Access</p>
        <h1>내 클론과 목소리를 안전하게 관리하는 작업 공간</h1>
        <p className="auth-copy">
          로그인하면 내 클론, 내 음성, 내 대화와 논쟁만 조회하고 이어서 사용할 수 있습니다.
        </p>
        <div className="auth-copy-grid">
          <div>
            <strong>Private Clones</strong>
            <span>내가 만든 클론만 조회하고 사용할 수 있습니다.</span>
          </div>
          <div>
            <strong>Owned Voices</strong>
            <span>등록한 목소리는 내 계정에서만 재생과 합성에 사용됩니다.</span>
          </div>
          <div>
            <strong>Session Guard</strong>
            <span>세션이 만료되면 자동으로 로그아웃되어 다시 인증을 요구합니다.</span>
          </div>
        </div>
      </section>

      <section className="auth-panel auth-panel-form">
        <div className="auth-tab-row">
          <button
            className={`auth-tab${isLogin ? ' auth-tab-active' : ''}`}
            onClick={() => onModeChange('login')}
            type="button"
          >
            로그인
          </button>
          <button
            className={`auth-tab${!isLogin ? ' auth-tab-active' : ''}`}
            onClick={() => onModeChange('signup')}
            type="button"
          >
            회원가입
          </button>
        </div>

        <form className="auth-form" onSubmit={onSubmit}>
          <div className="auth-form-copy">
            <h2>{isLogin ? '다시 돌아오신 걸 환영해요.' : '새 계정을 만들어 시작해보세요.'}</h2>
            <p>{isLogin ? '아이디와 비밀번호로 바로 작업 공간에 들어갈 수 있습니다.' : '표시명은 앱 안에서 보이는 이름입니다.'}</p>
          </div>

          <label className="auth-field">
            <span>아이디</span>
            <input
              autoComplete="username"
              onChange={(event) => onUsernameChange(event.target.value)}
              placeholder="아이디를 입력하세요"
              value={username}
            />
          </label>

          {!isLogin ? (
            <label className="auth-field">
              <span>표시명</span>
              <input
                autoComplete="nickname"
                onChange={(event) => onDisplayNameChange(event.target.value)}
                placeholder="앱에서 보일 이름"
                value={displayName}
              />
            </label>
          ) : null}

          <label className="auth-field">
            <span>비밀번호</span>
            <input
              autoComplete={isLogin ? 'current-password' : 'new-password'}
              onChange={(event) => onPasswordChange(event.target.value)}
              placeholder="비밀번호를 입력하세요"
              type="password"
              value={password}
            />
          </label>

          {error ? <p className="auth-error">{error}</p> : null}

          <button className="auth-submit" disabled={loading} type="submit">
            {loading ? (isLogin ? '로그인 중...' : '가입 중...') : isLogin ? '로그인' : '회원가입'}
          </button>
        </form>
      </section>
    </main>
  )
}

export default AuthPage
