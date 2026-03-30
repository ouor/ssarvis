import { useEffect, useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Textarea } from '../../../components/ui/Textarea'

type ClonePromptEditorProps = {
  initialValue?: string
  isSubmitting?: boolean
  error?: string | null
  onSubmit: (answer: string) => Promise<boolean> | boolean
}

export function ClonePromptEditor({
  initialValue = '',
  isSubmitting = false,
  error,
  onSubmit,
}: ClonePromptEditorProps) {
  const [value, setValue] = useState(initialValue)

  useEffect(() => {
    setValue(initialValue)
  }, [initialValue])

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await onSubmit(value.trim())
  }

  return (
    <Card>
      <form className="stack-md" onSubmit={handleSubmit}>
        <h2 className="section-title">프롬프트 에디터</h2>
        <Textarea
          placeholder="클론이 어떤 말투와 관점을 유지해야 하는지 작성하세요."
          value={value}
          onChange={(event) => setValue(event.target.value)}
        />
        {error ? <p className="meta-line">{error}</p> : null}
        <div>
          <Button
            type="submit"
            disabled={isSubmitting || value.trim().length === 0}
          >
            {isSubmitting ? '프롬프트 생성 중...' : '프롬프트 저장'}
          </Button>
        </div>
      </form>
    </Card>
  )
}
