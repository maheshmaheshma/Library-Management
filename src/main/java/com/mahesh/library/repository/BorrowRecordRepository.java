package com.mahesh.library.repository;
import com.mahesh.library.entity.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import org.springframework.data.domain.*; import java.time.LocalDate; import java.util.*;
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord,Long>{
 @Query("select br from BorrowRecord br join fetch br.member m join fetch br.book b join fetch b.category where br.status in ('BORROWED','OVERDUE') and br.dueDate < :today") List<BorrowRecord> findOverdueWithDetails(@Param("today") LocalDate today);
 @Query("select br from BorrowRecord br join fetch br.book b join fetch b.category where br.member.id=:memberId and br.status in ('BORROWED','OVERDUE')") List<BorrowRecord> findActiveByMember(@Param("memberId") Long memberId);
 @Query("select br from BorrowRecord br join fetch br.book b join fetch br.member m where br.member.id=:id order by br.borrowDate desc") List<BorrowRecord> findHistoryByMember(@Param("id") Long id);
 @Query("select br from BorrowRecord br where br.member.id=:memberId and br.book.id=:bookId and br.status in ('BORROWED','OVERDUE')") List<BorrowRecord> findActiveDuplicate(@Param("memberId") Long memberId,@Param("bookId") Long bookId);
 @Query("select br.member.id from BorrowRecord br where br.id=:id") Optional<Long> findMemberIdById(@Param("id") Long id);
 @Modifying @Query("update BorrowRecord br set br.status='OVERDUE' where br.status='BORROWED' and br.dueDate < :today") int markOverdue(@Param("today") LocalDate today);
}