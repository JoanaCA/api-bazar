# API Bazar

API REST para gestionar el catálogo, los clientes y las ventas de un bazar, con control de stock.
Proyecto basado en el integrador del curso [Desarrollo de APIs con Spring Boot](https://todocodeacademy.com/course/desarrollo-de-apis-con-spring-boot/) de TodoCode Academy, y llevado a un nivel más cercano a un entorno profesional.

> El enunciado original del curso está en [docs/ENUNCIADO.md](docs/ENUNCIADO.md).

## Stack

- Java 21
- Spring Boot 4.2 (Spring Web MVC, Spring Data JPA, Bean Validation)
- MySQL 8 y migraciones con Flyway
- MapStruct y Lombok
- JUnit 5, Mockito, AssertJ, Testcontainers y JaCoCo
- Maven (wrapper incluido)

## Funcionalidades

- CRUD de **productos**, **clientes** y **ventas**.
- Una venta tiene varios productos con **cantidad** y **precio unitario** (el precio se copia al momento de vender).
- Al registrar una venta se **valida el stock, se descuenta y se calcula el total** en el backend, todo en una transacción.
- Al editar o anular una venta, el stock se **restituye**.
- Listados **paginados y ordenables**.
- Errores en formato estándar **RFC 7807 (`ProblemDetail`)**.
- Reportes: productos con stock bajo, productos de una venta, totales por día y venta de mayor monto.

## Requisitos

- JDK 21
- MySQL 8 en ejecución (la base `bazar` se crea sola si el usuario tiene permisos; Flyway crea las tablas al arrancar)
- Docker en ejecución (solo si vas a correr los tests de integración contra MySQL real con `verify`)

## Configuración

La conexión a la base se toma de variables de entorno. Si no están definidas se usan estos valores de desarrollo:

| Variable | Valor por defecto |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/bazar?createDatabaseIfNotExist=true` |
| `DB_USER` | `root` |
| `DB_PASSWORD` | `tu_password` |

Otras propiedades (`src/main/resources/application.properties`):

| Propiedad | Descripción | Valor |
|---|---|---|
| `bazar.stock.minimo` | Umbral para considerar "stock bajo" | `5` |

> No subas contraseñas reales al repositorio: usá siempre `DB_PASSWORD`.

## Cómo ejecutarlo

Windows (PowerShell):

```powershell
$env:DB_PASSWORD = "mi_password"
.\mvnw.cmd spring-boot:run
```

Linux / macOS:

```bash
DB_PASSWORD=mi_password ./mvnw spring-boot:run
```

La API queda en `http://localhost:8080`.

## Endpoints

Base: `/api/v1`

### Productos

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| `POST` | `/productos` | Crear producto | `201` + `Location` |
| `GET` | `/productos` | Listar (paginado) | `200` |
| `GET` | `/productos/{id}` | Obtener uno | `200` / `404` |
| `PUT` | `/productos/{id}` | Reemplazar | `200` / `404` |
| `DELETE` | `/productos/{id}` | Eliminar | `204` / `404` / `409` si tiene ventas |
| `GET` | `/productos/stock-bajo` | Productos con stock menor al mínimo (req. 4) | `200` |

### Clientes

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| `POST` | `/clientes` | Crear cliente | `201` / `409` si el DNI ya existe |
| `GET` | `/clientes` | Listar (paginado) | `200` |
| `GET` | `/clientes/{id}` | Obtener uno | `200` / `404` |
| `PUT` | `/clientes/{id}` | Reemplazar | `200` / `404` / `409` |
| `DELETE` | `/clientes/{id}` | Eliminar | `204` / `404` / `409` si tiene ventas |

### Ventas

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| `POST` | `/ventas` | Registrar venta | `201` / `404` / `409` sin stock |
| `GET` | `/ventas` | Listar (paginado) | `200` |
| `GET` | `/ventas/{id}` | Obtener una | `200` / `404` |
| `PUT` | `/ventas/{id}` | Reemplazar (reajusta el stock) | `200` / `404` / `409` |
| `DELETE` | `/ventas/{id}` | Anular (devuelve el stock) | `204` / `404` |
| `GET` | `/ventas/{id}/productos` | Productos de la venta (req. 5) | `200` / `404` |
| `GET` | `/ventas/reportes/dia/{yyyy-MM-dd}` | Monto total y cantidad de ventas del día (req. 6) | `200` |
| `GET` | `/ventas/reportes/mayor` | Venta de mayor monto (req. 7) | `200` / `404` si no hay ventas |

La **paginación** usa los parámetros estándar: `?page=0&size=10&sort=nombre,asc`.

### Ejemplo: registrar una venta

```http
POST /api/v1/ventas
Content-Type: application/json

{
  "clienteId": 1,
  "fechaVenta": "2026-03-07",
  "items": [
    { "productoId": 1, "cantidad": 2 },
    { "productoId": 2, "cantidad": 1 }
  ]
}
```

El total **no se envía**: lo calcula el servidor. `fechaVenta` es opcional (por defecto, hoy).

### Ejemplo de error de validación (`400`)

```json
{
  "title": "Error de validación",
  "status": 400,
  "detail": "La solicitud contiene datos inválidos",
  "errores": {
    "nombre": "El nombre es obligatorio",
    "costo": "El costo no puede ser negativo"
  }
}
```

| Código | Cuándo |
|---|---|
| `400` | Datos inválidos o JSON mal formado |
| `404` | El recurso (o un producto/cliente referenciado) no existe |
| `409` | Stock insuficiente, DNI duplicado, recurso en uso o conflicto de concurrencia |
| `500` | Error inesperado (sin detalles internos) |

## Colección de Bruno

En [`coleccion_bruno/`](coleccion_bruno) hay una colección lista para abrir con [Bruno](https://www.usebruno.com/) con todos los endpoints. Orden sugerido: crear clientes y productos primero, y después las ventas.

## Pruebas

La suite está dividida en pruebas rápidas (`*Test`, Surefire) y pruebas contra base de datos real (`*IT`, Failsafe):

- **Suite rápida (sin Docker):** ejecuta tests unitarios (dominio, mappers, validaciones de DTOs, services), `GlobalExceptionHandler` y tests de capa web (`@WebMvcTest`):

  ```bash
  ./mvnw test
  ```

- **Suite completa (requiere Docker en ejecución):** ejecuta todo lo anterior más los tests de repositorios (`@DataJpaTest`), migraciones de Flyway, flujo completo y concurrencia contra **MySQL 8.4 real** mediante Testcontainers, y genera y verifica la cobertura con JaCoCo:

  ```bash
  ./mvnw verify
  ```

  En Windows usá `.\mvnw.cmd test` o `.\mvnw.cmd verify`.

- **Cobertura (JaCoCo):** tras `./mvnw verify` se genera el informe HTML en `target/site/jacoco/index.html`. El build falla si la cobertura cae por debajo de **80% de líneas** o **70% de ramas** sobre el código con lógica.

## Decisiones de diseño

- **Capas**: `controller` → `service` → `repository`. Los controllers no conocen las entidades JPA: reciben y devuelven DTOs (`record`) mapeados con MapStruct.
- **Venta con `DetalleVenta`** en lugar de una relación muchos-a-muchos simple: permite cantidades y conservar el precio histórico.
- **Dinero** en `BigDecimal` y **stock** en `Integer`.
- **Migraciones**: el esquema está versionado con **Flyway** (`src/main/resources/db/migration/V1__esquema_inicial.sql`) y `spring.jpa.hibernate.ddl-auto=validate` comprueba al arrancar que las entidades coincidan con la base.
- **Concurrencia**: `Producto` usa `@Version` (bloqueo optimista) y `VentaService` actualiza los productos en orden por ID antes de insertar detalles para evitar deadlocks en InnoDB; las colisiones responden `409 Conflict`.
- **Transacciones**: lecturas en `readOnly`; escrituras atómicas.
- **Errores**: un único `@RestControllerAdvice` que responde en formato `ProblemDetail`.

## Hoja de ruta

- [x] Tests unitarios e integración (JUnit 5, Mockito, Testcontainers) y cobertura con JaCoCo
- [x] Migraciones de base de datos con Flyway
- [ ] Autenticación y roles con Spring Security + JWT
- [ ] Docker, Docker Compose y GitHub Actions
- [ ] Documentación OpenAPI / Swagger
