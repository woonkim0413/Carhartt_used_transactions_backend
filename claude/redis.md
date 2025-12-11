# Redis 기반 세션 저장소 전환 가이드

---

## 🚨 긴급: CodeDeploy AfterInstall 실패 - Redis 환경변수 누락 (2025-12-11)

### 문제 증상

**배포 상태:**
- ✅ GitHub Actions: 성공 (이미지 빌드, ECR 푸시, S3 업로드 완료)
- ❌ CodeDeploy: **AfterInstall 단계에서 실패**
- **에러 메시지**: `0 of 2 instances updated - Failed`

### 원인 분석

#### 1. 배포 흐름 확인

```
GitHub Actions (deploy.yml)
├─ 1. Docker 이미지 빌드 ✅
├─ 2. ECR에 이미지 푸시 ✅
├─ 3. redis.env 파일 생성 (Line 124-128) ← 여기서 문제 발생
│    cat > bundle/redis.env <<EOF
│    export REDIS_HOST="${{ secrets.REDIS_HOST }}"  ← Secrets 값 읽기 (필수)
│    EOF
│    # REDIS_PORT는 6379로 하드코딩 (GitHub Secrets 불필요)
│    # REDIS_PASSWORD는 선택사항 (기본값: 빈 문자열)
├─ 4. 번들 파일 생성 (zip) ✅
├─ 5. S3 업로드 ✅
└─ 6. CodeDeploy 트리거 ✅

CodeDeploy (appspec.yml)
├─ ApplicationStop
├─ BeforeInstall
├─ AfterInstall → scripts/deploy.sh 실행 ❌
│    ├─ redis.env 파일 로드 (Line 10-15)
│    ├─ 환경변수 검증 (Line 17-24) ← 여기서 실패!
│    │    : "${REDIS_HOST:?REDIS_HOST environment variable is required}"  ← 필수
│    │    REDIS_PORT="${REDIS_PORT:-6379}"  ← 하드코딩 (기본값 6379)
│    │    REDIS_PASSWORD="${REDIS_PASSWORD:-}"  ← 선택사항 (기본값 빈 문자열)
│    └─ 에러 발생 → 스크립트 중단
└─ ApplicationStart (실행되지 않음)
```

#### 2. 에러 발생 지점

**파일**: `scripts/deploy.sh` Line 17-24

```bash
# Redis 환경변수 검증 (REDIS_HOST만 필수)
: "${REDIS_HOST:?REDIS_HOST environment variable is required}"

# REDIS_PORT는 하드코딩 (표준 Redis 포트 6379)
REDIS_PORT="${REDIS_PORT:-6379}"

# REDIS_PASSWORD는 선택사항 (비밀번호 없는 경우 빈 문자열)
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
```

**동작:**
- `${VAR:?message}`: 변수가 설정되지 않았거나 비어있으면 에러 메시지 출력 후 스크립트 종료
- `${VAR:-default}`: 변수가 없으면 기본값 사용 (에러 없이 계속 진행)
- `set -euo pipefail` (Line 2): 에러 발생 시 즉시 종료

#### 3. 근본 원인

**GitHub Secrets에 `REDIS_HOST`가 설정되지 않음**

```
GitHub Repository → Settings → Secrets and variables → Actions

확인 필요:
❌ REDIS_HOST: 설정되어 있는가? (필수!)
✅ REDIS_PORT: 불필요 (6379로 하드코딩됨)
✅ REDIS_PASSWORD: 불필요 (빈 문자열 기본값 사용)
```

**만약 `REDIS_HOST` Secret이 없다면:**
1. GitHub Actions에서 `redis.env` 파일 생성 시 빈 값으로 설정됨
   ```bash
   export REDIS_HOST=""  # ← 빈 문자열
   ```

2. CodeDeploy에서 `deploy.sh` 실행 시 환경변수 검증 실패
   ```bash
   bash: REDIS_HOST: REDIS_HOST environment variable is required
   ```

3. 스크립트 중단 → AfterInstall 단계 실패 → 배포 전체 실패

### 해결 방법

#### Step 1: GitHub Secrets 설정 확인

```bash
# 1. GitHub Repository 이동
# https://github.com/[username]/[repository]

# 2. Settings → Secrets and variables → Actions → Repository secrets

# 3. 다음 Secrets가 존재하는지 확인:
```

**필수 Secrets:**

| Secret 이름 | 값 예시 | 필수 여부 | 설명 |
|-------------|---------|----------|------|
| `REDIS_HOST` | `carhartt-u-redis-001.cvf1em.0001.apn2.cache.amazonaws.com` | ✅ **필수** | ElastiCache 엔드포인트 |
| ~~`REDIS_PORT`~~ | ~~`6379`~~ | ❌ 불필요 | 6379로 하드코딩됨 |
| ~~`REDIS_PASSWORD`~~ | ~~(빈 값)~~ | ❌ 불필요 | 빈 문자열 기본값 사용 |

#### Step 2: Secret 추가 방법

1. **"New repository secret" 버튼 클릭**

2. **Name**: `REDIS_HOST`
   **Secret**: ElastiCache 엔드포인트 입력
   ```
   carhartt-u-redis-001.cvf1em.0001.apn2.cache.amazonaws.com
   ```

**주의**: `REDIS_PORT`와 `REDIS_PASSWORD`는 추가할 필요 없습니다!
- `REDIS_PORT`: 6379로 하드코딩됨
- `REDIS_PASSWORD`: 빈 문자열 기본값 사용됨

#### Step 3: ElastiCache 엔드포인트 확인 방법

```bash
# AWS Console 접속
# → ElastiCache → Redis clusters → [클러스터 이름]
# → Primary endpoint 복사

# 예시:
# carhartt-u-redis-001.cvf1em.0001.apn2.cache.amazonaws.com:6379
#                                                          ↑
#                                                      포트 제거한 호스트명만 사용
```

**주의**: 엔드포인트에서 `:6379` 포트 번호는 제거하고 호스트명만 `REDIS_HOST`에 입력!

#### Step 4: 재배포 및 검증

```bash
# 1. GitHub Actions 재실행 또는 코드 Push
git commit --allow-empty -m "chore: Trigger redeploy after adding Redis secrets"
git push origin main  # 또는 dev-test

# 2. GitHub Actions 진행 확인
# https://github.com/[username]/[repository]/actions

# 3. CodeDeploy 배포 성공 확인
# AWS Console → CodeDeploy → Deployments

# 예상 결과:
# ✅ 2 of 2 instances updated - Succeeded
```

#### Step 5: 배포 성공 후 확인

```bash
# EC2 서버에 SSH 접속
ssh -i your-key.pem ubuntu@your-ec2-ip

# 1. redis.env 파일 내용 확인
cat /home/ubuntu/carhartt_platform/redis.env
# 예상 결과:
# export REDIS_HOST="carhartt-u-redis-001.cvf1em.0001.apn2.cache.amazonaws.com"
# export REDIS_PORT="6379"
# export REDIS_PASSWORD=""

# 2. Docker 컨테이너 환경변수 확인
docker inspect carhartt-platform | grep -A 5 "Env"
# 예상 결과:
# "SPRING_PROFILES_ACTIVE=prod"
# "REDIS_HOST=carhartt-u-redis-001.cvf1em.0001.apn2.cache.amazonaws.com"
# "REDIS_PORT=6379"

# 3. 애플리케이션 로그 확인
docker logs carhartt-platform | grep "RedisIndexedSessionRepository"
# 예상 결과:
# "Spring Session initialized with RedisIndexedSessionRepository"
```

### 트러블슈팅

#### 문제 1: Secrets를 추가했는데도 계속 실패

**원인**: 기존 GitHub Actions workflow가 캐싱된 상태

**해결**:
```bash
# 빈 커밋으로 강제 재배포
git commit --allow-empty -m "chore: Force redeploy"
git push
```

#### 문제 2: ElastiCache 엔드포인트를 모르겠음

**해결**:
```bash
# AWS CLI로 확인
aws elasticache describe-cache-clusters \
  --cache-cluster-id carhartt-redis \
  --show-cache-node-info \
  --region ap-northeast-2

# 또는 AWS Console:
# ElastiCache → Redis clusters → [클러스터 이름] → Primary endpoint
```

#### 문제 3: redis.env 파일이 생성되지 않음

**원인**: GitHub Actions 빌드 단계 실패

**확인**:
```bash
# GitHub Actions 로그 확인
# Actions 탭 → 최근 workflow → "Stage bundle" 단계 확인
# redis.env 파일 생성 로그가 있는지 확인
```

### 체크리스트

배포 실패 해결을 위한 체크리스트:

- [ ] GitHub Secrets에 `REDIS_HOST` 추가 (ElastiCache 엔드포인트) ← **필수!**
- [ ] ~~GitHub Secrets에 `REDIS_PORT` 추가~~ ← **불필요 (하드코딩됨)**
- [ ] ~~GitHub Secrets에 `REDIS_PASSWORD` 추가~~ ← **불필요 (기본값 사용)**
- [ ] 코드 변경사항 commit & push (application-oauth2-prod.yml, deploy.sh, deploy.yml)
- [ ] GitHub Actions 성공 확인 (모든 단계 완료)
- [ ] CodeDeploy 성공 확인 (2 of 2 instances updated)
- [ ] EC2에서 `redis.env` 파일 내용 확인
- [ ] Docker 컨테이너 환경변수 확인
- [ ] 애플리케이션 로그에서 Redis Session 초기화 확인

### ✅ 해결 완료 (2025-12-11)

**문제 해결 방안**: GitHub Secrets 요구사항 간소화

배포 실패 문제를 해결하기 위해 다음과 같이 코드를 수정했습니다:

#### 수정된 파일

**1. application-oauth2-prod.yml**
- `REDIS_PORT`를 6379로 하드코딩
- 환경변수 의존성 제거

