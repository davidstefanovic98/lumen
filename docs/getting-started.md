# Lumen Framework — Getting Started

**Version:** 0.1.0-SNAPSHOT

---

## Prerequisites

- **Java 25** or later
- **Maven 3.8+**
- **PostgreSQL** (for data examples; H2 in-memory is also supported)

---

## 1. Create a Maven project

Create a standard Maven project with the following parent and the starters you need. You only ever declare starters in your `pom.xml` — the internal framework modules are pulled in transitively.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>25</maven.compiler.source>
        <maven.compiler.target>25</maven.compiler.target>
        <!-- Required for @PathVariable and @Cacheable key expressions -->
        <maven.compiler.parameters>true</maven.compiler.parameters>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <lumen.version>0.1.0-SNAPSHOT</lumen.version>
    </properties>

    <dependencies>
        <!-- Web layer: DispatcherServlet, routing, JSON -->
        <dependency>
            <groupId>io.lumen</groupId>
            <artifactId>lumen-boot-starter-web</artifactId>
            <version>${lumen.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.2.0</version>
                <configuration>
                    <mainClass>com.example.MyApp</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 2. Application entry point

Create your main class and call `LumenApplication.run()`. Pass the class annotated with `@Configuration` and `@ComponentScan` as the first argument.

```java
package com.example;

import io.lumen.context.annotation.ComponentScan;
import io.lumen.core.config.Config;
import io.lumen.web.boot.LumenApplication;

@Config                          // same as @Configuration
@ComponentScan("com.example")    // scan this package and all sub-packages
public class MyApp {
    public static void main(String[] args) {
        LumenApplication.run(MyApp.class, args);
    }
}
```

---

## 3. Configuration file

Create `src/main/resources/application.properties`:

```properties
server.port=8080
```

---

## 4. Hello World REST controller

```java
package com.example.controller;

import io.lumen.web.annotation.GetMapping;
import io.lumen.web.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Lumen!";
    }
}
```

Run the application:

```bash
mvn exec:java
```

Open `http://localhost:8080/hello` — you should see `Hello from Lumen!`.

---

## 5. JSON REST API

Lumen serialises and deserialises JSON automatically using Jackson. Method parameters annotated with `@RequestBody` are deserialized; return values are serialised unless the method returns `void` or directly writes to `HttpServletResponse`.

```java
@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public List<Product> list() {
        return productService.findAll();
    }

    @GetMapping("/products/{id}")
    public Product get(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@RequestBody @Valid CreateProductRequest req) {
        return productService.create(req);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## 6. Service layer and dependency injection

`@Service` (or `@Component`) marks a class as a light. Dependencies are injected via constructor injection (preferred) or `@Inject`. (`@Light` is a method-level annotation used only for factory methods inside `@Configuration` classes — see the `SecurityConfig` example in section 10 — not a class-level stereotype.)

```java
package com.example.service;

import io.lumen.context.annotation.Service;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> findAll() {
        return repository.findAll();
    }
}
```

---

## 7. Data access with JPA

Add the data starter:

```xml
<dependency>
    <groupId>io.lumen</groupId>
    <artifactId>lumen-boot-starter-data-jpa</artifactId>
    <version>${lumen.version}</version>
</dependency>
```

Configure the data source in `application.properties`:

```properties
lumen.datasource.url=jdbc:postgresql://localhost:5432/mydb
lumen.datasource.username=postgres
lumen.datasource.password=secret
lumen.jpa.ddl-auto=update
```

Declare your entity:

```java
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private BigDecimal price;

    // getters and setters
}
```

Declare the repository interface:

```java
package com.example.repository;

import io.lumen.data.repository.LumenRepository;

public interface ProductRepository extends LumenRepository<Product, Long> {

    List<Product> findByName(String name);

    List<Product> findByPriceLessThan(BigDecimal maxPrice);
}
```

No implementation is required. The framework generates a proxy at startup.

---

## 8. Input validation

Add the validation starter:

```xml
<dependency>
    <groupId>io.lumen</groupId>
    <artifactId>lumen-boot-starter-validation</artifactId>
    <version>${lumen.version}</version>
</dependency>
```

Annotate your request class:

```java
public class CreateProductRequest {

    @NotBlank
    private String name;

    @NotNull
    @Min(0)
    private BigDecimal price;

    // getters
}
```

Add `@Valid` to the controller parameter:

```java
@PostMapping("/products")
public Product create(@RequestBody @Valid CreateProductRequest req) { ... }
```

When validation fails, the framework returns HTTP 422 automatically:

```json
{
  "errors": [
    { "field": "name", "message": "must not be blank" }
  ]
}
```

---

## 9. Exception handling

Use `@ControllerAdvice` for global exception handling across all controllers:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ProductNotFoundException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception e) {
        return new ErrorResponse("An unexpected error occurred");
    }
}
```

Alternatively, annotate the exception class directly with `@ResponseStatus`:

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Product " + id + " not found");
    }
}
```

---

## 10. Security

Add the security starter:

```xml
<dependency>
    <groupId>io.lumen</groupId>
    <artifactId>lumen-boot-starter-security</artifactId>
    <version>${lumen.version}</version>
