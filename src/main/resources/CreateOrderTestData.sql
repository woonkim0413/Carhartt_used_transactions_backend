-- MEMBER (buyer=1)
INSERT INTO "MEMBER" (
    DEFAULT_ADDRESS_ID,
    MEMBER_ID,
    COUNT_NUMBER,
    EMAIL,
    IMAGE_URL,
    LOGIN_PASSWORD,
    MEMBER_NAME,
    NICKNAME,
    OAUTH_ID,
    COUNT_BANK,
    LOGIN_TYPE,
    OAUTH_PROVIDER
) VALUES (
             101,              -- DEFAULT_ADDRESS_ID
             1,                -- MEMBER_ID (PK, 직접 지정도 가능)
             '123-456-7890',   -- COUNT_NUMBER
             'test@test.com',  -- EMAIL
             NULL,             -- IMAGE_URL (없으면 NULL)
             'pass1234',       -- LOGIN_PASSWORD
             '테스트구매자',
          '테스트닉네임',-- MEMBER_NAME
             NULL,             -- OAUTH_ID
             '농협은행',        -- COUNT_BANK (enum 값 중 하나)
             'LOCAL',          -- LOGIN_TYPE (enum: LOCAL / OAUTH)
             NULL              -- OAUTH_PROVIDER (enum: KAKAO / NAVER, LOCAL이면 NULL)
         );

INSERT INTO "MEMBER" (
    DEFAULT_ADDRESS_ID,
    MEMBER_ID,
    COUNT_NUMBER,
    EMAIL,
    IMAGE_URL,
    LOGIN_PASSWORD,
    MEMBER_NAME,
    NICKNAME,
    OAUTH_ID,
    COUNT_BANK,
    LOGIN_TYPE,
    OAUTH_PROVIDER
) VALUES (
             102,              -- DEFAULT_ADDRESS_ID
             3,                -- MEMBER_ID (PK, 직접 지정도 가능)
             '124-456-7890',   -- COUNT_NUMBER
             'test2@test2.com',  -- EMAIL
             NULL,             -- IMAGE_URL (없으면 NULL)
             'pass1234',       -- LOGIN_PASSWORD
             '테스트판매자',
             '테스트닉네임2',-- MEMBER_NAME
             NULL,             -- OAUTH_ID
             '농협은행',        -- COUNT_BANK (enum 값 중 하나)
             'LOCAL',          -- LOGIN_TYPE (enum: LOCAL / OAUTH)
             NULL              -- OAUTH_PROVIDER (enum: KAKAO / NAVER, LOCAL이면 NULL)
         );


-- ADDRESS (address_id=101, member_id=1) - 컬럼명은 실제 엔티티/DDL에 맞게
INSERT INTO ADDRESS (
    ADDRESS_ID,
    MEMBER_ID,
    ADDRESS_NAME,
    DETAIL_ADDRESS,
    ROAD_ADDRESS,
    ZIP
) VALUES (
             101,          -- ADDRESS_ID
             1,            -- MEMBER_ID (MEMBER 테이블의 PK)
             '집',          -- ADDRESS_NAME
             '101호',       -- DETAIL_ADDRESS
             '서울시 강남구 테스트로', -- ROAD_ADDRESS
             '12345'       -- ZIP
         );

-- ITEM (id=101)
-- ITEM (id=1101)
INSERT INTO ITEM (
    CHEST, HEM, ITEM_PRICE, RISE_LENGTH, SHOULDER, SLEEVE, THIGH, TOTAL_LENGTH,
    MEMBER_ID,  -- ← 판매자 ID 추가!
    ITEM_ID, SIGNED_DATE, UPDATE_DATE, DTYPE, ITEM_NAME, ITEM_STATUS
) VALUES (
             100, 45, 50000, 60, 50, 60, 55, 90,
             1,  -- ← MEMBER_ID (판매자도 1번 회원으로)
             1101, '2025-01-01 10:00:00', NULL, 'Item', '테스트상품', 'FOR_SALE'
         );

INSERT INTO ITEM (
    CHEST, HEM, ITEM_PRICE, RISE_LENGTH, SHOULDER, SLEEVE, THIGH, TOTAL_LENGTH,
    MEMBER_ID,  -- ← 판매자 ID 추가!
    ITEM_ID, SIGNED_DATE, UPDATE_DATE, DTYPE, ITEM_NAME, ITEM_STATUS
) VALUES (
             100, 45, 50000, 60, 50, 60, 55, 90,
             3,  -- ← MEMBER_ID (판매자도 1번 회원으로)
             200, '2025-01-01 10:00:01', NULL, 'Item', '테스트상품2', 'FOR_SALE'
         );