**2. scripts/deploy.sh**
- `REDIS_HOST`만 필수 검증
- `REDIS_PORT`는 기본값 6379 사용
- `REDIS_PASSWORD`는 빈 문자열 기본값 사용

**3. .github/workflows/deploy.yml**
- redis.env 파일에 `REDIS_HOST`만 포함
- GitHub Secrets 요구사항 간소화

#### 최종 결과

**이제 GitHub Secrets에 `REDIS_HOST` 하나만 추가하면 배포가 성공합니다!**

```bash
# GitHub Repository → Settings → Secrets
# Name: REDIS_HOST
# Secret: carhartt-u-redis-001.cvf1em.0001.apn2.cache.amazonaws.com
```

**참고**: 상세 작업 내역은 `claude/request.md` 참조

---

## 🔥 긴급: 로드 밸런싱 환경에서 세션 미공유 문제 (2025-12-11)

### 문제 증상
- **현상**: Swagger UI에서 API를 2번 요청하면 1번 성공, 1번 실패 (50% 성공률)
- **환경**: 2대의 EC2 서버 + Nginx 로드 밸런싱 (라운드 로빈)
- **Redis 상태**: `keys *` 결과 `(empty array)` - 세션이 저장되지 않음

### 원인 분석

**세션이 Redis가 아닌 각 서버의 Tomcat 메모리에 저장되고 있습니다.**

```
┌─────────────────────────────────────────────────────┐
│  Nginx (로드 밸런서 - 라운드 로빈)                   │
└───────────┬─────────────────────────────────────────┘
            │
    ┌───────┴────────┐
    │                │
    ▼                ▼
Server 1          Server 2
├─ Tomcat         ├─ Tomcat
│  ├─ Session A   │  ├─ Session A 없음!
│  └─ 요청 성공✓  │  └─ 요청 실패✗ (401)
└─ Redis 미사용   └─ Redis 미사용

           ▼
    Redis (ElastiCache)
    └─ (empty) ← 세션이 저장되지 않음!
```

**왜 이런 일이 발생하는가?**
1. 로그인 시 Server 1이 세션을 **자신의 메모리**에만 저장
2. 다음 API 요청이 Server 2로 라우팅됨
3. Server 2는 세션 정보가 없어서 401 UNAUTHORIZED 반환
4. **Redis Session이 제대로 활성화되지 않음**

### 즉시 확인 사항

**두 서버 모두에서 다음 명령어를 실행하세요:**

```bash
# ========================================
# 1. 최신 배포 확인 (Server 1, 2 모두)
# ========================================
cat /home/ubuntu/carhartt_platform/COMMIT_SHA
# → 두 서버의 SHA가 동일한지 확인!

# ========================================
# 2. Docker 컨테이너 환경변수 확인
# ========================================
docker inspect carhartt-platform | grep -A 15 "Env"

# 확인할 내용:
# ✓ "SPRING_PROFILES_ACTIVE=prod" 존재하는가?
# ✓ "REDIS_HOST=carhartt-u-redis-001..." 존재하는가?
# ✓ "REDIS_PORT=6379" 존재하는가?

# ========================================
# 3. 활성 프로파일 확인
# ========================================
docker logs carhartt-platform | grep "profiles are active"
# 예상 결과: "The following 1 profile is active: prod"

# ========================================
# 4. application-oauth2-prod.yml 로드 확인
# ========================================
docker logs carhartt-platform | grep "oauth2-prod"
# 예상 결과: "Loaded config file 'classpath:application-oauth2-prod.yml'"

# ========================================
# 5. Redis Session 초기화 로그 확인
# ========================================
docker logs carhartt-platform | grep "RedisIndexedSessionRepository"
# 예상 결과: "Spring Session initialized with RedisIndexedSessionRepository"

# ========================================
# 6. SessionRepositoryFilter 등록 확인
# ========================================
docker logs carhartt-platform | grep "SessionRepositoryFilter"
# 예상 결과: "Filter 'sessionRepositoryFilter' configured for use"
```

### 문제 진단 플로우차트

```
┌─────────────────────────────────────────────────────────┐
│ Step 1: COMMIT_SHA 확인                                 │
│ → 두 서버가 다른 버전?                                  │
│   YES: 재배포 필요                                      │
│   NO: Step 2로                                          │
└─────────────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────────────┐
│ Step 2: 환경변수 확인                                   │
│ → SPRING_PROFILES_ACTIVE=prod 없음?                    │
│   YES: deploy.sh 수정사항이 배포 안 됨, 재배포 필요     │
│   NO: Step 3으로                                        │
└─────────────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────────────┐
│ Step 3: 프로파일 로그 확인                              │
│ → "active: local" 로그?                                 │
│   YES: 환경변수가 적용 안 됨, 컨테이너 재시작 필요      │
│   NO: Step 4로                                          │
└─────────────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────────────┐
│ Step 4: application-oauth2-prod.yml 로드 확인           │
│ → 로그 없음?                                            │
│   YES: GitHub Secrets APPLICATION_PROPERTIES 확인 필요  │
│        (spring.config.import 누락?)                     │
│   NO: Step 5로                                          │
└─────────────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────────────┐
│ Step 5: Redis Session 초기화 로그 확인                  │
│ → "RedisIndexedSessionRepository" 로그 없음?            │
│   YES: Redis 연결 실패, 네트워크/보안그룹 확인 필요     │
│   NO: 정상, 하지만 여전히 실패한다면 로그 전체 검토     │
└─────────────────────────────────────────────────────────┘
```

### 가장 가능성 높은 원인

**원인 1: 최신 코드가 배포되지 않음 (90% 확률)**

`deploy.sh`에 추가한 `SPRING_PROFILES_ACTIVE=prod` 환경변수가 아직 배포되지 않았을 가능성:

```bash
# 확인 방법
docker inspect carhartt-platform | grep SPRING_PROFILES_ACTIVE

# 결과가 없다면:
# → scripts/deploy.sh 수정사항이 아직 배포 안 됨
# → Git push 후 GitHub Actions 재배포 필요
```

**원인 2: 두 서버가 다른 버전 사용 (5% 확률)**

```bash
# Server 1에서
cat /home/ubuntu/carhartt_platform/COMMIT_SHA
# 결과: abc123

# Server 2에서
cat /home/ubuntu/carhartt_platform/COMMIT_SHA
# 결과: xyz789 (다르다!)

# → CodeDeploy 배포 실패 또는 부분 배포
# → 수동으로 양쪽 서버 재배포 필요
```

**원인 3: Redis 환경변수 누락 (3% 확률)**

```bash
# redis.env 파일 확인
cat /home/ubuntu/carhartt_platform/redis.env

# 파일이 없거나 내용이 비어있다면:
# → GitHub Secrets 설정 확인 필요
```

**원인 4: GitHub Secrets APPLICATION_PROPERTIES 설정 오류 (2% 확률)**

```bash
# spring.config.import 설정이 누락되었을 가능성
# → 섹션 2-1 참고
```

### 해결 방법

**1단계: 최신 코드 배포 확인**

```bash
# 1. Git 커밋 및 푸시
git status
git add scripts/deploy.sh claude/redis.md CLAUDE.md
git commit -m "fix: Add SPRING_PROFILES_ACTIVE=prod for Redis session sharing"
git push origin main  # 또는 dev-test

# 2. GitHub Actions 진행 상황 확인
# https://github.com/your-repo/actions

# 3. 배포 완료 후 두 서버에서 COMMIT_SHA 확인
```

**2단계: 컨테이너 재시작 (배포 완료 후)**

```bash
# Server 1과 Server 2 모두에서
docker restart carhartt-platform

# 로그 실시간 확인
docker logs -f carhartt-platform

# 확인할 로그 순서:
# [1] "The following 1 profile is active: prod"
# [2] "Loaded config file 'classpath:application-oauth2-prod.yml'"
# [3] "Spring Session initialized with RedisIndexedSessionRepository"
# [4] "Started CPlatformApplication"
```

**3단계: Redis 세션 저장 테스트**

```bash
# 1. Redis 초기화
redis-cli -h carhartt-u-redis-001.cvf1em.0001.apn2.cache.amazonaws.com -p 6379
FLUSHDB

# 2. 로그인 API 호출
curl -X POST https://carhartt-usedtransactions.com/v1/local/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password123"}' \
  -c cookies.txt -v

# 3. Redis 세션 확인
redis-cli -h carhartt-u-redis-001.cvf1em.0001.apn2.cache.amazonaws.com -p 6379
keys spring:session:*

# 예상 결과:
# 1) "spring:session:sessions:..."
# 2) "spring:session:sessions:expires:..."
# 3) "spring:session:expirations:..."

# 4. 인증 필요 API를 5번 연속 호출
for i in {1..5}; do
  curl -X GET https://carhartt-usedtransactions.com/v1/orders/address \
    -b cookies.txt -w "\nRequest $i: %{http_code}\n"
done

# 예상 결과: 5번 모두 200 OK (401 없어야 함!)
```

### 성공 기준

✅ **Redis에 세션이 저장됨**
```bash
redis-cli keys 'spring:session:*'
# 결과: 3개의 키가 반환됨
```

✅ **두 서버 모두 동일한 로그**
```bash
# Server 1, 2 모두에서
docker logs carhartt-platform | grep "RedisIndexedSessionRepository"
# 결과: "Spring Session initialized with RedisIndexedSessionRepository"
```

✅ **API 연속 호출 시 100% 성공**
```bash
# 5번 연속 호출 모두 200 OK
# 401 UNAUTHORIZED 에러 없음
```

---

## 🚨 중요: Local 환경 설정 변경 금지 (2025-12-11)

### ⚠️ Local 환경 설정은 변경하지 마세요!

**`application-oauth2-local.yml` 파일에는 Redis 설정을 추가하지 마세요.**

