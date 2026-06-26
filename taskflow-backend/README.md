# TaskFlow Backend

A production-ready REST API built with Spring Boot, featuring JWT authentication, role-based access control, and MySQL with Flyway migrations.

**Live API:** `https://your-backend.railway.app`  
**Frontend:** [taskflow-frontend repo](https://github.com/you/taskflow-frontend)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (jjwt 0.12) |
| Database | MySQL 8 + Hibernate ORM |
| Migrations | Flyway |
| Connection Pool | HikariCP (built-in) |
| Validation | Jakarta Bean Validation |
| Build | Maven |
| Deployment | Railway (Docker) |

---

## Architecture

```
┌─────────────┐     JWT      ┌──────────────────────────────────────┐
│   Frontend  │ ──────────── │           Spring Boot API            │
│  (React)    │              │                                      │
└─────────────┘              │  ┌──────────┐    ┌───────────────┐  │
                             │  │Controller│───▶│    Service    │  │
                             │  └──────────┘    └───────┬───────┘  │
                             │                          │          │
                             │  ┌──────────────┐  ┌────▼───────┐  │
                             │  │Spring        │  │ Repository │  │
                             │  │Security +    │  │  (JPA)     │  │
                             │  │JWT Filter    │  └────┬───────┘  │
                             │  └──────────────┘       │          │
                             └────────────────────┬────┘──────────┘
                                                  │
                                           ┌──────▼──────┐
                                           │  MySQL DB   │
                                           │  (Flyway)   │
                                           └─────────────┘
```

**Request flow:**
1. Request hits `JwtAuthFilter` → extracts + validates Bearer token
2. Spring Security checks authorization (`@PreAuthorize` / path matchers)
3. Controller receives request → delegates to Service
4. Service applies business logic (ADMIN sees all, USER sees own)
5. Repository queries MySQL via Hibernate
6. `@ControllerAdvice` handles any exceptions → consistent JSON errors

---

## API Reference

### Auth

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register new user → returns JWT |
| POST | `/api/auth/login` | Public | Login → returns JWT |

**Register body:**
```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "Secret@123"
}
```

**Login body:**
```json
{
  "email": "jane@example.com",
  "password": "Secret@123"
}
```

**Auth response:**
```json
{
  "token": "eyJhbGci...",
  "type": "Bearer",
  "id": 1,
  "name": "Jane Doe",
  "email": "jane@example.com",
  "role": "USER"
}
```

---

### Tasks

> All task endpoints require `Authorization: Bearer <token>` header.
> **ADMIN** sees all tasks; **USER** sees only their own.

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/tasks` | List tasks (paginated, filterable) |
| GET | `/api/tasks/:id` | Get single task |
| POST | `/api/tasks` | Create task |
| PUT | `/api/tasks/:id` | Update task |
| DELETE | `/api/tasks/:id` | Delete task |
| GET | `/api/tasks/stats` | Task count stats |

**Query params for GET /api/tasks:**
- `status` — `TODO` | `IN_PROGRESS` | `DONE` | `CANCELLED`
- `priority` — `LOW` | `MEDIUM` | `HIGH` | `URGENT`
- `page` — 0-indexed (default: 0)
- `size` — items per page (default: 10)

**Task body:**
```json
{
  "title": "Fix login bug",
  "description": "Users can't log in on Safari",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "dueDate": "2025-01-31"
}
```

---

### Admin

> ADMIN role required.

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/admin/users` | List all users |
| GET | `/api/admin/users/:id` | Get user by ID |
| PATCH | `/api/admin/users/:id/role` | Change user role |
| DELETE | `/api/admin/users/:id` | Delete user + their tasks |

**Error response format (all endpoints):**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "timestamp": "2025-01-15T10:30:00",
  "fieldErrors": {
    "title": "Title is required",
    "email": "Invalid email format"
  }
}
```

---

## Local Setup

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.0+

### Steps

```bash
# 1. Clone
git clone https://github.com/you/taskflow-backend
cd taskflow-backend

# 2. Create database
mysql -u root -p -e "CREATE DATABASE taskflow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 3. Configure (edit application.properties or use env vars)
export DATABASE_URL=jdbc:mysql://localhost:3306/taskflow?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
export DATABASE_USERNAME=root
export DATABASE_PASSWORD=yourpassword
export JWT_SECRET=your-secret-at-least-32-chars-long

# 4. Run (Flyway auto-runs migrations)
mvn spring-boot:run

# API is live at http://localhost:8080
```

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DATABASE_URL` | `jdbc:mysql://localhost:3306/taskflow...` | MySQL JDBC URL |
| `DATABASE_USERNAME` | `root` | DB user |
| `DATABASE_PASSWORD` | `password` | DB password |
| `JWT_SECRET` | (insecure default) | **Change in production!** |
| `JWT_EXPIRATION` | `86400000` | Token TTL in ms (24h) |
| `CORS_ORIGINS` | `http://localhost:3000,...` | Allowed CORS origins |
| `PORT` | `8080` | Server port |

---

## Deployment (Railway)

1. Push to GitHub
2. Create new Railway project → "Deploy from GitHub repo"
3. Add MySQL plugin → copy connection variables
4. Set environment variables:
   ```
   DATABASE_URL=<from Railway MySQL plugin>
   DATABASE_USERNAME=<from Railway>
   DATABASE_PASSWORD=<from Railway>
   JWT_SECRET=<generate: openssl rand -base64 48>
   CORS_ORIGINS=https://your-frontend.vercel.app
   ```
5. Railway detects `Dockerfile` and deploys automatically
6. Flyway runs migrations on first boot

---

## Database Schema

```sql
users (id, name, email, password_hash, role, created_at, updated_at)
tasks (id, title, description, status, priority, due_date, created_at, updated_at, user_id → users.id)
```

Migrations live in `src/main/resources/db/migration/` — versioned `V{n}__description.sql`.

---

## Key Design Decisions

- **Stateless JWT** — no sessions, horizontally scalable
- **BCrypt cost 12** — secure password hashing
- **HikariCP pooling** — efficient DB connection reuse
- **Flyway migrations** — reproducible schema, tracks history
- **`@ControllerAdvice`** — single error-handling layer, consistent JSON
- **Role enforcement at service layer** — not just route-level, harder to bypass
