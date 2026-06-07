# Lumen Roadmap

This document tracks planned improvements, new features, and known design gaps. Each item is labelled with the type of release it would require: **patch** (bug fix), **minor** (new feature, backwards compatible), or **major** (breaking change).

Current stable release: **1.0.0**

---

## Version 1.x.x — Patch releases

Small bug fixes and non-breaking improvements that can ship at any time.

| Item | Notes |
|---|---|
| `CompletableFuture<T>` controller return type | `RouteInvoker` should detect `CompletableFuture` return values, call `request.startAsync()`, and write the response on completion — currently the future itself gets serialized as a raw JSON object |
| `ExceptionTranslationFilter` returns 401 for unauthenticated API requests | Currently redirects to `/login` (Spring form-login default), which causes a 500 when no `/login` route is registered — REST APIs should receive a plain 401 response |
| `AsyncProcessor` misses `@Async` on classes already proxied by earlier processors | When a class is wrapped by `CacheProcessor` before `AsyncProcessor` runs, `@Async` annotations are invisible through the ByteBuddy proxy — `AsyncProcessor` must walk the class hierarchy when scanning for `@Async` methods |
| `@CacheEvict` does not support multiple cache names | `value` is a single `String` — evicting two caches in one annotation is impossible; add `@Repeatable` + `@CacheEvicts` container and update `CacheProcessor` to handle it |
| Cache key collision across methods sharing the same cache name | When two `@Cacheable` methods on the same cache use overlapping key expressions (e.g., `findByProject(key="#projectId")` and `findById(key="#id")` both in `"tasks"`), a key value present in both causes a `ClassCastException` on retrieval; Spring avoids this by including the method signature in the default key — Lumen should do the same when an explicit `key` is not provided, or at minimum warn on type mismatch at cache retrieval |
| WebSocket security filter ordering — `WsFilter` runs before `LumenSecurityFilter` | `WsSci` is registered before `LumenServletContainerInitializer`, so `WsFilter` intercepts WS upgrade requests before the JWT filter runs — `SecurityContextHolder` is empty when `modifyHandshake()` is called, leaving `session.getPrincipal()` null; fix requires a three-phase SCI boot: LumenSCI (filters) → WsSci → deferred SCI (WebSocket endpoint registration) using a `DeferredLumenInitializer` marker interface |

---

## Version 1.x — Minor releases

New features that are fully backwards compatible. Existing applications require no changes.

### Security

| Item | Release type | Notes |
|---|---|---|
| OAuth2 / OIDC support | minor | `lumen-boot-starter-oauth2`; `OAuth2LoginFilter`, token introspection, JWT issuer validation |
| Remember-me authentication | minor | Cookie-based persistent login token |
| `@ConditionalOnRole` for `@Scheduled` | minor | Skip scheduled task execution if caller has no authority |
| CSRF protection | minor | `CsrfFilter` + `CsrfTokenRepository`; disable per-route |
| Session fixation protection | minor | Regenerate session ID after authentication |

### Web

| Item | Release type | Notes |
|---|---|---|
| Server-Sent Events (SSE) | minor | `SseEmitter` return type from controllers |
| `@ResponseBody` streaming | minor | `StreamingResponseBody` for large file responses |
| `@ControllerAdvice` for response wrapping | minor | Global response body transformation |
| Content negotiation | minor | Serve JSON or XML based on `Accept` header |
| Multipart improvements | minor | Multiple file upload, size limits via properties |
| HTTP client | minor | `LumenRestClient` — thin wrapper over `HttpClient` for service-to-service calls |

### Data

| Item | Release type | Notes |
|---|---|---|
| More derived query keywords | minor | `Between`, `NotLike`, `IsEmpty`, `MemberOf` |
| `@Query` named parameters | minor | `:paramName` syntax in JPQL in addition to positional `?1` |
| Optimistic locking support | minor | `@Version` field handling in repositories |
| Second-level cache integration | minor | Hibernate L2 cache via `CacheManager` SPI |
| Multiple `DataSource` support | minor | `@Primary` + `@Qualifier` to route repositories to different databases |

### Infrastructure

