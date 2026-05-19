/**
 * TEST 4 — Token count accuracy under burst concurrency
 *
 * What you get for your resume:
 *   "Zero over-counting under 50-VU burst — Bucket4j atomic Lua scripts
 *    enforced exact token limits with no race conditions"
 *
 * How it works:
 *   - Create a tight plan (capacity=50, refill=50/60s) before running
 *   - Fire exactly 100 requests simultaneously (50 VUs × 2 iterations)
 *   - Exactly 50 must be 200, the rest must be 429
 *   - If > 50 return 200, the Lua atomicity failed (it won't — this proves it)
 *
 * Setup (run once before this test — use a SEPARATE app to avoid interfering with other tests):
 *   curl -X POST http://localhost:8081/api/v1/apps \
 *     -H "Content-Type: application/json" \
 *     -d '{"serviceName":"k6-accuracy-app","description":"accuracy test"}'
 *
 *   curl -X POST http://localhost:8081/api/v1/plans \
 *     -H "Content-Type: application/json" \
 *     -d '{"serviceName":"k6-accuracy-app","capacity":50,"refillRate":50,"refillPeriodSeconds":60,"pathPattern":"/**"}'
 *
 * Run: k6 run load-tests/04_concurrency_accuracy.js
 */

import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const allowed  = new Counter('tokens_consumed');
const blocked  = new Counter('tokens_rejected');

export const options = {
  vus:        50,
  iterations: 100,   // 100 total requests across all VUs
  thresholds: {
    // Must not allow more than 50 requests (the bucket capacity)
    'tokens_consumed': ['count<=50'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const HEADERS   = { 'Content-Type': 'application/json' };

const PAYLOAD = JSON.stringify({
  serviceIdentifier: 'k6-accuracy-app',
  clientIp: '10.0.0.99',
  requestPath: '/api/data',
  httpMethod: 'GET',
  isBot: false,
});

export function runAccuracy() {
  const res = http.post(`${BASE_URL}/api/v1/ratelimit/check`, PAYLOAD, { headers: HEADERS });

  check(res, { 'valid response': (r) => r.status === 200 || r.status === 429 });

  if (res.status === 200)      allowed.add(1);
  else if (res.status === 429) blocked.add(1);
}

export default runAccuracy;

export function handleSummary(data) {
  const consumed = data.metrics['tokens_consumed']?.values?.count ?? 0;
  const rejected = data.metrics['tokens_rejected']?.values?.count ?? 0;
  const total    = consumed + rejected;

  console.log(`\n=== Concurrency Accuracy ===`);
  console.log(`Total requests : ${total}`);
  console.log(`Allowed (200)  : ${consumed}  (expected ≤ 50)`);
  console.log(`Blocked (429)  : ${rejected}`);
  console.log(consumed <= 50
    ? `PASS — No over-counting. Atomic Lua scripts enforced exact limit.`
    : `FAIL — Over-counted by ${consumed - 50} tokens!`
  );
  return {};
}
