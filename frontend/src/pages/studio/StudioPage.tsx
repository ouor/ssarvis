import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { EmptyState } from '../../components/shared/EmptyState'
import { LoadingState } from '../../components/shared/LoadingState'
import { PageHeader } from '../../components/shared/PageHeader'
import { ToastMessage } from '../../components/shared/ToastMessage'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { Chip } from '../../components/ui/Chip'
import { useAuth } from '../../hooks/useAuth'
import {
  getChatConversation,
  listChatConversations,
  sendChatMessage,
} from '../../features/chat/api'
import { ConversationHistoryList } from '../../features/chat/components/ConversationHistoryList'
import { TestChatPanel } from '../../features/chat/components/TestChatPanel'
import type {
  ChatConversationSummary,
  TestChatMessage,
} from '../../features/chat/types'
import {
  getClones,
  saveSystemPrompt,
  updateCloneVisibility,
} from '../../features/clone-studio/api'
import { CloneCard } from '../../features/clone-studio/components/CloneCard'
import { ClonePromptEditor } from '../../features/clone-studio/components/ClonePromptEditor'
import { formatCloneTimestamp } from '../../features/clone-studio/mappers'
import type {
  ClonePromptResponse,
  CloneResponse,
} from '../../features/clone-studio/types'
import {
  getVoices,
  updateVoiceVisibility,
  uploadVoice,
} from '../../features/voice-studio/api'
import { VoiceCard } from '../../features/voice-studio/components/VoiceCard'
import { VoiceUploadPanel } from '../../features/voice-studio/components/VoiceUploadPanel'
import { formatVoiceTimestamp } from '../../features/voice-studio/mappers'
import type { VoiceResponse } from '../../features/voice-studio/types'
import {
  getRequiredAccessToken,
  SessionRequiredError,
} from '../../lib/api/session'
import {
  getDemoStudioClone,
  getDemoStudioChatMessages,
  getDemoStudioConversationSummaries,
  getDemoStudioVoice,
} from '../../lib/demo/adapters'

