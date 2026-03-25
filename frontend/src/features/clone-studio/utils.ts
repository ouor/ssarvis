import type { CloneOption, VoiceOption } from './types'

export function formatCloneName(clone: CloneOption) {
  return clone.alias?.trim() ? clone.alias : `클론 ${clone.cloneId}`
}
export function formatVoiceLabel(voice: VoiceOption) {
  return `${voice.displayName || voice.preferredName} · ${voice.originalFilename}`
}
