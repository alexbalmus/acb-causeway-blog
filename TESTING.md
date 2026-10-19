# Regression tests

Run the same checks locally and in `.github/workflows/regression.yml`. Tests use
real Causeway 4.0.0-M2, EclipseLink, H2, compiled JTE templates and the production
Angular bundle. No external AI account or running development server is required.

Prerequisites: Java 25 and Maven. From the repository root:

```sh
mvn package
```

Maven runs Java tests, installs a pinned Node/npm toolchain under `target/frontend`,
installs locked client dependencies and Chromium, runs Angular tests with coverage,
builds the production Angular client, packages the executable JAR, and then runs
all Angular, HTMX and Wicket browser workflows. Any failure fails the Maven build.
No global Node/npm installation or manual npm command is required. Initial builds
need network access for tool/dependency downloads. `mvn clean package` and
`mvn verify` also run the complete suite; browser checks run once in the package phase.

On Linux, install Chromium's system libraries with the following command (also
used by CI); installing system packages may require sudo:

```sh
mvn package "-Dplaywright.install.args=install --with-deps chromium"
```

Angular's Karma runner automatically uses Playwright's Chromium. Both runners
honor `CHROME_BIN` when supplied. Use `mvn test` for Java tests only, or
`mvn package -DskipTests` to build Java and Angular while skipping all tests.
`"-Dmaven.test.skip=true"` also skips the tests and Java test compilation.

## Coverage

| Layer | What failures it catches |
| --- | --- |
| Existing Java unit tests | Sanitization, HTML escaping, ownership decisions, moderation rules, AI response parsing, image limits and session lifecycle |
| Application startup | Invalid Causeway metamodel or Spring bean wiring |
| JTE HTTP integration | Public reading, full pages/fragments/assets, native authentication, realm permissions, owner/admin isolation, CSRF and rotation, logout, validation, uploads and moderation rollback |
| REST HTTP integration | Basic authentication and request isolation; actual action methods and argument representations; persistence, list/search results, duplicate/length validation; handle propagation; ownership vetoes despite admin permissions; post deletion and blog cascade deletion |
| Angular HTTP integration | Login and session restore, failed/stale credentials, route guards, interceptor headers, safe/non-idempotent/idempotent action encoding, null properties, same-origin absolute links and field/general error feedback |
| Browser workflows | Actual Angular/JTE forms, HTMX document preservation, validation feedback, escaped content, browser history, public mobile layout, forms without JavaScript, session expiry, pictures and edits visible across clients, deletion and Angular login/logout |
| Wicket browser workflows | Native login/logout, action dialogs and cancellation, required/duplicate validation, blog/post renaming, property editors, escaped previews, picture upload/replacement/captions/removal, deletion confirmations and cascades, service menus/search, collection navigation, handle propagation and ownership/realm restrictions |

The browser runner starts and stops a disposable application on `127.0.0.1:18080`
and an Angular static server/proxy on `127.0.0.1:14200`. It refuses to reuse existing
servers, protecting developer data. Browser tests use unique names and isolated
cookie/storage contexts. Both desktop and mobile Chromium run the workflows;
these are viewport/touch checks, not Safari or physical-device testing.

The browser launcher runs the packaged application through Spring Boot's
`PropertiesLauncher`, adding `target/test-classes` to the classpath. Its test-only
`BrowserTestApplication` enables the `browser-tests` profile and creates three
author accounts with full realm permissions. This allows ownership checks between
authors without changing the production realm. The launcher and accounts are
excluded from the application JAR. Maven compiles these fixtures before starting
the browser suite. Tests perform Wicket mutations through rendered UI controls;
public HTTP reads additionally verify that changes reached the shared backend.
The collection-table scenario seeds twelve posts through the real REST API before
testing Wicket sorting, filtering and pagination. Other mutations use Wicket controls.
The homepage retains its original Blogs/Manage layout. Homepage tests exercise
creation, filtering and navigation; blog deletion is tested from the blog detail
page and handle changes through the Other service menu. The suite does not claim
coverage of the homepage Manage actions, which are not rendered by the current viewer.
Wicket coverage targets this application's blogging UI; framework maintenance and
prototyping tools such as metamodel downloads and impersonation are outside this suite.

Java tests use in-memory databases and write generated DDL under `target/`.
AI-enabled Java tests mock AI boundaries; they verify application behavior without
network calls, provider costs or nondeterministic model responses.

## Diagnosing a failure

The optional npm commands below are for focused frontend debugging after a full
Maven build; they require Node/npm on your PATH (Maven's local Node directory is
`target/frontend/node`). They are not required for the complete regression suite.

* Java reports: `target/surefire-unittest-reports/`.
* Angular coverage: `webclient/coverage/`.
* Browser report: `cd webclient && npm run test:e2e:report`.
* Failed browser tests retain a screenshot, video and trace in `webclient/test-results/`.
  Open a trace with `npx playwright show-trace <path-to-trace.zip>`.
* Run one browser workflow with `npm run test:e2e -- --project=desktop -g "Angular creates"`.
* Run only Wicket workflows with `npm run test:e2e:wicket` (both desktop and mobile),
  or `npm run test:e2e:wicket -- --project=desktop`.
* Run one Java class with `mvn test -Dtest=RestfulObjectsIntegrationTest`.

CI runs on pushes, pull requests and manual dispatch, and uploads reports even on
failure. Configure the repository's branch protection to require the
`regression` job before merging; committing a workflow alone does not enforce that policy.

No finite suite guarantees every change is safe. This suite targets the current
application's behavior; production database compatibility, deployment configuration,
load/concurrency, accessibility audits, other browser engines and live AI provider
compatibility need separate checks when those become requirements. Add a failing
regression test when fixing a bug and update tests when behavior intentionally changes.
