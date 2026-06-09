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
| — | `lumen-web` | `CorsFilter` joins all configured origins into one comma-separated `Access-Control-Allow-Origin` header value; the CORS spec requires a single origin or `*` — browsers reject multi-value headers; should reflect back only the matched request origin |
| — | `lumen-web` | `CorsConfiguration` defaults to `allowCredentials = true`; if a user sets `allowedOrigins("*")` the filter emits both `Access-Control-Allow-Origin: *` and `Access-Control-Allow-Credentials: true`, which browsers unconditionally reject per the CORS spec |
| — | `lumen-web` | `JsonHttpMessageConverter.getSupportedMediaTypes()` returns `APPLICATION_OCTET_STREAM` instead of `APPLICATION_JSON`; `RestResultHandler` uses this to set the response `Content-Type`, making every JSON response advertise `application/octet-stream` |
| — | `lumen-web` | `ControllerScanner.scanController()` uses `getDeclaredMethods()` only — handler methods (`@GetMapping` etc.) inherited from a superclass controller are never registered as routes |
| — | `lumen-web` | `ControllerAdviceRegistry.registerAdvice()` uses `getDeclaredMethods()` only — `@ExceptionHandler` methods inherited from a parent `@ControllerAdvice` class are never registered |
| — | `lumen-web` | `ObjectBinder.bind()` uses `getDeclaredFields()` only — fields declared in a DTO superclass are never bound from request parameters |
| — | `lumen-web` | `RequestParamArgumentResolver` imports `jdk.dynalink.linker.support.TypeUtilities`, an internal JDK API with no stability guarantee; `ReflectionUtil.wrapperFor()` already provides the same functionality |
| — | `lumen-web` | `PathVariableArgumentResolver` silently passes `null` when a path variable name is not found in the extracted map; primitive parameter types cause NPE on unboxing; object parameters receive `null` with no error |
| — | `lumen-web` | `TypeConverter.convert()` propagates `NumberFormatException` from `Integer.parseInt()` / `Long.parseLong()` as an unhandled exception, resulting in 500 for malformed path variable or request param values that should be 400 |
| — | `lumen-web` | `RestResultHandler` always sends HTTP 200 for non-`ResponseEntity` returns; method-level `@ResponseStatus` annotations (e.g. `@ResponseStatus(CREATED)`) are never checked |
| — | `lumen-web` | `AnnotationWebApplicationContext.registerFilters()` registers servlet filters in light instantiation order without sorting by `@Order`; Tomcat respects registration order, so filters may execute in wrong sequence |
| — | `lumen-web` | `DefaultExceptionResolver.writeJson()` catches all exceptions from `response.getWriter()` with `catch (Exception ignored) {}` — write failures are silently discarded |
| — | `lumen-web` | `GlobalExceptionHandleResolver`: when an `@ExceptionHandler` method itself throws, `resolveRecursively` returns `false` without logging, silently abandoning the original exception |
| — | `lumen-web` | `FlashMapManager.save()` calls `request.getSession()` (no argument), which creates an HTTP session if none exists; REST endpoints that use flash attributes unintentionally become stateful |
| — | `lumen-web` | `RouteRegistry.routes` is an `ArrayList`; concurrent calls to `register()` during parallel context initialization race on the list |
| — | `lumen-web` | `ControllerScanner.scanControllers(Collection<LightInstance>, RouteRegistry)` is dead code — route scanning is done entirely through `ControllerProcessor.afterInstantiation()` |
| — | `lumen-data` | `DynamicQueryExecutor.deriveCountQuery()` uses a non-greedy regex replace that stops at the first `FROM` keyword; JPQL with subqueries in the `SELECT` clause produces a malformed count query, breaking paged results |
| — | `lumen-data` | `DynamicQueryExecutor.executePaged()` casts `pageable.getOffset()` (a `long`) to `int` via `setFirstResult((int) pageable.getOffset())`; extreme page/size combinations silently truncate the offset |
| — | `lumen-data` | `CrudRepositoryExecutor.deleteAll(Iterable)` issues one `DELETE` per entity rather than a single bulk `DELETE … WHERE id IN (…)`; large collections cause N database round trips |
| — | `lumen-data` | `RepositoryFactory.resolveEntityClass()` only iterates `repoInterface.getGenericInterfaces()` — custom intermediate repository interfaces (e.g. `BaseRepo<T> extends JpaRepository<T, Long>`) cause entity class resolution to fail at startup with `IllegalArgumentException` |
| — | `lumen-data` | `JpaTransactionManager.applyIsolation()` calls `Connection.setTransactionIsolation()` which permanently modifies the JDBC connection; when the connection is returned to the pool it retains the non-default isolation level for future transactions |
| — | `lumen-data` | `LumenDataModule.configureJpa()` forwards `lumen.jpa.ddl-auto` to Hibernate without validation; an invalid or dangerous value (e.g. `drop-and-create`) fails silently or destroys schema with no framework-level warning |
| — | `lumen-data` | `NameResolvingQueryParser.validatePropertyPath()` resolves segments using `getDeclaredField()` on the field's declared type; Hibernate proxy or `@Embedded` association types cause the lookup to fail even when the JPQL path is valid |
| — | `lumen-data` | `ObjectBinder.classFieldCache` is a JVM-static `ConcurrentHashMap`; in hot-reload or custom classloader scenarios, stale `Field[]` from the old class version remain cached and references to them fail or reflect incorrect state |


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
| Method-level `@RequestMapping` support | minor | `@RequestMapping(method=GET, path="/foo")` on handler methods is currently silently ignored; only the shortcut annotations (`@GetMapping` etc.) dispatch to routes — add method-level `@RequestMapping` handling in `ControllerScanner` |
| Server-Sent Events (SSE) | minor | `SseEmitter` return type from controllers |
| `@ResponseBody` streaming | minor | `StreamingResponseBody` for large file responses |
| `@ControllerAdvice` for response wrapping | minor | Global response body transformation |
| Content negotiation | minor | Serve JSON or XML based on `Accept` header |
| Multipart improvements | minor | Multiple file upload, size limits via properties |
| HTTP client | minor | `LumenRestClient` — thin wrapper over `HttpClient` for service-to-service calls |

### Data

| Item | Release type | Notes |
|---|---|---|
| Efficient `existsBy*` derived query | minor | Currently generates `SELECT COUNT(e) … > 0`; replace with a short-circuit existence check (e.g. `SELECT 1 … LIMIT 1`) to avoid full-table counting on large datasets |
| `@Transactional` rollback-on-checked-exception option | minor | Checked exceptions commit by default (matches Spring), which is a footgun; add a `rollbackForChecked` attribute or document the behaviour clearly on the annotation |
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