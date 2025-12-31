# Redis Session 공유 부하 테스트 (30초, 100 VUs)

## 개선 사항
1. ✅ **Cookie 파싱 에러 수정**: `jsessionid[0].value` → `jsessionid[0]` (k6의 cookieJar는 이미 value 반환)
2. ✅ **세션 공유 검증 강화**: 서버별 응답 헤더 추적으로 세션이 실제로 공유되는지 확인
3. ✅ **에러 핸들링 개선**: 로그인 실패 시 더 상세한 디버깅 정보 출력
4. ✅ **통계 개선**: 서버별 요청 분포 확인

---

## k6 테스트 스크립트

```javascript
import http from 'k6/http';
import { sleep, check } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';
import { Counter } from 'k6/metrics';

export const options = {
  vus: 100,
  duration: '30s',

  thresholds: {
    'http_req_failed': ['rate<0.20'],  // 20% 미만 실패
    'http_req_duration': ['p(95)<10000'], // 95%가 10초 이내
  },
};

const BASE = 'https://carhartt-usedtransactions.com';

const ACCOUNTS = [
  { email: 'dnsrkd0414@naver.com', password: 'gjsxjsms123!' },
  { email: 'dnsrkd0410@naver.com', password: 'gjsxjsms123!!!' }
];

const VALID_ITEM_IDS = [22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 37, 38, 39];

const GROUP1_PERCENT = 0.34;  // 34% - 아이템 상세 조회
const GROUP2_PERCENT = 0.67;  // 33% - 검색
// 나머지 33% - 주소 조회

// Custom metrics for server tracking
const server1Requests = new Counter('requests_to_server1');
const server2Requests = new Counter('requests_to_server2');

export function setup() {
  console.log('\n' + '='.repeat(70));
  console.log('🔥 Redis Session Sharing Test (30s, 100 VUs)');
  console.log('='.repeat(70));

  const sessions = [];

  // 각 계정으로 로그인
  for (let i = 0; i < ACCOUNTS.length; i++) {
    console.log(`\n📝 Logging in account ${i + 1}/${ACCOUNTS.length}...`);
    console.log(`   Email: ${ACCOUNTS[i].email}`);

    // 각 로그인마다 새로운 Cookie Jar 생성
    const jar = http.cookieJar();
    jar.clear(BASE);

    const loginUrl = `${BASE}/v1/local/login`;
    const payload = JSON.stringify({
      email: ACCOUNTS[i].email,
      password: ACCOUNTS[i].password,
    });

    const res = http.post(loginUrl, payload, {
      headers: { 'Content-Type': 'application/json' },
      timeout: '10s',
      jar: jar,
    });

    console.log(`   📊 Login response status: ${res.status}`);

    if (res.status !== 200) {
      console.log(`   ❌ Login failed: status ${res.status}`);
      console.log(`   📄 Response body: ${res.body.substring(0, 300)}`);
      console.log(`   📋 Response headers: ${JSON.stringify(res.headers)}`);
      continue;
    }

    // Cookie Jar에서 쿠키 가져오기
    const cookies = jar.cookiesForURL(BASE);
    console.log(`   🍪 Cookies type: ${typeof cookies}`);
    console.log(`   🍪 Cookies keys: ${Object.keys(cookies)}`);

    if (!cookies || Object.keys(cookies).length === 0) {
      console.log(`   ❌ Login failed: no cookies in jar`);
      console.log(`   📄 Response Set-Cookie headers: ${res.headers['Set-Cookie'] || 'none'}`);
      continue;
    }

    // JSESSIONID 찾기
    const jsessionid = cookies.JSESSIONID;
    console.log(`   🔍 JSESSIONID type: ${typeof jsessionid}`);
    console.log(`   🔍 JSESSIONID value: ${JSON.stringify(jsessionid)}`);

    if (!jsessionid) {
      console.log(`   ❌ Login failed: no JSESSIONID found`);
      console.log(`   📋 Available cookies: ${Object.keys(cookies).join(', ')}`);
      continue;
    }

    // k6의 cookieJar.cookiesForURL()는 배열을 반환하므로 [0] 접근 필요
    // 그리고 각 항목은 이미 {name, value, ...} 객체
    let cookieString;
    if (Array.isArray(jsessionid) && jsessionid.length > 0) {
      const sessionValue = jsessionid[0].value || jsessionid[0];
      cookieString = `JSESSIONID=${sessionValue}`;
      console.log(`   ✅ Login OK - JSESSIONID: ${sessionValue.toString().substring(0, 10)}...`);
    } else if (typeof jsessionid === 'string') {
      cookieString = `JSESSIONID=${jsessionid}`;
      console.log(`   ✅ Login OK - JSESSIONID: ${jsessionid.substring(0, 10)}...`);
    } else {
      console.log(`   ❌ Unexpected JSESSIONID format: ${JSON.stringify(jsessionid)}`);
      continue;
    }

    sessions.push(cookieString);

    // 세션 저장 확인 (Redis에 저장되었는지 테스트)
    sleep(1);
    const testRes = http.get(`${BASE}/v1/items/22`, {
      headers: { Cookie: cookieString },
      timeout: '5s',
    });

    console.log(`   🧪 Session test status: ${testRes.status}`);

    if (testRes.status === 200) {
      const server = testRes.headers['x-upstream-server'] ||
                     testRes.headers['X-Upstream-Server'] ||
                     testRes.headers['X-UPSTREAM-SERVER'] ||
                     'unknown';
      console.log(`   ✅ Session verified on server: ${server}`);
    } else {
      console.log(`   ⚠️  Session test failed: ${testRes.status}`);
      console.log(`   📄 Response: ${testRes.body.substring(0, 200)}`);
    }
  }

  if (sessions.length === 0) {
    throw new Error('❌ No successful logins! Cannot proceed with test.');
  }

  console.log(`\n${'='.repeat(70)}`);
  console.log(`🎉 Setup complete!`);
  console.log(`   Total sessions: ${sessions.length}`);
  console.log(`   Starting 30s test with 100 VUs...`);
  console.log(`   🎯 Testing Redis session sharing across multiple EC2 instances`);
  console.log('='.repeat(70) + '\n');

  return { sessions };
}

export default function (data) {
  const vuId = __VU;
  const iter = __ITER;

  // VU별로 세션 할당 (라운드 로빈)
  const sessionIndex = (vuId - 1) % data.sessions.length;
  const myCookie = data.sessions[sessionIndex];

  if (!myCookie) {
    console.error(`❌ VU${vuId}: No session available`);
    sleep(1);
    return;
  }

  const headers = {
    Cookie: myCookie,
    'X-Request-Id': `test-${Date.now()}-${vuId}-${iter}`,
  };

  const combinedIndex = iter + (vuId - 1);
  const totalVUs = 100;
  const vuRatio = vuId / totalVUs;

  // 34% - 아이템 상세 조회
  if (vuRatio <= GROUP1_PERCENT) {
    const itemId = VALID_ITEM_IDS[combinedIndex % VALID_ITEM_IDS.length];
    const url = `${BASE}/v1/items/${itemId}`;

    const res = http.get(url, {
      headers,
      tags: { name: 'GET_item_detail' },
      timeout: '30s',
    });

    const server = res.headers['x-upstream-server'] ||
                   res.headers['X-Upstream-Server'] ||
                   res.headers['X-UPSTREAM-SERVER'] ||
                   'unknown';

    // Track server distribution
    if (server.includes('1') || server.includes('server1')) {
      server1Requests.add(1);
    } else if (server.includes('2') || server.includes('server2')) {
      server2Requests.add(1);
    }

    check(res, {
      'detail 200': (r) => r.status === 200,
      'has upstream': (r) => {
        return r.headers['x-upstream-server'] !== undefined ||
               r.headers['X-Upstream-Server'] !== undefined ||
               r.headers['X-UPSTREAM-SERVER'] !== undefined;
      },
      'session valid': (r) => r.status !== 401,
    });
  }
  // 33% - 검색
  else if (vuRatio <= GROUP2_PERCENT) {
    const res = http.get(`${BASE}/v1/items?keyword=&page=0&size=10&sort=price`, {
      headers,
      tags: { name: 'GET_items_search' },
      timeout: '30s',
    });

    const server = res.headers['x-upstream-server'] ||
                   res.headers['X-Upstream-Server'] ||
                   res.headers['X-UPSTREAM-SERVER'] ||
                   'unknown';

    // Track server distribution
    if (server.includes('1') || server.includes('server1')) {
      server1Requests.add(1);
    } else if (server.includes('2') || server.includes('server2')) {
      server2Requests.add(1);
    }

    check(res, {
      'search 200': (r) => r.status === 200,
      'has upstream': (r) => {
        return r.headers['x-upstream-server'] !== undefined ||
               r.headers['X-Upstream-Server'] !== undefined ||
               r.headers['X-UPSTREAM-SERVER'] !== undefined;
      },
      'session valid': (r) => r.status !== 401,
    });
  }
  // 33% - 주소 조회
  else {
    const res = http.get(`${BASE}/v1/orders/address`, {
      headers,
      tags: { name: 'GET_address' },
      timeout: '30s',
    });

    const server = res.headers['x-upstream-server'] ||
                   res.headers['X-Upstream-Server'] ||
                   res.headers['X-UPSTREAM-SERVER'] ||
                   'unknown';

    // Track server distribution
    if (server.includes('1') || server.includes('server1')) {
      server1Requests.add(1);
    } else if (server.includes('2') || server.includes('server2')) {
      server2Requests.add(1);
    }

    check(res, {
      'address 200': (r) => r.status === 200,
      'has upstream': (r) => {
        return r.headers['x-upstream-server'] !== undefined ||
               r.headers['X-Upstream-Server'] !== undefined ||
               r.headers['X-UPSTREAM-SERVER'] !== undefined;
      },
      'session valid': (r) => r.status !== 401,
    });
  }

  sleep(0.1);
}

export function teardown(data) {
  console.log('\n' + '='.repeat(70));
  console.log('🧹 Cleaning up sessions...');

  for (let i = 0; i < data.sessions.length; i++) {
    try {
      const res = http.post(`${BASE}/v1/local/logout`, null, {
        headers: { Cookie: data.sessions[i] },
        timeout: '5s',
      });
      console.log(`   ✅ Session ${i + 1} logged out (status: ${res.status})`);
    } catch (e) {
      console.log(`   ⚠️  Session ${i + 1} logout error: ${e.message}`);
    }
  }

  console.log('='.repeat(70) + '\n');
}

export function handleSummary(data) {
  const server1Count = data.metrics.requests_to_server1?.values?.count || 0;
  const server2Count = data.metrics.requests_to_server2?.values?.count || 0;
  const totalRequests = server1Count + server2Count;

  let customSummary = textSummary(data, { indent: '  ', enableColors: true });

  customSummary += '\n\n' + '='.repeat(70) + '\n';
  customSummary += '📊 Redis Session Sharing Results\n';
  customSummary += '='.repeat(70) + '\n';
  customSummary += `  Total Requests: ${totalRequests}\n`;
  customSummary += `  Server 1: ${server1Count} (${((server1Count/totalRequests)*100).toFixed(1)}%)\n`;
  customSummary += `  Server 2: ${server2Count} (${((server2Count/totalRequests)*100).toFixed(1)}%)\n`;
  customSummary += '\n✅ Expected: ~50%/50% distribution if load balancing works\n';
  customSummary += '✅ Expected: 0% 401 errors if Redis session sharing works\n';
  customSummary += '='.repeat(70) + '\n';

  return {
    stdout: customSummary,
  };
}
```

