type Question = {
  question: string
  choices: string[]
  autochoice: Record<string, string>
}

type QuestionnairePanelProps = {
  answeredCount: number
  loadingQuestions: boolean
  questions: Question[]
  submitting: boolean
  answers: Record<number, string>
  onSubmit: React.FormEventHandler<HTMLFormElement>
  onAnswerChange: (questionIndex: number, answer: string) => void
}

function QuestionnairePanel({
  answeredCount,
  loadingQuestions,
  questions,
  submitting,
  answers,
  onSubmit,
  onAnswerChange,
}: QuestionnairePanelProps) {
  return (
    <form className="questionnaire-panel" onSubmit={onSubmit}>
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

      <div className="progress-copy">현재 {questions.length}개 중 {answeredCount}개 답변했습니다.</div>

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
                        onChange={() => onAnswerChange(index, choice)}
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
  )
}

export type { Question }
export default QuestionnairePanel
