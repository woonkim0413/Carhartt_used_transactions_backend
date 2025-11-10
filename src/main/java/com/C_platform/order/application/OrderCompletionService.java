package com.C_platform.order.application;

import com.C_platform.item.domain.Images;
import com.C_platform.item.domain.Item;
import com.C_platform.item.infrastructure.ItemRepository;
import com.C_platform.order.domain.Order;
import com.C_platform.order.domain.OrderRepository;
import com.C_platform.order.domain.OrderStatus;
import com.C_platform.order.ui.dto.OrderCompletionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회 전용이므로 readOnly=true를 설정합니다.
public class OrderCompletionService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;

    /**
     * GetOrderDetailsQuery: 결제 승인된 주문 ID로 주문 상세 정보를 조회합니다.
     *
     * @param orderId 조회할 주문 ID
     * @return OrderCompletionResponse DTO
     * @throws NoSuchElementException 해당 ID의 주문을 찾을 수 없을 때
     * @throws IllegalStateException 주문 상태가 PAID가 아닐 때
     */
    public OrderCompletionResponse getOrderDetailsQuery(Long orderId) {

        // Order 엔티티 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("주문 ID " + orderId + "에 해당하는 주문을 찾을 수 없습니다."));

        // 상태 검증: 반드시 'PAID' 상태여야 합니다.
        if (order.getOrderStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("주문 ID " + orderId + "는 결제 승인(PAID) 상태가 아닙니다. 현재 상태: " + order.getOrderStatus());
        }

        // ItemSnapshot에서 itemId 가져오기
        Long itemId = order.getItemSnapshot().getItemId();

        // Item 조회 (이미지 정보 가져오기 위해)
        Item item = itemRepository.findByIdWithImages(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + itemId));

        // 이미지 URL 추출
        List<String> imageUrls = item.getImages().stream()
                .map(image -> image.getImageUrl())
                .toList();

        // 대표 이미지 URL 추출
        String representImageUrl = item.getImages().stream()
                .findFirst()
                .map(Images::getRepresentUrl)  // ← 이렇게만 하면 됨
                .orElse(null);

        // DTO로 변환
        return new OrderCompletionResponse(
                order.getId(),

                // ItemSnapshot 정보 매핑
                order.getItemSnapshot().getItemId(),
                order.getItemSnapshot().getItemName(),  // ← 수정
                order.getItemSnapshot().getPrice(),

                imageUrls,
                representImageUrl,
                // ✅ 변경 후 (Order에서 Address 직접 참조)
                order.getAddress().getMember().getName(),  // 수신자 이름: Member의 이름을 사용할 수도 있음
                order.getAddress().getRoadAddress() + " " + order.getAddress().getDetailAddress(),
                order.getAddress().getZip(),

                // 주문 시점 정보
                order.getOrderDateTime(),
                order.getOrderStatus().name()
        );
    }
}