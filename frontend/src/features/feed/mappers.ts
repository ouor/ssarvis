import type { FeedPostResponse, FeedPostViewModel } from './types'

function formatPostedAt(isoDate: string) {
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

export function toFeedPostViewModel(
  response: FeedPostResponse,
): FeedPostViewModel {
  return {
    id: response.postId,
    author: {
      userId: response.ownerUserId,
      username: response.ownerUsername,
      displayName: response.ownerDisplayName,
      visibility: response.ownerVisibility,
    },
    postedAt: formatPostedAt(response.createdAt),
    content: response.content,
  }
}
