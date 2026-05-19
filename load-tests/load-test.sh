#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# run.sh — Self-contained k6 load test runner (no separate JS file needed)
#
# Copy this single file to any server with k6 installed and run it directly.
#
# Usage:
#   chmod +x run.sh
#   ./run.sh                                     # all tests
#   ./run.sh -e TEST=throughput                  # single test
#   BASE_URL=http://1.2.3.4:8081 ./run.sh
#
# With remote server SSH spec detection:
#   SERVER_USER=ubuntu SERVER_IP=1.2.3.4 BASE_URL=http://localhost:8081 ./run.sh
#
# Environment variables:
#   BASE_URL     — target URL (default: http://localhost:8081)
#   SERVER_USER  — SSH user for remote spec detection
#   SERVER_IP    — SSH host for remote spec detection
#   TEST         — run a single test (throughput|latency|cache|accuracy|failopen)
#   REDIS_AUTO   — set to false to skip automatic Redis stop/start (default: true)
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
SERVER_USER="${SERVER_USER:-}"
SERVER_IP="${SERVER_IP:-}"
REDIS_AUTO="${REDIS_AUTO:-true}"

# ─── Server spec detection ────────────────────────────────────────────────────

echo ""
echo "Detecting server specs..."

if [ -n "$SERVER_USER" ] && [ -n "$SERVER_IP" ]; then
  echo "  SSH → $SERVER_USER@$SERVER_IP"
  SSH="ssh -o ConnectTimeout=5 -o BatchMode=yes $SERVER_USER@$SERVER_IP"

  SERVER_CPU=$($SSH "echo \$(nproc) vCPU" 2>/dev/null || echo "unknown")
  SERVER_RAM=$($SSH "free -h | awk '/^Mem:/{print \$2}'" 2>/dev/null || echo "unknown")
  SERVER_OS=$($SSH  "lsb_release -d 2>/dev/null | cut -f2 || grep PRETTY_NAME /etc/os-release | cut -d= -f2 | tr -d '\"'" 2>/dev/null || echo "unknown")
  SERVER_DISK=$($SSH "df -h / | awk 'NR==2{print \$2}'" 2>/dev/null || echo "unknown")
  SERVER_NOTES="Remote server via SSH tunnel"
else
  echo "  No SERVER_USER/SERVER_IP set — using local machine specs"
  if [[ "$(uname)" == "Darwin" ]]; then
    _cpu_count=$(sysctl -n hw.logicalcpu 2>/dev/null || echo "?")
    _cpu_brand=$(sysctl -n machdep.cpu.brand_string 2>/dev/null || echo "Apple Silicon")
    _ram_bytes=$(sysctl -n hw.memsize 2>/dev/null || echo "0")
    _ram_gb=$(( _ram_bytes / 1024 / 1024 / 1024 ))
    _disk=$(df -h / | awk 'NR==2{print $2}')
    SERVER_CPU="$_cpu_count vCPU ($_cpu_brand)"
    SERVER_RAM="${_ram_gb} GB"
    SERVER_OS="macOS $(sw_vers -productVersion 2>/dev/null || echo '')"
    SERVER_DISK="$_disk"
    SERVER_NOTES="Local Mac (tunnel to remote service)"
  else
    SERVER_CPU="$(nproc) vCPU"
    SERVER_RAM="$(free -h | awk '/^Mem:/{print $2}')"
    SERVER_OS="$(lsb_release -d 2>/dev/null | cut -f2 || grep PRETTY_NAME /etc/os-release | cut -d= -f2 | tr -d '"' || echo 'Linux')"
    SERVER_DISK="$(df -h / | awk 'NR==2{print $2}')"
    SERVER_NOTES="Local Linux machine"
  fi
fi

echo "  CPU  : $SERVER_CPU"
echo "  RAM  : $SERVER_RAM"
echo "  OS   : $SERVER_OS"
echo "  Disk : $SERVER_DISK"
echo ""

# ─── Redis lifecycle helpers ──────────────────────────────────────────────────

redis_cmd() {
  local action="$1"
  if [ -n "$SERVER_USER" ] && [ -n "$SERVER_IP" ]; then
    ssh -o ConnectTimeout=5 -o BatchMode=yes "$SERVER_USER@$SERVER_IP" "sudo systemctl $action redis"
  else
    sudo systemctl "$action" redis
  fi
}

