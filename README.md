# RoadRescue

RoadRescue is a full-stack roadside assistance platform for customers, mechanics, garage owners, and admins.

## Migrated Stack

- Frontend: Angular 22, Angular Router, reactive forms, HttpClient, Leaflet
- Backend: Spring Boot 4.1, Java 21 LTS, Spring Web MVC, Spring JDBC, Flyway
- Database: PostgreSQL, preserving the original Prisma-created tables, enums, arrays, relationships, and quoted PascalCase identifiers
- Auth: JWT bearer tokens with the same `userId` and `role` claims, bcrypt-compatible password hashes

## Architecture Mapping

| Old | New |
| --- | --- |
| React + Vite pages/components | Angular standalone route components |
| Zustand auth store | Angular `AuthStore` service |
| Axios API wrappers | Angular `HttpClient` services |
| Express routers/controllers | Spring MVC controllers |
| Prisma Client | Spring JDBC repositories with explicit PostgreSQL SQL |
| Prisma migration SQL | Flyway `V1__init.sql` copied from the original migration |
| Socket.IO live update hooks | Tracking page currently polls `/api/requests/:id` every 5 seconds |

## API Endpoint Mapping

All external REST paths are preserved:

- `GET /api/health`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/users/vehicles`
- `PUT /api/users/provider-profile`
- `PATCH /api/users/availability`
- `GET /api/requests/nearby-providers`
- `GET /api/requests`
- `POST /api/requests`
- `GET /api/requests/{requestId}`
- `PATCH /api/requests/{requestId}/status`
- `POST /api/requests/{requestId}/review`
- `GET /api/admin/analytics`
- `GET /api/admin/users`
- `PATCH /api/admin/users/{userId}`

## Database Model Mapping

The database schema remains compatible with the original Prisma schema:

- `User`, `Vehicle`, `MechanicProfile`, `GarageProfile`
- `AssistanceRequest`, `ServiceUpdate`, `Review`, `Payment`, `Notification`
- Enums: `Role`, `IssueType`, `RequestStatus`, `ServiceType`, `ProviderKind`, `PaymentMethod`, `PaymentStatus`, `NotificationType`

No table or column rename is required. Existing `DATABASE_URL` values using `?schema=...` are converted internally to PostgreSQL JDBC `currentSchema`.

## Environment Variables

Backend:

- `CLIENT_URL`
- `CLIENT_URLS`
- `SERVER_PORT` or `PORT`
- `DATABASE_URL`
- `JWT_SECRET`

Frontend:

- Angular uses `client/src/environments/environment.ts`.
- Local default API: `http://localhost:5000/api`
- Default deployed API: `https://roadrescue-backend.onrender.com/api`

## Commands

Install dependencies:

```powershell
npm install
```

Run backend:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.11'
mvn -f server/pom.xml spring-boot:run
```

Create/verify the Neon schemas and seed local demo users:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.11'
npm run db:setup
```

Run frontend:

```powershell
npm run dev --workspace client
```

Build backend:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.11'
mvn -f server/pom.xml -DskipTests package
```

Build frontend:

```powershell
npm run build --workspace client
```

## Database Migration

Spring Boot runs Flyway automatically on startup using `server/src/main/resources/db/migration/V1__init.sql`.

For an existing database already migrated by Prisma, Flyway uses `baseline-on-migrate=true`, so it can attach safely without recreating existing tables.

The setup tool creates:

- `roadrescue_local`: local development and testing data, seeded with one demo user per role
- `roadrescue_prod`: production schema, schema-only by default

## Demo Accounts

If your database still contains the previous seeded data:

- Admin: `admin@roadrescue.app` / `Admin@123`
- Customer: `riya@roadrescue.app` / `Customer@123`
- Mechanic: `arun@roadrescue.app` / `Mechanic@123`
- Garage owner: `metro@roadrescue.app` / `Garage@123`

## Verification Notes

- Backend build passes with `mvn -f server/pom.xml -DskipTests package`.
- Frontend build passes with `npm run build --workspace client`.
- Known difference: the previous Socket.IO push channel has been replaced with polling on the Angular tracking page. REST API behavior and request status persistence are preserved.
