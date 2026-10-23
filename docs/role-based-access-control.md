# Role-Based Access Control Guide

This guide explains how to add roles and protect backend endpoints and frontend routes in the Buzz Trader application.

The supported roles are currently:

- `CLIENT`
- `ANALYST`
- `MANAGER`

Backend authorization is authoritative. Frontend role data is used for navigation and route protection only and must not be treated as a security boundary.

## SECTION 1 -- Add A New Role

Use the same role name across Java, SQL, API responses, and Angular route metadata. Role names are uppercase and must match exactly.

### Step 1: Add the role to the Java enum

Edit:

`backend/src/main/java/com/buzzleapyear/trading_api/entity/Role.java`

Add the new value to `RoleName`:

```java
public enum RoleName {
    CLIENT,
    ANALYST,
    MANAGER,
    AUDITOR
}
```

Use a descriptive name that represents a stable permission group, not a screen or individual user.

### Step 2: Add the role to database seed or migration data

If `V2__seed_data.sql` has already run in a deployed database, do not edit it and expect Flyway to rerun it. Create the next migration instead, for example:

`backend/src/main/resources/db/migration/V3__add_auditor_role.sql`

```sql
INSERT INTO roles (role_name, description)
VALUES ('AUDITOR', 'Users who can review audit information')
ON CONFLICT (role_name) DO NOTHING;
```

Use a forward-only migration for existing databases. Do not rewrite an applied migration.

### Step 3: Assign the role to users

Use the existing role-assignment service where possible:

`backend/src/main/java/com/buzzleapyear/trading_api/service/UserRoleService.java`

```java
userRoleService.assignRoleToUser(userId, Role.RoleName.AUDITOR, assignedBy);
```

For a default registration role, update the registration flow deliberately. New registrations currently receive `CLIENT` in:

[`AuthController.java`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/AuthController.java)

Do not make elevated roles the default registration role.

### Step 4: Map the role to a Spring authority

[`CustomUserDetailsService`](../backend/src/main/java/com/buzzleapyear/trading_api/security/CustomUserDetailsService.java) converts each persisted role into a Spring authority by adding the `ROLE_` prefix:

```java
.map(userRole -> new SimpleGrantedAuthority(
    "ROLE_" + userRole.getRole().getRoleName().name()))
```

Therefore, `AUDITOR` becomes `ROLE_AUDITOR`. Keep the prefix in the authority mapping; `hasRole("AUDITOR")` expects it.

### Step 5: Return the role in the auth contract

Login, registration, and token refresh responses include role names in [`AuthResponse`](../frontend/src/app/models/auth-response.model.ts):

```json
{
  "token": "eyJ...",
  "type": "Bearer",
  "username": "auditor-user",
  "email": "auditor@example.com",
  "roles": ["AUDITOR"]
}
```

Update the backend response only with canonical enum names. The Angular model in:

[`frontend/src/app/models/auth-response.model.ts`](`frontend/src/app/models/auth-response.model.ts`)

uses the same string values.

[`AuthController`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/AuthController.java)
uses a shared response builder to include every assigned role without the Spring
`ROLE_` prefix. A manager receives `"roles": ["MANAGER"]`, which the frontend uses
to select the manager dashboard. Users without role assignments receive an empty
list; registration assigns `CLIENT`. Access tokens remain in the response body,
and refresh tokens remain in the HttpOnly cookie, not the response body.

### Step 6: Add role-focused tests

At minimum, test that:

1. The enum contains the role.
2. Seed or migration data creates the role.
3. A user with the role receives `ROLE_<NAME>`.
4. Login returns the role name.
5. A new registration still receives only the intended default role.
6. Token refresh returns all current role names and keeps the refresh token cookie-only.

Run the focused backend tests:

```bash
cd backend
./mvnw -Dtest=AuthRoleTests test
```

## SECTION 2 -- Configure Endpoint Permissions

Backend endpoint permissions belong in:

[`backend/src/main/java/com/buzzleapyear/trading_api/config/SecurityConfig.java`](../backend/src/main/java/com/buzzleapyear/trading_api/config/SecurityConfig.java)

### Step 1: Decide whether the endpoint is public or protected

Public authentication endpoints are explicitly permitted:

```java
.requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/auth/validate").permitAll()
```

Only add an endpoint to `permitAll()` when it genuinely needs anonymous access.

### Step 2: Add the most specific restricted matcher

Use `hasRole` for one role:

```java
.requestMatchers("/api/v1/audit/**").hasRole("AUDITOR")
```

Use `hasAnyRole` for several roles:

```java
.requestMatchers("/api/v1/reports/**")
    .hasAnyRole("MANAGER", "ANALYST", "AUDITOR")
```

