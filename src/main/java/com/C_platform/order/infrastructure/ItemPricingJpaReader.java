package com.C_platform.order.infrastructure;

import com.C_platform.order.application.port.ItemPricingReader;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ItemPricingJpaReader implements ItemPricingReader {

    @PersistenceContext
    private EntityManager em;

    @Override
    public ItemView getById(Long itemId) {
        return em.createQuery("""
            select new com.C_platform.order.application.port.ItemPricingReader$ItemView(
                i.id,
                cast(null as long),         
                cast(i.price as big_decimal)  
            )
            from com.C_platform.item.domain.Item i
            where i.id = :id
              and i.status = com.C_platform.item.domain.ItemStatus.FOR_SALE
            """, ItemView.class)
                .setParameter("id", itemId)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
}

