
# GitHub Copilot Instructions — Performance Coding Guidelines (Workspace)

> Place this file at `.github/copilot-instructions.md` in the repository. Copilot for JetBrains uses this file to bias suggestions and Copilot Chat answers toward our team’s standards.

## How Copilot should behave
- Follow these rules when generating code, refactors, tests, or docs.
- Prefer time/space-efficient solutions; note algorithmic complexity when relevant.
- If a rule must be relaxed, briefly explain the trade‑off in a comment.
- When asked to *optimize* or *review*, propose diffs and reference affected files.

---

## Profiling
- Use JDK/IDE profilers and flame graphs to identify hotspots before deep refactors.
- Optimize measured bottlenecks; avoid speculative micro‑optimizations.

## Efficiency
- **Parallelize** independent work (e.g., `CompletableFuture`, judicious parallel streams). Avoid parallelism for small workloads.
- Choose **efficient algorithms** (binary search vs. linear scan; indexed lookups vs. scans). Prefer well‑optimized library functions.
- Pick **appropriate data structures**:
  - Random access: `ArrayList` over `LinkedList`.
  - Fast lookups (no ordering): `HashMap` / `ConcurrentHashMap` over `TreeMap`.
- **Minimize synchronization**: keep `synchronized` blocks small; prefer concurrent collections (`ConcurrentHashMap`, `ConcurrentLinkedQueue`).
- **Logging**: avoid excessive logging on hot paths; log only necessary parameters; use appropriate levels.
- **Reduce object churn**: avoid creating new objects in tight loops; reuse buffers/objects; consider pooling only when profiling shows benefit.
- **Prefer primitives** over wrappers where feasible.
- Use **asynchronous processing** for non‑critical work (queue + worker) while returning promptly.
- Always use **database connection pools**.
- Favor **stateless microservices**.
- For I/O‑bound workloads, prefer **reactive** approaches (Spring WebFlux).

## String handling
- Use `StringBuilder` (or `StringBuffer` if thread‑safe) for concatenation, especially in loops.
- Prefer Apache **`StringUtils`** for null‑safe, efficient operations when applicable.
- Avoid `String.format(...)` on hot paths; use simple concatenation or parameterized logging.
- Avoid **regular expressions** for simple checks; use core `String` methods or tokenizers.

## Threads & Concurrency
- Avoid shared mutable state; prefer **immutable objects** (final fields, no setters).
- Use **managed thread pools** and structured concurrency.
  ```java
  ExecutorService executor = Executors.newFixedThreadPool(10);
  CompletableFuture.runAsync(() -> processData(), executor);
  ```
- Scope locks narrowly; prefer high‑level concurrency utilities over manual synchronization.
- Where applicable (Java 21+), consider **virtual threads (Project Loom)** for I/O‑bound concurrency.

## Caching
- Cache read‑heavy or expensive lookups.
- Choose the right layer:
  - **L1 (in‑memory):** Caffeine/Guava/Spring Cache for per‑instance speed.
  - **L2 (distributed):** Redis or managed equivalents for cross‑instance sharing.
- Define **TTL**, size limits, and eviction policy per cache.
- Use **stable, simple keys** (String/Long or explicit key objects). Avoid relying on `toString()`.
- For Spring, prefer `@Cacheable(key = "#user.id")` patterns where appropriate.

## Resilience (SRE)
- Implement **circuit breakers**, **retries with backoff**, and **bulkheads/rate limiters** on remote calls.
- Provide **graceful degradation** with fallbacks when dependencies fail.

## Deployment & JVM
- Keep deployment footprint **minimal**; ship only required libraries.
- **Tune JVM**: heap sizes, GC selection, and GC logging based on profiling/workload.

## Database
- Use **prepared statements**; select only required columns.
- Implement **pagination** for large result sets.
- Align queries with **indexes**; regularly review **query plans**.
- Use **batch operations** for inserts/updates (`saveAll()` / JDBC batch).
- For Couchbase or similar, set appropriate **durability** levels when strong consistency is required.

## Communication
- Prefer **gRPC** for inter‑service calls where latency/throughput matter.
- Enable **payload compression** when beneficial (balance CPU vs. network).
- For HTTP in Spring, prefer **WebClient** over `RestTemplate` for non‑blocking I/O.

## Logging
- Use **parameterized logging** (no string concatenation in logging statements).
- Avoid logging sensitive data; ensure log levels are appropriate and configurable.

## Memory & GC
- Drop references to large, no‑longer‑used objects (`obj = null`) to enable faster GC.
- Avoid retaining large temporary collections beyond their scope.

## When Copilot suggests code
- Favor **clear, efficient** implementations that match these rules.
- Include **complexity notes** for non‑trivial changes.
- Where the rule drives a decision (e.g., caching, pagination), add a short comment explaining the choice.

---

## Quick examples Copilot may follow

**Concurrent processing:**
```java
ExecutorService pool = Executors.newFixedThreadPool(16);
CompletableFuture<Void> f1 = CompletableFuture.runAsync(() -> service.a(), pool);
CompletableFuture<Void> f2 = CompletableFuture.runAsync(() -> service.b(), pool);
CompletableFuture.allOf(f1, f2).join();
```

**Spring WebClient (non‑blocking):**
```java
WebClient client = WebClient.builder()
    .baseUrl("https://api.example.com")
    .build();
Mono<Response> resp = client.get()
    .uri("/resource")
    .retrieve()
    .bodyToMono(Response.class);
```

**Caffeine cache:**
```java
Cache<String, Data> cache = Caffeine.newBuilder()
    .maximumSize(10_000)
    .expireAfterWrite(Duration.ofMinutes(5))
    .build();
```

**Pagination (Spring Data):**
```java
PageRequest page = PageRequest.of(pageNumber, pageSize);
Page<User> users = userRepo.findByStatus(status, page);
```

**Parameterized logging:**
```java
logger.info("Processed {} users in {} ms", count, elapsedMs);
```
