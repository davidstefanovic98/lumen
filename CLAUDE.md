# Lumen Framework

A lightweight, annotation-driven Java framework inspired by Spring. Built with Java 21, Maven, ByteBuddy for bytecode generation, and Tomcat Embed for the servlet container.

## Module Overview

| Module | Description |
|---|---|
| `lumen-core` | IoC container (DI, scopes, lifecycle, lazy loading) |
| `lumen-context` | Annotation config, component scanning, SPI module loading |
| `lumen-aop` | ByteBuddy-based AOP proxy generation |
| `lumen-web` | DispatcherServlet, routing, argument resolution, HTTP converters |
| `lumen-web-mvc` | ModelAndView, view resolution layer |
| `lumen-web-thymeleaf` | Thymeleaf view resolver integration |
| `lumen-security` | Filter chain, authentication, authorization |
| `lumen-data` | JPA repository pattern — **in progress** |

## Architecture

**Beans are called "lights"** throughout the framework (LightContainer, LightDefinition, @Light).

**Module loading** uses the Java SPI (`ServiceLoader`). Each module implements `LumenModule` and registers itself in `META-INF/services/io.lumen.core.LumenModule`. Modules receive the `LightContainer` and base packages during `init()`.

**Proxy generation** is also SPI-based via `ProxyProvider`. `lumen-aop` provides the ByteBuddy implementation for:
- Lazy bean initialization
- AOP method interception
- Configuration method caching (`@Configuration` class proxying)
- Interface-based repository proxies (`lumen-data`)

**Startup flow:**
1. `AnnotationApplicationContext` created with a `@Configuration` class
2. `ConfigProcessor` processes `@Light` factory methods
3. `ModuleInitializer` loads all `LumenModule` implementations via SPI
4. `PackageScanner` scans component packages
5. `LightContainer.initialize()` resolves and instantiates beans

## Key Conventions

- Dependency injection: constructor (preferred), field, or setter via `@Inject`
- Qualifiers: `@Qualifier`, `@Primary`, `@Order` for disambiguation
- Lifecycle: `@PostConstruct` for initialization callbacks
- Scopes: `@Scope("singleton")` (default) or `@Scope("prototype")`
- Profiles: `@Profile("name")` and `Environment.setProfile()`
- Conditionals: `@Conditional`, `@ConditionalOnClass`, `@ConditionalOnProperty`, `@ConditionalOnLight`

## lumen-data (In Progress)

**What's done:**
- `Repository` → `CrudRepository` → `JpaRepository` interface hierarchy
- CRUD proxy generation (`save`, `findById`, `findAll`, `deleteById`)
- `@Query` annotation for custom JPQL
- Method-name query parsing (`findByName`, `findByAgeGreaterThan`, AND/OR)
- Part-type conditions: `GreaterThan`, `LessThan`, `Like`, `Not`, `True`, `False`, `IsNull`, `IsNotNull`
- Entity field validation at repository proxy creation time

**What's missing (priority order):**
1. **Transaction management** — EntityManager lifecycle, `@Transactional` support
2. **`saveAll()` / `deleteAll()`** — batch operations on `JpaRepository`
3. **Pagination & sorting** — `Page<T>`, `Pageable`, `Sort` types and `findAll(Pageable)`
4. **Nested property traversal** — e.g., `findByAddress_City`
5. **`count*` / `exists*` / `delete*` derived methods** — by-name query derivation beyond `find`
6. **`@Modifying` queries** — UPDATE/DELETE via `@Query`
7. **Named parameters** — `:name` style binding alongside positional `?1`
8. **Return type flexibility** — `Optional<T>`, `List<T>`, `long` counts
9. **Tests** — zero test coverage in `lumen-data` currently

**Known issues:**
- `NameResolvingQueryParser` line 56 has a `System.out.println` debug statement to remove
- No `@Transactional` — callers must manage `EntityManager` lifecycle

## Build & Test

```bash
# Build all modules
mvn clean install

# Run tests
mvn test

# Run tests for a specific module
mvn test -pl lumen-core
```

Tests use JUnit 5. Test classes live in `src/test/java/` within each module.