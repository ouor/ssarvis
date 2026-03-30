import {
  toDmMessageViewModel,
  toDmThreadDetailViewModel,
  toDmThreadViewModel,
} from './mappers'

describe('dm mappers', () => {
  it('maps thread previews into UI models', () => {
    const result = toDmThreadViewModel({
      threadId: 10,
      otherParticipant: {
        userId: 2,
        username: 'miso',
        displayName: '미소',
        visibility: 'PUBLIC',
      },
      createdAt: '2026-03-28T00:00:00Z',
      latestMessagePreview: '최근 메시지',
      latestMessageCreatedAt: '2026-03-28T00:01:00Z',
    })

    expect(result.id).toBe(10)
    expect(result.preview).toBe('최근 메시지')
    expect(result.user.displayName).toBe('미소')
  })

  it('maps thread detail responses into conversation models', () => {
    const result = toDmThreadDetailViewModel({
      threadId: 10,
      otherParticipant: {
        userId: 2,
        username: 'miso',
        displayName: '미소',
        visibility: 'PUBLIC',
      },
      createdAt: '2026-03-28T00:00:00Z',
      hiddenBundleMessageIds: [],
      messages: [
        {
          messageId: 30,
          senderUserId: 1,
          senderDisplayName: '하루',
          aiGenerated: false,
          bundleRootMessageId: null,
          format: 'TEXT',
          audioMimeType: null,
          audioBase64: null,
          content: '안녕!',
          createdAt: '2026-03-28T00:01:00Z',
        },
      ],
    })

    expect(result.messages).toHaveLength(1)
    expect(result.messages[0].kind).toBe('text')
    expect(result.user.username).toBe('miso')
  })

  it('marks voice responses correctly', () => {
    const result = toDmMessageViewModel({
      messageId: 31,
      senderUserId: 2,
      senderDisplayName: '미소',
      aiGenerated: true,
      bundleRootMessageId: 29,
      format: 'VOICE',
      audioMimeType: 'audio/webm',
      audioBase64: 'AQID',
      content: '음성 메시지',
      createdAt: '2026-03-28T00:01:10Z',
    })

    expect(result.kind).toBe('voice')
    expect(result.isAiGenerated).toBe(true)
    expect(result.duration).toBeDefined()
  })
})
