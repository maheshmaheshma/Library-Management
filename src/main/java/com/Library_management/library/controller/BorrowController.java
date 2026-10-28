package com.mahesh.library.controller;

import com.mahesh.library.dto.ApiDtos.BorrowRequest;
import com.mahesh.library.dto.ApiDtos.BorrowResponse;
import com.mahesh.library.dto.PageResponse;
import com.mahesh.library.service.LibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Borrowing", description = "Borrow and return books, and track overdue loans")
public class BorrowController {

    private final LibraryService service;

    public BorrowController(LibraryService service) {
        this.service = service;
    }

    @PostMapping("/borrow")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Borrow a book",
            description = "The member must be active with a valid card, have fewer than 3 active loans "
                    + "and not already hold this book. The book must have a copy available.")
    public BorrowResponse borrow(@Valid @RequestBody BorrowRequest request) {
        return service.borrow(request.bookId(), request.memberId());
    }

    @PutMapping("/return/{borrowId}")
    @Operation(summary = "Return a borrowed book",
            description = "Any fine for late return (daily rate x days late) is calculated and stored on the loan.")
    public BorrowResponse returnBook(@PathVariable Long borrowId) {
        return service.returnBook(borrowId);
    }

    @GetMapping("/borrow")
    @Operation(summary = "List all loans (paginated)")
    public PageResponse<BorrowResponse> list(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(defaultValue = "id") String sortBy,
                                             @RequestParam(defaultValue = "asc") String direction) {
        return service.listBorrows(page, size, sortBy, direction);
    }

    @GetMapping("/borrow/overdue")
    @Operation(summary = "List loans that are past their due date and not yet returned")
    public List<BorrowResponse> overdue() {
        return service.overdue();
    }

    @GetMapping("/borrow/member/{memberId}/active")
    @Operation(summary = "List a member's current (not yet returned) loans")
    public List<BorrowResponse> activeForMember(@PathVariable Long memberId) {
        return service.activeByMember(memberId);
    }

    @PostMapping("/borrow/refresh-overdue")
    @Operation(summary = "Mark past-due loans as OVERDUE", description = "Returns how many loans were updated.")
    public Map<String, Integer> refreshOverdue() {
        return Map.of("updated", service.refreshOverdue());
    }
}
