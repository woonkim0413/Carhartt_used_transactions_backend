# Deploy 환경변수 주입 가이드

## 개요

`application.properties`와 `env_values/.env`를 참조하여 `.github/workflows/deploy.yml` 및 `scripts/deploy.sh`를 수정하는 방법을 설명합니다.

GitHub Secrets에 저장된 `.env` 파일을 deploy agent에 전달하고, Docker 컨테이너 실행 시 `--env-file` 플래그를 사용하여 모든 환경변수를 주입하는 방식으로 개선합니다.

---

## 1. 문제점 분석

### 현재 구조의 문제

**환경변수 요구사항:**
- `application.properties`는 총 11개의 환경변수를 필요로 합니다:
  - Database: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
  - AWS S3: `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`
  - Kakao Pay: `KAKAO_PAY_AUTHORIZATION`
  - Naver Pay: `NAVER_PAY_CLIENT_ID`, `NAVER_PAY_CLIENT_SECRET`, `NAVER_PAY_CHAIN_ID`
  - Email: `MAIL_USERNAME`, `MAIL_PASSWORD`
  - Redis: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` (이미 전달 중)

**현재 배포 프로세스:**
1. `env_values/.env` 파일에는 모든 환경변수가 정의되어 있음
2. GitHub Actions는 이 파일 전체를 `redis.env`로 bundle에 포함
3. **하지만** `deploy.sh`에서는 Redis 관련 환경변수 3개만 컨테이너에 전달
4. 나머지 8개 환경변수(DB, AWS, Payment, Email)가 누락됨

**결과:**
- Spring Boot 실행 시 환경변수 참조 실패
- 데이터베이스 연결 실패, S3 업로드 실패 등

---

## 2. 해결방안

### 개선 방향

1. **파일명 명확화**: `redis.env` → `application.env`
2. **전체 환경변수 주입**: `--env-file` 플래그 사용
3. **로직 간소화**: 개별 환경변수 검증 제거

---

## 3. deploy.yml 수정

### 파일 위치
`.github/workflows/deploy.yml`

### 수정 대상
Line 124-128

### 수정 전
```yaml
        # Redis 환경변수 파일 생성 (GitHub Secrets에서 주입)
        # REDIS_HOST만 필수, PORT는 6379로 하드코딩, PASSWORD는 선택사항, <<EOF 값은 cat에 전달
          cat > bundle/redis.env <<EOF
               ${{ secrets.APPLICATION_ENV_FILE }}
          EOF
```

### 수정 후
```yaml
        # 애플리케이션 환경변수 파일 생성 (GitHub Secrets에서 주입)
        # 모든 필요한 환경변수를 포함 (DB, AWS, Redis, Payment, Email)
          cat > bundle/application.env <<EOF
${{ secrets.APPLICATION_ENV_FILE }}
          EOF
```

### 변경사항 요약

| 항목 | 수정 전 | 수정 후 |
|------|---------|---------|
| 파일명 | `redis.env` | `application.env` |
| 주석 | Redis 전용으로 오해 | 모든 환경변수 포함을 명시 |
| 줄바꿈 | `<<EOF` 뒤에 공백 | 공백 제거하여 정확한 값 전달 |

---

## 4. deploy.sh 수정

### 파일 위치
`scripts/deploy.sh`

### 수정 대상
Line 9-50 (환경변수 로드 및 Docker 실행 부분)

### 수정 전
```bash
# Redis 환경변수 로드 (GitHub Actions에서 생성한 파일)
if [ -f "$APP_HOME/redis.env" ]; then
    echo "[deploy] Loading Redis configuration from redis.env"
    source "$APP_HOME/redis.env"
else
    echo "[deploy] WARNING: redis.env not found, using environment variables"
fi

# Redis 환경변수 검증 (REDIS_HOST만 필수)
: "${REDIS_HOST:?REDIS_HOST environment variable is required}"

# REDIS_PORT는 하드코딩 (표준 Redis 포트 6379)
REDIS_PORT="${REDIS_PORT:-6379}"

# REDIS_PASSWORD는 선택사항 (비밀번호 없는 경우 빈 문자열)
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

echo "[deploy] Using image: $IMAGE_URI"
echo "[deploy] Redis configuration: $REDIS_HOST:$REDIS_PORT"

# 1) ECR 로그인 (EC2 인스턴스 롤에 ecr:GetAuthorizationToken 등 Pull 권한 필수)
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$(echo "$IMAGE_URI" | awk -F/ '{print $1}')"

