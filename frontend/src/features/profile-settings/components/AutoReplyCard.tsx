import { Card } from '../../../components/ui/Card'
import { Chip } from '../../../components/ui/Chip'
import { Button } from '../../../components/ui/Button'
import type { AutoReplyMode } from '../types'

type AutoReplyCardProps = {
  mode: AutoReplyMode
  lastActivityAt: string
  isSubmitting?: boolean
  error?: string | null
  onChange: (mode: AutoReplyMode) => Promise<void> | void
}

export function AutoReplyCard({
  mode,
  lastActivityAt,
  isSubmitting = false,
  error,
  onChange,
}: AutoReplyCardProps) {
  return (
    <Card className="stack-md">
      <div className="entity-title">
        <h2 className="section-title">자동응답</h2>
        <Chip tone="accent">{mode}</Chip>
      </div>
      <p className="muted-copy">
        마지막 활동 이후에는 상태에 맞춰 자동응답 흐름이 동작합니다. 프로필
        설정에서 톤과 모드를 조정할 수 있도록 설계합니다.
      </p>
      <p className="meta-line">마지막 활동: {lastActivityAt}</p>
      {error ? <p className="meta-line">{error}</p> : null}
      <div className="entity-title">
        {(['ALWAYS', 'AWAY', 'OFF'] as const).map((nextMode) => (
          <Button
            key={nextMode}
            variant={mode === nextMode ? 'primary' : 'secondary'}
            disabled={isSubmitting}
            onClick={() => onChange(nextMode)}
          >
            {nextMode}
          </Button>
        ))}
      </div>
    </Card>
  )
}
