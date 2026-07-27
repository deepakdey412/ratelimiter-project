# Rate Limiter Spring Boot Starter

A production-ready, annotation-driven rate limiting library for Spring Boot applications with pluggable algorithms and key resolution strategies.

## Architecture

**Multi-module Maven project:**

- **rate-limiter-core**: Framework-agnostic rate limiting algorithms (Token Bucket, Sliding Window), annotations, and domain logic. Zero Spring dependencies.
- **rate-limiter-spring-boot-autoconfigure**: Spring Boot auto-configuration, AOP aspect, key resolvers, exception handling, and properties binding.
- **rate-limiter-spring-boot-starter**: Dependency aggregator for end-users. Add this single dependency to enable rate limiting.
- **rate-limiter-demo-app**: Runnable Spring Boot demo application showcasing all features.

## Usage

## Using This Library In Another Project

This is a standalone multi-module Maven library. Any other Spring Boot project on the same machine can depend on it without copying files.

**Project Structure Example:**

```
Desktop/
├── ratelimiter-project/          (this library)
│   ├── rate-limiter-core/
│   ├── rate-limiter-spring-boot-autoconfigure/
│   ├── rate-limiter-spring-boot-starter/
│   ├── rate-limiter-demo-app/
│   └── pom.xml
│
└── order-service/                 (a separate, independent Spring Boot project)
    ├── pom.xml
    └── src/
        └── ...
```

**Steps to use this library in `order-service`:**

1. **Install this library to your local Maven repository** (run once):
   ```bash
   cd ratelimiter-project
   mvn clean install
   ```

2. **Add the starter dependency** to `order-service/pom.xml`:
   ```xml
   <dependency>
       <groupId>com.example.ratelimiter</groupId>
       <artifactId>rate-limiter-spring-boot-starter</artifactId>
       <version>0.1.0-SNAPSHOT</version>
   </dependency>
   ```

3. **Maven resolves it automatically** from `~/.m2/repository` — no manual file copying, no need to place `order-service` inside `ratelimiter-project`.

Multiple independent projects (order-service, user-service, payment-service, etc.) can all reuse this same library just by adding the dependency, without duplicating any code.

**Current Limitation:** This library is installed only to the local `.m2` repository and not published to Maven Central or GitHub Packages. It works only on machines where `mvn install` has been run for this project. To share across machines or teams, publish to a remote Maven repository.

### Add Dependency

```xml
<dependency>
    <groupId>com.example.ratelimiter</groupId>
    <artifactId>rate-limiter-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Annotate Controller Methods

```java
@RestController
public class ApiController {

    // IP-scoped (default): 5 requests per minute per IP
    @RateLimit(limit = 5, window = "1m")
    @GetMapping("/api/data")
    public String getData() {
        return "data";
    }

    // Token Bucket with burst allowance
    @RateLimit(limit = 10, window = "30s", strategy = RateLimitStrategy.TOKEN_BUCKET)
    @GetMapping("/api/burst")
    public String burst() {
        return "burst allowed";
    }

    // Sliding Window (strict, no burst)
    @RateLimit(limit = 10, window = "30s", strategy = RateLimitStrategy.SLIDING_WINDOW)
    @GetMapping("/api/strict")
    public String strict() {
        return "strict enforcement";
    }

    // USER-scoped: different users from same IP get independent limits
    @RateLimit(limit = 20, window = "1m", scope = RateLimitScope.USER)
    @GetMapping("/api/user-data")
    public String userScoped() {
        return "per-user limit";
    }

    // API_KEY-scoped: rate limit by X-API-Key header
    @RateLimit(limit = 100, window = "1m", scope = RateLimitScope.API_KEY)
    @GetMapping("/api/partner")
    public String apiKeyScoped() {
        return "partner API";
    }

