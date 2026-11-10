package com.mahesh.library.repository;

import com.mahesh.library.entity.Book;
import com.mahesh.library.entity.BorrowRecord;
import com.mahesh.library.entity.Category;
import com.mahesh.library.entity.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class BorrowRecordRepositoryTest {

	@Autowired
	BorrowRecordRepository borrows;

	@Autowired
	BookRepository books;

	@Autowired
	CategoryRepository categories;

	@Autowired
	MemberRepository members;

	@Test
	void findsActiveBorrowingForMemberAndBook() {
		Category category = categories.save(new Category("Loans", "Loan test"));
		Book book = books.save(new Book("Loaned Book", "ISBN-LOANED", 2024, 1, 0, category));
		Member member = members.save(new Member("Reader", "loan-reader@example.com", "555"));
		borrows.save(new BorrowRecord(book, member, LocalDate.now().minusDays(2), LocalDate.now().plusDays(12)));

		assertEquals(1, borrows.findActiveDuplicate(member.getId(), book.getId()).size());
	}

}
