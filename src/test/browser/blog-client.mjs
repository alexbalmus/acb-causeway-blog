// Run from the repository root after mvn package and the Angular production build.
// Test-only dependency: npm install --prefix target/browser --no-save playwright
import { chromium } from '../../../target/browser/node_modules/playwright/index.mjs';
import { spawn } from 'node:child_process';
import { createServer, request as httpRequest } from 'node:http';
import { readFileSync, existsSync, createWriteStream } from 'node:fs';
import { resolve, extname } from 'node:path';
import assert from 'node:assert/strict';

const origin = 'http://localhost:18080';
const angularOrigin = 'http://localhost:14200';
const log = createWriteStream('target/browser-app.log');
const app = spawn('java', ['-jar', 'target/acb-blog-1.0.0-SNAPSHOT-exec.jar', '--server.port=18080',
  '--spring.profiles.active=Dev', '--spring.ai.openai.api-key=test-only', '--eclipselink.application-location=target'],
  { windowsHide: true });
app.stdout.pipe(log); app.stderr.pipe(log);
let browser, page;
const webRoot = resolve('webclient/dist/acb-blog-webclient/browser');
const server = createServer((req, res) => {
  if (/^\/restful(\/|$)/.test(req.url)) {
    const proxy = httpRequest({ hostname: '127.0.0.1', port: 18080, path: req.url, method: req.method, headers: req.headers }, incoming => {
      res.writeHead(incoming.statusCode, incoming.headers); incoming.pipe(res);
    });
    proxy.on('error', () => { res.writeHead(502); res.end(); }); req.pipe(proxy); return;
  }
  const candidate = resolve(webRoot, '.' + new URL(req.url, angularOrigin).pathname);
  const file = candidate.startsWith(webRoot + '/') || candidate.startsWith(webRoot + '\\') ? candidate : '';
  const resolved = file && existsSync(file) && extname(file) ? file : resolve(webRoot, 'index.html');
  const types = { '.html': 'text/html', '.js': 'application/javascript', '.css': 'text/css', '.ico': 'image/x-icon' };
  res.setHeader('Content-Type', types[extname(resolved)] || 'application/octet-stream'); res.end(readFileSync(resolved));
});
async function signIn(target) {
  await target.getByLabel('Username').fill('sven');
  await target.getByLabel('Password', { exact: true }).fill('pass');
  await target.getByRole('button', { name: 'Sign in', exact: true }).click();
}
try {
  await new Promise(resolve => server.listen(14200, '127.0.0.1', resolve));
  let ready = false;
  for (let i = 0; i < 90; i++) {
    if (app.exitCode !== null) throw Error('App exited: see target/browser-app.log');
    if (existsSync('target/browser-app.log') && readFileSync('target/browser-app.log', 'utf8').includes('Started ACBBlogApp')) { ready = true; break; }
    await new Promise(resolve => setTimeout(resolve, 1000));
  }
  assert.ok(ready, 'Application must start');
  browser = await chromium.launch({ executablePath: process.env.CHROME_BIN || 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
  const context = await browser.newContext({ viewport: { width: 1365, height: 950 } });
  page = await context.newPage();
  const errors = []; page.on('pageerror', error => errors.push(error.message));
  await page.goto(origin + '/blog/my');
  await signIn(page);
  await page.waitForURL('**/blog/my');
  await page.evaluate(() => window.testDocumentIdentity = 'same-document');
  await page.getByRole('link', { name: '+ New Blog', exact: true }).click();
  await page.getByLabel('Name', { exact: true }).fill('Autumn journal');
  await page.getByLabel('Handle', { exact: true }).fill('sven');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await page.waitForURL('**/blog/blogs/*/edit');
  assert.equal(await page.evaluate(() => window.testDocumentIdentity), 'same-document');
  const blogEdit = page.url();
  await page.getByRole('link', { name: '+ New Post', exact: true }).click();
  await page.getByLabel('Title', { exact: true }).fill('Notes from the trail');
  await page.getByLabel('Content', { exact: true }).fill('A quiet morning on the trail.\n\nRendered by JTE and updated with HTMX.');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await page.waitForURL('**/blog/posts/*/edit');
  const postEdit = page.url();
  assert.equal(await page.evaluate(() => window.testDocumentIdentity), 'same-document');
  await page.getByRole('link', { name: 'Edit Content', exact: true }).click();
  await page.getByLabel('Content', { exact: true }).fill('Updated through HTMX without reloading.\n\n<script>escaped text</script>');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await page.waitForFunction(() => document.querySelector('article')?.textContent.includes('Updated through HTMX') && !document.querySelector('textarea'));
  assert.equal(await page.evaluate(() => window.testDocumentIdentity), 'same-document');
  await page.screenshot({ path: 'target/jte-post.png', fullPage: true });
  await page.goto(origin + '/blog/my');
  await page.getByRole('link', { name: '+ New Blog', exact: true }).click();
  await page.getByLabel('Name', { exact: true }).fill('Autumn journal');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await page.locator('#blog-content [role="alert"]').waitFor();
  assert.equal(await page.getByLabel('Name', { exact: true }).inputValue(), 'Autumn journal');
  // Native Basic authentication remains separate from the JTE session.
  const angular = await context.newPage();
  await angular.goto(angularOrigin);
  await signIn(angular);
  await angular.getByRole('link', { name: 'Autumn journal', exact: true }).click();
  await angular.getByRole('link', { name: 'Notes from the trail', exact: true }).click();
  await angular.getByText('Updated through HTMX without reloading.', { exact: true }).waitFor();
  // Native Wicket authentication also remains separate.
  const wicket = await context.newPage();
  await wicket.goto(origin + '/wicket/');
  await wicket.locator('input[name="username"]').fill('sven');
  await wicket.locator('input[type="password"]').fill('pass');
  await wicket.locator('input[type="submit"], button[type="submit"]').first().click();
  await wicket.locator('input[type="password"]').waitFor({ state: 'hidden' });
  const publicContext = await browser.newContext({ viewport: { width: 390, height: 844 } });
  const mobile = await publicContext.newPage();
  await mobile.goto(postEdit.replace('/edit', ''));
  assert.equal(await mobile.getByRole('link', { name: 'Edit', exact: true }).count(), 1);
  assert.ok(await mobile.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
  await mobile.screenshot({ path: 'target/jte-mobile.png', fullPage: true });
  await mobile.getByRole('link', { name: 'Edit', exact: true }).click();
  await signIn(mobile);
  await mobile.waitForURL(postEdit);
  const fallback = await (await browser.newContext({ javaScriptEnabled: false })).newPage();
  await fallback.goto(blogEdit);
  await signIn(fallback);
  await fallback.getByRole('link', { name: 'Rename', exact: true }).click();
  await fallback.getByLabel('Name', { exact: true }).fill('Autumn renamed without JavaScript');
  await fallback.getByRole('button', { name: 'Save', exact: true }).click();
  await fallback.getByRole('heading', { name: 'Autumn renamed without JavaScript' }).waitFor();
  await page.goto(origin + '/blog');
  await page.getByLabel('Find Blogs', { exact: true }).fill('nothing-matches');
  await page.getByRole('button', { name: 'Search', exact: true }).click();
  await page.waitForURL('**/blog?q=nothing-matches');
  await page.getByText('No blogs found.', { exact: true }).waitFor();
  await page.goBack();
  await page.getByRole('link', { name: 'Autumn renamed without JavaScript', exact: true }).waitFor();
  await page.goto(postEdit);
  const token = await page.locator('meta[name="blog-csrf"]').getAttribute('content');
  assert.equal((await context.request.post(origin + '/blog/logout', { headers: { 'X-Blog-CSRF': token }, maxRedirects: 0 })).status(), 303);
  await wicket.reload();
  assert.equal(await wicket.locator('input[type="password"]').count(), 0);
  await angular.reload();
  await angular.getByText('Updated through HTMX without reloading.', { exact: true }).waitFor();
  await page.getByRole('link', { name: 'Edit Content', exact: true }).click();
  await page.waitForURL('**/blog/login');
  await signIn(page);
  await page.waitForURL(postEdit + '?panel=content');
  await page.getByLabel('Content', { exact: true }).waitFor();
  assert.equal(await page.locator('nav[aria-label="Main navigation"]').count(), 1);
  assert.deepEqual(errors, []);
  console.log('PASS: native Causeway login, HTMX CRUD/validation/history, public mobile reading, no-JS forms, session expiry, Angular Basic and Wicket login/logout independence.');
} catch (error) {
  if (page) { console.log('Page at failure:', page.url(), await page.locator('body').innerText()); await page.screenshot({ path: 'target/browser-failure.png', fullPage: true }); }
  throw error;
} finally {
  await browser?.close(); server.close(); app.kill(); log.end();
}