Pass role names without the `ROLE_` prefix. Spring adds that prefix when evaluating `hasRole` and `hasAnyRole`.

### Step 3: Place matchers in the correct order

Spring Security evaluates request matchers in declaration order. Put specific paths before broad paths:

```java
.requestMatchers("/api/v1/admin/audit/**").hasRole("AUDITOR")
.requestMatchers("/api/v1/admin/**").hasRole("MANAGER")
.requestMatchers("/api/v1/**").authenticated()
.anyRequest().authenticated()
```

A broad matcher placed first can prevent a later role-specific rule from being reached.

### Step 4: Confirm authorities are loaded from the database

[`JwtAuthenticationFilter`](../backend/src/main/java/com/buzzleapyear/trading_api/security/JwtAuthenticationFilter.java) validates the token, extracts the username, and loads the current user through [`CustomUserDetailsService`](../backend/src/main/java/com/buzzleapyear/trading_api/security/CustomUserDetailsService.java). The service derives authorities from persisted `user_roles` records.

Do not authorize backend requests using:

- Angular `localStorage` values
- The `roles` array returned to the browser
- A client-supplied request field

A user changing browser storage can affect navigation, but it must not change backend permissions.

### Step 5: Test both allowed and denied access

For every new restricted endpoint, test at least:

- A user with the required role receives the expected success response.
- An authenticated user without the role receives `403 Forbidden`.
- An unauthenticated request receives `401 Unauthorized` where applicable.

Also verify that the endpoint is not accidentally covered by a broader `authenticated()` rule.

## SECTION 3 -- Configure Page And Route Permissions

Frontend route permissions belong in:

[`frontend/src/app/app.module.ts`](../frontend/src/app/app.module.ts)

The shared guard is:

[`frontend/src/app/auth.guard.ts`](../frontend/src/app/auth.guard.ts)

### Step 1: Add the page component

Create the page using the existing Angular page structure. For example:

```text
frontend/src/app/pages/audit/
  audit.component.ts
  audit.component.html
```

Declare the component in `AppModule` unless the application is converted to standalone components.

### Step 2: Add the route and required role metadata

Use `data.roles` to describe the required role:

```typescript
{
  path: 'audit',
  component: AuditComponent,
  canActivate: [authGuard],
  data: { roles: ['AUDITOR'] }
}
```

For multiple acceptable roles:

```typescript
data: { roles: ['MANAGER', 'AUDITOR'] }
```

The current guard allows navigation when the user has at least one listed role.

### Step 3: Protect all authenticated pages

A page that requires only a valid login still uses the guard without role metadata:

```typescript
{
  path: 'dashboard',
  component: DashboardComponent,
  canActivate: [authGuard]
}
```

A page that requires a role must include both `canActivate: [authGuard]` and `data.roles`.

### Step 4: Configure the post-login landing page

Login and registration currently use the same rule:

- `MANAGER` users go to `/admin`.
- Other authenticated users go to `/dashboard`.

When adding a new elevated landing page, update both:

- `frontend/src/app/pages/login/login.component.ts`
- `frontend/src/app/pages/register/register.component.ts`

Use `AuthService.hasRole()` rather than reading `localStorage` directly.

### Step 5: Understand the frontend security boundary

`AuthService` stores the token and role names in browser storage so the UI can make navigation decisions. A user can change those values. This is expected and does not provide authorization.

The backend must independently enforce every sensitive operation. A protected Angular route improves the user experience, while Spring Security protects the data and API operation itself.

### Step 6: Test route behavior

For a role-protected route, test that:

1. No token redirects to `/login`.
2. An invalid token is cleared and redirects to `/login`.
3. An authenticated user without the required role redirects to `/dashboard`.
4. A user with the required role can activate the route.
5. The corresponding backend endpoint rejects unauthorized users.

Build the frontend after route changes:

```bash
cd frontend
npm run build
```

If browser-based tests are available, also run:

```bash
npm test -- --watch=false --browsers=ChromeHeadless
```

## Checklist For A New Role-Protected Feature

*For new Roles*
- [ ] Add the role to `Role.RoleName`.
- [ ] Add a forward-only Flyway migration when the database is already deployed.
- [ ] Seed or assign the role to intended users.
- [ ] Confirm `CustomUserDetailsService` maps it to `ROLE_<NAME>`.
- [ ] Add the role to the auth response contract.

*For new features*
- [ ] Add backend endpoint matchers in `SecurityConfig`.
- [ ] Add allowed and denied backend tests.
- [ ] Create and declare the Angular page component.
- [ ] Add `canActivate: [authGuard]` and `data.roles` to the route.
- [ ] Update landing-page behavior if needed.
- [ ] Run backend tests and the frontend build.
- [ ] Verify frontend storage changes do not bypass backend authorization.