---

## 실행 방법

```bash
# 스크립트 저장
# 파일명: redis_session_test.js

# 실행
k6 run redis_session_test.js

# 출력을 파일로 저장
k6 run redis_session_test.js > test_results.txt
```

---

## 예상 결과 (성공 케이스)

```
=======================================================================
🔥 Redis Session Sharing Test (30s, 100 VUs)
=======================================================================

📝 Logging in account 1/2...
   Email: dnsrkd0414@naver.com
   📊 Login response status: 200
   🍪 Cookies type: object
   🍪 Cookies keys: JSESSIONID
   🔍 JSESSIONID type: object
   🔍 JSESSIONID value: [{"name":"JSESSIONID","value":"abc123...","domain":"..."}]
   ✅ Login OK - JSESSIONID: abc123...
   🧪 Session test status: 200
   ✅ Session verified on server: server1

📝 Logging in account 2/2...
   Email: dnsrkd0410@naver.com
   📊 Login response status: 200
   ✅ Login OK - JSESSIONID: def456...
   🧪 Session test status: 200
   ✅ Session verified on server: server2

=======================================================================
🎉 Setup complete!
   Total sessions: 2
   Starting 30s test with 100 VUs...
   🎯 Testing Redis session sharing across multiple EC2 instances
=======================================================================

... (테스트 실행 중) ...

=======================================================================
📊 Redis Session Sharing Results
=======================================================================
  Total Requests: 30000
  Server 1: 15120 (50.4%)
  Server 2: 14880 (49.6%)

✅ Expected: ~50%/50% distribution if load balancing works
✅ Expected: 0% 401 errors if Redis session sharing works
=======================================================================

checks.........................: 100.00% ✓ 90000      ✗ 0
  ✓ detail 200..................: 100.00% ✓ 10200      ✗ 0
  ✓ search 200..................: 100.00% ✓ 9900       ✗ 0
  ✓ address 200.................: 100.00% ✓ 9900       ✗ 0
  ✓ has upstream................: 100.00% ✓ 30000      ✗ 0
  ✓ session valid...............: 100.00% ✓ 30000      ✗ 0
http_req_duration..............: avg=120ms min=50ms med=115ms max=500ms p(95)=250ms
http_req_failed................: 0.00%   ✓ 0          ✗ 30000
```

