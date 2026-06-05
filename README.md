# Lumen

A lightweight, annotation-driven Java web framework inspired by Spring. Built from scratch as a reference implementation of the core patterns that make such frameworks work: dependency injection, AOP-based proxying, filter chains, and SPI-based autoconfiguration.

**Java 25 · Maven · ByteBuddy · Tomcat Embed · Hibernate 7**

---

## What it is

Lumen is a boot-first framework — add a starter, write your application, run it. No XML, no manual wiring. The programming model mirrors Spring to minimise the learning curve while keeping the implementation small enough to read and understand in full.

Managed objects are called **lights** (not beans).

---

## Quick start

```xml
<dependency>
    <groupId>io.lumen</groupId>
    <artifactId>lumen-boot-starter-web</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
@LumenBootApplication
public class App {
    public static void main(String[] args) {
        LumenApplication.run(App.class, args);
    }
}

@Service
public class GreetingService {
    public String greet(String name) { return "Hello, " + name; }
}

@RestController
@RequestMapping("/hello")
public class HelloController {

    private final GreetingService greetingService;

    public HelloController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GetMapping("/{name}")
    public String hello(@PathVariable String name) {
        return greetingService.greet(name);
    }
}
```

```properties
# application.properties
server.port=8080
```

---

## Starters

| Starter | What it enables |
|---|---|
| `lumen-boot-starter` | IoC container, DI, AOP, boot |
| `lumen-boot-starter-web` | HTTP layer, routing, CORS, exception handling |
| `lumen-boot-starter-security` | Filter chain, JWT support, `@PreAuthorize` / `@PostAuthorize` |
| `lumen-boot-starter-data-jpa` | JPA repositories, derived queries, `@Transactional` |
| `lumen-boot-starter-cache` | `@Cacheable` / `@CacheEvict` / `@CachePut` |
| `lumen-boot-starter-validation` | `@Valid`, constraint annotations, 422 responses |
| `lumen-boot-starter-async` | `@Async`, `@Scheduled` (fixedRate / fixedDelay / cron) |
| `lumen-boot-starter-websocket` | JSR-356 WebSocket endpoints, origin validation, auth propagation |
| `lumen-boot-starter-mail` | `MailSender`, `JavaMailSender`, `MimeMessageHelper` |
| `lumen-boot-starter-thymeleaf` | Thymeleaf template rendering |
| `lumen-boot-starter-flyway` | Flyway database migration |
| `lumen-boot-starter-actuator` | `/actuator/health`, `/info`, `/env`, `/lights` endpoints |

---

## Features

- **IoC container** — constructor, field, and setter injection; singleton, prototype, request, and session scopes; `@Lazy`, `@Profile`, conditional annotations
- **AOP** — ByteBuddy proxies for caching, security, transactions, and lazy initialisation
- **Security** — HTTP filter chain, `@PreAuthorize` / `@PostAuthorize` via the **Gleam** expression engine, `SecurityRuleContributor` SPI, `SecurityContext` propagated into `@Async` and WebSocket handler threads
- **WebSocket** — `HandshakeInterceptor` SPI; authentication available on every handler call via `WebSocketSession.getPrincipal()`
- **Async** — `TaskDecorator` SPI for ThreadLocal propagation across thread boundaries
- **Expression language** — Gleam (`lumen-gleam`), used by security and cache key expressions
- **Graceful shutdown** — connector paused before the drain window; `lumen.shutdown.timeout-seconds` configurable

---

## Building

```bash
# Build and install all modules
mvn clean install

# Run a specific module's tests
mvn test -pl lumen-security

# Run the demo app
cd ../lumen-demo && mvn exec:exec

# Build a runnable fat JAR
mvn package -Pfat-jar -DmainClass=com.example.App
```

---

## Documentation

- [Getting Started](docs/getting-started.md)
- [API Reference](docs/api-reference.md)
- [Architecture](docs/architecture.md)
- [Developer Guide](docs/developer-guide.md)

---

## License

[MIT](LICENSE)