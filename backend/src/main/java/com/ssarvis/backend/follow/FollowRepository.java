package com.ssarvis.backend.follow;

import com.ssarvis.backend.auth.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerIdAndFolloweeId(Long followerUserId, Long followeeUserId);

    Optional<Follow> findByFollowerIdAndFolloweeId(Long followerUserId, Long followeeUserId);

    @Query("""
            select follow
            from Follow follow
            join fetch follow.followee followee
            where follow.follower.id = :followerUserId
              and followee.deletedAt is null
            order by followee.displayName asc, followee.username asc
            """)
    List<Follow> findFollowingByFollowerId(@Param("followerUserId") Long followerUserId);

    @Query("""
            select user
            from UserAccount user
            where user.deletedAt is null
              and user.id <> :currentUserId
              and (
                    lower(user.username) like lower(concat('%', :query, '%'))
                    or lower(user.displayName) like lower(concat('%', :query, '%'))
              )
              and (
                    user.visibility = com.ssarvis.backend.auth.AccountVisibility.PUBLIC
                    or exists (
                        select 1
                        from Follow follow
                        where follow.follower.id = :currentUserId
                          and follow.followee.id = user.id
                    )
              )
            order by user.displayName asc, user.username asc
            """)
    List<UserAccount> searchDiscoverableUsers(
            @Param("currentUserId") Long currentUserId,
            @Param("query") String query,
            Pageable pageable
    );
}
