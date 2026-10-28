package com.mahesh.library.controller;

import com.mahesh.library.dto.ApiDtos.BorrowResponse;
import com.mahesh.library.dto.ApiDtos.CardRequest;
import com.mahesh.library.dto.ApiDtos.MemberRequest;
import com.mahesh.library.dto.ApiDtos.MemberResponse;
import com.mahesh.library.dto.ApiDtos.MemberStatusRequest;
import com.mahesh.library.dto.PageResponse;
import com.mahesh.library.service.LibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "Members", description = "Register members, issue membership cards and view their loans and fines")
public class MemberController {

    private final LibraryService service;

    public MemberController(LibraryService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a member")
    public MemberResponse create(@Valid @RequestBody MemberRequest request) {
        return service.createMember(request);
    }

    @GetMapping
    @Operation(summary = "List members (paginated)")
    public PageResponse<MemberResponse> list(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(defaultValue = "id") String sortBy,
                                             @RequestParam(defaultValue = "asc") String direction) {
        return service.listMembers(page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a member by id")
    public MemberResponse get(@PathVariable Long id) {
        return service.getMember(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a member's details")
    public MemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        return service.updateMember(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a member")
    public void delete(@PathVariable Long id) {
        service.deleteMember(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change a member's status", description = "Only active members can borrow books.")
    public MemberResponse updateStatus(@PathVariable Long id, @Valid @RequestBody MemberStatusRequest request) {
        return service.setMemberStatus(id, request.status());
    }

    @PostMapping("/{id}/card")
    @Operation(summary = "Issue a membership card", description = "A member needs a valid, unexpired card to borrow books.")
    public MemberResponse issueCard(@PathVariable Long id, @Valid @RequestBody CardRequest request) {
        return service.issueCard(id, request);
    }

    @GetMapping("/{id}/borrow-history")
    @Operation(summary = "List every loan a member has made, including returned ones")
    public List<BorrowResponse> borrowHistory(@PathVariable Long id) {
        return service.memberHistory(id);
    }

    @GetMapping("/{id}/fines")
    @Operation(summary = "Get the total fines a member owes")
    public BigDecimal fines(@PathVariable Long id) {
        return service.memberFines(id);
    }
}
