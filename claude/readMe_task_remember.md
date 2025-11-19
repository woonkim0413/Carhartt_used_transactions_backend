# README 작성 기준 문서 (HoneyFlow 양식 기반)

> 이 문서는 `@claude/request.md`를 분석하여 작성된 README 작성 기준입니다.
> 앞으로 README.md 작성 및 수정 작업은 이 문서를 기반으로 진행됩니다.

---

## 📋 README 전체 구조

```
1. 프로젝트 헤더 (제목 + 부제 + 설명)
   └─ 프로젝트 로고/이미지

2. 바로가기 섹션 (링크 모음)
   └─ 테이블 형식: [아이콘 텍스트](URL)

3. 기술 스택 섹션
   └─ Badge 형식: ![tech](https://img.shields.io/badge/...)

4. 프로젝트 개요 섹션
   └─ 개요 이미지만 표시 (설명 제거, claude/img_1.png 사용)

5. 주요 기능 소개
   ├─ 기능 1 (제목 + 설명 + 스크린샷)
   ├─ 기능 2
   ├─ 기능 3
   └─ ...

6. 해결 경험 / 기술 선택 근거
   ├─ 핵심 기술 1 (설명 + 상세 링크)
   ├─ 핵심 기술 2
   └─ ...

7. 디렉토리 구조
   └─ Tree 형식의 폴더 구조

8. 인프라 아키텍처 구조
   └─ 아키텍처 이미지

9. 팀 소개
   └─ 팀원 테이블 (이름/사진/역할/GitHub)

10. (선택사항) 추가 섹션
    ├─ 로컬 개발 시작
    ├─ 라이선스
    └─ ...
```

---

## 🔍 각 섹션 상세 정보

### 1️⃣ 프로젝트 헤더

**구조:**
```markdown
# 프로젝트명

> 영문 부제
>
> 한글 설명

<div align="center">
  <img src="이미지URL" width="512px" />
</div>
```

**Carhartt C_Platform 예시:**
```markdown
# Carhartt C_Platform

> Second-hand Marketplace Platform
>
> 칼하트 중고 마켓플레이스 플랫폼

<div align="center">
  <img src="프로젝트_로고_이미지" width="512px" />
</div>
```

---

### 2️⃣ 바로가기 섹션 (🔗)

**구조:**
```markdown
<div align="center">

### 🔗 바로가기

|[아이콘 텍스트](URL)|[아이콘 텍스트](URL)|[아이콘 텍스트](URL)|[아이콘 텍스트](URL)|
|:-:|:-:|:-:|:-:|

</div>
```

**HoneyFlow 예시:**
```markdown
|[📜 노션](URL)|[🎨 피그마](URL)|[📚 위키](URL)|[🍯 배포 주소](URL)|
|:-:|:-:|:-:|:-:|
```

**Carhartt 예시:**
```markdown
|[📋 요구사항](URL)|[🎨 설계](URL)|[📚 API문서](URL)|[🐙 GitHub](URL)|
|:-:|:-:|:-:|:-:|
```

---

### 3️⃣ 기술 스택 섹션 (💻)

**구조:**
```markdown
<div align="center">

### 💻 기술 스택

[뱃지1] [뱃지2] [뱃지3]

[뱃지4] [뱃지5] [뱃지6]

[뱃지7] [뱃지8] [뱃지9]

</div>
```

**뱃지 형식:**
```markdown
[![기술명](https://img.shields.io/badge/기술명-색상코드?logo=로고명&logoColor=fff)](#)
```

**Carhartt 예시 (백엔드 중심):**
```markdown
### 💻 기술 스택

[![Java](https://img.shields.io/badge/Java%2017-ED8B00?logo=java&logoColor=fff)](#)
[![SpringBoot](https://img.shields.io/badge/Spring%20Boot%203.5.4-6DB33F?logo=springboot&logoColor=fff)](#)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?logo=springsecurity&logoColor=fff)](#)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?logo=mysql&logoColor=fff)](#)

[![H2Database](https://img.shields.io/badge/H2-004B87?logo=h2&logoColor=fff)](#)
[![AWS S3](https://img.shields.io/badge/AWS%20S3-FF9900?logo=amazons3&logoColor=fff)](#)
[![Gradle](https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=fff)](#)

[![OAuth2](https://img.shields.io/badge/OAuth2-007C8C?logo=oauth&logoColor=fff)](#)
[![KakaoPay](https://img.shields.io/badge/Kakao%20Pay-FFE812?logo=kakao&logoColor=000)](#)
[![NaverPay](https://img.shields.io/badge/Naver%20Pay-00C73C?logo=naver&logoColor=fff)](#)
```

