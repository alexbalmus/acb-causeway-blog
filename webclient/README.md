# ACB Blog — Angular web client

An Angular frontend for the ACB Blog Causeway application. It consumes the
REST API that Apache Causeway auto-generates from the domain model (the
[Restful Objects](https://www.restfulobjects.org) viewer, served under
`/restful`), and mirrors the functionality of the default Wicket UI:

- **Sign in** with the app's users (HTTP Basic against `/restful/user`)
- **Home**: your blogs, New Blog, Change Handle, Delete Blog, Find Blogs
- **Blog page**: rename, posts list, New Post (title, content, picture,
  picture description), Delete Post, Delete Blog
- **Post page**: server-rendered article preview (picture + content, the same
  markup the Wicket UI shows), edit content, rename, update/clear picture,
  edit picture description
- Server-side rules are surfaced: `disabledReason`s from the API disable the
  corresponding buttons (e.g. ownership vetoes), and 422 validation responses
  are shown as per-field errors (e.g. duplicate titles, AI safety checks)

## Running

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
   `/restful` to the backend — same-origin, so no CORS setup is needed.

3. Open <http://localhost:4200> and sign in (e.g. `sven` / `pass` — the only
   user whose role currently grants the `blog.*` namespace).

## How it talks to Causeway

`src/app/core/` contains the API layer:

| File                  | Role                                                             |
| --------------------- | ---------------------------------------------------------------- |
| `ro.service.ts`       | Generic Restful Objects client (objects, collections, actions)   |
| `blog-api.service.ts` | Domain facade: one method per Blog/Post action or property       |
| `auth.service.ts`     | Basic-auth credentials, validated against `GET /restful/user`    |
| `auth.interceptor.ts` | Attaches credentials to `/restful` requests, handles 401         |
| `ro.types.ts`         | Typings for RO representations + validation-error extraction     |

Invocation conventions (verified against Causeway 3.6):

- **Safe** actions → `GET .../actions/{id}/invoke` with *plain* query
  parameters (`?name=My`), not JSON-encoded ones.
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