REDIS_STOPPED_FLAG="$(mktemp /tmp/run_sh_redis_stopped.XXXXXX)"
rm -f "$REDIS_STOPPED_FLAG"

schedule_redis_lifecycle() {
  local stop_after="$1"
  local restart_after="$2"
  (
    sleep "$stop_after"
    echo ""
    echo "[run.sh] Stopping Redis for fail-open test..."
    if redis_cmd stop; then
      echo "[run.sh] Redis stopped."
      touch "$REDIS_STOPPED_FLAG"
    else
      echo "[run.sh] WARNING: could not stop Redis — check permissions"
    fi
    sleep "$restart_after"
    echo "[run.sh] Restarting Redis..."
    redis_cmd start && echo "[run.sh] Redis restarted." || echo "[run.sh] WARNING: could not restart Redis — run: sudo systemctl start redis"
    rm -f "$REDIS_STOPPED_FLAG"
  ) &
}

# ─── Build k6 args ────────────────────────────────────────────────────────────

K6_ARGS=(
  -e "BASE_URL=$BASE_URL"
  -e "SERVER_CPU=$SERVER_CPU"
  -e "SERVER_RAM=$SERVER_RAM"
  -e "SERVER_OS=$SERVER_OS"
  -e "SERVER_DISK=$SERVER_DISK"
  -e "SERVER_NOTES=$SERVER_NOTES"
)

[ -n "${TEST:-}" ] && K6_ARGS+=(-e "TEST=$TEST")

# ─── Redis stop/start scheduling ─────────────────────────────────────────────
#
# Full suite:   failopen_baseline ends at 495s → stop at 496s, restart at 590s
# TEST=failopen: baseline ends at 30s  → stop at 31s,  restart at 125s

if [ "$REDIS_AUTO" = "true" ]; then
  if [ -z "${TEST:-}" ]; then
    echo "Redis lifecycle: stop at ~496s, restart at ~590s (full suite)"
    schedule_redis_lifecycle 496 94
  elif [ "${TEST:-}" = "failopen" ]; then
    echo "Redis lifecycle: stop at ~31s, restart at ~125s (failopen only)"
    schedule_redis_lifecycle 31 94
  fi
fi

# Ensure Redis is restarted if the script exits early (Ctrl+C, error, etc.)
trap '
  if [ -f "$REDIS_STOPPED_FLAG" ]; then
    echo ""
    echo "[run.sh] Ensuring Redis is restarted before exit..."
    redis_cmd start && echo "[run.sh] Redis restarted." || echo "[run.sh] WARNING: could not restart Redis — run: sudo systemctl start redis"
    rm -f "$REDIS_STOPPED_FLAG"
  fi
  rm -f "$K6_SCRIPT"
' EXIT

# ─── Write embedded k6 JS to a temp file and run ─────────────────────────────

K6_SCRIPT="$(mktemp /tmp/k6-ratelimit-XXXXXX.js)"
mkdir -p results

cat > "$K6_SCRIPT" << 'EOF'
/**
 * Rate Limiter — Full Load Test Suite
 * Generated by run.sh — do not edit this temp file directly.
 *
 * Tests:
 *   1. Throughput      — ramp 50→100 VUs, measure req/sec
 *   2. Latency         — 20 VUs steady, measure p50/p95/p99
 *   3. Cache           — cold vs warm DB elimination
 *   4. Accuracy        — 50-VU burst, prove no over-counting
 *   5. Fail-open       — Redis down, service must stay up
 */

import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// ─── Metrics ─────────────────────────────────────────────────────────────────

const errorRate       = new Rate('errors');
const checkLatency    = new Trend('check_latency', true);
const coldLatency     = new Trend('cold_req_duration', true);
const warmLatency     = new Trend('warm_req_duration', true);
const tokensAllowed   = new Counter('tokens_consumed');
const tokensBlocked   = new Counter('tokens_rejected');
const successRate     = new Rate('success_rate');
const baselineLatency = new Trend('baseline_duration', true);
const degradedLatency = new Trend('degraded_duration', true);

// ─── Per-VU state (each VU gets its own isolated copy) ───────────────────────

let cacheIsFirst = true;

// ─── Common config ────────────────────────────────────────────────────────────

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const HEADERS  = { 'Content-Type': 'application/json' };

