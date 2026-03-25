export type Question = {
  question: string
  choices: string[]
}

export type ApiErrorResponse = {
  message?: string
  details?: string[]
}

export type CloneOption = {
  cloneId: number
  createdAt: string
  preview: string
}

export type VoiceOption = {
  registeredVoiceId: number
  voiceId: string
  preferredName: string
  originalFilename: string
  audioMimeType: string
  createdAt?: string
}

export type ChatMessage = {
  role: 'user' | 'assistant'
  content: string
  ttsAudioDataUrl?: string
  ttsVoiceId?: string
}

export type DebateTurn = {
  turnIndex: number
  speaker: string
  cloneId: number
  content: string
  ttsAudioDataUrl?: string
  ttsVoiceId?: string
}

export type PromptGenerateResponse = {
  promptGenerationLogId: number
  systemPrompt: string
}

export type VoiceRegisterResponse = {
  registeredVoiceId: number
  voiceId: string
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
  stopping: boolean
  error: string
}
