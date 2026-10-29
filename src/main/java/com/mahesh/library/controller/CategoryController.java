package com.mahesh.library.controller;

import com.mahesh.library.dto.ApiDtos.CategoryRequest;
import com.mahesh.library.dto.ApiDtos.CategoryResponse;
import com.mahesh.library.dto.PageResponse;
import com.mahesh.library.service.LibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.mahesh.library.usermgmt.security.AccessRules;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Create and manage book categories")
public class CategoryController {

    private final LibraryService service;

    public CategoryController(LibraryService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize(AccessRules.STAFF)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a category")
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return service.createCategory(request);
    }

    @GetMapping
    @Operation(summary = "List categories (paginated)")
    public PageResponse<CategoryResponse> list(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(defaultValue = "id") String sortBy,
                                               @RequestParam(defaultValue = "asc") String direction) {
        return service.listCategories(page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a category by id")
    public CategoryResponse get(@PathVariable Long id) {
        return service.getCategory(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize(AccessRules.STAFF)
    @Operation(summary = "Update a category")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return service.updateCategory(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(AccessRules.STAFF)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a category")
    public void delete(@PathVariable Long id) {
        service.deleteCategory(id);
    }
}