// ─── Scenarios ────────────────────────────────────────────────────────────────

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
    executor:    'shared-iterations',
    vus:         50,
    iterations:  100,
    maxDuration: '60s',
    exec:        'runAccuracy',
    startTime:   '420s',
  },
  failopen_baseline: {
    executor:  'constant-vus',
    vus:       10,
    duration:  '30s',
    exec:      'runFailOpen',
    env:       { PHASE: 'baseline' },
    startTime: '465s',
  },
  // 30s gap (495s–525s) — run.sh automatically stops Redis here
  failopen_degraded: {
    executor:  'constant-vus',
    vus:       10,
    duration:  '60s',
    exec:      'runFailOpen',
    env:       { PHASE: 'degraded' },
    startTime: '525s',
  },
};

function selectScenarios(name) {
  if (name === 'failopen') {
    return {
      failopen_baseline: { ...ALL_SCENARIOS['failopen_baseline'], startTime: '0s' },
      failopen_degraded: { ...ALL_SCENARIOS['failopen_degraded'], startTime: '30s' },
    };
  }
  if (ALL_SCENARIOS[name]) {
    return { [name]: { ...ALL_SCENARIOS[name], startTime: '0s' } };
  }
  throw new Error(`Unknown TEST="${name}". Valid: throughput, latency, cache, accuracy, failopen`);
}

const selected  = __ENV.TEST;
const scenarios = selected ? selectScenarios(selected) : ALL_SCENARIOS;

// ─── Options ─────────────────────────────────────────────────────────────────

export const options = {
  scenarios,
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(50)', 'p(90)', 'p(95)', 'p(99)'],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    errors:            ['rate<0.01'],
    tokens_consumed:   ['count<=50'],
    success_rate:      ['rate>=0.99'],
  },
};

// ─── TEST 1: Throughput ───────────────────────────────────────────────────────

const THROUGHPUT_PAYLOAD = JSON.stringify({
  serviceIdentifier: 'k6-test-app',
  clientIp:          '10.0.0.1',
  requestPath:       '/api/data',
  httpMethod:        'GET',
  traceId:           'k6-trace-001',
  deviceType:        'desktop',
  isBot:             false,
});

export function runThroughput() {
  const res = http.post(`${BASE_URL}/api/v1/ratelimit/check`, THROUGHPUT_PAYLOAD, { headers: HEADERS });

  const ok = check(res, {
    'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
    'response has allowed field': (r) => {
      try { return JSON.parse(r.body).allowed !== undefined; } catch { return false; }
    },
  });

  errorRate.add(!ok);
}

// ─── TEST 2: Latency percentiles ──────────────────────────────────────────────

const LATENCY_PAYLOAD = JSON.stringify({
  serviceIdentifier: 'k6-test-app',
  clientIp:          '10.0.0.2',
  requestPath:       '/api/data',
  httpMethod:        'GET',
  traceId:           'k6-lat-001',
  deviceType:        'desktop',
  isBot:             false,
});

export function runLatency() {
  const start = Date.now();
  const res   = http.post(`${BASE_URL}/api/v1/ratelimit/check`, LATENCY_PAYLOAD, { headers: HEADERS });
  checkLatency.add(Date.now() - start);

  check(res, { 'status 200': (r) => r.status === 200 });
}

// ─── TEST 3: Cache effectiveness ──────────────────────────────────────────────
//
// Pre-req: 'k6-test-app' plan must exist in the DB.

export function runCacheTest() {
  const wasFirst = cacheIsFirst;
  cacheIsFirst = false;

  const serviceIdentifier = wasFirst ? `k6-cold-vu${__VU}` : 'k6-test-app';

  const payload = JSON.stringify({
    serviceIdentifier,
    clientIp:    `10.0.${__VU}.1`,
    requestPath: '/api/data',
    httpMethod:  'GET',
    isBot:       false,
  });

  const start   = Date.now();
  const res     = http.post(`${BASE_URL}/api/v1/ratelimit/check`, payload, { headers: HEADERS });
  const elapsed = Date.now() - start;

  if (wasFirst) coldLatency.add(elapsed);
  else          warmLatency.add(elapsed);

  check(res, { 'status 200 or 429': (r) => r.status === 200 || r.status === 429 });
}

