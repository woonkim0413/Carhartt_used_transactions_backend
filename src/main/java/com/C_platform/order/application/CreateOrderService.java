package com.C_platform.order.application;

import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.infrastructure.db.MemberRepository;
import com.C_platform.exception.CreateOrderException;
import com.C_platform.global.error.CreateOrderErrorCode;
import com.C_platform.order.application.dto.CreateOrderCommand;
import com.C_platform.order.application.port.AddressReader;
import com.C_platform.order.application.port.ItemPricingReader;
import com.C_platform.order.application.port.ItemPricingReader.ItemView;
import com.C_platform.order.domain.ItemSnapshot;
import com.C_platform.order.domain.Order;
import com.C_platform.order.domain.OrderAddress;
import com.C_platform.order.domain.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateOrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;     // 향후 buyer/seller 연동 시 사용
    private final ItemPricingReader itemReader;          // 아이템 가격/판매자 정보
    private final AddressReader addressReader;           // 배송지 스냅샷 조회

    public Long create(CreateOrderCommand cmd) {
        log.info("REQ buyerId={}, itemId={}, addressId={}",
                cmd.buyerId(), cmd.itemId(), cmd.addressId());

        // 1) 아이템 조회 실패 → O004
        log.info("STEP getItemOrThrow in");
        ItemView item = getItemOrThrow(cmd.itemId());
        log.info("STEP getItemOrThrow out: id={}, price={}", item.id(), item.price());

        // 2) 배송지 스냅샷 실패 → O004
        log.info("STEP getShippingOrThrow in");
        var shipping = getShippingOrThrow(cmd.buyerId(), cmd.addressId());
        log.info("STEP getShippingOrThrow out: {}", shipping);

        // 3) 가격 스냅샷
        var snapshot = ItemSnapshot.of(item.id(), item.price());

        // 4) (임시) buyer/seller 미연동 → Draft 저장
        var order = Order.createDraft(shipping, cmd.detailMessage(), snapshot);
        orderRepository.save(order);

        // ✅ 4) buyer 조회
        //Member buyer = memberRepository.findById(cmd.buyerId())
        //        .orElseThrow(() -> new CreateOrderException(CreateOrderErrorCode.O006));

        // ✅ 5) seller 조회 (ItemView에 sellerId 있다고 가정)
       // Member seller = memberRepository.findById(item.sellerId())
        //        .orElseThrow(() -> new CreateOrderException(CreateOrderErrorCode.O007));

        // ✅ 6) buyer/seller 포함해서 Order 생성
        //var order = Order.createOrder(buyer, seller, shipping, cmd.detailMessage(), snapshot);
        return order.getId();
    }

    // --- helpers ---

    private ItemView getItemOrThrow(Long itemId) {
        try {
            var item = itemReader.getById(itemId);
            if (item == null) throw new CreateOrderException(CreateOrderErrorCode.O004);
            // item.id(), item.price() 등을 서비스에서 사용하므로 null 방어
            if (item.id() == null) throw new CreateOrderException(CreateOrderErrorCode.O004);
            return item;
        } catch (EntityNotFoundException | NoSuchElementException e) {
            log.warn("itemReader not found: {}", itemId, e);   // ★스택 찍기
            throw new CreateOrderException(CreateOrderErrorCode.O004);
        } catch (RuntimeException e) {
            // 어댑터에서 다른 런타임을 던지더라도 FE 규칙에 맞춘 코드로 매핑
            log.warn("itemReader runtime error: {}", itemId, e); // ★스택 찍기
            throw new CreateOrderException(CreateOrderErrorCode.O004);
        }
    }

    private OrderAddress getShippingOrThrow(Long buyerId, Long addressId) {
        try {
            var shipping = addressReader.snapshotOf(buyerId, addressId);
            if (shipping == null) throw new CreateOrderException(CreateOrderErrorCode.O004);
            return shipping;
        } catch (EntityNotFoundException | NoSuchElementException e) {
            throw new CreateOrderException(CreateOrderErrorCode.O004);
        } catch (RuntimeException e) {
            throw new CreateOrderException(CreateOrderErrorCode.O004);
        }
    }

}