---

### 4️⃣ 프로젝트 개요 섹션 (🛍️)

**구조:**
```markdown
## 🛍️ 프로젝트 개요

**프로젝트명**은 [핵심 설명]입니다.

[배경 및 문제상황]

**프로젝트명**은 [해결방안]을 제공합니다!

<div align="center">
  <img src="개요_이미지" width="512px" />
</div>
```

**Carhartt 예시 (수정된 형식):**
```markdown
## 🛍️ 프로젝트 개요

<div align="center">
  <img src="claude/img_1.png" width="512px" />
</div>
```

**설명:**
- 프로젝트 개요 섹션은 간단하게 이미지만 표시
- 프로젝트 설명(핵심, 배경, 해결방안)은 헤더 바로 아래에 위치
- 이미지는 `claude/img_1.png` 경로 사용 (request.md의 프로젝트 개요 사진)

---

### 5️⃣ 주요 기능 소개 섹션 (🐝 주요 기능 소개)

**구조:**
```markdown
## 🐝 주요 기능 소개

**프로젝트명**은 [핵심 설명]입니다.

[이제 소개 멘트]

### 기능명 1

> 기능 설명

<img alt="기능명" src="스크린샷URL" width="512" />

### 기능명 2

> 기능 설명

<img alt="기능명" src="스크린샷URL" width="512" />
```

**Carhartt 예시:**
```markdown
## 🐝 주요 기능 소개

**Carhartt C_Platform**은 정확한 사이즈 정보와 실측 사진을 통해
안전한 중고 거래를 실현하는 플랫폼입니다.

이제 Carhartt C_Platform의 주요 기능들을 소개할게요!

### 사이즈 실측 정보 관리

> 상품의 정확한 실측 사이즈를 필수로 등록하여 구매자에게 정보를 제공합니다.

<img alt="Size Info" src="스크린샷URL" width="512" />

### 판매자/구매자 내역 관리

> 판매 및 구매 거래 내역을 체계적으로 관리합니다.

<img alt="Transaction History" src="스크린샷URL" width="512" />
```

---

### 6️⃣ 해결 경험 / 기술 선택 근거 (🐝 우리만의 해결 경험)

**구조:**
```markdown
## 🐝 우리만의 해결 경험

### 주제 1: [기술선택/문제해결]

[설명 문장]

- [📌 상세 링크 1](URL)
- [📌 상세 링크 2](URL)

### 주제 2: [기술선택/문제해결]

[설명 문장]

- [📌 상세 링크 1](URL)
- [📌 상세 링크 2](URL)
```

**Carhartt 예시:**
```markdown
## 🐝 우리만의 해결 경험

### 사이즈 정보 표준화

칼하트의 다양한 사이즈 규격을 표준화하고 사용자 편의를 위해
카테고리별 사이즈 정보를 구조화했습니다.

- [📌 사이즈 정보 표준화 설계](URL)
- [📌 데이터베이스 구조 최적화](URL)

### Spring Security를 활용한 인증 시스템

OAuth2와 로컬 인증을 결합하여 안전하고 유연한 인증 체계를 구축했습니다.

- [📌 OAuth2 통합 설계](URL)
- [📌 로컬 인증 구현 가이드](URL)
```

---

### 7️⃣ 디렉토리 구조 섹션 (🐝 디렉토리 구조)

**구조:**
```markdown
## 🐝 디렉토리 구조

\`\`\`
📂 프로젝트폴더/
├── 📂 폴더1/               설명
│   ├── 📂 하위폴더1/
│   ├── 📄 파일1
│   └── 📄 파일2
├── 📂 폴더2/               설명
└── 📄 파일3
\`\`\`
```

