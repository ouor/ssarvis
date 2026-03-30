import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { ContextPanel } from '../../components/shared/ContextPanel'
import { EmptyState } from '../../components/shared/EmptyState'
import { LoadingState } from '../../components/shared/LoadingState'
import { ToastMessage } from '../../components/shared/ToastMessage'
import { PageHeader } from '../../components/shared/PageHeader'
import { Card } from '../../components/ui/Card'
import { useAuth } from '../../hooks/useAuth'
import {
  getThread,
  getThreads,
  sendTextMessage,
  uploadVoiceMessage,
} from '../../features/dm/api'
import { ConversationView } from '../../features/dm/components/ConversationView'
import { MessageComposer } from '../../features/dm/components/MessageComposer'
import { ThreadList } from '../../features/dm/components/ThreadList'
import {
  toDmMessageViewModel,
  toDmThreadDetailViewModel,
  toDmThreadViewModel,
} from '../../features/dm/mappers'
import type {
  DmMessageViewModel,
  DmThreadDetailViewModel,
  DmThreadViewModel,
} from '../../features/dm/types'
import {
  getRequiredAccessToken,
  SessionRequiredError,
} from '../../lib/api/session'
import {
  getDemoActiveThread,
  getDemoThreadPreviews,
  getDemoUser,
} from '../../lib/demo/adapters'

