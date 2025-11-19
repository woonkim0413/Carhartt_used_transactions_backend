# README.md 수정 가이드

## 📋 목차
1. [현재 상태 분석](#현재-상태-분석)
2. [수정 필요 부분](#수정-필요-부분)
3. [상세 수정 내용](#상세-수정-내용)
4. [⚠️ 추가 필요 섹션](#⚠️-추가-필요-섹션)
5. [🎯 최우선 수정 항목](#🎯-최우선-수정-항목-순서대로)
6. [최종 체크리스트](#최종-체크리스트)
7. [🚀 빠른 시작](#🚀-빠른-시작-3단계)
8. [추가 정보](#추가-정보)

---

## 현재 상태 분석

### 현재 README.md의 문제점

README.md의 **"🏗️ 시스템 아키텍처"** 와 **"🚀 현재 CI/CD 배포 구조"** 섹션이 실제 프로젝트 상황을 반영하지 못하고 있습니다.

**세 참고 파일에서 확인된 변화:**

1. **separate_propertiesFile.md** - 환경별 설정 분리 (로컬 vs 프로덕션)
2. **carharrt_infra_change.md** - 인프라 구조 변경 (Docker, RDS, ECR 도입)
3. **cicd_using_gitactions.md** - CI/CD 파이프라인 상세 (GitHub Actions + CodeDeploy)

---

## 수정 필요 부분

### 1️⃣ 🏗️ 시스템 아키텍처 섹션

**현재 상태:** 기본적인 계층 구조만 설명 (변경 불필요)

**추가 필요 정보:**
- 환경별 설정 분리 전략 (Local vs Production)
- 프로퍼티 파일 구조
- Docker 기반 배포 구조

---

### 2️⃣ 🚀 현재 CI/CD 배포 구조 섹션

**현재 상태:** 과거 구조 (`.jar` 파일 S3 전달 방식)

**실제 구조:**
- CI: GitHub Actions + Gradle build
- 빌드 산출물: `.jar` → Docker Image (ECR에 저장)
- CD: AWS CodeDeploy Agent
- 배포 대상: EC2 (Docker Container로 실행)
- DB: RDS MySQL (EC2와 별도)

---

## 상세 수정 내용

### 섹션 1: 시스템 아키텍처에 추가할 내용

#### 1.1 환경별 설정 분리

```markdown
### 환경별 설정 분리

프로젝트는 **로컬(Local)** 과 **프로덕션(Production)** 환경을 **논리적으로** 분리합니다:

**설정 파일 구조:**
```
src/main/resources/
├── application.properties           # 공통 설정 (프로필 활성화 정의)
├── application-local.properties     # 로컬 환경 전용
├── application-oauth2-local.yml     # 로컬 OAuth2 설정
├── application-oauth2-prod.yml      # 프로덕션 OAuth2 설정
└── application-dev.properties       # 프로덕션 환경 전용
```

**작동 원리:**
- `application.properties`에 기본값: `spring.profiles.active=local` 설정
- 런타임에 JVM 인자로 `-Dapp.env=prod` 전달하면 프로덕션 설정으로 전환
- Spring이 `ConfigurableEnvironment`를 생성하며 우선순위에 따라 값 결정:
  1. JVM 시스템 속성 (`-D` 옵션)
  2. 환경 변수
  3. `.properties` / `.yml` 파일

**CI/CD 에서의 적용:**
- 로컬 실행: 기본 설정 사용
- 프로덕션 배포: GitHub Actions에서 `-Dapp.env=prod` 전달
```

#### 1.2 인프라 계층 추가

```markdown
### 인프라 계층 구조

**로컬 개발 환경:**
```
┌─────────────────────────────────────┐
│     Spring Boot Application         │
│  (Port 8080, H2 In-Memory DB)       │
└─────────────────────────────────────┘
```

**프로덕션 환경:**
```
┌────────────────────────────────────────────────────────┐
│                    Nginx (Reverse Proxy)               │
│              (Port 80/443 → 8080 forwarding)           │
└──────────────────────┬─────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
   ┌────────────────────────────────────┐
   │  AWS EC2 Instance (t4g.small)      │
   │  ├─ Docker Engine                  │
   │  │  └─ Spring Boot Container       │
   │  │     (Port 8080)                 │
   │  └─ AWS CLI v2                     │
   └─────────────┬──────────────────────┘
                 │
        ┌────────┴────────────────┐
        ▼                         ▼
   ┌─────────────┐          ┌──────────────────┐
   │  AWS RDS    │          │  AWS S3          │
   │  MySQL 8.0  │          │  (Product Images)│
   │             │          │  (Pre-signed URL)│
   └─────────────┘          └──────────────────┘

        별도 구성:
   ┌────────────────────────┐
   │  AWS ECR               │
   │ (Docker Image Registry)│
   └────────────────────────┘
```

**주요 구성 요소:**
- **EC2**: t4g.small (vCPU 2개, RAM 2GB)
  - 네트워크 대역폭: 5Gbps
  - 유휴 상태 CPU 크래딧 적립

- **Docker**: 애플리케이션 컨테이너화
  - 기본 이미지: `eclipse-temurin:17-jre-jammy`
  - 빌드 이미지: `gradle:8.9-jdk17`
  - 다중 스테이지 빌드로 최종 이미지 크기 최소화

- **RDS MySQL**: 데이터 영속성
  - 보안 그룹으로 EC2만 접근 허가
  - SSL 연결 지원 (`ssl-mode=REQUIRED`)
  - Parameter Group으로 문자 인코딩 설정 (UTF-8MB4, UTC Timezone)

- **AWS ECR**: Docker Image 저장소
  - GitHub Actions에서 빌드한 이미지 저장
  - EC2에서 이미지 Pull하여 배포
```

### 섹션 2: 현재 CI/CD 배포 구조 전체 교체

#### 2.1 최신 배포 파이프라인

```markdown
### 배포 파이프라인 (Docker + ECR + RDS 기반)

**전체 흐름:**

```
┌─────────────────────────────────────────────────────────────┐
│  1. Git Push to main Branch                                 │
│     (GitHub에서 CI/CD 워크플로우 트리거)                     │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│  2. GitHub Actions CI: 빌드 & 컨테이너 준비                  │
│     ├─ Checkout 코드                                        │
│     ├─ Java 17 & Gradle 설정 (캐싱 포함)                    │
│     ├─ application.properties 제거                          │
│     ├─ GitHub Secrets에서 설정 파일 주입                   │
│     ├─ ./gradlew build -x test (테스트 생략)               │
│     ├─ [NEW] AWS 자격증명 설정                             │
│     └─ [NEW] Docker Image 생성 & ECR에 Push               │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│  3. GitHub Actions CD: 배포 번들 준비                        │
│     ├─ appspec.yml (CodeDeploy 설정)                       │
│     ├─ scripts/deploy.sh (Docker 기반 배포)                │
│     ├─ scripts/nginx_setup.sh (Nginx 설정)                 │
│     ├─ IMAGE_URI 파일 (ECR 이미지 경로 저장)               │
│     └─ 위 파일들을 ZIP으로 압축하여 S3에 업로드            │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│  4. AWS CodeDeploy: 배포 명령 전달                          │
│     ├─ S3에서 배포 번들 다운로드                             │
│     ├─ CodeDeploy Agent에 배포 실행 명령                   │
│     └─ appspec.yml 기반 배포 프로세스 시작                  │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│  5. EC2에서 배포 실행 (deploy.sh)                           │
│     ├─ AWS ECR 로그인 (임시 토큰 발급)                     │
│     ├─ IMAGE_URI 읽어오기                                  │
│     ├─ ECR에서 Docker Image Pull                          │
│     ├─ 기존 Container 중지 & 삭제                         │
│     ├─ 새 Container 실행                                   │
│     └─ Application 시작 (Port 8080)                        │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│  6. 배포 완료                                                 │
│     ├─ Nginx: localhost:127.0.0.1:8080 로 리버스 프록시    │
│     ├─ RDS MySQL: 데이터 영속성 관리                        │
│     └─ Swagger UI: http://<EC2_IP>:8080/swagger-ui/       │
└─────────────────────────────────────────────────────────────┘
```

#### 2.2 주요 변경 사항

| 구분 | 이전 구조 | 현재 구조 |
|:-:|:-:|:-:|
| **빌드 산출물** | `.jar` 파일 | Docker Image |
| **저장소** | S3 버킷 | AWS ECR (Elastic Container Registry) |
| **배포 방식** | 직접 `.jar` 실행 | Docker Container 실행 |
| **데이터베이스** | EC2 내 MySQL Container | RDS MySQL (별도 인스턴스) |
| **배포 번들** | `.jar` 포함 전체 repo | appspec.yml + 스크립트만 |
| **배포 스크립트** | `deploy.sh` (`.jar` 실행) | `deploy.sh` (Docker 명령어) |

#### 2.3 각 단계별 상세 설명

**[GitHub Actions CI 단계]**

```markdown
**1. Gradle 빌드**
- 명령어: `./gradlew build -x test`
- 테스트 생략으로 빌드 시간 단축
- 결과물: `build/libs/app.jar` (고정 이름)

**2. Docker Image 빌드 & ECR Push**
- Dockerfile 실행:
  - 빌드 스테이지: Gradle로 `.jar` 컴파일
  - 실행 스테이지: 경량 JRE 이미지에 `.jar` 복사
  - 최종 이미지: 약 300-400MB
- ECR 레지스트리에 Push:
  - 이미지명: `{AWS_ACCOUNT}.dkr.ecr.ap-northeast-2.amazonaws.com/c-platform:${COMMIT_SHA}`
  - 태그: GitHub commit SHA 사용 (배포 추적용)
```

**[AWS CodeDeploy 단계]**

```markdown
**appspec.yml 역할:**
- `files`: S3 -> EC2 파일 복사 규칙
  ```yaml
  files:
    - source: /
      destination: /home/ubuntu/carhartt_platform
      overwrite: true
  ```
- `hooks`: 배포 생명주기 단계별 실행 스크립트
  ```yaml
  hooks:
    AfterInstall:
      - location: scripts/deploy.sh
        timeout: 60
        runas: root
  ```

**deploy.sh 흐름:**
1. `IMAGE_URI` 파일에서 ECR 이미지 경로 읽기
2. `aws ecr get-login-password` → Docker 로그인
3. `docker pull {IMAGE_URI}` → 최신 이미지 다운로드
4. `docker rm -f carhartt-platform` → 기존 컨테이너 중지/삭제
5. `docker run -d --restart=always -p 8080:8080` → 새 컨테이너 시작
```

#### 2.4 환경 변수 및 설정 주입

```markdown
**GitHub Secrets에 저장되는 정보:**
- `APPLICATION_PROPERTIES`: 프로덕션 설정 파일 내용
  - Spring Datasource (MySQL RDS 접속 정보)
  - OAuth2 Provider 자격증명 (Kakao, Naver)
  - AWS S3 버킷 정보

**AWS CodeDeploy 과정:**
- Secrets에서 주입된 설정으로 `.jar` 생성
- `.jar` → Docker Image로 변환
- 배포 시점에 Image 환경변수 전달:
  ```bash
  docker run -e SPRING_PROFILES_ACTIVE=prod ...
  ```

**데이터베이스 연결:**
- RDS MySQL 엔드포인트: `carharttdb.cl4eesq4iow3.ap-northeast-2.rds.amazonaws.com`
- 접속: Private IP (EC2와 같은 VPC 내)
- 보안: Security Group으로 EC2만 허가
- SSL: `serverTimezone=UTC&sslMode=REQUIRED`
```

#### 2.5 배포 모니터링

```markdown
**배포 상태 확인:**

```bash
# EC2에서 Container 상태 확인
sudo docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'

# Application 로그 실시간 모니터링
sudo docker logs -f carhartt-platform

# Container 내부 접속 (디버깅)
docker exec -it carhartt-platform bash

# 8080 포트 점유 프로세스 확인
sudo lsof -i :8080
```

**주의사항:**
- 배포 후 로그에서 `Hibernate: ddl-auto=update` 설정 확인
- 이전에 `create` 또는 `create-drop`으로 설정했다면 데이터 손실 위험
- RDS Snapshot으로 정기적 백업 (`mysqldump` 권장)
```

#### 2.6 환경별 설정 비교

```markdown
### 환경별 상세 설정

| 항목 | Local | Production |
|:-:|:-:|:-:|
| **DB** | H2 In-Memory | MySQL RDS |
| **DB 포트** | 내장 | 3306 |
| **S3** | Disabled | Enabled |
| **OAuth2** | Local 설정 | Prod 설정 |
| **Docker** | X | Container (ECR Image) |
| **Nginx** | X | Reverse Proxy |
| **배포 방식** | `./gradlew bootRun` | Docker + CodeDeploy |
| **프로필** | `spring.profiles.active=local` | `spring.profiles.active=prod` |

**로컬 개발:**
```bash
# 기본 설정 (local 프로필)
./gradlew bootRun

# 또는 명시적으로
java -Dapp.env=local -jar app.jar
```

**프로덕션 (CI/CD):**
```bash
# GitHub Actions에서 자동 실행
java -Dapp.env=prod -jar app.jar  # 이후 Docker Image로 패징

# EC2에서 실행 (자동)
docker run -e SPRING_PROFILES_ACTIVE=prod {IMAGE_URI}
```
```

---

## ⚠️ 추가 필요 섹션

### 3. 트러블슈팅 & 주의사항

#### 3.1 데이터베이스 이슈

**[문제]** 배포 후 매번 기존 데이터가 초기화되는 현상

**[원인]** `application-oauth2-prod.yml`에서 `spring.jpa.hibernate.ddl-auto=create`로 설정되어 매 배포마다 테이블 재생성

**[해결]**
```yaml
# application-oauth2-prod.yml에서 다음 부분 제거/수정
jpa:
  hibernate:
    ddl-auto: update  # ← create가 아닌 update로 변경
```

**[데이터 백업 전략]**
```bash
# mysqldump를 사용한 정기적 백업
mysqldump -h "RDS_ENDPOINT" -u admin -p carhartt_rds | gzip > backup.sql.gz

# Cron으로 자동화 (2일마다)
0 0 */2 * * /path/to/backup-script.sh
```

#### 3.2 Docker 이미지 아키텍처 불일치

**[문제]** GitHub Actions Runner (amd64) vs EC2 Instance (arm64)에서 Docker 이미지 실행 불가

**[해결]**
```yaml
# deploy.yml에 QEMU 설정 추가
- name: Set up QEMU
  uses: docker/setup-qemu-action@v3

- name: Set up Docker Buildx
  uses: docker/setup-buildx-action@v3

- name: Build and Push (arm64)
  run: |
    docker buildx build --platform linux/arm64 \
      -t ${ECR_REGISTRY}/${ECR_REPOSITORY}:${COMMIT_SHA} \
      --push .
```

#### 3.3 RDS 연결 실패

**[증상]** `characterEncoding=utf8mb4` 설정으로 연결 오류

**[해결]**
```yaml
# application.properties 수정
spring.datasource.url=jdbc:mysql://RDS_ENDPOINT:3306/carhartt_rds\
  ?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
  # characterEncoding=utf8mb4 제거
```

**[MySQL 클라이언트로 연결 테스트]**
```bash
mysql -h carharttdb.cl4eesq4iow3.ap-northeast-2.rds.amazonaws.com \
      -u admin -p -P 3306 \
      --ssl-mode=REQUIRED \
      --ssl-ca=/etc/ssl/certs/ca-certificates.crt
```

#### 3.4 배포 후 모니터링 명령어

```bash
# EC2에서 Container 상태 확인
sudo docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'

# 실시간 로그 확인
sudo docker logs -f carhartt-platform

# 컨테이너 내부 접속
docker exec -it carhartt-platform bash

# 8080 포트 점유 프로세스 확인
sudo lsof -i :8080

# MySQL WorkBench로 RDS 확인
# SSH Tunnel 사용: EC2 → RDS (Private IP 접속)
```

#### 3.5 RDS 데이터 확인 (CLI)

```bash
# MySQL 클라이언트 설치
sudo apt-get install -y mysql-client

# RDS 접속 (비밀번호: GitHub Secrets에 저장)
mysql -h carharttdb.cl4eesq4iow3.ap-northeast-2.rds.amazonaws.com \
      -P 3306 -u admin -p \
      --ssl-mode=REQUIRED carhartt_rds

# 데이터베이스 조회
SHOW DATABASES;
USE carhartt_rds;
SHOW TABLES;
SELECT * FROM member LIMIT 10;
```

### 4. 보안 설정 가이드

#### 4.1 GitHub Secrets 저장 정보

```
ACCESS_KEY_ID              → AWS IAM Access Key
SECRET_ACCESS_KEY          → AWS IAM Secret Access Key
APPLICATION_PROPERTIES     → 프로덕션 설정 파일 전체 내용
DOCKER_USERNAME            → Docker Hub 계정명 (선택)
DOCKER_PASSWORD            → Docker Hub 토큰 (선택)
```

#### 4.2 AWS Security Group 규칙

**EC2 Instance Security Group (Inbound):**
- SSH (22): 관리자 IP만 허용
- HTTP (80): 모든 IP에서 허용
- HTTPS (443): 모든 IP에서 허용
- 8080 (내부): 닫혀있음 (Nginx 리버스 프록시 경유)

**RDS MySQL Security Group (Inbound):**
- 3306 (MySQL): EC2의 Security Group만 허용

#### 4.3 IAM Role 설정

**EC2 Instance Role (최소 권한):**
- `AmazonEC2ContainerRegistryPowerUser` - ECR 이미지 pull
- `AWSCodeDeployRoleForEC2` - CodeDeploy Agent 통신
- `AmazonS3ReadOnlyAccess` - S3에서 배포 번들 다운로드

**GitHub Actions IAM User:**
- `AmazonEC2ContainerRegistryPowerUser` - ECR에 이미지 push
- `AWSCodeDeployFullAccess` - CodeDeploy에 배포 명령

### 5. 성능 최적화 팁

#### 5.1 EC2 메모리 최적화

```bash
# JVM 힙 메모리 제한 (2GB EC2인 경우)
docker run -e JAVA_OPTS="-Xmx1g -Xms512m" \
  -p 8080:8080 {IMAGE_URI}

# 또는 Dockerfile에서
ENV JAVA_TOOL_OPTIONS="-Xmx1g -Xms512m -Dfile.encoding=UTF-8"
```

#### 5.2 RDS Connection Pool 튜닝

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10    # 동시 연결 최대치
      minimum-idle: 2          # 최소 유휴 연결
      connection-timeout: 30000 # 연결 타임아웃 30초
      idle-timeout: 600000      # 유휴 타임아웃 10분
```

#### 5.3 Gradle 캐싱 활성화

```yaml
# deploy.yml에 이미 적용됨
- name: Setup Gradle (with caching)
  uses: gradle/actions/setup-gradle@v4
```

### 6. 개발팀 체크리스트

README.md 수정 전 필수 확인사항:

- [ ] 세 참고 파일 모두 읽음
- [ ] 로컬에서 `./gradlew bootRun` 테스트 완료
- [ ] CI/CD 환경 변수 (AWS 키, RDS 비밀번호 등) 보안 확인
- [ ] Docker 이미지 빌드 테스트: `docker build -t test .`
- [ ] EC2에서 컨테이너 실행 테스트: `docker run -p 8080:8080 test`
- [ ] RDS 연결 테스트: MySQL 클라이언트로 접속 확인
- [ ] 배포 후 로그 확인: `docker logs carhartt-platform`
- [ ] Swagger UI 접근 확인: `http://<EC2_IP>:8080/swagger-ui/`

---

## 추가 정보

### 📌 참고 파일 링크

1. **separate_propertiesFile.md**
   - 환경별 설정 분리 전략
   - 로컬 vs 프로덕션 설정 파일 구조
   - Spring profiles 우선순위

2. **carharrt_infra_change.md**
   - Docker 도입 배경
   - RDS MySQL 구성
   - ECR 레지스트리 설정
   - 트러블슈팅 (DB 연결, 아키텍처 불일치)

3. **cicd_using_gitactions.md**
   - GitHub Actions 워크플로우 기본
   - CodeDeploy 설정
   - EC2 권한 설정 (IAM Role)

### 🔧 수정 팁

1. **기술 정확성:**
   - "CI/CD" 용어 구분: CI (GitHub Actions), CD (CodeDeploy)
   - "배포" vs "배포 번들" 명확히 구분

2. **시각화:**
   - ASCII 다이어그램 사용하여 흐름 명확히
   - 테이블로 비교 정보 정리

3. **일관성:**
   - 파일 경로는 항상 `/home/ubuntu/carhartt_platform` 사용
   - 이미지 태그는 `${COMMIT_SHA}` 변수명 통일
   - 포트 번호는 명확히 (Nginx: 80/443, 내부: 8080, RDS: 3306)

4. **주의 사항:**
   - 보안: RDS 비밀번호, IAM access key는 절대 노출 금지
   - 성능: EC2 메모리 최적화 (JVM 튜닝, SSD swap)
   - 데이터: `ddl-auto=update` 설정 확인 (create가 아님)

---

## 최종 체크리스트

README.md 수정 완료 후:

- [ ] 시스템 아키텍처 섹션: 환경별 설정 분리 추가됨
- [ ] CI/CD 섹션: Docker/ECR/RDS 기반 최신 구조 반영
- [ ] 다이어그램: ASCII로 인프라 구조 시각화
- [ ] 테이블: 로컬 vs 프로덕션 비교 정보 포함
- [ ] 배포 흐름: 단계별 상세 설명 추가
- [ ] 모니터링: Docker 로그 확인 방법 포함
- [ ] 링크: 참고 파일 경로 정확함

---

## 🎯 최우선 수정 항목 (순서대로)

### Phase 1: README.md 주요 섹션 수정

#### 1. 🚀 현재 CI/CD 배포 구조 - 전체 교체

**현재 상태 (X):**
```
┌─────────────────────────────────────────────┐
│    Production Deployment                    │
│  - MySQL 데이터베이스 연동                   │
│  - AWS S3 이미지 저장소                     │
│  - OAuth2 시크릿 주입 (환경변수)            │
│  - 애플리케이션 서버 시작                   │
└─────────────────────────────────────────────┘
```

**변경해야 할 내용 (✓):**
- `.jar` 직접 실행 → Docker Container 실행
- S3에 `.jar` 저장 → ECR에 Docker Image 저장
- CodeDeploy Agent가 `.jar` 직접 실행 → Docker 명령어로 컨테이너 배포
- 간단한 텍스트 설명 → **상세한 6단계 파이프라인 다이어그램** 추가

**참고:**
- `carharrt_infra_change.md` 의 **"CICD 구조 변경 방향"** 섹션 (라인 129-137)
- **"변경한 deploy.yml 코드"** 섹션 (라인 807-839)

#### 2. 🏗️ 시스템 아키텍처 - 환경별 설정 추가

**추가할 내용:**
```markdown
### 환경별 설정 분리 (Logical Profile-based)

프로젝트는 Spring Profiles를 활용하여 로컬과 프로덕션 환경을 완벽히 분리합니다:

**설정 파일 구조:**
```
src/main/resources/
├── application.properties          # 공통 설정 (기본값: spring.profiles.active=local)
├── application-local.properties    # 로컬 환경 설정 (H2 DB)
├── application-oauth2-local.yml    # 로컬 OAuth2 (테스트 자격증명)
├── application-oauth2-prod.yml     # 프로덕션 OAuth2 + JPA 설정
└── *.sql                           # 테스트 데이터 (로컬만)
```

**활성화 방식:**
- **로컬**: `spring.profiles.active=local` (기본값)
- **프로덕션**: JVM 인자 `-Dapp.env=prod` 또는 환경변수로 재정의
```

**참고:**
- `separate_propertiesFile.md` 의 **"환경설정 파일 분리"** 섹션 (라인 65-86)
- **"활성화 된 .properites(or .yml)에서 값 가져오기"** 섹션 (라인 99-117)

#### 3. 🏗️ 시스템 아키텍처 - 인프라 다이어그램 추가

**추가할 프로덕션 아키텍처 다이어그램:**
```
┌─────────────────────────────────────────────────────────────┐
│  AWS Cloud                                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ EC2 Instance (t4g.small, 2vCPU, 2GB RAM)            │  │
│  │ ├─ Nginx (Reverse Proxy, Port 80/443 → 8080)       │  │
│  │ ├─ Docker Engine                                    │  │
│  │ │  └─ Spring Boot Container (Port 8080)            │  │
│  │ └─ AWS CLI v2 (ECR pull용)                         │  │
│  └──────────────────────────────────────────────────────┘  │
│         │                               │                  │
│         ▼                               ▼                  │
│  ┌──────────────┐              ┌──────────────────┐       │
│  │ RDS MySQL    │              │ AWS ECR          │       │
│  │ (Port 3306)  │              │ (Docker Images)  │       │
│  │ • VPC 내     │              │                  │       │
│  │ • SSL 지원   │              └──────────────────┘       │
│  └──────────────┘                                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**참고:**
- `carharrt_infra_change.md` 의 **"구조 변경 방향"** 섹션 (라인 144-148)

#### 4. README 배포 파이프라인 - 6단계 상세 플로우 추가

**기존 (너무 단순):**
```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Commit     │→ │ GitHub Build │→ │   Deploy     │
└──────────────┘  └──────────────┘  └──────────────┘
```

**변경 (상세 6단계):**
```
1️⃣ Git Push to main
2️⃣ GitHub Actions: Gradle build + Docker Image 생성 + ECR Push
3️⃣ CodeDeploy: 배포 번들(appspec.yml + scripts) S3 업로드
4️⃣ CodeDeploy Agent: S3에서 번들 다운로드
5️⃣ EC2: deploy.sh 실행 (ECR에서 Image pull → Container 시작)
6️⃣ Nginx: 리버스 프록시로 8080 포트 포워딩
```

**참고:**
- `carharrt_infra_change.md` 의 **"현재 CI/CD 구조 분석"** (라인 47-127)
- **".github/workflows/deploy.yml 수정"** 섹션 (라인 393-627)

### Phase 2: 추가 섹션 (README에 추가할 것)

#### 새로운 섹션: "🔧 배포 후 모니터링"

```markdown
## 🔧 배포 후 모니터링

### 컨테이너 상태 확인
\`\`\`bash
sudo docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
sudo docker logs -f carhartt-platform
\`\`\`

### RDS MySQL 데이터 확인
\`\`\`bash
mysql -h carharttdb.cl4eesq4iow3.ap-northeast-2.rds.amazonaws.com \
      -u admin -p --ssl-mode=REQUIRED carhartt_rds
\`\`\`

### API 테스트
\`\`\`
http://<EC2_PUBLIC_IP>:8080/swagger-ui/index.html
\`\`\`
```

#### 새로운 섹션: "⚠️ 주의사항"

```markdown
## ⚠️ 주의사항

### 데이터 손실 위험
- ❌ `ddl-auto: create` (매 배포마다 테이블 재생성)
- ✅ `ddl-auto: update` (기존 데이터 유지)

### 보안
- GitHub Secrets에 RDS 비밀번호, AWS 키 저장
- Security Group으로 RDS 접근 제한 (EC2만 허용)
- SSH 접근은 특정 관리자 IP로 제한

### 성능
- EC2 메모리: 2GB (Nginx + Docker + JVM)
- JVM 힙 크기: -Xmx1g -Xms512m
- RDS Connection Pool: max=10, min=2
```

**참고:**
- `carharrt_infra_change.md` 의 **"트러블슈팅"** 섹션 (라인 930-1030)

### Phase 3: 최종 검증

README.md 작성 후 아래 항목 확인:

```markdown
## 최종 검증 체크리스트

문서 정확성:
- [ ] 포트 번호 정확 (Nginx: 80/443, Spring: 8080, MySQL: 3306)
- [ ] 파일 경로 정확 (/home/ubuntu/carhartt_platform)
- [ ] 이미지 태그 정확 (${COMMIT_SHA})
- [ ] RDS 엔드포인트 정확

기술 수준:
- [ ] 초급 개발자도 이해 가능한 수준의 설명
- [ ] 각 섹션마다 "왜?"에 대한 설명 포함
- [ ] 실제 배포 시 필요한 명령어 포함

시각화:
- [ ] ASCII 다이어그램으로 전체 구조 표현
- [ ] 로컬 vs 프로덕션 비교 테이블 포함
- [ ] 배포 파이프라인 6단계 명확히 구분

참고:
- [ ] 세 참고 파일 링크 명시
- [ ] 트러블슈팅 섹션 포함
- [ ] 보안 설정 가이드 포함
```

---

## 🚀 빠른 시작 (3단계)

### 1단계: 참고 파일 검토 시간
```
separate_propertiesFile.md (10분)
  → 환경별 설정 분리 방식 이해

carharrt_infra_change.md (20분)
  → Docker, RDS, ECR 도입 배경 + 트러블슈팅 학습

cicd_using_gitactions.md (15분)
  → CI/CD 파이프라인 상세 학습
```

### 2단계: README.md 섹션별 수정 (30분)
```
① CI/CD 배포 구조 - 6단계 다이어그램 교체
② 시스템 아키텍처 - 환경별 설정 추가
③ 인프라 다이어그램 - 프로덕션 구조 추가
④ 새 섹션 추가 - 모니터링, 주의사항, 보안
```

### 3단계: 검증 (10분)
```
✓ 포트/경로/엔드포인트 정확성 확인
✓ 다이어그램 가독성 확인
✓ 링크 오류 여부 확인
✓ 전체 문서 일관성 확인
```

**총 소요 시간: 약 1.5시간**
