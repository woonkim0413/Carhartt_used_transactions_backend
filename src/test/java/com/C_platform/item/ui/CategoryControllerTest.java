package com.C_platform.item.ui;

import com.C_platform.item.application.CategoryUseCase;
import com.C_platform.item.domain.Category;
import com.C_platform.item.infrastructure.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CategoryController의 통합 테스트 클래스
 * - GET /v1/categories: 카테고리 목록 조회
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryUseCase categoryUseCase;

    private Category topCategory;
    private Category childCategory;

    @BeforeEach
    void setUp() {
        // 기존 카테고리 모두 삭제 (테스트 격리)
        categoryRepository.deleteAll();

        // 테스트용 카테고리 생성 및 저장
        topCategory = Category.builder()
                .name("상의")
                .build();
        categoryRepository.save(topCategory);

        childCategory = Category.builder()
                .name("티셔츠")
                .parent(topCategory)
                .build();
        categoryRepository.save(childCategory);
    }

    /**
     * 테스트 1: 카테고리 조회 API - 정상 케이스
     */
    @Test
    @DisplayName("정상적인 카테고리 조회 요청 시 카테고리 목록과 200 OK 반환")
    void getCategories_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/categories"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1)) // root 카테고리 하나
                .andExpect(jsonPath("$.data[0].category_id").value(0)) // root categoryId는 0
                .andExpect(jsonPath("$.data[0].category_name").value("전체"))
                .andExpect(jsonPath("$.data[0].children").isArray())
                .andExpect(jsonPath("$.data[0].children.length()").value(1)) // 상의
                .andExpect(jsonPath("$.data[0].children[0].category_name").value("상의"))
                .andExpect(jsonPath("$.data[0].children[0].children").isArray())
                .andExpect(jsonPath("$.data[0].children[0].children.length()").value(1)) // 티셔츠
                .andExpect(jsonPath("$.data[0].children[0].children[0].category_name").value("티셔츠"))
                .andExpect(jsonPath("$.meta.timestamp").exists());
    }

    /**
     * 테스트 2: 카테고리 조회 API - 카테고리가 없는 경우
     */
    @Test
    @DisplayName("카테고리가 없는 경우 에러 응답 반환")
    void getCategories_EmptyCategories() throws Exception {
        // given: 모든 카테고리 삭제
        categoryRepository.deleteAll();

        // when & then
        mockMvc.perform(get("/v1/categories"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C001"))
                .andExpect(jsonPath("$.meta.timestamp").exists());
    }

    /**
     * 테스트 3: 카테고리 조회 API - 계층 구조 검증 (3단계)
     */
    @Test
    @DisplayName("카테고리 계층 구조가 올바르게 반환되는지 검증")
    void getCategories_HierarchyStructure() throws Exception {
        // given: 3단계 카테고리 추가
        Category grandChildCategory = Category.builder()
                .name("반팔티")
                .parent(childCategory)
                .build();
        categoryRepository.save(grandChildCategory);

        // when & then
        mockMvc.perform(get("/v1/categories"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].children[0].children[0].children").isArray())
                .andExpect(jsonPath("$.data[0].children[0].children[0].children.length()").value(1))
                .andExpect(jsonPath("$.data[0].children[0].children[0].children[0].category_name").value("반팔티"));
    }

    /**
     * 테스트 4: 카테고리 조회 API - 여러 최상위 카테고리
     */
    @Test
    @DisplayName("여러 최상위 카테고리가 있는 경우 모두 반환되는지 검증")
    void getCategories_MultipleTopLevelCategories() throws Exception {
        // given: 추가 최상위 카테고리 생성
        Category bottomCategory = Category.builder()
                .name("하의")
                .build();
        categoryRepository.save(bottomCategory);

        Category pantsCategory = Category.builder()
                .name("바지")
                .parent(bottomCategory)
                .build();
        categoryRepository.save(pantsCategory);

        // when & then
        mockMvc.perform(get("/v1/categories"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].children").isArray())
                .andExpect(jsonPath("$.data[0].children.length()").value(2)) // 상의, 하의
                .andExpect(jsonPath("$.data[0].children[*].category_name",
                        containsInAnyOrder("상의", "하의")));
    }

    /**
     * 테스트 5: 카테고리 조회 API - 부모 ID 검증
     */
    @Test
    @DisplayName("최상위 카테고리의 parentId가 0으로 설정되는지 검증")
    void getCategories_ParentIdCheck() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/categories"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].children[0].p_id").value(0)); // 최상위 카테고리의 parentId는 0
    }
}
