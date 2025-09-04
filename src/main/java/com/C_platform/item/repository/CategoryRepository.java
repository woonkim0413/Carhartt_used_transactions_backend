package com.C_platform.item.repository;


import com.C_platform.item.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // parent_category가 null이거나 '0'인 최상위
    @Query(value = """
      SELECT c.* FROM category c
      WHERE c.parent_category IS NULL OR c.parent_category = '0'
      ORDER BY c.category_id
      """, nativeQuery = true)
    List<Category> findTopLevel();

    // 특정 parent의 자식들
    @Query(value = """
      SELECT c.* FROM category c
      WHERE CAST(c.parent_category AS UNSIGNED) = :parentId
      ORDER BY c.category_id
      """, nativeQuery = true)
    List<Category> findChildren(@Param("parentId") int parentId);

    // 전체 조회가 필요하면 이것만 써도 됨
    @Query(value = "SELECT c.* FROM category c", nativeQuery = true)
    List<Category> findAllRaw();
}
