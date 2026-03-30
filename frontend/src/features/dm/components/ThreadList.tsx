import { Avatar } from '../../../components/ui/Avatar'
import { Card } from '../../../components/ui/Card'
import { Chip } from '../../../components/ui/Chip'
import { cn } from '../../../lib/utils/cn'
import type { DmThreadViewModel } from '../types'

type ThreadListProps = {
  threads: DmThreadViewModel[]
  selectedThreadId?: number | null
  onSelectThread?: (threadId: number) => void
}

export function ThreadList({
  threads,
  selectedThreadId,
  onSelectThread,
}: ThreadListProps) {
  return (
    <div className="stack-md">
      {threads.map((thread) => (
        <Card
          key={thread.id}
          className={cn(
            'thread-card',
            selectedThreadId === thread.id && 'thread-card-selected',
          )}
        >
          <button
            type="button"
            className="thread-card-button"
            onClick={() => onSelectThread?.(thread.id)}
          >
            <div className="thread-row">
              <Avatar name={thread.user.displayName} />
              <div className="thread-meta stack-sm">
                <div className="thread-title">
                  <strong>{thread.user.displayName}</strong>
                  <span className="meta-line">@{thread.user.username}</span>
                  {selectedThreadId === thread.id ? (
                    <Chip tone="accent">Open</Chip>
                  ) : null}
                </div>
                <p className="muted-copy">{thread.preview}</p>
              </div>
              <span className="meta-line">{thread.updatedAt}</span>
            </div>
          </button>
        </Card>
      ))}
    </div>
  )
}
