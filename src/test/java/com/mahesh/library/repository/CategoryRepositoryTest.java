package com.mahesh.library.repository;

import com.mahesh.library.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class CategoryRepositoryTest {

	@Autowired
	CategoryRepository categories;

	@Test
	void findsCategoryIgnoringCase() {
		categories.save(new Category("Science Fiction", "Speculative fiction"));

		assertEquals("Science Fiction", categories.findByNameIgnoreCase("science fiction").orElseThrow().getName());
	}

}
