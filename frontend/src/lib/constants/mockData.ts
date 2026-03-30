import type { AppUser, Visibility } from '../types/common'

export type FeedPost = {
  id: number
  author: AppUser
  postedAt: string
  content: string
  mood?: string
}

export type ThreadPreview = {
  id: number
  user: AppUser
  preview: string
  updatedAt: string
  unread: boolean
}

export type MessageItem = {
  id: number
  authorId: number
  kind: 'text' | 'voice'
  content: string
  duration?: string
}

type StudioStatus = {
  cloneSummary: string
  cloneVisibility: Visibility
  voiceReady: boolean
  voiceVisibility: Visibility
}

export const me: AppUser = {
  userId: 1,
  username: 'haru',
  displayName: '하루',
  visibility: 'PUBLIC',
}

export const suggestedUsers: AppUser[] = [
  { userId: 2, username: 'nabi', displayName: '나비', visibility: 'PUBLIC' },
  { userId: 3, username: 'jun', displayName: '준', visibility: 'PRIVATE' },
  { userId: 4, username: 'mira', displayName: '미라', visibility: 'PUBLIC' },
]

export const feedPosts: FeedPost[] = [
  {
    id: 1,
    author: suggestedUsers[0],
    postedAt: '방금 전',
    content: '오늘은 음성 메시지 톤을 조금 더 부드럽게 다듬어봤어요. 대화가 한결 사람답게 들리네요.',
    mood: 'Voice',
  },
  {
    id: 2,
    author: suggestedUsers[2],
    postedAt: '12분 전',
    content: '클론 프롬프트를 공개로 바꿔두었어요. 반응이 어떻게 달라질지 궁금합니다.',
    mood: 'Clone',
  },
  {
    id: 3,
    author: me,
    postedAt: '1시간 전',
    content: '피드와 DM, 그리고 스튜디오가 자연스럽게 이어지는 UI를 상상 중입니다.',
    mood: 'Note',
  },
]

export const threadPreviews: ThreadPreview[] = [
  {
    id: 11,
    user: suggestedUsers[0],
    preview: '음성 카드 느낌이 정말 좋아졌어요.',
    updatedAt: '2분 전',
    unread: true,
  },
  {
    id: 12,
    user: suggestedUsers[1],
    preview: '비공개 계정 정책도 화면에 잘 녹이면 좋겠어요.',
    updatedAt: '18분 전',
    unread: false,
  },
]

export const activeMessages: MessageItem[] = [
  {
    id: 1,
    authorId: 2,
    kind: 'text',
    content: '새로운 DM 화면에서 음성 메시지가 더 돋보이게 보였으면 좋겠어요.',
  },
  {
    id: 2,
    authorId: 1,
    kind: 'voice',
    content: 'wave',
    duration: '0:18',
  },
  {
    id: 3,
    authorId: 1,
    kind: 'text',
    content: '좋아요. 보이스 카드가 일반 버블보다 조금 더 미래적으로 보이게 해볼게요.',
  },
]

export const studioStatus: StudioStatus = {
  cloneSummary:
    '일상적인 말투와 부드러운 공감 표현을 유지하도록 프롬프트가 구성되어 있습니다.',
  cloneVisibility: 'PUBLIC',
  voiceReady: true,
  voiceVisibility: 'PRIVATE',
}
