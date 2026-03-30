import {
  activeMessages,
  feedPosts,
  me,
  studioStatus,
  suggestedUsers,
  threadPreviews,
} from '../constants/mockData'
import type { ChatConversationSummary, TestChatMessage } from '../../features/chat/types'
import type { CloneResponse } from '../../features/clone-studio/types'
import type {
  DmThreadDetailViewModel,
  DmThreadViewModel,
} from '../../features/dm/types'
import type { FeedPostViewModel } from '../../features/feed/types'
import type {
  AutoReplySettings,
  ProfileSummary,
} from '../../features/profile-settings/types'
import type { VoiceResponse } from '../../features/voice-studio/types'
import type { AppUser } from '../types/common'

export function getDemoUser(): AppUser {
  return me
}

export function getDemoSuggestedUsers() {
  return suggestedUsers
}

export function getDemoFeedPosts(): FeedPostViewModel[] {
  return feedPosts.map((post) => ({
    id: post.id,
    author: post.author,
    postedAt: post.postedAt,
    content: post.content,
  }))
}

export function getDemoProfilePosts(userId: number) {
  return getDemoFeedPosts().filter((post) => post.author.userId === userId)
}

export function getDemoPeopleResults(): ProfileSummary[] {
  return suggestedUsers.map((user, index) => ({
    ...user,
    me: false,
    following: index === 0,
  }))
}

export function getDemoProfileSummary(): ProfileSummary {
  return {
    userId: me.userId,
    username: me.username,
    displayName: me.displayName,
    visibility: me.visibility,
    me: true,
    following: false,
  }
}

export function getDemoAutoReplySettings(): AutoReplySettings {
  return {
    mode: 'AWAY',
    lastActivityAt: '데모 세션',
  }
}

export function getDemoThreadPreviews(): DmThreadViewModel[] {
  return threadPreviews.map((thread) => ({
    id: thread.id,
    user: thread.user,
    preview: thread.preview,
    updatedAt: thread.updatedAt,
  }))
}

export function getDemoActiveThread(): DmThreadDetailViewModel {
  return {
    id: threadPreviews[0]?.id ?? 0,
    user: suggestedUsers[0],
    createdAt: '방금 전',
    messages: activeMessages.map((message) => ({
      id: message.id,
      authorId: message.authorId,
      authorName:
        message.authorId === me.userId ? me.displayName : suggestedUsers[0].displayName,
      kind: message.kind,
      content: message.content,
      createdAt: '방금 전',
      duration: message.duration,
      isAiGenerated: false,
    })),
  }
}

export function getDemoStudioClone(): CloneResponse {
  return {
    cloneId: 1,
    createdAt: new Date().toISOString(),
    alias: '차분한 조력자',
    shortDescription: studioStatus.cloneSummary,
    isPublic: studioStatus.cloneVisibility === 'PUBLIC',
    ownerDisplayName: '하루',
  }
}

export function getDemoStudioVoice(): VoiceResponse {
  return {
    registeredVoiceId: 1,
    voiceId: 'demo-voice',
    displayName: '차분한 민지',
    preferredName: 'samplevoice',
    originalFilename: 'voice.mp3',
    audioMimeType: 'audio/mpeg',
    createdAt: new Date().toISOString(),
    isPublic: studioStatus.voiceVisibility === 'PUBLIC',
    ownerDisplayName: '하루',
  }
}

export function getDemoStudioConversationSummaries(): ChatConversationSummary[] {
  return []
}

export function getDemoStudioChatMessages(
  alias: string,
  hasVoiceReply: boolean,
  message: string,
): TestChatMessage[] {
  const timestamp = new Intl.DateTimeFormat('ko-KR', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date())

  return [
    {
      id: `user-${Date.now()}`,
      role: 'user',
      content: message,
      createdAt: timestamp,
    },
    {
      id: `assistant-${Date.now() + 1}`,
      role: 'assistant',
      content: `${alias} 톤으로 응답을 테스트하고 있습니다: ${message}`,
      createdAt: timestamp,
      audioState: hasVoiceReply ? 'voice' : 'text',
    },
  ]
}

export function getDemoStudioStatus() {
  return studioStatus
}
