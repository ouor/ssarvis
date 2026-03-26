package com.ssarvis.backend.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    boolean existsByUsername(String username);

    Optional<UserAccount> findByUsernameAndDeletedAtIsNull(String username);

    Optional<UserAccount> findByIdAndDeletedAtIsNull(Long id);
}