| Item | Release type | Notes |
|---|---|---|
| Bucket4j rate limiting integration | minor | `lumen-boot-starter-ratelimit`; `@RateLimit` annotation + `RateLimiter` SPI; in-memory default, Bucket4j backend optional |
| Redis cache integration | minor | `lumen-boot-starter-cache-redis`; `RedisCacheManager` implementing `CacheManager` SPI |
| Distributed session store | minor | `lumen-boot-starter-session-redis`; `RedisSecurityContextRepository` |
| Metrics / tracing | minor | `lumen-boot-starter-metrics`; Micrometer integration, expose via actuator |
| `@Retryable` | minor | `lumen-boot-starter-retry`; retry on exception with backoff |
| Health indicator for DataSource | minor | `DataSourceHealthIndicator` — checks DB connectivity in `/actuator/health` |
| Health indicator for mail | minor | `MailHealthIndicator` |
| Conditional annotations | minor | `@ConditionalOnBean`, `@ConditionalOnMissingBean`, `@ConditionalOnExpression` |
| `@Value` for property injection | minor | `@Value("${server.port}")` on fields and constructor params |

### WebSocket

| Item | Release type | Notes |
|---|---|---|
| STOMP over WebSocket | minor | `lumen-boot-starter-messaging`; `@MessageMapping`, in-memory message broker, per-destination security, `SimpMessagingTemplate` |
| WebSocket session registry | minor | `WebSocketSessionRegistry` — list and close active sessions by user |
| Per-message `@PreAuthorize` | minor | Method security on `@MessageMapping` methods (depends on STOMP) |

### Developer experience

| Item | Release type | Notes |
|---|---|---|
| OpenAPI / Swagger UI | minor | `lumen-boot-starter-openapi`; auto-generate spec from controller annotations |
| Dev mode hot reload hint | minor | Detect classpath changes and log a restart suggestion |
| Prettier startup banner | minor | ASCII art + version + active profiles printed on boot |
| `@Profile`-aware `@Scheduled` | minor | Skip scheduled tasks when profile is not active |

---

## Version 2.0 — Major release

Changes that break backwards compatibility or require application code to change.

| Item | Release type | Notes |
|---|---|---|
| Lumen Maven plugin | major | `lumen-boot-maven-plugin` replaces the `fat-jar` profile; `mvn package` produces a runnable JAR by default for application poms |
| Core-first support | major | Allow use without Boot — `LumenModule` impls split into separate boot-level modules; manual container setup without `application.properties` |
| Reactive / non-blocking stack | major | `lumen-boot-starter-webflux`; `Mono` / `Flux` return types, non-blocking I/O via Netty |
| `LumenModule.init()` signature change | major | Add `ModuleContext` parameter to carry more bootstrap state cleanly (currently two params: `container` + `basePackages`) |
| Java version policy | major | Evaluate moving minimum to Java 25 LTS features (records, pattern matching, virtual threads) more aggressively |
| Virtual thread executor for `@Async` | major | Replace fixed thread pool with `Executors.newVirtualThreadPerTaskExecutor()` by default; `lumen.async.virtual-threads=true` |

---

## Demo application (`lumen-demo`)

The demo should grow alongside the framework to showcase each new feature.

| Item | Tracks |
|---|---|
| WebSocket chat room using `HandshakeInterceptor` auth | 1.x |
| Rate limiting on public API endpoints | 1.x (after Bucket4j starter) |
| SSE live feed endpoint | 1.x |
| STOMP messaging example | 1.x (after STOMP) |
| OAuth2 login flow | 1.x (after OAuth2 starter) |
| OpenAPI UI at `/swagger-ui` | 1.x |
| Multi-datasource example | 1.x |

---

## Out of scope (by design)

These will not be added to Lumen regardless of version:

- **Built-in in-memory rate limiting** — single-node rate limiting is misleading in a distributed deployment; use a reverse proxy or Bucket4j with a shared store
- **XML configuration** — annotation-driven only
- **Groovy / Kotlin DSL** — Java only for now
- **Spring compatibility layer** — Lumen is not a drop-in replacement; the API mirrors Spring but is not binary compatible