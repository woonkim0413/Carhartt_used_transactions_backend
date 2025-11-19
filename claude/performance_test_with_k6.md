
# *Tags :* 
# *linked file :* 
# *reference address :*

<font color="#de7802">핵심 자료들</font>
https://velog.io/@sontulip/how-to-set-up-infra (인프라 구성 방법, 번외 느낌)
https://velog.io/@sontulip/web-performance-budget <- (성능 테스트 블로그1 (메인))
https://velog.io/@sontulip/performance-test  
https://velog.io/@sontulip/how-to-shoot-trouble
https://velog.io/@sontulip/performance-tuning

https://sasca37.tistory.com/239#google_vignette <- (성능 테스트 블로그2)

https://velog.io/@eastperson/Spring-Boot-%ED%99%98%EA%B2%BD%EC%97%90%EC%84%9C-K6-Grafana%EB%A5%BC-%ED%99%9C%EC%9A%A9%ED%95%9C-%EB%B6%80%ED%95%98%ED%85%8C%EC%8A%A4%ED%8A%B8-%ED%95%B4%EB%B3%B4%EA%B8%B0 <- (k6 성능 테스트 하는 법1)
https://medium.com/weolbu/%EC%9B%94%EA%B8%89%EC%9F%81%EC%9D%B4%EB%B6%80%EC%9E%90%EB%93%A4%EC%9D%98-%EB%B6%80%ED%95%98%ED%85%8C%EC%8A%A4%ED%8A%B8%EB%A5%BC-%EC%9C%84%ED%95%9C-k6-%EB%8F%84%EC%9E%85%EA%B8%B0-d7c82e7fe65f <- (k6 기반 성능 테스트 하는 법2)
https://yeon-dev.tistory.com/203 <- (k6 기반 성능 테스트 하는 법3)


<font color="#de7802">보충 자료들</font>
https://techblog.woowahan.com/4886/ (우아한형제들 장애 대응)

https://techblog.woowahan.com/2700/ (우아한형제들 root case 찾고 재발 방지하는 법)

---
# < 내용 >

### <font color="#de7802">성능 테스트 목표 및 예산 설정</font>
트래픽은 RPS(Request Per Seconds)로 자주 표현된다
<font color="#92d050">PRS의 정의</font>는 1초 동안 발생한 요청의 수이다

<font color="#92d050">성능 테스트</font>는 application에 트래픽이 가장 많이 발생한 상황을 기준으로
성능, 발생한 에러 등을 찾아내는 작업이다

웹 성능을 높이기 위해선 자원이 들어간다 그리고 자원은 곧 돈이다 (EC2 메모리 크기, RDS 크기)
웹 성능을 기준 없이 무작정 높이는 것은 좋지 않다
<font color="#92d050">ROI(Return on Investment)</font>가 낮기 때문이다

웹 성능의 마지노선은 "<font color="#92d050">LCP(Largest Content Paints)를 3초 이하로 유지</font>"하는 것이다
이상적인 웹 성능 목표 기준은 경쟁 업체 대비 20% 이상 LCP가 빠른 것이다

웹 페이지의 대략적인 성능 (Time to Interactive, First Contentful Paint, Largest Contentful Paint)
은 https://pagespeed.web.dev/ (PageSpeed Insight url)에 들어가서 성능을 측정해보고 싶은
페이지의 url을 넣으면 된다

\* <font color="#92d050">API 성능 지표 분석하기</font>
API를 평가하기 위한 단위는 LCP외에도 TTI, FCP 등이 있다
TTI: Time To Interactive
FCP: First Contents Paint
해당 지표들은 DevTools를 사용하여 자세히 확인할 수 있다
[[browser developer tool (F12) 사용법]] <- (Performance Insights 및 Network 보는 법)

### <font color="#de7802">Application 부하 수준 도출</font>
<font color="#de7802">::: 테스트 대상 API</font>
DB데이터를 조회하는 API들 위주로 테스트를 진행해보려 한다
- 아이템 조회 API(전체, 키워드 필터링) 
- 주소 목록 API
- 찜 목록 API

