# 🛠 Tech Stack & Tools

<div style="text-align: center;">
  <a href="https://skillicons.dev">
    <img height="80" src="https://skillicons.dev/icons?i=idea,gradle,mysql,github,docker,aws,discord,notion&theme=light" />
  </a>
</div>

| 분류 | 도구 | 활용 내용 |
|:---:|:---:|:---|
| **IDE / Build** | ![IntelliJ](https://img.shields.io/badge/IntelliJ-000000?logo=intellijidea&logoColor=white) ![Gradle](https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=white) | Spring Boot 개발 및 의존성 관리 |
| **Infra / DB** | ![AWS](https://img.shields.io/badge/AWS-232F3E?logo=amazon-aws&logoColor=white) ![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white) | EC2/S3 클라우드 환경 구축 및 Docker 컨테이너 활용 |
| **Collaboration** | ![Notion](https://img.shields.io/badge/Notion-000000?logo=notion&logoColor=white) | API 명세서 작성, 회의록 및 컨벤션 문서화 |
| **Communication** | ![Discord](https://img.shields.io/badge/Discord-5865F2?logo=discord&logoColor=white) | 실시간 이슈 공유 및 주간 스크럼 진행 |

# 공통 응답 구조 설계

프로젝트의 모든 API는 일관된 경험을 제공하기 위해 표준화된 공통 응답 구조를 따릅니다. 이는 클라이언트(웹/앱) 개발의 편의성을 높이고, 예측 가능한 에러 핸들링을 가능하게 합니다.

### 1. 공통 응답 포맷

모든 응답은 성공과 실패 케이스를 명확히 구분할 수 있는 형태를 가집니다.

*   **성공 시:** `data` 필드에 요청에 대한 결과가 담깁니다.
*   **실패 시:** `error` 필드에 에러 코드(`code`), 메시지(`message`), 그리고 상세 내용(`details`)이 포함됩니다.

**성공 예시**
```json
{
  "success": true,
  "data": {
    "name": "Detroit Jacket Winter 2025",
    "item_price": 150000
  },
  "meta": {
    "timestamp": "2025-09-01T12:00:00Z"
  }
}
```

**실패 예시**
```json
{
  "success": false,
  "error": {
    "code": "I001",
    "message": "상품을 찾을 수 없습니다.",
    "details": []
  },
  "meta": {
    "request_id": "req_...",
    "timestamp": "2025-09-01T12:00:00Z"
  }
}
```

### 2. 설계 이유 및 기대 효과

만약 공통 응답 구조가 없다면, 각 API는 저마다 다른 형식으로 응답을 반환하게 됩니다. 어떤 API는 성공 시 데이터만 반환하고, 실패 시에는 에러 메시지를 문자열로만 보내거나 예측 불가능한 JSON 객체를 반환할 수 있습니다. 이러한 비일관성은 클라이언트 측에서 매번 다른 형식의 응답을 처리하기 위한 분기 로직을 추가하게 만들어 코드를 복잡하게 하고, 에러 처리를 어렵게 만듭니다.

저희는 이러한 문제점을 해결하고 아래와 같은 이점을 얻기 위해 공통 응답 구조를 설계했습니다.

*   **클라이언트의 예측 가능성 확보:** 모든 응답이 동일한 구조를 가지므로, 클라이언트 측에서는 성공과 실패를 일관된 방식으로 처리할 수 있습니다. 이는 프론트엔드 개발 생산성을 향상시키고 휴먼 에러를 줄입니다.


*   **체계적인 에러 관리:** 모든 에러를 `ErrorCode`라는 `enum` 타입으로 중앙에서 관리합니다. 이를 통해 에러 코드가 중복되거나 누락되는 것을 방지하고, 애플리케이션 전체의 에러 상황을 한눈에 파악할 수 있습니다.또한 내부적으로 에러코드를 관리하여 향후 문서화 작업 을 원활하게 할수 있습니다. 에러 발생시 에는 에러코드를 확인하여 문제 해결을 신속하게 할수 있습니다.


*   **다국어 지원 및 메시지 중앙화:** 에러 메시지를 하드코딩하는 대신, Spring의 `MessageSource`를 활용하여 `messages_errors.properties` 파일에서 관리합니다. `ErrorCode`를 키로 사용하여 메시지를 조회하므로, 향후 `messages_errors_en.properties` 와 같은 파일을 추가하는 것만으로 손쉽게 다국어 지원이 가능합니다.


*   **디버깅 및 추적 용이성:** 응답에 포함된 `meta.request_id`는 로깅 시스템(e.g., Logback)의 MDC(Mapped Diagnostic Context)와 연동되어, 특정 요청에 대한 모든 로그를 쉽게 추적할 수 있게 해줍니다. 사용자가 에러를 보고할 때 `request_id`만 전달받으면, 개발자는 해당 요청의 전체 처리 과정을 신속하게 파악하고 디버깅할 수 있습니다.

# 🚀 주요 기능 소개

본 프로젝트의 핵심 기능을 도메인별로 분류하여 API 엔드포인트와 함께 간략하게 설명합니다.

| 도메인 | API URL | 설명 |
|:------|:--------|:-----|
| **회원** | `POST /api/v1/members/register` | 새로운 사용자 계정을 생성합니다. |
| | `POST /api/v1/members/login` | OAuth2를 통한 소셜 로그인 기능을 제공합니다. |
| | `GET /api/v1/members/profile` | 현재 로그인된 사용자의 프로필 정보를 조회합니다. |
| | `PUT /api/v1/members/profile` | 현재 로그인된 사용자의 프로필 정보를 수정합니다. |
| **상품** | `POST /api/v1/items` | 새로운 상품을 등록합니다. |
| | `GET /api/v1/items/{itemId}` | 특정 상품의 상세 정보를 조회합니다. |
| | `GET /api/v1/items` | 등록된 상품 목록을 필터링 및 페이징하여 조회합니다. |
| | `PUT /api/v1/items/{itemId}` | 특정 상품의 정보를 수정합니다. |
| | `DELETE /api/v1/items/{itemId}` | 특정 상품을 삭제합니다. |
| | `GET /api/v1/categories` | 상품 카테고리 목록을 조회합니다. |
| **주문** | `POST /api/v1/orders` | 선택된 상품들로 새로운 주문을 생성합니다. |
| | `GET /api/v1/orders/{orderId}` | 특정 주문의 상세 정보를 조회합니다. |
| | `GET /api/v1/orders/my` | 현재 로그인된 사용자의 주문 목록을 조회합니다. |
| **결제** | `POST /api/v1/payments/ready` | 결제 서비스 제공자(PG사)와 연동하여 결제를 준비합니다. |
| | `POST /api/v1/payments/callback` | PG사로부터 결제 결과를 전달받아 처리합니다. |
| **찜** | `POST /api/v1/wishes/{itemId}` | 특정 상품을 찜 목록에 추가하거나 해제합니다. |
| | `GET /api/v1/wishes` | 현재 로그인된 사용자의 찜 목록을 조회합니다. |
| **이미지** | `POST /api/v1/images/presigned-url` | AWS S3에 이미지를 직접 업로드하기 위한 Pre-signed URL을 발급합니다. |