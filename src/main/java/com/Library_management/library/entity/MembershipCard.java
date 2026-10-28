package com.mahesh.library.entity;
import jakarta.persistence.*; import jakarta.validation.constraints.NotBlank; import java.time.LocalDate;
@Entity @Table(name="membership_cards",uniqueConstraints=@UniqueConstraint(columnNames="card_number"))
public class MembershipCard extends BaseEntity {
 @NotBlank @Column(name="card_number",nullable=false,unique=true) private String cardNumber;
 private LocalDate issueDate; private LocalDate expiryDate;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="member_id",nullable=false,unique=true) private Member member;
 protected MembershipCard(){} public MembershipCard(String n,LocalDate issue,LocalDate expiry,Member m){cardNumber=n;issueDate=issue;expiryDate=expiry;member=m;}
 public String getCardNumber(){return cardNumber;} public LocalDate getIssueDate(){return issueDate;} public LocalDate getExpiryDate(){return expiryDate;} public Member getMember(){return member;} public boolean isValidOn(LocalDate date){return expiryDate!=null&&!expiryDate.isBefore(date);}
}