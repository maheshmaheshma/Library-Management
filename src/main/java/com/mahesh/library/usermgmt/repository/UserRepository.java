package com.mahesh.library.usermgmt.repository;

import com.mahesh.library.usermgmt.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Username and email are stored lower-case; callers pass lower-case values. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByMemberId(Long memberId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("select u from User u where u.username like concat('%', :q, '%') "
            + "or u.email like concat('%', :q, '%') or lower(u.fullName) like concat('%', :q, '%')")
    Page<User> search(@Param("q") String lowerCaseQuery, Pageable pageable);

    /** Admins other than the given user who can still sign in (enabled and not locked by an admin). */
    @Query("select count(u) from User u join u.roles r where r.name = 'ROLE_ADMIN' "
            + "and u.enabled = true and u.accountLocked = false and u.id <> :id")
    long countOtherActiveAdmins(@Param("id") Long id);
}
