package com.C_platform.item.applicaion;


import com.C_platform.Member.domain.Member.Member;
import com.C_platform.Member.infrastructure.MemberRepository;
import com.C_platform.exception.CategoryException;
import com.C_platform.exception.ItemException;
import com.C_platform.global.error.CategoryErrorCode;
import com.C_platform.global.error.ItemErrorCode;
import com.C_platform.item.domain.*;
import com.C_platform.item.infrastructure.CategoryRepository;
import com.C_platform.item.infrastructure.ItemRepository;
import com.C_platform.item.ui.dto.CreateItemRequestDto;
import com.C_platform.item.ui.dto.ItemDetailResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ItemUseCase {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;


    @Transactional
    public Item createItem(CreateItemRequestDto requestDto , Long memberId) {

        Category category = categoryRepository.findById(requestDto.getCategoryId().longValue())
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.C001));

        Member findMember = memberRepository.findById(memberId)
                .orElseThrow(()-> new IllegalStateException("존재하지 않는 회원입니다."));

        TopItemEmbeddable topItemEmbeddable = TopItemEmbeddable.builder()
                .chest(requestDto.getSizes().getChest()) // 가슴
                .shoulder(requestDto.getSizes().getShoulder()) // 어깨
                .sleeve(requestDto.getSizes().getSleeve()) // 소매
                .build();

        Item item = TopItem.builder()
                .name(requestDto.getName())
                .member(findMember)
                .price(requestDto.getPrice())
                .status(ItemStatus.FOR_SALE)
                .signedDate(LocalDateTime.now())
                .description(requestDto.getDescription())
                .totalLength(requestDto.getSizes().getTottalength())
                .directTrade(requestDto.getDirectTrade())
                .topinfo(topItemEmbeddable)
                .build();

        // CategoryItem 생성 및 Item에 추가
        CategoryItem categoryItem = CategoryItem.builder()
                .category(category)
                .item(item)
                .build();
        item.getCategories().add(categoryItem);

        // Images 생성 및 Item에 추가
        requestDto.getImagePaths().forEach(imagePath -> {
            Images image = Images.builder()
                    .imageUrl(imagePath)
                    .representUrl(imagePath)
                    .item(item)
                    .build();
            item.getImages().add(image);
        });

        // Item 저장 (Images와 CategoryItem이 함께 저장됨)
        itemRepository.save(item);

        return item;
    }

    @Transactional
    public Item updateItem(Long itemId, Long memberId, com.C_platform.item.ui.dto.UpdateItemRequestDto requestDto) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 상품입니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 회원입니다."));


        if (!item.getMember().getMemberId().equals(member.getMemberId())) {
            throw new IllegalStateException("상품을 수정할 권한이 없습니다.");
        }

        Category category = categoryRepository.findById(requestDto.getCategoryId().longValue())
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.C001));

        item.updateDetails(requestDto.getName(), requestDto.getPrice(), requestDto.getDescription(), requestDto.getDirectTrade());
        item.changeCategory(category);

        if (item instanceof TopItem) {
            TopItemEmbeddable topItemEmbeddable = TopItemEmbeddable.builder()
                    .chest(requestDto.getSizes().getChest())
                    .shoulder(requestDto.getSizes().getShoulder())
                    .sleeve(requestDto.getSizes().getSleeve())
                    .build();
            ((TopItem) item).updateSize(topItemEmbeddable);
        }

        return itemRepository.save(item);
    }

    @Transactional(propagation= Propagation.REQUIRED)
    public void deleteItem(Long itemId, Long memberId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemException(ItemErrorCode.I002));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 회원입니다."));

        if (!item.getMember().getMemberId().equals(member.getMemberId())) {
            throw new IllegalStateException("상품을 삭제할 권한이 없습니다.");
        }

        try {
            itemRepository.delete(item);
        } catch (Exception e) {
            throw new ItemException(ItemErrorCode.I002);
        }
    }

    @Transactional(readOnly = true)
    public ItemDetailResponseDto findItemDetailById(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemException(ItemErrorCode.I002));

        return ItemDetailResponseDto.of(item);
    }
}
