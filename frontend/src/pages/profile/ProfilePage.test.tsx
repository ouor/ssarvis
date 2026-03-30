import { render, screen } from '@testing-library/react'
import { ProfilePage } from './ProfilePage'
import { createAuthWrapper } from '../../test/auth-test-utils'

describe('ProfilePage', () => {
  it('renders profile controls in demo mode', () => {
    render(
      <div>
        <ProfilePage />
      </div>,
      {
        wrapper: createAuthWrapper(),
      },
    )

    expect(
      screen.getByRole('heading', {
        name: '내 프로필',
      }),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '표시 이름 저장' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '로그아웃' })).toBeInTheDocument()
  })
})
