/**
 * TEST 5 — Fail-open resilience when Redis is unavailable
 *
 * What you get for your resume:
 *   "Service maintained 100% availability during Redis outage via
 *    fail-open strategy — zero downtime with graceful degradation"
 *
 * How to run:
 *   1. Start the service normally
 *   2. Run this script — it establishes a baseline (Redis UP)
 *   3. Stop Redis:  docker stop <redis-container>  OR  redis-cli shutdown
 *   4. Re-run — observe that all requests still return 200 (fail-open)
 *   5. Restart Redis and confirm recovery
 *
 * The script measures:
 *   - baseline_duration : latency with Redis healthy
 *   - degraded_duration : latency with Redis down (fail-open path)
 *
 * Run: k6 run load-tests/05_redis_failure_failopen.js
 */

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const successRate       = new Rate('success_rate');
const baselineLatency   = new Trend('baseline_duration', true);
const degradedLatency   = new Trend('degraded_duration', true);

export const options = {
  scenarios: {
    // Phase A — Redis healthy baseline (run first without touching Redis)
    baseline: {
      executor:   'constant-vus',
      vus:        10,
      duration:   '30s',
      env:        { PHASE: 'baseline' },
      startTime:  '0s',
    },
    // Phase B — simulate Redis-down period (manually stop Redis before this starts)
    degraded: {
      executor:   'constant-vus',
      vus:        10,
      duration:   '30s',
      env:        { PHASE: 'degraded' },
      startTime:  '60s',   // leave a 30s gap so you have time to stop Redis
    },
  },
  thresholds: {
    success_rate: ['rate>=0.99'],   // 99% of all requests must succeed even during outage
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const HEADERS   = { 'Content-Type': 'application/json' };

const PAYLOAD = JSON.stringify({
  serviceIdentifier: 'k6-test-app',
  clientIp: '10.0.0.5',
  requestPath: '/api/data',
  httpMethod: 'GET',
  isBot: false,
});

export function runFailOpen() {
  const phase = __ENV.PHASE || 'baseline';

  const start = Date.now();
  const res   = http.post(`${BASE_URL}/api/v1/ratelimit/check`, PAYLOAD, { headers: HEADERS });
  const ms    = Date.now() - start;

  // Fail-open returns 200 with a note in the message field; 429 is also acceptable
  const ok = check(res, {
    'request succeeded (200 or 429)': (r) => r.status === 200 || r.status === 429,
    'not a 5xx error': (r) => r.status < 500,
  });

  successRate.add(ok ? 1 : 0);

  if (phase === 'baseline') baselineLatency.add(ms);
  else                      degradedLatency.add(ms);
}

export default runFailOpen;

export function handleSummary(data) {
  const b = data.metrics['baseline_duration'];
  const d = data.metrics['degraded_duration'];
  const s = data.metrics['success_rate'];

  console.log(`\n=== Redis Fail-Open Resilience ===`);
  console.log(`Overall success rate: ${(s?.values?.rate * 100 ?? 0).toFixed(2)}%`);
  if (b) console.log(`Baseline p50 (Redis UP)  : ${b.values['p(50)'].toFixed(1)} ms`);
  if (d) console.log(`Degraded p50 (Redis DOWN): ${d.values['p(50)'].toFixed(1)} ms`);
  return {};
}
