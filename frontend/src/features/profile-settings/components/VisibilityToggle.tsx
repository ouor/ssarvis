import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Chip } from '../../../components/ui/Chip'
import type { Visibility } from '../../../lib/types/common'

type VisibilityToggleProps = {
  visibility: Visibility
  isSubmitting?: boolean
  error?: string | null
  onChange: (visibility: Visibility) => Promise<void> | void
}

export function VisibilityToggle({
  visibility,
  isSubmitting = false,
  error,
  onChange,
}: VisibilityToggleProps) {
  return (
    <Card className="stack-md">
      <div className="entity-title">
        <h2 className="section-title">공개성</h2>
        <Chip tone={visibility === 'PUBLIC' ? 'success' : 'warm'}>
          {visibility}
        </Chip>
      </div>
      <p className="muted-copy">
        현재 MVP에서는 Settings 성격의 항목으로 프로필 공개 범위를 관리합니다.
      </p>
      {error ? <p className="meta-line">{error}</p> : null}
      <div className="entity-title">
        <Button
          variant={visibility === 'PUBLIC' ? 'primary' : 'secondary'}
          disabled={isSubmitting}
          onClick={() => onChange('PUBLIC')}
        >
          PUBLIC
        </Button>
        <Button
          variant={visibility === 'PRIVATE' ? 'primary' : 'secondary'}
          disabled={isSubmitting}
          onClick={() => onChange('PRIVATE')}
        >
          PRIVATE
        </Button>
      </div>
    </Card>
  )
}
