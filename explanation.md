### Table of contents

1) Big picture

2) Modules and structure

3) Architecture (layers + adapters)

4) Request flow (success and error)

5) Domain model, DTOs, and mapping

6) Validation groups

7) Ports and adapters

8) Persistence and data access

9) Error handling and error envelope

10) TraceId and logging

11) Virtual threads

12) Observability (Actuator)

13) CORS

14) API key security

15) Pagination

16) Swagger/OpenAPI

17) Environment management and profiles

18) Docker and packaging

19) Tests

20) How all additions interact

21) Growth paths and improvements

---

### Explanation: Item Detail API (Spring Boot) - Study Notes

This document is a deep-dive into the project architecture, implementation,

and all the additions we introduced. It is intentionally detailed for study.

---

### 1) Big picture

This project is a Spring Boot service that manages "models" (items) with a

simple CRUD-style API.

It was evolved into a microservice-style multi-module

Maven structure and improved with:

- DTOs and mapping
- Validation groups
- Ports and adapters
- A centralized error envelope
- TraceId logging
- Optional API key security
- Observability with Actuator
- Pagination endpoint
- CORS configuration
- Swagger/OpenAPI documentation

The goal is to keep the API clean, structured, and easy to extend.

---

### 2) Modules and structure

We moved from a single-module layout to a multi-module Maven layout.

**Repo layout:**

```
/
  pom.xml                    (parent)
  model-service/
    pom.xml                  (microservice)
    src/main/java/...         (application code)
    src/main/resources/...    (YAML config)
    src/test/...              (tests)
```

This allows:

- Adding more microservices later without mixing dependencies
- Clean separation of concerns and easier scaling

---

### 3) Architecture (layers + adapters)

We use a layered architecture with ports/adapters:

**Layers**

- Controller: HTTP entry point
- DTO/Mapper: transform API objects to entities
- Service: business rules
- Port: interfaces for persistence
- Adapter: implementation for persistence (JPA)
- Repository: Spring Data JPA

**ASCII diagram (general architecture)**

```
Client
  |
  v
Controller (HTTP)
  |
  v
DTO/Mapper
  |
  v
Service (business)
  |
  v
Port (interface) <----> Adapter (JPA) ----> Repository (Spring Data)
  |
  v
Database (H2)
```

This design gives flexibility:

- You can swap persistence (JPA -> other) by changing the adapter
- You can mock the port for unit tests

---

### 4) Request flow (success and error)

**Success flow**

1) Request arrives

2) TraceIdFilter injects X-Request-Id if missing:

They provide request traceability and log correlation. In model-service/src/main/java/com/hackerrank/sample/config/
TraceIdFilter.java the filter:

- Reads X-Request-Id from the incoming request; if missing, generates a UUID.
- Stores it in the MDC under traceId so it shows up in logs.
- Sets the same X-Request-Id on the response.
- Cleans the MDC after the request to avoid leaking context across threads.

This ties into model-service/src/main/resources/application.yml, where the log pattern includes [traceId=%X{traceId}] so every log line is tied to a specific request.

3) Controller validates request body:

The controller validates request bodies via Spring’s Bean Validation on the @RequestBody parameter. In model-service/
src/main/java/com/hackerrank/sample/controller/ModelController.java, the createNewModel method uses
@Validated(ValidationGroups.Create.class) on ModelRequest, which triggers group-based validation for create requests.

The actual constraints live on the DTO in model-service/src/main/java/com/hackerrank/sample/dto/ModelRequest.java: id
is @NotNull and name is @NotBlank, both scoped to the Create group. If validation fails, Spring throws
MethodArgumentNotValidException, and model-service/src/main/java/com/hackerrank/sample/exception/
GlobalExceptionHandler.java formats a 400 BAD_REQUEST response with a VALIDATION_ERROR code and field-specific
messages. Invalid JSON bodies are handled separately as INVALID_JSON.

ValidationGroup class:

ValidationGroups defines validation groups so you can apply different constraint sets depending on the use case
(create, update, etc.). In this project it lets the controller trigger only the constraints tagged with a specific
group via @Validated(ValidationGroups.Create.class), so a field can be required for create but not for other
operations. In short: it enables reusing the same DTO with different validation rules per operation.

