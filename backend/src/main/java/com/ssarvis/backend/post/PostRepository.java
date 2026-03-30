package com.ssarvis.backend.post;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("""
            select post
            from Post post
            join fetch post.user owner
            where owner.deletedAt is null
              and (
                    owner.id = :currentUserId
                    or owner.visibility = com.ssarvis.backend.auth.AccountVisibility.PUBLIC
                    or exists (
                        select 1
                        from Follow follow
                        where follow.follower.id = :currentUserId
                          and follow.followee.id = owner.id
                    )
              )
            order by post.createdAt desc
            """)
    List<Post> findVisibleFeedPosts(@Param("currentUserId") Long currentUserId);

    @Query("""
            select post
            from Post post
            join fetch post.user owner
            where owner.id = :profileUserId
              and owner.deletedAt is null
            order by post.createdAt desc
            """)
    List<Post> findAllByOwnerIdOrderByCreatedAtDesc(@Param("profileUserId") Long profileUserId);

    @Query("""
            select post
            from Post post
            join fetch post.user owner
            where post.id = :postId
              and owner.deletedAt is null
            """)
    Optional<Post> findByIdWithOwner(@Param("postId") Long postId);
}
