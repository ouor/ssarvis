import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Textarea } from '../../../components/ui/Textarea'

type PostComposerProps = {
  isSubmitting?: boolean
  error?: string | null
  onSubmit: (content: string) => Promise<boolean> | boolean
}

export function PostComposer({
  isSubmitting = false,
  error,
  onSubmit,
}: PostComposerProps) {
  const [content, setContent] = useState('')

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const nextContent = content.trim()

    if (!nextContent) {
      return
    }

    const wasSuccessful = await onSubmit(nextContent)

    if (wasSuccessful) {
      setContent('')
    }
  }

  return (
    <Card>
      <form className="stack-md" onSubmit={handleSubmit}>
        <div className="stack-sm">
          <h2 className="section-title">지금 떠오르는 생각을 남겨보세요</h2>
        </div>
        <Textarea
          placeholder="오늘의 대화, 감정, 스튜디오에서 다듬은 아이디어를 적어보세요."
          value={content}
          onChange={(event) => setContent(event.target.value)}
        />
        {error ? <p className="meta-line">{error}</p> : null}
        <div className="justify-end">
          <Button
            type="submit"
            disabled={isSubmitting || content.trim().length === 0}
          >
            {isSubmitting ? '게시 중...' : '게시하기'}
          </Button>
        </div>
      </form>
    </Card>
  )
}
