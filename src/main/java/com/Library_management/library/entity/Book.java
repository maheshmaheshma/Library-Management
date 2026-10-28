package com.mahesh.library.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.*;
@Entity @Table(name="books", uniqueConstraints=@UniqueConstraint(columnNames="isbn"))
public class Book extends BaseEntity {
 @NotBlank @Column(nullable=false) private String title;
 @NotBlank @Column(nullable=false,unique=true) private String isbn;
 private Integer publishedYear;
 @Min(0) @Column(nullable=false) private int totalCopies;
 @Min(0) @Column(nullable=false) private int availableCopies;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="category_id",nullable=false) private Category category;
 @ManyToMany(fetch=FetchType.LAZY) @JoinTable(name="book_authors",joinColumns=@JoinColumn(name="book_id"),inverseJoinColumns=@JoinColumn(name="author_id")) private Set<Author> authors=new HashSet<>();
 @Version private Long version;
 protected Book(){} public Book(String title,String isbn,Integer year,int total,int available,Category category){this.title=title;this.isbn=isbn;this.publishedYear=year;this.totalCopies=total;this.availableCopies=available;this.category=category;}
 public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getIsbn(){return isbn;} public void setIsbn(String v){isbn=v;} public Integer getPublishedYear(){return publishedYear;} public void setPublishedYear(Integer v){publishedYear=v;} public int getTotalCopies(){return totalCopies;} public void setTotalCopies(int v){if(v<totalCopies- (totalCopies-availableCopies)) throw new IllegalArgumentException("totalCopies cannot be below currently borrowed copies"); totalCopies=v; availableCopies=Math.min(availableCopies,v);} public int getAvailableCopies(){return availableCopies;} public void setAvailableCopies(int v){availableCopies=v;} public Category getCategory(){return category;} public void setCategory(Category v){category=v;} public Set<Author> getAuthors(){return authors;} public Long getVersion(){return version;} public int getBorrowedCopies(){return totalCopies-availableCopies;}
}