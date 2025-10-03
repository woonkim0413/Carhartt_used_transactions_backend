package com.C_platform.item.infrastructure;


import com.C_platform.item.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item,Long> {
    Page<Item> findByNameContaining(String name, Pageable pageable);

}
