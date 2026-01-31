# Item Detail API (Spring Boot)

Backend API para manejo de modelo item usando Spring Boot 3.2.5 and Java 21, en una arquitectura en Spring boot MVC.

Indice rapido:
- Manejo de environments -> ver seccion "Manejo de environments".
- Microservicio -> ver seccion "Microservicio (estructura multi-modulo)".
- Seguridad -> ver seccion "Seguridad (API Key)".


## Arquitectura

Este proyecto sigue una arquitectura en capas clasica:
- Controller (HTTP): `ModelController` expone endpoints REST y valida el payload.
- DTO/Mapper: `ModelRequest`, `ModelResponse` y `ModelMapper` separan API de persistencia.
- Service (negocio): `ModelService` y `ModelServiceImpl` contienen reglas y validaciones de dominio.
- Port/Adapter: `ModelRepositoryPort` abstrae datos y `ModelRepositoryAdapter` conecta con JPA.
- Repository (datos): `ModelRepository` usa Spring Data JPA para acceso a la base.
- Model (entidad): `Model` define el esquema JPA con validaciones basicas.
- Manejo de errores: `GlobalExceptionHandler` centraliza excepciones y estandariza la respuesta.

Arquitectura general (capas + adaptadores) en detalle:

- Capa HTTP: recibe requests, valida, y arma respuestas con `ResponseEntity`.
- Capa DTO/Mapper: traduce entre modelos de API y entidades JPA.
- Capa de negocio: concentra reglas y casos de uso.
- Puerto (interface): define contratos para persistencia y facilita tests.
- Adaptador: implementa el puerto usando Spring Data JPA.
- Infraestructura: base de datos H2 y componentes transversales (logging, tracing).

Flujo de una solicitud:

1) Controller recibe el request y valida el body con `@Valid`.
2) Service aplica reglas (por ejemplo, id requerido, id duplicado).
3) Repository persiste en H2 via JPA.
4) Errores de negocio se convierten a una respuesta JSON uniforme.
5) Un filtro asigna `X-Request-Id` para trazabilidad.

Persistencia:

- Base en memoria H2 (por defecto).
- JPA (Hibernate) manejan el mapeo y el ciclo de vida de entidades.

Configuracion:

- `model-service/src/main/resources/application.yml` define virtual threads, logging y la consola H2.
- Java 21 y Spring Boot 3.2.5.

## Diagramas de flujo, arquitectura y diseno

Arquitectura general (capas + adaptadores):
```
Cliente HTTP
    |
    v
+------------------+     +------------------+
|  TraceIdFilter   |---->| GlobalException  |
| (X-Request-Id)   |     |     Handler      |
+--------+---------+     +--------+---------+
         |                        |
         v                        |
 +--------------------+           |
 |  ModelController   |-----------+
 +---------+----------+
           |
           v
 +--------------------+       +--------------------+
 |   DTO / Mapper     |       | Validation Groups  |
 | ModelRequest/Resp  |       | Create/Update      |
 +---------+----------+       +--------------------+
           |
           v
 +--------------------+       +--------------------+
 |  ModelServiceImpl  |<----->| ModelService Port  |
 +---------+----------+       +--------------------+
           |
           v
 +--------------------+       +--------------------+
 | ModelRepository    |<----->| ModelRepository    |
 |   Adapter          |       | Port (interface)   |
 +---------+----------+       +--------------------+
           |
           v
 +--------------------+
 | JPA / Hibernate    |
 +---------+----------+
           |
           v
 +--------------------+
 |      H2 DB         |
 +--------------------+
```

Flujo de request con respuesta exitosa:
```
HTTP Request
   |
   v
TraceIdFilter (asigna X-Request-Id)
   |
   v
Controller -> Mapper -> Service -> Repo -> DB
   |
   v
Response (ModelResponse / Page<ModelResponse>)
```

Flujo de error (respuesta uniforme):
```
Controller/Service/Repo lanza excepcion
   |
   v
GlobalExceptionHandler
   |
   v
ErrorResponse { status, error, code, message, path, traceId }
```

Detalle del flujo:

1) El cliente envia un request HTTP.
2) El controller valida y delega al service por inyección de dependencias.
3) El service aplica reglas (use case en arquitectura hexagonal) y usa el repository.
4) El repository consulta/persiste en H2 via JPA. Podríamos conectarlo a una base de datos.
5) Errores se capturan en el centralizador y vuelven como JSON uniforme.

## Endpoints

- `GET /` -> health page
- `POST /model` -> create a model 
- `GET /model` -> list all models (con paginación Ejemplo: /model/page?page=0&size=2&sort=id,desc)
- `GET /model/{id}` -> fetch a model by id
- `GET /model/page` -> list models with pagination
- `DELETE /model/{id}` -> delete a model by id
- `DELETE /erase` -> delete all models
- `GET /debug/thread` -> show current request thread info (temporary)

## Swagger (OpenAPI)

Se integra Swagger UI via Springdoc (compatible con Spring Boot 3.2.5 y Java 21).

- UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Las anotaciones `@Operation` y `@ApiResponse` en el controller describen cada endpoint
y sus respuestas esperadas.

## Observabilidad

Actuator:

- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`

Logging:

- Se incluye `traceId` en consola para correlacionar requests.
- El header `X-Request-Id` se propaga en todas las respuestas.

## TraceId

El traceId es un identificador unico por request que permite correlacionar
logs y respuestas.

Como funciona:

- Se genera (o se recibe) en `X-Request-Id` por request.
- Se agrega a los logs via MDC.
- Se devuelve en `ErrorResponse.traceId` y en el header.

Beneficios:

- Facilita debugging distribuido.
- Reduce tiempo de diagnostico en errores intermitentes.

Detalle de `TraceIdFilter`:

- Lee `X-Request-Id` o genera un UUID si no existe.
- Guarda el valor en MDC (`traceId`) para logging.
- Devuelve el mismo `X-Request-Id` en la respuesta.
- Limpia el MDC al final del request.

## CORS

Se agrega configuracion opcional de CORS para permitir requests desde un frontend
en otro origen.

Como activar:

- Define `APP_CORS_ALLOWED_ORIGINS` con una lista separada por comas.
- Ejemplo: `APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,https://app.example.com`

Notas:

- Si la variable esta vacia, no se habilita CORS.
- Se permiten metodos `GET`, `POST` y `DELETE`.

## Seguridad (API Key)

Se agrega una capa simple de seguridad por API Key.

Como funciona:

- Si `APP_API_KEY` esta vacio, no se aplica seguridad (modo dev/test).
- Si `APP_API_KEY` tiene valor, los endpoints requieren el header `X-API-Key`.
- Los paths publicos permanecen abiertos: `/`, `/swagger-ui/**`, `/v3/api-docs/**`,
  `/actuator/health`, `/actuator/info`, `/actuator/metrics`, `/h2-console/**`.

Variables:

- `APP_API_KEY`: valor secreto de la llave.
- `APP_API_KEY_HEADER`: header (default `X-API-Key`).

## Manejo de environments

Este proyecto usa variables de entorno para credenciales y configuracion sensible.
Se proveen archivos de referencia:

- `.env` para ejecucion normal/local (no se versiona).
- `.env.test.example` como plantilla para pruebas locales.
- `.env.example` como plantilla general con todas las variables.

Variables soportadas:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_API_KEY`
- `APP_API_KEY_HEADER`

Uso recomendado:

1) Carga las variables en tu shell (por ejemplo, `export VAR=...`).
2) Arranca la app con Maven o tu IDE.

Nota: Spring Boot no carga `.env` automaticamente; necesitas exportar las variables
o usar una herramienta/plug-in que lo haga.

Perfiles:

- `dev`: usa `model-service/src/main/resources/application-dev.yml`.
- `test`: usa `model-service/src/main/resources/application-test.yml`.
- Activa con `SPRING_PROFILES_ACTIVE=dev` (o `test`).

## Endpoint de threads

`GET /debug/thread` devuelve informacion del thread que atiende el request.

Campos de respuesta:

- `name`: nombre del thread.
- `id`: id del thread.
- `state`: estado actual del thread.
- `virtual`: `true` si es virtual thread.

Ejemplo de respuesta:

```json
{
  "name": "VirtualThread[#23]/runnable@ForkJoinPool-1-worker-1",
  "id": 23,
  "state": "RUNNABLE",
  "virtual": true
}
```

Usos en una aplicacion del endpoint:
- Verificar si Spring esta usando virtual threads.
- Diagnostico rapido de concurrencia en entornos de desarrollo.
- Confirmar el thread actual durante pruebas de carga o debugging.

Nota: (sólo por motivos de prueba) este endpoint es temporal y no debe exponerse en produccion sin proteccion.

## Manejo centralizado de errores

Se usa un `@RestControllerAdvice` para devolver una respuesta JSON concisa y consistente.

Formato de respuesta:

- `status`: codigo HTTP.
- `error`: razon HTTP (por ejemplo, `Bad Request`).
- `code`: codigo interno corto.
- `message`: detalle corto del error.
- `path`: ruta del request.
- `traceId`: id de trazabilidad (tambien en header `X-Request-Id`).

Ejemplo (validacion):

```json
{
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "id: id is required; name: name is required",
  "path": "/model",
  "traceId": "2c56d4ce-12a5-4c7b-8a4f-1a5f273c1d52"
}
```

Casos cubiertos:

- `BadResourceRequestException` -> 400.
- `NoSuchResourceFoundException` -> 404.
- `MethodArgumentNotValidException` -> 400 con resumen de errores de campos.
- Body JSON invalido -> 400 con mensaje "Invalid JSON body.".
- Otros errores -> 500 con mensaje "Unexpected error.".

Como funciona:

- El controller y el service lanzan excepciones de negocio.
- Spring enruta esas excepciones al `GlobalExceptionHandler`.
- El handler construye un `ErrorResponse` con status, error, message y path.
- El `traceId` se genera por request y se devuelve en la respuesta.

## Validation groups

Los validation groups permiten aplicar reglas distintas segun el caso de uso
sin duplicar DTOs.

- `ValidationGroups.Create` se usa en `POST /model`.
- En `ModelRequest`, `id` y `name` son obligatorios para Create.

Ejemplo de integracion en el controller:

- `@Validated(ValidationGroups.Create.class)` en el endpoint de creacion.

Beneficios:

- Reglas claras por operacion.
- Menos DTOs duplicados.
- Cambios locales sin romper otros endpoints.


## Especificaciones tecnicas

Virtual threads (Java 21):

- Mejor uso de recursos para IO bloqueante, con menos hilos del sistema.
- Menor costo de creacion y memoria por request concurrente.
- Escala mejor en workloads de muchas conexiones simultaneas.

Manejo de pools de threads:

- Separar carga CPU-bound vs IO-bound evita saturar el pool de requests.
- Pools con cola acotada reducen latencia y evitan consumo excesivo de memoria.
- Backpressure y timeouts protegen el sistema ante picos de carga.
- Menos bloqueos y menos context switches mejoran uso de CPU.

Centralizador de errores (`GlobalExceptionHandler`):

- Respuesta consistente para clientes y pruebas automatizadas.
- Menos duplicacion de manejo de errores en controllers.
- Separacion clara entre negocio y representacion HTTP.

## Explicación de cómo usar h2 en consola (sólo en dev)

1) URL para conexion `http://localhost:8080/h2-console`.
2) Use driver class `org.h2.Driver`.
3) Use the JDBC URL shown in the app logs (or configure a fixed URL such as `jdbc:h2:mem:testdb`) Esto lo verás al arrancar spring boot.
4) Username `sa`, password blank (unless you override them).
Nota: en archivos de configuración se puede setear otros valores.

