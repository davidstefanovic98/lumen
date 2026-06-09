# Lumen Roadmap

This document tracks planned improvements, new features, and known design gaps. Each item is labelled with the type of release it would require: **patch** (bug fix), **minor** (new feature, backwards compatible), or **major** (breaking change).

Current stable release: **1.0.2**

---

## Version 1.x.x — Patch releases

Small bug fixes and non-breaking improvements that can ship at any time.

| # | Module | Description |
|---|---|---|
| — | `lumen-cache` | `CacheInterceptor.isCompatibleReturnType()` always returns `true` for primitive return types — a key collision storing e.g. a `String` under the same key as a `long`-returning method bypasses the mismatch check and causes a `ClassCastException` at the call site |
| — | `lumen-data` | `TransactionalProcessor.needsProxy()` uses `getDeclaredMethods()` only — misses `@Transactional` inherited from a superclass, so subclasses are not proxied |
| — | `lumen-web` | `DispatcherServlet.matchesMediaType()` does not handle `*/*` or `type/*` wildcards in the `Accept` header — every browser request to a `produces = "application/json"` endpoint returns 406 |
| — | `lumen-core` | Add `ReflectionUtil.wrapperFor(Class<?>)` shared utility mapping primitive types to their wrapper types; needed by the cache fix and avoids repeating the mapping elsewhere |
| — | `lumen-security` | `LumenSecurityModule` registers `MethodSecurityProcessor` with default proxy order `0`; CLAUDE.md and the post-processor design both require `-1` so security is outermost and cache hits still enforce `@PreAuthorize` |
| — | `lumen-security` | `DefaultLoginPageGeneratingFilter` reads path with `request.getPathInfo()` which is `null` in embedded Tomcat (path lives in `getServletPath()`); the login page is never served |
| — | `lumen-security` | `UsernamePasswordAuthenticationFilter.requiresAuthentication()` compares `loginUrl` against `getRequestURI()` which includes the servlet context path; form login silently breaks when deployed with a non-empty context path |
| — | `lumen-security` | `DaoAuthenticationProvider.authenticate()` does not guard against null credentials; a missing `password` form field causes `NullPointerException` inside `BCryptPasswordEncoder.matches()` instead of `BadCredentialsException` |
| — | `lumen-security` | `LogoutFilter.requiresLogout()` checks only the URI, not the HTTP method; a `GET /logout` (browser prefetch, linked image, attacker page) logs out the current user |
| — | `lumen-security` | `MethodSecurityExpressionEvaluator.buildContext()` skips binding `returnObject` when the return value is `null`; `@PostAuthorize` expressions that reference `#returnObject` throw a confusing evaluation error instead of `AccessDeniedException` |
| — | `lumen-security` | `HttpSecurity.CONTRIBUTORS` is a JVM-global static list; contributors accumulate across test cases (without explicit `clearContributors()`) and across multiple `build()` calls |
| — | `lumen-security` | `RegexRequestMatcher` resolves path via `getPathInfo()` then `getServletPath()`; `AntPathRequestMatcher` does the opposite — the same URL can resolve to different values depending on which matcher is used |
| — | `lumen-security` | `UsernamePasswordAuthenticationFilter.onSuccessfulAuthentication()` writes directly to the session using a hardcoded string key instead of delegating to `HttpSessionSecurityContextRepository` |
| — | `lumen-security` | `InMemoryUserDetailsManager.users` is a plain `HashMap`; concurrent reads during request handling alongside a write during late initialisation are not safe |
| — | `lumen-security` | `SecurityContextTaskDecorator`: `SecurityContextHolder.getContext()` never returns `null`, so the `else` branch in the `finally` block (`SecurityContextHolder.clear()`) is unreachable dead code |
| — | `lumen-security` | `BCryptPasswordEncoder` does not validate that `logRounds` is in the BCrypt-valid range (4–31) at construction time; invalid values produce an `IllegalArgumentException` at first `encode()` call |

---

## Version 1.x — Minor releases

New features that are fully backwards compatible. Existing applications require no changes.

### Security

| Item | Release type | Notes |
|---|---|---|
| `hasAuthority()` in `AuthorizeRequestBuilder` | minor | HTTP filter layer can only express `hasRole()` (auto-prefixes `ROLE_`); add `hasAuthority()` / `hasAnyAuthority()` for exact-match authority rules (e.g. `SCOPE_read`) |
| Deprecate `MethodSecurityExpressionEvaluator.check(String)` | minor | The no-arg-context overload is unused internal dead code exposed as `public` API; deprecate in a minor release and remove in the next major |
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