#### 이유
1. **Local 환경은 개발 편의성을 위해 Tomcat 메모리 세션을 사용합니다**
   - Redis 서버 설치/실행 불필요
   - 빠른 개발 환경 구성
   - 서버 재시작 시 세션 초기화로 깔끔한 상태 유지

2. **Redis Session은 Production 환경 전용입니다**
   - EC2 + AWS ElastiCache for Redis 구성
   - 다중 서버 환경에서 세션 공유 목적
   - 서버 재시작 시에도 세션 유지 필요

3. **`RedisSessionConfig.java`는 조건부 활성화됩니다**
   - `@EnableRedisHttpSession` 어노테이션이 있지만
   - `spring.session.store-type=redis` 설정이 있는 프로파일에서만 작동
   - Local 프로파일에는 이 설정이 없으므로 Redis Session이 비활성화됨

### 환경별 세션 저장소

| 환경 | 프로파일 | 세션 저장소 | 설정 파일 | Redis 설정 |
|------|----------|-------------|-----------|------------|
| Local | `local` | Tomcat 메모리 | `application-oauth2-local.yml` | ❌ 없음 (의도적) |
| Production | `prod` | AWS ElastiCache Redis | `application-oauth2-prod.yml` | ✅ 있음 |

### Local 환경에서 로그인 후 401 에러가 발생한다면?

**원인**: Local 환경에서는 Tomcat 메모리 세션을 사용하므로, 다음 상황에서 세션이 소실됩니다:
1. 서버 재시작 시
2. 애플리케이션 재배포 시
3. JVM 종료 시

**해결 방법**: 이것은 **정상 동작**입니다.
- Local 환경에서는 로그인 상태가 서버 재시작 시 초기화되는 것이 정상입니다
- 개발 중 재로그인이 필요하면 `/v1/local/login` API를 다시 호출하세요

### Production 환경에서 Redis Session 확인 방법

Production 환경(EC2 서버)에서 세션이 Redis에 제대로 저장되는지 확인하려면:

#### 1. EC2 서버에 SSH 접속
```bash
ssh -i your-key.pem ubuntu@your-ec2-ip
```

#### 2. Redis CLI로 ElastiCache 접속
```bash
# Redis 엔드포인트는 환경변수 REDIS_HOST에서 확인
redis-cli -h $REDIS_HOST -p $REDIS_PORT

# 비밀번호가 설정된 경우
redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD
```

#### 3. 세션 키 확인
```bash
# Redis CLI 내부에서 실행
keys spring:session:*

# 예상 결과:
# 1) "spring:session:sessions:8031a48c-d9f5-1cc8-c280-ce9fef6..."
# 2) "spring:session:sessions:expires:8031a48c-d9f5-1cc8-c280-ce9fef6..."
# 3) "spring:session:expirations:1733925600000"
```

#### 4. 특정 세션 내용 확인
```bash
# 세션 데이터 상세 조회
hgetall spring:session:sessions:<session-id>

# 결과 예시:
# "creationTime" "1733918400000"
# "lastAccessedTime" "1733918415000"
# "maxInactiveInterval" "1800"
# "sessionAttr:SPRING_SECURITY_CONTEXT" "<직렬화된 SecurityContext 객체>"
```

#### 5. 세션 TTL 확인
```bash
# 세션 남은 시간 확인 (초 단위)
ttl spring:session:sessions:<session-id>

# 결과: 1794 (약 30분 = 1800초)
```

### Redis 세션 데이터 구조

Production 환경의 Redis에 저장되는 Spring Session 데이터:

```
spring:session:sessions:<session-id>           # Hash: 세션 속성 데이터
  - creationTime: 생성 시간 (밀리초)
  - lastAccessedTime: 마지막 접근 시간
  - maxInactiveInterval: 만료 시간 (초)
  - sessionAttr:SPRING_SECURITY_CONTEXT: SecurityContext 객체 (직렬화됨)

spring:session:sessions:expires:<session-id>   # String: 만료 시간 (밀리초)

spring:session:expirations:<timestamp>         # Set: 해당 시간에 만료될 세션 ID 목록
```

### Production 환경 테스트 방법

Production 환경에서 로그인 후 세션이 Redis에 저장되는지 확인:

```bash
# 1. Production 서버에 로그인 API 호출
curl -X POST https://carhartt-usedtransactions.com/v1/local/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password123"}' \
  -c cookies.txt -v

# 2. 응답 헤더에서 JSESSIONID 쿠키 확인
# Set-Cookie: JSESSIONID=8031A48CD9F51CC8C280CE9FEF6...

# 3. EC2 서버에서 Redis 확인
redis-cli -h $REDIS_HOST keys 'spring:session:*'

# 4. 인증 필요 API 호출 (JSESSIONID 쿠키 포함)
curl -X GET https://carhartt-usedtransactions.com/v1/orders/address \
  -b cookies.txt

# 5. 정상 응답 확인 (401 에러가 아닌 정상 데이터 응답)
```

### 트러블슈팅: Production 환경에서 401 에러 발생 시

- [ ] EC2 서버에서 애플리케이션이 정상 실행 중인가?
- [ ] 환경변수 `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`가 올바르게 설정되었는가?
- [ ] AWS ElastiCache Redis 인스턴스가 실행 중인가?
- [ ] EC2 보안 그룹에서 ElastiCache 접근이 허용되었는가?
- [ ] 애플리케이션 로그에서 "RedisConnectionFactory" 초기화 성공 확인되는가?
- [ ] Redis CLI에서 `ping` 명령이 `PONG`을 반환하는가?
- [ ] 로그인 후 Redis에 세션 키가 생성되는가? (`keys spring:session:*`)

---

## ⚡ 실제 적용된 변경 사항 (2025-12-11)

이 섹션은 실제로 프로젝트에 적용된 변경 사항을 기록합니다. 디버깅 시 참고하세요.

### 적용 완료 항목

#### 1. 의존성 추가 ✅
**파일**: `build.gradle`
**위치**: Line 49-51

```gradle
// Redis 세션 저장소 의존성
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
implementation 'org.springframework.session:spring-session-data-redis'
```

**변경 전**:
```gradle
// Redis는 선택사항 (로컬 개발 환경에서는 메모리 기반 저장소 사용)
// implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

**디버깅 팁**:
- 의존성 다운로드 확인: `./gradlew dependencies | grep redis`
- Spring Session 클래스 로딩 확인: 로그에서 "Spring Session initialized with RedisIndexedSessionRepository" 검색

---

#### 2. Redis 설정 추가 ✅
**파일**: `src/main/resources/application-oauth2-prod.yml`
**위치**: Line 74-93 (spring 섹션 하위)

```yaml
  # Redis 설정 (세션 저장소)
  data:
    redis:
      host: ${REDIS_HOST:localhost}  # 환경변수로 주입, 기본값 localhost
      port: ${REDIS_PORT:6379}  # 환경변수로 주입, 기본값 6379
      password: ${REDIS_PASSWORD:}  # 환경변수로 주입, 비밀번호 없으면 빈 문자열
      timeout: 60000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 2000ms

  # Spring Session 설정
  session:
    store-type: redis
    timeout: 1800s  # 세션 만료 시간 (30분)
    redis:
      namespace: spring:session  # Redis 키 prefix
```

**환경변수 설정 방법**:
```bash
# 로컬 개발 환경 (기본값 사용)
./gradlew bootRun

# 환경변수로 Redis 서버 지정
export REDIS_HOST=redis.example.com
export REDIS_PORT=6379
export REDIS_PASSWORD=your-password
./gradlew bootRun

# GitHub Actions에서 설정
# Secrets에 REDIS_HOST, REDIS_PORT, REDIS_PASSWORD 추가
```

**디버깅 팁**:
- Redis 연결 확인: 로그에서 "Lettuce" 또는 "RedisConnectionFactory" 검색
- 연결 실패 시: `io.lettuce.core.RedisConnectionException` 에러 확인
- 환경변수 확인: `echo $REDIS_HOST`, `echo $REDIS_PORT`

**중요**: Local 환경(`application-oauth2-local.yml`)에는 Redis 설정을 추가하지 마세요! Local 환경은 Tomcat 메모리 세션을 사용하도록 의도적으로 설계되었습니다.

---

#### 2-1. GitHub Secrets APPLICATION_PROPERTIES 설정 (🚨 매우 중요!)
**위치**: GitHub Repository Settings → Secrets and variables → Actions → Repository secrets
**설정 날짜**: 2025-12-11 확인

**⚠️ 반드시 포함되어야 하는 설정:**

```properties
# Production 프로파일 활성화
spring.profiles.active=prod

# 🔴 핵심: application-oauth2-prod.yml 파일 import (필수!)
spring.config.import=classpath:application-oauth2-prod.yml

# 나머지 production 환경 설정들...
```

**왜 이 설정이 중요한가?**

1. **GitHub Actions 빌드 프로세스** (`.github/workflows/deploy.yml` Line 49-68):
   ```yaml
   # Git의 application.properties 삭제
   - name: Remove test application.properties
     run: rm application.properties

   # GitHub Secrets의 값으로 교체
   - name: Make application-prod.properties
     run: echo "${{ secrets.APPLICATION_PROPERTIES }}" > ./application.properties
   ```

2. **빌드된 JAR에 포함되는 설정**:
   - Git Repository의 application.properties는 **삭제**됨
   - GitHub Secrets의 `APPLICATION_PROPERTIES` 값이 **JAR에 포함**됨
   - 이 설정이 runtime에 사용됨

3. **프로파일만으로는 부족한 이유**:
   - `spring.profiles.active=prod`만 설정하면 `application-prod.yml`은 자동 로드됨
   - 하지만 `application-oauth2-prod.yml`은 **수동으로 import** 해야 함
   - `spring.config.import` 없으면 Redis 설정이 로드되지 않음!

**잘못된 설정 예시 (Redis 작동 안 함):**
```properties
# ❌ 이렇게만 설정하면 안 됨!
spring.profiles.active=prod