<font color="#de7802">::: application 부하 수준 결정 (당근 마켓 VUser 참고)</font> (<font color="#d094db">매우 중요</font>)
<font color="#00b0f0">당근 마켓 분석</font>
당근 마켓의 경우 2024년의 DAU(Daily Active User)가 약 530만 정도였다
이후 분기마다 DAU 수치가 우상향 하였다 (약 5~9%)
그렇기에 당근 마켓의 <font color="#92d050">2025년 DAU값을 600만</font>으로 가정해보자

당근마켓에 한 번 들어가면 기본적으로 게시물 새로고침, 게시물 상세 보기 등
꽤 많은 HTTP 요청을 보내게 되므로
<font color="#92d050">1명당 하루 HTTP 요청 수를 러프하게 50개</font>로 잡아보자

<font color="#00b0f0">당근 마켓 피크 RPS 구하기</font>
하루 동안 당근 마켓에서 발생하는 총 요청 수: 50 x 6,000,000 = 300M(3억)
당근마켓 평균 RPS(Request Per Second): 300M / 86,400(하루 초) = 약 3,500
<font color="#92d050">당근 마켓 피크 RPS (평균 RPS x 5) = 17,500</font>

<font color="#00b0f0">당근 마켓을 테스트하기 위한 VUser 구하기</font>
VUser을 구하는 공식은 아래와 같다
(<font color="#92d050">thinking time = 0, 요청 응답시간 = 1s로 가정</font>하고 VUser를 계산)

목표 RPS = VUser X 1VS당 RPS (1 / 응답시간)
VUser = 목표 RPS / 1VS당 RPS = 17,500 / (1 / 1s) = 17,500

당근 마켓의 피크 RPS를 구현하기 위해선 17,500의 VUser가 필요하다
17,500의 VUser을 가정하여 부하 테스트를 진행하기 위해선 k6을 실행하는
컴퓨터를 여러 대 사용하여 전체 VUser을 각 컴퓨터에 분산 할당하는 것이 적절하다
(<font color="#92d050">5대로 분산했을 때 각 컴퓨터 사양 : 4~8 vCPU, 8~16GB RAM, VU ≈ 3,500개 감당</font>)

하나의 컴퓨터로 17,500의 VUser을 만들어내기 위해선 RAM과 vCPU가 무척 커야하고
고속 네트워크가 필수적으로 요구된다

\* <font color="#92d050">thinking time = 4, 요청 응답시간 = 0.2s로 가정했다면</font>
thinking time은 요청과 요청 사이에 사용자의 생각 시간을 의미한다
즉, 사용자는 하나의 요청을 보내고 thinking time만큼 다음 요청을 보내지 않고 대기한다
그렇기에 "응답시간 =  요청 응답 시간 + thinking time"이 된다
VUser = 17,500 / (1 / (4 + 0.2)) = 17,500 / 0.24(0.238....) = 73,000 (72916.666...) 

<font color="#00b0f0">내 Application VUser 구하기</font>
보통 API의 LCP(Largest Contents Paint)는 3s로 설정한다
(API의 LCP가 3s이상이면 50%이상의 유저가 이탈한다)
그러나 API의 DB 조회 구조가 복잡하지 않으므로 <font color="#92d050">1s를 목표</font>로 하려고 한다 (일단 목표)

RPS는 100으로 잡았을 때,
VUser = 목표 RPS x 1VS당 RPS (1초에 하나의 요청을 보내니 1VS의 PRS는 1가 된다)
<font color="#92d050">VUser = 100 x (1 / 1) = 100명</font>

즉, <font color="#92d050">내 Application이 VUser가 100명 접속하고 있는 상황에서 DB에서 데이터를 가져오는 API를 요청 응답 시간 1s이하로 5분간 버텨낼 수 있는지 k6를 이용하여 테스트 </font>할 것이다 (<font color="#d094db">결론</font>)

## <font color="#de7802">부하 테스트 환경 셋팅 후 실행 (처음은 K6만 사용)</font>
<font color="#de7802">::: k6 사용 조건 (프론트가 완성되지 않은 현재 상황에서도 테스트 가능)</font>
프론트단이 제대로 완성되어 있지 않아도 API가 모두 완성되어 있고 Application을
EC2(서버)에 올릴 수 있는 상황이라면 K6로 부하 테스트를 할 수 있다

