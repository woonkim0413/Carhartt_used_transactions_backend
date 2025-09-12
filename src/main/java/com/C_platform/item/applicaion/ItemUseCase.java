package com.C_platform.item.applicaion;

import com.C_platform.exception.CategoryException;
import com.C_platform.global.error.CategoryErrorCode;
import com.C_platform.item.domain.*;
import com.C_platform.item.infrastructure.CategoryItemRepository;
import com.C_platform.item.infrastructure.CategoryRepository;
import com.C_platform.item.infrastructure.ImagesRepository;
import com.C_platform.item.infrastructure.ItemRepository;
import com.C_platform.item.ui.dto.CreateItemRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ItemUseCase {

    private final ItemRepository itemRepository;
    private final ImagesRepository imagesRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryItemRepository categoryItemRepository;


    @Transactional
    public Item createItem(CreateItemRequestDto requestDto) {

        Category category = categoryRepository.findById(requestDto.getCategoryId().longValue())
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.C001));

        TopItemEmbeddable topItemEmbeddable = TopItemEmbeddable.builder()
                .chest(requestDto.getSizes().getChest()) // 가슴
                .shoulder(requestDto.getSizes().getShoulder()) // 어깨
                .sleeve(requestDto.getSizes().getSleeve()) // 소매
                .build();

        Item item = TopItem.builder()
                .name(requestDto.getName())
                .price(requestDto.getPrice())
                .status(ItemStatus.FOR_SALE)
                .signedDate(LocalDateTime.now())
                .topinfo(topItemEmbeddable)
                .build();

        itemRepository.save(item);

        requestDto.getImagePaths().forEach(imagePath -> {
            Images image = Images.builder()
                    .imageUrl(imagePath)
                    .representUrl(imagePath)
                    .item(item)
                    .build();
            imagesRepository.save(image);
        });



        CategoryItem categoryItem = CategoryItem.builder()
                .category(category)
                .item(item)
                .build();

        categoryItemRepository.save(categoryItem);


        return item;
    }
}
