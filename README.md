# TaskFlow Frontend

A React SPA for task management with JWT authentication, protected routing, and role-based UI.


---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | React 18 |
| Routing | React Router v6 |
| HTTP | Axios (with JWT interceptor) |
| Notifications | react-hot-toast |
| Date Utils | date-fns |
| Build Tool | Vite |
| Deployment | Vercel |

---

## Architecture

```
src/
├── context/
│   └── AuthContext.jsx     # Global auth state (user, login, logout, register)
├── services/
│   └── api.js              # Axios instance + JWT interceptor + all API calls
├── components/
│   ├── ProtectedRoute.jsx  # Redirect to /login if unauthenticated
│   ├── AppShell.jsx        # Sidebar + main content layout
│   └── Sidebar.jsx         # Navigation (role-aware: shows Admin link for ADMIN)
├── pages/
│   ├── LoginPage.jsx       # Login form
│   ├── RegisterPage.jsx    # Register form with field validation
│   ├── DashboardPage.jsx   # Task list, filters, stats, pagination
│   ├── TaskFormPage.jsx    # Create / edit task
│   └── AdminPage.jsx       # User table with role toggle + delete (ADMIN only)
└── index.css               # Design system (CSS variables, components)
```

**Auth flow:**
```
User visits /dashboard
      │
      ▼
ProtectedRoute checks localStorage for user
      │
  Not logged in ──────────────────▶ /login
      │
  Logged in
      │
      ▼
AppShell renders — Sidebar shows Admin link if role=ADMIN
      │
      ▼
Every Axios request: interceptor reads token from localStorage
and sets Authorization: Bearer <token> header automatically
      │
      ▼
401 response? Interceptor clears storage → redirects to /login
```

---

## Pages

| Route | Access | Description |
|---|---|---|
| `/login` | Public | Email/password login |
| `/register` | Public | Account creation with validation |
| `/dashboard` | Auth | Task list with filters, stats cards |
| `/tasks/new` | Auth | Task creation form |
| `/tasks/:id/edit` | Auth | Task edit form |
| `/admin` | ADMIN only | User management table |

---

## Local Setup

```bash
# 1. Clone
git clone https://github.com/you/taskflow-frontend
cd taskflow-frontend

# 2. Install
npm install

# 3. Configure
cp .env.example .env.local
# Edit .env.local:
#   VITE_API_URL=http://localhost:8080/api

# 4. Run
npm run dev
# App runs at http://localhost:3000
```

### Build for production

```bash
npm run build
# Output in /dist
```

---

## Deployment (Vercel)

1. Push to GitHub
2. Import repo in Vercel dashboard
3. Set environment variable:
   ```
   VITE_API_URL=https://your-backend.railway.app/api
   ```
4. Vercel auto-detects Vite — deploy!
5. `vercel.json` handles SPA routing (all paths → `index.html`)

---

## Key Implementation Details

**JWT interceptor** (`src/services/api.js`):
```js
api.interceptors.request.use(config => {
  const token = localStorage.getItem('taskflow_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Auto-logout on 401
api.interceptors.response.use(null, error => {
  if (error.response?.status === 401) {
    localStorage.clear();
    window.location.href = '/login';
  }
  return Promise.reject(error);
});
```

**Protected routes** (`src/components/ProtectedRoute.jsx`):
```jsx
// Redirects to /login if not authenticated
// Redirects to /dashboard if not admin (adminOnly routes)
<ProtectedRoute adminOnly>
  <AdminPage />
</ProtectedRoute>
```

**Role-based UI** — sidebar shows Admin link only when `user.role === 'ADMIN'`.  
**Field validation errors** — backend validation errors mapped to individual fields in forms.  
**Meaningful error messages** — API errors surfaced as toast notifications or inline form errors, never raw JSON.
