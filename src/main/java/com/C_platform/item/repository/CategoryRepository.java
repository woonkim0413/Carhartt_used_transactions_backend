package com.C_platform.item.repository;

import com.C_platform.item.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Integer, Category> {
}
