import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { Chip } from '../../../components/ui/Chip'
import type { TestChatMessage } from '../types'

type TestChatPanelProps = {
  title?: string
  messages: TestChatMessage[]
  isSubmitting?: boolean
  error?: string | null
  disabled?: boolean
  hasVoiceReply?: boolean
  onSubmit: (message: string) => Promise<boolean> | boolean
}

export function TestChatPanel({
  title = '테스트 대화',
  messages,
  isSubmitting = false,
  error,
  disabled = false,
  hasVoiceReply = false,
  onSubmit,
}: TestChatPanelProps) {
  const [value, setValue] = useState('')

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const nextValue = value.trim()

    if (!nextValue || disabled) {
      return
    }

    const wasSuccessful = await onSubmit(nextValue)

    if (wasSuccessful) {
      setValue('')
    }
  }

  return (
    <Card className="stack-md">
      <div className="entity-title">
        <h2 className="section-title">{title}</h2>
        {hasVoiceReply ? <Chip tone="accent">Voice Reply</Chip> : null}
      </div>
      <div className="test-chat-log">
        {messages.length === 0 ? (
          <p className="muted-copy">
            아직 테스트 대화가 없습니다. 첫 질문을 보내 클론의 현재 톤을 확인해보세요.
          </p>
        ) : (
          messages.map((message) => (
            <div
              key={message.id}
              className={`test-chat-message${message.role === 'assistant' ? ' assistant' : ''}`}
            >
              <div className="entity-title">
                <strong>{message.role === 'assistant' ? 'Clone' : 'You'}</strong>
                {message.audioState === 'voice' ? (
                  <Chip tone="warm">Voice</Chip>
                ) : null}
                <span className="meta-line">{message.createdAt}</span>
              </div>
              <p className="muted-copy">{message.content}</p>
            </div>
          ))
        )}
      </div>
      <form className="stack-md" onSubmit={handleSubmit}>
        <Input
          placeholder="클론에게 짧은 테스트 메시지를 보내보세요."
          value={value}
          onChange={(event) => setValue(event.target.value)}
          disabled={disabled}
        />
        {error ? <p className="meta-line">{error}</p> : null}
        <div className="entity-title">
          <Button
            type="submit"
            disabled={disabled || isSubmitting || value.trim().length === 0}
          >
            {isSubmitting ? '응답 생성 중...' : '테스트 보내기'}
          </Button>
        </div>
      </form>
    </Card>
  )
}
