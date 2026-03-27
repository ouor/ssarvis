package com.ssarvis.backend.friend;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    @Query("""
            select request
            from FriendRequest request
            join fetch request.requester requester
            join fetch request.receiver receiver
            where request.requester.id = :requesterId
              and request.status = com.ssarvis.backend.friend.FriendRequestStatus.PENDING
              and requester.deletedAt is null
              and receiver.deletedAt is null
            order by request.id desc
            """)
    List<FriendRequest> findPendingSentByRequesterId(@Param("requesterId") Long requesterId);

    @Query("""
            select request
            from FriendRequest request
            join fetch request.requester requester
            join fetch request.receiver receiver
            where request.receiver.id = :receiverId
              and request.status = com.ssarvis.backend.friend.FriendRequestStatus.PENDING
              and requester.deletedAt is null
              and receiver.deletedAt is null
            order by request.id desc
            """)
    List<FriendRequest> findPendingReceivedByReceiverId(@Param("receiverId") Long receiverId);

    @Query("""
            select request
            from FriendRequest request
            join fetch request.requester requester
            join fetch request.receiver receiver
            where request.status = com.ssarvis.backend.friend.FriendRequestStatus.ACCEPTED
              and (
                    request.requester.id = :userId
                    or request.receiver.id = :userId
              )
              and requester.deletedAt is null
              and receiver.deletedAt is null
            order by request.respondedAt desc, request.id desc
            """)
    List<FriendRequest> findAcceptedByUserId(@Param("userId") Long userId);

    @Query("""
            select case
                     when request.requester.id = :userId then request.receiver.id
                     else request.requester.id
                   end
            from FriendRequest request
            where request.status = com.ssarvis.backend.friend.FriendRequestStatus.ACCEPTED
              and (
                    request.requester.id = :userId
                    or request.receiver.id = :userId
              )
              and request.requester.deletedAt is null
              and request.receiver.deletedAt is null
            """)
    List<Long> findAcceptedFriendIds(@Param("userId") Long userId);

    @Query("""
            select case when count(request) > 0 then true else false end
            from FriendRequest request
            where request.status = com.ssarvis.backend.friend.FriendRequestStatus.PENDING
              and (
                    (request.requester.id = :firstUserId and request.receiver.id = :secondUserId)
                    or
                    (request.requester.id = :secondUserId and request.receiver.id = :firstUserId)
              )
            """)
    boolean existsPendingBetweenUsers(@Param("firstUserId") Long firstUserId, @Param("secondUserId") Long secondUserId);

    @Query("""
            select request
            from FriendRequest request
            where request.status = com.ssarvis.backend.friend.FriendRequestStatus.PENDING
              and request.requester.id = :requesterId
              and request.receiver.id = :receiverId
            """)
    Optional<FriendRequest> findPendingRequest(@Param("requesterId") Long requesterId, @Param("receiverId") Long receiverId);

    @Query("""
            select case when count(request) > 0 then true else false end
            from FriendRequest request
            where request.status = com.ssarvis.backend.friend.FriendRequestStatus.ACCEPTED
              and (
                    (request.requester.id = :firstUserId and request.receiver.id = :secondUserId)
                    or
                    (request.requester.id = :secondUserId and request.receiver.id = :firstUserId)
              )
            """)
    boolean existsAcceptedBetweenUsers(@Param("firstUserId") Long firstUserId, @Param("secondUserId") Long secondUserId);

    @Query("""
            select request
            from FriendRequest request
            join fetch request.requester requester
            join fetch request.receiver receiver
            where request.id = :requestId
              and request.receiver.id = :receiverId
              and request.status = com.ssarvis.backend.friend.FriendRequestStatus.PENDING
            """)
    Optional<FriendRequest> findPendingByIdAndReceiverId(@Param("requestId") Long requestId, @Param("receiverId") Long receiverId);

    @Query("""
            select request
            from FriendRequest request
            join fetch request.requester requester
            join fetch request.receiver receiver
            where request.id = :requestId
              and request.requester.id = :requesterId
              and request.status = com.ssarvis.backend.friend.FriendRequestStatus.PENDING
            """)
    Optional<FriendRequest> findPendingByIdAndRequesterId(@Param("requestId") Long requestId, @Param("requesterId") Long requesterId);
}