export function StudioPage() {
  const { isDemo } = useAuth()
  const [clone, setClone] = useState<CloneResponse | null>(
    isDemo ? getDemoStudioClone() : null,
  )
  const [promptResult, setPromptResult] = useState<ClonePromptResponse | null>(null)
  const [voice, setVoice] = useState<VoiceResponse | null>(
    isDemo ? getDemoStudioVoice() : null,
  )
  const [isLoading, setIsLoading] = useState(!isDemo)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [cloneError, setCloneError] = useState<string | null>(null)
  const [promptError, setPromptError] = useState<string | null>(null)
  const [voiceError, setVoiceError] = useState<string | null>(null)
  const [isSavingPrompt, setIsSavingPrompt] = useState(false)
  const [isSavingCloneVisibility, setIsSavingCloneVisibility] = useState(false)
  const [isSavingVoiceVisibility, setIsSavingVoiceVisibility] = useState(false)
  const [isUploadingVoice, setIsUploadingVoice] = useState(false)
  const [voiceSuccessMessage, setVoiceSuccessMessage] = useState<string | null>(null)
  const [conversations, setConversations] = useState<ChatConversationSummary[]>(
    () => getDemoStudioConversationSummaries(),
  )
  const [chatConversationId, setChatConversationId] = useState<number | null>(null)
  const [chatMessages, setChatMessages] = useState<TestChatMessage[]>([])
  const [chatError, setChatError] = useState<string | null>(null)
  const [isSendingChat, setIsSendingChat] = useState(false)
  const studioRequestIdRef = useRef(0)
  const chatConversationRequestIdRef = useRef(0)

  const loadStudio = useCallback(async () => {
    const requestId = ++studioRequestIdRef.current

    if (isDemo) {
      if (requestId === studioRequestIdRef.current) {
        setLoadError(null)
        setIsLoading(false)
      }
      return
    }

    if (requestId === studioRequestIdRef.current) {
      setIsLoading(true)
      setLoadError(null)
    }

    try {
      const token = getRequiredAccessToken(
        '세션 정보가 없어 스튜디오를 불러올 수 없습니다.',
      )
      const [clonesResponse, voicesResponse, conversationsResponse] =
        await Promise.all([
          getClones(token),
          getVoices(token),
          listChatConversations(token),
        ])

      if (requestId === studioRequestIdRef.current) {
        setClone(clonesResponse[0] ?? null)
        setVoice(voicesResponse[0] ?? null)
        setConversations(conversationsResponse)
        setChatConversationId(conversationsResponse[0]?.conversationId ?? null)
      }
    } catch (error) {
      if (requestId === studioRequestIdRef.current) {
        if (error instanceof SessionRequiredError) {
          setLoadError(error.message)
        } else {
          setLoadError('스튜디오 자산을 불러오지 못했습니다.')
        }
      }
    } finally {
      if (requestId === studioRequestIdRef.current) {
        setIsLoading(false)
      }
    }
  }, [isDemo])

  useEffect(() => {
    void loadStudio()
  }, [loadStudio])

  const loadChatConversation = useCallback(async () => {
    const requestId = ++chatConversationRequestIdRef.current

    if (isDemo || !chatConversationId) {
      if (requestId === chatConversationRequestIdRef.current) {
        setChatMessages([])
      }
      return
    }

    try {
      const token = getRequiredAccessToken('')
      const response = await getChatConversation(token, chatConversationId)
      if (requestId === chatConversationRequestIdRef.current) {
        setChatMessages(
          response.messages.map((message, index) => ({
            id: `${message.role}-${index}-${message.createdAt}`,
            role: message.role === 'assistant' ? 'assistant' : 'user',
            content: message.content,
            createdAt: new Intl.DateTimeFormat('ko-KR', {
              month: 'short',
              day: 'numeric',
              hour: '2-digit',
              minute: '2-digit',
            }).format(new Date(message.createdAt)),
            audioState: message.ttsAudioUrl || message.ttsVoiceId ? 'voice' : 'text',
          })),
        )
        setChatError(null)
      }
    } catch (error) {
      if (error instanceof SessionRequiredError) {
        return
      }
      if (requestId === chatConversationRequestIdRef.current) {
        setChatError('이전 테스트 대화를 불러오지 못했습니다.')
      }
    }
  }, [chatConversationId, isDemo])

  useEffect(() => {
    void loadChatConversation()
  }, [loadChatConversation])

  useEffect(() => {
    if (!voiceSuccessMessage) {
      return
    }

    const timer = window.setTimeout(() => {
      setVoiceSuccessMessage(null)
    }, 2800)

    return () => window.clearTimeout(timer)
  }, [voiceSuccessMessage])

  async function handlePromptSubmit(answer: string) {
    if (!answer) {
      setPromptError('프롬프트 생성을 위한 답변을 입력해주세요.')
      return false
    }

    if (isDemo) {
      setPromptResult({
        promptGenerationLogId: Date.now(),
        alias: '차분한 조력자',
        shortDescription: answer,
        systemPrompt: answer,
      })
      setClone((current) =>
        current
          ? {
              ...current,
              alias: '차분한 조력자',
              shortDescription: answer,
              isPublic: false,
              createdAt: new Date().toISOString(),
            }
          : {
              cloneId: Date.now(),
              createdAt: new Date().toISOString(),
              alias: '차분한 조력자',
              shortDescription: answer,
              isPublic: false,
              ownerDisplayName: '하루',
            },
      )
      setPromptError(null)
      return true
    }

    setIsSavingPrompt(true)
    setPromptError(null)

    try {
      const token = getRequiredAccessToken(
        '세션이 없어 프롬프트를 저장할 수 없습니다.',
      )
      const response = await saveSystemPrompt(token, answer)
      setPromptResult(response)
      await loadStudio()
      return true
    } catch (error) {
      if (error instanceof SessionRequiredError) {
        setPromptError(error.message)
      } else {
        setPromptError('프롬프트 생성에 실패했습니다.')
      }
      return false
    } finally {
      setIsSavingPrompt(false)
    }
  }

  async function handleCloneVisibilityToggle(isPublic: boolean) {
    if (!clone) {
      return
    }

    if (isDemo) {
      setClone({ ...clone, isPublic })
      setCloneError(null)
      return
    }

    setIsSavingCloneVisibility(true)
    setCloneError(null)

    try {
      const token = getRequiredAccessToken(
        '세션이 없어 공개성을 변경할 수 없습니다.',
      )
      await updateCloneVisibility(token, clone.cloneId, isPublic)
      setClone({ ...clone, isPublic })
    } catch (error) {
      if (error instanceof SessionRequiredError) {
        setCloneError(error.message)
      } else {
        setCloneError('클론 공개성 변경에 실패했습니다.')
      }
    } finally {
      setIsSavingCloneVisibility(false)
    }
  }

  async function handleVoiceUpload(sample: File, alias?: string) {
    if (isDemo) {
      setVoice({
        registeredVoiceId: Date.now(),
        voiceId: 'demo-uploaded-voice',
        displayName: alias?.trim() || sample.name,
        preferredName: alias?.trim() || 'samplevoice',
        originalFilename: sample.name,
        audioMimeType: sample.type || 'audio/mpeg',
        createdAt: new Date().toISOString(),
        isPublic: false,
        ownerDisplayName: '하루',
      })
      setVoiceError(null)
      setVoiceSuccessMessage('대표 보이스가 데모 상태에서 갱신되었습니다.')
      return true
    }

    setIsUploadingVoice(true)
    setVoiceError(null)

    try {
      const token = getRequiredAccessToken(
        '세션이 없어 보이스를 업로드할 수 없습니다.',
      )
      const response = await uploadVoice(token, sample, alias)
      setVoice(response)
      setVoiceSuccessMessage(
        `${response.displayName} 보이스가 업로드되어 대표 자산으로 반영되었습니다.`,
      )
      return true
    } catch (error) {
      if (error instanceof SessionRequiredError) {
        setVoiceError(error.message)
      } else {
        setVoiceError('보이스 업로드에 실패했습니다.')
      }
      return false
    } finally {
      setIsUploadingVoice(false)
    }
  }

  async function handleVoiceVisibilityToggle(isPublic: boolean) {
    if (!voice) {
      return
    }

    if (isDemo) {
      setVoice({ ...voice, isPublic })
      setVoiceError(null)
      return
    }

    setIsSavingVoiceVisibility(true)
    setVoiceError(null)

    try {
      const token = getRequiredAccessToken(
        '세션이 없어 보이스 공개성을 변경할 수 없습니다.',
      )
      await updateVoiceVisibility(token, voice.registeredVoiceId, isPublic)
      setVoice({ ...voice, isPublic })
    } catch (error) {
      if (error instanceof SessionRequiredError) {
        setVoiceError(error.message)
      } else {
        setVoiceError('보이스 공개성 변경에 실패했습니다.')
      }
    } finally {
      setIsSavingVoiceVisibility(false)
    }
  }

  async function handleTestChatSubmit(message: string) {
    if (!clone) {
      setChatError('대표 클론이 있어야 테스트 대화를 시작할 수 있습니다.')
      return false
    }

    if (isDemo) {
      setChatMessages((current) => [
        ...current,
        ...getDemoStudioChatMessages(clone.alias, Boolean(voice), message),
      ])
      setChatError(null)
      return true
    }

    setIsSendingChat(true)
    setChatError(null)

    try {
      const token = getRequiredAccessToken(
        '세션이 없어 테스트 대화를 보낼 수 없습니다.',
      )
      const response = await sendChatMessage(token, {
        promptGenerationLogId: null,
        conversationId: chatConversationId,
        registeredVoiceId: voice?.registeredVoiceId ?? null,
        message,
      })

      const timestamp = new Intl.DateTimeFormat('ko-KR', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      }).format(new Date())

      setChatConversationId(response.conversationId)
      setConversations((current) => {
        const preview = response.assistantMessage
        const nextConversation = {
          conversationId: response.conversationId,
          cloneId: clone.cloneId,
          cloneAlias: clone.alias,
          createdAt: new Date().toISOString(),
          latestMessagePreview: preview,
          messageCount: (current.find((item) => item.conversationId === response.conversationId)?.messageCount ?? 0) + 2,
        }

        const others = current.filter(
          (item) => item.conversationId !== response.conversationId,
        )

        return [nextConversation, ...others]
      })
      setChatMessages((current) => [
        ...current,
        {
          id: `user-${Date.now()}`,
          role: 'user',
          content: message,
          createdAt: timestamp,
        },
        {
          id: `assistant-${Date.now() + 1}`,
          role: 'assistant',
          content: response.assistantMessage,
          createdAt: timestamp,
          audioState: response.ttsAudioBase64 ? 'voice' : 'text',
        },
      ])
      return true
    } catch (error) {
      if (error instanceof SessionRequiredError) {
        setChatError(error.message)
      } else {
        setChatError('테스트 대화 응답 생성에 실패했습니다.')
      }
      return false
    } finally {
      setIsSendingChat(false)
    }
  }

  const cloneVisibilityLabel = clone?.isPublic ? 'PUBLIC' : 'PRIVATE'
  const voiceVisibilityLabel = voice?.isPublic ? 'PUBLIC' : 'PRIVATE'
  const promptInitialValue = useMemo(
    () => promptResult?.systemPrompt ?? clone?.shortDescription ?? '',
    [clone?.shortDescription, promptResult?.systemPrompt],
  )

  return (
    <div className="page-shell">
      {voiceSuccessMessage ? (
        <ToastMessage
          title="보이스 업로드 완료"
          copy={voiceSuccessMessage}
        />
      ) : null}
      <PageHeader
        eyebrow="Studio"
        title="사람의 말투와 음성을 다듬는 페르소나 작업실"
        subtitle="스튜디오는 메인 소셜 흐름 안에 있지만, 시각적으로는 조금 더 정제된 실험실 느낌을 가져갑니다."
        actions={<Button>테스트 대화</Button>}
      />
      <div className="entity-title">
        <Chip tone={clone ? 'success' : 'warm'}>
          {clone ? 'Clone Ready' : 'Clone Missing'}
        </Chip>
        <Chip tone={voice ? 'success' : 'warm'}>
          {voice ? 'Voice Ready' : 'Voice Missing'}
        </Chip>
        {isDemo ? <Chip tone="warm">DEMO</Chip> : null}
      </div>
      {isLoading ? (
        <LoadingState
          title="스튜디오 자산을 불러오는 중입니다"
          copy="대표 클론과 대표 보이스를 기준으로 현재 작업실 상태를 구성하고 있어요."
        />
      ) : loadError ? (
        <EmptyState title="스튜디오를 불러올 수 없어요" copy={loadError} />
      ) : (
        <div className="two-col">
          <div className="stack-md">
            {clone ? (
              <CloneCard
                alias={clone.alias}
                summary={clone.shortDescription}
                isPublic={clone.isPublic}
                createdAt={formatCloneTimestamp(clone.createdAt)}
                isSubmitting={isSavingCloneVisibility}
                error={cloneError}
                onToggleVisibility={handleCloneVisibilityToggle}
              />
            ) : (
              <EmptyState
                title="대표 클론이 아직 없어요"
                copy="아래 프롬프트 에디터에서 답변을 입력해 대표 클론을 생성해보세요."
              />
            )}
            <ClonePromptEditor
              initialValue={promptInitialValue}
              isSubmitting={isSavingPrompt}
              error={promptError}
              onSubmit={handlePromptSubmit}
            />
          </div>
          <div className="stack-md">
            <VoiceCard
              ready={Boolean(voice)}
              visibility={voiceVisibilityLabel}
              displayName={voice?.displayName}
              filename={voice?.originalFilename}
              createdAt={
                voice ? formatVoiceTimestamp(voice.createdAt) : undefined
              }
              isSubmitting={isSavingVoiceVisibility}
              error={voiceError}
              onToggleVisibility={handleVoiceVisibilityToggle}
            />
            <VoiceUploadPanel
              isSubmitting={isUploadingVoice}
              error={voiceError}
              onSubmit={handleVoiceUpload}
            />
          </div>
        </div>
      )}
      <TestChatPanel
        title="Studio 테스트 대화"
        messages={chatMessages}
        isSubmitting={isSendingChat}
        error={chatError}
        disabled={!clone}
        hasVoiceReply={Boolean(voice)}
        onSubmit={handleTestChatSubmit}
      />
      <ConversationHistoryList
        conversations={conversations}
        selectedConversationId={chatConversationId}
        onSelectConversation={setChatConversationId}
      />
      <Card className="stack-md">
        <h2 className="section-title">다음 단계</h2>
        <p className="muted-copy">
          클론과 보이스는 현재 대표 자산 1개씩을 중심으로 관리되며, 갱신 시
          기본 공개 여부가 다시 닫히는 정책을 UI에도 반영합니다.
        </p>
        <div className="entity-title">
          <Chip tone={cloneVisibilityLabel === 'PUBLIC' ? 'success' : 'warm'}>
            Clone {cloneVisibilityLabel}
          </Chip>
          <Chip tone={voiceVisibilityLabel === 'PUBLIC' ? 'accent' : 'warm'}>
            Voice {voiceVisibilityLabel}
          </Chip>
        </div>
      </Card>
    </div>
  )
}
