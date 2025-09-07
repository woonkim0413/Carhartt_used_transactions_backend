package com.C_platform.item.service;

import com.C_platform.item.dto.CategoryResponseDto;
import com.C_platform.item.entity.Category;
import com.C_platform.item.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponseDto> getCategories() {
        List<Category> allCategories = categoryRepository.findAll();

        // 상위 카테고리 를 기준으로 자식 카테고리를 갖는 MAP 생성
        Map<Long, List<Category>> parentIdToChildren = new HashMap<>();
        for (Category category : allCategories) {
            if (category.getParent() != null) {  // 상위 카테고리가 있다면
                Long parentId = category.getParent().getId(); // 상위 카테고리 ID 를 가져오고
                // 상위 카테고리 id 를 키로 자식 카테고리 리스트를 추출
                parentIdToChildren.computeIfAbsent(parentId, k -> new ArrayList<>()).add(category);
            }
        }

        List<CategoryResponseDto> topLevelDtos = new ArrayList<>();
        for (Category category : allCategories) {
            if (category.getParent() == null) {
                topLevelDtos.add(convertToDto(category, parentIdToChildren, true));
            }
        }

        CategoryResponseDto root = CategoryResponseDto.builder()
                .categoryId(0L)
                .categoryName("전체")
                .children(topLevelDtos)
                .build();

        List<CategoryResponseDto> result = new ArrayList<>();
        result.add(root);
        return result;
    }

    private CategoryResponseDto convertToDto(Category category, Map<Long, List<Category>> parentIdToChildren, boolean isTopLevel) {
        CategoryResponseDto dto = CategoryResponseDto.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .parentId(isTopLevel ? Long.valueOf(0L) : (category.getParent() != null ? category.getParent().getId() : null))
                .build();

        List<Category> children = parentIdToChildren.get(category.getId());
        if (children != null && !children.isEmpty()) {
            List<CategoryResponseDto> childDtos = children.stream()
                    .map(child -> convertToDto(child, parentIdToChildren, false))
                    .collect(Collectors.toList());
            dto.addChildren(childDtos);
        }

        return dto;
    }

}
