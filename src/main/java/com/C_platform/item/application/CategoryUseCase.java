package com.C_platform.item.application;

import com.C_platform.item.ui.dto.CategoryResponseDto;
import com.C_platform.item.infrastructure.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryUseCase {

    private final CategoryRepository categoryRepository;

    public Optional<List<CategoryResponseDto>> getCategories() {
        List<com.C_platform.item.domain.Category> allCategories = categoryRepository.findAll();

        if (allCategories.isEmpty()) {
            return Optional.empty();
        }

        //        // 상위 카테고리 를 기준으로 자식 카테고리를 갖는 MAP 생성
        //        Map<Long, List<com.C_platform.item.domain.Category>> parentIdToChildren = new HashMap<>();
        //        for (com.C_platform.item.domain.Category category : allCategories) {
        //            if (category.getParent() != null) {  // 상위 카테고리가 있다면
        //                Long parentId = category.getParent().getId(); // 상위 카테고리 ID 를 가져오고
        //                // 상위 카테고리 id 를 키로 자식 카테고리 리스트를 추출
        //                parentIdToChildren.computeIfAbsent(parentId, k -> new ArrayList<>()).add(category);
        //            }
        //        }
        Map<Long, List<com.C_platform.item.domain.Category>> parentIdToChildren = allCategories.stream()
                .filter(category -> category.getParent() != null)
                .collect(Collectors.groupingBy(category -> category.getParent().getId()));


        //        List<CategoryResponseDto> topLevelDtos = new ArrayList<>();
        //        for (com.C_platform.item.domain.Category category : allCategories) {
        //            if (category.getParent() == null) {
        //                topLevelDtos.add(convertToDto(category, parentIdToChildren, true));
        //            }
        //        }
        List<CategoryResponseDto> topLevelDtos = allCategories.stream()
                .filter(category -> category.getParent() == null)
                .map(category -> convertToDto(category, parentIdToChildren, true))
                .collect(Collectors.toList());

        CategoryResponseDto root = CategoryResponseDto.builder()
                .categoryId(0L)
                .categoryName("전체")
                .children(topLevelDtos)
                .build();

        List<CategoryResponseDto> result = new ArrayList<>();
        result.add(root);
        return Optional.of(result);
    }

    private CategoryResponseDto convertToDto(com.C_platform.item.domain.Category category, Map<Long, List<com.C_platform.item.domain.Category>> parentIdToChildren, boolean isTopLevel) {
        CategoryResponseDto dto = CategoryResponseDto.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .parentId(isTopLevel ? Long.valueOf(0L) : (category.getParent() != null ? category.getParent().getId() : null))
                .build();

        List<com.C_platform.item.domain.Category> children = parentIdToChildren.get(category.getId());
        if (children != null && !children.isEmpty()) {
            List<CategoryResponseDto> childDtos = children.stream()
                    .map(child -> convertToDto(child, parentIdToChildren, false))
                    .collect(Collectors.toList());
            dto.addChildren(childDtos);
        }

        return dto;
    }

}