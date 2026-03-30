import { render, screen } from '@testing-library/react'
import { HomePage } from './HomePage'
import { createAuthWrapper } from '../../test/auth-test-utils'

describe('HomePage', () => {
  it('renders the primary feed heading', () => {
    render(
      <div>
        <HomePage />
      </div>,
      {
        wrapper: createAuthWrapper(),
      },
    )

    expect(
      screen.getByRole('heading', {
        name: /친구들의 소식과 내 이야기를 바로 확인하세요/i,
      }),
    ).toBeInTheDocument()
    expect(screen.getByText('내 스튜디오')).toBeInTheDocument()
  })
})