// ─── TEST 4: Concurrency accuracy ─────────────────────────────────────────────
//
// Pre-req: create a tight plan before running (capacity=50, refillPeriodSeconds=60):
//   curl -X POST http://localhost:8081/api/v1/apps \
//     -H "Content-Type: application/json" \
//     -d '{"serviceName":"k6-accuracy-app","description":"accuracy test"}'
//
//   curl -X POST http://localhost:8081/api/v1/plans \
//     -H "Content-Type: application/json" \
//     -d '{"serviceName":"k6-accuracy-app","capacity":50,"refillRate":50,"refillPeriodSeconds":60,"pathPattern":"/**"}'

const ACCURACY_PAYLOAD = JSON.stringify({
  serviceIdentifier: 'k6-accuracy-app',
  clientIp:          '10.0.0.99',
  requestPath:       '/api/data',
  httpMethod:        'GET',
  isBot:             false,
});

export function runAccuracy() {
  const res = http.post(`${BASE_URL}/api/v1/ratelimit/check`, ACCURACY_PAYLOAD, { headers: HEADERS });

  check(res, { 'valid response': (r) => r.status === 200 || r.status === 429 });

  if (res.status === 200)      tokensAllowed.add(1);
  else if (res.status === 429) tokensBlocked.add(1);
}

// ─── TEST 5: Redis fail-open resilience ───────────────────────────────────────

const FAILOPEN_PAYLOAD = JSON.stringify({
  serviceIdentifier: 'k6-test-app',
  clientIp:          '10.0.0.5',
  requestPath:       '/api/data',
  httpMethod:        'GET',
  isBot:             false,
});

export function runFailOpen() {
  const phase = __ENV.PHASE || 'baseline';

  const start = Date.now();
  const res   = http.post(`${BASE_URL}/api/v1/ratelimit/check`, FAILOPEN_PAYLOAD, { headers: HEADERS });
  const ms    = Date.now() - start;

  const ok = check(res, {
    'request succeeded (200 or 429)': (r) => r.status === 200 || r.status === 429,
    'not a 5xx error':                (r) => r.status < 500,
  });

  successRate.add(ok ? 1 : 0);

  if (phase === 'baseline') baselineLatency.add(ms);
  else                      degradedLatency.add(ms);
}

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
    console.log('║  465s failopen    — baseline 30s, then stop Redis!  ║');
    console.log('║  525s failopen    — degraded 60s (Redis DOWN)       ║');
    console.log('╚══════════════════════════════════════════════════════╝\n');
    console.log('NOTE: run.sh handles Redis stop/start automatically.');
    console.log('      If running k6 directly, stop Redis manually during the 30s gap.\n');
  } else {
    console.log(`\nRunning single test: ${selected}\n`);
  }
}

// ─── Summary (printed to console AND saved to ./results/run-<timestamp>.txt) ──

