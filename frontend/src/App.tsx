import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

type Question = {
  question: string
  choices: string[]
  autochoice: Record<string, string>
}

type AnswerItem = {
  question: string
  answer: string
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
const questionAssetPath = `${import.meta.env.BASE_URL}questions.json`

function App() {
  const [questions, setQuestions] = useState<Question[]>([])
  const [answers, setAnswers] = useState<Record<number, string>>({})
  const [loadingQuestions, setLoadingQuestions] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [systemPrompt, setSystemPrompt] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    async function loadQuestions() {
      setLoadingQuestions(true)
      setError('')

      try {
        const response = await fetch(questionAssetPath, {
          signal: controller.signal,
        })

        if (!response.ok) {
          throw new Error(`질문 목록을 불러오지 못했습니다. (${response.status})`)
        }

        const data: Question[] = await response.json()
        setQuestions(data)
      } catch (fetchError) {
        if (fetchError instanceof DOMException && fetchError.name === 'AbortError') {
          return
        }

        setError(fetchError instanceof Error ? fetchError.message : '질문 목록을 불러오는 중 오류가 발생했습니다.')
      } finally {
        setLoadingQuestions(false)
      }
    }

    loadQuestions()

    return () => controller.abort()
  }, [])

  const answeredCount = Object.values(answers).filter((answer) => answer.trim().length > 0).length

  const isComplete = questions.length > 0 && answeredCount === questions.length

  function updateAnswer(questionIndex: number, answer: string) {
    setAnswers((current) => ({
      ...current,
      [questionIndex]: answer,
    }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!isComplete) {
      setError('모든 질문에 답변해야 시스템 프롬프트를 생성할 수 있습니다.')
      return
    }

    const payload: { answers: AnswerItem[] } = {
      answers: questions.map((question, index) => ({
        question: question.question,
        answer: answers[index],
      })),
    }

    setSubmitting(true)
    setError('')

    try {
      const response = await fetch(`${apiBaseUrl}/api/system-prompt`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      })

      if (!response.ok) {
        const message = await response.text()
        throw new Error(message || `시스템 프롬프트 생성에 실패했습니다. (${response.status})`)
      }

      const data: { systemPrompt: string } = await response.json()
      setSystemPrompt(data.systemPrompt)
    } catch (submitError) {
      setSystemPrompt('')
      setError(submitError instanceof Error ? submitError.message : '시스템 프롬프트 생성 중 오류가 발생했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="app-shell">
      <section className="hero-panel">
        <p className="eyebrow">Prompt Builder</p>
        <h1>응답 기반 시스템 프롬프트 생성기</h1>
        <p className="hero-copy">
          질문에 답변하면 백엔드가 OpenAI를 호출해 성향과 대화 스타일을 반영한 시스템 프롬프트를 생성합니다.
        </p>
        <div className="hero-meta">
          <span>{loadingQuestions ? '질문 로딩 중' : `질문 ${questions.length}개`}</span>
          <span>답변 {answeredCount}개</span>
        </div>
      </section>

      <section className="content-grid">
        <form className="questionnaire-panel" onSubmit={handleSubmit}>
          <div className="panel-header">
            <div>
              <p className="section-label">Questionnaire</p>
              <h2>질문에 응답하기</h2>
            </div>
            <button
              className="primary-button"
              type="submit"
              disabled={loadingQuestions || submitting || !questions.length}
            >
              {submitting ? '생성 중...' : '시스템 프롬프트 생성'}
            </button>
          </div>

          {loadingQuestions ? (
            <div className="empty-state">질문 목록을 불러오는 중입니다.</div>
          ) : (
            <div className="question-list">
              {questions.map((question, index) => (
                <article className="question-card" key={`${question.question}-${index}`}>
                  <div className="question-order">Q{index + 1}</div>
                  <h3>{question.question}</h3>
                  <div className="choice-grid">
                    {question.choices.map((choice) => {
                      const inputId = `question-${index}-${choice}`
                      const checked = answers[index] === choice

                      return (
                        <label className={`choice-item${checked ? ' is-selected' : ''}`} htmlFor={inputId} key={choice}>
                          <input
                            checked={checked}
                            id={inputId}
                            name={`question-${index}`}
                            onChange={() => updateAnswer(index, choice)}
                            type="radio"
                            value={choice}
                          />
                          <span>{choice}</span>
                        </label>
                      )
                    })}
                  </div>
                </article>
              ))}
            </div>
          )}
        </form>

        <aside className="result-panel">
          <div className="panel-header">
            <div>
              <p className="section-label">Result</p>
              <h2>생성된 시스템 프롬프트</h2>
            </div>
          </div>

          {error ? <div className="feedback error">{error}</div> : null}

          {systemPrompt ? (
            <div className="prompt-output">
              <pre>{systemPrompt}</pre>
            </div>
          ) : (
            <div className="empty-state">
              모든 질문에 답변한 뒤 생성 버튼을 누르면 여기에 결과가 표시됩니다.
            </div>
          )}
        </aside>
      </section>
    </main>
  )
}

export default App
