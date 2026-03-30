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
        name: /사람의 흐름 위에 ai 스튜디오가 겹쳐지는 피드/i,
      }),
    ).toBeInTheDocument()
    expect(screen.getByText('Studio Snapshot')).toBeInTheDocument()
  })
})