**Carhartt 예시:**
```markdown
## 🐝 디렉토리 구조

\`\`\`
📂 C_platform/
├── 📂 src/main/java/com/C_platform/
│   ├── 📂 Member_woonkim/      사용자 인증 관리
│   │   ├── domain/              엔티티, Value Objects
│   │   ├── application/         UseCase
│   │   ├── presentation/        컨트롤러, DTO
│   │   └── infrastructure/      Repository, OAuth
│   ├── 📂 item/                 상품 카탈로그
│   ├── 📂 order/                주문 관리
│   ├── 📂 payment/              결제 처리
│   ├── 📂 config/               설정
│   ├── 📂 global/               전역 유틸리티
│   └── 📂 exception/            예외 처리
├── 📂 src/main/resources/
│   ├── application.properties   기본 설정
│   ├── application-oauth2-local.yml
│   └── messages.properties      메시지
└── build.gradle                 빌드 설정
\`\`\`
```

---

### 8️⃣ 인프라 아키텍처 구조 (🐝 인프라 아키텍처 구조)

**구조:**
```markdown
## 🐝 인프라 아키텍처 구조

<img width="747" alt="아키텍처" src="아키텍처이미지URL">
```

**또는 텍스트 다이어그램:**
```markdown
## 🐝 인프라 아키텍처 구조

\`\`\`
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
┌──────▼──────────────────────┐
│   Spring Boot Application   │
│  ├─ OAuth2 (Kakao, Naver)  │
│  ├─ Local Auth              │
│  └─ Payment (KakaoPay)      │
└──────┬──────────────────────┘
       │
┌──────▼──────┐    ┌──────────────┐
│   MySQL     │    │   AWS S3     │
│ (Prod)      │    │ (Image Store)│
└─────────────┘    └──────────────┘
\`\`\`
```

---

### 9️⃣ 팀 소개 섹션 (🐝 팀 소개)

**구조 1: 테이블 형식 (이름 + 사진 + 역할 + GitHub)**

```markdown
## 🐝 팀 소개

### TEAM 팀명

| 이름 | 팀원1 | 팀원2 | 팀원3 |
|:-:|:-:|:-:|:-:|
| 사진 | ![image1](URL) | ![image2](URL) | ![image3](URL) |
| 역할 | BE | FE | FE |
| GitHub | [@계정](URL) | [@계정](URL) | [@계정](URL) |
```

**Carhartt 예시 (수정된 형식):**
```markdown
## 🐝 팀 소개

### TEAM Carhartt

| 이름 | 김운강 | 김희수 | 정동희 | 장태규 | 수빈 |
|:-:|:-:|:-:|:-:|:-:|:-:|
| 사진 | ![kim-ungang](URL) | [사진] | [사진] | [사진] | [사진] |
| 역할 | BE | BE | BE | FE | FE |
| GitHub | [@woonkim0413](https://github.com/woonkim0413) | [링크] | [링크] | [링크] | [링크] |
```

---

## 📊 README 작성 체크리스트

작업을 시작하기 전에 다음을 확인하세요:

```
□ 1. 프로젝트 헤더 (제목 + 로고 이미지)
□ 2. 바로가기 섹션 (4개 링크 이상)
□ 3. 기술 스택 섹션 (뱃지 형식)
□ 4. 프로젝트 개요 (배경 + 설명 + 개요 이미지)
□ 5. 주요 기능 소개 (3개 이상의 기능 + 스크린샷)
□ 6. 해결 경험/기술 선택 근거 (2개 이상의 주제)
□ 7. 디렉토리 구조 (Tree 형식)
□ 8. 인프라 아키텍처 (이미지 또는 다이어그램)
□ 9. 팀 소개 (팀원 테이블)
□ 10. 추가 섹션 (필요시)
```

---

## 🎯 작성 시 주의사항

### 이미지 경로
- **로컬 파일**: `![이름](../경로/파일명)` 형식
- **원격 URL**: `![이름](https://github.com/...)`형식
- **권장 크기**: 512px (메인), 384px (보조)

### 마크다운 문법
- `<div align="center">` : 중앙 정렬
- `|:-:|:-:|:-:|` : 테이블 중앙 정렬
- `<br/>` : 줄 간격 추가
- 뱃지는 shields.io 사용