Authentication이 필요한 API에 대해 부하테스트 하기 위해서 사전에 회원가입이 완료된 
계정 하나를 사용하여 VU들을 로그인 시킨 뒤 부하 테스트를 진행하려 한다
(JSESSIONID를 공유하거나 각 VU마다 JSESSIONID를 할당 받는 구조)
(<font color="#d094db">하나의 계정으로 여러 기기에 로그인한 상황 가정</font>)

사전에 여러 개의 계정을 회원가입을 통해 만든 다음 서로 다른 사용자가
동시에 application을 사용하는 상황을 가정하여 테스트 할 수도 있다
하지만 해당 방식보다 <font color="#92d050">하나의 계정을 여러 기기에 로그인 한 상황으로</font>
<font color="#92d050">테스트 하는 것이 더 간편</font>하기에 이 방식은 사용하지 않으려 한다

<font color="#de7802">::: wsl2에 k6 설치</font>
\* <font color="#00b0f0">wsl2가 외부와 통신이 안 될 땐 아래 명령어 실행 (nameserver 추가)</font>
``` zsh
sudo bash -c 'echo "nameserver 8.8.8.8" > /etc/resolv.conf && echo "nameserver 1.1.1.1" >> /etc/resolv.conf'
```

<font color="#00b0f0">wsl2 apt을 최신으로 업데이트</font>
``` zsh
sudo apt update
sudo apt install -y ca-certificates curl gnupg apt-transport-https
```

<font color="#00b0f0">wsl2에 k6 설치</font>
``` zsh
# 1. GPG 키 다운로드 (deb822 형식)
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://dl.k6.io/key.gpg | sudo tee /etc/apt/keyrings/k6.gpg > /dev/null
sudo chmod 644 /etc/apt/keyrings/k6.gpg

# 2. APT 저장소 추가 (deb822 형식)
cat <<EOF | sudo tee /etc/apt/sources.list.d/k6.sources
Enabled: yes
Types: deb
URIs: https://dl.k6.io/deb
Suites: stable
Components: main
Signed-By: /etc/apt/keyrings/k6.gpg
EOF

# 3. 설치
sudo apt update
sudo apt install -y k6
```


<font color="#de7802">::: k6 첫 번째 스크립트 테스트 (몇 개 호출에서 과부화 발생)</font>
<font color="#00b0f0">스크립트 정보</font>
- VUser 100명 사용
- 모든 api의 기대 응답 시간 2s
- 사용 api (VUser가 각각 34, 33, 33씩 할당되어서 호출)
  1) `v1/local/login` (Authentication api 호출을 위해 시작 전 한 번 호출 후 쿠키 공유)
  2) `v1/items/{item_id}` (물건의 상세 정보 호출)
  3) `/v1/items?keyword=${}&page=${}&size=${}&sort=${}` (아이템 목록 검색 결과 호출)
  4) `/v1/orders/address` (주소 목록 호출)

