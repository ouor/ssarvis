import { fireEvent, render, screen } from '@testing-library/react'
import { AuthPage } from './AuthPage'
import { createAuthWrapper } from '../../test/auth-test-utils'

describe('AuthPage', () => {
  it('shows the login form by default and swaps to signup on toggle', () => {
    render(<AuthPage />, {
      wrapper: createAuthWrapper({
        isAuthenticated: false,
      }),
    })

    expect(screen.getByRole('heading', { name: '로그인' })).toBeInTheDocument()
    expect(screen.getByLabelText('아이디')).toBeInTheDocument()
    expect(screen.queryByLabelText('표시 이름')).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '회원가입' }))

    expect(screen.getByRole('heading', { name: '회원가입' })).toBeInTheDocument()
    expect(screen.getByLabelText('표시 이름')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '로그인' })).toBeInTheDocument()
  })
})