### 한글 인코딩
- 파일 저장 시 **UTF-8** 인코딩 필수
- 모든 한글이 정상 표시되는지 확인

### 링크 형식
- `[텍스트](URL)` 형식 준수
- 내부 링크는 상대 경로 사용
- 외부 링크는 절대 URL 사용

---

## 📝 작성 예시 (Carhartt 기본 틀)

```markdown
# Carhartt C_Platform

> Second-hand Marketplace Platform
>
> 칼하트 중고 마켓플레이스 플랫폼

<div align="center">
  <img src="이미지경로" width="512px" />
</div>

<br/>

<div align="center">

### 🔗 바로가기

|[📋 요구사항](#)|[🎨 설계](#)|[📚 API](#)|[🐙 GitHub](#)|
|:-:|:-:|:-:|:-:|

</div>

<br/>

<div align="center">

### 💻 기술 스택

[뱃지들...]

</div>

<br/>

## 🛍️ 프로젝트 개요

[프로젝트 설명 및 이미지]

<br/>

## 🐝 주요 기능 소개

[기능별 설명 및 스크린샷]

<br/>

## 🐝 우리만의 해결 경험

[기술 선택 근거 및 링크]

<br/>

## 🐝 디렉토리 구조

[폴더 구조]

<br/>

## 🐝 팀 소개

[팀원 정보 테이블]
```

---

## 🔗 참고 자료

- **원본 양식**: HoneyFlow GitHub README
  - https://github.com/boostcampwm-2024/web29-honeyflow
- **배지 생성**: shields.io
  - https://shields.io
- **마크다운 가이드**: GitHub Markdown
  - https://docs.github.com/en/get-started/writing-on-github

---

**마지막 업데이트**: 2025-11-19 (프로젝트 개요 섹션 간소화, 이미지 경로 변경)
**작성자**: Claude Code
**용도**: Carhartt C_Platform README 작성 기준

## 📌 최근 변경 사항 (2025-11-19)

### README.md 구조 변경
- ✅ 프로젝트 개요 섹션 이전의 모든 설명 내용 제거
- ✅ 프로젝트 개요 섹션을 이미지만 표시하도록 간소화
- ✅ 프로젝트 설명(핵심, 배경, 해결방안)은 헤더 아래로 이동
- ✅ 이미지 경로를 `claude/img_1.png`로 통일 (request.md의 프로젝트 개요 사진)

### readMe_task_remember.md 업데이트
- ✅ 섹션 4️⃣ (프로젝트 개요) 예시 코드 수정
- ✅ 전체 구조에서 "개요 설명" 제거 및 "이미지만 표시" 명시
- ✅ Carhartt 예시를 새로운 형식으로 반영

---

## 📌 최근 변경 사항 (2025-11-19 - Phase 2)

### 1️⃣ 시스템 아키텍처 섹션 확장
- ✅ **환경별 설정 분리** 추가
  - 로컬 vs 프로덕션 설정 파일 구조 명시
  - Spring Profiles를 활용한 활성화 방식 설명
  - JVM 인자와 환경변수 우선순위 명확화

- ✅ **인프라 계층 구조** 추가
  - 로컬 개발 환경 다이어그램
  - 프로덕션 환경 전체 아키텍처 (Nginx, EC2, Docker, RDS, ECR)
  - 각 구성 요소별 역할과 사양 상세 설명

### 2️⃣ CI/CD 배포 구조 전체 교체
- ✅ **6단계 배포 파이프라인** 시각화
  ```
  1. Git Push to main Branch
  2. GitHub Actions CI: 빌드 & Docker Image 생성 & ECR Push
  3. GitHub Actions CD: 배포 번들 준비 (appspec.yml, deploy.sh)
  4. AWS CodeDeploy: 배포 명령 전달
  5. EC2 배포 실행 (deploy.sh - Docker 컨테이너 배포)
  6. 배포 완료 (Nginx, RDS, Swagger)
  ```

- ✅ **주요 변경 사항 테이블**
  - 이전 구조 vs 현재 구조 비교 (Docker/ECR 도입)
  - 빌드 산출물, 저장소, 배포 방식, 데이터베이스 변경 명확화

- ✅ **환경별 설정 테이블 개선**
  - Local vs Production 차이점 일목요연 정리
  - Docker, Nginx, 배포 방식 추가 비교

