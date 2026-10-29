package com.mahesh.library.controller;

import com.mahesh.library.dto.ApiDtos.BookRequest;
import com.mahesh.library.dto.ApiDtos.BookResponse;
import com.mahesh.library.dto.ApiDtos.TopBook;
import com.mahesh.library.dto.PageResponse;
import com.mahesh.library.service.LibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.mahesh.library.usermgmt.security.AccessRules;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Books", description = "Manage the catalogue, search books and check availability")
public class BookController {

    private final LibraryService service;

    public BookController(LibraryService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize(AccessRules.STAFF)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a book", description = "The category and authors must already exist.")
    public BookResponse create(@Valid @RequestBody BookRequest request) {
        return service.createBook(request);
    }

    @GetMapping
    @Operation(summary = "List books (paginated)")
    public PageResponse<BookResponse> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(defaultValue = "id") String sortBy,
                                           @RequestParam(defaultValue = "asc") String direction) {
        return service.listBooks(page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a book by id")
    public BookResponse get(@PathVariable Long id) {
        return service.getBook(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize(AccessRules.STAFF)
    @Operation(summary = "Update a book")
    public BookResponse update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return service.updateBook(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(AccessRules.STAFF)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a book")
    public void delete(@PathVariable Long id) {
        service.deleteBook(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Search books",
            description = "All filters are optional and case-insensitive. Title and author match partially; "
                    + "category must match the full name. When author is given, category is ignored.")
    public PageResponse<BookResponse> search(@RequestParam(required = false) String title,
                                             @RequestParam(required = false) String author,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(defaultValue = "title") String sortBy,
                                             @RequestParam(defaultValue = "asc") String direction) {
        return service.searchBooks(title, author, category, page, size, sortBy, direction);
    }

    @GetMapping("/available")
    @Operation(summary = "List books with at least one copy available to borrow")
    public List<BookResponse> available() {
        return service.availableBooks();
    }

    @GetMapping("/top-borrowed")
    @Operation(summary = "List the 5 most borrowed books")
    public List<TopBook> topBorrowed() {
        return service.topBooks();
    }
}