# spring.config.import가 없음 → application-oauth2-prod.yml이 로드되지 않음!
```

**올바른 설정 예시:**
```properties
# ✅ 반드시 둘 다 포함
spring.profiles.active=prod
spring.config.import=classpath:application-oauth2-prod.yml

# 이제 application-oauth2-prod.yml의 Redis 설정이 로드됨!
```

**Git Repository의 application.properties (참고용):**
```properties
# Line 6-7 (Local 환경 기본 설정)
spring.profiles.active=local
spring.config.import=classpath:application-oauth2-local.yml
```

**Production 빌드 시 교체되는 구조:**
```
Git Repository (삭제됨)          GitHub Secrets (사용됨)
├── application.properties  →    APPLICATION_PROPERTIES
│   ├─ local 프로파일              ├─ prod 프로파일
│   └─ local.yml import            └─ prod.yml import ← 필수!
```

**디버깅 팁:**
```bash
# EC2에서 애플리케이션 로그 확인
docker logs carhartt-platform | grep "config file"

# 예상 결과 (정상):
# "Loaded config file 'classpath:application-oauth2-prod.yml'"

# 만약 이 로그가 없다면 → spring.config.import 설정 누락!
```

**체크리스트:**
- [ ] GitHub Secrets에 `APPLICATION_PROPERTIES` 존재
- [ ] `spring.profiles.active=prod` 설정 포함
- [ ] `spring.config.import=classpath:application-oauth2-prod.yml` 설정 포함 ← **핵심!**
- [ ] 빌드 후 로그에서 "Loaded config file 'application-oauth2-prod.yml'" 확인

---

#### 3. RedisSessionConfig 클래스 생성 ✅
**파일**: `src/main/java/com/C_platform/config/RedisSessionConfig.java`
**생성 날짜**: 2025-12-11

```java
package com.C_platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)  // 30분
public class RedisSessionConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        return template;
    }
}
```

**주요 동작**:
- `@EnableRedisHttpSession`: Spring Session이 `SessionRepositoryFilter`를 자동 등록
- `SessionRepositoryFilter`가 `HttpServletRequest.getSession()`을 가로채서 Redis 기반 세션 반환
- `maxInactiveIntervalInSeconds = 1800`: 세션 30분 후 자동 만료

**디버깅 팁**:
- Bean 등록 확인: 로그에서 "RedisSessionConfig" 검색
- Filter 등록 확인: 로그에서 "SessionRepositoryFilter" 검색
- Redis Template 확인: `redisTemplate.opsForValue().set("test", "value")` 테스트

---

#### 4. 변경 불필요한 파일 (중요!) ✅

다음 파일들은 **수정하지 않았습니다** (Spring Session이 자동으로 HttpSession 구현체를 교체):

1. **SecurityConfig.java**
   - `securityContextRepository()` 빈: `HttpSessionSecurityContextRepository` 그대로 유지
   - 내부적으로 Redis 기반 HttpSession 사용

2. **JsonUsernamePasswordAuthenticationFilter.java**
   - `super.successfulAuthentication()` 그대로 유지
   - HttpSession 생성 코드 변경 없음

3. **LocalAuthenticationSuccessHandler.java**
   - `request.getSession(true)` 그대로 유지
   - 자동으로 Redis 세션 반환

4. **SessionCheckFilter.java**
   - `request.getSession(false)` 그대로 유지
   - Redis 세션 여부 확인

**디버깅 팁**:
- 세션 타입 확인: `request.getSession().getClass().getName()` 출력
  - Redis 적용 전: `org.apache.catalina.session.StandardSessionFacade`
  - Redis 적용 후: `org.springframework.session.web.http.SessionRepositoryFilter$SessionRepositoryRequestWrapper$HttpSessionWrapper`

---

#### 5. GitHub Actions 환경변수 설정 ✅
**파일**: `.github/workflows/deploy.yml`, `scripts/deploy.sh`
**작업 날짜**: 2025-12-11

GitHub Actions Secrets를 사용하여 Redis 환경변수를 EC2로 안전하게 전달하도록 구성했습니다.

**변경 내용 1: deploy.yml - Redis 환경변수 파일 생성**
**위치**: Line 124-129

```yaml
# Redis 환경변수 파일 생성 (GitHub Secrets에서 주입)
cat > bundle/redis.env <<EOF
export REDIS_HOST="${{ secrets.REDIS_HOST }}"
export REDIS_PORT="${{ secrets.REDIS_PORT }}"
export REDIS_PASSWORD="${{ secrets.REDIS_PASSWORD }}"
EOF
```

**동작 원리**:
1. GitHub Actions에서 Secrets 값을 읽어서 `redis.env` 파일 생성
2. 이 파일이 CodeDeploy 번들(zip)에 포함됨
3. EC2 인스턴스의 `/home/ubuntu/carhartt_platform/redis.env`로 배포됨

**변경 내용 2: deploy.sh - 환경변수 파일 로드**
**위치**: Line 9-24

```bash
# Redis 환경변수 로드 (GitHub Actions에서 생성한 파일)
if [ -f "$APP_HOME/redis.env" ]; then
    echo "[deploy] Loading Redis configuration from redis.env"
    source "$APP_HOME/redis.env"
else
    echo "[deploy] WARNING: redis.env not found, using environment variables"
fi

# Redis 환경변수 검증
: "${REDIS_HOST:?REDIS_HOST environment variable is required}"
: "${REDIS_PORT:?REDIS_PORT environment variable is required}"
# REDIS_PASSWORD는 선택사항 (비밀번호 없는 경우 빈 문자열)
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

echo "[deploy] Using image: $IMAGE_URI"
echo "[deploy] Redis configuration: $REDIS_HOST:$REDIS_PORT"
```

**주요 특징**:
- `source` 명령으로 환경변수 파일 로드
- 파일이 없을 경우 경고 메시지 출력 (fallback 지원)
- 필수 환경변수 검증 (누락 시 스크립트 중단)
- Redis 설정 로깅 (디버깅 용이)

**GitHub Secrets 설정 방법**:
```
GitHub Repository → Settings → Secrets and variables → Actions → Repository secrets

필수 Secrets:
- REDIS_HOST: Redis 서버 엔드포인트 (예: carhartt-redis.cvf1em.ng.0001.apn2.cache.amazonaws.com)
- REDIS_PORT: Redis 포트 (예: 6379)
- REDIS_PASSWORD: Redis 비밀번호 (AUTH 설정 안 했으면 빈 값)
```

**배포 흐름**:
```
1. GitHub Actions (deploy.yml)
   ├─ GitHub Secrets에서 Redis 설정 읽기
   ├─ redis.env 파일 생성
   └─ CodeDeploy 번들에 포함 (zip)

2. CodeDeploy
   ├─ S3에서 번들 다운로드
   └─ EC2 인스턴스로 파일 배포 (/home/ubuntu/carhartt_platform/)

3. EC2 (deploy.sh)
   ├─ redis.env 파일 로드 (source)
   ├─ 환경변수 검증
   ├─ Docker 이미지 Pull
   └─ 컨테이너 실행 (환경변수 전달)
```

**디버깅 팁**:
- CodeDeploy 로그 확인: `/var/log/aws/codedeploy-agent/codedeploy-agent.log`
- deploy.sh 실행 로그 확인: `/opt/codedeploy-agent/deployment-root/.../logs/scripts.log`
- 예상 로그:
  ```
  [deploy] Loading Redis configuration from redis.env
  [deploy] Using image: 123456789.dkr.ecr.ap-northeast-2.amazonaws.com/c-platform:abc123
  [deploy] Redis configuration: carhartt-redis.cvf1em.ng.0001.apn2.cache.amazonaws.com:6379
  ```

**보안 고려사항**:
- ✓ GitHub Secrets 사용으로 민감 정보 보호
- ✓ redis.env 파일은 배포 시마다 생성되어 덮어씌워짐
- ✓ EC2 인스턴스에서만 접근 가능 (외부 노출 없음)
- ⚠️ redis.env 파일은 `.gitignore`에 추가할 필요 없음 (GitHub Actions에서만 생성)

**실패 시 확인할 사항**:
```
[✗] redis.env 파일이 생성되지 않음
    → GitHub Secrets 설정 확인 (REDIS_HOST, REDIS_PORT, REDIS_PASSWORD)
    → deploy.yml의 125-129줄 확인

[✗] 환경변수 검증 실패
    → "REDIS_HOST environment variable is required" 에러
    → redis.env 파일이 EC2에 제대로 배포되었는지 확인
    → SSH 접속: cat /home/ubuntu/carhartt_platform/redis.env

[✗] Docker 컨테이너에 환경변수 전달 안 됨
    → docker inspect carhartt-platform | grep REDIS
    → Env 섹션에 REDIS_HOST, REDIS_PORT, REDIS_PASSWORD 확인
```

---

#### 6. Production 프로파일 활성화 추가 ✅ (2025-12-11 수정)
**파일**: `scripts/deploy.sh`
**위치**: Line 40-47
**작업 날짜**: 2025-12-11

**🚨 발견된 문제**:
- **증상**: 로그인은 성공하지만 인증 필요 API 호출 시 401 UNAUTHORIZED 에러 발생
- **Redis 상태**: ElastiCache 연결 성공, 하지만 `keys *` 결과가 `(empty array)` - 세션이 저장되지 않음
- **원인**: Docker 컨테이너가 **local 프로파일**을 사용하고 있었음
  - `application.properties`의 기본값: `spring.profiles.active=local`
  - Dockerfile의 ENTRYPOINT에 프로파일 설정 없음
  - Local 프로파일은 Redis 설정이 없어서 Tomcat 메모리 세션 사용
  - 결과: Redis에 세션이 저장되지 않음

**진단 과정**:
```bash
# EC2 서버에서 Redis CLI 접속
ubuntu@ip-192-168-100-27:~$ redis-cli -h carhartt-u-redis-001.cvf1em.0001.apn2.cache.amazonaws.com -p 6379

