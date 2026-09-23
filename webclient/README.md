# ACB Blog — Angular web client

An Angular frontend for the ACB Blog Causeway application. It consumes the
REST API that Apache Causeway auto-generates from the domain model (the
[Restful Objects](https://www.restfulobjects.org) viewer, served under
`/restful`), and mirrors the functionality of the default Wicket UI:

- **Sign in** with the app's users (Spring Security session login with CSRF protection)
- **Home**: your blogs, New Blog, Change Handle, Delete Blog, Find Blogs
- **Blog page**: rename, posts list, New Post (title, content, picture,
  picture description), Delete Post, Delete Blog
- **Post page**: server-rendered article preview (picture + content, the same
  markup the Wicket UI shows), edit content, rename, update/clear picture,
  edit picture description
- Server-side rules are surfaced: `disabledReason`s from the API disable the
  corresponding buttons (e.g. ownership vetoes), and 422 validation responses
  are shown as per-field errors (e.g. duplicate titles, AI safety checks)

## Styling

The UI is built with **Bootstrap 5** and the **Bootswatch "Litera"** theme —
the same theme the Wicket viewer uses (`causeway.viewer.wicket.themes.initial`
in `application.yml`), so both front-ends look consistent. The theme is a
prebuilt stylesheet (`bootswatch/dist/litera/bootstrap.min.css`) wired into
`angular.json`; no Bootstrap JavaScript is used (panels and toasts are driven
by Angular). App-specific CSS is limited to a few rules in `src/styles.css`
(post-preview typography and the toast stack); components style themselves
with Bootstrap utility classes in their templates.

## Running

See [authentication setup](../SECURITY.md) for required development-account passwords.

1. Start the backend from the repo root (the REST API listens on
   `127.0.0.1:8080`):

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=Dev
   ```

2. Start the client (from `webclient/`):

   ```bash
   npm install
   npm start
   ```

   `npm start` runs `ng serve` with `proxy.conf.json`, which forwards
   `/api` and `/restful` to the backend — same-origin, so no CORS setup is needed.

3. Open <http://localhost:4200> and sign in with a configured development account.

## How it talks to Causeway

`src/app/core/` contains the API layer:

| File                  | Role                                                             |
| --------------------- | ---------------------------------------------------------------- |
| `ro.service.ts`       | Generic Restful Objects client (objects, collections, actions)   |
| `blog-api.service.ts` | Domain facade: one method per Blog/Post action or property       |
| `auth.service.ts`     | Cookie session, identity from `GET /api/auth/me`    |
| `auth.interceptor.ts` | Handles API 401/403; Angular sends the CSRF header         |
| `ro.types.ts`         | Typings for RO representations + validation-error extraction     |

Invocation conventions (verified against Causeway 3.6):

- **Safe** actions → `GET .../actions/{id}/invoke` with the query
  parameter `x-causeway-querystring` containing JSON (`{"name":{"value":"My"}}`).
  Actions without arguments still require this parameter, with `{}` as its value.
- **Idempotent** actions → `PUT .../invoke` with `{"param":{"value":...}}`.
- **Non-idempotent** actions → `POST .../invoke` with the same body shape.
- **Editable properties** → `PUT .../properties/{id}` with `{"value":...}`.
- **Image parameters** (`BufferedImage`) take a Blob-style value:
  `{"value":{"name":"x.png","mimeType":"image/png","bytes":"<base64>"}}`.
- List-returning actions produce a `causeway.applib.DomainObjectList` view
  model; the elements are fetched from its `objects` collection.

Notes:

- The home page relies on `Blogs.listAll()`. It was originally
  `RestrictTo.PROTOTYPING`, which made it vanish from the REST API (404,
  empty home page) whenever the backend ran in production mode; the
  restriction has been removed since the action only returns the current
  user's own blogs.
- The Causeway action *prompt* endpoints (`.../actions/{id}` without
  `/invoke`) return 400 in this backend version, so parameter defaults
  (e.g. AI-generated post defaults in Dev+Ai mode) are not pre-filled; the
  client falls back to empty forms and lets the server validate.
- The article previews (`picturePreview`, `contentPreview`) are HTML built
  and sanitized server-side (`ImageSupport`/`MarkupSupport`), rendered with
  `bypassSecurityTrustHtml`.

## Building for production

```bash
npm run build   # outputs to dist/acb-blog-webclient
```

The build emits static files that could be served by the Spring Boot app
itself (e.g. copied into `src/main/resources/static` or wired up with
`frontend-maven-plugin`) so client and API share an origin in deployment.
