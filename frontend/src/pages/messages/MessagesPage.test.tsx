import { render, screen } from '@testing-library/react'
import { MessagesPage } from './MessagesPage'
import { createAuthWrapper } from '../../test/auth-test-utils'

describe('MessagesPage', () => {
  it('renders the conversation shell for demo mode', () => {
    render(
      <div>
        <MessagesPage />
      </div>,
      {
        wrapper: createAuthWrapper(),
      },
    )

    expect(
      screen.getByRole('heading', {
        name: /텍스트와 음성이 자연스럽게 섞이는 대화 공간/i,
      }),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '메시지 보내기' })).toBeInTheDocument()
  })
})
