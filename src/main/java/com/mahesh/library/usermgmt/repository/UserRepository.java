package com.mahesh.library.usermgmt.repository;

import com.mahesh.library.usermgmt.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Username and email are stored lower-case; callers pass lower-case values. */
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	Optional<User> findByEmail(String email);

	Optional<User> findByMemberId(Long memberId);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	boolean existsByMemberId(Long memberId);

	/**
	 * Atomically records a failed login and applies the temporary lock at the threshold.
	 */
	@Modifying(clearAutomatically = true)
	@Query("update User u set u.failedLoginAttempts = u.failedLoginAttempts + 1, "
			+ "u.accountLocked = case when u.failedLoginAttempts + 1 >= :maxAttempts then true else u.accountLocked end, "
			+ "u.lockedUntil = case when u.failedLoginAttempts + 1 >= :maxAttempts then :lockedUntil else u.lockedUntil end "
			+ "where u.id = :id and u.enabled = true "
			+ "and (u.accountLocked = false or (u.lockedUntil is not null and u.lockedUntil <= :now))")
	int recordFailedLogin(@Param("id") Long id, @Param("maxAttempts") int maxAttempts,
			@Param("lockedUntil") java.time.Instant lockedUntil, @Param("now") java.time.Instant now);

	@Modifying
	@Query("update User u set u.failedLoginAttempts = 0 where u.id = :id")
	int resetFailedLoginAttempts(@Param("id") Long id);

	@Query("select u from User u where u.username like concat('%', :q, '%') "
			+ "or u.email like concat('%', :q, '%') or lower(u.fullName) like concat('%', :q, '%')")
	Page<User> search(@Param("q") String lowerCaseQuery, Pageable pageable);

	/**
	 * Admins other than the given user who can still sign in (enabled and not locked by
	 * an admin).
	 */
	@Query("select count(u) from User u join u.roles r where r.name = 'ROLE_ADMIN' "
			+ "and u.enabled = true and u.accountLocked = false and u.id <> :id")
	long countOtherActiveAdmins(@Param("id") Long id);

}
