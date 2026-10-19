import { test, expect, Page } from '@playwright/test';
import { randomUUID } from 'node:crypto';

const angularOrigin = 'http://127.0.0.1:14200';
const unique = (prefix: string) => `${prefix}-${randomUUID().slice(0, 12)}`;
const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=', 'base64');

async function signIn(page: Page, username = 'sven', password = 'pass') {
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
}
async function createJteBlog(page: Page, name = unique('Journal')) {
  await page.goto('/blog/my');
  await signIn(page);
  await page.getByRole('link', { name: '+ New Blog', exact: true }).click();
  await page.getByLabel('Name', { exact: true }).fill(name);
  const handle = page.getByLabel('Handle', { exact: true });
  if (await handle.isEditable()) await handle.fill('sven');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page).toHaveURL(/\/blog\/blogs\/[^/]+\/edit$/);
  return { name, edit: page.url() };
}
async function createJtePost(page: Page, title = unique('Article')) {
  await page.getByRole('link', { name: '+ New Post', exact: true }).click();
  await page.getByLabel('Title', { exact: true }).fill(title);
  await page.getByLabel('Content', { exact: true }).fill('Original paragraph');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page).toHaveURL(/\/blog\/posts\/[^/]+\/edit$/);
  return { title, edit: page.url() };
}

test.beforeEach(async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', error => errors.push(error.message));
  // Assert after the whole workflow, including navigation and HTMX updates.
  (page as Page & { browserErrors: string[] }).browserErrors = errors;
});
test.afterEach(async ({ page }) => {
  expect((page as Page & { browserErrors: string[] }).browserErrors).toEqual([]);
});

test('JTE edits, validates duplicates and renders escaped content without a document reload', async ({ page }) => {
  const blog = await createJteBlog(page);
  await page.evaluate(() => { (window as unknown as { identity: string }).identity = 'preserved'; });
  const post = await createJtePost(page);
  await page.getByRole('link', { name: 'Edit Content', exact: true }).click();
  await page.getByLabel('Content', { exact: true }).fill('Updated paragraph\n\n<script>window.injected = true</script>');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.locator('article')).toContainText('Updated paragraph');
  expect(await page.evaluate(() => (window as unknown as { identity: string }).identity)).toBe('preserved');
  expect(await page.evaluate(() => (window as unknown as { injected?: boolean }).injected)).toBeUndefined();
  await expect(page.locator('article script')).toHaveCount(0);
  await page.goto(blog.edit);
  await page.getByRole('link', { name: '+ New Post', exact: true }).click();
  await page.getByLabel('Title', { exact: true }).fill(post.title);
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.getByText(`Post with title '${post.title}' already defined for this blog`, { exact: true })).toBeVisible();
  await expect(page.getByLabel('Title', { exact: true })).toHaveValue(post.title);
});

