package com.ssarvis.backend.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    boolean existsByUsername(String username);

    Optional<UserAccount> findByUsernameAndDeletedAtIsNull(String username);

    Optional<UserAccount> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
            select user
            from UserAccount user
            where user.deletedAt is null
              and user.id <> :currentUserId
              and (
                    lower(user.username) like lower(concat('%', :query, '%'))
                    or lower(user.displayName) like lower(concat('%', :query, '%'))
              )
            order by user.displayName asc, user.username asc
            """)
    List<UserAccount> searchActiveUsers(
            @Param("currentUserId") Long currentUserId,
            @Param("query") String query,
            Pageable pageable
    );
}