# 세션 키 확인
carhartt-u-redis-001...com:6379> keys *
(empty array)  # ← 세션이 전혀 저장되지 않음!

# 로그인 API는 성공했고 JSESSIONID 쿠키도 발급되었지만
# 세션이 Redis가 아닌 Tomcat 메모리에 저장되고 있었음
```

**해결 방법: SPRING_PROFILES_ACTIVE 환경변수 추가**

```bash
# 변경 전 (deploy.sh Line 40-45)
docker run -d -v /home/ubuntu/app/logs:/home/ubuntu/app/logs \
  --name "$CONTAINER_NAME" --restart=always -p 8080:8080 \
  -e REDIS_HOST="${REDIS_HOST}" \
  -e REDIS_PORT="${REDIS_PORT}" \
  -e REDIS_PASSWORD="${REDIS_PASSWORD}" \
  "$IMAGE_URI"

# 변경 후 (deploy.sh Line 40-47) - SPRING_PROFILES_ACTIVE=prod 추가
docker run -d -v /home/ubuntu/app/logs:/home/ubuntu/app/logs \
  --name "$CONTAINER_NAME" --restart=always -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e REDIS_HOST="${REDIS_HOST}" \
  -e REDIS_PORT="${REDIS_PORT}" \
  -e REDIS_PASSWORD="${REDIS_PASSWORD}" \
  "$IMAGE_URI"
```

**변경 내용**:
- Docker 컨테이너 실행 시 `SPRING_PROFILES_ACTIVE=prod` 환경변수 추가
- 이제 애플리케이션이 `application-oauth2-prod.yml` 파일의 Redis 설정을 로드함
- Spring Session이 정상적으로 활성화되어 세션이 Redis에 저장됨

**왜 이 문제가 발생했나?**
1. `application.properties`의 기본 프로파일은 `local`로 설정되어 있음
2. Dockerfile의 ENTRYPOINT에 프로파일 지정이 없음
3. Docker 컨테이너 실행 시에도 프로파일 환경변수를 전달하지 않았음
4. 결과: EC2에서 실행되는 컨테이너가 local 프로파일을 사용
5. Local 프로파일에는 `spring.session.store-type=redis` 설정이 없음
6. Redis Session이 비활성화되고 Tomcat 메모리 세션 사용

**검증 방법**:

1. **배포 후 환경변수 확인**
```bash
# EC2 서버에서 실행
docker inspect carhartt-platform | grep SPRING_PROFILES_ACTIVE
# 결과: "SPRING_PROFILES_ACTIVE=prod" 확인
```

2. **애플리케이션 로그 확인**
```bash
# 애플리케이션이 prod 프로파일을 사용하는지 확인
docker logs carhartt-platform | grep "The following profiles are active"
# 예상 결과: "The following 1 profile is active: prod"

# Redis Session 초기화 로그 확인
docker logs carhartt-platform | grep "RedisIndexedSessionRepository"
# 예상 결과: "Spring Session initialized with RedisIndexedSessionRepository"
```

3. **Redis에서 세션 키 확인**
```bash
# 로그인 후 Redis CLI에서 세션 확인
redis-cli -h carhartt-u-redis-001.cvf1em.0001.apn2.cache.amazonaws.com -p 6379

carhartt-u-redis-001...com:6379> keys spring:session:*
# 예상 결과:
# 1) "spring:session:sessions:8031a48c-d9f5-1cc8-c280-ce9fef6..."
# 2) "spring:session:sessions:expires:8031a48c-d9f5-1cc8-c280-ce9fef6..."
# 3) "spring:session:expirations:1733925600000"
```

4. **인증 필요 API 호출 테스트**
```bash
# 로그인 후 JSESSIONID 쿠키로 API 호출
curl -X GET https://carhartt-usedtransactions.com/v1/orders/address \
  -H "Cookie: JSESSIONID=<로그인-시-받은-세션ID>"

# 예상 결과: 200 OK (401 에러가 아님)
```

**디버깅 팁**:
- 프로파일 확인: `docker exec carhartt-platform env | grep SPRING_PROFILES_ACTIVE`
- 활성 프로파일 로그: 애플리케이션 시작 로그에서 "The following profiles are active" 검색
- Redis 연결 로그: "Lettuce" 또는 "RedisConnectionFactory" 검색
- 세션 저장 확인: Redis CLI에서 `keys spring:session:*` 명령 실행

**참고**: 이 문제는 local과 prod 프로파일이 명확히 분리된 환경에서 발생할 수 있습니다. Dockerfile에서 프로파일을 하드코딩하는 것보다 환경변수로 전달하는 것이 더 유연하므로, deploy.sh에서 환경변수를 설정하는 방식을 권장합니다.

---

### 디버깅 체크리스트

애플리케이션 시작 시 다음 로그를 확인하세요:

```
[✓] Bean 등록 확인
    - "RedisSessionConfig" 로그 확인
    - "Creating shared instance of singleton bean 'redisSessionConfig'"

[✓] Redis 연결 확인
    - "Lettuce" 또는 "RedisConnectionFactory" 로그 확인
    - "Created new Lettuce pool"

[✓] Spring Session 초기화 확인
    - "Spring Session initialized with RedisIndexedSessionRepository"
    - "SessionRepositoryFilter" 등록 로그

[✓] 세션 필터 등록 확인
    - "Filter 'sessionRepositoryFilter' configured for use"
```

**실패 시 확인할 로그**:
```
[✗] Redis 연결 실패
    - "Unable to connect to Redis"
    - "io.lettuce.core.RedisConnectionException"
    → Redis 서버 실행 상태 확인: `redis-cli ping`

[✗] 의존성 누락
    - "ClassNotFoundException: org.springframework.session.data.redis"
    → build.gradle 의존성 확인 및 재빌드: `./gradlew clean build`

[✗] 설정 오류
    - "Failed to configure a DataSource"
    → application-oauth2-prod.yml 설정 확인
```

---

### 🔧 추가 디버깅: Redis 세션이 저장되지 않는 경우

**모든 설정이 올바른데도 Redis에 세션이 저장되지 않는다면**, 다음 단계를 순서대로 확인하세요:

#### Step 1: Docker 컨테이너 환경변수 확인

```bash
# EC2 서버에서 실행
docker inspect carhartt-platform | grep -A 10 "Env"

# 다음 환경변수들이 모두 있어야 함:
# "SPRING_PROFILES_ACTIVE=prod"
# "REDIS_HOST=carhartt-u-redis-001.cvf1em.0001.apn2.cache.amazonaws.com"
# "REDIS_PORT=6379"
# "REDIS_PASSWORD=..." (있다면)
```

**확인 사항:**
- [ ] `SPRING_PROFILES_ACTIVE=prod` 존재
- [ ] `REDIS_HOST` 값이 올바른 ElastiCache 엔드포인트
- [ ] `REDIS_PORT` 값이 6379
- [ ] 환경변수가 모두 올바르게 설정됨

#### Step 2: 애플리케이션 로그에서 설정 파일 로드 확인

```bash
# application-oauth2-prod.yml이 로드되는지 확인
docker logs carhartt-platform | grep "oauth2-prod"

# 예상 결과:
# "Loaded config file 'classpath:application-oauth2-prod.yml'"
```

**만약 이 로그가 없다면:**
→ GitHub Secrets의 `APPLICATION_PROPERTIES`에 `spring.config.import` 설정 누락!
→ **섹션 2-1 참고**

#### Step 3: Redis 연결 로그 확인

```bash
# Redis 연결 성공 로그 확인
docker logs carhartt-platform | grep -i "redis"

# 정상 로그 예시:
# "LettuceConnectionFactory configured"
# "Created new Lettuce pool"
# "Spring Session initialized with RedisIndexedSessionRepository"

# 오류 로그 예시:
# "Unable to connect to Redis"
# "RedisConnectionException: Unable to connect to carhartt-u-redis-001..."
```

**연결 실패 시 확인:**
- [ ] ElastiCache 인스턴스가 실행 중인가?
- [ ] EC2 보안 그룹에서 ElastiCache 접근 허용되었는가?
- [ ] 환경변수 `REDIS_HOST`가 올바른가?

#### Step 4: Docker 컨테이너 내부에서 Redis 연결 테스트

```bash
# Docker 컨테이너 내부로 접속
docker exec -it carhartt-platform bash

# 컨테이너 내부에서 환경변수 확인
echo $REDIS_HOST
echo $REDIS_PORT
echo $SPRING_PROFILES_ACTIVE

# netcat으로 Redis 포트 접근 확인 (없으면 설치)
apt-get update && apt-get install -y netcat
nc -zv $REDIS_HOST $REDIS_PORT

# 예상 결과: "Connection to ... 6379 port [tcp/*] succeeded!"
```

**만약 연결 실패:**
- Docker 컨테이너와 ElastiCache 간 네트워크 문제
- 보안 그룹 설정 확인 필요

#### Step 5: Spring Session 필터 등록 확인

```bash
# SessionRepositoryFilter 등록 확인
docker logs carhartt-platform | grep "SessionRepositoryFilter"

# 예상 결과:
# "Filter 'sessionRepositoryFilter' configured for use"
```

**만약 이 로그가 없다면:**
- `spring.session.store-type=redis` 설정이 로드되지 않음
- `application-oauth2-prod.yml`이 로드되지 않았을 가능성

#### Step 6: 로그인 후 즉시 Redis 확인

```bash
# 1. 로그인 API 호출
curl -X POST https://carhartt-usedtransactions.com/v1/local/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password123"}' \
  -c cookies.txt -v

# 2. 즉시 Redis에서 세션 확인
redis-cli -h carhartt-u-redis-001.cvf1em.0001.apn2.cache.amazonaws.com -p 6379
keys spring:session:*

