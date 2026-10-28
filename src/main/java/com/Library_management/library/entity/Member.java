package com.mahesh.library.entity;
import jakarta.persistence.*; import jakarta.validation.constraints.*; import java.time.LocalDate; import java.util.*;
@Entity @Table(name="members",uniqueConstraints=@UniqueConstraint(columnNames="email"))
public class Member extends BaseEntity {
 @NotBlank @Column(nullable=false) private String fullName;
 @Email @NotBlank @Column(nullable=false,unique=true) private String email;
 private String phone;
 @Column(nullable=false) private LocalDate membershipDate=LocalDate.now();
 @Enumerated(EnumType.STRING) @Column(nullable=false) private MemberStatus status=MemberStatus.ACTIVE;
 @OneToMany(mappedBy="member",fetch=FetchType.LAZY) private List<BorrowRecord> borrowRecords=new ArrayList<>();
 @OneToOne(mappedBy="member",fetch=FetchType.LAZY,cascade=CascadeType.ALL,orphanRemoval=true) private MembershipCard membershipCard;
 protected Member(){} public Member(String name,String email,String phone){this.fullName=name;this.email=email;this.phone=phone;this.membershipDate=LocalDate.now();}
 public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;} public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getPhone(){return phone;} public void setPhone(String v){phone=v;} public LocalDate getMembershipDate(){return membershipDate;} public MemberStatus getStatus(){return status;} public void setStatus(MemberStatus v){status=v;} public List<BorrowRecord> getBorrowRecords(){return borrowRecords;} public MembershipCard getMembershipCard(){return membershipCard;} public void setMembershipCard(MembershipCard v){membershipCard=v;}
}