    // CUSTOM scope with named resolver bean
    @RateLimit(limit = 50, window = "1h", scope = RateLimitScope.CUSTOM, keyResolverBeanName = "tenantResolver")
    @GetMapping("/api/tenant")
    public String customScoped() {
        return "tenant-specific";
    }
}
```

### @RateLimit Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `limit` | long | (required) | Maximum requests allowed within `window` |
| `window` | String | (required) | Time window: `"30s"`, `"1m"`, `"5m"`, `"1h"` |
| `strategy` | RateLimitStrategy | TOKEN_BUCKET | Algorithm: `TOKEN_BUCKET` (burst) or `SLIDING_WINDOW` (strict) |
| `scope` | RateLimitScope | IP | Key resolution: `IP`, `USER`, `API_KEY`, or `CUSTOM` |
| `keyResolverBeanName` | String | "" | Bean name for CUSTOM scope only |

## Configuration Properties

Add to `application.yml` or `application.properties`:

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `ratelimiter.enabled` | boolean | true | Enable/disable rate limiting globally |
| `ratelimiter.default-strategy` | RateLimitStrategy | TOKEN_BUCKET | Default algorithm if not specified in @RateLimit |
| `ratelimiter.default-scope` | RateLimitScope | IP | Default key resolution scope |
| `ratelimiter.api-key-header` | String | X-API-Key | HTTP header name for API_KEY scope |

**Example:**

```yaml
ratelimiter:
  enabled: true
  default-strategy: TOKEN_BUCKET
  default-scope: IP
  api-key-header: X-API-Key
```

## Build Instructions

### Prerequisites

- Java 17+
- Maven 3.9+ (or use included Maven Wrapper)

### Build All Modules

```bash
# Using Maven Wrapper (Windows)
mvnw.cmd clean install

# Using Maven Wrapper (Unix/Linux/macOS)
./mvnw clean install

# Using system Maven
mvn clean install
```

### Run Tests Only

```bash
mvnw.cmd test
```

### Run Demo Application

**Step 1: Build all modules (from project root)**

```bash
# Windows
mvnw.cmd clean install

# Linux/macOS
./mvnw clean install
```

**Step 2: Start the demo application**

```bash
# Option 1: From project root
mvnw.cmd spring-boot:run -pl rate-limiter-demo-app

# Option 2: From demo-app directory
cd rate-limiter-demo-app
..\mvnw.cmd spring-boot:run
```

Application starts at `http://localhost:8080`

**Verify it's running:**

```bash
curl http://localhost:8080/api/ping
```

Expected response: `{"message":"pong","serverTime":"2026-07-27T..."}`

## Testing with cURL

The demo application (`rate-limiter-demo-app`) provides 6 endpoints to test all rate limiting features. Make sure the app is running on `http://localhost:8080` before testing.

### Available Demo Endpoints

| Endpoint | Strategy | Scope | Limit | Window | Description |
|----------|----------|-------|-------|--------|-------------|
| `/api/ping` | TOKEN_BUCKET | IP | 5 | 1 minute | Default behavior (burst allowed) |
| `/api/ping-strict` | SLIDING_WINDOW | IP | 10 | 30 seconds | Strict sliding window (no burst) |
| `/api/user-limited` | TOKEN_BUCKET | USER | 20 | 1 minute | User-scoped (falls back to IP) |
| `/api/key-limited` | TOKEN_BUCKET | API_KEY | 100 | 1 minute | API key-scoped |
| `/api/user-demo` | TOKEN_BUCKET | USER | 3 | 10 seconds | User scope demo (low limit) |
| `/api/apikey-demo` | TOKEN_BUCKET | API_KEY | 5 | 15 seconds | API key demo (low limit) |

---

### Test 1: Basic IP-Scoped Rate Limiting (TOKEN_BUCKET)

**Endpoint:** `GET /api/ping`  
**Limit:** 5 requests per minute

```bash
# Send 6 requests rapidly - the 6th should be rate limited
for i in 1 2 3 4 5 6; do 
  echo "Request $i:"
  curl -i http://localhost:8080/api/ping
  echo ""
done
```

**Expected Results:**
- Requests 1-5: `HTTP/1.1 200` with JSON response `{"message":"pong","serverTime":"..."}`
- Request 6: `HTTP/1.1 429` with `Retry-After` header and error JSON:
  ```json
  {"error":"Too Many Requests","retryAfterSeconds":12}
  ```

**Wait and retry:**
```bash
# After ~60 seconds, tokens refill and requests work again
sleep 60
curl http://localhost:8080/api/ping  # Should return 200 OK
```

---

### Test 2: Sliding Window (Strict, No Burst)

**Endpoint:** `GET /api/ping-strict`  
**Limit:** 10 requests per 30 seconds (strictly enforced)