# 2) 최신 이미지 Pull
docker pull "$IMAGE_URI"

# 3) 기존 컨테이너 중지/삭제(있다면)
docker rm -f "$CONTAINER_NAME" 2>/dev/null || true

# 4) log file을 host의 file과 volums mount 함
# Redis 환경변수 전달 (GitHub Actions Secrets에서 주입받음)
# SPRING_PROFILES_ACTIVE=prod 설정으로 production 프로파일 활성화
docker run -d -v /home/ubuntu/app/logs:/home/ubuntu/app/logs \
  --name "$CONTAINER_NAME" --restart=always -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e REDIS_HOST="${REDIS_HOST}" \
  -e REDIS_PORT="${REDIS_PORT}" \
  -e REDIS_PASSWORD="${REDIS_PASSWORD}" \
  "$IMAGE_URI"


echo "[deploy] Container $CONTAINER_NAME started."
```

### 수정 후
```bash
# 애플리케이션 환경변수 파일 경로
ENV_FILE="$APP_HOME/application.env"

# 환경변수 파일 존재 확인
if [ ! -f "$ENV_FILE" ]; then
    echo "[deploy] ERROR: application.env not found at $ENV_FILE"
    exit 1
fi

echo "[deploy] Using image: $IMAGE_URI"
echo "[deploy] Loading environment variables from application.env"

# 1) ECR 로그인 (EC2 인스턴스 롤에 ecr:GetAuthorizationToken 등 Pull 권한 필수)
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$(echo "$IMAGE_URI" | awk -F/ '{print $1}')"

# 2) 최신 이미지 Pull
docker pull "$IMAGE_URI"

# 3) 기존 컨테이너 중지/삭제(있다면)
docker rm -f "$CONTAINER_NAME" 2>/dev/null || true

# 4) 새 컨테이너 실행 (--env-file로 모든 환경변수 주입)
# log file을 host의 file과 volumes mount
# SPRING_PROFILES_ACTIVE=prod 설정으로 production 프로파일 활성화
docker run -d -v /home/ubuntu/app/logs:/home/ubuntu/app/logs \
  --name "$CONTAINER_NAME" --restart=always -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  --env-file "$ENV_FILE" \
  "$IMAGE_URI"

echo "[deploy] Container $CONTAINER_NAME started with environment variables from application.env"
```

### 주요 변경사항

| 항목 | 수정 전 | 수정 후 |
|------|---------|---------|
| 파일명 | `redis.env` | `application.env` |
| 환경변수 로드 | `source` 명령으로 개별 로드 | 파일 존재만 확인 |
| 환경변수 검증 | Redis 관련만 검증 | 파일 존재 여부만 확인 |
| Docker 환경변수 전달 | `-e` 플래그로 3개만 개별 전달 | `--env-file`로 전체 파일 주입 |
| 로그 메시지 | Redis 전용 메시지 | 전체 환경변수 로딩 메시지 |

---

## 5. 주의사항

### GitHub Secrets 확인

1. **Secret 이름**: `APPLICATION_ENV_FILE`
2. **형식**: `.env` 파일 형식 (`KEY=VALUE`)
3. **필수 환경변수** (총 11개 이상):
   ```
   REDIS_HOST=...
   REDIS_PORT=6379
   REDIS_PASSWORD=...
   DB_URL=...
   DB_USERNAME=...
   DB_PASSWORD=...
   AWS_ACCESS_KEY=...
   AWS_SECRET_KEY=...
   KAKAO_PAY_AUTHORIZATION=...
   NAVER_PAY_CLIENT_ID=...
   NAVER_PAY_CLIENT_SECRET=...
   NAVER_PAY_CHAIN_ID=...
   MAIL_USERNAME=...
   MAIL_PASSWORD=...
   ```

### 환경변수 파일 형식

- 각 줄은 `KEY=VALUE` 형식
- 빈 줄이나 주석(`#`)은 허용됨
- 값에 공백이 포함된 경우 따옴표 불필요 (Docker가 자동 처리)
- 줄바꿈은 LF (Unix 형식) 사용

### 배포 순서

1. `deploy.yml` 수정 후 커밋/푸시
2. GitHub Actions 빌드 확인 (bundle에 `application.env` 포함 확인)
3. CodeDeploy가 EC2에 배포
4. `deploy.sh` 실행 → `application.env` 읽어 컨테이너에 주입
5. Spring Boot 애플리케이션 정상 시작 확인

---

## 6. 검증 방법

