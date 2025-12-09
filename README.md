## 🛍️ 프로젝트 개요

<div align="center">
  <img src="image/carhartt_logo.png" width="300px" />
</div>

칼하트 구행 자켓은 빈티지 제품이기에 무엇보다 **사이즈 실측 정보가 중요**합니다

현재 칼하트 구행 자켓을 구매할 수 있는 **당근마켓, 번개장터, 크림**과 같은 경우
실측 사이즈를 제대로 전달해주지 않거나 일부 사이즈 정보만 알려주는 경우가 대부분입니다

carhartt_usedTransaction은 중요 **실측 정보와 사진을 필수로 기재**하게 하여
구매자로 하여금 나에게 딱 맞는 칼하트 자켓을 구매할 수 있도록 돕습니다 !

<br>


### 디렉토르 계층 구조 (DDD 채택)

```
📂 src/main/java/com/C_platform/
├── 📂 Member_woonkim/          # 사용자 인증 & 프로필 관리
│   ├── domain/                  # Member 엔티티, Value Objects
│   ├── application/             # OAuth2, 로컬 로그인 UseCase
│   ├── presentation/            # 컨트롤러, DTO
│   ├── infrastructure/          # Repository, OAuth 어댑터
│   └── utils/                   # OAuth, 로깅 헬퍼
├── 📂 item/                     # 상품 카탈로그
│   ├── domain/                  # Item 엔티티, Category
│   ├── application/             # 상품 UseCase
│   ├── infrastructure/          # Repository
│   └── ui/                      # 컨트롤러, DTO
├── 📂 order/                    # 주문 관리
│   ├── domain/                  # Order 집합, Value Objects
│   ├── application/             # 주문 생성 UseCase
│   ├── infrastructure/          # Repository
│   └── ui/                      # 컨트롤러, DTO
├── 📂 payment/                  # 결제 처리
│   ├── domain/                  # Payment 엔티티
│   ├── application/             # 결제 UseCase
│   ├── infrastructure/          # KakaoPayAdapter, NaverPayAdapter
│   └── ui/                      # 컨트롤러, DTO
├── 📂 config/                   # Spring 설정
│   ├── SecurityConfig.java      # OAuth2, CSRF, 보안 규칙
│   ├── WebConfig.java           # HTTP 메시지 변환기
│   ├── FileConfig.java          # AWS S3 설정
│   └── RestTemplateConfig.java  # RestTemplate 빈
├── 📂 global/                   # 전역 유틸리티
│   ├── error/                   # ErrorCode, 예외 핸들러
│   └── ApiResponse.java         # 표준 응답 래퍼
└── 📂 exception/                # 커스텀 예외 클래스
```

<br>


## 🏗️ 서버 인프라 (CI/CD + AWS 생태계)

<p align="center"><img src="image/architecture_image.png" width="500" height="300" /></p>


### - 🌐️ **인프라 구성 요소 (AWS 생태계)**

#### 1. <u>**Route53** (DNS & 도메인 관리)</u>
- 도메인 이름을 AWS 리소스 엔드포인트로 라우팅
- 백엔드 API: `api.carhartt.com` → EC2 Elastic IP로 라우팅 




#### 2. <u>**CloudFront** - CDN (정적 자산 배포)</u>
- React 빌드 산출물(정적 HTML, CSS, JS) 배포

#### 3. <u>**EC2** - 애플리케이션 서버</u>
- **인스턴스 스펙**: t4g.small (ARM 기반, vCPU: 2개, RAM: 2GB CPU 크래딧)
- **설치 소프트웨어**: Nginx, Docker Engine, AWS CLI v2, codeDeploy agent, docker container(springboot 내장) 

#### 4. <u>**Nginx** (EC2 호스트 레벨에 설치됨)</u>
- 외부 트래픽을 Docker 컨테이너의 Spring Boot 애플리케이션으로 포워딩 <br>(공개 포트: 80 (HTTP), 443 (HTTPS) -> localhost:8080)
- SSL/TLS 종료 (HTTPS 암호화)



#### 5. <u>**Docker Engine**</u>
- EC2에서 Docker Container을 띄우기 위해 사용

#### 6. <u>**AWS S3**</u>
- CI/CD 배포 번들 임시 저장 후 CodeDeploy Agent에게 중개 (CD에 사용) <BR>(파일: `appspec.yml`, `scripts/deploy.sh`, `scripts/nginx_setup.sh`)
- Application에서 사용할 이미지 저장