```bash
# Send 11 requests - the 11th should fail
for i in {1..11}; do 
  echo "Request $i: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/ping-strict)"
done
```

**Expected Results:**
- Requests 1-10: `200`
- Request 11: `429`

**Difference from Token Bucket:**  
Sliding Window strictly counts requests in any trailing 30-second window. Even if you wait 15 seconds and send 5 more, you can't exceed 10 total in the rolling 30-second period.

---

### Test 3: USER-Scoped Limiting

**Endpoint:** `GET /api/user-demo`  
**Limit:** 3 requests per 10 seconds per user

> **Note:** This demo app doesn't have Spring Security configured, so USER scope falls back to IP. In a real app with authentication, different users from the same IP would get independent limits.

```bash
# Send 4 requests - 4th should be rate limited
for i in 1 2 3 4; do 
  echo "Request $i:"
  curl -i http://localhost:8080/api/user-demo
  echo ""
done
```

**Expected Results:**
- Requests 1-3: `HTTP/1.1 200`
- Request 4: `HTTP/1.1 429`

**Testing with authentication (if you add Spring Security):**
```bash
# User alice gets 3 requests per 10s
curl -u alice:password http://localhost:8080/api/user-demo  # 1
curl -u alice:password http://localhost:8080/api/user-demo  # 2
curl -u alice:password http://localhost:8080/api/user-demo  # 3
curl -u alice:password http://localhost:8080/api/user-demo  # 429 - alice's limit

# User bob from SAME IP gets independent 3 requests per 10s
curl -u bob:password http://localhost:8080/api/user-demo    # 1 (bob's quota)
curl -u bob:password http://localhost:8080/api/user-demo    # 2
curl -u bob:password http://localhost:8080/api/user-demo    # 3
curl -u bob:password http://localhost:8080/api/user-demo    # 429 - bob's limit
```

---

### Test 4: API_KEY-Scoped Limiting

**Endpoint:** `GET /api/apikey-demo`  
**Limit:** 5 requests per 15 seconds per API key

```bash
# Test with API key "client-abc-123"
for i in {1..6}; do 
  echo "Request $i (client-abc-123): $(curl -s -o /dev/null -w '%{http_code}' -H 'X-API-Key: client-abc-123' http://localhost:8080/api/apikey-demo)"
done
```

**Expected Results:**
- Requests 1-5: `200`
- Request 6: `429`

**Test with different API key (independent quota):**
```bash
# Different API key "client-xyz-789" gets its own 5 requests per 15s
for i in {1..6}; do 
  echo "Request $i (client-xyz-789): $(curl -s -o /dev/null -w '%{http_code}' -H 'X-API-Key: client-xyz-789' http://localhost:8080/api/apikey-demo)"
done
```

**Test without API key header (falls back to IP):**
```bash
# Without X-API-Key header, uses IP address as key
curl -i http://localhost:8080/api/apikey-demo
```

---

### Test 5: Verify Retry-After Header

**Endpoint:** Any endpoint  
**Purpose:** Check that 429 responses include `Retry-After` header

```bash
# Hit limit first (5 requests)
for i in {1..5}; do curl -s http://localhost:8080/api/ping > /dev/null; done

# 6th request - check headers
curl -i http://localhost:8080/api/ping
```

**Expected Response:**
```http
HTTP/1.1 429 
Retry-After: 58
Content-Type: application/json
Transfer-Encoding: chunked
Date: Mon, 27 Jul 2026 07:18:45 GMT

{"error":"Too Many Requests","retryAfterSeconds":58}
```

The `Retry-After` header tells clients how many seconds to wait before retrying.

---

### Test 6: Burst vs Strict Enforcement

**Compare Token Bucket vs Sliding Window:**

```bash
# Token Bucket (/api/ping) - allows IMMEDIATE burst of 5
echo "=== Token Bucket (allows burst) ==="
for i in {1..6}; do 
  echo -n "Request $i: "
  curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/ping
done

echo ""
echo "Wait 70 seconds for reset..."
sleep 70

# Sliding Window (/api/ping-strict) - strictly counts in 30s window
echo "=== Sliding Window (strict) ==="
for i in {1..11}; do 
  echo -n "Request $i: "
  curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/ping-strict
done
```

