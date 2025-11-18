package com.C_platform.Member_woonkim.infrastructure.db;

import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.domain.enums.LocalProvider;
import com.C_platform.Member_woonkim.domain.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // memberId로 회원 조회
    Optional<Member> findById(Long memberId);

    // email로 회원 조회
    Optional<Member> findByEmail(String email);

    // Oauth 로그일 때 회원 조회
    Optional<Member> findByOauthProviderAndOauthId(OAuthProvider provider, String oauthId);
    // Local 로그인일 때 회원 조회
    Optional<Member> findByLocalProviderAndEmail(LocalProvider localProvider, String email);    // Local 로그인일 때 회원 조회

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
