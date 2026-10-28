package com.mahesh.library.dto;
import jakarta.validation.constraints.*; import java.time.LocalDate; import java.math.BigDecimal; import java.util.*;
public final class ApiDtos { private ApiDtos(){}
 public record AuthorRequest(@NotBlank String name,String bio,String country){}
 public record CategoryRequest(@NotBlank String name,String description){}
 public record BookRequest(@NotBlank String title,@NotBlank String isbn,Integer publishedYear,@Min(0) int totalCopies,@NotNull Long categoryId,Set<Long> authorIds){}
 public record MemberRequest(@NotBlank String fullName,@Email @NotBlank String email,String phone){}
 public record CardRequest(@NotBlank String cardNumber,@NotNull LocalDate issueDate,@NotNull LocalDate expiryDate){}
 public record MemberStatusRequest(@NotNull com.mahesh.library.entity.MemberStatus status){}
 public record BorrowRequest(@NotNull Long bookId,@NotNull Long memberId){}
 public record AuthorResponse(Long id,String name,String bio,String country){}
 public record CategoryResponse(Long id,String name,String description){}
 public record BookResponse(Long id,String title,String isbn,Integer publishedYear,int totalCopies,int availableCopies,Long categoryId,String categoryName,List<String> authors){}
 public record MemberResponse(Long id,String fullName,String email,String phone,LocalDate membershipDate,com.mahesh.library.entity.MemberStatus status,String cardNumber,LocalDate cardExpiry){}
 public record BorrowResponse(Long id,Long bookId,String bookTitle,Long memberId,String memberName,LocalDate borrowDate,LocalDate dueDate,LocalDate returnDate,BigDecimal fineAmount,com.mahesh.library.entity.BorrowStatus status){}
 public record ErrorResponse(int status,String error,String message,LocalDate timestamp){}
 public record TopBook(String title,long borrowCount){}
}