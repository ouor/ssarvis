import { render, screen } from '@testing-library/react'
import { StudioPage } from './StudioPage'
import { createAuthWrapper } from '../../test/auth-test-utils'

describe('StudioPage', () => {
  it('renders the studio workspace in demo mode', () => {
    render(
      <div>
        <StudioPage />
      </div>,
      {
        wrapper: createAuthWrapper(),
      },
    )

    expect(
      screen.getByRole('heading', {
        name: /사람의 말투와 음성을 다듬는 페르소나 작업실/i,
      }),
    ).toBeInTheDocument()
    expect(screen.getByText(/차분한 조력자/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '프롬프트 저장' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '테스트 보내기' })).toBeInTheDocument()
  })
})