(k6 스크립트 실행)
``` java
import http from 'k6/http';
import { sleep, check } from 'k6';

/*
  동작 요약
  - setup 단계에서 로그인하여 인증 토큰/쿠키 획득
  - vus: 100 (1..34 => item detail, 35..67 => items search, 68..100 => address)
  - 각 VU는 루프마다:
      - item VU: GET /v1/items/{id} (36번 제외, 22-39 중 17개) then sleep(1)
      - search VU: GET /v1/items?keyword=&page=0&size=10&sort=price then sleep(1)
      - address VU: GET /v1/orders/address then sleep(1)
*/

export const options = {
  vus: 100,
  duration: '1m',
  thresholds: {
    'http_req_failed': ['rate<0.05'],
    'http_req_duration': ['p(95)<2000'],
  },
};

const BASE = 'https://carhartt-usedtransactions.com';
const LOGIN_EMAIL = 'dnsrkd0414@naver.com';
const LOGIN_PASSWORD = 'gjsxjsms123!';

// ✅ ID 36 (Pants) 제외한 유효한 ID 목록
const VALID_ITEM_IDS = [
  22, 23, 24, 25, 26, 27, 28, 29, 30,
  31, 32, 33, 34, 35, /* 36 제외 */ 37, 38, 39
];

// VU split: 34 + 33 + 33 = 100
const GROUP1_END = 34;   // 1..34: item detail
const GROUP2_END = 67;   // 35..67: items search
// 68..100: address

// 간단한 Request ID 생성 함수
function generateRequestId() {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

// Setup 함수: 테스트 시작 전 1번만 실행되어 로그인 수행
export function setup() {
  console.log('🔐 Attempting login...');
  
  const loginUrl = `${BASE}/v1/local/login`;
  const loginPayload = JSON.stringify({
    email: LOGIN_EMAIL,
    password: LOGIN_PASSWORD,
  });
  
  const loginHeaders = {
    'Content-Type': 'application/json',
  };
  
  const loginRes = http.post(loginUrl, loginPayload, { headers: loginHeaders });
  
  console.log(`Login Status: ${loginRes.status}`);
  console.log(`Login Response: ${loginRes.body}`);
  
  if (loginRes.status !== 200) {
    console.error('❌ Login failed!');
    throw new Error('Login failed, aborting test');
  }
  
  // Set-Cookie 헤더에서 쿠키 추출
  let authCookie = '';
  const setCookieHeader = loginRes.headers['Set-Cookie'];
  
  if (setCookieHeader) {
    const cookies = Array.isArray(setCookieHeader) ? setCookieHeader : [setCookieHeader];
    const cookiePairs = cookies.map(cookie => cookie.split(';')[0].trim());
    authCookie = cookiePairs.join('; ');
    
    console.log(`🍪 Extracted Cookie: ${authCookie}`);
  }
  
  if (!authCookie) {
    console.error('⚠️ No cookies extracted!');
    throw new Error('Cookie extraction failed');
  }
  
  console.log(`✅ Login successful!`);
  console.log(`ℹ️  Testing ${VALID_ITEM_IDS.length} items (excluding ID 36 - Pants)`);
  
  return { authCookie: authCookie };
}

export default function (data) {
  const vuId = __VU;
  const iter = __ITER;
  const combinedIndex = iter + (vuId - 1);
  
  // X-Request-Id 헤더 + 인증 쿠키 추가
  const headers = {
    'X-Request-Id': generateRequestId(),
  };
  
  // 로그인에서 받은 쿠키 추가
  if (data.authCookie) {
    headers['Cookie'] = data.authCookie;
  }
  
  if (vuId <= GROUP1_END) {
    // ===== GROUP 1: Item Detail (VU 1..34) =====
    // ✅ 36번 제외한 17개 ID 중에서 순환
    const itemId = VALID_ITEM_IDS[combinedIndex % VALID_ITEM_IDS.length];
    const itemUrl = `${BASE}/v1/items/${itemId}`;
    
    const res = http.get(itemUrl, { 
      headers: headers,
      tags: { name: 'GET_item_detail' } 
    });
    
    check(res, {
      'detail status 200': (r) => r.status === 200,
    });
    
  } else if (vuId <= GROUP2_END) {
    // ===== GROUP 2: Items Search (VU 35..67) =====
    const page = 0;
    const keyword = '';
    const size = 10;
    const sort = 'price';
    
    const searchUrl = `${BASE}/v1/items?keyword=${keyword}&page=${page}&size=${size}&sort=${sort}`;
    
    const res = http.get(searchUrl, { 
      headers: headers,
      tags: { name: 'GET_items_search' } 
    });
    
    check(res, {
      'search status 200': (r) => r.status === 200,
    });
    
  } else {
    // ===== GROUP 3: Address (VU 68..100) =====
    const addressUrl = `${BASE}/v1/orders/address`;
    
    const res = http.get(addressUrl, { 
      headers: headers,
      tags: { name: 'GET_address' } 
    });
    
    check(res, {
      'address status 200': (r) => r.status === 200,
    });
  }
  
  // 각 VU는 요청 하나 수행 후 1초 대기
  // sleep(1);
}
```