### 1. 배포 후 컨테이너 환경변수 확인

```bash
# EC2 서버에 SSH 접속 후
docker exec carhartt-platform env | grep -E "DB_URL|REDIS_HOST|AWS_ACCESS_KEY|MAIL_USERNAME"
```

**예상 출력:**
```
DB_URL=jdbc:mysql://...
REDIS_HOST=carhartt-u-redis-001...
AWS_ACCESS_KEY=AKIA...
MAIL_USERNAME=gamegemos588@gmail.com
```

### 2. 애플리케이션 로그 확인

```bash
# 컨테이너 로그 확인
docker logs carhartt-platform

# Spring Boot 시작 로그에서 에러 확인
docker logs carhartt-platform 2>&1 | grep -i "error\|exception"

# 성공적인 시작 확인
docker logs carhartt-platform 2>&1 | grep "Started CPlatformApplication"
```

### 3. 환경변수 파일 확인

```bash
# EC2 서버에서 파일 존재 확인
ls -la /home/ubuntu/carhartt_platform/application.env

# 파일 내용 확인 (민감정보 포함되므로 주의!)
cat /home/ubuntu/carhartt_platform/application.env
```

### 4. 데이터베이스 연결 테스트

```bash
# 컨테이너 내부에서 MySQL 연결 테스트 (mysql-client가 설치된 경우)
docker exec carhartt-platform bash -c 'mysql -h${DB_URL##*://} -u${DB_USERNAME} -p${DB_PASSWORD} -e "SELECT 1"'
```

### 5. API Health Check

```bash
# Health endpoint 확인 (Actuator가 활성화된 경우)
curl http://localhost:8080/actuator/health

# 기본 API 테스트
curl http://localhost:8080/v1/categories
```

---

## 7. 트러블슈팅

### 문제 1: application.env 파일이 없음

**에러 메시지:**
```
[deploy] ERROR: application.env not found at /home/ubuntu/carhartt_platform/application.env
```

**원인:**
- GitHub Actions에서 파일 생성 실패
- CodeDeploy 번들에 파일 누락

**해결방법:**
1. `.github/workflows/deploy.yml` Line 124-128 확인
2. GitHub Actions 로그에서 "Stage bundle" 단계 확인
3. S3 버킷에서 번들 ZIP 파일 다운로드하여 `application.env` 포함 여부 확인

### 문제 2: 환경변수가 컨테이너에 전달되지 않음

**에러 메시지:**
```
Caused by: java.lang.IllegalArgumentException: Could not resolve placeholder 'DB_URL' in value "${DB_URL}"
```

**원인:**
- `--env-file` 플래그 누락
- 환경변수 파일 형식 오류

**해결방법:**
1. `deploy.sh`의 `docker run` 명령에 `--env-file "$ENV_FILE"` 포함 확인
2. `application.env` 파일 형식 확인 (`KEY=VALUE`)
3. 환경변수 값에 특수문자가 올바르게 이스케이프되었는지 확인

### 문제 3: 환경변수 값에 공백이나 특수문자 포함

**증상:**
- 환경변수 값이 잘림
- 특수문자가 올바르게 전달되지 않음

**해결방법:**
1. GitHub Secrets에서 값 재입력
2. 앞뒤 공백 제거
3. 필요한 경우 따옴표로 감싸기 (단, Docker `--env-file`은 자동 처리하므로 일반적으로 불필요)

### 문제 4: Redis 연결 실패

**에러 메시지:**
```
Unable to connect to Redis; nested exception is io.lettuce.core.RedisConnectionException
```

**해결방법:**
1. `REDIS_HOST` 환경변수 확인
2. EC2 보안 그룹에서 Redis 포트(6379) 허용 확인
3. ElastiCache Redis 엔드포인트 정확성 확인

### 문제 5: MySQL 연결 실패

**에러 메시지:**
```
Communications link failure
```

**해결방법:**
1. `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 환경변수 확인
2. RDS 보안 그룹에서 EC2 인바운드 허용 확인
3. RDS 엔드포인트 정확성 확인

---

## 8. 롤백 계획

### 즉시 롤백 (긴급 상황)

만약 수정 후 심각한 문제가 발생하면:

```bash
# EC2에서 수동으로 이전 방식으로 컨테이너 실행
docker rm -f carhartt-platform

