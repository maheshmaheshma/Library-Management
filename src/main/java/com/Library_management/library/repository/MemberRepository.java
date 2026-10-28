package com.mahesh.library.repository;
import com.mahesh.library.entity.Member; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import org.springframework.data.domain.*; import java.math.BigDecimal; import java.util.*;
public interface MemberRepository extends JpaRepository<Member,Long>{
 @Query("select count(br) from BorrowRecord br where br.member.id=:id and br.status in ('BORROWED','OVERDUE')") long countActiveBorrowings(@Param("id") Long id);
 @Query("select coalesce(sum(br.fineAmount),0) from BorrowRecord br where br.member.id=:id") BigDecimal totalFineForMember(@Param("id") Long id);
 @Query("select m from Member m join fetch m.membershipCard where m.id=:id") Optional<Member> findWithCard(@Param("id") Long id);
}