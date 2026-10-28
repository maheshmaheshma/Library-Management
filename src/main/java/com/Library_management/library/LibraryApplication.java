package com.mahesh.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import com.mahesh.library.config.LibraryProperties;

@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties(LibraryProperties.class)
public class LibraryApplication {
  public static void main(String[] args) { SpringApplication.run(LibraryApplication.class, args); }
}