</dependency>
```

Implement `UserDetailsService` to load users:

```java
@Service
public class MyUserDetailsService implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));
        return new UserPrincipal(user.getId(), user.getUsername(),
                                 user.getPassword(), user.getRole());
    }
}
```

Configure route permissions:

```java
@Configuration
public class SecurityConfig {

    @Light
    public SecurityConfiguration securityConfiguration() {
        return SecurityConfiguration.builder()
            .authorizeRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .build();
    }
}
```

Protect individual service methods with `@PreAuthorize`:

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) { ... }
```

---

## 11. Caching

Add the cache starter:

```xml
<dependency>
    <groupId>io.lumen</groupId>
    <artifactId>lumen-boot-starter-cache</artifactId>
    <version>${lumen.version}</version>
</dependency>
```

```java
@Cacheable(value = "products", key = "#id")
public Product findById(Long id) { ... }

@CachePut(value = "products", key = "#product.id")
public Product update(Product product) { ... }

@CacheEvict(value = "products", key = "#id")
public void delete(Long id) { ... }
```

The default cache is in-memory (`ConcurrentHashMap`). Register a custom `CacheManager` light to use Redis or another backend.

---

## 12. Async execution

Add the async starter:

```xml
<dependency>
    <groupId>io.lumen</groupId>
    <artifactId>lumen-boot-starter-async</artifactId>
    <version>${lumen.version}</version>
</dependency>
```

```java
@Async
public CompletableFuture<Void> sendEmail(String to, String subject, String body) {
    mailSender.send(/* ... */);
    return CompletableFuture.completedFuture(null);
}

@Scheduled(fixedRate = 60_000)
public void cleanExpiredSessions() {
    sessionRepository.deleteExpired();
}
```

---

## 13. MVC with Thymeleaf

Add the Thymeleaf starter:

```xml
<dependency>
    <groupId>io.lumen</groupId>
    <artifactId>lumen-boot-starter-thymeleaf</artifactId>
    <version>${lumen.version}</version>
</dependency>
```

Use `@Controller` (not `@RestController`) and return a view name:

```java
@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(ModelAndView mav) {
        mav.addAttribute("tasks", taskService.findAll());
        return "dashboard";     // resolves to classpath:templates/dashboard.html
    }
}
```

Templates live in `src/main/resources/templates/`:

```html
<!-- src/main/resources/templates/dashboard.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
  <ul>
    <li th:each="task : ${tasks}" th:text="${task.title}"></li>
  </ul>
</body>
</html>
```

---

## 14. WebSocket

Add the WebSocket starter:

```xml
<dependency>
    <groupId>io.lumen</groupId>
    <artifactId>lumen-boot-starter-websocket</artifactId>
    <version>${lumen.version}</version>
</dependency>
```

```java
@LumenWebSocket(value = "/ws/notifications",
                allowedOrigins = {"http://localhost:5173"})
public class NotificationEndpoint implements WebSocketHandler {

    @Override
    public void onOpen(Session session) {
        SessionRegistry.add(session);
    }

    @Override
    public void onMessage(Session session, String message) {
        // handle incoming message
    }

    @Override
    public void onClose(Session session, CloseReason reason) {
        SessionRegistry.remove(session);
    }

    @Override
    public void onError(Session session, Throwable error) { }
}
```

---

## 15. Mail

Add the mail starter:

```xml
<dependency>
    <groupId>io.lumen</groupId>
    <artifactId>lumen-boot-starter-mail</artifactId>
    <version>${lumen.version}</version>
</dependency>
```

Configure in `application.properties`:

```properties
lumen.mail.host=smtp.gmail.com
lumen.mail.port=587
lumen.mail.username=you@gmail.com
lumen.mail.password=your-app-password
```

Send a simple email:

```java
@Inject
private MailSender mailSender;

SimpleMailMessage msg = new SimpleMailMessage();
msg.setTo("recipient@example.com");
msg.setSubject("Welcome");
msg.setText("Thank you for signing up.");
mailSender.send(msg);
```

Send an HTML email with an attachment:

```java
@Inject
private JavaMailSender javaMailSender;

javaMailSender.send(mimeMessage -> {
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
    helper.setTo("recipient@example.com");
    helper.setSubject("Your monthly report");
    helper.setText("<h1>Report</h1><p>See attached.</p>", true);
    helper.addAttachment("report.pdf", new File("/tmp/report.pdf"));
});
```

If `lumen.mail.host` is not set, `NoOpMailSender` is used and all messages are silently discarded — useful in development.

---

## 16. Running as a fat JAR

Activate the `fat-jar` Maven profile to create a self-contained JAR:

```bash
mvn package -Pfat-jar -DmainClass=com.example.MyApp
java -jar target/my-app-1.0.0-SNAPSHOT.jar
```

The fat-jar profile uses Maven Shade with `ServicesResourceTransformer`, which merges all `META-INF/services/*` files so the SPI module loader can discover every `LumenModule` implementation.

---

## 17. Profile-specific configuration

Create `src/main/resources/application-dev.properties` for development overrides:

```properties
lumen.datasource.url=jdbc:h2:mem:testdb
lumen.jpa.ddl-auto=create-drop
lumen.mail.host=
```

Activate it:

```properties
# application.properties
lumen.profiles.active=dev
```

Profile files override the base `application.properties`. Multiple profiles can be active: `lumen.profiles.active=dev,local`.