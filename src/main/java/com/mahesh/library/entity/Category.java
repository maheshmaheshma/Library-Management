package com.mahesh.library.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
@Entity @Table(name="categories", uniqueConstraints=@UniqueConstraint(columnNames="name"))
public class Category extends BaseEntity {
 @NotBlank @Column(nullable=false,unique=true) private String name;
 private String description;
 protected Category(){} public Category(String name,String description){this.name=name;this.description=description;}
 public String getName(){return name;} public void setName(String v){name=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;}
}