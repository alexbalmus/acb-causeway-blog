# Authentication and local development

The application uses Spring Security **server-side sessions**, shared by Angular,
Wicket, the JTE/HTMX blog client and GraphQL. Passwords use BCrypt. No Basic credentials or JWTs are stored
in browser storage. The session cookie is HttpOnly, SameSite=Lax and expires after
30 minutes of inactivity. Logout invalidates the server session.

## Public blog client

`/blog` lists and searches all blogs. `/blog/blogs/{id}` and `/blog/posts/{id}`
are readable without signing in, including pictures and captions. There is no
private/draft state; existing and newly created content is publicly readable here.
The existing Angular, Wicket, REST and GraphQL authentication requirements remain.

Edit links lead to protected editor pages and `/blog/login`, then return to the
requested local editor. All changes use authenticated, CSRF-protected POSTs and
enforce ownership through Causeway wrappers. An administrator cannot edit another
user's blog. POST `/blog/logout` invalidates the same session used by other clients.
HTMX receives HTML fragments, never credentials or session tokens in browser storage.
History snapshots are disabled and personalized HTML uses `Cache-Control: no-store`.

The new multipart upload path limits file size and decoded pixel count before
image decoding. These limits do not change the existing REST image-upload contract.

## Start locally

Java 25 and Maven are required. Set a password for each development account you
want to create, using environment variables or external Spring configuration:

```powershell
$env:ACB_SECURITY_SEED_SVEN_PASSWORD = '<your local admin password>'
$env:ACB_SECURITY_SEED_BOB_PASSWORD = '<your local user password>'
mvn spring-boot:run '-Dspring-boot.run.profiles=Dev'
```

Supported development usernames are `sven`, `dick`, `bob` and `joe`. `sven` has
`ROLE_ADMIN` and `ROLE_USER`; the others have `ROLE_USER`. No account is created
without a configured password. Seeding runs only under `Dev` and never overwrites
an existing account. The equivalent property is
`acb.security.seed.<username>.password`. Do not commit passwords.

The existing Spring AI configuration still requires `SPRING_AI_OPENAI_API_KEY`;
AI behavior and the `Ai` profile are unchanged by this security migration.

Open Wicket at `http://localhost:8080/wicket/`. For Angular:

```powershell
cd webclient
npm install
npm start
```

Open `http://localhost:4200`. The development proxy forwards `/api` and `/restful`
to the backend. Use the **same hostname** for both UIs (`localhost`, rather than
mixing it with `127.0.0.1`) to share the session cookie. Cookies are shared across
ports; same-origin deployment is recommended outside development.

## Authentication API

1. `GET /api/auth/csrf` initializes the readable `XSRF-TOKEN` cookie.
2. `POST /api/auth/login`, form-encoded `username` and `password`, with the cookie
   value in `X-XSRF-TOKEN`, returns 204 or a generic 401. Login changes the session ID.
3. Fetch `/api/auth/csrf` again after login to obtain a fresh CSRF token.
4. `GET /api/auth/me` returns `{ "userName": "bob", "roles": ["ROLE_USER"] }`.
5. Send cookies with REST/GraphQL requests and the CSRF header with modifying
   requests, including GraphQL POST requests.
6. `POST /api/auth/logout` with CSRF protection returns 204 and clears the session.
   Fetch a fresh token before the next login.

Angular handles this sequence automatically. API 401 means authentication is
required; 403 indicates missing permissions or failed CSRF validation. Mutations
are never automatically replayed. Wicket uses Spring's form login; its Logout
menu opens Spring's confirmation form, whose POST ends the shared session.

Ordinary users can read blogs and manage their own content. The existing domain
ownership checks also apply to administrators. Framework administration remains
restricted to admins. Authentication entities are excluded from the Causeway
metamodel, and all identities/authorities originate from Spring Security.

## Deployment boundaries

- H2 remains **in memory**: accounts and blogs disappear on restart. This change
  does not introduce a persistent database, schema migrations, account-management
  screens, registration, password reset, or distributed sessions.
- Outside `Dev`, provision accounts in the configured database using BCrypt hashes;
  there are no default users or passwords. A durable production database and its
  provisioning/migration process require separate configuration/work.
- Set `ACB_SECURE_COOKIES=true` for HTTPS deployments (session and CSRF cookies),
  and configure the reverse proxy's trusted forwarded-header handling as needed.
  Plain HTTP loopback development uses `false`.
- Retain same-origin access. No permissive CORS policy is enabled.
- H2 console, forced prototyping, GraphiQL/schema printing and GraphQL fallback
  authentication are disabled. Explicitly enabled diagnostics require admin access.
- Keep `causeway.security.spring.allow-csrf-filters=true`. The app imports the
  Spring authenticator selectively: its own Wicket bridge and REST authentication
  strategy replace the stock blanket filter, and roles are copied explicitly.

## Verification

```powershell
mvn test '-Declipselink.application-location=target'
cd webclient
npm test -- --watch=false --browsers=ChromeHeadless
npm run build
```

Backend integration tests use a random HTTP port and test-only accounts to check
the actual servlet filter chain, rather than relying solely on mocked requests.
