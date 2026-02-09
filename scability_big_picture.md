# Scalability Big Picture

Based on `explanation.md`, this document shows (with ASCII console diagrams)
how the project would look after adding the improvements and growth features.

---

## 1) Evolution from the current state

```
[Today]
Client
  |
  v
Model Service (Spring Boot)
  |
  v
H2 (in-memory)
```

```
[Scalable]
Client
  |
  v
Load Balancer
  |
  v
API Gateway / Edge
  |
  +--> Catalog Service (Models)
  +--> Inventory Service
  +--> Pricing Service
  +--> Auth Service (if needed)
```

---

## 2) Target architecture (services + data + cache + events)

```
                         +-------------------+
                         |  Observability    |
                         |  Metrics/Tracing  |
                         +-------------------+

Client
  |
  v
Load Balancer
  |
  v
API Gateway / Edge
  |
  +----------------------------+----------------------------+
  |                            |                            |
  v                            v                            v
Catalog Service           Inventory Service             Pricing Service
  |                            |                            |
  v                            v                            v
PostgreSQL                 PostgreSQL                 PostgreSQL
  |
  +-----> Redis (cache)
  |
  +-----> RabbitMQ (events)
```

Key notes:
- Each service owns its database.
- Redis accelerates hot reads.
- RabbitMQ decouples flows and supports eventual consistency.

---

## 3) Synchronous flow (request/response)

```
Client
  |
  v
Load Balancer -> API Gateway
  |
  v
Catalog Service
  |
  +--> Redis (cache hit?)
  |
  +--> PostgreSQL (cache miss)
  |
  v
Response
```

---

## 4) Asynchronous flow (events)

```
Catalog Service (POST /model)
  |
  v
DB Commit
  |
  v
Publish Event: ModelCreated
  |
  v
RabbitMQ Exchange
  |
  +--> Inventory Service (consume)
  |
  +--> Pricing Service (consume)
  |
  +--> Analytics/Reporting (future)
```

---

## 4.1) Messaging: where RabbitMQ sits and how it connects

```
            (async events)
Catalog Service  --->  RabbitMQ  --->  Inventory Service
      |                 |                 |
      |                 +---------------> Pricing Service
      |                 |
      +---------------> (event publisher)
```

Event examples:
- ModelCreated
- ModelDeleted
- InventoryUpdated

---

## 4.2) RabbitMQ internals (exchange -> queues -> consumers)

```
               routing key: model.*
Catalog Service
  |
  v
Exchange (topic)
  |
  +--> Queue: inventory.events  ---> Inventory Service (consumer)
  |
  +--> Queue: pricing.events    ---> Pricing Service (consumer)
  |
  +--> Queue: analytics.events  ---> Analytics/Reporting (consumer)
```

Notes:
- Each service has its own queue (isolated processing).
- The exchange routes to multiple queues by routing key.

---

## 5) Strategic cache (hot reads)

```
GET /model/{id}
  |
  v
Redis (TTL 1-5 min)
  |
  +--> HIT  -> return cached
  |
  +--> MISS -> DB -> store in cache -> return
```

Invalidation:
- POST/PUT/DELETE invalidate the related key.

---

## 6) Security and traffic control

```
Client
  |
  v
Load Balancer (TLS termination)
  |
  v
API Gateway
  |
  +--> AuthN/AuthZ (JWT/OAuth2)
  +--> Rate Limiting
  +--> CORS
```

---

## 7) Observability and tracing

```
Request
  |
  v
TraceId / CorrelationId
  |
  v
Logs + Metrics + Tracing
  |
  +--> Centralized Logs (ELK/Cloud)
  +--> Metrics (Prometheus/Cloud)
  +--> Traces (OpenTelemetry)
```

---

## 8) CI/CD (Azure DevOps -> VM)

```
Developer -> Git
  |
  v
Azure DevOps Pipeline
  |
  +--> Build & Tests
  +--> Docker Build
  +--> Push to ACR
  |
  v
VM (Docker Compose)
  |
  v
Rolling / Blue-Green Deployment
```

---

## 9) Adoption path (phases)

```
Phase 1: PostgreSQL + Migrations
Phase 2: Redis cache
Phase 3: Split into 2 services (Catalog + Inventory)
Phase 4: RabbitMQ events
Phase 5: Load Balancer + multi-instance
Phase 6: CI/CD pipeline
```

---

## 10) Final integrated view (big picture)

```
                              +---------------------------+
                              |   CI/CD (Azure DevOps)    |
                              | Build -> ACR -> VM Deploy |
                              +---------------------------+

Client
  |
  v
Load Balancer
  |
  v
API Gateway / Edge
  |
  +--------------------+--------------------+--------------------+
  |                    |                    |                    |
  v                    v                    v                    v
Catalog Service   Inventory Service    Pricing Service      Auth Service
  |                    |                    |                    |
  v                    v                    v                    v
PostgreSQL         PostgreSQL         PostgreSQL         (IdP/DB)
  |                    |                    |
  +---------+----------+---------+----------+
            |                    |
            v                    v
         Redis                RabbitMQ
            |                    |
            +-------> Event-driven updates
```

---

## 11) Summary benefits

- Scalability: independent services, cache, load balancing.
- Reliability: async events, resilience, retries.
- Performance: Redis + PostgreSQL.
- Agility: CI/CD and repeatable deployments.
- Safer evolution: phased adoption.