#### 7. <u>**AWS CodeDeploy Agent**</u>
- GitHub Actions에서 보낸 배포 명령을 수신하고 EC2에서 배포 실행


#### 8. <u>**RDS (MySQL 내장)**</u>
- MySQL 8.0을 내장하여 Application에 영속성 DB 제공

#### 9. <u>**AWS ECR** (Docker Image 저장소)</u>
- GitHub Actions에서 빌드한 Docker 이미지를 저장하고 EC2에 전달

<br/>

### - 📚 개발 환경 분리 (Local, deploy)
**설정 파일 구조:**
```
src/main/resources/
├── application.properties (CI주입)  # 배포 환경 (MySQL, info debug log level)
├── application.properties           # 로컬 환경 (H2(InMemory), debug log level)
├── application-oauth2-local.yml     # 로컬 OAuth2 설정 (여러 dummy 값 사용)
├── application-oauth2-prod.yml      # 배포 OAuth2 설정 (cors 쿠키 설정)
└── *.sql                            # 테스트 데이터 (로컬만)
```
**로컬(Local)** 과 **배포(Production)** 환경을 **Spring Profiles를 활용**하여 분리하였습니다. <br>
배포 환경에서 사용할 환경파일은 CI과정 중 Actions Securtiy Repo에서 주입 받도록 하여 
민감한 값들 (aws, s3, db key들)이 외부로 노출되지 않도록 하였습니다.
 <br>

### - 🚀 CI/CD 배포 구조



**전체 흐름:**

```
┌─────────────────────────────────────────────────────────────┐
│  1. Git Push to main Branch                                 │
│     (GitHub에서 CI/CD 워크플로우 트리거)                      │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│  2. GitHub Actions CI: 빌드 & 컨테이너 준비                   │
│     ├─ Checkout 코드                                         │
│     ├─ Java 17 & Gradle 설정 (캐싱 포함)                      │
│     ├─ application.properties (Local) 제거                   │
│     ├─ GitHub Secrets에서 설정 파일 주입                      │
│     ├─ ./gradlew build -x test (테스트 생략)                 │
│     ├─ AWS 자격증명 설정                                     │
│     └─ Docker Image (arm64) 생성 & ECR에 Push                │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│  3. GitHub Actions CD: 배포 번들 준비                        │
│     ├─ appspec.yml (CodeDeploy 설정)                        │
│     ├─ scripts/deploy.sh (Docker 기반 배포)                  │
│     ├─ scripts/nginx_setup.sh (Nginx 설정)                  │
│     ├─ IMAGE_URI 파일 (ECR 이미지 경로 저장)                 │
│     └─ 위 파일들을 ZIP으로 압축하여 S3에 업로드                │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│  4. AWS CodeDeploy: 배포 명령 전달                           │
│     ├─ S3에서 배포 번들 다운로드                              │
│     ├─ CodeDeploy Agent에 배포 실행 명령                     │
│     └─ appspec.yml 기반 배포 프로세스 시작                    │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│  5. EC2에서 배포 실행 (deploy.sh)                            │
│     ├─ AWS ECR 로그인 (임시 토큰 발급)                        │
│     ├─ IMAGE_URI 읽어오기 (aws account + commit SHA)         │
│     ├─ ECR에서 Docker Image Pull                             │
│     ├─ 기존 Container 중지 & 삭제                            │
│     ├─ 새 Container 실행                                    │
│     └─ Application 시작 (Port 8080)                         │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│  6. 배포 완료                                                │
│     ├─ Nginx: localhost:127.0.0.1:8080 로 리버스 프록시      │
│     ├─ RDS MySQL: 데이터 영속성 관리                         │
│     └─ Swagger UI: https://<EC2_IP>/swagger-ui로 확인       │
└─────────────────────────────────────────────────────────────┘
```
**CI/CD 구축으로 얻는 이점 :** <BR>
완성된 기능을 EC2환경에서 테스트 하고 프론트 분들과 공유하기 위해선
프로젝트 개발 중 **반복적으로 배포**가 이루어져야 합니다 <BR><BR>
CI/CD를 한 번 구축해 놓으면 **지정한 branch에 PR을 발생시킬 때마다
자동으로 통합 - 배포**가 이루어지기에 편리합니다 <BR><BR>
또한 CI/CD는 **사람이 직접 배포할 때 발생할 수 있는 실수를 예방**하며,
모든 **팀원이 배포 과정을 숙지하고 있지 않아도 되기에** 분업화에도 탁월합니다.



## 📊 성능 테스트 (K6)

