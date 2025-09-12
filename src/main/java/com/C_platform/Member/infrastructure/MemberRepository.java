package com.C_platform.Member.infrastructure;

import com.C_platform.Member.domain.Member;
import com.C_platform.Member.domain.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // memberId로 회원 조회
    Optional<Member> findById(Long memberId);

    // email로 회원 조회
    Optional<Member> findByEmail(String email);

    // Provider과 OauthId로 회원 조회
    @Query("select m.memberId from Member m where m.oauthProvider = :provider and m.oauthId = :oauthId")
    Optional<Member> findIdByProviderAndOauthId(OAuthProvider provider, String oauthId);

    // orderId로 구매자 회원 조회
    @Query(value = """
        SELECT m.* FROM member m
        JOIN orders o ON o.buyer_id = m.member_id
        WHERE o.order_id = :orderId
        """, nativeQuery = true)
    Optional<Member> findBuyerByOrderId(@Param("orderId") Long orderId);

    // orderId로 판매자 회원 조회
    @Query(value = """
        SELECT m.* FROM member m
        JOIN orders o ON o.seller_id = m.member_id
        WHERE o.order_id = :orderId
        """, nativeQuery = true)
    Optional<Member> findSellerByOrderId(@Param("orderId") Long orderId);


}
