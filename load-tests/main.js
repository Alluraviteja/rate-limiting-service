/**
 * main.js — Run all load tests in sequence, or pick one with -e TEST=<name>
 *
 * Run ALL tests (~10 min total):
 *   k6 run load-tests/main.js -e BASE_URL=http://<server-ip>:<port>
 *
 * Run a SINGLE test:
 *   k6 run load-tests/main.js -e BASE_URL=http://<server-ip>:<port> -e TEST=throughput
 *   k6 run load-tests/main.js -e BASE_URL=http://<server-ip>:<port> -e TEST=latency
 *   k6 run load-tests/main.js -e BASE_URL=http://<server-ip>:<port> -e TEST=cache
 *   k6 run load-tests/main.js -e BASE_URL=http://<server-ip>:<port> -e TEST=accuracy
 *   k6 run load-tests/main.js -e BASE_URL=http://<server-ip>:<port> -e TEST=failopen
 *
 * Or still run individually:
 *   k6 run load-tests/01_throughput.js -e BASE_URL=http://<server-ip>:<port>
 *
 * Test sequence (when running all):
 *   0s   → throughput   (3m 15s)  ramp 50→100 VUs
 *   210s → latency      (2m)      20 VUs steady
 *   345s → cache        (1m)      30 VUs, cold vs warm
 *   420s → accuracy     (~30s)    50 VUs × 100 iterations
 *   465s → failopen     (2m)      stop Redis during 60s gap — instructions printed at start
 */

import { runThroughput }  from './01_throughput.js';
import { runLatency }     from './02_latency_percentiles.js';
import { runCacheTest }   from './03_cache_effectiveness.js';
import { runAccuracy }    from './04_concurrency_accuracy.js';
import { runFailOpen }    from './05_redis_failure_failopen.js';

// ─── Scenario definitions ────────────────────────────────────────────────────

const ALL_SCENARIOS = {
  throughput: {
    executor:  'ramping-vus',
    startVUs:  0,
    stages: [
      { duration: '30s', target: 50  },
      { duration: '60s', target: 50  },
      { duration: '30s', target: 100 },
      { duration: '60s', target: 100 },
      { duration: '15s', target: 0   },
    ],
    exec:      'runThroughput',
    startTime: '0s',
  },
  latency: {
    executor:  'constant-vus',
    vus:       20,
    duration:  '120s',
    exec:      'runLatency',
    startTime: '210s',
  },
  cache: {
    executor:  'constant-vus',
    vus:       30,
    duration:  '60s',
    exec:      'runCacheTest',
    startTime: '345s',
  },
  accuracy: {
    executor:   'shared-iterations',
    vus:        50,
    iterations: 100,
    maxDuration: '60s',
    exec:       'runAccuracy',
    startTime:  '420s',
  },
  failopen: {
    executor:  'constant-vus',
    vus:       10,
    duration:  '120s',
    exec:      'runFailOpen',
    env:       { PHASE: 'baseline' },
    startTime: '465s',
  },
};

// Select single test via -e TEST=<name>, or run all
const selected = __ENV.TEST;
const scenarios = selected
  ? (ALL_SCENARIOS[selected]
      ? { [selected]: { ...ALL_SCENARIOS[selected], startTime: '0s' } }
      : (() => { throw new Error(`Unknown TEST="${selected}". Valid: ${Object.keys(ALL_SCENARIOS).join(', ')}`); })())
  : ALL_SCENARIOS;

// ─── Options ─────────────────────────────────────────────────────────────────

export const options = {
  scenarios,
  thresholds: {
    http_req_duration:                  ['p(95)<500'],
    'errors':                           ['rate<0.01'],
    'tokens_consumed':                  ['count<=50'],
    'success_rate':                     ['rate>=0.99'],
  },
};

// ─── Exported exec functions (k6 calls these by name from scenarios) ─────────

export { runThroughput, runLatency, runCacheTest, runAccuracy, runFailOpen };

// ─── Lifecycle ────────────────────────────────────────────────────────────────

