import { useEffect, useMemo, useRef, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { apiBaseUrl, apiFetch, formatAssetAccessError, questionAssetPath, readErrorMessage, readNdjsonStream } from '../api'
import type {
  CloneOption,
  ChatConversationDetail,
  ChatConversationSummary,
  CurrentUser,
  DebateSessionDetail,
  DebateSessionSummary,
  FriendRequestSummary,
  FriendSummary,
  LiveChatState,
  LiveDebateState,
  ModalState,
  PromptGenerateResponse,
  Question,
  StudioTab,
  UserSearchResponse,
  VoiceOption,
  VoiceRegisterResponse,
} from '../types'
import PcmStreamPlayer from '../../../utils/PcmStreamPlayer'
import { useSpeechInput } from './useSpeechInput'

export function useCloneStudio(currentUser: CurrentUser) {
  const [activeTab, setActiveTab] = useState<StudioTab>('clones')
  const [modalState, setModalState] = useState<ModalState>(null)
  const [questions, setQuestions] = useState<Question[]>([])
  const [answers, setAnswers] = useState<Record<number, string>>({})
  const [loadingQuestions, setLoadingQuestions] = useState(true)
  const [questionError, setQuestionError] = useState('')
  const [creatingClone, setCreatingClone] = useState(false)
  const [createCloneError, setCreateCloneError] = useState('')
  const [mineClones, setMineClones] = useState<CloneOption[]>([])
  const [friendClones, setFriendClones] = useState<CloneOption[]>([])
  const [publicClones, setPublicClones] = useState<CloneOption[]>([])
  const [cloneLoadError, setCloneLoadError] = useState('')
  const [mineVoices, setMineVoices] = useState<VoiceOption[]>([])
  const [friendVoices, setFriendVoices] = useState<VoiceOption[]>([])
  const [publicVoices, setPublicVoices] = useState<VoiceOption[]>([])
  const [voiceLoadError, setVoiceLoadError] = useState('')
  const [cloneVisibilityUpdatingId, setCloneVisibilityUpdatingId] = useState<number | null>(null)
  const [voiceVisibilityUpdatingId, setVoiceVisibilityUpdatingId] = useState<number | null>(null)
  const [chatHistory, setChatHistory] = useState<ChatConversationSummary[]>([])
  const [chatHistoryLoadError, setChatHistoryLoadError] = useState('')
  const [debateHistory, setDebateHistory] = useState<DebateSessionSummary[]>([])
  const [debateHistoryLoadError, setDebateHistoryLoadError] = useState('')
  const [friends, setFriends] = useState<FriendSummary[]>([])
  const [receivedFriendRequests, setReceivedFriendRequests] = useState<FriendRequestSummary[]>([])
  const [sentFriendRequests, setSentFriendRequests] = useState<FriendRequestSummary[]>([])
  const [friendLoadError, setFriendLoadError] = useState('')
  const [friendSearchQuery, setFriendSearchQuery] = useState('')
  const [friendSearchResults, setFriendSearchResults] = useState<UserSearchResponse[]>([])
  const [friendSearchError, setFriendSearchError] = useState('')
  const [friendSearchLoading, setFriendSearchLoading] = useState(false)
  const [friendActionKey, setFriendActionKey] = useState<string | null>(null)
  const [selectedVoiceId, setSelectedVoiceId] = useState('')
  const [voiceAlias, setVoiceAlias] = useState('')
  const [voiceFile, setVoiceFile] = useState<File | null>(null)
  const [voiceRegistering, setVoiceRegistering] = useState(false)
  const [voiceRegisterError, setVoiceRegisterError] = useState('')
  const [debateTopic, setDebateTopic] = useState('')
  const [debateOpponentId, setDebateOpponentId] = useState('')
  const [debateVoiceAId, setDebateVoiceAId] = useState('')
  const [debateVoiceBId, setDebateVoiceBId] = useState('')
  const [debateSetupError, setDebateSetupError] = useState('')
  const [liveChat, setLiveChat] = useState<LiveChatState | null>(null)
  const [liveDebate, setLiveDebate] = useState<LiveDebateState | null>(null)

  const chatAbortControllerRef = useRef<AbortController | null>(null)
  const debateAbortControllerRef = useRef<AbortController | null>(null)
  const chatPlayerRef = useRef<PcmStreamPlayer | null>(null)
  const debatePlayerRef = useRef<PcmStreamPlayer | null>(null)
  const debateSessionIdRef = useRef<number | null>(null)
  const debateRunningRef = useRef(false)
  const liveChatInputRef = useRef('')

  const speechInput = useSpeechInput({
    getInput: () => liveChatInputRef.current,
    onInputChange: (value) => {
      setLiveChat((current) =>
        current
          ? {
              ...current,
              input: value,
            }
          : current
      )
    },
  })

  const selectedClone =
    modalState?.type === 'clone-actions' || modalState?.type === 'voice-picker' || modalState?.type === 'debate-setup'
      ? modalState.clone
      : null

  const answeredCount = useMemo(
    () => Object.values(answers).filter((answer) => answer.trim().length > 0).length,
    [answers]
  )

  const canCreateClone = questions.length > 0 && answeredCount === questions.length
  const clones = useMemo(() => {
    const seenIds = new Set<number>()
    return [...mineClones, ...friendClones, ...publicClones].filter((clone) => {
      if (seenIds.has(clone.cloneId)) {
        return false
      }
      seenIds.add(clone.cloneId)
      return true
    })
  }, [mineClones, friendClones, publicClones])
  const voices = useMemo(() => {
    const seenIds = new Set<number>()
    return [...mineVoices, ...friendVoices, ...publicVoices].filter((voice) => {
      if (seenIds.has(voice.registeredVoiceId)) {
        return false
      }
      seenIds.add(voice.registeredVoiceId)
      return true
    })
  }, [mineVoices, friendVoices, publicVoices])
  const debateOpponent = useMemo(
    () => clones.find((clone) => clone.cloneId === Number(debateOpponentId)) ?? null,
    [clones, debateOpponentId]
  )

  function createAccessAwareMessage(message: string, fallbackMessage: string) {
    return formatAssetAccessError(message, fallbackMessage)
  }

  function stopRenderedAudio() {
    const audioElements = document.querySelectorAll('audio')
    for (const audioElement of audioElements) {
      audioElement.pause()
    }
  }

  async function clearActiveSession() {
    chatAbortControllerRef.current?.abort()
    debateAbortControllerRef.current?.abort()
    await chatPlayerRef.current?.dispose()
    await debatePlayerRef.current?.dispose()
    chatPlayerRef.current = null
    debatePlayerRef.current = null
    stopRenderedAudio()
    await speechInput.stop()
  }

  useEffect(() => {
    const controller = new AbortController()

    async function loadQuestions() {
      setLoadingQuestions(true)
      setQuestionError('')

      try {
        const response = await fetch(questionAssetPath, { signal: controller.signal })
        if (!response.ok) {
          throw new Error(`질문 목록을 불러오지 못했습니다. (${response.status})`)
        }
        const data: Question[] = await response.json()
        setQuestions(data)
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') {
          return
        }
        setQuestionError(error instanceof Error ? error.message : '질문 목록을 불러오는 중 오류가 발생했습니다.')
      } finally {
        setLoadingQuestions(false)
      }
    }

    void loadQuestions()

    return () => controller.abort()
  }, [])

  useEffect(() => {
    const controller = new AbortController()

    async function loadUserOwnedResources() {
      setActiveTab('clones')
      setModalState(null)
      setAnswers({})
      setCreateCloneError('')
      setCloneLoadError('')
      setMineClones([])
      setFriendClones([])
      setPublicClones([])
      setVoiceLoadError('')
      setMineVoices([])
      setFriendVoices([])
      setPublicVoices([])
      setChatHistoryLoadError('')
      setChatHistory([])
      setDebateHistoryLoadError('')
      setDebateHistory([])
      setFriends([])
      setReceivedFriendRequests([])
      setSentFriendRequests([])
      setFriendLoadError('')
      setFriendSearchQuery('')
      setFriendSearchResults([])
      setFriendSearchError('')
      setFriendSearchLoading(false)
      setFriendActionKey(null)
      setSelectedVoiceId('')
      setVoiceAlias('')
      setVoiceFile(null)
      setVoiceRegisterError('')
      setDebateTopic('')
      setDebateOpponentId('')
      setDebateVoiceAId('')
      setDebateVoiceBId('')
      setDebateSetupError('')
      setLiveChat(null)
      setLiveDebate(null)
      await clearActiveSession()
      await Promise.all([
        loadClones(controller.signal),
        loadVoices(controller.signal),
        loadChatHistory(controller.signal),
        loadDebateHistory(controller.signal),
      ])
    }

    void loadUserOwnedResources()

    return () => {
      controller.abort()
    }
  }, [currentUser.userId])

  useEffect(() => {
    if (activeTab !== 'friends') {
      return
    }

    const controller = new AbortController()
    void loadFriendData(controller.signal)

    return () => {
      controller.abort()
    }
  }, [activeTab, currentUser.userId])

  useEffect(() => {
    debateSessionIdRef.current = liveDebate?.debateSessionId ?? null
    debateRunningRef.current = liveDebate?.running ?? false
  }, [liveDebate])

  useEffect(() => {
    liveChatInputRef.current = liveChat?.input ?? ''
  }, [liveChat?.input])

  useEffect(() => {
    if (activeTab !== 'live' && liveDebate?.running) {
      void handleDebateExit()
    }
  }, [activeTab, liveDebate?.running])

  useEffect(() => {
    setLiveChat((current) =>
      current
        ? {
            ...current,
            speechSupported: speechInput.supported,
            speechListening: speechInput.listening,
            speechError: speechInput.error,
          }
        : current
    )
  }, [speechInput.supported, speechInput.listening, speechInput.error])

  useEffect(() => {
    function handlePageLeave() {
      void clearActiveSession()
    }

    window.addEventListener('pagehide', handlePageLeave)

    return () => {
      window.removeEventListener('pagehide', handlePageLeave)
      handlePageLeave()
    }
  }, [])

  async function loadClones(signal?: AbortSignal) {
    try {
      setCloneLoadError('')
      const [mineResponse, friendResponse, publicResponse] = await Promise.all([
        apiFetch(`${apiBaseUrl}/api/clones?scope=mine`, { signal }),
        apiFetch(`${apiBaseUrl}/api/clones?scope=friend`, { signal }),
        apiFetch(`${apiBaseUrl}/api/clones?scope=public`, { signal }),
      ])
      if (!mineResponse.ok) {
        throw new Error(await readErrorMessage(mineResponse, `내 클론 목록을 불러오지 못했습니다. (${mineResponse.status})`))
      }
      if (!friendResponse.ok) {
        throw new Error(await readErrorMessage(friendResponse, `친구 클론 목록을 불러오지 못했습니다. (${friendResponse.status})`))
      }
      if (!publicResponse.ok) {
        throw new Error(await readErrorMessage(publicResponse, `공개 클론 목록을 불러오지 못했습니다. (${publicResponse.status})`))
      }
      const [mineData, friendData, publicData]: [CloneOption[], CloneOption[], CloneOption[]] = await Promise.all([
        mineResponse.json(),
        friendResponse.json(),
        publicResponse.json(),
      ])
      if (signal?.aborted) {
        return
      }
      setMineClones(mineData)
      setFriendClones(friendData)
      setPublicClones(publicData)
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        return
      }
      setCloneLoadError(error instanceof Error ? error.message : '클론 목록을 불러오는 중 오류가 발생했습니다.')
    }
  }

  async function loadVoices(signal?: AbortSignal) {
    try {
      setVoiceLoadError('')
      const [mineResponse, friendResponse, publicResponse] = await Promise.all([
        apiFetch(`${apiBaseUrl}/api/voices?scope=mine`, { signal }),
        apiFetch(`${apiBaseUrl}/api/voices?scope=friend`, { signal }),
        apiFetch(`${apiBaseUrl}/api/voices?scope=public`, { signal }),
      ])
      if (!mineResponse.ok) {
        throw new Error(await readErrorMessage(mineResponse, `내 목소리 목록을 불러오지 못했습니다. (${mineResponse.status})`))
      }
      if (!friendResponse.ok) {
        throw new Error(await readErrorMessage(friendResponse, `친구 목소리 목록을 불러오지 못했습니다. (${friendResponse.status})`))
      }
      if (!publicResponse.ok) {
        throw new Error(await readErrorMessage(publicResponse, `공개 목소리 목록을 불러오지 못했습니다. (${publicResponse.status})`))
      }
      const [mineData, friendData, publicData]: [VoiceOption[], VoiceOption[], VoiceOption[]] = await Promise.all([
        mineResponse.json(),
        friendResponse.json(),
        publicResponse.json(),
      ])
      if (signal?.aborted) {
        return
      }
      setMineVoices(mineData)
      setFriendVoices(friendData)
      setPublicVoices(publicData)
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        return
      }
      setVoiceLoadError(error instanceof Error ? error.message : '목소리 목록을 불러오는 중 오류가 발생했습니다.')
    }
  }

  async function loadChatHistory(signal?: AbortSignal) {
    try {
      setChatHistoryLoadError('')
      const response = await apiFetch(`${apiBaseUrl}/api/chat/conversations`, { signal })
      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `채팅 기록을 불러오지 못했습니다. (${response.status})`))
      }
      const data: ChatConversationSummary[] = await response.json()
      if (signal?.aborted) {
        return
      }
      setChatHistory(data)
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        return
      }
      setChatHistoryLoadError(error instanceof Error ? error.message : '채팅 기록을 불러오는 중 오류가 발생했습니다.')
    }
  }

  async function loadDebateHistory(signal?: AbortSignal) {
    try {
      setDebateHistoryLoadError('')
      const response = await apiFetch(`${apiBaseUrl}/api/debates`, { signal })
      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `논쟁 기록을 불러오지 못했습니다. (${response.status})`))
      }
      const data: DebateSessionSummary[] = await response.json()
      if (signal?.aborted) {
        return
      }
      setDebateHistory(data)
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        return
      }
      setDebateHistoryLoadError(error instanceof Error ? error.message : '논쟁 기록을 불러오는 중 오류가 발생했습니다.')
    }
  }

  async function loadFriendData(signal?: AbortSignal) {
    try {
      setFriendLoadError('')
      const [friendsResponse, receivedResponse, sentResponse] = await Promise.all([
        apiFetch(`${apiBaseUrl}/api/friends`, { signal }),
        apiFetch(`${apiBaseUrl}/api/friends/requests/received`, { signal }),
        apiFetch(`${apiBaseUrl}/api/friends/requests/sent`, { signal }),
      ])

      if (!friendsResponse.ok) {
        throw new Error(await readErrorMessage(friendsResponse, `친구 목록을 불러오지 못했습니다. (${friendsResponse.status})`))
      }
      if (!receivedResponse.ok) {
        throw new Error(await readErrorMessage(receivedResponse, `받은 요청을 불러오지 못했습니다. (${receivedResponse.status})`))
      }
      if (!sentResponse.ok) {
        throw new Error(await readErrorMessage(sentResponse, `보낸 요청을 불러오지 못했습니다. (${sentResponse.status})`))
      }

      const [friendsData, receivedData, sentData]: [FriendSummary[], FriendRequestSummary[], FriendRequestSummary[]] =
        await Promise.all([friendsResponse.json(), receivedResponse.json(), sentResponse.json()])

      if (signal?.aborted) {
        return
      }

      setFriends(friendsData)
      setReceivedFriendRequests(receivedData)
      setSentFriendRequests(sentData)
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        return
      }
      setFriendLoadError(error instanceof Error ? error.message : '친구 정보를 불러오는 중 오류가 발생했습니다.')
    }
  }

  function resetCreateCloneFlow() {
    setAnswers({})
    setCreateCloneError('')
  }

  function resetVoicePickerState() {
    setSelectedVoiceId('')
    setVoiceAlias('')
    setVoiceFile(null)
    setVoiceRegisterError('')
  }

  function resetDebateSetupState() {
    setDebateTopic('')
    setDebateOpponentId('')
    setDebateVoiceAId('')
    setDebateVoiceBId('')
    setDebateSetupError('')
  }

  function closeModal() {
    setModalState(null)
    resetCreateCloneFlow()
    resetVoicePickerState()
    resetDebateSetupState()
  }

  function openCreateCloneModal() {
    resetCreateCloneFlow()
    setModalState({ type: 'create-clone' })
  }

  function openCloneActions(clone: CloneOption) {
    resetVoicePickerState()
    resetDebateSetupState()
    setModalState({ type: 'clone-actions', clone })
  }

  function openVoicePicker() {
    if (selectedClone) {
      setModalState({ type: 'voice-picker', clone: selectedClone })
    }
  }

  function openDebateSetup() {
    if (selectedClone) {
      setModalState({ type: 'debate-setup', clone: selectedClone })
    }
  }

  function goBackToCloneActions() {
    if (selectedClone) {
      setModalState({ type: 'clone-actions', clone: selectedClone })
    }
  }

  function syncCloneOption(updatedClone: CloneOption) {
    setModalState((current) => {
      if (!current || current.type === 'create-clone' || current.clone.cloneId !== updatedClone.cloneId) {
        return current
      }
      return { ...current, clone: updatedClone }
    })
    setLiveChat((current) =>
      current && current.clone.cloneId === updatedClone.cloneId ? { ...current, clone: updatedClone } : current
    )
    setLiveDebate((current) =>
      current
        ? {
            ...current,
            cloneA: current.cloneA.cloneId === updatedClone.cloneId ? updatedClone : current.cloneA,
            cloneB: current.cloneB.cloneId === updatedClone.cloneId ? updatedClone : current.cloneB,
          }
        : current
    )
  }

  function handleQuestionAnswer(questionIndex: number, answer: string) {
    setAnswers((current) => ({
      ...current,
      [questionIndex]: answer,
    }))
  }

  async function handleCloneCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!canCreateClone) {
      setCreateCloneError('모든 질문에 답변해야 새 클론을 만들 수 있습니다.')
      return
    }

    setCreatingClone(true)
    setCreateCloneError('')

    try {
      const response = await apiFetch(`${apiBaseUrl}/api/system-prompt`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          answers: questions.map((question, index) => ({
            question: question.question,
            answer: answers[index],
          })),
        }),
      })

      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `클론 생성에 실패했습니다. (${response.status})`))
      }

      const data: PromptGenerateResponse = await response.json()
      const nextClone: CloneOption = {
        cloneId: data.promptGenerationLogId,
        createdAt: new Date().toISOString(),
        alias: data.alias,
        shortDescription: data.shortDescription,
        isPublic: false,
        ownerDisplayName: currentUser.displayName,
      }

      setMineClones((current) => [nextClone, ...current.filter((clone) => clone.cloneId !== nextClone.cloneId)])
      closeModal()
    } catch (error) {
      setCreateCloneError(error instanceof Error ? error.message : '클론 생성 중 오류가 발생했습니다.')
    } finally {
      setCreatingClone(false)
    }
  }

  async function handleVoiceRegister(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!voiceFile) {
      setVoiceRegisterError('업로드할 목소리 샘플 파일을 선택해 주세요.')
      return
    }

    setVoiceRegistering(true)
    setVoiceRegisterError('')

    try {
      const formData = new FormData()
      formData.append('sample', voiceFile)
      if (voiceAlias.trim()) {
        formData.append('alias', voiceAlias.trim())
      }

      const response = await apiFetch(`${apiBaseUrl}/api/voices`, {
        method: 'POST',
        body: formData,
      })

      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `목소리 등록에 실패했습니다. (${response.status})`))
      }

      const data: VoiceRegisterResponse = await response.json()
      const nextVoice: VoiceOption = {
        registeredVoiceId: data.registeredVoiceId,
        voiceId: data.voiceId,
        displayName: data.displayName,
        preferredName: data.preferredName,
        originalFilename: data.originalFilename,
        audioMimeType: data.audioMimeType,
        createdAt: new Date().toISOString(),
        isPublic: false,
        ownerDisplayName: currentUser.displayName,
      }

      setMineVoices((current) => [nextVoice, ...current.filter((voice) => voice.registeredVoiceId !== nextVoice.registeredVoiceId)])
      setSelectedVoiceId(String(nextVoice.registeredVoiceId))
      setDebateVoiceAId(String(nextVoice.registeredVoiceId))
      setDebateVoiceBId(String(nextVoice.registeredVoiceId))
      setVoiceAlias('')
      setVoiceFile(null)
    } catch (error) {
      setVoiceRegisterError(error instanceof Error ? error.message : '목소리 등록 중 오류가 발생했습니다.')
    } finally {
      setVoiceRegistering(false)
    }
  }

  async function streamChatMessage(message: string) {
    if (!liveChat) {
      return
    }

    await speechInput.stop()

    const player = new PcmStreamPlayer()
    const abortController = new AbortController()
    chatAbortControllerRef.current = abortController
    chatPlayerRef.current = player

    setLiveChat((current) =>
      current
        ? {
            ...current,
            submitting: true,
            error: '',
            input: '',
            messages: [...current.messages, { role: 'user', content: message }],
          }
        : current
    )

    try {
      const requestBody = {
        promptGenerationLogId: liveChat.clone.cloneId,
        conversationId: liveChat.conversationId,
        message,
        ...(liveChat.voiceId ? { registeredVoiceId: liveChat.voiceId } : {}),
      }

      const response = await apiFetch(`${apiBaseUrl}/api/chat/messages/stream`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody),
        signal: abortController.signal,
      })

      if (!response.ok) {
        const rawMessage = await readErrorMessage(response, `대화 시작에 실패했습니다. (${response.status})`)
        throw new Error(createAccessAwareMessage(rawMessage, '선택한 클론 또는 목소리로 대화를 시작할 수 없습니다.'))
      }

      let pendingVoiceId: string | undefined

      await readNdjsonStream(response, async (streamEvent) => {
        if (streamEvent.type === 'message') {
          setLiveChat((current) =>
            current
              ? {
                  ...current,
                  conversationId: streamEvent.conversationId,
                  messages: [...current.messages, { role: 'assistant', content: streamEvent.assistantMessage }],
                }
              : current
          )
          return
        }

        if (streamEvent.type === 'audio_chunk') {
          player.configure(streamEvent.sampleRate, streamEvent.channels)
          await player.appendBase64Chunk(streamEvent.chunkBase64)
          return
        }

        if (streamEvent.type === 'done') {
          pendingVoiceId = streamEvent.ttsVoiceId || undefined
          await player.finish()
          const wavUrl = player.buildWavUrl()
          setLiveChat((current) => {
            if (!current) {
              return current
            }
            const nextMessages = [...current.messages]
            for (let index = nextMessages.length - 1; index >= 0; index -= 1) {
              if (nextMessages[index].role === 'assistant' && !nextMessages[index].ttsAudioDataUrl) {
                nextMessages[index] = {
                  ...nextMessages[index],
                  ttsAudioDataUrl: wavUrl,
                  ttsVoiceId: pendingVoiceId,
                }
                break
              }
            }
            return {
              ...current,
              messages: nextMessages,
            }
          })
          return
        }

        if (streamEvent.type === 'error') {
          throw new Error(streamEvent.message)
        }
      })
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        return
      }
      setLiveChat((current) =>
        current
          ? {
              ...current,
              error: error instanceof Error ? error.message : '대화 중 오류가 발생했습니다.',
            }
          : current
      )
    } finally {
      await player.dispose()
      if (chatPlayerRef.current === player) {
        chatPlayerRef.current = null
      }
      chatAbortControllerRef.current = null
      void loadChatHistory()
      setLiveChat((current) =>
        current
          ? {
              ...current,
              submitting: false,
            }
          : current
      )
    }
  }

  async function streamDebateTurn(url: string, body?: object) {
    const player = new PcmStreamPlayer()
    const abortController = new AbortController()
    debateAbortControllerRef.current = abortController
    debatePlayerRef.current = player

    try {
      const response = await apiFetch(url, {
        method: 'POST',
        headers: body ? { 'Content-Type': 'application/json' } : undefined,
        body: body ? JSON.stringify(body) : undefined,
        signal: abortController.signal,
      })

      if (!response.ok) {
        const rawMessage = await readErrorMessage(response, `논쟁 스트림 생성에 실패했습니다. (${response.status})`)
        throw new Error(createAccessAwareMessage(rawMessage, '선택한 클론 또는 목소리로 논쟁을 시작할 수 없습니다.'))
      }

      let currentTurnIndex: number | null = null
      let currentVoiceId: string | undefined

      await readNdjsonStream(response, async (streamEvent) => {
        if (streamEvent.type === 'turn') {
          currentTurnIndex = streamEvent.turn.turnIndex
          setLiveDebate((current) =>
            current
              ? {
                  ...current,
                  debateSessionId: streamEvent.debateSessionId,
                  turns: [...current.turns, { ...streamEvent.turn }],
                }
              : current
          )
          return
        }

        if (streamEvent.type === 'audio_chunk') {
          player.configure(streamEvent.sampleRate, streamEvent.channels)
          await player.appendBase64Chunk(streamEvent.chunkBase64)
          return
        }

        if (streamEvent.type === 'done') {
          currentVoiceId = streamEvent.ttsVoiceId || undefined
          await player.finish()
          const wavUrl = player.buildWavUrl()
          setLiveDebate((current) =>
            current
              ? {
                  ...current,
                  turns: current.turns.map((turn) =>
                    turn.turnIndex === currentTurnIndex
                      ? { ...turn, ttsAudioDataUrl: wavUrl, ttsVoiceId: currentVoiceId }
                      : turn
                  ),
                }
              : current
          )
          return
        }

        if (streamEvent.type === 'error') {
          throw new Error(streamEvent.message)
        }
      })
    } finally {
      if (debatePlayerRef.current === player) {
        debatePlayerRef.current = null
      }
      debateAbortControllerRef.current = null
      await player.dispose()
    }
  }

  async function continueDebateLoop() {
    while (debateRunningRef.current && debateSessionIdRef.current) {
      try {
        await streamDebateTurn(`${apiBaseUrl}/api/debates/${debateSessionIdRef.current}/next/stream`)
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') {
          return
        }
        setLiveDebate((current) =>
          current
            ? {
                ...current,
                running: false,
                error: error instanceof Error ? error.message : '다음 논쟁 턴을 불러오지 못했습니다.',
              }
            : current
        )
        return
      }
    }
  }

  async function handleDebateExit() {
    setLiveDebate((current) =>
      current
        ? {
            ...current,
            running: false,
          }
        : current
    )
    debateAbortControllerRef.current?.abort()
    setActiveTab('clones')
  }

  async function toggleCloneVisibility(clone: CloneOption) {
    setCloneVisibilityUpdatingId(clone.cloneId)
    setCloneLoadError('')
    try {
      const response = await apiFetch(`${apiBaseUrl}/api/clones/${clone.cloneId}/visibility`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ isPublic: !clone.isPublic }),
      })
      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `클론 공개 상태를 변경하지 못했습니다. (${response.status})`))
      }

      const nextIsPublic = !clone.isPublic
      const updatedClone = { ...clone, isPublic: nextIsPublic }
      setMineClones((current) =>
        current.map((item) => (item.cloneId === clone.cloneId ? { ...item, isPublic: nextIsPublic } : item))
      )
      if (!nextIsPublic) {
        setPublicClones((current) => current.filter((item) => item.cloneId !== clone.cloneId))
      }
      syncCloneOption(updatedClone)
    } catch (error) {
      setCloneLoadError(error instanceof Error ? error.message : '클론 공개 상태를 변경하지 못했습니다.')
    } finally {
      setCloneVisibilityUpdatingId(null)
    }
  }

  async function toggleVoiceVisibility(voice: VoiceOption) {
    setVoiceVisibilityUpdatingId(voice.registeredVoiceId)
    setVoiceLoadError('')
    try {
      const response = await apiFetch(`${apiBaseUrl}/api/voices/${voice.registeredVoiceId}/visibility`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ isPublic: !voice.isPublic }),
      })
      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `목소리 공개 상태를 변경하지 못했습니다. (${response.status})`))
      }

      const nextIsPublic = !voice.isPublic
      setMineVoices((current) =>
        current.map((item) => (item.registeredVoiceId === voice.registeredVoiceId ? { ...item, isPublic: nextIsPublic } : item))
      )
      if (!nextIsPublic) {
        setPublicVoices((current) => current.filter((item) => item.registeredVoiceId !== voice.registeredVoiceId))
      }
    } catch (error) {
      setVoiceLoadError(error instanceof Error ? error.message : '목소리 공개 상태를 변경하지 못했습니다.')
    } finally {
      setVoiceVisibilityUpdatingId(null)
    }
  }

  async function openChatHistorySession(conversationId: number) {
    try {
      setChatHistoryLoadError('')
      await speechInput.stop()
      await clearActiveSession()

      const response = await apiFetch(`${apiBaseUrl}/api/chat/conversations/${conversationId}`)
      if (!response.ok) {
        const rawMessage = await readErrorMessage(response, `채팅 기록을 불러오지 못했습니다. (${response.status})`)
        throw new Error(createAccessAwareMessage(rawMessage, '선택한 채팅 기록을 다시 불러올 수 없습니다.'))
      }

      const detail: ChatConversationDetail = await response.json()
      const clone =
        clones.find((item) => item.cloneId === detail.cloneId) ?? {
          cloneId: detail.cloneId,
          createdAt: detail.createdAt,
          alias: detail.cloneAlias,
          shortDescription: detail.cloneShortDescription,
          isPublic: false,
          ownerDisplayName: currentUser.displayName,
        }

      setLiveDebate(null)
      setLiveChat({
        clone,
        voiceId: null,
        conversationId: detail.conversationId,
        messages: detail.messages.map((message) => ({
          role: message.role,
          content: message.content,
          createdAt: message.createdAt,
          ttsAudioDataUrl: message.ttsAudioUrl ?? undefined,
          ttsVoiceId: message.ttsVoiceId ?? undefined,
        })),
        input: '',
        submitting: false,
        error: '',
        speechSupported: speechInput.supported,
        speechListening: false,
        speechError: '',
      })
      setActiveTab('live')
    } catch (error) {
      setChatHistoryLoadError(error instanceof Error ? error.message : '채팅 기록을 불러오지 못했습니다.')
    }
  }

  async function openDebateHistorySession(debateSessionId: number) {
    try {
      setDebateHistoryLoadError('')
      await speechInput.stop()
      await clearActiveSession()

      const response = await apiFetch(`${apiBaseUrl}/api/debates/${debateSessionId}`)
      if (!response.ok) {
        const rawMessage = await readErrorMessage(response, `논쟁 기록을 불러오지 못했습니다. (${response.status})`)
        throw new Error(createAccessAwareMessage(rawMessage, '선택한 논쟁 기록을 다시 불러올 수 없습니다.'))
      }

      const detail: DebateSessionDetail = await response.json()
      const cloneA =
        clones.find((item) => item.cloneId === detail.cloneAId) ?? {
          cloneId: detail.cloneAId,
          createdAt: detail.createdAt,
          alias: detail.cloneAAlias,
          shortDescription: detail.cloneAShortDescription,
          isPublic: false,
          ownerDisplayName: currentUser.displayName,
        }
      const cloneB =
        clones.find((item) => item.cloneId === detail.cloneBId) ?? {
          cloneId: detail.cloneBId,
          createdAt: detail.createdAt,
          alias: detail.cloneBAlias,
          shortDescription: detail.cloneBShortDescription,
          isPublic: false,
          ownerDisplayName: currentUser.displayName,
        }

      setLiveChat(null)
      setLiveDebate({
        cloneA,
        cloneB,
        cloneAVoiceId: detail.cloneAVoiceId,
        cloneBVoiceId: detail.cloneBVoiceId,
        topic: detail.topic,
        debateSessionId: detail.debateSessionId,
        turns: detail.turns.map((turn) => ({
          turnIndex: turn.turnIndex,
          speaker: turn.speaker,
          cloneId: turn.cloneId,
          content: turn.content,
          createdAt: turn.createdAt,
          ttsAudioDataUrl: turn.ttsAudioUrl ?? undefined,
          ttsVoiceId: turn.ttsVoiceId ?? undefined,
        })),
        running: false,
        error: '',
      })
      setActiveTab('live')
    } catch (error) {
      setDebateHistoryLoadError(error instanceof Error ? error.message : '논쟁 기록을 불러오지 못했습니다.')
    }
  }

  function handleVoiceFileChange(event: ChangeEvent<HTMLInputElement>) {
    setVoiceFile(event.target.files?.[0] ?? null)
  }

  async function handleStartChat() {
    if (!selectedClone) {
      return
    }
    setLiveDebate(null)
    setLiveChat({
      clone: selectedClone,
      voiceId: selectedVoiceId ? Number(selectedVoiceId) : null,
      conversationId: null,
      messages: [],
      input: '',
      submitting: false,
      error: '',
      speechSupported: speechInput.supported,
      speechListening: false,
      speechError: '',
    })
    setActiveTab('live')
    closeModal()
  }

  async function handleChatSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!liveChat || !liveChat.input.trim() || liveChat.submitting) {
      return
    }
    await streamChatMessage(liveChat.input.trim())
  }

  function handleChatInputChange(value: string) {
    speechInput.clearError()
    setLiveChat((current) =>
      current
        ? {
            ...current,
            input: value,
            speechError: '',
          }
        : current
    )
  }

  function handleChatSpeechToggle() {
    void speechInput.toggle()
  }

  async function handleStartDebate() {
    if (!selectedClone) {
      return
    }
    if (!debateOpponentId || Number(debateOpponentId) === selectedClone.cloneId) {
      setDebateSetupError('논쟁시킬 두 번째 클론을 선택해 주세요.')
      return
    }
    if (!debateVoiceAId || !debateVoiceBId) {
      setDebateSetupError('두 클론의 목소리를 모두 선택해 주세요.')
      return
    }
    if (!debateTopic.trim()) {
      setDebateSetupError('논쟁 주제를 입력해 주세요.')
      return
    }

    const cloneB = clones.find((clone) => clone.cloneId === Number(debateOpponentId))
    if (!cloneB) {
      setDebateSetupError('선택한 상대 클론을 찾을 수 없습니다.')
      return
    }

    const nextState: LiveDebateState = {
      cloneA: selectedClone,
      cloneB,
      cloneAVoiceId: Number(debateVoiceAId),
      cloneBVoiceId: Number(debateVoiceBId),
      topic: debateTopic.trim(),
      debateSessionId: null,
      turns: [],
      running: true,
      error: '',
    }

    await speechInput.stop()
    setLiveChat(null)
    setLiveDebate(nextState)
    setActiveTab('live')
    closeModal()

    try {
      await streamDebateTurn(`${apiBaseUrl}/api/debates/stream`, {
        cloneAId: selectedClone.cloneId,
        cloneBId: cloneB.cloneId,
        cloneAVoiceId: Number(debateVoiceAId),
        cloneBVoiceId: Number(debateVoiceBId),
        topic: debateTopic.trim(),
      })
      void loadDebateHistory()
      if (debateRunningRef.current) {
        void continueDebateLoop()
      }
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        return
      }
      setLiveDebate((current) =>
        current
          ? {
              ...current,
              running: false,
              error: error instanceof Error ? error.message : '논쟁을 시작하지 못했습니다.',
            }
          : current
      )
    }
  }

  async function handleFriendSearchSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const query = friendSearchQuery.trim()
    if (!query) {
      setFriendSearchResults([])
      setFriendSearchError('검색어를 입력해 주세요.')
      return
    }

    setFriendSearchLoading(true)
    setFriendSearchError('')
    try {
      const response = await apiFetch(`${apiBaseUrl}/api/friends/users/search?query=${encodeURIComponent(query)}`)
      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `사용자 검색에 실패했습니다. (${response.status})`))
      }
      const data: UserSearchResponse[] = await response.json()
      setFriendSearchResults(data)
    } catch (error) {
      setFriendSearchError(error instanceof Error ? error.message : '사용자 검색 중 오류가 발생했습니다.')
    } finally {
      setFriendSearchLoading(false)
    }
  }

  async function sendFriendRequest(receiverUserId: number) {
    const actionKey = `send-${receiverUserId}`
    setFriendActionKey(actionKey)
    setFriendSearchError('')
    try {
      const response = await apiFetch(`${apiBaseUrl}/api/friends/requests`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ receiverUserId }),
      })
      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `친구 요청을 보내지 못했습니다. (${response.status})`))
      }
      await loadFriendData()
      setFriendSearchResults((current) => current.filter((user) => user.userId !== receiverUserId))
    } catch (error) {
      setFriendSearchError(error instanceof Error ? error.message : '친구 요청을 보내는 중 오류가 발생했습니다.')
    } finally {
      setFriendActionKey(null)
    }
  }

  async function acceptFriendRequest(requestId: number) {
    const actionKey = `accept-${requestId}`
    setFriendActionKey(actionKey)
    setFriendLoadError('')
    try {
      const response = await apiFetch(`${apiBaseUrl}/api/friends/requests/${requestId}/accept`, { method: 'POST' })
      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `친구 요청을 수락하지 못했습니다. (${response.status})`))
      }
      await loadFriendData()
    } catch (error) {
      setFriendLoadError(error instanceof Error ? error.message : '친구 요청을 수락하는 중 오류가 발생했습니다.')
    } finally {
      setFriendActionKey(null)
    }
  }

  async function rejectFriendRequest(requestId: number) {
    const actionKey = `reject-${requestId}`
    setFriendActionKey(actionKey)
    setFriendLoadError('')
    try {
      const response = await apiFetch(`${apiBaseUrl}/api/friends/requests/${requestId}/reject`, { method: 'POST' })
      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `친구 요청을 거절하지 못했습니다. (${response.status})`))
      }
      await loadFriendData()
    } catch (error) {
      setFriendLoadError(error instanceof Error ? error.message : '친구 요청을 거절하는 중 오류가 발생했습니다.')
    } finally {
      setFriendActionKey(null)
    }
  }

  async function cancelFriendRequest(requestId: number) {
    const actionKey = `cancel-${requestId}`
    setFriendActionKey(actionKey)
    setFriendLoadError('')
    try {
      const response = await apiFetch(`${apiBaseUrl}/api/friends/requests/${requestId}/cancel`, { method: 'POST' })
      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `친구 요청을 취소하지 못했습니다. (${response.status})`))
      }
      await loadFriendData()
    } catch (error) {
      setFriendLoadError(error instanceof Error ? error.message : '친구 요청을 취소하는 중 오류가 발생했습니다.')
    } finally {
      setFriendActionKey(null)
    }
  }

  async function unfriend(friendUserId: number) {
    const actionKey = `unfriend-${friendUserId}`
    setFriendActionKey(actionKey)
    setFriendLoadError('')
    try {
      const response = await apiFetch(`${apiBaseUrl}/api/friends/${friendUserId}`, { method: 'DELETE' })
      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `친구 해제를 완료하지 못했습니다. (${response.status})`))
      }
      await loadFriendData()
    } catch (error) {
      setFriendLoadError(error instanceof Error ? error.message : '친구 해제 중 오류가 발생했습니다.')
    } finally {
      setFriendActionKey(null)
    }
  }

  return {
    activeTab,
    setActiveTab,
    modalState,
    selectedClone,
    questions,
    answers,
    loadingQuestions,
    questionError,
    creatingClone,
    createCloneError,
    clones,
    mineClones,
    friendClones,
    publicClones,
    cloneLoadError,
    voices,
    mineVoices,
    friendVoices,
    publicVoices,
    voiceLoadError,
    cloneVisibilityUpdatingId,
    voiceVisibilityUpdatingId,
    chatHistory,
    chatHistoryLoadError,
    debateHistory,
    debateHistoryLoadError,
    friends,
    receivedFriendRequests,
    sentFriendRequests,
    friendLoadError,
    friendSearchQuery,
    setFriendSearchQuery,
    friendSearchResults,
    friendSearchError,
    friendSearchLoading,
    friendActionKey,
    selectedVoiceId,
    setSelectedVoiceId,
    voiceAlias,
    setVoiceAlias,
    voiceRegistering,
    voiceRegisterError,
    debateTopic,
    setDebateTopic,
    debateOpponentId,
    setDebateOpponentId,
    debateOpponent,
    debateVoiceAId,
    setDebateVoiceAId,
    debateVoiceBId,
    setDebateVoiceBId,
    debateSetupError,
    liveChat,
    liveDebate,
    answeredCount,
    canCreateClone,
    closeModal,
    openCreateCloneModal,
    openCloneActions,
    openVoicePicker,
    openDebateSetup,
    goBackToCloneActions,
    handleQuestionAnswer,
    handleCloneCreate,
    toggleCloneVisibility,
    handleVoiceRegister,
    handleVoiceFileChange,
    toggleVoiceVisibility,
    handleStartChat,
    handleChatSubmit,
    handleChatInputChange,
    handleChatSpeechToggle,
    handleFriendSearchSubmit,
    sendFriendRequest,
    acceptFriendRequest,
    rejectFriendRequest,
    cancelFriendRequest,
    unfriend,
    openChatHistorySession,
    openDebateHistorySession,
    handleStartDebate,
    handleDebateExit,
  }
}
