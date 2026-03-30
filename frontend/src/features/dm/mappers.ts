import type {
  DmMessageResponse,
  DmMessageViewModel,
  DmThreadDetailResponse,
  DmThreadDetailViewModel,
  DmThreadResponse,
  DmThreadViewModel,
} from './types'

function formatDateTime(isoDate: string) {
  const date = new Date(isoDate)

  if (Number.isNaN(date.getTime())) {
    return isoDate
  }

  return new Intl.DateTimeFormat('ko-KR', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

function estimateVoiceDuration(audioBase64: string | null) {
  if (!audioBase64) {
    return '0:00'
  }

  const estimatedSeconds = Math.max(1, Math.round(audioBase64.length / 16000))
  const minutes = Math.floor(estimatedSeconds / 60)
  const seconds = estimatedSeconds % 60

  return `${minutes}:${String(seconds).padStart(2, '0')}`
}

export function toDmThreadViewModel(
  response: DmThreadResponse,
): DmThreadViewModel {
  return {
    id: response.threadId,
    user: response.otherParticipant,
    preview: response.latestMessagePreview ?? '아직 메시지가 없습니다.',
    updatedAt: formatDateTime(response.latestMessageCreatedAt ?? response.createdAt),
  }
}

export function toDmMessageViewModel(
  response: DmMessageResponse,
): DmMessageViewModel {
  return {
    id: response.messageId,
    authorId: response.senderUserId,
    authorName: response.senderDisplayName,
    kind: response.format === 'VOICE' ? 'voice' : 'text',
    content: response.content,
    createdAt: formatDateTime(response.createdAt),
    duration:
      response.format === 'VOICE'
        ? estimateVoiceDuration(response.audioBase64)
        : undefined,
    isAiGenerated: response.aiGenerated,
  }
}

export function toDmThreadDetailViewModel(
  response: DmThreadDetailResponse,
): DmThreadDetailViewModel {
  return {
    id: response.threadId,
    user: response.otherParticipant,
    createdAt: formatDateTime(response.createdAt),
    messages: response.messages.map(toDmMessageViewModel),
  }
}
