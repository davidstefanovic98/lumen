# Lumen Framework — API Reference

**Version:** 0.1.0-SNAPSHOT

---

## Table of Contents

1. [Core Annotations](#1-core-annotations)
2. [Context Annotations](#2-context-annotations)
3. [Web Annotations](#3-web-annotations)
4. [Security Annotations](#4-security-annotations)
5. [Data Annotations](#5-data-annotations)
6. [Cache Annotations](#6-cache-annotations)
7. [Async and Scheduling Annotations](#7-async-and-scheduling-annotations)
8. [Validation Annotations](#8-validation-annotations)
9. [WebSocket Annotations](#9-websocket-annotations)
10. [Key Interfaces and Classes](#10-key-interfaces-and-classes)
11. [HTTP Utilities](#11-http-utilities)
12. [Mail API](#12-mail-api)
13. [Event System](#13-event-system)
14. [Configuration Properties](#14-configuration-properties)
15. [Exception Types](#15-exception-types)

---

## 1. Core Annotations

### `@Light`

Marks a **factory method** inside a `@Configuration` class. The method's return value is registered as a light in the container. This is Lumen's equivalent of Spring's `@Bean`.

`@Light` is method-level only (`@Target(METHOD)`) — it cannot annotate a class. To register a class as a light, use `@Component` or `@Service`.

```java
@Configuration
public class AppConfig {
    @Light
    public DataSource dataSource() { ... }
}
```

**Attributes:**

| Attribute | Type | Default | Description |
|---|---|---|---|
| `name` | `String` | `""` | Optional light name. Defaults to the method name. |
| `profile` | `String` | `""` | Only register when this profile is active. |
| `condition` | `String` | `""` | Only register when this conditional expression passes. |

---

### `@Component`

Marks a **class** as a light. Discovered during component scan and instantiated by the container, with constructor dependencies resolved automatically.

```java
@Component
public class TokenGenerator { ... }
```

`@Service` is a specialization of `@Component` (meta-annotated with it) carrying the same behaviour; use it to mark service-layer classes for readability.

```java
@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {  // injected
        this.repository = repository;
    }
}
```

---

### `@Inject`

Injects a dependency from the container into a field, setter, or constructor parameter.

```java
@Service
public class OrderService {
    @Inject
    private UserRepository userRepository;   // field injection

    @Inject
    public OrderService(PaymentService payment) { ... }  // constructor injection
}
```

Used on constructor parameters, fields, or setter methods.

---

### `@Value`

Injects a value from `application.properties` into a field or constructor parameter.

```java
@Configuration
public class MailConfig {
    @Value("lumen.mail.host")
    private String host;

    @Value("lumen.mail.port")
    private int port;
}
```

Type coercion is performed automatically for `int`, `long`, `boolean`, and `String`.

---

### `@Qualifier`

Disambiguates injection when multiple lights of the same type are registered.

```java
@Inject
@Qualifier("primaryDataSource")
private DataSource dataSource;
```

---

### `@Primary`

Marks a light as the preferred candidate when multiple lights of the same type exist and no `@Qualifier` is specified.

```java
@Component
@Primary
public class DefaultCacheManager implements CacheManager { ... }
```

---

### `@PostConstruct`

Marks a method to be called after the light is fully initialised (all dependencies injected).

```java
@Component
public class ConnectionPool {
    @PostConstruct
    public void init() {
        // runs once after injection is complete
    }
}
```

---

### `@Order`

Controls ordering of modules, processors, initializers, and filter chain elements. Lower values run first.

```java
@Order(1)
public class MyLumenModule implements LumenModule { ... }
```

---

### `@Lazy`

Defers instantiation of a dependency until the first method call on the injected instance.

```java
@Inject
@Lazy
private HeavyService heavy;
```

A ByteBuddy proxy is injected at startup; the real light is created on first use.

---

### `@Scope`

Overrides the default singleton scope for a light.

```java
@Component
@Scope(ScopeType.PROTOTYPE)
public class RequestContext { ... }
```

**Values:** `SINGLETON` (default), `PROTOTYPE`, `REQUEST`, `SESSION`.

---

### `@Profile`

Restricts a light to specific active profiles.

```java
@Component
@Profile("dev")
public class MockPaymentGateway implements PaymentGateway { ... }
```

Activate a profile with `lumen.profiles.active=dev` in `application.properties`.

---

## 2. Context Annotations

### `@Configuration`

Marks a class as a source of light definitions. Methods annotated with `@Light` inside a `@Configuration` class are treated as factory methods. Repeated calls to the same factory method return the cached instance (implemented via a ByteBuddy subclass proxy).

```java
@Configuration
public class SecurityConfig {
    @Light
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

### `@ComponentScan`

Triggers a classpath scan for classes annotated with `@Component`, `@Service`, `@Controller`, `@RestController`, `@Configuration`, and similar stereotype annotations.

```java
@Configuration
@ComponentScan("io.example")
public class AppConfig { }
```

**Attributes:**

| Attribute | Type | Default | Description |
|---|---|---|---|
| `value` | `String[]` | `{}` | Base packages to scan. |

---

### `@ConditionalOnProperty`

Registers the light only when the specified property has the given value.

```java
@Component
@ConditionalOnProperty(name = "lumen.mail.host", havingValue = "smtp.gmail.com")
public class GmailHealthChecker { ... }
```

| Attribute | Type | Description |
|---|---|---|
| `name` | `String` | Property key to check. |
| `havingValue` | `String` | Required value (string comparison). |

---

### `@ConditionalOnClass`

Registers the light only when the specified class is present on the classpath.

```java
@Component
@ConditionalOnClass("com.zaxxer.hikari.HikariDataSource")
public class HikariPoolMetrics { ... }
```

---

### `@ConditionalOnLight`

Registers the light only when another light of the specified type is present in the container.

```java
@Component
@ConditionalOnLight(Validator.class)
public class ValidatingService { ... }
```

---

## 3. Web Annotations

### `@Controller`

Marks a class as an MVC controller. Methods return a view name (string) or `ModelAndView`.

```java
@Controller
public class HomeController {
    @GetMapping("/")
    public String home(ModelAndView mav) {
        mav.addAttribute("title", "Home");
        return "home";
    }
}
```

---

### `@RestController`

Combines `@Controller` and `@ResponseBody`. All handler methods serialize their return value directly to the HTTP response body as JSON (or another registered media type). Equivalent to `@Controller` + `@ResponseBody` on every method.

```java
@RestController
public class UserController {
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) { ... }
}
```

---

### `@RequestMapping`

Maps HTTP requests to a handler class or method. More specific mapping annotations (`@GetMapping`, etc.) are shorthand forms.

```java
@RequestMapping("/api/v1/orders")
@RestController
public class OrderController { ... }
```

| Attribute | Type | Description |
|---|---|---|
| `value` | `String` | URL path (supports `{variable}` templates). |
| `method` | `HttpMethod` | Limits the mapping to a specific HTTP method. |

---

### `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`

Shorthand for `@RequestMapping(method = HttpMethod.GET/POST/PUT/DELETE/PATCH)`.

```java
@GetMapping("/products")
public List<Product> list() { ... }

@PostMapping("/products")
public Product create(@RequestBody ProductRequest req) { ... }

@DeleteMapping("/products/{id}")
public void delete(@PathVariable Long id) { ... }
```

---

### `@PathVariable`

Binds a URI template variable to a method parameter.

```java
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) { ... }

@GetMapping("/users/{userId}/orders/{orderId}")
public Order getOrder(@PathVariable Long userId, @PathVariable Long orderId) { ... }
```

---

### `@RequestParam`

Binds a query string or form parameter to a method parameter.

```java
@GetMapping("/search")
public List<Product> search(@RequestParam String query,
                            @RequestParam(required = false) Integer page) { ... }
```

| Attribute | Type | Default | Description |
|---|---|---|---|
| `value` | `String` | Parameter name from `-parameters` flag | Query parameter name. |
| `required` | `boolean` | `true` | Whether to throw 400 if absent. |
| `defaultValue` | `String` | `""` | Value used when the parameter is absent. |

---

### `@RequestBody`

Deserializes the HTTP request body (JSON by default) into a method parameter.

```java
@PostMapping("/users")
public User create(@RequestBody @Valid CreateUserRequest req) { ... }
```

Combine with `@Valid` to trigger constraint validation.

---

### `@RequestHeader`

Binds an HTTP request header to a method parameter.

```java
@GetMapping("/profile")
public Profile getProfile(@RequestHeader("Authorization") String token) { ... }
```

| Attribute | Type | Default | Description |
|---|---|---|---|
| `value` | `String` | — | Header name (case-insensitive). |
| `required` | `boolean` | `true` | Throws `MissingRequestHeaderException` if absent and `true`. |

---

### `@ResponseBody`

Serialises the return value of a handler method directly to the HTTP response body, bypassing view resolution.

```java
@Controller
public class ApiController {
    @GetMapping("/status")
    @ResponseBody
    public Map<String, String> status() {
        return Map.of("status", "ok");
    }
}
```

Not needed when using `@RestController`.

---

### `@ResponseStatus`

Sets a fixed HTTP status code for the response, or marks an exception class with a default HTTP status.

```java
@PostMapping("/items")
@ResponseStatus(HttpStatus.CREATED)
public Item create(@RequestBody Item item) { ... }

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ItemNotFoundException extends RuntimeException { ... }
```

---

### `@ControllerAdvice`

Marks a class as a global exception handler. Methods annotated with `@ExceptionHandler` inside this class handle exceptions thrown from any controller.

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ItemNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ItemNotFoundException e) {
        return new ErrorResponse(e.getMessage());
    }
}
```

---

### `@ExceptionHandler`

Declares a method as a handler for a specific exception type. Must be used inside a `@ControllerAdvice` class.

```java
@ExceptionHandler(ValidationException.class)
public ResponseEntity<String> handleValidation(ValidationException e) {
    return ResponseEntity.status(422).body(e.getMessage());
}
```

---

### `@ModelAttribute`

Binds request parameters to a method parameter by populating an object's fields from the request.

```java
@GetMapping("/items")
public String search(@ModelAttribute SearchForm form, ModelAndView mav) { ... }
```

---

### `@Consumes` / `@Produces`

Restricts a handler to requests with a specific `Content-Type` (`@Consumes`) or advertises the response media type (`@Produces`).

```java
@PostMapping("/upload")
@Consumes("multipart/form-data")
@Produces("application/json")
public UploadResult upload(@RequestPart MultipartFile file) { ... }
```

---

## 4. Security Annotations

### `@PreAuthorize`

Evaluates a security expression before the method is invoked. Throws `AccessDeniedException` if the expression evaluates to `false`. Expressions are evaluated by the **Gleam** engine (`lumen-gleam`).

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) { ... }

@PreAuthorize("isAuthenticated()")
public Profile getMyProfile() { ... }

// Boolean operators and parameter binding
@PreAuthorize("hasRole('ADMIN') || #userId == #authentication.name")
public Profile getProfile(String userId) { ... }
```

**Functions:**

| Expression | Description |
|---|---|
| `hasRole('ADMIN')` | Current user has the given role (`ROLE_` prefix added automatically). |
| `hasAnyRole('R1', 'R2')` | Current user has at least one of the given roles. |
| `hasAuthority('ROLE_ADMIN')` | Current user has the exact authority string (no prefix added). |
| `hasAnyAuthority('A', 'B')` | Current user has at least one of the exact authorities. |
| `isAuthenticated()` | User is logged in (non-anonymous). |
| `isAnonymous()` | User is not authenticated. |
| `permitAll()` | Always allows. |
| `denyAll()` | Always denies. |

**Variables:**

| Variable | Description |
|---|---|
| `#authentication` | The current `Authentication`; supports property navigation, e.g. `#authentication.name`. |
| `#paramName` | A method parameter by its declared name (requires the `-parameters` compiler flag). |

**Operators:** Gleam supports `&&`, `||`, `!`, `==`, `!=`, `<`, `>`, `<=`, `>=`, and property/method chaining.

---

### `@PostAuthorize`

Evaluates a security expression after the method returns. Useful for verifying the returned object belongs to the current user. Accepts the same functions, variables, and operators as `@PreAuthorize`, plus `#returnObject` — the value returned by the method.

```java
@PostAuthorize("#returnObject.ownerId == #authentication.name")
public Order getOrder(Long id) { ... }
```

---

## 5. Data Annotations

### `@Transactional`

Wraps the method (or all public methods of the class) in a database transaction. A proxy is created by `TransactionalProcessor` using `JpaTransactionManager`.

```java
@Service
public class UserService {
    @Transactional
    public void transferFunds(Long fromId, Long toId, BigDecimal amount) { ... }
}
```

The class must implement at least one interface, OR be wrapped via `createDelegatingProxy` (which is the case since the last refactoring in Lumen). If no transaction is active, one is started; if one is already active, the existing one is used (required semantics).

---

### `@Query`

Provides an explicit JPQL query for a repository method, bypassing derived query parsing.

```java
public interface OrderRepository extends LumenRepository<Order, Long> {
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.user.id = :userId")
    List<Order> findByStatusAndUserId(String status, Long userId);
}
```

---

### Repository interfaces

Repositories are declared as Java interfaces extending one of:

| Interface | Provided methods |
|---|---|
| `LumenRepository<T, ID>` | `save`, `findById`, `findAll`, `delete`, `deleteById`, `existsById`, `count` |
| `PagingAndSortingRepository<T, ID>` | All of above + `findAll(Sort)`, `findAll(Pageable)` |
| `JpaSpecificationExecutor<T>` | `findAll(Specification)` |

**Derived query methods** are generated automatically from the method name:

```java
List<Task> findByProjectAndStatus(Project project, String status);
long countByStatus(String status);
boolean existsByEmail(String email);
List<Task> findByTitleContainingOrderByCreatedAtDesc(String keyword);
```

Supported predicates: `Equals`, `IgnoreCase`, `StartingWith`, `EndingWith`, `Containing`, `Between`, `GreaterThan`, `LessThan`, `IsNull`, `IsNotNull`, `IsIn`, `IsNotIn`, `True`, `False`.

---

## 6. Cache Annotations

### `@Cacheable`

Caches the return value of a method. On subsequent calls with the same key, the cached value is returned without executing the method body.

```java
@Cacheable(value = "products", key = "#id")
public Product findById(Long id) { ... }

@Cacheable("all-products")
public List<Product> findAll() { ... }
```

| Attribute | Type | Description |
|---|---|---|
| `value` | `String` | Cache name. |
| `key` | `String` | Key expression. Blank → `methodName:firstArg`. `#paramName` → parameter value. Literal otherwise. |

---

### `@CacheEvict`

Removes an entry (or all entries) from a cache.

```java
@CacheEvict(value = "products", key = "#id")
public void delete(Long id) { ... }

@CacheEvict(value = "products", allEntries = true)
public void clearAll() { ... }
```

| Attribute | Type | Default | Description |
|---|---|---|---|
| `value` | `String` | — | Cache name. |
| `key` | `String` | `""` | Key to evict. |
| `allEntries` | `boolean` | `false` | Clear all entries in the cache. |

---

### `@CachePut`

Always executes the method and stores the result in the cache, updating any existing entry.

```java
@CachePut(value = "products", key = "#product.id")
public Product update(Product product) { ... }
```

---

## 7. Async and Scheduling Annotations

### `@Async`

Executes the method on a thread pool thread. Returns immediately with a `CompletableFuture`; the method body runs asynchronously.

```java
@Async
public CompletableFuture<String> sendNotification(String message) {
    // runs on the async thread pool
    return CompletableFuture.completedFuture("sent");
}

@Async
public void fireAndForget(String event) {
    // void methods run asynchronously; exceptions go to AsyncUncaughtExceptionHandler
}
```

| Attribute | Type | Default | Description |
|---|---|---|---|
| `value` | `String` | `"default"` | Executor name (currently single pool). |

Void methods that throw exceptions are handled by `AsyncUncaughtExceptionHandler`. The default implementation logs the exception. Register a custom handler as a light to override.

---

### `@Scheduled`

Schedules a method for periodic execution after the container is initialised.

```java
@Scheduled(fixedRate = 60_000)
public void runEveryMinute() { ... }

@Scheduled(fixedDelay = 30_000, initialDelay = 5_000)
public void runWithDelay() { ... }
```

| Attribute | Type | Default | Description |
|---|---|---|---|
| `fixedRate` | `long` | `-1` | Execute every N milliseconds from the previous start. Mutually exclusive with `fixedDelay`. |
| `fixedDelay` | `long` | `-1` | Execute every N milliseconds after the previous execution completes. |
| `initialDelay` | `long` | `0` | Wait N milliseconds before the first execution. |

At least one of `fixedRate` or `fixedDelay` must be set.

---

## 8. Validation Annotations

Apply these to fields of request body classes or entity fields. Trigger validation by adding `@Valid` to a `@RequestBody` parameter.

| Annotation | Validates |
|---|---|
| `@NotNull` | Field must not be `null`. |
| `@NotBlank` | String must not be `null` or blank (empty after trimming). |
| `@NotEmpty` | String or collection must not be `null` or empty. |
| `@Min(value)` | Numeric value must be ≥ `value`. |
| `@Max(value)` | Numeric value must be ≤ `value`. |
| `@Size(min, max)` | String length or collection size must be within `[min, max]`. |
| `@Email` | String must be a valid email address format. |
| `@Pattern(regex)` | String must match the regular expression. |

```java
public class CreateUserRequest {
    @NotBlank
    private String username;

    @Email
    @NotNull
    private String email;

    @Size(min = 8, max = 64)
    private String password;
}
```

A 422 response is returned automatically when validation fails, with a JSON body listing each violation:

```json
{
  "errors": [
    { "field": "email", "message": "must be a valid email address" },
    { "field": "password", "message": "size must be between 8 and 64" }
  ]
}
```

---

## 9. WebSocket Annotations

### `@LumenWebSocket`

Registers a class as a WebSocket endpoint. The class must implement `WebSocketHandler`.

```java
@LumenWebSocket("/ws/chat")
public class ChatEndpoint implements WebSocketHandler {
    @Override
    public void onMessage(Session session, String message) { ... }

    @Override
    public void onOpen(Session session) { ... }

    @Override
    public void onClose(Session session, CloseReason reason) { ... }

    @Override
    public void onError(Session session, Throwable error) { ... }
}
```

| Attribute | Type | Default | Description |
|---|---|---|---|
| `value` | `String` | — | WebSocket path. |
| `allowedOrigins` | `String[]` | `{}` | Allowed origins. Empty = same-origin only. `"*"` = all origins. |

Origin validation is enforced by `OriginValidatingConfigurator`. If `allowedOrigins` is not set on the annotation, the `lumen.websocket.allowed-origins` property is read. If that is also absent, only same-origin connections are accepted.

---

## 10. Key Interfaces and Classes

### `LumenApplication`

Entry point for all Lumen web applications.

```java
public class MyApp {
    public static void main(String[] args) {
        LumenApplication.run(MyApp.class, args);
    }
}
```

`run()` creates an `AnnotationWebApplicationContext`, scans from the config class, starts Tomcat, and blocks until the JVM shuts down.

---

### `LightContainer`

The IoC container. Normally accessed via injection; occasionally needed in `LumenModule.init()`.

```java
container.register(MyService.class);
container.registerExternalInstance(DataSource.class, ds);
MyService svc = container.getLight(MyService.class);
List<Plugin> plugins = container.getLights(Plugin.class);
boolean present = container.hasLight(CacheManager.class);
container.addPostProcessor(new MyProcessor());
container.initialize();
```

---

### `Environment`

Read-only view of loaded properties and active profiles.

```java
@Inject
private Environment env;

String host = env.getProperty("lumen.mail.host");
String port = env.getProperty("lumen.mail.port", "25");
List<String> profiles = env.getActiveProfiles();
boolean dev = env.acceptsProfile("dev");
```

---

### `LumenModule`

SPI interface for autoconfiguration. Implement this to create a new module.

```java
@Order(5)
public class MyModule implements LumenModule {
    @Override
    public void init(LightContainer container, String... basePackages) {
        Environment env = container.getLight(Environment.class);
        container.register(MyService.class);
    }
}
```

Register in `META-INF/services/io.lumen.core.LumenModule`.

---

### `LightProcessor`

Post-processing hook that runs after each light is instantiated.

```java
public class MyProcessor implements LightProcessor {
    @Override
    public Object afterInstantiation(LightInstance light, Object instance) {
        // Optionally wrap instance in a proxy and return the proxy
        return instance;
    }
}
```

---

### `LumenInitializer`

Hook that runs after `container.initialize()` completes, with access to the `ServletContext`.

```java
@Order(3)
public class MyInitializer implements LumenInitializer {
    @Override
    public void onStartup(ServletContext servletContext) throws Exception {
        // register servlets, listeners, etc.
    }
}
```

Register as a light (`container.register(MyInitializer.class)`).

---

### `ViewResolver`

Interface for MVC view rendering. Implement and register as a light to add a custom template engine.

```java
@Component
public class MustacheViewResolver implements ViewResolver {
    @Override
    public void resolve(String viewName, Map<String, Object> model,
                        HttpServletRequest req, HttpServletResponse resp) throws Exception {
        // render viewName template with model into resp.getWriter()
    }
}
```

---

### `CacheManager`

SPI for custom cache backends. Register a `@Component` of this type (a light) to replace the default in-memory implementation.

```java
@Component
public class RedisCacheManager implements CacheManager {
    @Override
    public Cache getCache(String name) { ... }
}
```

---

### `ResponseEntity<T>`

Carries a response body, HTTP status code, and optional headers. Returned from controller methods.

```java
@PostMapping("/users")
public ResponseEntity<User> create(@RequestBody @Valid CreateUserRequest req) {
    User user = userService.create(req);
    return ResponseEntity.status(HttpStatus.CREATED).body(user);
}

@DeleteMapping("/users/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();
}
```

---

### `ModelAndView`

Carries a view name and a model map for MVC controllers. Injected as a method parameter by the framework.

```java
@GetMapping("/dashboard")
public String dashboard(ModelAndView mav) {
    mav.addAttribute("user", currentUser());
    mav.addAttribute("tasks", taskService.findAll());
    return "dashboard";     // resolved to templates/dashboard.html by ThymeleafViewResolver
}
```

---

## 11. HTTP Utilities

### `HttpStatus`

Enumeration of HTTP status codes.

```java
HttpStatus.OK              // 200
HttpStatus.CREATED         // 201
HttpStatus.NO_CONTENT      // 204
HttpStatus.BAD_REQUEST     // 400
HttpStatus.UNAUTHORIZED    // 401
HttpStatus.FORBIDDEN       // 403
HttpStatus.NOT_FOUND       // 404
HttpStatus.UNPROCESSABLE_ENTITY  // 422
HttpStatus.INTERNAL_SERVER_ERROR // 500
```

---

### `MultipartFile`

Represents an uploaded file from a `multipart/form-data` request.

```java
@PostMapping("/upload")
public String upload(@RequestPart MultipartFile file) {
    String name = file.getOriginalFilename();
    byte[] bytes = file.getBytes();
    long size = file.getSize();
    String type = file.getContentType();
    InputStream stream = file.getInputStream();
    return "uploaded";
}
```

---

## 12. Mail API

### `MailSender`

Base interface for sending simple email messages.

```java
@Inject
private MailSender mailSender;

SimpleMailMessage msg = new SimpleMailMessage();
msg.setTo("user@example.com");
msg.setSubject("Welcome");
msg.setText("Hello, welcome to our service!");
mailSender.send(msg);
```

---

### `JavaMailSender`

Extends `MailSender` with MIME message support.

```java
@Inject
private JavaMailSender javaMailSender;

javaMailSender.send(mimeMessage -> {
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
    helper.setTo("user@example.com");
    helper.setSubject("Report");
    helper.setText("<h1>Hello</h1>", true);   // true = HTML
    helper.addAttachment("report.pdf", file);
});
```

---

### `MimeMessageHelper`

Builder for constructing MIME messages.

| Method | Description |
|---|---|
| `setTo(String...)` | Set recipient addresses. |
| `setCc(String...)` | Set CC addresses. |
| `setBcc(String...)` | Set BCC addresses. |
| `setFrom(String)` | Set sender address. |
| `setSubject(String)` | Set email subject. |
| `setText(String)` | Set plain text body. |
| `setText(String, boolean)` | Set body; `true` = HTML. |
| `addAttachment(String, File)` | Attach a file. |
| `addInline(String, File)` | Embed an inline resource. |

---

## 13. Event System

### `ApplicationEvent`

Base class for all application events.

```java
public class UserCreatedEvent extends ApplicationEvent {
    private final User user;

    public UserCreatedEvent(Object source, User user) {
        super(source);
        this.user = user;
    }

    public User getUser() { return user; }
}
```

---

### `ApplicationEventPublisher`

Publishes events to all registered listeners. Injected by the framework.

```java
@Inject
private ApplicationEventPublisher publisher;

public User create(CreateUserRequest req) {
    User user = userRepository.save(new User(req));
    publisher.publishEvent(new UserCreatedEvent(this, user));
    return user;
}
```

---

### `@EventListener`

Marks a method as an event listener. The method parameter type determines which event type it handles.

```java
@Component
public class NotificationService {
    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        sendWelcomeEmail(event.getUser());
    }
}
```

Events are delivered synchronously in the thread of the publisher. Use `@Async` on the listener method to decouple it.

```java
@EventListener
@Async
public void onOrderPlaced(OrderPlacedEvent event) {
    // runs on async thread pool
}
```

---

## 14. Configuration Properties

All properties are loaded from `application.properties` on the classpath. Profile-specific files (`application-dev.properties`) override the base file for the active profile.

### Server

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP listening port. |
| `lumen.profiles.active` | — | Comma-separated list of active profiles. |

### Data source

| Property | Default | Description |
|---|---|---|
| `lumen.datasource.url` | — | JDBC URL. |
| `lumen.datasource.username` | — | Database username. |
| `lumen.datasource.password` | — | Database password. |
| `lumen.jpa.ddl-auto` | `none` | Schema generation: `none`, `validate`, `update`, `create`, `create-drop`. |
| `lumen.jpa.show-sql` | `false` | Log SQL statements. |

### HikariCP connection pool

| Property | Default | Description |
|---|---|---|
| `lumen.datasource.maximum-pool-size` | `10` | Maximum number of connections. |
| `lumen.datasource.minimum-idle` | `2` | Minimum idle connections. |
| `lumen.datasource.connection-timeout` | `30000` | Connection acquisition timeout (ms). |
| `lumen.datasource.idle-timeout` | `600000` | Idle connection timeout (ms). |
| `lumen.datasource.max-lifetime` | `1800000` | Maximum connection lifetime (ms). |

### Mail

| Property | Default | Description |
|---|---|---|
| `lumen.mail.host` | — | SMTP server hostname. Empty → `NoOpMailSender`. |
| `lumen.mail.port` | `587` | SMTP port. |
| `lumen.mail.username` | — | SMTP authentication username. |
| `lumen.mail.password` | — | SMTP authentication password. |
| `lumen.mail.smtp.auth` | `true` | Enable SMTP AUTH. |
| `lumen.mail.smtp.starttls.enable` | `true` | Enable STARTTLS. |
| `lumen.mail.default-encoding` | `UTF-8` | Default charset. |

### Async

| Property | Default | Description |
|---|---|---|
| `lumen.async.pool-size` | `8` | Thread pool size for `@Async`. |
| `lumen.async.queue-capacity` | `100` | Task queue capacity before rejection. |

### WebSocket

| Property | Default | Description |
|---|---|---|
| `lumen.websocket.allowed-origins` | — | Comma-separated list of allowed WebSocket origins. |

### Thymeleaf

| Property | Default | Description |
|---|---|---|
| `lumen.thymeleaf.mvc.enabled` | `true` | Set to `false` to disable `ThymeleafViewResolver` even when lumen-web-mvc is present. |

---

## 15. Exception Types

### Container exceptions (`lumen-core`)

| Exception | Thrown when |
|---|---|
| `NoLightFoundException` | No light of the requested type is registered. |
| `AmbiguousLightException` | Multiple lights of the same type exist and no `@Qualifier` or `@Primary` resolves the ambiguity. |
| `CircularDependencyException` | A circular dependency is detected during resolution. |
| `LightInitializationException` | A `@PostConstruct` method throws an exception. |
| `LightInstantiationException` | Reflection-based instantiation fails. |
| `MissingDependencyException` | A required dependency cannot be resolved. |

### Web exceptions (`lumen-web`)

| Exception | HTTP Status | Description |
|---|---|---|
| `NotFoundException` | 404 | Thrown from a controller to return 404. |
| `HttpMediaTypeNotSupportedException` | 415 | Request `Content-Type` not supported. |
| `HttpMediaTypeNotAcceptableException` | 406 | Requested `Accept` type not producible. |
| `MissingRequestParameterException` | 400 | Required `@RequestParam` absent. |
| `MissingRequestHeaderException` | 400 | Required `@RequestHeader` absent. |
| `MultipartException` | 400 | Multipart parsing failed. |

### Security exceptions (`lumen-security`)

| Exception | Description |
|---|---|
| `AuthenticationException` | Base for all authentication failures. |
| `UsernameNotFoundException` | User not found during authentication. |
| `BadCredentialsException` | Incorrect password. |
| `AccessDeniedException` | `@PreAuthorize` expression evaluated to `false`. |

### Validation exceptions (`lumen-validation`)

| Exception | HTTP Status | Description |
|---|---|---|
| `ValidationException` | 422 | One or more constraint violations found. Carries the list of `ConstraintViolation` objects. |

### Mail exceptions (`lumen-mail`)

| Exception | Description |
|---|---|
| `MailSendException` | SMTP send failure. |
| `MailParseException` | Message structure is invalid. |
| `MailPreparationException` | Error during `MimeMessagePreparator`. |
| `MailAuthenticationException` | SMTP authentication failed. |