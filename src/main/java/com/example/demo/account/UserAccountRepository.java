package com.example.demo.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByIdAndEmail(Long id, String email);

    boolean existsByIdAndEmailAndAuthVersion(Long id, String email, long authVersion);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserAccount u set u.emailVerifiedAt = :now where u.id = :id and u.authVersion = :version and u.emailVerifiedAt is null")
    int markEmailVerified(@Param("id") long id, @Param("version") long version, @Param("now") java.time.Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserAccount u set u.passwordHash = :hash, u.authVersion = u.authVersion + 1 where u.id = :id and u.authVersion = :version")
    int resetPassword(@Param("id") long id, @Param("version") long version, @Param("hash") String passwordHash);
}
