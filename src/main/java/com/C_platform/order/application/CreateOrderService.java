package com.C_platform.order.application;

import com.C_platform.Member_woonkim.domain.entitys.Address;
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
    private final MemberRepository memberRepository;
    private final ItemPricingReader itemReader;
    private final AddressReader addressReader;

    public Long create(CreateOrderCommand cmd, String requestId) {

        // 이미 처리된 요청인지 확인
        if (orderRepository.existsByRequestId(requestId)) {
            throw new IllegalStateException("Duplicate requestId: " + requestId);
        }

        log.info("REQ buyerId={}, itemId={}, addressId={}",
                cmd.buyerId(), cmd.itemId(), cmd.addressId());

        // 1) 아이템 조회
        log.info("STEP getItemOrThrow in");
        ItemView item = getItemOrThrow(cmd.itemId());
        log.info("STEP getItemOrThrow out: id={}, price={}, sellerId={}",
                item.id(), item.price(), item.sellerId());

        // 2) 배송지
        Address address = addressReader.getAddressOrThrow(cmd.addressId());

        // 3) 가격 스냅샷
        var snapshot = ItemSnapshot.of(item.id(), item.name(), item.price());

        // ✅ 4) buyer 조회
        Member buyer = memberRepository.findById(cmd.buyerId())
                .orElseThrow(() -> new CreateOrderException(CreateOrderErrorCode.O006));

        // ✅ 5) seller 조회
        Member seller = memberRepository.findById(item.sellerId())
                .orElseThrow(() -> new CreateOrderException(CreateOrderErrorCode.O007));

        // ✅ 6) buyer/seller 포함해서 Order 생성
        var order = Order.createOrder(buyer, seller, address, cmd.detailMessage(), snapshot);
        orderRepository.save(order);

        log.info("주문 생성 완료: orderId={}, buyerId={}, sellerId={}",
                order.getId(), buyer.getMemberId(), seller.getMemberId());

        return order.getId();
    }

    // --- helpers ---

    private ItemView getItemOrThrow(Long itemId) {
        try {
            var item = itemReader.getById(itemId);
            if (item == null || item.id() == null) {
                log.warn("itemReader returned null for itemId={}", itemId);
                throw new CreateOrderException(CreateOrderErrorCode.O001); // ✅ 아이템 없음
            }
            return item;
        } catch (EntityNotFoundException | NoSuchElementException e) {
            log.warn("itemReader not found: {}", itemId, e);
            throw new CreateOrderException(CreateOrderErrorCode.O001); // ✅ 아이템 없음
        } catch (RuntimeException e) {
            log.warn("itemReader runtime error: {}", itemId, e);
            throw new CreateOrderException(CreateOrderErrorCode.O001); // ✅ 아이템 조회 중 런타임 예외
        }
    }

}