4) Mapper converts DTO -> Entity

5) Service executes business rules

6) Repository saves or fetches data

7) Controller returns ResponseEntity with a DTO

**Error flow**

1) Service/Controller throws domain exceptions

2) GlobalExceptionHandler intercepts

3) ErrorResponse is returned with code and traceId

**ASCII flow**

```
HTTP Request
  |
  v
TraceIdFilter -> Controller -> Mapper -> Service -> Adapter -> JPA -> H2
  |
  v
Response (success or error envelope)
```

---

### 5) Domain model, DTOs, and mapping

**Entity:**

- Model (JPA entity)

**DTOs:**

- ModelRequest: incoming payload
- ModelResponse: outgoing payload

**Mapper:**

- ModelMapper converts between ModelRequest/ModelResponse and Model

Why this helps:

- Keeps API independent from database schema
- Prevents leaking entity changes to clients
- Makes future changes safer (versioned APIs, custom responses)

---

### 6) Validation groups

Validation groups let you apply different validation rules for different

operations without duplicating DTOs.

We created:

- ValidationGroups.Create
- ValidationGroups.Update (available for future use)

**Example:**

- @Validated(ValidationGroups.Create.class) on POST /model

This means:

- id and name are required for Create
- You can relax or change rules for Update later

---

### 7) Ports and adapters

**Port:**

- ModelRepositoryPort: interface with CRUD and pagination methods

**Adapter:**

- ModelRepositoryAdapter: uses Spring Data JPA under the hood

Why this helps:

- Decouples service from JPA implementation details
- Simplifies testing (mock the port)
- Helps future DB migrations or multiple data sources

---

### 8) Persistence and data access

- H2 in-memory database is used by default
- Spring Data JPA provides repository capabilities
- The adapter implements the port and delegates to JPA

This is lightweight for development and easy to swap to PostgreSQL/MySQL later.

---

### 9) Error handling and error envelope

GlobalExceptionHandler produces a standard ErrorResponse:

**Fields:**

- status: HTTP status code
- error: HTTP reason (Bad Request, Not Found)
- code: internal error code
- message: human readable message
- path: request path
- traceId: request correlation id

This makes clients easier to implement because errors are always predictable.

**Example:**

```json
{
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "id: id is required; name: name is required",
  "path": "/model",
  "traceId": "..."
}
```

---

### 10) TraceId and logging

TraceIdFilter:

- Reads X-Request-Id or generates a UUID
- Puts it in MDC so logs include it:
    
    MDC stands for Mapped Diagnostic Context. It’s a per-thread context map used by SLF4J/Logback to enrich logs with
    request-scoped data like a traceId. The TraceIdFilter puts the traceId into the MDC, the log pattern prints it
    automatically, and it’s removed at the end of the request to avoid leaking context to other requests.
    
- Returns the same header to the client

Benefits:

- Helps trace a request across logs
- Useful in distributed systems or when debugging issues

---

### 11) Virtual threads (Java 21)

Virtual threads reduce the cost of handling concurrent I/O workloads.

- Lower memory overhead per request
- Lower context-switch cost
- Better scalability for high concurrency

This is especially helpful for I/O-heavy services.

---

### 12) Observability (Actuator)

We enabled Actuator endpoints:

- /actuator/health
- /actuator/info
- /actuator/metrics

These provide a baseline for monitoring and diagnostics.

---

### 13) CORS

We added optional CORS config:

- Controlled by APP_CORS_ALLOWED_ORIGINS
- If empty, CORS is not enabled

This is safer because it only opens CORS when needed.

---

### 14) API key security (optional)

Simple API key security:

- If APP_API_KEY is empty: security disabled (safe for tests/Hackerrank)
- If APP_API_KEY is set: requests must include X-API-Key

**Public endpoints:**

