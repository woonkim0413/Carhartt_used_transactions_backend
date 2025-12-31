_import http from 'k6/http';
import { sleep, check } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

export const options = {
vus: 100,
duration: '30s',

thresholds: {
'http_req_failed': ['rate<0.20'],
'http_req_duration': ['p(95)<10000'],
},
};

const BASE = 'https://carhartt-usedtransactions.com';

const ACCOUNTS = [
{ email: 'dnsrkd0414@naver.com', password: 'gjsxjsms123!' },
{ email: 'dnsrkd0410@naver.com', password: 'gjsxjsms123!!!' }
];

const VALID_ITEM_IDS = [22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 37, 38, 39];

const GROUP1_PERCENT = 0.34;
const GROUP2_PERCENT = 0.67;

export function setup() {
console.log('\n' + '='.repeat(70));
console.log('🔥 30sec Load Test with ElastiCache Session Sharing');
console.log('='.repeat(70));

const sessions = [];

// 각 계정으로 로그인
for (let i = 0; i < ACCOUNTS.length; i++) {
console.log(`\n📝 Logging in account ${i + 1}/${ACCOUNTS.length}...`);

    // ✅ 각 로그인마다 새로운 Cookie Jar 생성
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
      jar: jar,  // ✅ Cookie Jar 사용
    });
    
    console.log(`   📊 Login response status: ${res.status}`);
    
    if (res.status !== 200) {
      console.log(`   ❌ Login failed: status ${res.status}`);
      console.log(`   📄 Response body: ${res.body.substring(0, 200)}`);
      continue;
    }

    // ✅ Cookie Jar에서 쿠키 가져오기
    const cookies = jar.cookiesForURL(BASE);
    console.log(`   🍪 Cookies in jar: ${JSON.stringify(cookies)}`);
    
    if (!cookies || Object.keys(cookies).length === 0) {
      console.log(`   ❌ Login failed: no cookies in jar`);
      continue;
    }

    // JSESSIONID 찾기
    const jsessionid = cookies.JSESSIONID;
    if (!jsessionid || !jsessionid[0]) {
      console.log(`   ❌ Login failed: no JSESSIONID found`);
      console.log(`   📋 Available cookies: ${Object.keys(cookies).join(', ')}`);
      continue;
    }

    const cookieString = `JSESSIONID=${jsessionid[0].value}`;
    sessions.push(cookieString);
    
    console.log(`   ✅ Login OK - JSESSIONID: ${jsessionid[0].value.substring(0, 10)}...`);
    
    // 세션 저장 확인
    sleep(1);
    const testRes = http.get(`${BASE}/v1/items/22`, {
      headers: { Cookie: cookieString },
      timeout: '5s',
    });
    
    console.log(`   🧪 Session test status: ${testRes.status}`);
    
    if (testRes.status === 200) {
      const server = testRes.headers['x-upstream-server'] || testRes.headers['X-Upstream-Server'] || 'unknown';
      console.log(`   ✅ Session verified on server: ${server}`);
    } else {
      console.log(`   ⚠️  Session test failed: ${testRes.status}`);
      console.log(`   📄 Response: ${testRes.body.substring(0, 200)}`);
    }

}

if (sessions.length === 0) {
throw new Error('❌ No successful logins!');
}

console.log(`\n${'='.repeat(70)}`);
console.log(`🎉 Setup complete!`);
console.log(`   Total sessions: ${sessions.length}`);
console.log(`   Starting 30s test with 100 VUs...`);
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

