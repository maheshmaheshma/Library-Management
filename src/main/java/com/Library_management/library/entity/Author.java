package com.mahesh.library.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.*;
@Entity @Table(name="authors")
public class Author extends BaseEntity {
 @NotBlank @Column(nullable=false) private String name;
 @Column(length=2000) private String bio;
 private String country;
 @ManyToMany(mappedBy="authors", fetch=FetchType.LAZY) private Set<Book> books=new HashSet<>();
 protected Author(){} public Author(String name,String bio,String country){this.name=name;this.bio=bio;this.country=country;}
 public String getName(){return name;} public void setName(String v){name=v;} public String getBio(){return bio;} public void setBio(String v){bio=v;} public String getCountry(){return country;} public void setCountry(String v){country=v;} public Set<Book> getBooks(){return books;}
}