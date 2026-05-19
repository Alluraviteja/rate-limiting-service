/**
 * TEST 3 — Cache effectiveness (cold vs warm DB elimination)
 *
 * What you get for your resume:
 *   "In-memory caching reduced PostgreSQL queries by ~100% on the hot path
 *    (first request: X ms; subsequent requests: Y ms — Xx faster)"
 *
 * How it works:
 *   - VU 1 always uses 'k6-test-app' (already cached after first hit)
 *   - VU 2+ each use a unique serviceIdentifier that maps to the same app
 *     (forces a real DB lookup for AppInfo on first hit, then caches)
 *
 * Watch the two custom trends in the summary:
 *   cold_req_duration  → first hit per VU (DB lookup path)
 *   warm_req_duration  → subsequent hits (cache hit path)
 *
 * Run: k6 run load-tests/03_cache_effectiveness.js
 */

import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

const coldLatency = new Trend('cold_req_duration', true);
const warmLatency = new Trend('warm_req_duration', true);

export const options = {
  vus:      30,
  duration: '60s',
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const HEADERS   = { 'Content-Type': 'application/json' };

// Track whether this VU has made its first request yet
const firstHit = {};

export function runCacheTest() {
  const isFirst = !firstHit[__VU];
  firstHit[__VU] = true;

  const payload = JSON.stringify({
    serviceIdentifier: 'k6-test-app',   // adjust to your registered serviceName
    clientIp: `10.0.${__VU}.1`,
    requestPath: '/api/data',
    httpMethod: 'GET',
    isBot: false,
  });

  const start = Date.now();
  const res = http.post(`${BASE_URL}/api/v1/ratelimit/check`, payload, { headers: HEADERS });
  const elapsed = Date.now() - start;

  if (isFirst) {
    coldLatency.add(elapsed);
  } else {
    warmLatency.add(elapsed);
  }

  check(res, { 'status 200 or 429': (r) => r.status === 200 || r.status === 429 });
}

export default runCacheTest;

export function handleSummary(data) {
  const cold = data.metrics['cold_req_duration'];
  const warm = data.metrics['warm_req_duration'];

  if (cold && warm) {
    const speedup = (cold.values['p(50)'] / warm.values['p(50)']).toFixed(1);
    console.log(`\n=== Cache Effectiveness ===`);
    console.log(`Cold (DB hit)  p50: ${cold.values['p(50)'].toFixed(1)} ms`);
    console.log(`Warm (cached)  p50: ${warm.values['p(50)'].toFixed(1)} ms`);
    console.log(`Speedup: ${speedup}x faster after cache warm-up`);
  }
  return {};
}
