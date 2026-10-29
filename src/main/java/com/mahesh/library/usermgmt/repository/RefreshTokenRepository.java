package com.mahesh.library.usermgmt.repository;

import com.mahesh.library.usermgmt.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String tokenHash);

    /**
     * Atomically consumes a token: returns 1 only for the one caller that flips it from active to revoked.
     * The database row lock makes a concurrent second call see revoked = true and return 0.
     */
    @Modifying
    @Query("update RefreshToken t set t.revoked = true "
            + "where t.token = :token and t.revoked = false and t.expiryDate > :now")
    int consumeIfActive(@Param("token") String tokenHash, @Param("now") Instant now);

    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.user.id = :userId and t.revoked = false")
    int revokeAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from RefreshToken t where t.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
