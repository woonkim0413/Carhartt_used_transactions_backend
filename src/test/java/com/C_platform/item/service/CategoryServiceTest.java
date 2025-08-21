package com.C_platform.item.service;

import com.C_platform.item.entity.Category;
import com.C_platform.item.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.annotation.Transactional;


@Transactional
class CategoryServiceTest {

    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setParentCategory("");
    }


}