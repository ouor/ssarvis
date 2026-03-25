import type { CloneOption, VoiceOption } from './types'

export function formatCloneName(clone: CloneOption) {
  return `클론 ${clone.cloneId}`
}

export function formatVoiceLabel(voice: VoiceOption) {
  return `${voice.displayName || voice.preferredName} · ${voice.originalFilename}`
}

export function previewTitle(preview: string) {
  return preview.length > 56 ? `${preview.slice(0, 56)}...` : preview
}
