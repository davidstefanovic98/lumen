# Lumen Roadmap

This document tracks planned improvements, new features, and known design gaps. Each item is labelled with the type of release it would require: **patch** (bug fix), **minor** (new feature, backwards compatible), or **major** (breaking change).

Current stable release: **1.0.2**

---

## Version 1.x.x — Patch releases

Small bug fixes and non-breaking improvements that can ship at any time.

| # | Module | Description                                                                                                                                                                                                                                                                    |
|---|---|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| — | `lumen-security` | ~~`LumenSecurityModule` registers `MethodSecurityProcessor` with default proxy order `0`; CLAUDE.md and the post-processor design both require `-1` so security is outermost and cache hits still enforce `@PreAuthorize`~~                                                    |
| — | `lumen-security` | ~~`DefaultLoginPageGeneratingFilter` reads path with `request.getPathInfo()` which is `null` in embedded Tomcat (path lives in `getServletPath()`); the login page is never served~~ *(fixed in #35)*                                                                          |
| — | `lumen-security` | ~~`UsernamePasswordAuthenticationFilter.requiresAuthentication()` compares `loginUrl` against `getRequestURI()` which includes the servlet context path; form login silently breaks when deployed with a non-empty context path~~                                              |
| — | `lumen-security` | ~~`DaoAuthenticationProvider.authenticate()` does not guard against null credentials; a missing `password` form field causes `NullPointerException` inside `BCryptPasswordEncoder.matches()` instead of `BadCredentialsException`~~                                              |
| — | `lumen-security` | ~~`LogoutFilter.requiresLogout()` checks only the URI, not the HTTP method; a `GET /logout` (browser prefetch, linked image, attacker page) logs out the current user~~                                                                                                          |
| — | `lumen-security` | ~~`MethodSecurityExpressionEvaluator.buildContext()` skips binding `returnObject` when the return value is `null`; `@PostAuthorize` expressions that reference `#returnObject` throw a confusing evaluation error instead of `AccessDeniedException`~~                           |
| — | `lumen-security` | ~~`HttpSecurity.CONTRIBUTORS` is a JVM-global static list; contributors accumulate across test cases (without explicit `clearContributors()`) and across multiple `build()` calls~~                                                                                                |
| — | `lumen-security` | ~~`RegexRequestMatcher` resolves path via `getPathInfo()` then `getServletPath()`; `AntPathRequestMatcher` does the opposite — the same URL can resolve to different values depending on which matcher is used~~ *(refined 2026-09-22: unifying both onto `RequestPaths.resolve()` had itself picked the wrong priority order — servletPath-first only looks correct for Lumen's own path-mapped `DispatcherServlet` where `pathInfo` is always `null`; for a prefix-mapped servlet (`/api/*`) it silently matches against the mapping prefix instead of the real sub-path. `RequestPaths.resolve()` now prefers `pathInfo` first, matching what `LogoutFilter`/`UsernamePasswordAuthenticationFilter`/`DefaultLoginPageGeneratingFilter` already did correctly on their own — see the three rows below)* |
| — | `lumen-security` | ~~`UsernamePasswordAuthenticationFilter.onSuccessfulAuthentication()` writes directly to the session using a hardcoded string key instead of delegating to `HttpSessionSecurityContextRepository`~~                                                                                |
| — | `lumen-security` | ~~`InMemoryUserDetailsManager.users` is a plain `HashMap`; concurrent reads during request handling alongside a write during late initialisation are not safe~~                                                                                                                    |
| — | `lumen-security` | ~~`SecurityContextTaskDecorator`: `SecurityContextHolder.getContext()` never returns `null`, so the `else` branch in the `finally` block (`SecurityContextHolder.clear()`) is unreachable dead code~~                                                                              |
| — | `lumen-security` | ~~`BCryptPasswordEncoder` does not validate that `logRounds` is in the BCrypt-valid range (4–31) at construction time; invalid values produce an `IllegalArgumentException` at first `encode()` call~~                                                                             |
| — | `lumen-security` | ~~`AuthorizeRequestBuilder.AntMatcherConfig`/`RegexMatcherConfig.hasRole()` stored the raw role string with no `"ROLE_"` prefix, while `MethodSecurityExpressionEvaluator`'s `hasRole()`/`hasAnyRole()` Gleam functions normalized it; an authority stored as `"ROLE_ADMIN"` (the conventional form `SimpleGrantedAuthority` expects) never satisfied `.antMatchers(...).hasRole("ADMIN")` at the HTTP layer — a silent authorization lockout, found during a 2026-09-22 architecture sweep, not by a user report~~ *(fixed — both layers now share `RoleAuthorities.normalize()`)* |
| — | `lumen-security` | ~~`LogoutFilter.requiresLogout()`, `UsernamePasswordAuthenticationFilter.requiresAuthentication()`, and `DefaultLoginPageGeneratingFilter` each still resolved the request path inline instead of through `RequestPaths.resolve()` — the exact bug class already fixed once for the request matchers (see above), just never swept to these three call sites~~ *(fixed — all three now delegate to `RequestPaths.resolve()`, which also needed its own priority-order fix — see above)* |
| — | `lumen-websocket` | ~~`OriginValidatingConfigurator.checkOrigin()` returned `true` unconditionally when no `allowedOrigins` were configured, instead of the same-origin check the code comment and CLAUDE.md both claimed — a security bug (accepts cross-origin WebSocket connections by default), found during a 2026-09-22 architecture sweep. `checkOrigin(String)` only ever receives the `Origin` header, with no access to the request's own host, and the JSR-356 container default (Tomcat's `DefaultServerEndpointConfigurator`) doesn't enforce same-origin either — it unconditionally returns `true` too, verified by decompiling it~~ *(fixed — new `WebSocketOriginCaptureFilter` captures the request's own origin into a thread-local before Tomcat's `WsFilter` performs the handshake (verified empirically with a real embedded Tomcat that Lumen's own filters run first, matching the SCI registration order `AnnotationWebApplicationContext.startWebServer()` already uses), and `checkOrigin()` now does a genuine same-origin comparison against it, failing closed if the filter didn't run)* |
| — | `lumen-web` | ~~`CorsFilter` joins all configured origins into one comma-separated `Access-Control-Allow-Origin` header value; the CORS spec requires a single origin or `*` — browsers reject multi-value headers; should reflect back only the matched request origin~~                        |
| — | `lumen-web` | ~~`CorsConfiguration` defaults to `allowCredentials = true`; if a user sets `allowedOrigins("*")` the filter emits both `Access-Control-Allow-Origin: *` and `Access-Control-Allow-Credentials: true`, which browsers unconditionally reject per the CORS spec~~                   |
| — | `lumen-web` | ~~`JsonHttpMessageConverter.getSupportedMediaTypes()` returns `APPLICATION_OCTET_STREAM` instead of `APPLICATION_JSON`; `RestResultHandler` uses this to set the response `Content-Type`, making every JSON response advertise `application/octet-stream`~~                        |
| — | `lumen-web` | ~~`ControllerScanner.scanController()` uses `getDeclaredMethods()` only — handler methods (`@GetMapping` etc.) inherited from a superclass controller are never registered as routes~~                                                                                             |
| — | `lumen-web` | ~~`ControllerAdviceRegistry.registerAdvice()` uses `getDeclaredMethods()` only — `@ExceptionHandler` methods inherited from a parent `@ControllerAdvice` class are never registered~~                                                                                              |
| — | `lumen-web` | ~~`ObjectBinder.bind()` uses `getDeclaredFields()` only — fields declared in a DTO superclass are never bound from request parameters~~                                                                                                                                            |
| — | `lumen-web` | ~~`RequestParamArgumentResolver` imports `jdk.dynalink.linker.support.TypeUtilities`, an internal JDK API with no stability guarantee; `ReflectionUtil.wrapperFor()` already provides the same functionality~~                                                                     |
| — | `lumen-web` | ~~`PathVariableArgumentResolver` silently passes `null` when a path variable name is not found in the extracted map; primitive parameter types cause NPE on unboxing; object parameters receive `null` with no error~~                                                             |
| — | `lumen-web` | ~~`TypeConverter.convert()` propagates `NumberFormatException` from `Integer.parseInt()` / `Long.parseLong()` as an unhandled exception, resulting in 500 for malformed path variable or request param values that should be 400~~                                                 |
| — | `lumen-web` | ~~`RestResultHandler` always sends HTTP 200 for non-`ResponseEntity` returns; method-level `@ResponseStatus` annotations (e.g. `@ResponseStatus(CREATED)`) are never checked~~                                                                                                     |
| — | `lumen-web` | ~~`AnnotationWebApplicationContext.registerFilters()` registers servlet filters in light instantiation order without sorting by `@Order`; Tomcat respects registration order, so filters may execute in wrong sequence~~                                                           |
| — | `lumen-web` | ~~`DefaultExceptionResolver.writeJson()` catches all exceptions from `response.getWriter()` with `catch (Exception ignored) {}` — write failures are silently discarded~~                                                                                                          |
| — | `lumen-web` | ~~`GlobalExceptionHandleResolver`: when an `@ExceptionHandler` method itself throws, `resolveRecursively` returns `false` without logging, silently abandoning the original exception~~                                                                                            |
| — | `lumen-web` | ~~`FlashMapManager.save()` calls `request.getSession()` (no argument), which creates an HTTP session if none exists; REST endpoints that use flash attributes unintentionally become stateful~~ *(does not reproduce: `save()` is only reachable from `ViewResultHandler`, which `RestResultHandler`/`route.isRest()` excludes entirely, and it already skips session creation when there are no attributes to flash — investigated 2026-09-21, no code change)*                                                                                    |
| — | `lumen-web` | ~~`RouteRegistry.routes` is an `ArrayList`; concurrent calls to `register()` during parallel context initialization race on the list~~ *(hardened ahead of need — see "Hot reload" below)*                                                                                                                                             |
| — | `lumen-web` | ~~`ControllerScanner.scanControllers(Collection<LightInstance>, RouteRegistry)` is dead code — route scanning is done entirely through `ControllerProcessor.afterInstantiation()`~~                                                                                                |
| — | `lumen-data` | ~~`DynamicQueryExecutor.deriveCountQuery()` uses a non-greedy regex replace that stops at the first `FROM` keyword; JPQL with subqueries in the `SELECT` clause produces a malformed count query, breaking paged results~~ *(fixed — logic extracted to `CountQueryDeriver`, which finds the query's own top-level `FROM` by tracking paren depth/string literals instead of a reluctant regex match)* |
| — | `lumen-data` | ~~`DynamicQueryExecutor.executePaged()` casts `pageable.getOffset()` (a `long`) to `int` via `setFirstResult((int) pageable.getOffset())`; extreme page/size combinations silently truncate the offset~~ *(fixed — `Query.setFirstResult(int)` is the JPA API itself, so there's no wider type to switch to; `AbstractRepositoryExecutor.firstResultOf()` now fails fast with a clear `IllegalArgumentException` instead of silently wrapping, applied consistently across `DynamicQueryExecutor`, `PagingAndSortingExecutor`, and `SpecificationExecutor`)* |
| — | `lumen-data` | ~~`CrudRepositoryExecutor.deleteAll(Iterable)` issues one `DELETE` per entity rather than a single bulk `DELETE … WHERE id IN (…)`; large collections cause N database round trips~~ *(investigated 2026-09-22: this per-entity loop is correct, Spring-parity behaviour — it preserves cascade/lifecycle callbacks, and `JpaRepository` already declares a separate `deleteAllInBatch(Iterable)` for the bulk case. The real bug was that `JpaRepositoryExecutor.deleteAllInBatch(Iterable)` was implemented as the same per-entity loop instead of an actual bulk delete — fixed there instead: it now resolves each entity's id via `getPersistenceUnitUtil().getIdentifier()` and the id attribute name via the JPA metamodel, then issues a single `DELETE ... WHERE id IN (:ids)`, bypassing the persistence context entirely as its name promises)* |
| — | `lumen-data` | ~~`RepositoryFactory.resolveEntityClass()` only iterates `repoInterface.getGenericInterfaces()` — custom intermediate repository interfaces (e.g. `BaseRepo<T> extends JpaRepository<T, Long>`) cause entity class resolution to fail at startup with `IllegalArgumentException`~~ *(fixed — `resolveEntityClass()` now walks the full interface hierarchy with proper type-variable substitution, so it also resolves when an intermediate interface binds the entity concretely and is then extended raw one or more levels down, e.g. `interface UserOps extends BaseRepo<User> {}` then `interface UserRepository extends UserOps {}` — the exact `BaseRepo<T> extends JpaRepository<T, Long>` example above actually already resolved fine before this fix; the real failure needed a non-generic pass-through interface like `UserOps`)* |
| — | `lumen-data` | ~~`JpaTransactionManager.applyIsolation()` calls `Connection.setTransactionIsolation()` which permanently modifies the JDBC connection; when the connection is returned to the pool it retains the non-default isolation level for future transactions~~ *(fixed — `applyIsolation()` now captures the connection's isolation level before changing it and restores it in `commit()`/`rollback()` before the `EntityManager` closes. Investigated 2026-09-22: this only reproduced with Hibernate's own built-in "not for production use" connection handling — HikariCP, the documented/shipped path via `lumen-boot-starter-data-jpa`, already resets a changed isolation level on its own when the connection is returned. Fixed anyway at the framework level rather than relying on the pool implementation, matching how Spring's own transaction managers handle this, and it also fixes the non-Hikari fallback)* |
| — | `lumen-data` | ~~`LumenDataModule.configureJpa()` forwards `lumen.jpa.ddl-auto` to Hibernate without validation; an invalid or dangerous value (e.g. `drop-and-create`) fails silently or destroys schema with no framework-level warning~~ *(fixed — `LumenDataModule.validateDdlAuto()` checks the value against Hibernate's known set (`none`/`validate`/`update`/`create`/`create-drop`) before `configureJpa()` builds the `EntityManagerFactory`, throwing `InvalidDdlAutoException` for anything else. A new `InvalidDdlAutoFailureAnalyzer` gives it a clean startup banner naming the valid options, following the same `StartupFailureAnalyzer` pattern as `PropertyResolveFailureAnalyzer`. The property remaining unset is unaffected — it still defaults to `none`)* |
| — | `lumen-data` | ~~`NameResolvingQueryParser.validatePropertyPath()` resolves segments using `getDeclaredField()` on the field's declared type; Hibernate proxy or `@Embedded` association types cause the lookup to fail even when the JPQL path is valid~~ *(investigated 2026-09-22: `@Embedded` paths already resolved correctly — this check runs on raw declared classes, not runtime Hibernate proxies, so "Hibernate proxy" didn't apply either. The real, reproducible gap was collection-valued to-many associations (e.g. `findByItems_Name`), which threw the generic "no property path" error even though the property genuinely exists. Fixed the diagnostic: `validatePropertyPath()` now recognizes a `Collection`-typed segment and explains that derived query names can't navigate through a to-many association yet, instead of claiming the property doesn't exist. Actually making `findByItems_Name` work end-to-end needs implicit JOIN generation, which is a real feature — see "Implicit joins for collection-valued derived query properties" under Data/minor below)* |
| — | `lumen-web` | `ObjectBinder.classFieldCache` is a JVM-static `ConcurrentHashMap`; in hot-reload or custom classloader scenarios, stale `Field[]` from the old class version remain cached and references to them fail or reflect incorrect state *(mislabeled as `lumen-data` above — the file is in `lumen-web`, corrected here. Investigated 2026-09-22: doesn't currently reproduce as a leak — Lumen has no hot-reload/custom-classloader story yet (see "Core-first support" and "Hot reload" below), so this cache is bounded by the app's fixed set of DTO classes for the process lifetime, not a growing leak. A real fix would trade `bind()`'s lock-free hot per-request path for `WeakHashMap`-style reload-safety, unlike `RouteRegistry`'s `CopyOnWriteArrayList` hardening which had no such cost — not worth it until hot reload actually lands)* |

---

## Architecture Refactor (repo-wide)

**Status: scheduled, not yet started.** This is a deliberate step back from working the patch table item-by-item: a 2026-09-22 review swept every module for the same root smell — **the same idea implemented independently in multiple places**, the exact "divergent implementations" pattern this document's own Engineering Standards section already names — rather than just the specific bugs already listed above. It started with `lumen-data`/`lumen-web`, then extended to every other module once the pattern kept recurring.

Verdict per module: `lumen-data`, `lumen-web`, `lumen-core`, `lumen-context`, `lumen-aop`, `lumen-validation`, and `lumen-cache` all have real findings below. `lumen-gleam`, `lumen-migration`, `lumen-async`, `lumen-mail`, `lumen-web-mvc`, `lumen-boot`, `lumen-boot-thymeleaf`, `lumen-boot-flyway`, and `lumen-boot-actuator` were reviewed and found genuinely solid — no phase-worthy findings, nothing listed for them below. `lumen-security` was reviewed given its unusually deep patch history (see the patch table above) specifically to check whether many individual point-fixes had left sibling code unswept — two genuine live bugs were found and fixed immediately as patches (see above) rather than deferred here; what's left below is pure architecture, not correctness.

This won't be executed as one big-bang branch; it's tracked here so it can be picked up a phase at a time as capacity allows. In the meantime, **new work in any module is held to the standard this plan implies** even before the refactor itself starts — no new hand-rolled JPQL/JSON templates, no new independent reflection-based "is optional module X present" checks, no new divergent copies of logic that already exists once elsewhere. Architecture and code quality come first; ROADMAP items are secondary to getting the underlying design right.

### Phase 1 — Consolidate duplicated logic

| Module | Item |
|---|---|
| `lumen-data` | The JPQL template `"... FROM " + entityClass.getSimpleName() + " e"` is hand-built independently ~9 times across `CrudRepositoryExecutor`, `JpaRepositoryExecutor`, `PagingAndSortingExecutor`, and `NameResolvingQueryParser` — every copy shares the same latent bug (`getSimpleName()` silently ignores `@Entity(name = "Custom")`). Extract one shared entity-name/template helper, resolved via the JPA metamodel, used everywhere. |
| `lumen-web` | Error-response JSON is hand-built independently in `DefaultExceptionResolver`, `MediaTypeExceptionResolver`, `NotFoundExceptionResolver` (a byte-for-byte duplicate of `MediaTypeExceptionResolver`'s copy), and `ValidationExceptionResolver` — 3 of the 4 don't escape the interpolated message, so a `"` in an exception message breaks the response body. Route all of them through the module's own existing `JsonHttpMessageConverter`/`HttpMessageConverterRegistry` instead of string concatenation. |
| `lumen-web` | `GlobalExceptionHandleResolver.handleResult()` reimplements a cruder subset of `RestResultHandler`'s content-negotiation logic for `@ExceptionHandler` methods returning `ResponseEntity` (no Accept-header negotiation, hardcoded String-vs-JSON branch) — the same return type gets a measurably different HTTP response depending on which path produced it. Delegate to `RestResultHandler`/`RouteResultHandler` instead of a second response-writing implementation. |
| cross-cutting (`lumen-core`, `lumen-context`, `lumen-web`, `lumen-gleam`) | String→primitive type coercion is independently reimplemented at least 5 times, each with different type coverage and error handling: `Environment.getProperty(key, Class)`, `ConfigurationPropertiesProcessor.convert()`, `PropertyDependencyProvider.coerce()`, `TypeConverter.convert()`, and `PropertyAccessor.coerce()`. Originates in `lumen-core` itself and radiates outward. Extract one `TypeCoercion` utility in `lumen-core`, everything else delegates to it. |

### Phase 2 — Dispatch & extension-point architecture

| Module | Item |
|---|---|
| `lumen-data` | `RepositoryFragment` dispatch (`JpaRepositoryInterceptor`'s hardcoded 5-fragment list, each with its own `canHandle()`) has no exhaustiveness guarantee — `CrudRepositoryExecutor` and `SpecificationExecutor` silently agree not to collide on `delete`/`count`/`findAll` with nothing enforcing it; a gap surfaces as `UnsupportedOperationException` at invocation time, not startup. Build a signature-keyed dispatch table from the repository interface's own declared methods, failing fast at startup if any method has zero or more than one handler. |
| `lumen-web` | "Is optional module X present, then call into it" is reinvented independently 4+ times: `RequestBodyArgumentResolver`, `ValidationExceptionResolver`, `CompositeExceptionResolver.isValidationPresent()` (three separate validation-presence checks), and `DispatcherServlet.service()`'s inline cause-chain walk comparing a class name against the literal string `"io.lumen.security.exception.AccessDeniedException"`. Introduce one shared helper for the reflection case, and a real typed mechanism (not string comparison) for the security integration. |
| `lumen-web` | `InvocationTargetException` unwrapping is duplicated a 3rd and 4th time (`RequestBodyArgumentResolver.validateAndThrow()`, `GlobalExceptionHandleResolver.resolveRecursively()`) beyond the two this document already documents as intentional (`ByteBuddyLazyInterceptor`, `RouteInvoker.invokeAndWrite()`) — each copy unwraps/rewraps slightly differently. Extract one `ReflectionUtil.invokeUnwrapped(...)` in `lumen-core`, used everywhere instead of copy-pasted per call site. |
| `lumen-validation` | No `ConstraintValidator`-style SPI — `DefaultValidator.validateField()` is one 85-line method with a hardcoded `if (field.isAnnotationPresent(X.class))` branch per constraint type. Adding a new constraint means editing this one method; third-party/user-defined constraints are impossible. Introduce a `ConstraintValidator<A extends Annotation, T>` interface + a registry keyed by annotation type, each existing branch becoming its own small validator class. |
| `lumen-cache` | `CacheInterceptor.handleCaching()` (for `@Caching`) re-inlines the get-cache/resolve-key/check-cached/store sequence that `handleCacheable()`, `doEvict()`, and `handleCachePut()` already implement standalone, instead of calling them. Also, `CacheKeyGenerator` is a declared SPI interface that's effectively dead — `CacheInterceptor` is typed against the concrete `SimpleKeyGenerator`, so there's no way for a user to actually substitute their own key-generation strategy despite the interface suggesting they can. Make `handleCaching()` delegate instead of duplicate; either wire `CacheKeyGenerator` up as a real extension point or remove it. |

### Phase 3 — Unify divergent execution paths

| Module | Item |
|---|---|
| `lumen-data` | `AbstractRepositoryExecutor.inTransaction()` hand-rolls its own begin/commit/rollback + join-if-active logic — a shadow reimplementation of `JpaTransactionManager`'s `REQUIRED` case, without isolation levels, `readOnly` flush mode, or rollback classification, and without going through `LumenTransactionManager` at all. Any future transaction-boundary fix (e.g. the isolation-restore fix already shipped) only touches one of the two paths today. Route `inTransaction()` through `LumenTransactionManager` instead. |
| `lumen-web` | Sync (`DispatcherServlet.service()`) and async (`RouteInvoker.handleAsync()`) exception handling are two separate, hand-synced code paths with different fallback behavior — the async path does a raw `setStatus()` with no converter involved at all. Unify into one path both call. |

### Phase 4 — Hygiene

| Module | Item |
|---|---|
| `lumen-data` | Inconsistent optional-dependency style: Hibernate is handled via a clean compile-time import guarded by one `Class.forName` check; HikariCP is handled via 7 individual reflective setter calls in `LumenDatasourceModule.tryBuildHikariDataSource()`. Align both on the Hibernate pattern — `lumen-boot-thymeleaf`'s `Class.forName`-guarded optional-dependency check is a clean reference example of the pattern done consistently. |
| `lumen-data` | No shared exception hierarchy — `PropertyResolveException`, `InvalidDdlAutoException`, `IllegalTransactionStateException` each extend `RuntimeException` directly with no common base, unlike `lumen-security`'s documented exception hierarchy. |
| `lumen-data` | Package placement inconsistency — `RepositoryFactory`, `LumenDataModule`, `LumenDatasourceModule`, `EntityManagerFactoryDisposable` sit in the `io.lumen.data` package root while everything else is cleanly sub-packaged (`repository/`, `query/`, `transaction/`, ...). |
| `lumen-web` | Dead code: `PathMatcher.couldShadowLiteral()` is never called anywhere in the module — the same class of issue as the already-fixed `ControllerScanner.scanControllers()` dead code, just not caught that time. |
| `lumen-web` | `Route` is a mutable, half-built value object — constructed with 4 required fields, then `setRest`/`setConsumes`/`setProduces` called later by `ControllerScanner`, with no validation that they were actually called before the route is registered. Move to a builder or make it fully immutable. |
| `lumen-core` | `LightContainer` has 4 independent copies of the same "find lights assignable to a type" loop (`getLight`, `hasLight`, `doGetLightByType`, `doGetLightsByType`), each with slightly different pre/post logic. Consolidate into one private `findByType(...)` with the four public methods as thin wrappers. |
| `lumen-core` | `DefaultLightCreator.create()` wraps instantiation/injection failures in `LightInstantiationException`; `createPrototype()` — same body, same injection steps — does not, so a prototype-scoped light's constructor failure propagates unwrapped while a singleton's doesn't, with no stated reason for the difference. Extract the shared instantiate+inject sequence into one method both call. |
| `lumen-core` | `FieldInjectionStep` uses the classloader-safe `ReflectionUtil.hasAnnotationByName()` for `@Inject` detection (documented as necessary to avoid `ClassCastException` under `exec:exec`'s split classloaders); `SetterInjectionStep`, in the same instantiation pipeline, uses the direct `method.isAnnotationPresent(Inject.class)` check that helper exists specifically to replace. Align both on the same detection strategy. |
| `lumen-context` | `AnnotationLightAnalyzer.analyzeFactory()` and `.analyzeClass()` are near-duplicates — both extract the same five annotations from an `AnnotatedElement`, verbatim except `Executable` vs `Class`. Collapse into one method taking `AnnotatedElement`. |
| `lumen-context` | `PackageScanner.scanDirectory()`/`scanJar()` silently swallow `ClassNotFoundException` with only a comment, no logging — unlike the rest of the codebase's stated preference for logging instead of swallowing (see `lumen-web`'s exception-handling-paths precedent). Add a `debug`/`warn` log. |
| `lumen-context` | `ConfigurationPropertiesProcessor.findPrefix()` resolves `@ConfigurationProperties` via string-comparing the annotation's fully-qualified name then reflectively invoking `value()`/`prefix()` — a heavier, slower reinvention of the classloader-resilient `ReflectionUtil.hasAnnotationByName()`/`getAnnotationStringValue()` idiom already established for exactly this situation. Route through the established helper instead. |
| `lumen-aop` | `ByteBuddyDelegatingInterceptor` is 100% dead code — zero references outside its own declaration. `ByteBuddyProxyProvider.createDelegatingProxy()` instead hand-rolls an equivalent-but-different anonymous `MethodInvocation` inline with its own separate counter, rather than reusing `ReflectiveMethodInvocation` (the one shared chain implementation two other interceptors already use). Delete the dead class, or make `createDelegatingProxy()` actually use it. |
| `lumen-security` | `RequestMatcher` is package-private while its public consumers (`AuthorizationRule`, the `SecurityRuleContributor` SPI) aren't — external contributors like `lumen-boot-actuator`'s `ActuatorSecurityContributor` can construct a `new AuthorizationRule(new AntPathRequestMatcher(...), ...)` today only by accident of an obscure Java accessibility rule (supplying a concrete subtype without ever naming the interface directly). Make `RequestMatcher` public — it's already de facto part of the documented extension surface. |
| `lumen-security` | `HttpSecurity.build()` is a single ~35-line method doing five jobs (context repository, form-login/logout filter wiring, exception-translation/authorization filter appending, contributor-rule merging, sorting) that grows with every new filter type added to the chain. Consider decomposing into a composable filter-registration list. |
| `lumen-web` | `Route` is a mutable, half-built value object — constructed with 4 required fields, then `setRest`/`setConsumes`/`setProduces` called later by `ControllerScanner`, with no validation that they were actually called before the route is registered. Move to a builder or make it fully immutable. |

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
| Implicit joins for collection-valued derived query properties | minor | `findByItems_Name`-style paths through a `@OneToMany`/`@ManyToMany` association currently fail with a clear "not supported yet" error from `NameResolvingQueryParser.validatePropertyPath()` (patch-fixed 2026-09-22 to at least explain why). Making it actually work needs `NameResolvingQueryParser`/`PartTree` to emit an implicit `JOIN e.items i` and rewrite the condition against `i` instead of dot-navigating through the collection, which isn't legal JPQL — real query-generation work, not a validation tweak |
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
| Hot reload (re-scan without restart) | minor | Watch the classpath and re-run `ControllerScanner`/route registration into the *running* container instead of just suggesting a restart. Needs registration to be safe against concurrent `findMatch()` calls from in-flight requests while the reload is happening — `RouteRegistry.routes` is already a `CopyOnWriteArrayList` with a `synchronized register()` for exactly this reason (see lumen-web patch-release table history), but nothing else in the init path (`LightContainer.initialize()`, `ModuleInitializer`) is reload-safe yet |
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