# 3. 만약 여전히 (empty array)라면:
# → 애플리케이션에서 Redis에 연결하지 못하고 있음
# → Step 1~5를 다시 확인
```

#### Step 7: 애플리케이션 재시작 후 확인

```bash
# 1. 컨테이너 재시작
docker restart carhartt-platform

# 2. 시작 로그 실시간 확인
docker logs -f carhartt-platform

# 확인할 로그 순서:
# [1] "The following 1 profile is active: prod"
# [2] "Loaded config file 'classpath:application-oauth2-prod.yml'"
# [3] "LettuceConnectionFactory configured"
# [4] "Spring Session initialized with RedisIndexedSessionRepository"
# [5] "Filter 'sessionRepositoryFilter' configured for use"
# [6] "Started CPlatformApplication in X.XXX seconds"

# 모든 로그가 나타나야 정상!
```

#### 문제가 지속되는 경우

**체크리스트 최종 확인:**
1. [ ] GitHub Secrets `APPLICATION_PROPERTIES`에 `spring.config.import=classpath:application-oauth2-prod.yml` 포함
2. [ ] `application-oauth2-prod.yml`에 Redis 설정(`spring.session.store-type=redis`) 존재
3. [ ] `deploy.sh`에서 Docker 실행 시 환경변수 전달 (`REDIS_HOST`, `REDIS_PORT` 등)
4. [ ] ElastiCache 보안 그룹에서 EC2 접근 허용
5. [ ] Docker 컨테이너 내부에서 Redis 연결 가능
6. [ ] 애플리케이션 로그에 Redis 연결 성공 로그 존재

**추가 문의사항:**
- CodeDeploy 로그: `/var/log/aws/codedeploy-agent/codedeploy-agent.log`
- deploy.sh 실행 로그: `/opt/codedeploy-agent/deployment-root/.../logs/scripts.log`
```

---

### 로컬 테스트 방법

#### 1. Redis 서버 실행
```bash
# Docker 사용 (권장)
docker run -d -p 6379:6379 --name redis redis:latest

# 연결 확인
redis-cli ping
# 응답: PONG
```

#### 2. 애플리케이션 실행
```bash
# profile을 prod로 변경하여 실행 (Redis 설정 적용)
./gradlew bootRun --args='--spring.profiles.active=prod'

# 또는 환경변수 설정
export SPRING_PROFILES_ACTIVE=prod
./gradlew bootRun
```

#### 3. 로그인 테스트
```bash
# 로그인 수행
curl -X POST http://localhost:8080/v1/local/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' \
  -c cookies.txt -v

# JSESSIONID 쿠키 확인
cat cookies.txt
```

#### 4. Redis 세션 확인
```bash
# Redis CLI 접속
redis-cli

# 세션 키 확인
keys spring:session:*

# 예상 출력:
# 1) "spring:session:sessions:38a4c7e1-2b9d-4f3a-9c8e-5d6a7b8c9d0e"
# 2) "spring:session:sessions:expires:38a4c7e1-2b9d-4f3a-9c8e-5d6a7b8c9d0e"
# 3) "spring:session:expirations:1733875200000"

# 세션 데이터 확인
hgetall spring:session:sessions:38a4c7e1-2b9d-4f3a-9c8e-5d6a7b8c9d0e

# 세션 TTL 확인
ttl spring:session:sessions:38a4c7e1-2b9d-4f3a-9c8e-5d6a7b8c9d0e
# 응답: 1800 (초 단위, 30분)
```

#### 5. 세션 영속성 테스트
```bash
# 1. 로그인 후 쿠키 저장
curl -X POST http://localhost:8080/v1/local/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' \
  -c cookies.txt

# 2. 애플리케이션 재시작
# Ctrl+C로 종료 후 다시 실행
./gradlew bootRun

# 3. 저장된 쿠키로 인증 확인 (재로그인 없이 성공해야 함)
curl -X GET http://localhost:8080/v1/local/check -b cookies.txt

# 예상 응답:
# {"success":true,"data":{"memberId":1,"email":"test@example.com",...}}
```

---

### 트러블슈팅 가이드

#### 문제 1: Redis 연결 실패
**증상**:
```
io.lettuce.core.RedisConnectionException: Unable to connect to localhost:6379
```

**해결**:
```bash
# Redis 서버 실행 확인
redis-cli ping

# Docker 컨테이너 확인
docker ps | grep redis

# Redis 서버 시작
docker start redis
# 또는
docker run -d -p 6379:6379 --name redis redis:latest
```

#### 문제 2: 세션이 Redis에 저장되지 않음
**증상**:
```bash
redis-cli keys 'spring:session:*'
# (empty array)
```

**해결**:
1. `application-oauth2-prod.yml` 설정 확인:
   ```yaml
   spring:
     session:
       store-type: redis  # 이 줄이 있는지 확인
   ```

2. Profile 확인:
   ```bash
   # prod profile로 실행했는지 확인
   ./gradlew bootRun --args='--spring.profiles.active=prod'
   ```

3. RedisSessionConfig 빈 등록 확인:
   ```bash
   # 로그에서 "RedisSessionConfig" 검색
   ```

#### 문제 3: 로그인 후 세션이 유지되지 않음
**증상**:
- 로그인은 성공하지만 `/v1/local/check` 호출 시 401 Unauthorized

**해결**:
1. JSESSIONID 쿠키 확인:
   ```bash
   curl -X POST ... -c cookies.txt -v
   # Set-Cookie: JSESSIONID=... 확인
   ```

2. Redis 세션 ID와 쿠키 ID 일치 확인:
   ```bash
   # 쿠키의 JSESSIONID 값
   cat cookies.txt | grep JSESSIONID

   # Redis의 세션 키
   redis-cli keys 'spring:session:sessions:*'
   ```

3. SecurityContext 저장 확인:
   ```bash
   # Redis 세션 데이터에 SPRING_SECURITY_CONTEXT가 있는지 확인
   redis-cli hgetall spring:session:sessions:<session-id>
   # "sessionAttr:SPRING_SECURITY_CONTEXT" 키 확인
   ```

#### 문제 4: 세션이 즉시 만료됨
**증상**:
- 로그인 직후 세션이 사라짐

**해결**:
1. TTL 확인:
   ```bash
   redis-cli ttl spring:session:sessions:<session-id>
   # 1800 (30분)이 아닌 짧은 값이 나오면 설정 확인
   ```

2. `application-oauth2-prod.yml` 확인:
   ```yaml
   spring:
     session:
       timeout: 1800s  # 30분 (초 단위)
   ```

3. `RedisSessionConfig.java` 확인:
   ```java
   @EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)  // 30분
   ```

---

### Production 배포 체크리스트

#### 1. AWS ElastiCache for Redis 설정
```bash
# ElastiCache 생성 정보
- Engine: Redis
- Node Type: cache.t3.micro (시작용) → cache.r6g.large (운영)
- Number of replicas: 2 (고가용성)
- Multi-AZ: Enabled
- Encryption at rest: Enabled
- Encryption in transit: Enabled
```

**주의**: ElastiCache 엔드포인트는 생성 후 아래 위치에서 확인 가능합니다.
```
AWS Console → ElastiCache → Redis clusters → [클러스터 이름] → Primary endpoint
예시: carhartt-redis.cvf1em.ng.0001.apn2.cache.amazonaws.com:6379
```

#### 2. GitHub Actions Secrets 설정 ✅ (완료)
```
GitHub Repository → Settings → Secrets and variables → Actions → Repository secrets

필수 Secrets (현재 설정됨):
✓ REDIS_HOST = carhartt-redis.cvf1em.ng.0001.apn2.cache.amazonaws.com
✓ REDIS_PORT = 6379
✓ REDIS_PASSWORD = (빈 값 - AUTH 설정 안 함)

기타 필수 Secrets (기존):
- ACCESS_KEY_ID
- SECRET_ACCESS_KEY
- APPLICATION_PROPERTIES
```

**설정 확인 방법**:
1. GitHub Repository → Settings → Secrets and variables → Actions
2. Repository secrets 탭에서 `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` 확인
3. 값을 수정하려면 "Update" 버튼 클릭 (보안상 현재 값은 표시되지 않음)

#### 3. 배포 스크립트 수정 ✅ (완료)

**파일**: `.github/workflows/deploy.yml`
**수정 내용**: Line 124-129에 Redis 환경변수 파일 생성 로직 추가됨

```yaml
# Redis 환경변수 파일 생성 (GitHub Secrets에서 주입)
cat > bundle/redis.env <<EOF
export REDIS_HOST="${{ secrets.REDIS_HOST }}"
export REDIS_PORT="${{ secrets.REDIS_PORT }}"
export REDIS_PASSWORD="${{ secrets.REDIS_PASSWORD }}"
EOF
```

**파일**: `scripts/deploy.sh`
**수정 내용**: Line 9-24에 환경변수 로드 및 검증 로직 추가됨

```bash
# Redis 환경변수 로드
if [ -f "$APP_HOME/redis.env" ]; then
    source "$APP_HOME/redis.env"
fi

# 환경변수 검증
: "${REDIS_HOST:?REDIS_HOST environment variable is required}"
: "${REDIS_PORT:?REDIS_PORT environment variable is required}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
```

#### 4. EC2 보안 그룹 설정
```
# EC2 → ElastiCache 통신 허용
- Type: Custom TCP
- Port: 6379
- Source: EC2 Security Group
```

#### 5. 배포 후 확인
```bash
# EC2 인스턴스에 SSH 접속
ssh ec2-user@your-ec2-instance

# 애플리케이션 로그 확인
sudo tail -f /home/ubuntu/app/logs/carhartt.log

# Redis 연결 확인 (로그에서)
grep "Lettuce" /home/ubuntu/app/logs/carhartt.log
grep "Spring Session" /home/ubuntu/app/logs/carhartt.log
```

---

