import { useRef, useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Input } from '../../../components/ui/Input'
import { Textarea } from '../../../components/ui/Textarea'

type MessageComposerProps = {
  isSubmitting?: boolean
  error?: string | null
  voiceError?: string | null
  isUploadingVoice?: boolean
  disabled?: boolean
  onSubmit: (content: string) => Promise<boolean> | boolean
  onVoiceSubmit: (audio: File) => Promise<boolean> | boolean
}

export function MessageComposer({
  isSubmitting = false,
  error,
  voiceError,
  isUploadingVoice = false,
  disabled = false,
  onSubmit,
  onVoiceSubmit,
}: MessageComposerProps) {
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const [content, setContent] = useState('')
  const [audioFile, setAudioFile] = useState<File | null>(null)

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const nextContent = content.trim()

    if (!nextContent || disabled) {
      return
    }

    const wasSuccessful = await onSubmit(nextContent)

    if (wasSuccessful) {
      setContent('')
    }
  }

  async function handleVoiceUpload() {
    if (!audioFile || disabled) {
      return
    }

    const wasSuccessful = await onVoiceSubmit(audioFile)

    if (wasSuccessful) {
      setAudioFile(null)
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    }
  }

  return (
    <div className="glass-card ui-card">
      <form className="stack-md" onSubmit={handleSubmit}>
        <Textarea
          placeholder="메시지를 입력하거나, 보이스 메시지를 녹음할 준비를 하세요."
          value={content}
          onChange={(event) => setContent(event.target.value)}
          disabled={disabled}
        />
        {error ? <p className="meta-line">{error}</p> : null}
        <div className="field">
          <label htmlFor="dm-voice-upload">음성 메시지 파일</label>
          <Input
            id="dm-voice-upload"
            ref={fileInputRef}
            type="file"
            accept="audio/*"
            disabled={disabled || isUploadingVoice}
            onChange={(event) => setAudioFile(event.target.files?.[0] ?? null)}
          />
        </div>
        {audioFile ? (
          <div className="upload-summary">
            <strong>선택된 음성 메시지</strong>
            <p className="muted-copy">{audioFile.name}</p>
          </div>
        ) : null}
        {voiceError ? <p className="meta-line">{voiceError}</p> : null}
        <div className="entity-title">
          <Button
            type="submit"
            disabled={disabled || isSubmitting || content.trim().length === 0}
          >
            {isSubmitting ? '전송 중...' : '메시지 보내기'}
          </Button>
          <Button
            type="button"
            variant="secondary"
            disabled={disabled || isUploadingVoice || !audioFile}
            onClick={() => void handleVoiceUpload()}
          >
            {isUploadingVoice ? '보이스 전송 중...' : '보이스 업로드'}
          </Button>
        </div>
      </form>
    </div>
  )
}
