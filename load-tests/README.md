# k6 Load Tests

A single self-contained script (`load-test.sh`) that embeds all six k6 scenarios. No separate JS files needed — copy the script to any server with k6 installed and run it directly.

---

## Prerequisites

Install k6 on the target server:

```bash
# Ubuntu / Debian
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install k6

# macOS
brew install k6
```

---

## Copy to server and run

**Step 1 — copy the script:**
```bash
scp load-tests/load-test.sh <user>@<server-ip>:~/
```

**Step 2 — SSH into the server:**
```bash
ssh <user>@<server-ip>
```

**Step 3 — make it executable:**
```bash
chmod +x load-test.sh
```

**Step 4 — run all tests:**
```bash
BASE_URL=http://localhost:8081 ./load-test.sh
```

**Run a single test:**
```bash
BASE_URL=http://localhost:8081 TEST=throughput ./load-test.sh
BASE_URL=http://localhost:8081 TEST=latency    ./load-test.sh
BASE_URL=http://localhost:8081 TEST=cache      ./load-test.sh
BASE_URL=http://localhost:8081 TEST=accuracy   ./load-test.sh
BASE_URL=http://localhost:8081 TEST=failopen   ./load-test.sh
```

---

## Setup — run once before any test

Register the two test apps (replace `BASE_URL`):

```bash
BASE_URL=http://localhost:8081

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

## Running from a remote machine via SSH tunnel

If you want to run k6 locally and hit a remote server through a tunnel:

**Terminal 1 — keep the tunnel open:**
```bash
ssh -L 8081:<container-ip>:8081 <user>@<server-ip> -N
```

**Terminal 2 — run the tests:**
```bash
SERVER_USER=<user> SERVER_IP=<server-ip> BASE_URL=http://localhost:8081 \
  ./load-tests/load-test.sh
```

> Note: latency numbers will include SSH tunnel round-trip overhead. Running directly on the server (above) gives application-only latency.

---

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `BASE_URL` | `http://localhost:8081` | Target service URL |
| `TEST` | _(all)_ | Run a single test: `throughput`, `latency`, `cache`, `accuracy`, `failopen` |
| `SERVER_USER` | — | SSH user for remote spec detection and Redis control |
| `SERVER_IP` | — | SSH host for remote spec detection and Redis control |
| `REDIS_AUTO` | `true` | Set to `false` to skip automatic Redis stop/start |

**Skip Redis automation:**
```bash
REDIS_AUTO=false TEST=throughput BASE_URL=http://localhost:8081 ./load-test.sh
```

---

## Test sequence (full run)

```
  0s  → throughput        (3m 15s)   ramp 50→100 VUs
210s  → latency           (2m)       20 VUs steady
345s  → cache             (1m)       30 VUs, cold vs warm
420s  → accuracy          (~30s)     50 VUs × 100 shared iterations
465s  → failopen_baseline (30s)      10 VUs, Redis healthy
496s  →   [30s gap]                  load-test.sh stops Redis automatically
525s  → failopen_degraded (60s)      10 VUs, Redis down (fail-open path)
585s  →   load-test.sh restarts Redis
```

### Fail-open test — fully automated

`load-test.sh` stops and restarts Redis at the right times via `sudo systemctl`. No manual steps needed.

If `load-test.sh` is interrupted mid-test (Ctrl+C), it restarts Redis automatically before exiting.

If you need to control Redis manually:
```bash
sudo systemctl stop redis
sudo systemctl start redis
```

---

## What each scenario measures

| TEST= name | What it measures |
|---|---|
| `throughput` | Max req/sec — ramps 50→100 VUs over 3m 15s |
| `latency` | Clean p50/p95/p99 at 20 VUs steady, no throughput pressure |
| `cache` | Cold (first DB lookup) vs warm (in-process cache) latency |
| `accuracy` | 50 VUs fire 100 shared requests at a 50-token bucket — verifies no over-count |
| `failopen` | Baseline (Redis up) then degraded (Redis down) — verifies 100% availability |

---

## Reading the summary

At the end of every run the script prints a results box and saves it to `results/run-<timestamp>.txt`:

```
╔══════════════════════════════════════════════════════╗
║                  RESULTS SUMMARY                    ║
╠══════════════════════════════════════════════════════╣
║  Run at            : 2026-05-19T23:15:35.233Z      ║
║  Test              : all                           ║
╠══════════════════════════════════════════════════════╣
║                  SERVER SPECS                        ║
╠══════════════════════════════════════════════════════╣
║  CPU               : 3 vCPU                        ║
║  RAM               : 3.7Gi                         ║
║  Disk              : 75G                           ║
║  OS                : Ubuntu 22.04.5 LTS            ║
║  Notes             : Local Linux machine           ║
╠══════════════════════════════════════════════════════╣
║                  PERFORMANCE                         ║
╠══════════════════════════════════════════════════════╣
║  Total requests    : 635870                        ║
║  Req/sec (avg)     : 1086.9 req/s                  ║
╠══════════════════════════════════════════════════════╣
║  Latency p50       : 12.3 ms                       ║
║  Latency p90       : 73.2 ms                       ║
║  Latency p95       : 97.4 ms                       ║
║  Latency p99       : 162.6 ms                      ║
╠══════════════════════════════════════════════════════╣
║              CACHE EFFECTIVENESS                     ║
╠══════════════════════════════════════════════════════╣
║  Cache cold p50    : 22.5 ms                       ║
║  Cache warm p50    : 15.0 ms                       ║
║  Cache speedup     : 1.5x faster after cache warm-up║
╠══════════════════════════════════════════════════════╣
║              CONCURRENCY ACCURACY                    ║
╠══════════════════════════════════════════════════════╣
║  Total requests    : 100                           ║
║  Tokens allowed    : 50 / 50 capacity              ║
║  Tokens blocked    : 50                            ║
║  Accuracy          : PASS (no over-count)          ║
╠══════════════════════════════════════════════════════╣
║              REDIS FAIL-OPEN RESILIENCE              ║
╠══════════════════════════════════════════════════════╣
║  Fail-open success : 100.00%                       ║
║  Baseline p50      : 5.0 ms (Redis UP)             ║
║  Degraded p50      : 4.0 ms (Redis DOWN)           ║
╠══════════════════════════════════════════════════════╣
║  Overall error rate: 0.00%                         ║
╚══════════════════════════════════════════════════════╝
```

> The degraded p50 (4.0 ms) is faster than the healthy baseline (5.0 ms) because when the circuit breaker opens, the service bypasses the Redis call entirely — the Redis round-trip itself is the dominant cost on the hot path.