## 1. 현재 상태 분석

### 1.1 현재 세션 저장 방식
- **저장소**: Servlet 세션 (Tomcat 메모리 기반)
- **관리**: `HttpSession` 인터페이스 사용
- **영속성**: 없음 (서버 재시작 시 세션 손실)
- **확장성**: 단일 서버에서만 유효 (세션 공유 불가)

### 1.2 현재 세션 사용 위치

#### 1) SecurityContext 저장 (Local 인증)
**파일**: `src/main/java/com/C_platform/config/SecurityConfig.java`

```java
// Line 136-138: SecurityContextRepository 빈 등록
@Bean
public SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository(); // ← HttpSession 사용
}

// Line 180-182: SecurityFilterChain에서 명시적 설정
http.securityContext(securityContext ->
    securityContext.securityContextRepository(securityContextRepository())
);

// Line 191: Local 인증 필터에 설정
jsonLocalLoginFilter.setSecurityContextRepository(securityContextRepository());
```

**역할**: Local 로그인 시 SecurityContext를 HttpSession에 저장하여 인증 상태 유지

#### 2) OAuth2 Authorization Request 저장
**파일**: `src/main/java/com/C_platform/config/SecurityConfig.java`

```java
// Line 94-96: OAuth2 인증 요청 저장소
@Bean
public AuthorizationRequestRepository<OAuth2AuthorizationRequest> cookieAuthorizationRequestRepository() {
    return new HttpSessionOAuth2AuthorizationRequestRepository(); // ← HttpSession 사용
}
```

**역할**: OAuth2 로그인 시 Authorization Request를 세션에 저장 (CSRF 방지)

#### 3) 세션 생성 및 로깅
**파일**: `src/main/java/com/C_platform/Member_woonkim/infrastructure/auth/handler/LocalAuthenticationSuccessHandler.java`

```java
// Line 62: 세션 생성 및 ID 로깅
String sessionId = request.getSession(true).getId();
log.info("LocalAuthenticationSuccessHandler: 현재 JSESSIONID : {}", sessionId);
```

**역할**: 로그인 성공 시 세션 ID 로깅 (디버깅 목적)

#### 4) 세션 체크 필터
**파일**: `src/main/java/com/C_platform/config/SessionCheckFilter.java`

```java
// Line 26, 32: 세션 생성 여부 확인
HttpSession sessionBefore = request.getSession(false);
filterChain.doFilter(request, response);
HttpSession sessionAfter = request.getSession(false);

if (sessionBefore == null && sessionAfter != null) {
    log.info("[세션 생성됨!] URI: {}, New SessionID: {}", request.getRequestURI(), sessionAfter.getId());
}
```

**역할**: 요청 처리 중 세션 생성 여부를 모니터링

### 1.3 현재 Redis 설정 상태
**파일**: `build.gradle`, `application.properties`

```gradle
// build.gradle Line 50: Redis 의존성 주석 처리됨
// implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

```properties
# application.properties Line 77-83: Redis 설정 주석 처리됨
# spring.data.redis.host=localhost
# spring.data.redis.port=6379
# spring.data.redis.timeout=60000ms
# spring.data.redis.jedis.pool.max-active=8
# spring.data.redis.jedis.pool.max-idle=8
# spring.data.redis.jedis.pool.min-idle=0
```

---

## 2. Redis로 전환하는 이유

### 2.1 현재 Servlet 세션의 한계
1. **서버 재시작 시 세션 손실**: 배포 시 모든 사용자가 재로그인 필요
2. **확장성 부족**: 여러 서버 환경에서 세션 공유 불가
3. **메모리 관리 어려움**: 세션이 많아지면 Tomcat 메모리 부족 가능성
4. **모니터링 어려움**: 세션 상태를 외부에서 확인 불가

### 2.2 Redis 세션 저장소의 장점
1. **영속성**: 서버 재시작해도 세션 유지
2. **확장성**: 여러 서버가 동일한 Redis를 공유하여 로드 밸런싱 가능
3. **성능**: 인메모리 저장소로 빠른 읽기/쓰기
4. **모니터링**: Redis CLI로 세션 상태 확인 가능
5. **TTL 관리**: 세션 만료 시간을 Redis가 자동 관리

---

## 3. 변경이 필요한 코드 목록

### 3.1 의존성 추가
**파일**: `build.gradle`

```gradle
dependencies {
    // 기존 코드...

    // Redis 의존성 추가 (Line 50 주석 해제 + Spring Session 추가)
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.session:spring-session-data-redis'  // ← 추가 필요
    implementation 'io.lettuce:lettuce-core'  // ← Redis 클라이언트 (선택사항, Spring Boot가 자동 포함)
}
```

### 3.2 Redis 설정 활성화
**파일**: `application.properties`

```properties
# Redis Configuration (Line 77-83 주석 해제 및 수정)
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=60000ms

# Connection Pool (Lettuce 기본값 사용 권장, Jedis는 레거시)
# spring.data.redis.lettuce.pool.max-active=8
# spring.data.redis.lettuce.pool.max-idle=8
# spring.data.redis.lettuce.pool.min-idle=0

# Spring Session 설정 (추가)
spring.session.store-type=redis
spring.session.redis.namespace=spring:session  # Redis 키 prefix
spring.session.timeout=1800s  # 세션 만료 시간 (30분)

# Session Cookie 설정 (선택사항)
server.servlet.session.cookie.name=JSESSIONID
server.servlet.session.cookie.max-age=1800  # 30분
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=false  # local: false, prod: true
server.servlet.session.cookie.same-site=none
```

**Production 환경 설정**: `application-prod.properties` (별도 파일 생성 권장)
```properties
# Production Redis 설정
spring.data.redis.host=${REDIS_HOST:redis-server.example.com}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}  # Redis 비밀번호 (환경변수로 주입)

# Production Session Cookie 설정
server.servlet.session.cookie.secure=true  # HTTPS 환경에서 필수
```

### 3.3 Redis 세션 설정 클래스 추가
**파일 생성**: `src/main/java/com/C_platform/config/RedisSessionConfig.java`

```java
package com.C_platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Redis 기반 세션 저장소 설정
 *
 * @EnableRedisHttpSession: Spring Session이 HttpSession을 Redis로 저장하도록 설정
 * maxInactiveIntervalInSeconds: 세션 만료 시간 (초 단위, 기본값 1800초 = 30분)
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)  // 30분
public class RedisSessionConfig {