**Key Difference:**
- **Token Bucket:** Allows burst consumption up to limit, then gradually refills
- **Sliding Window:** Rejects any request exceeding limit in trailing window

---

### Test 7: Full Scenario - API Key with High Limit

**Endpoint:** `GET /api/key-limited`  
**Limit:** 100 requests per minute

```bash
# Send 105 requests with API key
echo "Sending 105 requests..."
for i in {1..105}; do 
  response=$(curl -s -o /dev/null -w '%{http_code}' -H 'X-API-Key: partner-key-001' http://localhost:8080/api/key-limited)
  if [ "$response" = "429" ]; then
    echo "Rate limited at request $i"
    break
  fi
done
```

**Expected:** First 100 succeed, 101st returns `429`

---

### Test 8: Concurrent Requests (Stress Test)

**Test thread-safety of rate limiter:**

```bash
# Send 10 requests concurrently (requires GNU Parallel or similar)
# Install: sudo apt-get install parallel  (Linux)
# Or use Windows Subsystem for Linux (WSL)

seq 10 | parallel -j 10 "curl -s -o /dev/null -w 'Request {}: %{http_code}\n' http://localhost:8080/api/ping"
```

Or without parallel (Windows PowerShell):
```powershell
1..10 | ForEach-Object -Parallel {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/ping" -UseBasicParsing
    Write-Host "Request $_: $($response.StatusCode)"
} -ThrottleLimit 10
```

**Expected:** All rate limiting logic remains thread-safe and accurate under concurrent load.

---

### Quick Test Script (Copy-Paste)

Save as `test-ratelimiter.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"

echo "=== Test 1: Basic IP Rate Limiting ==="
for i in {1..6}; do 
  echo -n "Request $i: "
  curl -s -o /dev/null -w '%{http_code}\n' $BASE_URL/api/ping
done

echo ""
echo "=== Test 2: API Key Limiting ==="
for i in {1..6}; do 
  echo -n "Request $i (key-123): "
  curl -s -o /dev/null -w '%{http_code}\n' -H 'X-API-Key: key-123' $BASE_URL/api/apikey-demo
done

echo ""
echo "=== Test 3: Different API Key ==="
for i in {1..6}; do 
  echo -n "Request $i (key-456): "
  curl -s -o /dev/null -w '%{http_code}\n' -H 'X-API-Key: key-456' $BASE_URL/api/apikey-demo
done

echo ""
echo "=== All tests complete ==="
```

Run with: `bash test-ratelimiter.sh`

---

### Troubleshooting

**Issue:** `curl: (7) Failed to connect to localhost port 8080`  
**Solution:** Make sure the demo app is running:
```bash
cd ratelimiter-project
mvnw.cmd spring-boot:run -pl rate-limiter-demo-app
```

**Issue:** All requests return 200, no 429 responses  
**Solution:** Check that `ratelimiter.enabled=true` in `application.yml` (it's enabled by default)

**Issue:** Want to reset rate limits immediately  
**Solution:** Restart the application (rate limits are in-memory):
```bash
# Stop the app (Ctrl+C)
# Start again
mvnw.cmd spring-boot:run -pl rate-limiter-demo-app
```

## Algorithms

### Token Bucket
- Allows burst traffic up to `limit`
- Tokens refill continuously at rate of `limit/window`
- Good for: protecting backends from spikes, lenient rate limiting

### Sliding Window Counter
- Strictly enforces `limit` requests in any trailing `window`
- No burst allowance beyond limit
- Good for: hard quotas, billing-relevant APIs, strict fairness

## Key Resolution Scopes

### IP (Default)
- Limits by client IP address (`request.getRemoteAddr()`)
- Falls back to IP when other scopes unavailable

### USER
- Limits by authenticated username from Spring Security
- Falls back to IP if no authentication present
- Different users from same IP get independent limits

### API_KEY
- Limits by value of configurable header (default: `X-API-Key`)
- Falls back to IP if header absent
- Each API key gets independent quota

### CUSTOM
- Use custom `RateLimitKeyResolver` bean
- Specify bean name in `@RateLimit(keyResolverBeanName = "...")`
- Implement any key extraction logic (tenant ID, customer tier, etc.)

## License

This is a demonstration project.
