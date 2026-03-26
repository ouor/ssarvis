import type { CloneOption, VoiceOption } from './types'

export function formatCloneName(clone: CloneOption) {
  return clone.alias?.trim() ? clone.alias : `클론 ${clone.cloneId}`
}

export function formatVoiceLabel(voice: VoiceOption) {
  return `${voice.displayName || voice.preferredName} · ${voice.originalFilename}`
}

export function formatVisibilityLabel(isPublic: boolean) {
  return isPublic ? '공개' : '비공개'
}
