package com.mahesh.library.repository;

import com.mahesh.library.entity.Book;
import com.mahesh.library.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class BookRepositoryTest {

	@Autowired
	BookRepository books;

	@Autowired
	CategoryRepository categories;

	@Test
	void findsAvailableBooks() {
		Category category = categories.save(new Category("Available", "Books in stock"));
		books.save(new Book("Available Book", "ISBN-AVAILABLE", 2024, 3, 2, category));
		books.save(new Book("Unavailable Book", "ISBN-UNAVAILABLE", 2024, 1, 0, category));

		assertEquals(1, books.findAvailableBooks().size());
		assertEquals("Available Book", books.findAvailableBooks().get(0).getTitle());
	}

}
