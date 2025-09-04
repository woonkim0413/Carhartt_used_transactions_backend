package com.C_platform.item.service;


import com.C_platform.item.dto.CategoriesResponse;
import com.C_platform.item.dto.CategoryNode;
import com.C_platform.item.entity.Category;
import com.C_platform.item.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;


    public CategoriesResponse getCategoryList(){

        List<Category> all = categoryRepository.findAllRaw();

        Map<Integer, List<Category>> byParent = all.stream()
                .collect(Collectors.groupingBy(Category::parentIdOrZero));

        List<CategoryNode> firstLevel = byParent.getOrDefault(0, List.of()).stream()
                .sorted(Comparator.comparing(Category::getId))
                .map(parent -> {
                    List<CategoryNode> children = byParent.getOrDefault(parent.getId(), List.of()).stream()
                            .sorted(Comparator.comparing(Category::getId))
                            .map(child -> CategoryNode.leaf(parent.getId().intValue(), child.getId().intValue(), child.getName()))
                            .toList();
                    return new CategoryNode(0, parent.getId().intValue(), parent.getName(), children);
                })
                .toList();


        CategoryNode root = new CategoryNode(null, 0, "전체", firstLevel);
        Map<String,Object> meta = Map.of("timestamp", Instant.now().toString());
        return new CategoriesResponse(true, List.of(root), meta);

    }
}