---

## 검증 포인트

### ✅ Redis Session 공유 성공 조건
1. **401 에러 0개**: 모든 요청이 인증 통과 (session valid 100%)
2. **서버 분포 균등**: Server 1과 2가 각각 ~50% 요청 처리
3. **응답 성공률 100%**: detail/search/address 모두 200 OK

### ❌ 실패 시나리오
1. **401 에러 발생**: Redis 세션 공유 실패 → 각 서버가 다른 서버의 세션 인식 못함
2. **서버 분포 불균등**: 한쪽 서버만 요청 처리 → 로드 밸런싱 미작동
3. **응답 실패**: 서버 과부하 또는 애플리케이션 오류

---

## 트러블슈팅

### 문제 1: `TypeError: Cannot read property 'substring' of undefined`
**원인**: k6의 `cookieJar.cookiesForURL()` 반환 형식 오류
**해결**: Cookie 파싱 로직 개선 (Line 90-107)

### 문제 2: 401 Unauthorized 에러 발생
**원인**: Redis 세션 공유 미작동
**해결**:
```bash
# 1. Redis 연결 확인
redis-cli -h <REDIS_HOST> -p 6379 ping
# 예상: PONG

# 2. 세션 키 확인
redis-cli -h <REDIS_HOST> -p 6379
keys spring:session:*
# 예상: spring:session:sessions:<session-id> 3개 이상

# 3. EC2 환경변수 확인
docker exec carhartt-platform env | grep REDIS
# 예상: REDIS_HOST=<엔드포인트>
```

### 문제 3: 서버 분포 불균등 (한쪽 100%)
**원인**: Nginx 로드 밸런싱 미작동
**해결**:
```bash
# Nginx 설정 확인
sudo vim /etc/nginx/sites-available/default

# upstream 블록 확인
upstream backend {
    server <server1-ip>:8080;
    server <server2-ip>:8080;
}

# Nginx 재시작
sudo systemctl reload nginx
```

---

## 성능 목표

| Metric | Target | Description |
|--------|--------|-------------|
| http_req_failed | < 20% | 실패율 20% 미만 |
| http_req_duration (p95) | < 10s | 95%가 10초 이내 |
| checks (session valid) | 100% | 세션 인증 100% 통과 |
| Server distribution | ~50/50 | 균등한 부하 분산 |

---

## 참고 자료
- Redis Session Storage: `claude/redis.md`
- Load Balancing 설정: Nginx upstream 설정
- 환경변수 관리: GitHub Secrets → CodeDeploy → EC2 Docker
