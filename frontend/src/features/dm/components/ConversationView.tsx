import type { DmMessageViewModel } from '../types'
import { VoiceMessageCard } from './VoiceMessageCard'
import { Chip } from '../../../components/ui/Chip'

type ConversationViewProps = {
  messages: DmMessageViewModel[]
  myUserId: number
}

export function ConversationView({
  messages,
  myUserId,
}: ConversationViewProps) {
  return (
    <div className="conversation">
      {messages.map((message) =>
        message.kind === 'voice' ? (
          <VoiceMessageCard
            key={message.id}
            duration={message.duration ?? '0:00'}
            mine={message.authorId === myUserId}
            label={message.isAiGenerated ? 'AI Voice Reply' : 'Voice Message'}
          />
        ) : (
          <div
            key={message.id}
            className={`message-bubble${message.authorId === myUserId ? ' mine' : ''}`}
          >
            <div className="stack-sm">
              <div className="entity-title">
                <strong>{message.authorName}</strong>
                {message.isAiGenerated ? <Chip tone="warm">AI</Chip> : null}
                <span className="meta-line">{message.createdAt}</span>
              </div>
              <span>{message.content}</span>
            </div>
          </div>
        ),
      )}
    </div>
  )
}
