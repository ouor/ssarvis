import { toFeedPostViewModel } from './mappers'

describe('feed mappers', () => {
  it('maps backend post responses into UI-friendly feed items', () => {
    const result = toFeedPostViewModel({
      postId: 21,
      ownerUserId: 2,
      ownerUsername: 'miso',
      ownerDisplayName: '미소',
      ownerVisibility: 'PUBLIC',
      content: '피드 게시물',
      createdAt: '2026-03-28T00:00:00Z',
    })

    expect(result.id).toBe(21)
    expect(result.author.displayName).toBe('미소')
    expect(result.content).toBe('피드 게시물')
  })
})