export function MessagesPage() {
  const { currentUser, isDemo } = useAuth()
  const [threads, setThreads] = useState<DmThreadViewModel[]>(() => getDemoThreadPreviews())
  const [selectedThreadId, setSelectedThreadId] = useState<number | null>(
    () => getDemoThreadPreviews()[0]?.id ?? null,
  )
  const [activeThread, setActiveThread] = useState<DmThreadDetailViewModel | null>(
    isDemo ? getDemoActiveThread() : null,
  )
  const [isThreadsLoading, setIsThreadsLoading] = useState(!isDemo)
  const [isMessagesLoading, setIsMessagesLoading] = useState(false)
  const [threadsError, setThreadsError] = useState<string | null>(null)
  const [messageError, setMessageError] = useState<string | null>(null)
  const [composerError, setComposerError] = useState<string | null>(null)
  const [voiceError, setVoiceError] = useState<string | null>(null)
  const [isSending, setIsSending] = useState(false)
  const [isUploadingVoice, setIsUploadingVoice] = useState(false)
  const [voiceSuccessMessage, setVoiceSuccessMessage] = useState<string | null>(
    null,
  )
  const threadsRequestIdRef = useRef(0)
  const activeThreadRequestIdRef = useRef(0)

  const user = currentUser ?? getDemoUser()

  const activeUser = useMemo(
    () => activeThread?.user ?? threads.find((thread) => thread.id === selectedThreadId)?.user,
    [activeThread, selectedThreadId, threads],
  )

  const loadThreads = useCallback(async () => {
    const requestId = ++threadsRequestIdRef.current

    if (isDemo) {
      if (requestId === threadsRequestIdRef.current) {
        setThreads(
          getDemoThreadPreviews(),
        )
        setSelectedThreadId(getDemoThreadPreviews()[0]?.id ?? null)
        setThreadsError(null)
        setIsThreadsLoading(false)
      }
      return
    }

    if (requestId === threadsRequestIdRef.current) {
      setIsThreadsLoading(true)
      setThreadsError(null)
    }

    try {
      const token = getRequiredAccessToken(
        '세션 정보가 없어 메시지 목록을 불러올 수 없습니다.',
      )
      const response = await getThreads(token)
      const nextThreads = response.map(toDmThreadViewModel)
      if (requestId === threadsRequestIdRef.current) {
        setThreads(nextThreads)
        setSelectedThreadId((current) => current ?? nextThreads[0]?.id ?? null)
      }
    } catch (error) {
      if (requestId === threadsRequestIdRef.current) {
        if (error instanceof SessionRequiredError) {
          setThreadsError(error.message)
        } else {
          setThreadsError('메시지 목록을 불러오지 못했습니다.')
        }
      }
    } finally {
      if (requestId === threadsRequestIdRef.current) {
        setIsThreadsLoading(false)
      }
    }
  }, [isDemo])

  const loadActiveThread = useCallback(async () => {
    const requestId = ++activeThreadRequestIdRef.current

    if (!selectedThreadId) {
      if (requestId === activeThreadRequestIdRef.current) {
        setActiveThread(null)
      }
      return
    }

    if (isDemo) {
      return
    }

    if (requestId === activeThreadRequestIdRef.current) {
      setIsMessagesLoading(true)
      setMessageError(null)
    }

    try {
      const token = getRequiredAccessToken(
        '세션 정보가 없어 대화를 불러올 수 없습니다.',
      )
      const response = await getThread(token, selectedThreadId)
      if (requestId === activeThreadRequestIdRef.current) {
        setActiveThread(toDmThreadDetailViewModel(response))
      }
    } catch (error) {
      if (requestId === activeThreadRequestIdRef.current) {
        if (error instanceof SessionRequiredError) {
          setMessageError(error.message)
        } else {
          setMessageError('대화 내용을 불러오지 못했습니다.')
        }
      }
    } finally {
      if (requestId === activeThreadRequestIdRef.current) {
        setIsMessagesLoading(false)
      }
    }
  }, [isDemo, selectedThreadId])

  useEffect(() => {
    void loadThreads()
  }, [loadThreads])

  useEffect(() => {
    void loadActiveThread()
  }, [loadActiveThread])

  useEffect(() => {
    if (!voiceSuccessMessage) {
      return
    }

    const timer = window.setTimeout(() => {
      setVoiceSuccessMessage(null)
    }, 2600)

    return () => window.clearTimeout(timer)
  }, [voiceSuccessMessage])

  async function handleSendMessage(content: string) {
    if (!selectedThreadId) {
      setComposerError('먼저 대화할 스레드를 선택해주세요.')
      return false
    }

    if (isDemo) {
      const nextMessage: DmMessageViewModel = {
        id: Date.now(),
        authorId: user.userId,
        authorName: user.displayName,
        kind: 'text',
        content,
        createdAt: '방금 전',
        isAiGenerated: false,
      }

      setActiveThread((current) =>
        current
          ? {
              ...current,
              messages: [...current.messages, nextMessage],
            }
          : current,
      )
      setComposerError(null)
      return true
    }

    setIsSending(true)
    setComposerError(null)

    try {
      const token = getRequiredAccessToken(
        '세션 정보가 없어 메시지를 전송할 수 없습니다.',
      )
      const response = await sendTextMessage(token, selectedThreadId, content)
      const nextMessage = toDmMessageViewModel(response)

      setActiveThread((current) =>
        current
          ? {
              ...current,
              messages: [...current.messages, nextMessage],
            }
          : current,
      )

      setThreads((current) =>
        current.map((thread) =>
          thread.id === selectedThreadId
            ? {
                ...thread,
                preview: response.content,
                updatedAt: nextMessage.createdAt,
              }
            : thread,
        ),
      )
      return true
    } catch (error) {
      if (error instanceof SessionRequiredError) {
        setComposerError(error.message)
      } else {
        setComposerError('메시지 전송에 실패했습니다. 잠시 후 다시 시도해주세요.')
      }
      return false
    } finally {
      setIsSending(false)
    }
  }

  async function handleVoiceUpload(audio: File) {
    if (!selectedThreadId) {
      setVoiceError('먼저 대화할 스레드를 선택해주세요.')
      return false
    }

    if (isDemo) {
      const nextMessage: DmMessageViewModel = {
        id: Date.now(),
        authorId: user.userId,
        authorName: user.displayName,
        kind: 'voice',
        content: '음성 메시지',
        createdAt: '방금 전',
        duration: '0:03',
        isAiGenerated: false,
      }

      setActiveThread((current) =>
        current
          ? {
              ...current,
              messages: [...current.messages, nextMessage],
            }
          : current,
      )
      setVoiceError(null)
      setVoiceSuccessMessage(`${audio.name} 음성 메시지를 추가했습니다.`)
      return true
    }

    setIsUploadingVoice(true)
    setVoiceError(null)

    try {
      const token = getRequiredAccessToken(
        '세션 정보가 없어 보이스 메시지를 전송할 수 없습니다.',
      )
      const response = await uploadVoiceMessage(token, selectedThreadId, audio)
      const nextMessage = toDmMessageViewModel(response)

      setActiveThread((current) =>
        current
          ? {
              ...current,
              messages: [...current.messages, nextMessage],
            }
          : current,
      )
      setThreads((current) =>
        current.map((thread) =>
          thread.id === selectedThreadId
            ? {
                ...thread,
                preview: response.content,
                updatedAt: nextMessage.createdAt,
              }
            : thread,
        ),
      )
      setVoiceSuccessMessage(`${audio.name} 음성 메시지를 전송했습니다.`)
      return true
    } catch (error) {
      if (error instanceof SessionRequiredError) {
        setVoiceError(error.message)
      } else {
        setVoiceError('보이스 메시지 업로드에 실패했습니다.')
      }
      return false
    } finally {
      setIsUploadingVoice(false)
    }
  }

  return (
    <>
      <div className="page-shell">
        {voiceSuccessMessage ? (
          <ToastMessage title="보이스 메시지 완료" copy={voiceSuccessMessage} />
        ) : null}
        <PageHeader
          eyebrow="Messages"
          title="텍스트와 음성이 자연스럽게 섞이는 대화 공간"
          subtitle="왼쪽에는 스레드, 중앙에는 대화, 오른쪽에는 상대 정보와 관계 맥락을 두는 3열 구조를 기본값으로 잡습니다."
        />
        <div className="split-grid">
          <div className="stack-md">
            {isThreadsLoading ? (
              <LoadingState
                title="스레드를 불러오는 중입니다"
                copy="가장 최근 대화부터 정리해서 보여주고 있어요."
              />
            ) : threadsError ? (
              <EmptyState title="메시지 목록을 불러올 수 없어요" copy={threadsError} />
            ) : threads.length === 0 ? (
              <EmptyState
                title="아직 대화가 없어요"
                copy="People에서 새로운 사람을 찾고 첫 DM 스레드를 시작해보세요."
              />
            ) : (
              <ThreadList
                threads={threads}
                selectedThreadId={selectedThreadId}
                onSelectThread={setSelectedThreadId}
              />
            )}
          </div>
          <div className="stack-md">
            {!activeUser ? (
              <EmptyState
                title="대화를 선택해주세요"
                copy="왼쪽 스레드 목록에서 대화를 선택하면 메시지와 음성 응답 흐름을 볼 수 있습니다."
              />
            ) : (
              <>
                <Card>
                  <div className="entity-title">
                    <strong>{activeUser.displayName}</strong>
                    <span className="meta-line">@{activeUser.username}</span>
                  </div>
                </Card>
                {isMessagesLoading ? (
                  <LoadingState
                    title="대화 내용을 불러오는 중입니다"
                    copy="텍스트와 음성 메시지 흐름을 스레드 단위로 불러오고 있어요."
                  />
                ) : messageError ? (
                  <EmptyState
                    title="대화를 불러올 수 없어요"
                    copy={messageError}
                  />
                ) : activeThread && activeThread.messages.length > 0 ? (
                  <ConversationView
                    messages={activeThread.messages}
                    myUserId={user.userId}
                  />
                ) : (
                  <EmptyState
                    title="아직 메시지가 없어요"
                    copy="첫 메시지를 보내면 이 공간에서 텍스트와 음성 응답 리듬이 시작됩니다."
                  />
                )}
                <MessageComposer
                  onSubmit={handleSendMessage}
                  onVoiceSubmit={handleVoiceUpload}
                  isSubmitting={isSending}
                  isUploadingVoice={isUploadingVoice}
                  error={composerError}
                  voiceError={voiceError}
                  disabled={!activeUser}
                />
              </>
            )}
          </div>
        </div>
      </div>
      <ContextPanel>
        <Card className="stack-md">
          <h2 className="section-title">대화 상대</h2>
          {activeUser ? (
            <>
              <div className="entity-title">
                <strong>{activeUser.displayName}</strong>
                <span className="meta-line">@{activeUser.username}</span>
              </div>
              <p className="muted-copy">
                공개 프로필은 누구나 DM을 시작할 수 있고, 비공개 프로필은 관계
                상태에 따라 접근이 제한됩니다.
              </p>
            </>
          ) : (
            <p className="muted-copy">선택된 대화 상대가 없습니다.</p>
          )}
        </Card>
        <Card className="stack-md">
          <h2 className="section-title">Voice Detail</h2>
          <p className="muted-copy">
            보이스 메시지는 일반 텍스트 버블과 구분된 시각 리듬을 가지며,
            자동응답 조건이 맞으면 AI 응답이 함께 들어올 수 있습니다.
          </p>
        </Card>
      </ContextPanel>
    </>
  )
}
