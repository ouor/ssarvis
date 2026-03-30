import type { Visibility } from '../../lib/types/common'

export type DmParticipant = {
  userId: number
  username: string
  displayName: string
  visibility: Visibility
}

export type DmThreadResponse = {
  threadId: number
  otherParticipant: DmParticipant
  createdAt: string
  latestMessagePreview?: string | null
  latestMessageCreatedAt?: string | null
}

export type DmMessageResponse = {
  messageId: number
  senderUserId: number
  senderDisplayName: string
  aiGenerated: boolean
  bundleRootMessageId: number | null
  format: 'TEXT' | 'VOICE'
  audioMimeType: string | null
  audioBase64: string | null
  content: string
  createdAt: string
}

export type DmThreadDetailResponse = {
  threadId: number
  otherParticipant: DmParticipant
  createdAt: string
  messages: DmMessageResponse[]
  hiddenBundleMessageIds: number[]
}

export type DmThreadViewModel = {
  id: number
  user: DmParticipant
  preview: string
  updatedAt: string
}

export type DmMessageViewModel = {
  id: number
  authorId: number
  authorName: string
  kind: 'text' | 'voice'
  content: string
  createdAt: string
  duration?: string
  isAiGenerated: boolean
}

export type DmThreadDetailViewModel = {
  id: number
  user: DmParticipant
  createdAt: string
  messages: DmMessageViewModel[]
}
