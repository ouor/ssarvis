import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Chip } from '../../../components/ui/Chip'

type CloneCardProps = {
  alias: string
  summary: string
  isPublic: boolean
  createdAt?: string
  isSubmitting?: boolean
  error?: string | null
  onToggleVisibility: (isPublic: boolean) => Promise<void> | void
}

export function CloneCard({
  alias,
  summary,
  isPublic,
  createdAt,
  isSubmitting = false,
  error,
  onToggleVisibility,
}: CloneCardProps) {
  return (
    <Card className="stack-md">
      <div className="entity-title">
        <h2 className="section-title">Clone</h2>
        <Chip tone={isPublic ? 'success' : 'warm'}>
          {isPublic ? 'PUBLIC' : 'PRIVATE'}
        </Chip>
      </div>
      <strong>{alias}</strong>
      <p className="muted-copy">{summary}</p>
      {createdAt ? <p className="meta-line">최근 갱신: {createdAt}</p> : null}
      {error ? <p className="meta-line">{error}</p> : null}
      <div>
        <Button
          variant="secondary"
          disabled={isSubmitting}
          onClick={() => onToggleVisibility(!isPublic)}
        >
          {isSubmitting
            ? '변경 중...'
            : isPublic
              ? '비공개로 전환'
              : '공개로 전환'}
        </Button>
      </div>
    </Card>
  )
}