export function setup() {
  if (!selected) {
    console.log('\n╔══════════════════════════════════════════════════════╗');
    console.log('║         Rate Limiter — Full Load Test Suite          ║');
    console.log('╠══════════════════════════════════════════════════════╣');
    console.log('║  0s   throughput  — ramp 50→100 VUs (3m 15s)        ║');
    console.log('║  210s latency     — 20 VUs steady (2m)              ║');
    console.log('║  345s cache       — cold vs warm (1m)               ║');
    console.log('║  420s accuracy    — 50 VUs burst, 100 requests      ║');
    console.log('║  465s failopen    — stop Redis at ~500s mark!       ║');
    console.log('╚══════════════════════════════════════════════════════╝\n');
    console.log('ACTION NEEDED: When you see "failopen" scenario start (~7m 45s in),');
    console.log('  SSH into the server and run: docker stop redis');
    console.log('  Then restart after the test: docker start redis\n');
  } else {
    console.log(`\nRunning single test: ${selected}\n`);
  }
}

// ─── Summary ─────────────────────────────────────────────────────────────────

export function handleSummary(data) {
  const m = data.metrics;

  const p = (metric, pct) => m[metric]?.values?.[`p(${pct})`]?.toFixed(1) ?? 'n/a';
  const r = (metric)      => ((m[metric]?.values?.rate ?? 0) * 100).toFixed(2) + '%';
  const c = (metric)      => m[metric]?.values?.count ?? 0;

  console.log('\n╔══════════════════════════════════════════════════════╗');
  console.log('║                  RESULTS SUMMARY                    ║');
  console.log('╠══════════════════════════════════════════════════════╣');
  console.log(`║  Total requests    : ${String(m['http_reqs']?.values?.count ?? 0).padEnd(30)}║`);
  console.log(`║  Req/sec (avg)     : ${String((m['http_reqs']?.values?.rate ?? 0).toFixed(1) + ' req/s').padEnd(30)}║`);
  console.log('╠══════════════════════════════════════════════════════╣');
  console.log(`║  Latency p50       : ${String(p('http_req_duration', 50) + ' ms').padEnd(30)}║`);
  console.log(`║  Latency p95       : ${String(p('http_req_duration', 95) + ' ms').padEnd(30)}║`);
  console.log(`║  Latency p99       : ${String(p('http_req_duration', 99) + ' ms').padEnd(30)}║`);
  console.log('╠══════════════════════════════════════════════════════╣');

  const cold = m['cold_req_duration'];
  const warm = m['warm_req_duration'];
  if (cold && warm) {
    const speedup = (cold.values['p(50)'] / warm.values['p(50)']).toFixed(1);
    console.log(`║  Cache cold p50    : ${String(cold.values['p(50)'].toFixed(1) + ' ms').padEnd(30)}║`);
    console.log(`║  Cache warm p50    : ${String(warm.values['p(50)'].toFixed(1) + ' ms').padEnd(30)}║`);
    console.log(`║  Cache speedup     : ${String(speedup + 'x').padEnd(30)}║`);
    console.log('╠══════════════════════════════════════════════════════╣');
  }

  const consumed = c('tokens_consumed');
  if (consumed > 0 || m['tokens_rejected']) {
    const rejected = c('tokens_rejected');
    console.log(`║  Tokens allowed    : ${String(consumed + ' / 50 capacity').padEnd(30)}║`);
    console.log(`║  Tokens blocked    : ${String(rejected).padEnd(30)}║`);
    console.log(`║  Accuracy          : ${String(consumed <= 50 ? 'PASS (no over-count)' : 'FAIL').padEnd(30)}║`);
    console.log('╠══════════════════════════════════════════════════════╣');
  }

  if (m['success_rate']) {
    console.log(`║  Fail-open rate    : ${String(r('success_rate') + ' success').padEnd(30)}║`);
    console.log('╠══════════════════════════════════════════════════════╣');
  }

  console.log(`║  Error rate        : ${String(r('errors')).padEnd(30)}║`);
  console.log('╚══════════════════════════════════════════════════════╝\n');

  return {};
}
