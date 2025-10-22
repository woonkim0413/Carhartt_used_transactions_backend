package com.C_platform.wish.applicaion;

import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.infrastructure.db.MemberRepository;
import com.C_platform.exception.ItemException;
import com.C_platform.exception.WishException;
import com.C_platform.global.error.ItemErrorCode;
import com.C_platform.global.error.WishErrorCode;
import com.C_platform.item.domain.Item;
import com.C_platform.item.infrastructure.ItemRepository;
import com.C_platform.wish.domain.Wish;
import com.C_platform.wish.infrastructure.WishRepository;
import com.C_platform.wish.ui.dto.WishResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishUseCase {

    private final WishRepository wishRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;

    @Transactional
    public void addWish(Long memberId, Long itemId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemException(ItemErrorCode.I001));

        if (wishRepository.existsByMemberMemberIdAndItemId(memberId, itemId)) {
            throw new WishException(WishErrorCode.W003);
        }

        Wish wish = Wish.builder()
                .member(member)
                .item(item)
                .build();
        wishRepository.save(wish);
    }

    @Transactional
    public void removeWish(Long memberId, Long itemId) {
        Wish wish = wishRepository.findByMemberMemberIdAndItemId(memberId, itemId)
                .orElseThrow(() -> new WishException(WishErrorCode.W001));
        wishRepository.delete(wish);
    }

    @Transactional(readOnly = true)
    public List<WishResponseDto> getMyWishlist(Long memberId) {
        // Member member = memberRepository.findById(memberId)
        //         .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        // return member.getWishes().stream()
        //         .map(WishResponseDto::from)
        //         .collect(Collectors.toList());

        List<Wish> wishes = wishRepository.findByMemberMemberId(memberId);

        return wishes.stream()
                .map(WishResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isItemWished(Long memberId, Long itemId) {
        return wishRepository.existsByMemberMemberIdAndItemId(memberId, itemId);
    }
}
