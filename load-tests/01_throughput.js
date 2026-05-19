/**
 * TEST 1 — Throughput (req/sec) on the hot check path
 *
 * What you get for your resume:
 *   "Sustained X,XXX req/sec on the rate-limit check endpoint with <1% error rate"
 *
 * How to read results:
 *   - http_reqs rate  → your req/sec number
 *   - http_req_failed → should stay near 0% (only 429s would count as failures here,
 *                        but we use a high-capacity plan so they won't happen)
 *
 * Run: k6 run load-tests/01_throughput.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '30s', target: 50  },  // ramp up to 50 VUs
    { duration: '60s', target: 50  },  // hold — steady throughput measurement
    { duration: '30s', target: 100 },  // ramp to 100 VUs
    { duration: '60s', target: 100 },  // hold — peak throughput measurement
    { duration: '15s', target: 0   },  // ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95% of requests must finish within 500ms
    errors:            ['rate<0.01'],  // error rate must stay below 1%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

const PAYLOAD = JSON.stringify({
  serviceIdentifier: 'k6-test-app',
  clientIp: '10.0.0.1',
  requestPath: '/api/data',
  httpMethod: 'GET',
  traceId: 'k6-trace-001',
  deviceType: 'desktop',
  isBot: false,
});

const HEADERS = { 'Content-Type': 'application/json' };

export function runThroughput() {
  const res = http.post(`${BASE_URL}/api/v1/ratelimit/check`, PAYLOAD, { headers: HEADERS });

  const ok = check(res, {
    'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
    'response has allowed field': (r) => {
      try { return JSON.parse(r.body).allowed !== undefined; } catch { return false; }
    },
  });

  errorRate.add(!ok);
}

export default runThroughput;
