package com.mahesh.library.usermgmt.security;

import com.mahesh.library.repository.BorrowRecordRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Ownership checks for @PreAuthorize, e.g. "@memberAccess.isOwnMember(#request.memberId())".
 * The member a user may act for is the one staff linked to the account (User.memberId); it is never
 * derived from an email or any other value the user can set themselves.
 */
@Component("memberAccess")
public class MemberAccess {

    private final BorrowRecordRepository borrows;

    public MemberAccess(BorrowRecordRepository borrows) {
        this.borrows = borrows;
    }

    public Optional<Long> currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.ofNullable(principal.getMemberId());
    }

    public boolean isOwnMember(Long memberId) {
        return memberId != null && currentMemberId().map(memberId::equals).orElse(false);
    }

    public boolean ownsBorrow(Long borrowId) {
        return borrowId != null && borrows.findMemberIdById(borrowId).map(this::isOwnMember).orElse(false);
    }
}
