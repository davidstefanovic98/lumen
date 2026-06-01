# Lumen Framework — Developer Guide

**Version:** 0.1.0-SNAPSHOT

This guide is for contributors who want to extend or modify the Lumen framework itself. It covers the internal extension points, how to create new modules, how proxies work under the hood, and how to write tests for framework code.

---

## Table of Contents

1. [Module Structure](#1-module-structure)
2. [Creating a New Module](#2-creating-a-new-module)
3. [Custom LightProcessor](#3-custom-lightprocessor)
4. [Custom LumenInitializer](#4-custom-lumeninitializer)
5. [Custom ProxyProvider](#5-custom-proxyprovider)
6. [Custom CacheManager](#6-custom-cachemanager)
7. [Custom ViewResolver](#7-custom-viewresolver)
8. [Custom MethodArgumentResolver](#8-custom-methodargumentresolver)
9. [Custom ExceptionResolver](#9-custom-exceptionresolver)
10. [Working with the Event System](#10-working-with-the-event-system)
11. [Writing Tests](#11-writing-tests)
12. [Build and Release](#12-build-and-release)
13. [Proxy Internals](#13-proxy-internals)
14. [Container Bootstrap Internals](#14-container-bootstrap-internals)

---

## 1. Module Structure

Every Lumen module follows the same Maven layout:

```
lumen-my-module/
  pom.xml
  src/
    main/
      java/
        io/lumen/mymodule/
          LumenMyModule.java         — LumenModule implementation
          [annotations, classes, ...]
      resources/
        META-INF/services/
          io.lumen.core.LumenModule  — single line: io.lumen.mymodule.LumenMyModule
    test/
      java/
        io/lumen/mymodule/
          [test classes]
```

Every internal module declares `lumen-core` (or `lumen-context`) as a dependency and nothing else from the Lumen family unless strictly required. This keeps the dependency graph acyclic and modules independently testable.

A corresponding starter (`lumen-boot-starter-mymodule`) is a pom-only project that pulls in the internal module plus any third-party libraries it needs:

```xml
<!-- lumen-boot-starter-mymodule/pom.xml -->
<dependencies>
    <dependency>
        <groupId>io.lumen</groupId>
        <artifactId>lumen-boot-starter</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>io.lumen</groupId>
        <artifactId>lumen-my-module</artifactId>
        <version>${project.version}</version>
    </dependency>
    <!-- third-party dependencies here -->
</dependencies>
```

---

## 2. Creating a New Module

### Step 1 — Create the Maven module

Add the module to the parent `pom.xml`:

```xml
<modules>
    ...
    <module>lumen-my-module</module>
    <module>lumen-boot-starter-mymodule</module>
</modules>
```

Create `lumen-my-module/pom.xml`:

```xml
<project ...>
    <parent>
        <groupId>io.lumen</groupId>
        <artifactId>lumen-parent</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>
    <artifactId>lumen-my-module</artifactId>

    <dependencies>
        <dependency>
            <groupId>io.lumen</groupId>
            <artifactId>lumen-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        <!-- add lumen-context if you need classpath scanning or conditionals -->
    </dependencies>
</project>
```

### Step 2 — Implement LumenModule

```java
package io.lumen.mymodule;

import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;
import io.lumen.core.context.Environment;

@Order(5)   // pick an order that makes sense relative to existing modules
public class LumenMyModule implements LumenModule {

    @Override
    public void init(LightContainer container, String... basePackages) {
        Environment env = container.getLight(Environment.class);

        // read configuration
        String myProperty = env.getProperty("lumen.mymodule.setting", "default");

        // register beans
        container.register(MyService.class);

        // register an external instance (bypasses the normal lifecycle)
        MyClient client = new MyClient(myProperty);
        container.registerExternalInstance(MyClient.class, client);

        // add a post-processor that runs after every bean is instantiated
        container.addPostProcessor(new MyProcessor());
    }
}
```

### Step 3 — Register the SPI

Create `src/main/resources/META-INF/services/io.lumen.core.LumenModule` with a single line:

```
io.lumen.mymodule.LumenMyModule
```

### Step 4 — Add to the parent pom

Add the module entry to `pom.xml` and create a starter `pom.xml` that pulls in the module.

---

## 3. Custom LightProcessor

`LightProcessor` is a post-processor hook that runs after every bean is instantiated. Use it to wrap beans in a proxy, inject additional dependencies, or validate the bean's state.

```java
public class MyProcessor implements LightProcessor {

    @Override
    public Object afterInstantiation(LightInstance light, Object instance) {
        Class<?> type = instance.getClass();
        if (!type.isAnnotationPresent(MyAnnotation.class)) {
            return instance;    // not our concern; return unchanged
        }

        // wrap in a proxy that intercepts all methods
        return ProxyFactory.createDelegatingProxy(
            (Class<Object>) type,
            instance,
            List.of(new MyInterceptor(instance))
        );
    }
}
```

Register it in your `LumenModule.init()`:

```java
container.addPostProcessor(new MyProcessor());
```

### `afterInstantiation` vs `beforeInstantiation`

- `beforeInstantiation` — called before the bean is created; can substitute a different instance.
- `afterInstantiation` — called after creation and injection; the typical place for proxy wrapping.

### Proxy ordering

The proxy wrapping order equals the order in which processors were added. Later processors wrap earlier ones. To ensure your proxy is outermost (called first), add your processor before others — use a low `@Order` on your `LumenModule`.

---

## 4. Custom LumenInitializer

`LumenInitializer` runs after `container.initialize()` completes, when the `ServletContext` is available. Use it to register servlets, listeners, or perform tasks that require fully initialized beans.

```java
@Order(10)
public class MyInitializer implements LumenInitializer {

    private final MyService myService;

    public MyInitializer(MyService myService) {
        this.myService = myService;
    }

    @Override
    public void onStartup(ServletContext servletContext) throws Exception {
        myService.warmup();
        servletContext.addListener(new MySessionListener());
    }
}
```

Register it as a bean in your `LumenModule.init()`:

```java
container.register(MyInitializer.class);
```

The container resolves `MyInitializer`'s constructor dependencies automatically. `LumenInitializer` beans are detected and called in `@Order` order inside `LumenServletContainerInitializer.onStartup()`.

---

## 5. Custom ProxyProvider

`ProxyProvider` is the SPI for proxy creation. The default implementation uses ByteBuddy. You can replace it or add an alternative:

```java
public class MyProxyProvider implements ProxyProvider {

    @Override
    public <T> T createDelegatingProxy(Class<T> type, T delegate,
                                        List<MethodInterceptor> interceptors) {
        // return a proxy that delegates to `delegate` and applies `interceptors`
    }

    @Override
    public <T> T createLazyProxy(Class<T> type, Supplier<T> supplier) {
        // return a proxy that lazily initialises via `supplier`
    }

    // ... other methods
}
```

Register in `META-INF/services/io.lumen.core.proxy.ProxyProvider`. If multiple providers are available, the framework uses the first one discovered.

---

## 6. Custom CacheManager

Register a `CacheManager` bean before `LumenCacheModule` reads it. Because `LumenCacheModule` runs at `@Order(-1)`, register your custom manager in a module with an even lower order (e.g., `@Order(-2)`), or as an external instance in a `@Configuration` class processed before modules run.

```java
@Configuration
public class CacheConfig {

    @Light
    public CacheManager redisCacheManager() {
        return new RedisCacheManager(redisConnectionFactory());
    }
}
```

`LumenCacheModule` checks `container.hasLight(CacheManager.class)` before creating `SimpleCacheManager`. If a `CacheManager` is already registered, it uses that one.

---

## 7. Custom ViewResolver

Implement `ViewResolver` from `lumen-web-mvc` and register it as a bean. The framework calls it from `ViewResultHandler` when a controller method returns a `String` or `ModelAndView`.

```java
@Light
public class FreemarkerViewResolver implements ViewResolver {

    private final freemarker.template.Configuration freemarker;

    public FreemarkerViewResolver(freemarker.template.Configuration freemarker) {
        this.freemarker = freemarker;
    }

    @Override
    public void resolve(String viewName, Map<String, Object> model,
                        HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Template template = freemarker.getTemplate(viewName + ".ftl");
        resp.setContentType("text/html;charset=UTF-8");
        template.process(model, resp.getWriter());
    }
}
```

---

## 8. Custom MethodArgumentResolver

Implement `MethodArgumentResolver` and add it to the composite resolver chain.

```java
public class PrincipalArgumentResolver implements MethodArgumentResolver {

    @Override
    public boolean supportsParameter(Parameter parameter) {
        return parameter.isAnnotationPresent(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(Parameter parameter,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getPrincipal() : null;
    }
}
```

Wire it in a `LumenInitializer` or via a hook in `WebLumenInitializer`. The simplest approach is to register a `LumenInitializer` bean that injects the `CompositeMethodArgumentResolver` and calls `addResolver()`.

---

## 9. Custom ExceptionResolver

Implement `ExceptionResolver` and add it to the composite resolver chain via `LumenWebModule` or a custom `LumenInitializer`.

```java
public class MyExceptionResolver implements ExceptionResolver {

    @Override
    public boolean resolve(HttpServletRequest req, HttpServletResponse resp,
                           Exception ex) throws IOException {
        if (!(ex instanceof MyDomainException mde)) return false;

        resp.setStatus(mde.getHttpStatus().value());
        resp.setContentType("application/json");
        resp.getWriter().write("{\"error\":\"" + mde.getMessage() + "\"}");
        return true;
    }
}
```

`resolve()` must return `true` to signal that it handled the exception (preventing further resolvers from running), or `false` to pass it on.

---

## 10. Working with the Event System

### Publishing events

Inject `ApplicationEventPublisher` anywhere in the application:

```java
@Inject
private ApplicationEventPublisher publisher;

public void processOrder(Order order) {
    // business logic ...
    publisher.publishEvent(new OrderProcessedEvent(this, order));
}
```

### Writing event listeners

Annotate a method in any `@Light`-managed bean with `@EventListener`. The parameter type determines which events it receives:

```java
@EventListener
public void onOrderProcessed(OrderProcessedEvent event) {
    notificationService.notify(event.getOrder().getUserId(), "Your order is processed.");
}
```

Events are dispatched synchronously in the publishing thread by default. For fire-and-forget behaviour, combine with `@Async`:

```java
@EventListener
@Async
public void onOrderProcessed(OrderProcessedEvent event) {
    // runs on the async thread pool; publisher returns immediately
}
```

### How it works internally

`ApplicationEventMulticaster` is registered as an external instance of both `ApplicationEventMulticaster` and `ApplicationEventPublisher` in `AnnotationApplicationContext.registerDefaultProcessors()`. `EventListenerProcessor` (a `LightProcessor`) scans every instantiated bean for `@EventListener` methods and registers them with the multicaster. When `publishEvent()` is called, the multicaster iterates all registered listeners whose event type is assignable from the published event and invokes them.

---

## 11. Writing Tests

### Container tests

Use `LightContainer` directly to test bean wiring:

```java
@Test
void injectionWorks() {
    LightContainer container = new LightContainer();
    container.register(ServiceA.class);
    container.register(ServiceB.class);
    container.initialize();

    ServiceA a = container.getLight(ServiceA.class);
    assertNotNull(a);
}
```

### Integration tests with AnnotationApplicationContext

Use `AnnotationApplicationContext` to test the full scan-and-boot lifecycle without a web server:

```java
@Test
void configurationIsPickedUp() {
    AnnotationApplicationContext ctx = new AnnotationApplicationContext();
    ctx.scan(MyConfig.class);
    ctx.initialize();

    MyService svc = ctx.getLight(MyService.class);
    assertNotNull(svc);
}
```

### Testing web behaviour

`lumen-web`'s test suite uses a real `AnnotationWebApplicationContext` started on a random port (`server.port=0`). Retrieve the bound port via `WebServer.getBoundPort()` and use `java.net.http.HttpClient` to make real HTTP requests:

```java
@BeforeAll
static void startServer() throws Exception {
    System.setProperty("server.port", "0");
    ctx = new AnnotationWebApplicationContext();
    ctx.scan(TestConfig.class);
    ctx.startWebServer();
    port = ctx.getWebServer().getBoundPort();
}

@Test
void returnsHello() throws Exception {
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + port + "/hello"))
        .GET().build();
    HttpResponse<String> resp = client.send(req, BodyHandlers.ofString());
    assertEquals(200, resp.statusCode());
    assertEquals("\"Hello\"", resp.body());
}
```

### Testing proxied beans

Test proxied behaviour through the public interface:

```java
@Test
void cacheable_returnsCachedValue() {
    AnnotationApplicationContext ctx = new AnnotationApplicationContext();
    ctx.scan(CacheTestConfig.class);
    ctx.initialize();

    MyService svc = ctx.getLight(MyService.class);
    String first  = svc.expensive("x");
    String second = svc.expensive("x");
    assertSame(first, second);   // same instance → cache hit
}
```

### Mail tests with GreenMail

```java
private GreenMail greenMail;

@BeforeEach
void setUp() {
    greenMail = new GreenMail(new ServerSetup(2525, null, ServerSetup.PROTOCOL_SMTP));
    greenMail.start();
}

@Test
void sendsEmail() throws Exception {
    // configure JavaMailSenderImpl pointing at localhost:2525
    // send message
    greenMail.waitForIncomingEmail(1);
    MimeMessage[] messages = greenMail.getReceivedMessages();
    assertEquals(1, messages.length);
    assertEquals("Expected Subject", messages[0].getSubject());
}

@AfterEach
void tearDown() { greenMail.stop(); }
```

### Test conventions

- Use JUnit 5 (`@Test`, `@BeforeEach`, etc.).
- Each test module has its own test config class annotated with `@Configuration`.
- Do not share container instances between tests unless they are `@BeforeAll`/`static`.
- The `-parameters` compiler flag is inherited from the parent `pom.xml` — parameter names are available in tests.

---

## 12. Build and Release

### Full build

```bash
mvn clean install
```

Runs compile, test, and installs all modules to the local Maven repository in dependency order.

### Build a single module

```bash
mvn clean install -pl lumen-core
mvn clean install -pl lumen-cache -am     # -am also builds dependencies
```

### Run tests for one module

```bash
mvn test -pl lumen-core
mvn test -pl lumen-cache
mvn test -pl lumen-web
```

### Run the demo application

Requires PostgreSQL on `localhost:5432` (configured in `lumen-demo/src/main/resources/application.properties`):

```bash
cd ../lumen-demo && mvn exec:exec
```

Debug mode (port 5005):

```bash
cd ../lumen-demo && mvn exec:exec -Pdebug
```

### Fat JAR

```bash
cd ../lumen-demo
mvn package -Pfat-jar -DmainClass=io.lumen.demo.DemoApp
java -jar target/lumen-demo-*.jar
```

The `fat-jar` Maven Shade profile is defined in the parent `pom.xml` and is inherited by all modules. It uses `ServicesResourceTransformer` to merge `META-INF/services/*` files, which is essential for `ServiceLoader` to discover all `LumenModule` implementations in the fat JAR.

---

## 13. Proxy Internals

### Five proxy strategies

`ByteBuddyProxyProvider` (in `lumen-aop`) implements five distinct proxy strategies. Choosing the wrong one for a use case can cause subtle bugs:

| Strategy | Mechanism | Use case |
|---|---|---|
| `createLazyProxy` | ByteBuddy subclass; interceptor defers real bean creation | `@Lazy` injection |
| `createAopProxy` | ByteBuddy subclass; general interceptor chain | General AOP |
| `createConfigurationProxy` | ByteBuddy subclass; intercepts `@Light` method calls | `@Configuration` caching |
| `createInterfaceProxy` | ByteBuddy implements interface(s) | JPA repository interfaces |
| `createDelegatingProxy` | ByteBuddy subclass; all calls forwarded to the real instance | Cross-cutting processors |

### Why `createDelegatingProxy` uses Unsafe

When wrapping a bean that was created by constructor injection (no no-argument constructor), the proxy subclass cannot be instantiated with `getDeclaredConstructor().newInstance()` because the constructor requires arguments. The framework falls back to `sun.misc.Unsafe.allocateInstance()`, which allocates the object on the heap without invoking any constructor. The uninitialized fields of the proxy class itself are irrelevant because every method call is forwarded to the real delegate.

### Annotation resolution in delegating proxies

`MethodInterceptor` implementations that need to read annotations (e.g., `@Cacheable`, `@PreAuthorize`) must resolve the annotation from the real target class, not from the proxy class. This works correctly because `CacheProcessor` and `MethodSecurityProcessor` run before `TransactionalProcessor` — when they wrap a bean, the bean is still the original class with annotations intact. By the time `TransactionalProcessor` runs, the target passed to it is the cache/security proxy, not the original. `TransactionalInterceptor` reads annotations from the stored real target reference, not from `method.getDeclaringClass()`.

### Exception unwrapping

`method.invoke(target, args)` wraps any exception thrown by the real method in `InvocationTargetException`. Without unwrapping, user `catch (MyException e)` blocks would never fire because the actual exception is one level deeper. Both `RouteInvoker` and `ByteBuddyLazyInterceptor` catch `InvocationTargetException` and rethrow `e.getCause()`.

---

## 14. Container Bootstrap Internals

### External instance vs. registered bean

`registerExternalInstance(type, instance)` stores a bean in the `READY` state, bypassing the normal creation lifecycle. This is used by modules to inject pre-built objects (e.g., `Environment`, `ApplicationEventPublisher`, `ServletContext`) before `container.initialize()` is called.

`register(clazz)` stores a `LightDefinition` in the `PENDING` state. The bean is not instantiated until `container.initialize()` is called, or until another bean requests it as a dependency.

The `READY` state also means that `container.getLight(Environment.class)` works during `LumenModule.init()` even though the container is not yet initialized. This is how modules read properties.

### Why modules run before initialize()

`ModuleInitializer` calls `module.init()` before `container.initialize()`. This is intentional: modules only *register* beans and processors during `init()` — they do not *instantiate* them. Instantiation happens in `container.initialize()`, which respects all `@Order` constraints and runs the full processor chain. If modules called `getLight()` for application beans during `init()`, those beans would be instantiated before the processor chain was fully assembled, so proxy wrapping would be incomplete.

### Circular dependency detection

`LightResolver` maintains a `Set<Class<?>> resolutionInProgress` (thread-local). When it starts resolving a bean, it adds the type to the set. If the same type is encountered again before resolution completes, `CircularDependencyException` is thrown. `@Lazy` injection points break cycles — the injected proxy does not trigger resolution of the lazy bean at `initialize()` time.

### `@Configuration` proxy caching

When `@Configuration` is processed, `ConfigProcessor` registers the original class and then creates a ByteBuddy subclass proxy via `createConfigurationProxy()`. This proxy overrides every `@Light`-annotated method to check whether the container already holds an instance before calling the real method. Without this, two beans that both declare `@Inject UserService` via separate `@Light` factory methods in the same `@Configuration` would receive different instances.