export type CloneResponse = {
  cloneId: number
  createdAt: string
  alias: string
  shortDescription: string
  isPublic: boolean
  ownerDisplayName: string
}

export type ClonePromptResponse = {
  promptGenerationLogId: number
  alias: string
  shortDescription: string
  systemPrompt: string
}
