/**
 * TEST 2 — Latency percentiles (p50 / p95 / p99)
 *
 * What you get for your resume:
 *   "p99 latency of Xms under Y concurrent users"
 *
 * Uses a fixed VU count so latency isn't distorted by ramp-up noise.
 * 20 VUs is realistic for a sidecar/gateway call pattern.
 *
 * Run: k6 run load-tests/02_latency_percentiles.js
 */

import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

const checkLatency = new Trend('check_latency', true);

export const options = {
  vus:      20,
  duration: '120s',
  thresholds: {
    'http_req_duration{scenario:default}': [
      'p(50)<50',    // p50 under 50ms
      'p(95)<150',   // p95 under 150ms
      'p(99)<300',   // p99 under 300ms
    ],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

const PAYLOAD = JSON.stringify({
  serviceIdentifier: 'k6-test-app',
  clientIp: '10.0.0.2',
  requestPath: '/api/data',
  httpMethod: 'GET',
  traceId: 'k6-lat-001',
  deviceType: 'desktop',
  isBot: false,
});

const HEADERS = { 'Content-Type': 'application/json' };

export function runLatency() {
  const start = Date.now();
  const res = http.post(`${BASE_URL}/api/v1/ratelimit/check`, PAYLOAD, { headers: HEADERS });
  checkLatency.add(Date.now() - start);

  check(res, { 'status 200': (r) => r.status === 200 });
}

export default runLatency;
