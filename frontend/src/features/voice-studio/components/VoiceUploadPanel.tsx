import { useMemo, useRef, useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'

type VoiceUploadPanelProps = {
  isSubmitting?: boolean
  error?: string | null
  onSubmit: (sample: File, alias?: string) => Promise<boolean> | boolean
}

export function VoiceUploadPanel({
  isSubmitting = false,
  error,
  onSubmit,
}: VoiceUploadPanelProps) {
  const inputRef = useRef<HTMLInputElement | null>(null)
  const [alias, setAlias] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [localError, setLocalError] = useState<string | null>(null)

  const fileSummary = useMemo(() => {
    if (!file) {
      return null
    }

    const sizeInMb = `${(file.size / (1024 * 1024)).toFixed(2)} MB`

    return `${file.name} · ${file.type || 'audio/*'} · ${sizeInMb}`
  }, [file])

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!file) {
      setLocalError('업로드할 음성 파일을 먼저 선택해주세요.')
      return
    }

    if (!file.type.startsWith('audio/')) {
      setLocalError('audio/* 타입의 파일만 업로드할 수 있습니다.')
      return
    }

    const wasSuccessful = await onSubmit(file, alias)

    if (wasSuccessful) {
      setAlias('')
      setFile(null)
      setLocalError(null)
      if (inputRef.current) {
        inputRef.current.value = ''
      }
    }
  }

  function handleSelectFile(event: React.ChangeEvent<HTMLInputElement>) {
    const nextFile = event.target.files?.[0] ?? null
    setFile(nextFile)
    setLocalError(null)

    if (nextFile && !alias.trim()) {
      const filename = nextFile.name.replace(/\.[^.]+$/, '')
      setAlias(filename)
    }
  }

  function handleClearFile() {
    setFile(null)
    setLocalError(null)
    if (inputRef.current) {
      inputRef.current.value = ''
    }
  }

  return (
    <Card>
      <form className="stack-md" onSubmit={handleSubmit}>
        <h2 className="section-title">보이스 업로드</h2>
        <p className="muted-copy">
          샘플 음성 업로드와 대표 보이스 교체를 이 패널에서 처리합니다.
        </p>
        <div className="field">
          <label htmlFor="voice-alias">별칭</label>
          <Input
            id="voice-alias"
            value={alias}
            placeholder="차분한 민지"
            onChange={(event) => setAlias(event.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="voice-sample">음성 파일</label>
          <Input
            id="voice-sample"
            type="file"
            accept="audio/*"
            ref={inputRef}
            onChange={handleSelectFile}
          />
        </div>
        {fileSummary ? (
          <div className="upload-summary">
            <strong>선택된 파일</strong>
            <p className="muted-copy">{fileSummary}</p>
            <div>
              <Button type="button" variant="ghost" onClick={handleClearFile}>
                파일 제거
              </Button>
            </div>
          </div>
        ) : (
          <div className="upload-dropzone">
            <strong>업로드 가이드</strong>
            <p className="muted-copy">
              짧고 또렷한 샘플 음성을 올리면 대표 보이스를 교체할 수 있습니다.
            </p>
          </div>
        )}
        {localError ? <p className="meta-line">{localError}</p> : null}
        {error ? <p className="meta-line">{error}</p> : null}
        <div className="entity-title">
          <Button
            type="submit"
            variant="secondary"
            disabled={!file || isSubmitting}
          >
            {isSubmitting ? '업로드 중...' : '업로드'}
          </Button>
        </div>
      </form>
    </Card>
  )
}