test('public reading, search, history and mobile layout', async ({ page, browser }) => {
  const blog = await createJteBlog(page);
  const post = await createJtePost(page);
  const anonymous = await browser.newContext({ viewport: { width: 390, height: 844 } });
  try {
    const reader = await anonymous.newPage();
    await reader.goto(post.edit.replace('/edit', ''));
    await expect(reader.locator('article')).toContainText('Original paragraph');
    await expect(reader.getByRole('link', { name: 'Edit Content', exact: true })).toHaveCount(0);
    expect(await reader.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await reader.goto('/blog');
    await reader.getByLabel('Find Blogs', { exact: true }).fill(blog.name);
    await reader.getByRole('button', { name: 'Search', exact: true }).click();
    await expect(reader.getByRole('link', { name: blog.name, exact: true })).toBeVisible();
    await reader.getByLabel('Find Blogs', { exact: true }).fill(unique('missing'));
    await reader.getByRole('button', { name: 'Search', exact: true }).click();
    await expect(reader.getByText('No blogs found.', { exact: true })).toBeVisible();
    await reader.goBack();
    await expect(reader.getByRole('link', { name: blog.name, exact: true })).toBeVisible();
  } finally { await anonymous.close(); }
});

test('ordinary forms work with JavaScript disabled', async ({ browser }) => {
  const context = await browser.newContext({ javaScriptEnabled: false });
  try {
    const page = await context.newPage();
    await createJteBlog(page);
    const renamed = unique('No-JS');
    await page.getByRole('link', { name: 'Rename', exact: true }).click();
    await page.getByLabel('Name', { exact: true }).fill(renamed);
    await page.getByRole('button', { name: 'Save', exact: true }).click();
    await expect(page.getByRole('heading', { name: renamed, exact: true })).toBeVisible();
  } finally { await context.close(); }
});

test('JTE session expiry returns an HTMX editor to login and restores its destination', async ({ page, context }) => {
  await createJteBlog(page);
  const post = await createJtePost(page);
  const token = await page.locator('meta[name="blog-csrf"]').getAttribute('content');
  const response = await context.request.post('/blog/logout', { headers: { 'X-Blog-CSRF': token! }, maxRedirects: 0 });
  expect(response.status()).toBe(303);
  await page.getByRole('link', { name: 'Edit Content', exact: true }).click();
  await expect(page).toHaveURL(/\/blog\/login$/);
  await signIn(page);
  await expect(page).toHaveURL(post.edit + '?panel=content');
  await expect(page.getByLabel('Content', { exact: true })).toBeVisible();
  await expect(page.locator('nav[aria-label="Main navigation"]')).toHaveCount(1);
});

test('JTE uploads, edits captions, clears pictures and confirms cascade deletion', async ({ page, context }) => {
  const blog = await createJteBlog(page);
  const post = await createJtePost(page);
  await page.getByRole('link', { name: 'Update Picture', exact: true }).click();
  await page.getByLabel('Picture (optional)', { exact: true }).setInputFiles({ name: 'pixel.png', mimeType: 'image/png', buffer: png });
  await page.getByLabel('Picture description', { exact: true }).fill('Initial caption');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.locator('article img')).toBeVisible();
  await expect(page.locator('article')).toContainText('Initial caption');
  await page.getByRole('link', { name: 'Edit Description', exact: true }).click();
  await page.getByLabel('Picture description', { exact: true }).fill('Edited caption');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.locator('article')).toContainText('Edited caption');
  await page.getByRole('link', { name: 'Clear Picture', exact: true }).click();
  await page.getByRole('button', { name: 'Remove', exact: true }).click();
  await expect(page.locator('article img')).toHaveCount(0);
  await expect(page.locator('article')).not.toContainText('Edited caption');
  await page.goto(blog.edit);
  await page.getByRole('link', { name: 'Delete Blog', exact: true }).click();
  await expect(page.getByText('and all its posts', { exact: false })).toBeVisible();
  await page.getByRole('button', { name: 'Delete', exact: true }).click();
  await expect(page).toHaveURL(/\/blog\/my$/);
  expect((await context.request.get(new URL(post.edit).pathname.replace('/edit', ''))).status()).toBe(404);
  expect((await context.request.get(new URL(blog.edit).pathname.replace('/edit', ''))).status()).toBe(404);
});

