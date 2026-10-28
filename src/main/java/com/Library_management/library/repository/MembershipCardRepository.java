package com.mahesh.library.repository;
import com.mahesh.library.entity.MembershipCard; import org.springframework.data.jpa.repository.JpaRepository;
public interface MembershipCardRepository extends JpaRepository<MembershipCard,Long>{}