export function handleSummary(data) {
  const m = data.metrics;

  const p = (metric, pct) => m[metric]?.values?.[`p(${pct})`]?.toFixed(1) ?? 'n/a';
  const r = (metric)      => ((m[metric]?.values?.rate ?? 0) * 100).toFixed(2) + '%';
  const c = (metric)      => m[metric]?.values?.count ?? 0;

  const lines = [];
  const log = (line) => { lines.push(line); console.log(line); };

  const runAt   = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
  const outFile = `results/run-${runAt}.txt`;

  const serverCPU   = __ENV.SERVER_CPU   || 'unknown';
  const serverRAM   = __ENV.SERVER_RAM   || 'unknown';
  const serverOS    = __ENV.SERVER_OS    || 'unknown';
  const serverDisk  = __ENV.SERVER_DISK  || 'unknown';
  const serverNotes = __ENV.SERVER_NOTES || '-';
  const testMode    = __ENV.TEST         || 'all';

  log('\n╔══════════════════════════════════════════════════════╗');
  log('║                  RESULTS SUMMARY                    ║');
  log('╠══════════════════════════════════════════════════════╣');
  log(`║  Run at            : ${String(new Date().toISOString()).padEnd(30)}║`);
  log(`║  Test              : ${String(testMode).padEnd(30)}║`);
  log('╠══════════════════════════════════════════════════════╣');
  log('║                  SERVER SPECS                        ║');
  log('╠══════════════════════════════════════════════════════╣');
  log(`║  CPU               : ${String(serverCPU).padEnd(30)}║`);
  log(`║  RAM               : ${String(serverRAM).padEnd(30)}║`);
  log(`║  Disk              : ${String(serverDisk).padEnd(30)}║`);
  log(`║  OS                : ${String(serverOS).padEnd(30)}║`);
  log(`║  Notes             : ${String(serverNotes).padEnd(30)}║`);
  log('╠══════════════════════════════════════════════════════╣');
  log('║                  PERFORMANCE                         ║');
  log('╠══════════════════════════════════════════════════════╣');
  log(`║  Total requests    : ${String(m['http_reqs']?.values?.count ?? 0).padEnd(30)}║`);
  log(`║  Req/sec (avg)     : ${String((m['http_reqs']?.values?.rate ?? 0).toFixed(1) + ' req/s').padEnd(30)}║`);
  log('╠══════════════════════════════════════════════════════╣');
  log(`║  Latency p50       : ${String(p('http_req_duration', 50) + ' ms').padEnd(30)}║`);
  log(`║  Latency p90       : ${String(p('http_req_duration', 90) + ' ms').padEnd(30)}║`);
  log(`║  Latency p95       : ${String(p('http_req_duration', 95) + ' ms').padEnd(30)}║`);
  log(`║  Latency p99       : ${String(p('http_req_duration', 99) + ' ms').padEnd(30)}║`);
  log('╠══════════════════════════════════════════════════════╣');

  const cold = m['cold_req_duration'];
  const warm = m['warm_req_duration'];
  if (cold && warm) {
    const coldP50 = cold.values['p(50)'] ?? cold.values['med'];
    const warmP50 = warm.values['p(50)'] ?? warm.values['med'];
    const speedup = coldP50 && warmP50 ? (coldP50 / warmP50).toFixed(1) : 'n/a';
    log('║              CACHE EFFECTIVENESS                     ║');
    log('╠══════════════════════════════════════════════════════╣');
    log(`║  Cache cold p50    : ${String((coldP50?.toFixed(1) ?? 'n/a') + ' ms').padEnd(30)}║`);
    log(`║  Cache warm p50    : ${String((warmP50?.toFixed(1) ?? 'n/a') + ' ms').padEnd(30)}║`);
    log(`║  Cache speedup     : ${String(speedup + 'x faster after cache warm-up').padEnd(30)}║`);
    log('╠══════════════════════════════════════════════════════╣');
  }

  const consumed = c('tokens_consumed');
  const rejected = c('tokens_rejected');
  if (consumed > 0 || m['tokens_rejected']) {
    const accuracyResult = consumed <= 50 ? 'PASS (no over-count)' : `FAIL (over by ${consumed - 50})`;
    log('║              CONCURRENCY ACCURACY                    ║');
    log('╠══════════════════════════════════════════════════════╣');
    log(`║  Total requests    : ${String(consumed + rejected).padEnd(30)}║`);
    log(`║  Tokens allowed    : ${String(consumed + ' / 50 capacity').padEnd(30)}║`);
    log(`║  Tokens blocked    : ${String(rejected).padEnd(30)}║`);
    log(`║  Accuracy          : ${String(accuracyResult).padEnd(30)}║`);
    log('╠══════════════════════════════════════════════════════╣');
  }

  const baseline = m['baseline_duration'];
  const degraded = m['degraded_duration'];
  if (m['success_rate'] || baseline || degraded) {
    log('║              REDIS FAIL-OPEN RESILIENCE              ║');
    log('╠══════════════════════════════════════════════════════╣');
    log(`║  Fail-open success : ${String(r('success_rate')).padEnd(30)}║`);
    if (baseline) log(`║  Baseline p50      : ${String(p('baseline_duration', 50) + ' ms (Redis UP)').padEnd(30)}║`);
    if (degraded) log(`║  Degraded p50      : ${String(p('degraded_duration', 50) + ' ms (Redis DOWN)').padEnd(30)}║`);
    log('╠══════════════════════════════════════════════════════╣');
  }

  log(`║  Overall error rate: ${String(r('errors')).padEnd(30)}║`);
  log('╚══════════════════════════════════════════════════════╝');
  log(`\nResults saved to: ${outFile}`);
  log('');

  return {
    [outFile]: lines.join('\n'),
  };
}
EOF

k6 run "$K6_SCRIPT" "${K6_ARGS[@]}" "$@"
