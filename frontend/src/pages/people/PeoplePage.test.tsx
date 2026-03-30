import { render, screen } from '@testing-library/react'
import { PeoplePage } from './PeoplePage'
import { createAuthWrapper } from '../../test/auth-test-utils'

describe('PeoplePage', () => {
  it('renders people discovery in demo mode', () => {
    render(
      <div>
        <PeoplePage />
      </div>,
      {
        wrapper: createAuthWrapper(),
      },
    )

    expect(
      screen.getByRole('heading', {
        name: /관계를 넓히되, 프로필 공개 정책은 분명하게/i,
      }),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('사람 검색')).toBeInTheDocument()
  })
})