- /, /swagger-ui/**, /v3/api-docs/**, /actuator/health, /h2-console/

This adds protection without needing full OAuth2.

---

### 15) Pagination

**Endpoint:**

- GET /model/page?page=0&size=2&sort=id,desc

Why it matters:

- Smaller responses
- Lower memory usage
- Faster clients

**Example response:**

```json
{
  "content": [{ "id": 5, "name": "model-000-000-005" }],
  "totalElements": 5,
  "totalPages": 3,
  "size": 2,
  "number": 0
}
```

---

### 16) Swagger/OpenAPI

- Swagger UI: /swagger-ui/index.html
- OpenAPI JSON: /v3/api-docs

We annotate controllers to keep API documentation clear and up to date.

---

### 17) Environment management and profiles

Configuration:

For a developer who just downloaded the project in IntelliJ Community, the simplest setup is to configure environment
variables in the Run/Debug configuration:

- Import the Maven project and wait for indexing.
- Go to Run > Edit Configurations... and create/select a Spring Boot or Application config (main class
com.hackerrank.sample.Application).
- In “Environment variables” add the essentials:
    - SPRING_PROFILES_ACTIVE=dev
    - Optional DB: SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD (otherwise it uses
    the default in-memory H2).
    - Optional CORS: APP_CORS_ALLOWED_ORIGINS=http://localhost:3000
    - Optional security: APP_API_KEY=... and APP_API_KEY_HEADER=X-API-Key
- Run the app.

Note: per [README.md](http://readme.md/), Spring Boot doesn’t load .env automatically, so setting env vars in IntelliJ is the recommended
path.

We rely on environment variables and Spring profiles:

**Example variables:**

- SPRING_DATASOURCE_URL
- SPRING_DATASOURCE_USERNAME
- SPRING_DATASOURCE_PASSWORD
- APP_CORS_ALLOWED_ORIGINS
- APP_API_KEY
- APP_API_KEY_HEADER

**Profiles:**

- dev -> application-dev.yml
- test -> application-test.yml

**Activate with:**

- SPRING_PROFILES_ACTIVE=dev

**Templates:**

- .env.example
- .env.test.example

These are not automatically loaded by Spring, but provide a guide.

---

### 18) Docker and packaging

Dockerfile builds the model-service module:

- Maven build stage
- Java runtime stage

docker-compose.yml runs the service:

- Exposes port 8080
- Sets SPRING_PROFILES_ACTIVE and CORS origins

This makes deployment and local testing consistent.

---

### 19) Tests

Unit tests cover:

- Service logic (ModelServiceImplTest)
- Error handler behavior (GlobalExceptionHandlerTest)

Tests validate:

- Business rule errors (bad id, duplicates)
- Error response format
- Pagination call path

---

### 20) How all additions interact

**Combined flow:**

1) Request arrives

2) TraceIdFilter sets X-Request-Id and MDC

3) CORS config allows/rejects cross-origin if configured

4) Security filter checks API key if enabled

5) Controller handles DTOs and validation groups

6) Mapper converts DTO -> Entity

7) Service uses repository port

8) Adapter delegates to JPA repository

9) DB returns data

10) Controller returns ResponseEntity with DTO

11) Errors are centralized with a uniform JSON response

12) Logs include traceId for troubleshooting

This integration keeps the system consistent, testable, and easy to extend.

---

### 21) Growth paths and improvements

**Where to go next:**

- Split into more microservices (auth-service, inventory-service)
- Replace API key with JWT or OAuth2
- Add caching (Redis) for frequently accessed models
- Add migrations (Flyway/Liquibase) for real databases
- Add contract testing (Pact) for API version stability
- Add distributed tracing (OpenTelemetry)

**Why this design:**

- Clear separation of concerns
- Strong testability via ports
- Simple evolution path into full microservice architecture

**Possible improvements:**

- Replace H2 with PostgreSQL in prod
- Add async messaging (Kafka/RabbitMQ)
- Add rate limiting at the gateway level

## OAuth2 in Spring Boot

OAuth 2.0 is an authorization framework that lets a user grant a third-party application limited access to a protected resource without sharing credentials.

It does not define how users authenticate (that is OpenID Connect), but rather how an access token is issued and used to authorize API calls.

### Key actors

- Resource Owner: the end user who owns the data.
- Client: the application that wants access to the data.
- Authorization Server: the system that authenticates the user and issues tokens.
- Resource Server: the API that accepts tokens and serves protected data.

### Core tokens

- Access Token: short-lived token used to call the API.
- Refresh Token: long-lived token used to get a new access token without re-authenticating.

### Common OAuth2 flows (grant types)

- Authorization Code (with PKCE): the recommended flow for browser and mobile apps. User logs in at the authorization server, the client exchanges a code for tokens.
- Client Credentials: server-to-server access with no user.
- Device Code: device without a browser.
- Deprecated/legacy: Implicit and Password grants are no longer recommended.

### Security properties

- The client never sees the user password.
- Tokens are scoped to limit access (for example, "read:items").
- Tokens expire, reducing risk if compromised.

## How OAuth2 is implemented in Spring Boot

Spring Security splits responsibilities into:

- OAuth2 Client: if your app logs in users through a provider (Google, Auth0, Keycloak, etc.).
- OAuth2 Resource Server: if your app exposes APIs that validate access tokens.
- Authorization Server: can be implemented using Spring Authorization Server, but it is typically a separate service.

Below is a practical approach for a typical API:

1) Use a third-party Authorization Server (Auth0, Keycloak, Okta, etc.).

2) Configure your Spring Boot API as an OAuth2 Resource Server.

3) Validate JWT tokens and map scopes/roles to Spring Security authorities.

### Example: Resource Server with JWT

Add dependencies (Maven):

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

Configure the issuer or JWK set URI (application.yml):

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://issuer.example.com/
          # or jwk-set-uri: https://issuer.example.com/.well-known/jwks.json
```

Define security rules:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
  http
    .authorizeHttpRequests(auth -> auth
      .requestMatchers("/", "/actuator/health").permitAll()
      .requestMatchers("/admin/**").hasAuthority("SCOPE_admin")
      .anyRequest().authenticated()
    )
    .oauth2ResourceServer(oauth2 -> oauth2.jwt());

  return http.build();
}
```

How it works:

- The API receives a request with an Authorization header:
    
    `Authorization: Bearer <access_token>`.
    
- Spring Security validates the JWT using the provider's public keys.
- If valid, the request is authenticated and scopes map to authorities like `SCOPE_read` or `SCOPE_admin`.

### Example: OAuth2 Client (Login)

If your app needs user login through a provider, add:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

Then configure client registration:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          myprovider:
            client-id: your-client-id
            client-secret: your-client-secret
            scope: openid,profile,email
        provider:
          myprovider:
            issuer-uri: https://issuer.example.com/
```

This enables a login flow and the app can access user info from the provider.

## Mapping scopes/claims to application roles

Tokens contain claims (JSON fields). Common ones:

- sub: user id
- iss: issuer
- aud: audience
- scope: list of permissions

You can map claims to authorities:

```java
JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
converter.setJwtGrantedAuthoritiesConverter(jwt -> {
  Collection<GrantedAuthority> authorities = new ArrayList<>();
  String scope = jwt.getClaimAsString("scope");

  if (scope != null) {
    for (String s : scope.split(" ")) {
      authorities.add(new SimpleGrantedAuthority("SCOPE_" + s));
    }
  }

  return authorities;
});
```

Then plug it into the resource server:

```java
http.oauth2ResourceServer(oauth2 -> oauth2
  .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
```

## Refresh tokens and token lifetime

OAuth2 access tokens are short-lived. The refresh token is exchanged at the authorization server to obtain a new access token. In an API-only resource server, you typically do not handle refresh tokens directly; the client does.

## Authorization Server (optional)

If you must host your own OAuth2 provider, use Spring Authorization Server:

- It issues tokens, handles consent, and exposes OIDC endpoints.
- This is usually a separate application from your API.

Typical deployment:

- Authorization Server (identity)
- Resource Server (your API)
- Client app (frontend or other service)

## Implementation steps checklist

1) Pick an authorization server (Auth0, Keycloak, Okta, Azure AD, etc.).

2) Register your client and define scopes.

3) Configure your Spring Boot API as a resource server.

4) Secure endpoints with scopes or roles.

5) Validate tokens and map claims to authorities.

6) Test with real tokens (curl or Postman).

## Common pitfalls

- Using Implicit flow: avoid it, use Authorization Code + PKCE.
- Missing audience validation: ensure tokens are meant for your API.
- Skipping HTTPS: tokens must be transmitted over TLS.
- Overly broad scopes: keep least privilege.
- Logging tokens: treat them as secrets. Borra el explanation.md y agrégalo con esta información, bien organizado. Por favor, tal cual está allí.
