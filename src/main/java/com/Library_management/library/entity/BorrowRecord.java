package com.mahesh.library.entity;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.LocalDate;
@Entity @Table(name="borrow_records",indexes={@Index(columnList="due_date"),@Index(columnList="member_id,status")})
public class BorrowRecord extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="book_id",nullable=false) private Book book;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="member_id",nullable=false) private Member member;
 @Column(nullable=false) private LocalDate borrowDate;
 @Column(nullable=false) private LocalDate dueDate;
 private LocalDate returnDate;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal fineAmount=BigDecimal.ZERO;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private BorrowStatus status=BorrowStatus.BORROWED;
 protected BorrowRecord(){} public BorrowRecord(Book b,Member m,LocalDate borrow,LocalDate due){book=b;member=m;borrowDate=borrow;dueDate=due;status=BorrowStatus.BORROWED;fineAmount=BigDecimal.ZERO;}
 public Book getBook(){return book;} public Member getMember(){return member;} public LocalDate getBorrowDate(){return borrowDate;} public LocalDate getDueDate(){return dueDate;} public LocalDate getReturnDate(){return returnDate;} public BigDecimal getFineAmount(){return fineAmount;} public BorrowStatus getStatus(){return status;} public void markReturned(LocalDate date,BigDecimal fine){returnDate=date;fineAmount=fine;status=BorrowStatus.RETURNED;}
}
