# Week 7 E-commerce API

Spring Boot 3 / Java 17 REST API with PostgreSQL, Flyway-managed schema, Spring Data JPA,
auditing, BCrypt password hashing, stateless HTTP Basic authentication, and transactional
inventory-aware checkout.

## Run locally

1. Start PostgreSQL: `docker compose up -d postgres`
2. Start the API: `mvn spring-boot:run`
3. Run tests: `mvn test`

The local database defaults match `docker-compose.yml`. Override `DATABASE_URL`,
`DATABASE_USERNAME`, and `DATABASE_PASSWORD` to use another database. Flyway applies
the schema and sample catalog migrations on startup. Hibernate validates the schema;
it does not create or alter tables.

The `test` profile uses an isolated H2 database in PostgreSQL compatibility mode and
runs the same Flyway migrations. The `prod` profile requires explicit PostgreSQL
environment variables and uses a larger, configurable HikariCP pool:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DATABASE_URL = "jdbc:postgresql://db-host:5432/ecommerce"
$env:DATABASE_USERNAME = "..."
$env:DATABASE_PASSWORD = "..."
mvn spring-boot:run
```

Never use the local Docker credentials in production. Run production traffic over TLS.

## Authentication and authorization

Register a customer with `POST /api/users/register`, then authenticate subsequent
requests with HTTP Basic using the registered email and password. Passwords are stored
as BCrypt hashes. Catalog reads are public; cart, order, profile, and payment routes
require authentication. Catalog writes require `ROLE_ADMIN`.

No administrator is seeded with a default password. Promote a trusted registered
account through a secured operational database procedure, for example:

```sql
UPDATE app_users SET role = 'ADMIN' WHERE email = 'admin@example.com';
```

Use a secret-managed, audited production process for this operation.

## API

| Area | Routes |
| --- | --- |
| Users | `POST /api/users/register`, `GET/PATCH /api/users/me` |
| Catalog | `GET /api/products`, `GET /api/products/{id}`, `GET /api/categories`, `GET /api/categories/{id}` |
| Admin catalog | `POST/PUT/DELETE /api/admin/products[/{id}]`, `POST/PUT/DELETE /api/admin/categories[/{id}]` |
| Cart | `GET/DELETE /api/cart`, `POST /api/cart/items`, `PUT/DELETE /api/cart/items/{itemId}` |
| Orders | `POST /api/orders/checkout`, `GET /api/orders`, `GET /api/orders/{id}`, `POST /api/orders/{id}/cancel` |
| Payments | `POST /api/orders/{orderId}/payments`, `GET /api/payments/{id}` |

Catalog supports `page`, `size`, `sort`, `categoryId`, and `q` query parameters.
Page size is capped at 100. OpenAPI UI is at `/swagger-ui.html`; health and metrics
are exposed through Spring Boot Actuator.

An importable customer workflow is available in
[`postman/week7-ecommerce.postman_collection.json`](postman/week7-ecommerce.postman_collection.json).
Run "Register customer" first, then browse the seeded catalog, add the sample book,
check out, and initiate a pending payment.

## Order and payment behavior

Checkout runs in one database transaction: it locks the customer's cart and product
inventory rows in a deterministic order, validates stock, reserves inventory, snapshots
product prices and names into order items, creates a pending-payment order, and clears
the cart. Any failure rolls the entire operation back. Cancelling an unpaid order
restores inventory and cancels its pending payment records. Payment initiation stores
an idempotent pending payment for the order amount; **it does not charge a card or
settle a payment**. Connect and verify a payment-provider integration before accepting
real payments. The API never accepts or stores card numbers or CVVs.

## Database and operations

- `src/main/resources/db/migration/V1__create_ecommerce_schema.sql` defines foreign
  keys, checks, unique constraints, audit columns, and query indexes.
- `V2__seed_catalog.sql` seeds example categories and products.
- Spring Data auditing manages `created_at` and `updated_at`.
- HikariCP is configured through `DB_POOL_MAX_SIZE` and `DB_POOL_MIN_IDLE`.
- A bounded, expiring Caffeine cache is used for individual catalog product reads and
  evicted on catalog and inventory mutations.
- Development logging includes SQL, Hibernate statistics, slow-query timing, and pool
  activity. Bind-parameter values are not logged. Production logging is less verbose.

Create future schema changes as additional Flyway migrations; do not edit migrations
that have already been applied to a shared database.