<font color="#00b0f0">스크립트 실행 결과</font>
몇 개의 요청이 매우 느리게 응답했다 (max=29.93s)
그래서 100명의 VUser중 46개가 대기했고 54개만 정상 동작했다 (vus 54 min=54 max=100)
(스크립트 실행 후 터미널 출력 값)
``` zsh
➜  ~ k6 run script.js      

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: script.js
        output: -

     scenarios: (100.00%) 1 scenario, 100 max VUs, 1m30s max duration (incl. graceful stop):
              * default: 100 looping VUs for 1m0s (gracefulStop: 30s)

INFO[0000] 🔐 Attempting login...                         source=console
INFO[0000] Login Status: 200                             source=console
INFO[0000] Login Response: {"success":true,"data":{"memberId":7,"email":"dnsrkd0414@naver.com","name":"김운강","nickname":"은하늑대4634","loginType":"LOCAL"},"meta":{"timestamp":"2025-11-18T04:42:55Z"}}  source=console
INFO[0000] 🍪 Extracted Cookie: JSESSIONID=2D31430A4C89B95D4BC7BD918C6F60DE  source=console
INFO[0000] ✅ Login successful!                           source=console


  █ THRESHOLDS 

    http_req_duration
    ✗ 'p(95)<2000' p(95)=3.03s

    http_req_failed
    ✓ 'rate<0.05' rate=2.03%


  █ TOTAL RESULTS 

    checks_total.......: 3493   56.812032/s
    checks_succeeded...: 97.96% 3422 out of 3493
    checks_failed......: 2.03%  71 out of 3493

    ✗ detail status 200
      ↳  94% — ✓ 1144 / ✗ 71
    ✓ search status 200
    ✓ address status 200

    HTTP
    http_req_duration..............: avg=724.24ms min=16.63ms med=51.26ms max=29.93s p(90)=244.15ms p(95)=3.03s
      { expected_response:true }...: avg=726.37ms min=16.63ms med=50.7ms  max=29.93s p(90)=233.2ms  p(95)=2.96s
    http_req_failed................: 2.03%  71 out of 3494
    http_reqs......................: 3494   56.828297/s

    EXECUTION
    iteration_duration.............: avg=1.73s    min=1.01s   med=1.05s   max=30.93s p(90)=1.24s    p(95)=4.03s
    iterations.....................: 3493   56.812032/s
    vus............................: 54     min=54         max=100
    vus_max........................: 100    min=100        max=100

    NETWORK
    data_received..................: 5.0 MB 81 kB/s
    data_sent......................: 939 kB 15 kB/s




running (1m01.5s), 000/100 VUs, 3493 complete and 0 interrupted iterations
default ✓ [======================================] 100 VUs  1m0s
ERRO[0061] thresholds on metrics 'http_req_duration' have been crossed
```

서버 로그 분석 결과 DB엔 pants라는 row가 있으나 코드에는 `@DiscriminatorValue("Pants")`로 정의된 엔티티가 없어서 에러가 뜨고 있었다
해당 row의 item_id가 36번인 것을 MySQL에서 확인 한 뒤 이를 부하 테스트를 진행할 때
조회하지 않도록 k6 스크립트를 다시 작성했다
(서버 로그)
``` zsh
ubuntu@ip-172-31-3-69:~$ sudo docker logs --tail=2000 carhartt-platform | grep -i error
        at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:93)
04:43:53 INFO  HHH000327: Error performing load command
        at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:93)
04:43:53 ERROR Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: org.springframework.orm.jpa.JpaSystemException: Unrecognized discriminator value: Pants] with root cause
        at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:93)
ubuntu@ip-172-31-3-69:~$


```

