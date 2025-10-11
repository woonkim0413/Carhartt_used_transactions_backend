package com.C_platform.wish.infrastructure;

import com.C_platform.wish.domain.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishRepository extends JpaRepository<Wish, Long> {
    Optional<Wish> findByMemberMemberIdAndItemId(Long memberId, Long itemId);
    boolean existsByMemberMemberIdAndItemId(Long memberId, Long itemId);
    List<Wish> findByMemberMemberId(Long memberId);
}
