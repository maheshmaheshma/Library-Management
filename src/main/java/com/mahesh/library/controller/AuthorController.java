package com.mahesh.library.controller;

import com.mahesh.library.dto.ApiDtos.AuthorRequest;
import com.mahesh.library.dto.ApiDtos.AuthorResponse;
import com.mahesh.library.dto.ApiDtos.BookResponse;
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
@RequestMapping("/api/v1/authors")
@Tag(name = "Authors", description = "Create and manage book authors")
public class AuthorController {

    private final LibraryService service;

    public AuthorController(LibraryService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize(AccessRules.STAFF)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an author")
    public AuthorResponse create(@Valid @RequestBody AuthorRequest request) {
        return service.createAuthor(request);
    }

    @GetMapping
    @Operation(summary = "List authors (paginated)")
    public PageResponse<AuthorResponse> list(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(defaultValue = "id") String sortBy,
                                             @RequestParam(defaultValue = "asc") String direction) {
        return service.listAuthors(page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an author by id")
    public AuthorResponse get(@PathVariable Long id) {
        return service.getAuthor(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize(AccessRules.STAFF)
    @Operation(summary = "Update an author")
    public AuthorResponse update(@PathVariable Long id, @Valid @RequestBody AuthorRequest request) {
        return service.updateAuthor(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(AccessRules.STAFF)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an author")
    public void delete(@PathVariable Long id) {
        service.deleteAuthor(id);
    }

    @GetMapping("/{id}/books")
    @Operation(summary = "List all books written by an author")
    public List<BookResponse> books(@PathVariable Long id) {
        return service.authorBooks(id);
    }
}
