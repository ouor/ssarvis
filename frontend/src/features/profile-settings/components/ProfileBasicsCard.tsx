import { useEffect, useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'

type ProfileBasicsCardProps = {
  displayName: string
  isSubmitting?: boolean
  error?: string | null
  onSubmit: (displayName: string) => Promise<boolean> | boolean
}

export function ProfileBasicsCard({
  displayName,
  isSubmitting = false,
  error,
  onSubmit,
}: ProfileBasicsCardProps) {
  const [value, setValue] = useState(displayName)

  useEffect(() => {
    setValue(displayName)
  }, [displayName])

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await onSubmit(value.trim())
  }

  return (
    <Card>
      <form className="stack-md" onSubmit={handleSubmit}>
        <h2 className="section-title">기본 정보</h2>
        <div className="field">
          <label htmlFor="profile-display-name">표시 이름</label>
          <Input
            id="profile-display-name"
            value={value}
            onChange={(event) => setValue(event.target.value)}
          />
        </div>
        {error ? <p className="meta-line">{error}</p> : null}
        <div>
          <Button
            type="submit"
            disabled={isSubmitting || value.trim().length === 0}
          >
            {isSubmitting ? '저장 중...' : '표시 이름 저장'}
          </Button>
        </div>
      </form>
    </Card>
  )
}