test('Angular creates and edits persisted posts and pictures visible in JTE, then deletes them', async ({ page, context }) => {
  page.on('dialog', dialog => dialog.accept());
  await page.goto(angularOrigin);
  await signIn(page);
  const name = unique('Angular');
  await page.getByRole('button', { name: '+ New Blog', exact: true }).click();
  await page.getByLabel('Name', { exact: true }).fill(name);
  const handle = page.getByLabel('Handle', { exact: true });
  if (await handle.isEditable()) await handle.fill('sven');
  await page.getByRole('button', { name: 'Create', exact: true }).click();
  await expect(page).toHaveURL(/\/blogs\/[^/]+$/);
  await expect(page.getByRole('heading', { level: 1 })).toContainText(name);
  const blogUrl = page.url();
  await page.getByRole('button', { name: '+ New Post', exact: true }).click();
  const title = unique('Angular-post');
  await page.getByLabel('Title', { exact: true }).fill(title);
  await page.getByLabel('Content', { exact: true }).fill('Written in Angular');
  await page.getByLabel('Picture (optional)', { exact: true }).setInputFiles({ name: 'pixel.png', mimeType: 'image/png', buffer: png });
  await page.getByLabel('Picture description (optional)', { exact: true }).fill('Uploaded picture');
  await page.getByRole('button', { name: 'Create Post', exact: true }).click();
  await expect(page).toHaveURL(/\/posts\/[^/]+$/);
  const postId = new URL(page.url()).pathname.split('/').pop();
  await expect(page.locator('article')).toContainText('Written in Angular');
  await expect(page.locator('article img')).toBeVisible();
  await page.getByRole('button', { name: /Edit Content/ }).click();
  await page.getByLabel('Content', { exact: true }).fill('Changed in Angular');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.locator('article')).toContainText('Changed in Angular');
  await page.reload();
  await expect(page.locator('article')).toContainText('Changed in Angular');
  const reader = await context.newPage();
  await reader.goto('/blog/posts/' + postId);
  await expect(reader.locator('article')).toContainText('Changed in Angular');
  await expect(reader.locator('article img')).toBeVisible();
  await page.getByRole('button', { name: /Clear Picture/ }).click();
  await expect(page.locator('article img')).toHaveCount(0);
  await reader.reload();
  await expect(reader.locator('article img')).toHaveCount(0);
  await page.getByRole('button', { name: /Delete$/, exact: false }).click();
  await expect(page).toHaveURL(blogUrl);
  expect((await context.request.get('/blog/posts/' + postId)).status()).toBe(404);
  await page.getByRole('button', { name: /Delete Blog/ }).click();
  await expect(page).toHaveURL(angularOrigin + '/');
  await expect(page.getByRole('link', { name, exact: true })).toHaveCount(0);
});

test('Angular rejects bad credentials, guards routes, restores login and signs out', async ({ page }) => {
  await page.goto(angularOrigin + '/blogs/999999');
  await expect(page).toHaveURL(angularOrigin + '/login');
  await signIn(page, 'sven', 'wrong');
  await expect(page).toHaveURL(angularOrigin + '/login');
  await expect(page.locator('[role="alert"]')).toBeVisible();
  await signIn(page);
  await expect(page.getByRole('button', { name: 'Sign out', exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('button', { name: 'Sign out', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Sign out', exact: true }).click();
  await expect(page).toHaveURL(angularOrigin + '/login');
  await page.goto(angularOrigin);
  await expect(page).toHaveURL(angularOrigin + '/login');
});

test('JTE logout preserves independent Angular Basic and Wicket authentication', async ({ page, context }) => {
  await createJteBlog(page);
  const angular = await context.newPage();
  await angular.goto(angularOrigin);
  await signIn(angular);
  await expect(angular.getByRole('button', { name: 'Sign out', exact: true })).toBeVisible();
  const wicket = await context.newPage();
  await wicket.goto('/wicket/');
  await wicket.locator('input[name="username"]').fill('sven');
  await wicket.locator('input[type="password"]').fill('pass');
  // Wicket populates this required field asynchronously using the browser timezone.
  await expect(wicket.getByLabel('Timezone', { exact: true })).not.toHaveValue('');
  await wicket.locator('input[type="submit"], button[type="submit"]').first().click();
  await expect(wicket.locator('input[type="password"]')).toHaveCount(0);
  const token = await page.locator('meta[name="blog-csrf"]').getAttribute('content');
  expect((await context.request.post('/blog/logout', { headers: { 'X-Blog-CSRF': token! }, maxRedirects: 0 })).status()).toBe(303);
  await wicket.reload();
  await expect(wicket.locator('input[type="password"]')).toHaveCount(0);
  await angular.reload();
  await expect(angular.getByRole('button', { name: 'Sign out', exact: true })).toBeVisible();
  await page.goto('/blog/my');
  await expect(page).toHaveURL(/\/blog\/login$/);
});
