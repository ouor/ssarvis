import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { TestChatPanel } from './TestChatPanel'

describe('TestChatPanel', () => {
  it('keeps the draft when submit reports failure', async () => {
    render(<TestChatPanel messages={[]} onSubmit={async () => false} />)

    const input = screen.getByPlaceholderText(/클론에게 짧은 테스트 메시지/i)

    fireEvent.change(input, {
      target: { value: '테스트 메시지' },
    })

    fireEvent.click(screen.getByRole('button', { name: '테스트 보내기' }))

    await waitFor(() => {
      expect(screen.getByDisplayValue('테스트 메시지')).toBeInTheDocument()
    })
  })

  it('clears the draft after a successful submit', async () => {
    render(<TestChatPanel messages={[]} onSubmit={async () => true} />)

    const input = screen.getByPlaceholderText(/클론에게 짧은 테스트 메시지/i)

    fireEvent.change(input, {
      target: { value: '보낼 메시지' },
    })

    fireEvent.click(screen.getByRole('button', { name: '테스트 보내기' }))

    await waitFor(() => {
      expect(screen.queryByDisplayValue('보낼 메시지')).not.toBeInTheDocument()
    })
  })
})