<font color="#00b0f0">부하 테스트 성공 (VUser 100명)</font>
VUser 100명이 3개의 api에 대해서 요청을 보낼 때
요청의 95%가 2s안에 응답 성공했다 (1.28s)
(스크립트 실행 후 터미널 출력 값)
``` zsh
➜  ~ k6 run script.js

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: script.js
        output: -

     scenarios: (100.00%) 1 scenario, 100 max VUs, 1m30s max duration (incl. graceful stop):
              * default: 100 looping VUs for 1m0s (gracefulStop: 30s)

INFO[0000] 🔐 Attempting login...                         source=console
INFO[0000] Login Status: 200                             source=console
INFO[0000] Login Response: {"success":true,"data":{"memberId":7,"email":"dnsrkd0414@naver.com","name":"김운강","nickname":"은하늑대4634","loginType":"LOCAL"},"meta":{"timestamp":"2025-11-18T05:28:16Z"}}  source=console
INFO[0000] 🍪 Extracted Cookie: JSESSIONID=FA4D9B9B046ECD951A9B1EBDF06F7198  source=console
INFO[0000] ✅ Login successful!                           source=console
INFO[0000] ℹ️  Testing 17 items (excluding ID 36 - Pants)  source=console


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=1.28s

    http_req_failed
    ✓ 'rate<0.05' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 11930   202.182498/s
    checks_succeeded...: 100.00% 11930 out of 11930
    checks_failed......: 0.00%   0 out of 11930

    ✓ detail status 200
    ✓ search status 200
    ✓ address status 200

    HTTP
    http_req_duration..............: avg=491.64ms min=79.02µs med=401.63ms max=3.02s p(90)=1.03s p(95)=1.28s
      { expected_response:true }...: avg=491.64ms min=79.02µs med=401.63ms max=3.02s p(90)=1.03s p(95)=1.28s
    http_req_failed................: 0.00%  0 out of 11931
    http_reqs......................: 11931  202.199445/s

    EXECUTION
    iteration_duration.............: avg=504.47ms min=17.5ms  med=413.57ms max=3.02s p(90)=1.06s p(95)=1.29s
    iterations.....................: 11930  202.182498/s
    vus............................: 100    min=100        max=100
    vus_max........................: 100    min=100        max=100

    NETWORK
    data_received..................: 16 MB  274 kB/s
    data_sent......................: 2.8 MB 48 kB/s




running (0m59.0s), 000/100 VUs, 11930 complete and 0 interrupted iterations
default ✓ [======================================] 100 VUs  1m0s
```

<font color="#00b0f0">더 가혹한 부하 테스트 성공 (VUser 500까지 과부화 (sleep(0.1) 추가))</font>
VUser 500명이 3개의 api에 대해서 요청을 보낼 때
- ✅ **Detail API**: 매우 안정적 (99.996%)
- ⚠️ **Search API**: 안정적 (99.973%)
- ⚠️ **Address API**: 안정적 (99.356%)
- ✅ 실패율: 0.04% (매우 안정적) 
- ✅ 총 요청: 101,388개 처리(34개 실패) 
- ✅ 성공률: 99.96% 
- ✅ RPS: VUser 500명일 때 294.0  
- ✅ p(95) 응답시간: 2.38초 (양호) 
- ✅ Thresholds: 모두 통과 ✓
- ⚠️ 서버 임계점: 400VUser부터 API timeout발생, 응답 시간 급격히 증가

<font color="#92d050">동일한 스크립트로 3번 테스트한 결과</font>
![[성능 테스트 (k6)-20251118160925382.webp|593]]

