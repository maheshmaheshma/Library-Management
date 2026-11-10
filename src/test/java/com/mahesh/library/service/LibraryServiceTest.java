package com.mahesh.library.service;

import com.mahesh.library.config.LibraryProperties;
import com.mahesh.library.dto.ApiDtos.AuthorRequest;
import com.mahesh.library.dto.ApiDtos.BookRequest;
import com.mahesh.library.dto.ApiDtos.CategoryRequest;
import com.mahesh.library.dto.ApiDtos.MemberRequest;
import com.mahesh.library.entity.Author;
import com.mahesh.library.entity.Book;
import com.mahesh.library.entity.Category;
import com.mahesh.library.entity.Member;
import com.mahesh.library.entity.MemberStatus;
import com.mahesh.library.exception.ConflictException;
import com.mahesh.library.exception.NotFoundException;
import com.mahesh.library.repository.AuthorRepository;
import com.mahesh.library.repository.BookRepository;
import com.mahesh.library.repository.BorrowRecordRepository;
import com.mahesh.library.repository.CategoryRepository;
import com.mahesh.library.repository.MemberRepository;
import com.mahesh.library.repository.MembershipCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

	@Mock
	AuthorRepository authors;

	@Mock
	CategoryRepository categories;

	@Mock
	BookRepository books;

	@Mock
	MemberRepository members;

	@Mock
	MembershipCardRepository cards;

	@Mock
	BorrowRecordRepository borrows;

	@Mock
	LibraryProperties properties;

	private LibraryService service;

	@BeforeEach
	void setUp() {
		service = new LibraryService(authors, categories, books, members, cards, borrows, properties);
	}

	@Test
	void createsAuthor() {
		Author author = new Author("Ursula Le Guin", "Writer", "USA");
		when(authors.save(any(Author.class))).thenReturn(author);

		var response = service.createAuthor(new AuthorRequest("Ursula Le Guin", "Writer", "USA"));

		assertEquals("Ursula Le Guin", response.name());
		verify(authors).save(any(Author.class));
	}

	@Test
    void rejectsMissingAuthor() {
        when(authors.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getAuthor(99L));
    }

	@Test
	void createsCategory() {
		Category category = new Category("History", "Historical works");
		when(categories.save(any(Category.class))).thenReturn(category);

		var response = service.createCategory(new CategoryRequest("History", "Historical works"));

		assertEquals("History", response.name());
	}

	@Test
    void rejectsBookWithMissingCategory() {
        when(categories.findById(7L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.createBook(
                new BookRequest("Book", "ISBN-7", 2024, 2, 7L, null)));
        verify(books, never()).save(any(Book.class));
    }

	@Test
	void updatesMember() {
		Member member = new Member("Old Name", "old@example.com", "555");
		when(members.findById(1L)).thenReturn(Optional.of(member));

		var response = service.updateMember(1L, new MemberRequest("New Name", "new@example.com", "777"));

		assertEquals("New Name", response.fullName());
		assertEquals("new@example.com", member.getEmail());
	}

	@Test
	void preventsDeletingBookWithActiveBorrowings() {
		Book book = new Book("Book", "ISBN-1", 2024, 1, 0, new Category("Fiction", ""));
		when(books.findById(1L)).thenReturn(Optional.of(book));
		when(books.countActiveBorrowings(1L)).thenReturn(1L);

		assertThrows(ConflictException.class, () -> service.deleteBook(1L));
		verify(books, never()).delete(any(Book.class));
	}

	@Test
	void preventsInactiveMemberFromBorrowing() {
		Book book = new Book("Book", "ISBN-1", 2024, 1, 1, new Category("Fiction", ""));
		Member member = new Member("Reader", "reader@example.com", "555");
		member.setStatus(MemberStatus.INACTIVE);
		when(books.findById(1L)).thenReturn(Optional.of(book));
		when(members.findById(2L)).thenReturn(Optional.of(member));

		assertThrows(ConflictException.class, () -> service.borrow(1L, 2L));
		verify(borrows, never()).save(any());
	}

	@Test
    void listsBooksUsingRequestedPage() {
        when(books.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        var response = service.listBooks(1, 10, "title", "asc");

        assertEquals(0, response.content().size());
        verify(books).findAll(any(org.springframework.data.domain.Pageable.class));
    }

}
