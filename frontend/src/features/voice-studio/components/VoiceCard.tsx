import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Chip } from '../../../components/ui/Chip'

type VoiceCardProps = {
  ready: boolean
  visibility: 'PUBLIC' | 'PRIVATE'
  displayName?: string
  filename?: string
  createdAt?: string
  isSubmitting?: boolean
  error?: string | null
  onToggleVisibility: (isPublic: boolean) => Promise<void> | void
}

export function VoiceCard({
  ready,
  visibility,
  displayName,
  filename,
  createdAt,
  isSubmitting = false,
  error,
  onToggleVisibility,
}: VoiceCardProps) {
  const isPublic = visibility === 'PUBLIC'

  return (
    <Card className="stack-md">
      <div className="entity-title">
        <h2 className="section-title">Voice</h2>
        <Chip tone={ready ? 'success' : 'warm'}>
          {ready ? 'READY' : 'MISSING'}
        </Chip>
        <Chip tone={isPublic ? 'accent' : 'warm'}>
          {visibility}
        </Chip>
      </div>
      {displayName ? <strong>{displayName}</strong> : null}
      <p className="muted-copy">
        보이스는 현재 사용자의 대표 음성 자산으로 다뤄지며, 업데이트 중심의
        흐름으로 설계됩니다.
      </p>
      {filename ? <p className="meta-line">파일: {filename}</p> : null}
      {createdAt ? <p className="meta-line">최근 갱신: {createdAt}</p> : null}
      {error ? <p className="meta-line">{error}</p> : null}
      <div>
        <Button
          variant="secondary"
          disabled={!ready || isSubmitting}
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