## Curl examples

Health check (verifies the app is up):
```bash
curl -i http://localhost:8080/
```

Create a model (adds a new item with JSON payload):
```bash
curl -i -X POST http://localhost:8080/model \
  -H "Content-Type: application/json" \
  -d '{"id":1,"name":"Item name"}'
```

Respuesta exitosa (201):
```json
{
  "id": 1,
  "name": "Item name"
}
```

List all models (returns the current collection):
(puedes agregar pagination)
```bash
curl -i http://localhost:8080/model 
```

List models with pagination (page, size, sort):
```bash
curl -i "http://localhost:8080/model/page?page=0&size=2&sort=id,desc"
```

Fetch a model by id (returns 404 if it does not exist):
```bash
curl -i http://localhost:8080/model/1
```

Delete a model by id (removes one item):
```bash
curl -i -X DELETE http://localhost:8080/model/1
```

Respuesta exitosa (200):
```json
{
  "message": "Model deleted.",
  "id": 1
}
```

Delete all models (clears the table):
```bash
curl -i -X DELETE http://localhost:8080/erase
```

Respuesta exitosa (200):
```json
{
  "message": "All models deleted."
}
```

Show thread info (useful to verify virtual threads):
```bash
curl -i http://localhost:8080/debug/thread
```

## Paginacion

El endpoint `GET /model/page` acepta:

- `page`: indice de pagina (base 0).
- `size`: cantidad de elementos por pagina.
- `sort`: ordenamiento, por ejemplo `id,desc`.

Por que ayuda a optimizar?:

- Evita devolver miles de registros en una sola respuesta.
- Reduce uso de memoria y tiempo de respuesta.
- Permite al cliente pedir solo lo que necesita.

Como validar en la app:

1) Crea algunos modelos con `POST /model`.
2) Consulta una pagina:

```bash
curl -i "http://localhost:8080/model/page?page=0&size=2&sort=id,desc"
```

Respuesta tipica (Spring Page):

```json
{
  "content": [
    { "id": 5, "name": "model-000-000-005" },
    { "id": 4, "name": "model-000-000-004" }
  ],
  "totalElements": 5,
  "totalPages": 3,
  "size": 2,
  "number": 0,
  "first": true,
  "last": false,
  "numberOfElements": 2,
  "empty": false,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  }
}
```

## Example payload

```json
{
  "id": 1,
  "name": "Item name"
}
```

## Microservicio (estructura multi-modulo)

Este repo usa Maven multi-modulo:

```
/
  pom.xml                (parent)
  model-service/
    pom.xml              (microservicio)
    src/                 (codigo)
```

El microservicio actual es `model-service` y expone todos los endpoints.

### Build y run

Desde la raiz del repo:

```bash
mvn -pl model-service -am clean package
java -jar model-service/target/model-service-1.0.0.jar
```

### Docker

```bash
docker compose up --build
```


## Notes
- Validation errors return HTTP 400.
- Unknown ids return HTTP 404.
- Virtual threads are enabled via `spring.threads.virtual.enabled=true`.
- Podríamos escalar a una arquitectura hexagonal, más adecuado para proyectos de gran envergadura, listos para crecer y para arquitectura de sistemas distribuidos (microservicios).
