import { apiRequest } from '../../lib/api/client'
import type {
  ChatConversationDetail,
  ChatConversationSummary,
  ChatReplyResponse,
} from './types'

type SendChatMessageInput = {
  promptGenerationLogId?: number | null
  conversationId?: number | null
  registeredVoiceId?: number | null
  message: string
}

export function listChatConversations(token: string) {
  return apiRequest<ChatConversationSummary[]>('/api/chat/conversations', {
    token,
  })
}

export function getChatConversation(token: string, conversationId: number) {
  return apiRequest<ChatConversationDetail>(
    `/api/chat/conversations/${conversationId}`,
    { token },
  )
}

export function sendChatMessage(token: string, input: SendChatMessageInput) {
  return apiRequest<ChatReplyResponse>('/api/chat/messages', {
    method: 'POST',
    token,
    body: JSON.stringify(input),
  })
}
