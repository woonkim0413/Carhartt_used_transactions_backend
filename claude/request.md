## ✅ 작업 완료: Redis 환경변수 설정 간소화 (2025-12-11)

### 요청 사항
- GitHub Actions Secrets에 REDIS_PORT를 저장하지 않음 (어차피 6379 사용)
- REDIS_PORT는 6379로 하드코딩
- application-oauth2-prod.yml 및 deploy.sh 변경
- REDIS_HOST만 필수 검증, 나머지는 없어도 동작하도록 수정

### 완료된 작업

#### 1. application-oauth2-prod.yml 수정 ✅
**파일**: `src/main/resources/application-oauth2-prod.yml` (Line 78)

**변경 내용**:
```yaml
# 변경 전
port: ${REDIS_PORT:6379}  # 환경변수로 주입, 기본값 6379

# 변경 후
port: 6379  # 하드코딩 (표준 Redis 포트)
```

#### 2. deploy.sh 수정 ✅
**파일**: `scripts/deploy.sh` (Line 17-24)

**변경 내용**:
```bash
# 변경 전
: "${REDIS_HOST:?REDIS_HOST environment variable is required}"
: "${REDIS_PORT:?REDIS_PORT environment variable is required}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

# 변경 후
# Redis 환경변수 검증 (REDIS_HOST만 필수)
: "${REDIS_HOST:?REDIS_HOST environment variable is required}"

# REDIS_PORT는 하드코딩 (표준 Redis 포트 6379)
REDIS_PORT="${REDIS_PORT:-6379}"

# REDIS_PASSWORD는 선택사항 (비밀번호 없는 경우 빈 문자열)
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
```

**개선점**:
- REDIS_HOST만 필수 검증 (`${VAR:?message}` 문법 사용)
- REDIS_PORT는 기본값 6379로 설정 (`${VAR:-default}` 문법 사용)
- REDIS_PASSWORD는 선택사항으로 빈 문자열 기본값

#### 3. deploy.yml 수정 ✅
**파일**: `.github/workflows/deploy.yml` (Line 124-128)

**변경 내용**:
```yaml
# 변경 전
cat > bundle/redis.env <<EOF
export REDIS_HOST="${{ secrets.REDIS_HOST }}"
export REDIS_PORT="${{ secrets.REDIS_PORT }}"
export REDIS_PASSWORD="${{ secrets.REDIS_PASSWORD }}"
EOF

# 변경 후
# Redis 환경변수 파일 생성 (GitHub Secrets에서 주입)
# REDIS_HOST만 필수, PORT는 6379로 하드코딩, PASSWORD는 선택사항
cat > bundle/redis.env <<EOF
export REDIS_HOST="${{ secrets.REDIS_HOST }}"
EOF
```

**개선점**:
- redis.env 파일에 REDIS_HOST만 포함
- REDIS_PORT와 REDIS_PASSWORD는 deploy.sh에서 기본값 사용

#### 4. redis.md 문서 업데이트 ✅
**파일**: `claude/redis.md`

**업데이트 내용**:
- "🚨 긴급: CodeDeploy AfterInstall 실패" 섹션 전체 업데이트
- 배포 흐름 다이어그램 수정 (REDIS_PORT 하드코딩 반영)
- 필수 Secrets 테이블 업데이트
- 체크리스트 간소화

### 최종 결과

**GitHub Secrets 요구사항**:
| Secret 이름 | 필수 여부 | 설명 |
|-------------|----------|------|
| `REDIS_HOST` | ✅ **필수** | ElastiCache 엔드포인트 |
| ~~`REDIS_PORT`~~ | ❌ 불필요 | 6379로 하드코딩됨 |
| ~~`REDIS_PASSWORD`~~ | ❌ 불필요 | 빈 문자열 기본값 |

**이점**:
- ✅ GitHub Secrets 관리 간소화 (REDIS_HOST만 추가)
- ✅ 배포 설정 단순화
- ✅ 표준 포트(6379) 고정으로 설정 오류 방지
- ✅ REDIS_HOST 누락 시 명확한 에러 메시지
- ✅ REDIS_PORT/PASSWORD는 자동으로 기본값 사용

### 다음 단계

```bash
# 1. GitHub Secrets에 REDIS_HOST 추가
# Repository → Settings → Secrets and variables → Actions
# New repository secret:
#   Name: REDIS_HOST
#   Secret: carhartt-u-redis-001.cvf1em.0001.apn2.cache.amazonaws.com

# 2. 코드 커밋 및 푸시
git add src/main/resources/application-oauth2-prod.yml
git add scripts/deploy.sh
git add .github/workflows/deploy.yml
git add claude/redis.md
git add claude/request.md
git commit -m "feat: Hardcode REDIS_PORT to 6379, require only REDIS_HOST in Secrets"
git push origin main

# 3. 배포 성공 확인
# GitHub Actions → 모든 단계 성공 확인
# CodeDeploy → "2 of 2 instances updated - Succeeded" 확인
```

### 참고 문서
- 상세 분석: `claude/redis.md` (🚨 긴급: CodeDeploy AfterInstall 실패 섹션)
