import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { VoiceUploadPanel } from './VoiceUploadPanel'

describe('VoiceUploadPanel', () => {
  it('shows the selected file summary after choosing an audio file', () => {
    render(<VoiceUploadPanel onSubmit={async () => true} />)

    const input = screen.getByLabelText('음성 파일') as HTMLInputElement
    const file = new File(['voice'], 'sample.mp3', { type: 'audio/mpeg' })

    fireEvent.change(input, {
      target: { files: [file] },
    })

    expect(screen.getByText(/sample.mp3/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '파일 제거' })).toBeInTheDocument()
  })

  it('keeps the selected file when submit reports failure', async () => {
    render(<VoiceUploadPanel onSubmit={async () => false} />)

    const input = screen.getByLabelText('음성 파일') as HTMLInputElement
    const file = new File(['voice'], 'failed.mp3', { type: 'audio/mpeg' })

    fireEvent.change(input, {
      target: { files: [file] },
    })

    fireEvent.click(screen.getByRole('button', { name: '업로드' }))

    await waitFor(() => {
      expect(screen.getByText(/failed.mp3/i)).toBeInTheDocument()
    })
  })

  it('clears the selected file after a successful submit', async () => {
    render(<VoiceUploadPanel onSubmit={async () => true} />)

    const input = screen.getByLabelText('음성 파일') as HTMLInputElement
    const file = new File(['voice'], 'success.mp3', { type: 'audio/mpeg' })

    fireEvent.change(input, {
      target: { files: [file] },
    })

    fireEvent.click(screen.getByRole('button', { name: '업로드' }))

    await waitFor(() => {
      expect(screen.queryByText(/success.mp3/i)).not.toBeInTheDocument()
    })
  })
})