- ✅ **설정 파일 목록 업데이트**
  - GitHub Actions 워크플로우, CodeDeploy 설정, 배포 스크립트 추가

### 3️⃣ 성능 테스트 (K6) 섹션 신규 추가
- ✅ **개요** 섹션
  - LCP, RPS, 응답시간 성능 목표 명시

- ✅ **부하 테스트 전략**
  - 테스트 대상 3가지 API 선정
  - VUser 계산 공식 및 설명

- ✅ **테스트 결과** (2가지 테스트)
  - **Test 1**: VUser 100명 기본 테스트
    - 성공률 100% (11,930개 요청)
    - p95 응답시간: 1.28s ✓
  - **Test 2**: VUser 100→500 극한 테스트
    - 총 101,388개 요청 처리
    - 성공률 99.96% (41개 실패)
    - p95 응답시간: 2.38s

- ✅ **서버 용량 분석**
  - VUser 범위별 상태 및 권장사항 테이블
  - 실제 사용자 기준 수치 (600~700명 권장)
  - VUser 350~400명 안정적, 400명 이상 부하

- ✅ **병목 분석 및 개선 방향**
  - 각 API별 병목 현황 분석
  - Address API가 주 병목 (개선 우선순위 1순위)
  - DB 연결 풀 튜닝, RDS 성능 향상 제시

- ✅ **K6 설치 및 사용**
  - WSL2 설치 방법 (단계별)
  - 기본 테스트 실행 명령어

- ✅ **성능 모니터링 권장사항**
  - 정기적 테스트 일정 (매 배포, 월 1회, 필요시)
  - 추가 모니터링 도구 제시 (InfluxDB, Grafana, CloudWatch)

---

## 📝 README.md 최종 구조 (2025-11-19 업데이트)

```
1. 프로젝트 헤더
2. 바로가기 섹션
3. 기술 스택
4. 프로젝트 개요
5. 핵심 기능
6. 🏗️ 시스템 아키텍처 (EXPANDED)
   ├─ 아키텍처 패턴
   ├─ 계층 구조
   ├─ 환경별 설정 분리 (NEW)
   └─ 인프라 계층 구조 (NEW)
7. 🚀 CI/CD 배포 구조 (UPDATED)
   ├─ 배포 파이프라인 (6단계, NEW)
   ├─ 주요 변경 사항 (UPDATED)
   ├─ 환경별 설정 (UPDATED)
   └─ 설정 파일 (UPDATED)
8. 📊 성능 테스트 (K6) (NEW)
   ├─ 개요
   ├─ 부하 테스트 전략
   ├─ 테스트 결과 (Test 1, Test 2)
   ├─ 서버 용량 분석
   ├─ 병목 분석 및 개선 방향
   ├─ K6 설치 및 사용
   └─ 성능 모니터링 권장사항
9. 팀 소개
```

---

## 🎯 다음 작업 항목 (예정)

### Phase 3: 추가 섹션 및 완성도 개선
- [ ] 🔗 바로가기 섹션에 API 문서, 설계 문서 링크 추가
- [ ] 💻 기술 스택 뱃지 추가 (Docker, AWS ECR, K6 등)
- [ ] 🐝 주요 기능 소개 섹션 상세화 (스크린샷 추가)
- [ ] 🐝 우리만의 해결 경험 섹션 추가
  - Spring Security + OAuth2 통합 설계
  - Email 인증 시스템 구현
  - Password Recovery 기능 구현
  - RestTemplate Bean 설정 최적화
- [ ] 로컬 개발 시작 가이드 추가
- [ ] 라이선스 정보 추가

### Phase 4: 고급 기능 문서화
- [ ] API 사용 예시 추가
- [ ] 배포 트러블슈팅 가이드 작성
- [ ] 성능 최적화 팁 추가 (JVM 튜닝, DB 인덱싱)
- [ ] 모니터링 및 로깅 설정 가이드

---

**마지막 업데이트**: 2025-11-19 Phase 2 완료
**업데이트 내용**: 시스템 아키텍처 확장, CI/CD 파이프라인 6단계 상세화, 성능 테스트 섹션 신규 추가
