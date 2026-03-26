export type Question = {
  question: string
  choices: string[]
}

export type ApiErrorResponse = {
  message?: string
  details?: string[]
}

export type AuthResponse = {
  userId: number
  username: string
  displayName: string
  accessToken: string
}

export type CurrentUser = {
  userId: number
  username: string
  displayName: string
}

export type CloneOption = {
  cloneId: number
  createdAt: string
  alias: string
  shortDescription: string
  isPublic: boolean
  ownerDisplayName?: string | null
}

export type VoiceOption = {
  registeredVoiceId: number
  voiceId: string
  displayName: string
  preferredName: string
  originalFilename: string
  audioMimeType: string
  createdAt?: string
  isPublic: boolean
  ownerDisplayName?: string | null
}

export type ChatMessage = {
  role: 'user' | 'assistant'
  content: string
  createdAt?: string
  ttsAudioDataUrl?: string
  ttsVoiceId?: string
}

export type DebateTurn = {
  turnIndex: number
  speaker: string
  cloneId: number
  content: string
  createdAt?: string
  ttsAudioDataUrl?: string
  ttsVoiceId?: string
}

export type ChatConversationSummary = {
  conversationId: number
  cloneId: number
  cloneAlias: string
  createdAt: string
  latestMessagePreview: string
  messageCount: number
}

export type ChatConversationDetail = {
  conversationId: number
  cloneId: number
  cloneAlias: string
  cloneShortDescription: string
  createdAt: string
  messages: Array<{
    role: 'user' | 'assistant'
    content: string
    createdAt: string
    ttsAudioUrl?: string | null
    ttsVoiceId?: string | null
  }>
}

export type DebateSessionSummary = {
  debateSessionId: number
  cloneAId: number
  cloneAAlias: string
  cloneBId: number
  cloneBAlias: string
  topic: string
  createdAt: string
  turnCount: number
}

export type DebateSessionDetail = {
  debateSessionId: number
  cloneAId: number
  cloneAAlias: string
  cloneAShortDescription: string
  cloneAVoiceId: number
  cloneBId: number
  cloneBAlias: string
  cloneBShortDescription: string
  cloneBVoiceId: number
  topic: string
  createdAt: string
  turns: Array<{
    turnIndex: number
    speaker: string
    cloneId: number
    content: string
    createdAt: string
    ttsAudioUrl?: string | null
    ttsVoiceId?: string | null
  }>
}

export type PromptGenerateResponse = {
  promptGenerationLogId: number
  alias: string
  shortDescription: string
  systemPrompt: string
}

export type VoiceRegisterResponse = {
  registeredVoiceId: number
  voiceId: string
  displayName: string
  preferredName: string
  originalFilename: string
  audioMimeType: string
}

export type StreamEvent =
  | { type: 'message'; conversationId: number; assistantMessage: string }
  | {
      type: 'turn'
      debateSessionId: number
      topic: string
      turn: { turnIndex: number; speaker: string; cloneId: number; content: string }
    }
  | { type: 'audio_chunk'; audioFormat: string; sampleRate: number; channels: number; chunkBase64: string }
  | { type: 'done'; conversationId?: number; debateSessionId?: number; turnIndex?: number; ttsVoiceId?: string; hasAudio?: boolean }
  | { type: 'error'; message: string }

export type ModalState =
  | { type: 'create-clone' }
  | { type: 'clone-actions'; clone: CloneOption }
  | { type: 'voice-picker'; clone: CloneOption }
  | { type: 'debate-setup'; clone: CloneOption }
  | null

export type LiveChatState = {
  clone: CloneOption
  voiceId: number | null
  conversationId: number | null
  messages: ChatMessage[]
  input: string
  submitting: boolean
  error: string
  speechSupported: boolean
  speechListening: boolean
  speechError: string
}

export type LiveDebateState = {
  cloneA: CloneOption
  cloneB: CloneOption
  cloneAVoiceId: number
  cloneBVoiceId: number
  topic: string
  debateSessionId: number | null
  turns: DebateTurn[]
  running: boolean
  error: string
}
