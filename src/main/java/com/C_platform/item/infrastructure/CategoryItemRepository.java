package com.C_platform.item.infrastructure;

import com.C_platform.item.domain.CategoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryItemRepository extends JpaRepository<CategoryItem, Long> {
}
