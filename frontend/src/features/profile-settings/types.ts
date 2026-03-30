import type { Visibility } from '../../lib/types/common'

export type ProfileSummary = {
  userId: number
  username: string
  displayName: string
  visibility: Visibility
  me: boolean
  following: boolean
}

export type AutoReplyMode = 'ALWAYS' | 'AWAY' | 'OFF'

export type AutoReplySettings = {
  mode: AutoReplyMode
  lastActivityAt: string
}