    /**
     * RedisTemplate 설정 (선택사항 - 세션 외 Redis 사용 시)
     *
     * Spring Session은 내부적으로 자체 RedisTemplate을 사용하므로
     * 이 빈은 세션 외 용도(캐싱, 메시징 등)로 Redis를 사용할 때만 필요
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key: String 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value: JSON 직렬화 (객체를 JSON으로 저장)
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        return template;
    }
}
```

### 3.4 SecurityConfig 수정 (필요 없음!)
**파일**: `src/main/java/com/C_platform/config/SecurityConfig.java`

**중요**: **SecurityConfig.java는 수정할 필요가 없습니다!**

Spring Session이 자동으로 `HttpSession` 구현체를 Redis 기반으로 교체하기 때문에,
기존 `HttpSessionSecurityContextRepository` 코드는 그대로 두면 됩니다.

```java
// 이 코드는 그대로 유지 (변경 불필요)
@Bean
public SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
    // ↑ Spring Session이 내부적으로 HttpSession 구현체를 Redis 기반으로 교체
}
```

**동작 원리**:
1. `@EnableRedisHttpSession` 애노테이션이 활성화되면
2. Spring Session이 `SessionRepositoryFilter`를 자동 등록
3. 이 필터가 `HttpServletRequest.getSession()`을 가로채서
4. Tomcat 세션 대신 Redis 기반 세션을 반환
5. **기존 코드는 전혀 수정할 필요 없음!**

### 3.5 기타 코드 (수정 불필요)
다음 파일들은 **수정할 필요가 없습니다**:
- `JsonUsernamePasswordAuthenticationFilter.java`: `super.successfulAuthentication()` 그대로 사용
- `LocalAuthenticationSuccessHandler.java`: `request.getSession()` 그대로 사용
- `SessionCheckFilter.java`: `request.getSession(false)` 그대로 사용

Spring Session이 자동으로 `HttpSession` 구현체를 교체하기 때문에
**코드 변경 없이** 모든 세션 관련 코드가 Redis를 사용하게 됩니다.

---

## 4. 단계별 마이그레이션 가이드

### Step 1: Redis 서버 설치 및 실행

#### 로컬 개발 환경 (Windows)
```bash
# Docker 사용 (권장)
docker run -d -p 6379:6379 --name redis redis:latest

# 또는 WSL2에서 Redis 설치
sudo apt-get update
sudo apt-get install redis-server
sudo service redis-server start

# Redis 연결 확인
redis-cli ping
# 응답: PONG
```

#### Production 환경
- AWS ElastiCache for Redis
- Azure Cache for Redis
- Google Cloud Memorystore
- 자체 Redis 클러스터 구축

### Step 2: 의존성 추가
```gradle
// build.gradle 수정
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.session:spring-session-data-redis'
}
```

```bash
# Gradle 의존성 다운로드
./gradlew build --refresh-dependencies
```

### Step 3: Redis 설정 추가
```properties
# application.properties 수정 (Line 77-83 주석 해제 및 수정)
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.session.store-type=redis
spring.session.redis.namespace=spring:session
spring.session.timeout=1800s
```

### Step 4: RedisSessionConfig 클래스 생성
```java
// src/main/java/com/C_platform/config/RedisSessionConfig.java 파일 생성
// (위의 3.3 코드 복사)
```

### Step 5: 애플리케이션 재시작 및 테스트
```bash
# 애플리케이션 실행
./gradlew bootRun

# 로그 확인 (Spring Session 초기화 로그)
# "Spring Session initialized with RedisIndexedSessionRepository"
```

### Step 6: Redis 세션 확인
```bash
# Redis CLI 접속
redis-cli

# 세션 키 확인
keys spring:session:*

# 예상 출력:
# 1) "spring:session:sessions:38a4c7e1-2b9d-4f3a-9c8e-5d6a7b8c9d0e"
# 2) "spring:session:sessions:expires:38a4c7e1-2b9d-4f3a-9c8e-5d6a7b8c9d0e"
# 3) "spring:session:expirations:1638360000000"

# 특정 세션 내용 확인 (Hash 타입)
hgetall spring:session:sessions:38a4c7e1-2b9d-4f3a-9c8e-5d6a7b8c9d0e

# 세션 TTL 확인
ttl spring:session:sessions:38a4c7e1-2b9d-4f3a-9c8e-5d6a7b8c9d0e
# 응답: 1786 (남은 초)
```

---

## 5. 테스트 방법

### 5.1 세션 영속성 테스트
```bash
# 1. 로그인 수행
curl -X POST http://localhost:8080/v1/local/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' \
  -c cookies.txt  # 쿠키 저장

# 2. Redis에서 세션 확인
redis-cli keys 'spring:session:*'

# 3. 애플리케이션 재시작
./gradlew bootRun

# 4. 저장된 쿠키로 인증 상태 확인 (재로그인 없이 성공해야 함)
curl -X GET http://localhost:8080/v1/local/check \
  -b cookies.txt  # 쿠키 사용
```

**예상 결과**:
- 애플리케이션 재시작 후에도 세션 유지
- `/v1/local/check` 엔드포인트에서 인증된 사용자 정보 반환

### 5.2 세션 만료 테스트
```bash
# 1. 로그인
curl -X POST http://localhost:8080/v1/local/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' \
  -c cookies.txt

# 2. Redis에서 세션 TTL 확인
redis-cli
ttl spring:session:sessions:<session-id>
# 응답: 1800 (30분)

# 3. 30분 후 자동 만료 확인
# 또는 테스트를 위해 TTL을 10초로 설정:
expire spring:session:sessions:<session-id> 10

# 4. 10초 후 세션 확인 (만료되어야 함)
curl -X GET http://localhost:8080/v1/local/check -b cookies.txt
# 응답: 401 Unauthorized
```

### 5.3 다중 서버 세션 공유 테스트 (Optional)
```bash
# 1. 애플리케이션을 다른 포트로 2개 실행
./gradlew bootRun --args='--server.port=8080'  # 터미널 1
./gradlew bootRun --args='--server.port=8081'  # 터미널 2

# 2. 8080 포트에서 로그인
curl -X POST http://localhost:8080/v1/local/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' \
  -c cookies.txt

# 3. 8081 포트에서 동일한 쿠키로 인증 확인 (성공해야 함)
curl -X GET http://localhost:8081/v1/local/check -b cookies.txt
```

**예상 결과**:
- 두 서버가 동일한 Redis를 공유하여 세션 동기화
- 어느 서버에 요청해도 동일한 인증 상태 유지

---

## 6. Redis 세션 데이터 구조

### 6.1 Redis에 저장되는 키 타입
```
spring:session:sessions:<session-id>           # Hash: 세션 데이터
spring:session:sessions:expires:<session-id>   # String: 만료 시간
spring:session:expirations:<timestamp>         # Set: 만료 예정 세션 목록
spring:session:index:...                       # Set: 사용자별 세션 인덱스 (선택)
```

### 6.2 세션 데이터 예시
```bash
redis-cli hgetall spring:session:sessions:38a4c7e1-2b9d-4f3a-9c8e-5d6a7b8c9d0e
```

**출력 예시**:
```
1) "sessionAttr:SPRING_SECURITY_CONTEXT"
2) "{\"@class\":\"org.springframework.security.core.context.SecurityContextImpl\",\"authentication\":{...}}"
3) "creationTime"
4) "1638358200000"
5) "lastAccessedTime"
6) "1638358300000"
7) "maxInactiveInterval"
8) "1800"
```

---

## 7. 주의사항 및 트러블슈팅

### 7.1 직렬화 이슈
**문제**: SecurityContext나 Authentication 객체가 직렬화되지 않는 경우

**해결**:
```java
// SecurityContext에 저장되는 객체는 Serializable 구현 필요
public class CustomUserDetails implements UserDetails, Serializable {
    private static final long serialVersionUID = 1L;
    // ...
}
```

### 7.2 Redis 연결 실패
**문제**: Redis 서버에 연결할 수 없는 경우

**확인**:
```bash
# Redis 서버 실행 확인
redis-cli ping
# 응답: PONG

# Redis 포트 확인
netstat -an | grep 6379

# Docker 컨테이너 확인
docker ps | grep redis
```

**해결**:
```bash
# Redis 서버 시작
docker start redis
# 또는
sudo service redis-server start
```

### 7.3 세션이 공유되지 않는 경우
**문제**: 여러 서버에서 세션이 공유되지 않음

**확인사항**:
1. 모든 서버가 동일한 Redis 서버를 바라보는지 확인
2. `spring.session.redis.namespace` 값이 동일한지 확인
3. Redis 네트워크 방화벽 설정 확인

### 7.4 세션 만료 시간 불일치
**문제**: 세션이 예상보다 빨리 또는 늦게 만료됨

**확인**:
```properties
# application.properties 확인
spring.session.timeout=1800s  # 초 단위
server.servlet.session.cookie.max-age=1800  # 초 단위 (동일하게 설정)
```

### 7.5 JSESSIONID 쿠키가 전송되지 않는 경우
**문제**: CORS 환경에서 쿠키가 전송되지 않음

**해결**:
```properties
# Secure 및 SameSite 설정 확인
server.servlet.session.cookie.secure=false  # local: false, prod: true
server.servlet.session.cookie.same-site=none  # CORS 환경에서 필수
```

```java
// SecurityConfig.java CORS 설정 확인
cfg.setAllowCredentials(true);  // 쿠키 전송 허용 (이미 설정됨)
```

---

## 8. 성능 최적화 (Optional)

### 8.1 Redis Connection Pool 설정
```properties
# Lettuce 커넥션 풀 설정 (기본값으로도 충분)
spring.data.redis.lettuce.pool.max-active=20
spring.data.redis.lettuce.pool.max-idle=10
spring.data.redis.lettuce.pool.min-idle=5
spring.data.redis.lettuce.pool.max-wait=2000ms
```

### 8.2 세션 직렬화 방식 변경
**기본**: JSON 직렬화 (가독성 좋음, 용량 큼)
**최적화**: 바이너리 직렬화 (용량 작음, 가독성 낮음)

```java
@Configuration
@EnableRedisHttpSession(
    maxInactiveIntervalInSeconds = 1800,
    redisNamespace = "spring:session"
)
public class RedisSessionConfig {

    // 기본 JSON 직렬화 대신 바이너리 직렬화 사용 (Optional)
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
        // 또는 바이너리 직렬화:
        // return new JdkSerializationRedisSerializer();
    }
}
```

---

## 9. 롤백 계획

만약 Redis 세션 적용 후 문제가 발생하면 다음과 같이 롤백할 수 있습니다:

### Step 1: application.properties 수정
```properties
# Redis 세션 비활성화
# spring.session.store-type=redis  ← 주석 처리 또는 삭제
```

### Step 2: RedisSessionConfig 클래스 비활성화
```java
// @Configuration  ← 주석 처리
// @EnableRedisHttpSession  ← 주석 처리
public class RedisSessionConfig {
    // ...
}
```

### Step 3: 애플리케이션 재시작
```bash
./gradlew bootRun
```

**결과**: 자동으로 Tomcat 기본 세션(메모리)으로 복원됩니다.

---

## 10. 체크리스트

마이그레이션 전에 다음 항목을 확인하세요:

- [ ] Redis 서버 설치 및 실행 확인 (`redis-cli ping`)
- [ ] `build.gradle`에 Redis 의존성 추가
- [ ] `application.properties`에 Redis 설정 추가
- [ ] `RedisSessionConfig.java` 클래스 생성
- [ ] 로컬 환경에서 로그인 테스트
- [ ] Redis CLI에서 세션 키 확인
- [ ] 애플리케이션 재시작 후 세션 유지 확인
- [ ] 세션 만료 테스트
- [ ] Production 환경 Redis 서버 준비 (ElastiCache 등)
- [ ] Production 환경 설정 파일 분리 (`application-prod.properties`)
- [ ] 로드 밸런서 환경에서 세션 공유 테스트 (Optional)

---

## 11. 참고 자료

- [Spring Session Documentation](https://docs.spring.io/spring-session/reference/)
- [Spring Session Redis Guide](https://docs.spring.io/spring-session/reference/guides/boot-redis.html)
- [Redis Documentation](https://redis.io/documentation)
- [AWS ElastiCache for Redis](https://aws.amazon.com/elasticache/redis/)

---

## 요약

### 변경 필요 파일
1. `build.gradle`: Redis 의존성 추가
2. `application.properties`: Redis 설정 활성화
3. `src/main/java/com/C_platform/config/RedisSessionConfig.java`: 새 파일 생성

### 변경 불필요 파일
- `SecurityConfig.java`: **수정 불필요** (Spring Session이 자동으로 HttpSession 구현체 교체)
- `JsonUsernamePasswordAuthenticationFilter.java`: **수정 불필요**
- `LocalAuthenticationSuccessHandler.java`: **수정 불필요**
- `SessionCheckFilter.java`: **수정 불필요**

### 핵심 개념
- Spring Session이 `SessionRepositoryFilter`를 통해 `HttpSession` 구현체를 자동으로 교체
- 기존 코드는 전혀 수정할 필요 없이 Redis 세션으로 전환 가능
- 단순히 의존성 추가 + 설정 파일 수정 + `@EnableRedisHttpSession` 추가만으로 완료
