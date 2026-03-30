import { Card } from '../../../components/ui/Card'

type VoiceMessageCardProps = {
  duration: string
  mine?: boolean
  label?: string
}

export function VoiceMessageCard({
  duration,
  mine = false,
  label = 'Voice Message',
}: VoiceMessageCardProps) {
  return (
    <Card className={`voice-card${mine ? ' voice-card-mine' : ''}`}>
      <div className="entity-title">
        <strong>{label}</strong>
        <span className="meta-line">{duration}</span>
      </div>
      <div className="waveform" aria-hidden="true">
        <span style={{ height: '12px' }} />
        <span style={{ height: '28px' }} />
        <span style={{ height: '18px' }} />
        <span style={{ height: '32px' }} />
        <span style={{ height: '16px' }} />
        <span style={{ height: '24px' }} />
        <span style={{ height: '14px' }} />
      </div>
    </Card>
  )
}