docker run -d -v /home/ubuntu/app/logs:/home/ubuntu/app/logs \
  --name carhartt-platform --restart=always -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e REDIS_HOST="<REDIS_HOST>" \
  -e REDIS_PORT="6379" \
  -e REDIS_PASSWORD="" \
  -e DB_URL="<DB_URL>" \
  -e DB_USERNAME="<DB_USERNAME>" \
  -e DB_PASSWORD="<DB_PASSWORD>" \
  -e AWS_ACCESS_KEY="<AWS_ACCESS_KEY>" \
  -e AWS_SECRET_KEY="<AWS_SECRET_KEY>" \
  -e KAKAO_PAY_AUTHORIZATION="<KAKAO_PAY_AUTHORIZATION>" \
  -e NAVER_PAY_CLIENT_ID="<NAVER_PAY_CLIENT_ID>" \
  -e NAVER_PAY_CLIENT_SECRET="<NAVER_PAY_CLIENT_SECRET>" \
  -e NAVER_PAY_CHAIN_ID="<NAVER_PAY_CHAIN_ID>" \
  -e MAIL_USERNAME="<MAIL_USERNAME>" \
  -e MAIL_PASSWORD="<MAIL_PASSWORD>" \
  <이전_이미지_URI>
```

### Git 롤백

```bash
# deploy.yml과 deploy.sh를 이전 버전으로 되돌림
git revert <커밋_해시>

# 또는 직접 수정
git checkout HEAD~1 -- .github/workflows/deploy.yml scripts/deploy.sh
git commit -m "Revert: Rollback deploy configuration"
git push
```

---

## 9. 전체 수정 요약

### deploy.yml
- **Line 124-128**: `redis.env` → `application.env` 파일명 변경
- 주석 개선 및 줄바꿈 제거

### deploy.sh
- **Line 9-24**: 환경변수 개별 로드/검증 로직 제거
- **Line 44-50**: `--env-file` 플래그 추가, 개별 `-e` 플래그 제거
- 로그 메시지 개선

### 예상 효과
- ✅ 모든 환경변수가 컨테이너에 정상 전달
- ✅ Spring Boot 애플리케이션 정상 시작
- ✅ DB, Redis, S3, Payment, Email 기능 모두 정상 작동
- ✅ 유지보수 간소화 (환경변수 추가 시 `.env` 파일만 수정)

---

## 10. 참고 자료

### 관련 파일
- `src/main/resources/application.properties` - 환경변수 참조 위치
- `env_values/.env` - 환경변수 정의 파일 (GitHub Secrets에 저장)
- `.github/workflows/deploy.yml` - CI/CD 파이프라인
- `scripts/deploy.sh` - EC2 배포 스크립트
- `.gitignore` - `.env` 파일 제외 설정

### Docker 환경변수 주입 방식 비교

| 방식 | 장점 | 단점 |
|------|------|------|
| `-e KEY=VALUE` | 명시적, 개별 제어 가능 | 환경변수 많을 경우 복잡, 유지보수 어려움 |
| `--env-file` | 간결, 유지보수 용이 | 파일 관리 필요 |
| `-e` + `--env-file` 혼용 | 기본값 + 추가 설정 가능 | 우선순위 혼란 가능 |

**권장**: `--env-file`을 기본으로 사용하고, 필수 설정(`SPRING_PROFILES_ACTIVE`)만 `-e`로 명시

---

## 11. 체크리스트

배포 전 확인사항:

- [ ] GitHub Secrets에 `APPLICATION_ENV_FILE` 등록됨
- [ ] `.env` 파일에 모든 필수 환경변수 포함됨
- [ ] `deploy.yml` Line 124-128 수정됨
- [ ] `deploy.sh` Line 9-50 수정됨
- [ ] Git 커밋 및 푸시 완료
- [ ] GitHub Actions 빌드 성공
- [ ] CodeDeploy 배포 성공
- [ ] 컨테이너 정상 시작 확인
- [ ] 환경변수 주입 확인 (`docker exec ... env`)
- [ ] 애플리케이션 로그 확인 (에러 없음)
- [ ] API 테스트 통과

---

## 결론

이 가이드에 따라 `deploy.yml`과 `deploy.sh`를 수정하면, GitHub Secrets에 저장된 모든 환경변수를 Docker 컨테이너에 안전하게 주입할 수 있습니다.

`--env-file` 플래그를 사용함으로써:
- 코드 간소화
- 유지보수성 향상
- 환경변수 관리 용이
- 보안 강화 (개별 명령어에 민감정보 노출 최소화)

문제 발생 시 이 문서의 트러블슈팅 섹션을 참고하거나, 롤백 계획에 따라 이전 상태로 복구할 수 있습니다.
