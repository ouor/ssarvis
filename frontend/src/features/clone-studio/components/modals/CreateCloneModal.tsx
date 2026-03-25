import type { FormEvent } from 'react'
import AppModal from '../../../../components/AppModal'
import type { Question } from '../../types'

type CreateCloneModalProps = {
  questions: Question[]
  answers: Record<number, string>
  answeredCount: number
  loadingQuestions: boolean
  questionError: string
  createCloneError: string
  creatingClone: boolean
  canCreateClone: boolean
  onClose: () => void
  onAnswerChange: (questionIndex: number, answer: string) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
}

function CreateCloneModal({
  questions,
  answers,
  answeredCount,
  loadingQuestions,
  questionError,
  createCloneError,
  creatingClone,
  canCreateClone,
  onClose,
  onAnswerChange,
  onSubmit,
}: CreateCloneModalProps) {
  return (
    <AppModal
      onClose={onClose}
      subtitle="문답을 마치면 새로운 클론이 바로 스튜디오에 추가됩니다."
      title="새 클론 만들기"
    >
      <form className="modal-stack" onSubmit={onSubmit}>
        <div className="questionnaire-scroll">
          {loadingQuestions ? <p className="muted-copy">질문을 불러오는 중입니다.</p> : null}
          {questionError ? <p className="inline-error">{questionError}</p> : null}
          {questions.map((question, index) => (
            <fieldset key={question.question} className="question-card">
              <legend>
                <span>Q{index + 1}</span>
                {question.question}
              </legend>
              <div className="choice-grid">
                {question.choices.map((choice) => (
                  <label
                    key={choice}
                    className={answers[index] === choice ? 'choice-pill choice-pill-active' : 'choice-pill'}
                  >
                    <input
                      checked={answers[index] === choice}
                      name={`question-${index}`}
                      onChange={() => onAnswerChange(index, choice)}
                      type="radio"
                      value={choice}
                    />
                    <span>{choice}</span>
                  </label>
                ))}
              </div>
            </fieldset>
          ))}
        </div>
        {createCloneError ? <p className="inline-error">{createCloneError}</p> : null}
        <div className="modal-footer">
          <span>
            {answeredCount}/{questions.length} 응답 완료
          </span>
          <button className="primary-button" disabled={creatingClone || !canCreateClone} type="submit">
            {creatingClone ? '클론 생성 중...' : '클론 만들기'}
          </button>
        </div>
      </form>
    </AppModal>
  )
}

export default CreateCloneModal
