export type ChatConversationSummary = {
  conversationId: number
  cloneId: number
  cloneAlias: string
  createdAt: string
  latestMessagePreview: string
  messageCount: number
}

export type ChatHistoryMessage = {
  role: 'user' | 'assistant' | string
  content: string
  createdAt: string
  ttsAudioUrl: string | null
  ttsVoiceId: string | null
}

export type ChatConversationDetail = {
  conversationId: number
  cloneId: number
  cloneAlias: string
  cloneShortDescription: string
  createdAt: string
  messages: ChatHistoryMessage[]
}

export type ChatReplyResponse = {
  conversationId: number
  assistantMessage: string
  ttsVoiceId: string | null
  ttsAudioMimeType: string | null
  ttsAudioBase64: string | null
}

export type TestChatMessage = {
  id: string
  role: 'user' | 'assistant'
  content: string
  createdAt: string
  audioState?: 'voice' | 'text'
}
