package com.mahesh.library.controller;

import com.mahesh.library.dto.ApiDtos.BorrowResponse;
import com.mahesh.library.dto.ApiDtos.CardRequest;
import com.mahesh.library.dto.ApiDtos.MemberRequest;
import com.mahesh.library.dto.ApiDtos.MemberResponse;
import com.mahesh.library.dto.ApiDtos.MemberStatusRequest;
import com.mahesh.library.dto.PageResponse;
import com.mahesh.library.exception.NotFoundException;
import com.mahesh.library.service.LibraryService;
import com.mahesh.library.usermgmt.dto.LinkAccountRequest;
import com.mahesh.library.usermgmt.dto.UserResponse;
import com.mahesh.library.usermgmt.security.AccessRules;
import com.mahesh.library.usermgmt.security.MemberAccess;
import com.mahesh.library.usermgmt.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@PreAuthorize(AccessRules.STAFF)
@RequestMapping("/api/v1/members")
@Tag(name = "Members", description = "Register members, issue membership cards and view their loans and fines")
public class MemberController {

    private final LibraryService service;
    private final UserService userService;
    private final MemberAccess memberAccess;

    public MemberController(LibraryService service, UserService userService, MemberAccess memberAccess) {
        this.service = service;
        this.userService = userService;
        this.memberAccess = memberAccess;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the member record linked to the logged-in account")
    public MemberResponse me() {
        return service.getMember(memberAccess.currentMemberId()
                .orElseThrow(() -> new NotFoundException("No library member is linked to this account")));
    }

    @PutMapping("/{id}/account")
    @Operation(summary = "Link a login account to this member",
            description = "The linked user may then borrow, return and view active loans for this member. "
                    + "A member has at most one account and an account at most one member.")
    public UserResponse linkAccount(@PathVariable Long id, @Valid @RequestBody LinkAccountRequest request) {
        return userService.toResponse(userService.linkMember(id, request.userId()));
    }

    @DeleteMapping("/{id}/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unlink the login account from this member", description = "Required before deleting a linked member.")
    public void unlinkAccount(@PathVariable Long id) {
        userService.unlinkMember(id);
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
