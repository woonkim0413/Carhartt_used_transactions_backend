# Redis 기반 세션 저장소 전환 가이드

## 목차
1. [빠른 시작](#1-빠른-시작)
2. [환경별 설정](#2-환경별-설정)
3. [테스트 및 검증](#3-테스트-및-검증)
4. [트러블슈팅](#4-트러블슈팅)
5. [해결된 이슈 아카이브](#5-해결된-이슈-아카이브)
6. [참고 자료](#6-참고-자료)

---

## 1. 빠른 시작

### 1.1 Redis Session이란?

**현재 상태**: Servlet 세션 (Tomcat 메모리)
- ❌ 서버 재시작 시 세션 손실
- ❌ 여러 서버 간 세션 공유 불가

**Redis Session으로 전환 후**:
- ✅ 서버 재시작해도 세션 유지
- ✅ 여러 EC2 서버가 세션 공유 (로드 밸런싱 가능)
- ✅ 세션 상태 모니터링 가능
- ✅ 자동 TTL 관리 (30분)

### 1.2 환경별 요약

| 환경 | 세션 저장소 | 설정 위치 | 활성화 방법 |
|------|------------|----------|------------|
| **Local** | Servlet (Tomcat 메모리) | `application.properties` | 기본값 (변경 금지) |
| **Production** | Redis (ElastiCache) | `application-oauth2-prod.yml` | `SPRING_PROFILES_ACTIVE=prod` |

⚠️ **중요**: Local 환경은 Servlet 세션을 유지합니다. Redis Session은 Production 환경에서만 사용합니다.

---

## 2. 환경별 설정

### 2.1 Local 환경 (기본값 - 변경 금지)

**파일**: `src/main/resources/application.properties`

```properties
# Line 6-7: Local 환경에서는 Servlet 세션 사용
spring.config.import=optional:classpath:application-oauth2-local.yml
# spring.config.import=optional:classpath:application-oauth2-prod.yml  ← 주석 처리됨
```

**세션 저장소**: Servlet (Tomcat 메모리)
- Redis 설정 없음
- 서버 재시작 시 세션 손실 (로컬 개발에는 문제 없음)

### 2.2 Production 환경 (Redis Session)

#### Step 1: 의존성 추가 ✅ (완료)

**파일**: `build.gradle`

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.session:spring-session-data-redis'
}
```

#### Step 2: Redis 설정 추가 ✅ (완료)

**파일**: `src/main/resources/application-oauth2-prod.yml`

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}  # 환경변수로 주입
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 60000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 2000ms

  session:
    store-type: redis
    timeout: 1800s  # 30분
    redis:
      namespace: spring:session
```

#### Step 3: RedisSessionConfig 클래스 생성 ✅ (완료)

**파일**: `src/main/java/com/C_platform/config/RedisSessionConfig.java`

```java
package com.C_platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)  // 30분
public class RedisSessionConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }
}
```

#### Step 4: GitHub Secrets 설정 ✅ (완료)

**위치**: GitHub Repository → Settings → Secrets and variables → Actions

| Secret 이름 | 값 예시 | 필수 | 설명 |
|-------------|---------|------|------|
| `REDIS_HOST` | `carhartt-u-redis-001.cvf1em.0001.apn2.cache.amazonaws.com` | ✅ | ElastiCache 엔드포인트 |
| `REDIS_PORT` | `6379` | ❌ | 기본값 6379 사용 |
| `REDIS_PASSWORD` | (빈 값) | ❌ | AUTH 미설정 시 빈 값 |

#### Step 5: 배포 스크립트 설정 ✅ (완료)

**파일**: `.github/workflows/deploy.yml` (Line 124-129)

```yaml
# Redis 환경변수 파일 생성
cat > bundle/redis.env <<EOF
export REDIS_HOST="${{ secrets.REDIS_HOST }}"
export REDIS_PORT="6379"
export REDIS_PASSWORD=""
EOF
```

**파일**: `scripts/deploy.sh` (Line 9-24)

```bash
# Redis 환경변수 로드
if [ -f "$APP_HOME/redis.env" ]; then
    source "$APP_HOME/redis.env"
fi

# 환경변수 검증 (REDIS_HOST만 필수)
: "${REDIS_HOST:?REDIS_HOST environment variable is required}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
```

**Docker 실행 시 환경변수 전달** (Line 34-36)

```bash
docker run -d \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e REDIS_HOST="$REDIS_HOST" \
  -e REDIS_PORT="$REDIS_PORT" \
  -e REDIS_PASSWORD="$REDIS_PASSWORD" \
  # ...
```

⚠️ **핵심**: `SPRING_PROFILES_ACTIVE=prod` 환경변수가 없으면 Redis Session이 활성화되지 않습니다!

#### Step 6: application.properties 수정 ✅ (완료)

**파일**: `src/main/resources/application.properties`

```properties
# Line 6-7: Production 프로파일 활성화 시 설정 파일 import
spring.config.import=optional:classpath:application-oauth2-local.yml

# 🔴 중요: spring.config.import가 없으면 application-oauth2-prod.yml이 로드되지 않음!
# Production 환경에서는 SPRING_PROFILES_ACTIVE=prod 환경변수로 프로파일 전환
```

---

## 3. 테스트 및 검증

### 3.1 Redis 세션 확인 방법 (통합)

#### Production 환경 (EC2)

```bash
# 1. EC2 서버 SSH 접속
ssh -i your-key.pem ubuntu@your-ec2-ip

# 2. 환경변수 확인
docker exec carhartt-platform env | grep REDIS
# 예상 결과:
# SPRING_PROFILES_ACTIVE=prod
# REDIS_HOST=carhartt-u-redis-001.cvf1em.0001.apn2.cache.amazonaws.com
# REDIS_PORT=6379

# 3. 애플리케이션 로그 확인
docker logs carhartt-platform | grep -E "(profile|RedisIndexedSessionRepository)"
# 예상 결과:
# The following 1 profile is active: prod
# Spring Session initialized with RedisIndexedSessionRepository

# 4. Redis 세션 확인
redis-cli -h <REDIS_HOST> -p 6379
keys spring:session:*
# 예상 결과 (로그인 후):
# 1) "spring:session:sessions:abc123..."
# 2) "spring:session:sessions:expires:abc123..."
# 3) "spring:session:expirations:1733925600000"
```

#### Local 환경

```bash
# Local에서는 Redis를 사용하지 않습니다!
# Servlet 세션이 기본값으로 사용됨
# Redis 확인 불필요
```

### 3.2 Redis 세션 데이터 구조

```bash
# Redis CLI 접속
redis-cli -h <REDIS_HOST> -p 6379

# 세션 데이터 확인 (Hash 타입)
hgetall spring:session:sessions:<session-id>
# 결과 예시:
# "creationTime" "1733918400000"
# "lastAccessedTime" "1733918415000"
# "maxInactiveInterval" "1800"
# "sessionAttr:SPRING_SECURITY_CONTEXT" "<SecurityContext 객체>"

# 세션 TTL 확인 (초 단위)
ttl spring:session:sessions:<session-id>
# 결과: 1794 (약 30분)
```

### 3.3 테스트 시나리오

#### 시나리오 1: 세션 영속성 테스트

```bash
# 1. 로그인 API 호출
curl -X POST https://carhartt-usedtransactions.com/v1/local/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' \
  -c cookies.txt

# 2. Redis에서 세션 확인
redis-cli -h <REDIS_HOST> -p 6379
keys spring:session:*
# 결과: 세션 키 3개 반환

# 3. 서버 재시작
docker restart carhartt-platform

# 4. 쿠키로 인증 확인 (재로그인 없이 성공해야 함)
curl -X GET https://carhartt-usedtransactions.com/v1/local/check \
  -b cookies.txt
# 예상 결과: 200 OK (401 아님)
```

#### 시나리오 2: 로드 밸런싱 세션 공유 테스트

```bash
# 1. 로그인 (Server 1로 요청)
curl -X POST https://carhartt-usedtransactions.com/v1/local/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' \
  -c cookies.txt

# 2. 인증 필요 API를 5번 연속 호출
for i in {1..5}; do
  curl -X GET https://carhartt-usedtransactions.com/v1/orders/address \
    -b cookies.txt
done

# 예상 결과: 5번 모두 200 OK (401 UNAUTHORIZED 없어야 함!)
# → Nginx 로드 밸런싱으로 Server 1, 2 번갈아가며 처리
# → Redis Session 공유로 모든 서버에서 세션 인식
```

---

## 4. 트러블슈팅

### 4.1 일반적인 문제

#### 문제 1: 401 Unauthorized 에러 발생 (로드 밸런싱 환경)

**증상**:
- Swagger UI에서 API를 2번 요청하면 1번 성공, 1번 실패 (50% 성공률)
- 2대의 EC2 서버 + Nginx 로드 밸런싱 환경

**원인**: Redis Session이 활성화되지 않아 각 서버가 자신의 Tomcat 메모리에만 세션 저장

**해결**:

```bash
# 1. 환경변수 확인
docker exec carhartt-platform env | grep SPRING_PROFILES_ACTIVE
# 결과: "SPRING_PROFILES_ACTIVE=prod" 있어야 함

# 2. 로그 확인
docker logs carhartt-platform | grep "RedisIndexedSessionRepository"
# 결과: "Spring Session initialized with RedisIndexedSessionRepository" 있어야 함

# 3. Redis 세션 확인
redis-cli -h <REDIS_HOST> -p 6379
keys spring:session:*
# 결과: 세션 키 3개 이상 반환되어야 함 (empty array면 실패)
```

**해결 방법**:
- `scripts/deploy.sh`에서 `SPRING_PROFILES_ACTIVE=prod` 환경변수 전달 확인
- Line 34-36: Docker 실행 시 `-e SPRING_PROFILES_ACTIVE=prod` 포함 확인

#### 문제 2: Redis 연결 실패

**증상**: 애플리케이션 시작 시 `RedisConnectionException` 발생

**해결**:

```bash
# 1. Redis 서버 연결 확인
redis-cli -h <REDIS_HOST> -p 6379 ping
# 예상: PONG

# 2. EC2 보안 그룹 확인
# EC2 → ElastiCache 통신 허용 (포트 6379)

# 3. 환경변수 확인
docker exec carhartt-platform env | grep REDIS
# REDIS_HOST, REDIS_PORT 값 확인
```

#### 문제 3: application-oauth2-prod.yml이 로드되지 않음

**증상**: `prod` 프로파일을 활성화했는데도 Redis 설정이 적용되지 않음

**원인**: `application.properties`에 `spring.config.import` 설정 누락

**해결**:

```properties
# application.properties (Line 6-7)
spring.config.import=optional:classpath:application-oauth2-local.yml

# 🔴 주의: Local 환경에서는 위 설정 유지
# Production 환경에서는 SPRING_PROFILES_ACTIVE=prod로 프로파일만 전환
# (spring.config.import는 변경하지 않음)
```

**확인 방법**:

```bash
# 로그에서 설정 파일 로드 확인
docker logs carhartt-platform | grep "Loaded config file"
# 예상 결과: "Loaded config file 'classpath:application-oauth2-prod.yml'"
```

### 4.2 디버깅 체크리스트

#### Production 환경 체크리스트

- [ ] **GitHub Secrets 설정**
  - [ ] `REDIS_HOST` Secret 존재 (ElastiCache 엔드포인트)

- [ ] **배포 파일 확인**
  - [ ] `.github/workflows/deploy.yml` Line 124-129: redis.env 파일 생성
  - [ ] `scripts/deploy.sh` Line 9-24: 환경변수 로드 및 검증
  - [ ] `scripts/deploy.sh` Line 34-36: Docker 실행 시 환경변수 전달

- [ ] **EC2 환경변수 확인**
  ```bash
  docker exec carhartt-platform env | grep -E "(SPRING_PROFILES|REDIS)"
  ```
  - [ ] `SPRING_PROFILES_ACTIVE=prod` 존재
  - [ ] `REDIS_HOST` 값 확인
  - [ ] `REDIS_PORT=6379` 값 확인

- [ ] **애플리케이션 로그 확인**
  ```bash
  docker logs carhartt-platform | grep -E "(profile|oauth2-prod|RedisIndexedSessionRepository)"
  ```
  - [ ] "The following 1 profile is active: prod"
  - [ ] "Loaded config file 'classpath:application-oauth2-prod.yml'"
  - [ ] "Spring Session initialized with RedisIndexedSessionRepository"

- [ ] **Redis 세션 확인**
  ```bash
  redis-cli -h <REDIS_HOST> -p 6379
  keys spring:session:*
  ```
  - [ ] 세션 키 3개 이상 반환 (로그인 후)

- [ ] **세션 공유 테스트**
  ```bash
  # API를 5번 연속 호출
  for i in {1..5}; do curl https://carhartt-usedtransactions.com/v1/orders/address -b cookies.txt; done
  ```
  - [ ] 5번 모두 200 OK (401 없음)

#### Local 환경 체크리스트

- [ ] **application.properties 확인**
  - [ ] Line 6: `spring.config.import=optional:classpath:application-oauth2-local.yml`
  - [ ] Line 7: `# spring.config.import=optional:classpath:application-oauth2-prod.yml` 주석 처리

- [ ] **Redis 설정 없음 확인**
  - [ ] `application.properties`에 Redis 설정 주석 처리됨
  - [ ] Servlet 세션이 기본값으로 사용됨

### 4.3 자주 묻는 질문 (FAQ)

**Q1. Local 환경에서 로그인 후 401 에러가 발생합니다.**

A: Local 환경에서는 Servlet 세션을 사용합니다. 서버를 재시작하면 세션이 손실되므로 재로그인이 필요합니다. 이는 정상 동작입니다.

**Q2. Production 환경에서 Redis Session을 비활성화하고 싶습니다.**

A: `application-oauth2-prod.yml`에서 `spring.session.store-type: redis`를 주석 처리하거나 삭제하세요. 또는 `RedisSessionConfig` 클래스에 `@Profile("!prod")` 어노테이션을 추가하세요.

**Q3. Redis 세션이 저장되는데 TTL이 30분보다 짧습니다.**

A: `application-oauth2-prod.yml`의 `spring.session.timeout` 값과 `RedisSessionConfig`의 `maxInactiveIntervalInSeconds` 값이 일치하는지 확인하세요.

---

## 5. 해결된 이슈 아카이브

### 5.1 CodeDeploy AfterInstall 실패 (2025-12-11) ✅ 해결

**문제**: GitHub Secrets에 `REDIS_HOST`가 설정되지 않아 `deploy.sh` 환경변수 검증 실패

**해결**: GitHub Secrets에 `REDIS_HOST` 추가
- Secret 이름: `REDIS_HOST`
- 값: ElastiCache 엔드포인트 (예: `carhartt-u-redis-001.cvf1em.0001.apn2.cache.amazonaws.com`)

**관련 파일**:
- `.github/workflows/deploy.yml` Line 124-129
- `scripts/deploy.sh` Line 17-24

### 5.2 로드 밸런싱 환경에서 세션 미공유 (2025-12-11) ✅ 해결

**문제**: 2대의 EC2 서버 환경에서 API 호출 시 50% 성공률 (401 에러 발생)

**원인**: `SPRING_PROFILES_ACTIVE=prod` 환경변수가 Docker 컨테이너에 전달되지 않음

**해결**: `scripts/deploy.sh` Line 34-36에서 Docker 실행 시 환경변수 전달
```bash
docker run -d \
  -e SPRING_PROFILES_ACTIVE=prod \
  # ...
```

**검증**: Redis CLI에서 `keys spring:session:*` 실행 시 세션 키 3개 이상 반환

---

## 6. 참고 자료

### 6.1 핵심 파일 위치

| 파일 | 경로 | 설명 |
|------|------|------|
| Redis 설정 | `src/main/resources/application-oauth2-prod.yml` | Redis 연결 정보, 세션 설정 |
| Redis Session Config | `src/main/java/com/C_platform/config/RedisSessionConfig.java` | `@EnableRedisHttpSession` 설정 |
| 배포 스크립트 | `scripts/deploy.sh` | Redis 환경변수 로드 및 Docker 실행 |
| GitHub Actions | `.github/workflows/deploy.yml` | redis.env 파일 생성 |
| 의존성 | `build.gradle` | Redis, Spring Session 의존성 |

### 6.2 관련 문서

- [Spring Session Data Redis 공식 문서](https://docs.spring.io/spring-session/reference/guides/boot-redis.html)
- [AWS ElastiCache for Redis](https://docs.aws.amazon.com/elasticache/latest/red-ug/WhatIs.html)
- 부하 테스트 스크립트: `claude/response.md`
- 로컬 인증 구현: `claude/json_encoding_fix.md`

### 6.3 요약

**변경된 파일**:
1. `build.gradle` - Redis 의존성 추가
2. `application-oauth2-prod.yml` - Redis 연결 정보, 세션 설정
3. `config/RedisSessionConfig.java` - `@EnableRedisHttpSession` 설정
4. `scripts/deploy.sh` - Redis 환경변수 로드 및 Docker 실행
5. `.github/workflows/deploy.yml` - redis.env 파일 생성

**변경 불필요한 파일**:
- `SecurityConfig.java` - 기존 `HttpSessionSecurityContextRepository` 그대로 사용
- `JsonUsernamePasswordAuthenticationFilter.java` - Spring Session이 자동으로 `HttpSession` 구현체를 교체
- 기타 세션 사용 코드 - 모두 `request.getSession()` 호출하므로 코드 변경 불필요

**핵심 개념**:
- Spring Session이 `HttpSession` 구현체를 Redis 기반으로 자동 교체
- 기존 코드는 수정할 필요 없이 `request.getSession()` 호출만으로 Redis 세션 사용
- Local 환경은 Servlet 세션, Production 환경은 Redis 세션 (프로파일로 분리)
