package com.mahesh.library.repository;

import com.mahesh.library.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query(
        value = "SELECT DISTINCT b FROM Book b JOIN FETCH b.category LEFT JOIN FETCH b.authors a " +
                "WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%')) " +
                "AND (:category IS NULL OR LOWER(b.category.name) = LOWER(:category))",
        countQuery = "SELECT COUNT(DISTINCT b) FROM Book b " +
                "WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%')) " +
                "AND (:category IS NULL OR LOWER(b.category.name) = LOWER(:category))"
    )
    Page<Book> search(
        @Param("title") String title,
        @Param("category") String category,
        Pageable pageable
    );

    // Used by LibraryService.searchBooks(title, author, ...).
    @Query(
        value = "SELECT DISTINCT b FROM Book b JOIN b.category c LEFT JOIN b.authors a " +
                "WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%')) " +
                "AND LOWER(a.name) LIKE LOWER(CONCAT('%', :author, '%'))",
        countQuery = "SELECT COUNT(DISTINCT b) FROM Book b LEFT JOIN b.authors a " +
                "WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%')) " +
                "AND LOWER(a.name) LIKE LOWER(CONCAT('%', :author, '%'))"
    )
    Page<Book> searchByAuthor(
        @Param("title") String title,
        @Param("author") String author,
        Pageable pageable
    );

    @Query("SELECT DISTINCT b FROM Book b JOIN FETCH b.category JOIN FETCH b.authors a WHERE LOWER(a.name) = LOWER(:author)")
    List<Book> findBooksByAuthorName(@Param("author") String author);

    @Query("SELECT b FROM Book b JOIN FETCH b.category WHERE b.availableCopies > 0")
    List<Book> findAvailableBooks();

    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.book.id = :id AND br.status IN ('BORROWED', 'OVERDUE')")
    long countActiveBorrowings(@Param("id") Long id);

    @Query("SELECT b.title, COUNT(br.id) AS borrowCount FROM BorrowRecord br JOIN br.book b GROUP BY b.id, b.title ORDER BY COUNT(br.id) DESC")
    List<Object[]> findTopBorrowedBooks(Pageable pageable);

    @Modifying
    @Query("UPDATE Book b SET b.availableCopies = b.availableCopies + 1 WHERE b.id = :id AND b.availableCopies < b.totalCopies")
    int incrementAvailableCopies(@Param("id") Long id);

    Optional<Book> findByIsbn(String isbn);
}