if (vuRatio <= GROUP1_PERCENT) {
const itemId = VALID_ITEM_IDS[combinedIndex % VALID_ITEM_IDS.length];
const url = `${BASE}/v1/items/${itemId}`;

    const res = http.get(url, {
      headers,
      tags: { name: 'GET_item_detail' },
      timeout: '30s',
    });

    check(res, {
      'detail 200': (r) => r.status === 200,
      'has upstream': (r) => {
        return r.headers['x-upstream-server'] !== undefined || 
               r.headers['X-Upstream-Server'] !== undefined;
      },
    });

} else if (vuRatio <= GROUP2_PERCENT) {
const res = http.get(`${BASE}/v1/items?keyword=&page=0&size=10&sort=price`, {
headers,
tags: { name: 'GET_items_search' },
timeout: '30s',
});

    check(res, {
      'search 200': (r) => r.status === 200,
      'has upstream': (r) => {
        return r.headers['x-upstream-server'] !== undefined || 
               r.headers['X-Upstream-Server'] !== undefined;
      },
    });

} else {
const res = http.get(`${BASE}/v1/orders/address`, {
headers,
tags: { name: 'GET_address' },
timeout: '30s',
});

    check(res, {
      'address 200': (r) => r.status === 200,
      'has upstream': (r) => {
        return r.headers['x-upstream-server'] !== undefined || 
               r.headers['X-Upstream-Server'] !== undefined;
      },
    });

}

sleep(0.1);
}

export function teardown(data) {
console.log('\n' + '='.repeat(70));
console.log('🧹 Cleaning up sessions...');

for (let i = 0; i < data.sessions.length; i++) {
try {
http.post(`${BASE}/v1/local/logout`, null, {
headers: { Cookie: data.sessions[i] },
timeout: '5s',
});
console.log(`   ✅ Session ${i + 1} logged out`);
} catch (e) {
console.log(`   ⚠️  Session ${i + 1} logout error: ${e.message}`);
}
}

console.log('='.repeat(70) + '\n');
}

export function handleSummary(data) {
return {
stdout: textSummary(data, { indent: '  ', enableColors: true }),
};
}_
------
해당 k6 스크립트 테스트 하면 나오는 로그 값은 아래와 같다
----
➜  ~ k6 run two_ec2_k6_test.js

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   

/ \/ \ | |/ / / ‾‾\
/ \ |   (  |  (‾)  |
/ __________ \ |_|\_\ \_____/

     execution: local
        script: two_ec2_k6_test.js
        output: -

     scenarios: (100.00%) 1 scenario, 100 max VUs, 1m0s max duration (incl. graceful stop):
              * default: 100 looping VUs for 30s (gracefulStop: 30s)

WARN[0000] Error from API server error="listen tcp 127.0.0.1:6565: bind: address already in use"
INFO[0000]
====================================================================== source=console
INFO[0000] 🔥 30sec Load Test with ElastiCache Session Sharing source=console
INFO[0000] ====================================================================== source=console
INFO[0000]
📝 Logging in account 1/2... source=console
INFO[0000]    📊 Login response status: 200 source=console
INFO[0000]    🍪 Cookies in jar: {"JSESSIONID":["YzFmODlhM2MtOTgyMy00ZTlmLThlM2QtMmFiZmUwOWNiZTk5"]} source=console
data_received..................: 3.4 kB 17 kB/s
data_sent......................: 1.8 kB 9.1 kB/s
http_req_blocked...............: avg=75.73ms min=75.73ms med=75.73ms max=75.73ms p(90)=75.73ms p(95)=75.73ms
http_req_connecting............: avg=11.76ms min=11.76ms med=11.76ms max=11.76ms p(90)=11.76ms p(95)=11.76ms
✓ http_req_duration..............: avg=119.37ms min=119.37ms med=119.37ms max=119.37ms p(90)=119.37ms p(95)=119.37ms
{ expected_response:true }...: avg=119.37ms min=119.37ms med=119.37ms max=119.37ms p(90)=119.37ms p(95)=119.37ms
✓ http_req_failed................: 0.00% ✓ 0 ✗ 1
http_req_receiving.............: avg=362.54µs min=362.54µs med=362.54µs max=362.54µs p(90)=362.54µs p(95)=362.54µs
http_req_sending...............: avg=112.83µs min=112.83µs med=112.83µs max=112.83µs p(90)=112.83µs p(95)=112.83µs
http_req_tls_handshaking.......: avg=22.08ms min=22.08ms med=22.08ms max=22.08ms p(90)=22.08ms p(95)=22.08ms
http_req_waiting...............: avg=118.89ms min=118.89ms med=118.89ms max=118.89ms p(90)=118.89ms p(95)=118.89ms

Run       [======================================] setup()
default   [--------------------------------------]
ERRO[0000] TypeError: Cannot read property 'substring' of undefined or null