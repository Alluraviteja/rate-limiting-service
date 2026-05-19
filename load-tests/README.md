# k6 Load Tests

## Install k6
```bash
brew install k6
```

## Setup — run once before any test

Register the two test apps on the server (replace `BASE_URL`):

```bash
BASE_URL=http://<server-ip>:<port>

# App for throughput / latency / cache / fail-open tests (high-capacity plan)
curl -s -X POST $BASE_URL/api/v1/apps \
  -H "Content-Type: application/json" \
  -d '{"serviceName":"k6-test-app","description":"k6 load test app"}'

curl -s -X POST $BASE_URL/api/v1/plans \
  -H "Content-Type: application/json" \
  -d '{"serviceName":"k6-test-app","capacity":1000000,"refillRate":1000000,"refillPeriodSeconds":60,"pathPattern":"/**"}'

# App for concurrency accuracy test (tight 50-token plan)
curl -s -X POST $BASE_URL/api/v1/apps \
  -H "Content-Type: application/json" \
  -d '{"serviceName":"k6-accuracy-app","description":"accuracy test"}'

curl -s -X POST $BASE_URL/api/v1/plans \
  -H "Content-Type: application/json" \
  -d '{"serviceName":"k6-accuracy-app","capacity":50,"refillRate":50,"refillPeriodSeconds":60,"pathPattern":"/**"}'
```

---

## Running tests

### Run all tests in sequence (~10 min)
```bash
k6 run load-tests/main.js -e BASE_URL=http://<server-ip>:<port>
```

### Run a single test via `main.js`
```bash
k6 run load-tests/main.js -e BASE_URL=http://<server-ip>:<port> -e TEST=throughput
k6 run load-tests/main.js -e BASE_URL=http://<server-ip>:<port> -e TEST=latency
k6 run load-tests/main.js -e BASE_URL=http://<server-ip>:<port> -e TEST=cache
k6 run load-tests/main.js -e BASE_URL=http://<server-ip>:<port> -e TEST=accuracy
k6 run load-tests/main.js -e BASE_URL=http://<server-ip>:<port> -e TEST=failopen
```

### Run an individual file directly (also works)
```bash
k6 run load-tests/01_throughput.js -e BASE_URL=http://<server-ip>:<port>
k6 run load-tests/02_latency_percentiles.js -e BASE_URL=http://<server-ip>:<port>
```

---

## Scripts

| File | TEST= name | What it measures | Resume metric |
|---|---|---|---|
| `01_throughput.js` | `throughput` | Max req/sec, ramps 50→100 VUs | `X,XXX req/sec at <1% error rate` |
| `02_latency_percentiles.js` | `latency` | p50 / p95 / p99 at 20 VUs | `p99 Xms under 20 concurrent users` |
| `03_cache_effectiveness.js` | `cache` | Cold (DB hit) vs warm (cached) latency | `Xx faster after cache warm-up` |
| `04_concurrency_accuracy.js` | `accuracy` | No over-counting under 50-VU burst | `Zero race conditions, exact token limits` |
| `05_redis_failure_failopen.js` | `failopen` | 100% availability when Redis is stopped | `99%+ uptime during Redis outage` |

---

## Test sequence (when running all via main.js)

```
  0s  → throughput  (3m 15s)   ramp 50→100 VUs
210s  → latency     (2m)       20 VUs steady
345s  → cache       (1m)       30 VUs, cold vs warm
420s  → accuracy    (~30s)     50 VUs × 100 shared iterations
465s  → failopen    (2m)       ⚠ stop Redis at ~500s mark (see below)
```

### Fail-open test — manual step required

When the `failopen` scenario starts (~7m 45s into the full run), SSH into the server and stop Redis:

```bash
docker stop redis
# wait for the scenario to finish (~2 min), then restore:
docker start redis
```

---

## Reading the summary

At the end of every run `main.js` prints a results box:

```
╔══════════════════════════════════════════════════════╗
║                  RESULTS SUMMARY                    ║
╠══════════════════════════════════════════════════════╣
║  Total requests    : 48320                          ║
║  Req/sec (avg)     : 412.3 req/s                    ║
╠══════════════════════════════════════════════════════╣
║  Latency p50       : 18.4 ms                        ║
║  Latency p95       : 67.2 ms                        ║
║  Latency p99       : 124.5 ms                       ║
╠══════════════════════════════════════════════════════╣
║  Cache cold p50    : 45.2 ms                        ║
║  Cache warm p50    : 12.1 ms                        ║
║  Cache speedup     : 3.7x                           ║
╠══════════════════════════════════════════════════════╣
║  Tokens allowed    : 50 / 50 capacity               ║
║  Tokens blocked    : 50                             ║
║  Accuracy          : PASS (no over-count)           ║
╠══════════════════════════════════════════════════════╣
║  Fail-open rate    : 100.00% success                ║
╠══════════════════════════════════════════════════════╣
║  Error rate        : 0.00%                          ║
╚══════════════════════════════════════════════════════╝
```