### 개요

K6를 활용한 부하 테스트를 통해 애플리케이션의 성능 목표를 정의하고 검증했습니다.

**성능 목표:**
- **LCP (Largest Contentful Paint)**: 3초 이하 (3초를 초과하면 50% 유저가 이탈)
- **목표 RPS**: 100 (요청/초)
- **목표 응답시간**: 2초 이하 (p95 기준)

### 부하 테스트 전략

**테스트 대상 API:**
1. `GET /v1/items/{item_id}` - 상품 상세 조회
2. `GET /v1/items?keyword=&page=0&size=10&sort=price` - 상품 목록 검색
3. `GET /v1/orders/address` - 배송 주소 목록 조회 (**로그인 필요**)

**VUser 계산:**
```
VUser = 목표 RPS × (응답시간 + thinking time)
VUser = 100 × 1 = 100명 (응답시간 1초, thinking time 0초 가정)
```

### 테스트 결과 (기본 + 극한)

**🧊 Test 1: 기본 부하 테스트 (VUser 100명)**

| 지표 | 결과 |
|:-:|:-:|
| **성공률** | 100% (11,930개 요청) |
| **평균 응답시간** | 491.64ms |
| **p(95) 응답시간** | 1.28s ✓ |
| **최대 응답시간** | 3.02s |
| **RPS (처리량)** | 202.2 req/s |
| **실패율** | 0% |

**결과 해석**: VUser 100명 기준 모든 Threshold 통과 (p95 응답시간 2초 이하)

---

**🔥 Test 2: 극한 부하 테스트 (VUser 100→500 Ramp)**

| 구간 | VUser | 결과 |
|:-:|:-:|:-:|
| **1단계: Warm-up (0→100)** | 0~100 | 안정적 |
| **2단계: Baseline** | 100 | p95=1.28s ✓ |
| **3단계: 중부하** | 100→200 | 안정적 |
| **4단계: 고부하** | 200→300 | 안정적 |
| **5단계: 극한** | 300→400 | ⚠️ Timeout 시작 (34개 요청) |
| **6단계: 피크** | 400→500 | ⚠️ Timeout 증가 (41개 요청) |

**종합 결과:**
- **총 요청**: 101,388개 처리
- **성공률**: 99.96% (41개 실패)
- **평균 응답시간**: 737ms
- **p(95) 응답시간**: 2.38s
- **RPS**: 294 req/s
- **병목 API**: Address API (VUser 400 이상에서 부하)

### 서버 용량 분석

| VUser 범위 | 상태 | 권장 사항 |
|:-:|:-:|:-:|
| **0~350 VU** | ✅ 매우 안정적 | 정상 운영 |
| **350~400 VU** | ✅ 안정적 | 권장 최대값 |
| **400~500 VU** | ⚠️ 부하 증가 | 부분 Timeout |
| **500+ VU** | ❌ 과부하 | 권장하지 않음 |

**실제 사용자 기준:**
- **권장 동시 사용자**: 350~400명 (실제 사용자 600~700명)
- **피크 트래픽**: 최대 500명 (이상 상태 발생)

### 병목 분석 및 개선 방향

**식별된 병목:**

1. **Address API (주소 목록 조회)**
   - VUser 400 이상에서 급격한 성능 저하

2. **Search API (상품 검색)**
   - VUser 400 이상에서 일부 Timeout
   - 안정적 구간: VUser 350 이하

3. **Detail API (상품 상세 조회)**
   - 가장 안정적인 API
   - 모든 부하 수준에서 99%+ 성공률

**추후 개선 사항:**
1. Address API 쿼리 최적화 (캐싱 또는 인덱싱)
2. DB 연결 풀 튜닝 (HikariCP 설정)
3. RDS 성능 향상 (Read Replica 추가)
4. DataDog (APM 분석) 사용하여 모니터링 고도화



<br/>

## 🐝 팀 소개

### TEAM Carhartt




| 이름 |                                        김운강                                        | 김희수 | 정동희 | 장태규 | 수빈 |
|:-:|:---------------------------------------------------------------------------------:|:-:|:-:|:-:|:-:|
| 사진 |  <p align="center"><img src="image/woonkim.png" width="130" height="160" /></p>   | [사진] | [사진] | [사진] | [사진] |
| 역할 |                                        BE                                         | BE | BE | FE | FE |
| GitHub | [@woonkim0413](https://github.com/woonkim0413/Carhartt_used_transactions_backend) | [링크] | [링크] | [링크] | [링크] |

<br/>