-> <font color="#00b0f0">결론:</font> 현재 application의 <font color="#92d050">서버는 VUser 350~400명 까지 매우 안정적으로 유지</font>된다
(실 사용자 600~700명까지 감당 가능)
다만 VUser가 400명 이상으로 증가하면 서버에 부하가 오기 시작한다
특히 <font color="#92d050">VUser가 증가함에 따라 address API에서 부하</font>가 오기 시작한다
(예상 요청 분포 34% 33% 33% / 실제 요청: 77% / 18% / 5%)
address API를 최적화 화면 더욱 안정적인 서버가 될 수 있을 것이라 분석된다
(스크립트 실행 후 터미널 출력 값)
``` zsh
➜  ~ k6 run overload_script.js

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: overload_script.js
        output: -

     scenarios: (100.00%) 1 scenario, 500 max VUs, 6m30s max duration (incl. graceful stop):
              * default: Up to 500 looping VUs for 6m0s over 8 stages (gracefulRampDown: 30s, gracefulStop: 30s)

INFO[0000] 🔥 6-Minute EXTREME Load Test: 100 → 500 VUs   source=console
INFO[0000] ⚠️  WARNING: This is a stress test to find server limits!  source=console
INFO[0000] 📊 Timeline:                                   source=console
INFO[0000]    0:00-0:30  Warm-up (0→100)                 source=console
INFO[0000]    0:30-1:30  Baseline (100) - 1분             source=console
INFO[0000]    1:30-2:30  Step 1 (100→200)                source=console
INFO[0000]    2:30-3:30  Step 2 (200→300)                source=console
INFO[0000]    3:30-4:30  Step 3 (300→400)                source=console
INFO[0000]    4:30-5:00  Step 4 (400→500)                source=console
INFO[0000]    5:00-5:30  🔥 PEAK LOAD (500) 🔥             source=console
INFO[0000]    5:30-6:00  Cool-down (500→0)               source=console
INFO[0000]    Total: 6 minutes                           source=console
INFO[0000] Login Status: 200                             source=console
INFO[0000] ✅ Login successful!                           source=console
INFO[0000] ℹ️  Testing 17 items (excluding ID 36)        source=console
INFO[0000] 🚀 Starting extreme stress test...             source=console
WARN[0134] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/items/32\": request timeout"
WARN[0161] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/items/37\": request timeout"
WARN[0166] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/items/33\": request timeout"
WARN[0170] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/items?keyword=&page=0&size=10&sort=price\": request timeout"
WARN[0177] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/items?keyword=&page=0&size=10&sort=price\": request timeout"
WARN[0184] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/items?keyword=&page=0&size=10&sort=price\": request timeout"
WARN[0233] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/items?keyword=&page=0&size=10&sort=price\": request timeout"
WARN[0256] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/items?keyword=&page=0&size=10&sort=price\": request timeout"
WARN[0259] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0262] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0266] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0267] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0270] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0271] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0273] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0286] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0286] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0288] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0295] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0295] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0305] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0305] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0305] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0306] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0308] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0309] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0311] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0313] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0317] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0318] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0321] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0321] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0325] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0326] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0328] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0335] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0335] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0339] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0347] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0351] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
WARN[0351] Request Failed                                error="Get \"https://carhartt-usedtransactions.com/v1/orders/address\": request timeout"
INFO[0360] 
======================================================================  source=console
INFO[0360] 🔥 EXTREME LOAD TEST SUMMARY: 100 → 500 VUs    source=console
INFO[0360] ======================================================================  source=console
INFO[0360] 
📈 Request Statistics:                        source=console
INFO[0360]    Total Requests:  101388                    source=console
INFO[0360]    Success Rate:    99.96%                    source=console
INFO[0360]    Failure Rate:    0.04%                     source=console
INFO[0360] 
⏱️  Response Times:                          source=console
INFO[0360]    Average:   737ms                           source=console
INFO[0360]    Median:    395ms                           source=console
INFO[0360]    p(90):     1872ms                          source=console
INFO[0360]    p(95):     2380ms                          source=console
INFO[0360]    p(99):     N/Ams                           source=console
INFO[0360]    Max:       7162ms                          source=console
INFO[0360] 
🚀 Throughput:                                source=console
INFO[0360]    Requests/sec:    294.0 RPS                 source=console
INFO[0360]    Data Received:   107.35 MB                 source=console
INFO[0360]    Data Sent:       21.71 MB                  source=console
INFO[0360] 
👥 Virtual Users:                             source=console
INFO[0360]    Max VUs:          500                      source=console
INFO[0360]    Total Iterations: 101387                   source=console
INFO[0360] 
📊 Per-API Statistics:                        source=console
INFO[0360]    Detail API:  N/A% success                  source=console
INFO[0360]    Search API:  N/A% success                  source=console
INFO[0360]    Address API: N/A% success                  source=console
INFO[0360] 
🎯 Performance Analysis:                      source=console
INFO[0360]    Peak Load: 500 VUs sustained for 30 seconds  source=console
INFO[0360]    ⚠️  EXCELLENT: Server handled 500 VUs well  source=console
INFO[0360]    💛 Production Capacity: 350-450 VUs recommended  source=console
INFO[0360]    📊 Peak: 500 VUs (verified)                 source=console
INFO[0360] 
🔥 Peak Load Analysis (5:00-5:30 at 500 VUs):  source=console
INFO[0360]    ✅ Server rock solid during extreme load!   source=console
INFO[0360]    💪 500 VUs is safe for production           source=console
INFO[0360]    🚀 Server can probably handle even more!    source=console
INFO[0360] 
💡 Recommendations:                           source=console
INFO[0360]    • Normal operations: 350 VUs (70% of max)  source=console
INFO[0360]    • Peak traffic: 425 VUs (85% of max)       source=console
INFO[0360]    • Absolute max: 500 VUs (tested & verified)  source=console
INFO[0360]    • Scaling potential: Test 600-700 VUs next  source=console
INFO[0360] 
======================================================================  source=console
      ✗ detail status 200
       ↳  99% — ✓ 77726 / ✗ 3 (요청 비율 77%)
      ✓ detail < 10s
      ✗ search status 200
       ↳  99% — ✓ 18529 / ✗ 5 (요청 비율 18%)
      ✓ search < 10s
      ✗ address status 200
       ↳  99% — ✓ 5091 / ✗ 33 (요청 비율 5%)
      ✓ address < 10s

      checks.........................: 99.97% ✓ 202733     ✗ 41    
      data_received..................: 113 MB 327 kB/s
      data_sent......................: 23 MB  66 kB/s
      http_req_blocked...............: avg=187.48µs min=0s           med=5.64µs   max=1.06s    p(90)=9.41µs   p(95)=11.12µs
      http_req_connecting............: avg=98.69µs  min=-396373776ns med=0s       max=1.08s    p(90)=0s       p(95)=0s     
    ✓ http_req_duration..............: avg=736.52ms min=0s           med=394.83ms max=7.16s    p(90)=1.87s    p(95)=2.38s  
        { expected_response:true }...: avg=736.82ms min=18.08µs      med=395.23ms max=7.16s    p(90)=1.87s    p(95)=2.38s  
    ✓ http_req_failed................: 0.04%  ✓ 41         ✗ 101347
      http_req_receiving.............: avg=85.54µs  min=0s           med=75.8µs   max=10.91ms  p(90)=137.64µs p(95)=166.5µs
      http_req_sending...............: avg=25.14µs  min=0s           med=17.58µs  max=8.96ms   p(90)=53.09µs  p(95)=66.7µs 
      http_req_tls_handshaking.......: avg=92.14µs  min=0s           med=0s       max=186.11ms p(90)=0s       p(95)=0s     
      http_req_waiting...............: avg=736.41ms min=0s           med=394.68ms max=7.16s    p(90)=1.87s    p(95)=2.38s  
      http_reqs......................: 101388 294.044635/s
      iteration_duration.............: avg=875.76ms min=118.02ms     med=523.75ms max=30.1s    p(90)=2.02s    p(95)=2.51s  
      iterations.....................: 101387 294.041734/s
      vus............................: 9      min=2        max=500 

running (5m44.8s), 000/500 VUs, 101387 complete and 0 interrupted iterations
default ✓ [======================================] 000/500 VUs  6m0s
➜  ~ 
```


## <font color="#de7802">부하 테스트 환경 셋팅 (K6, InfluxDB, Grafana 사용) (이 번 프로젝트에선 사용 x)</font>
<font color="#de7802">::: 부하 테스트 구조</font>
k6는 WSL2에 직접 설치, InfluxDB 및 Grafana는 docker container에 각각 고립시킴
(InfluxDB 및 Grafana는 장기간 running되어야 하며 설치 및 운영도 image기반임)

<font color="#de7802">::: InfluxDB 설치 (시계열 db)</font>


<font color="#de7802">::: grafana 설치 (data를 깔끔한 그래프로 표현)</font>




---
**더 찾아볼 것 - 생각해볼 것 :**
## 출처 : 