import { Card } from '../../../components/ui/Card'
import { Chip } from '../../../components/ui/Chip'
import type { ChatConversationSummary } from '../types'

type ConversationHistoryListProps = {
  conversations: ChatConversationSummary[]
  selectedConversationId?: number | null
  onSelectConversation: (conversationId: number) => void
}

function formatDateTime(isoDate: string) {
  const date = new Date(isoDate)

  if (Number.isNaN(date.getTime())) {
    return isoDate
  }

  return new Intl.DateTimeFormat('ko-KR', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

export function ConversationHistoryList({
  conversations,
  selectedConversationId,
  onSelectConversation,
}: ConversationHistoryListProps) {
  return (
    <Card className="stack-md">
      <div className="entity-title">
        <h2 className="section-title">대화 히스토리</h2>
        <Chip tone="accent">{conversations.length}</Chip>
      </div>
      <div className="history-list">
        {conversations.length === 0 ? (
          <p className="muted-copy">
            아직 저장된 테스트 대화가 없습니다. 첫 메시지를 보내면 히스토리가 생깁니다.
          </p>
        ) : (
          conversations.map((conversation) => (
            <button
              key={conversation.conversationId}
              type="button"
              className={`history-item${selectedConversationId === conversation.conversationId ? ' active' : ''}`}
              onClick={() => onSelectConversation(conversation.conversationId)}
            >
              <div className="entity-title">
                <strong>{conversation.cloneAlias}</strong>
                <Chip tone="warm">{conversation.messageCount} msgs</Chip>
              </div>
              <p className="muted-copy">{conversation.latestMessagePreview}</p>
              <span className="meta-line">{formatDateTime(conversation.createdAt)}</span>
            </button>
          ))
        )}
      </div>
    </Card>
  )
}
