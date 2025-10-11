package com.C_platform.item.infrastructure;


import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.item.domain.Item;
import com.C_platform.item.domain.ItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item,Long> {
    Page<Item> findByNameContaining(String name, Pageable pageable);
    List<Item> findByMemberAndStatusIn(Member member, List<ItemStatus> statuses);

}
