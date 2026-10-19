import { test, expect, Page } from '@playwright/test';
import { action, choose, createBlog, createPost, field, home, login, menu, navigation,
  png, prompt, propertyEditor, ready, submit, unique } from './wicket-helpers';

test.use({ actionTimeout: 15_000 });
const runtimeErrors = new WeakMap<Page, string[]>();
test.beforeEach(async ({ page, context }) => {
  const errors: string[] = [];
  const watch = (p: Page) => p.on('pageerror', error => errors.push(error.message));
  watch(page); context.on('page', watch); runtimeErrors.set(page, errors);
});
test.afterEach(async ({ page }) => expect(runtimeErrors.get(page)).toEqual([]));

async function start(page: Page, user = 'wicket-writer') {
  await login(page, user);
  await home(page);
}
async function cancel(page: Page) {
  await prompt(page).getByRole('button', { name: 'Cancel', exact: true }).click();
  await expect(prompt(page)).toHaveCount(0);
  await ready(page);
}
async function confirm(page: Page) {
  await page.getByRole('button', { name: 'OK', exact: true }).filter({ visible: true }).click();
  await ready(page);
}
async function confirmAction(page: Page) {
  await page.getByRole('tooltip').filter({ hasText: 'Are you sure?' })
    .getByRole('link', { name: 'Confirm', exact: true }).click();
  await ready(page);
}
async function publicPost(page: Page, id: string) {
  const response = await page.request.get('/blog/posts/' + id);
  expect(response.status()).toBe(200);
  return response.text();
}

test('Wicket rejects bad credentials, restores a logged-in page and logs out', async ({ page }) => {
  await page.goto('/wicket/');
  await page.locator('input[name="username"]').fill('wicket-writer');
  await page.locator('input[type="password"]').fill('wrong');
  await expect(page.getByLabel('Timezone', { exact: true })).not.toHaveValue('');
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.locator('input[type="password"]')).toBeVisible();
  await expect(page.locator('body')).toContainText(/sign in failed|invalid|incorrect|authentication failed/i);
  await start(page);
  const blog = await createBlog(page);
  await page.reload(); await ready(page);
  await expect(page.locator('h3 a.objectUrlSource')).toContainText(blog.name);
  const nav = await navigation(page);
  const userMenu = nav.getByRole('button', { name: 'wicket-writer', exact: true });
  if ((page.viewportSize()?.width ?? 1280) >= 768) await userMenu.hover();
  else await userMenu.click();
  await nav.getByRole('link', { name: 'Logout', exact: true }).click();
  await expect(page.locator('input[name="username"]')).toBeVisible();
  await page.waitForLoadState('load');
  await page.goto(blog.url);
  await expect(page.locator('input[name="username"]')).toBeVisible();
});

test('Wicket blog dialogs enforce required fields, preserve duplicates and cancel creation', async ({ page }) => {
  await start(page);
  await action(page, 'New Blog');
  await submit(page);
  await expect(prompt(page)).toBeVisible();
  await expect(prompt(page)).toContainText(/required/i);
  const cancelled = unique('Cancelled');
  await field(prompt(page), 'Name').fill(cancelled);
  await cancel(page);
  await expect(page.locator('table')).not.toContainText(cancelled);
  const blog = await createBlog(page);
  await home(page);
  await action(page, 'New Blog');
  await field(prompt(page), 'Name').fill(blog.name);
  await expect(field(prompt(page), 'Handle')).not.toBeEditable();
  await submit(page);
  await expect(prompt(page)).toContainText('already exists');
  await expect(field(prompt(page), 'Name')).toHaveValue(blog.name);
  await cancel(page);
  await page.getByRole('searchbox', { name: 'Search:', exact: true }).fill(blog.name);
  await expect(page.locator('tbody tr')).toHaveCount(1);
});

test('Wicket renames a blog and keeps the new name after reload', async ({ page }) => {
  await start(page);
  const blog = await createBlog(page);
  await action(page, 'Rename');
  await expect(field(prompt(page), 'Name')).toHaveValue(blog.name);
  const name = unique('Renamed');
  await field(prompt(page), 'Name').fill(name);
  await submit(page);
  await expect(page.locator('h3 a.objectUrlSource')).toContainText(name);
  await page.reload(); await ready(page);
  await expect(page.locator('.causeway-blog-Blog-name')).toContainText(name);
  expect(await (await page.request.get('/blog/blogs/' + blog.id)).text()).toContain(name);
});

test('Wicket creates a post, edits its title and content, and renders escaped paragraphs', async ({ page }) => {
  await start(page);
  await createBlog(page);
  const post = await createPost(page);
  await action(page, 'Rename');
  const renamed = unique('Renamed-post');
  await field(prompt(page), 'Name').fill(renamed);
  await submit(page);
  await expect(page.locator('h3 a.objectUrlSource')).toHaveText(renamed);
  await propertyEditor(page, 'content');
  const body = 'Edited through Wicket\n\n<script>window.wicketInjected = true</script>';
  await page.locator('textarea:not([readonly])').fill(body);
  await confirm(page);
  await expect(page.locator('.causeway-blog-Post-contentPreview')).toContainText('Edited through Wicket');
  await expect(page.locator('.causeway-blog-Post-contentPreview script')).toHaveCount(0);
  expect(await page.evaluate(() => (window as unknown as { wicketInjected?: boolean }).wicketInjected)).toBeUndefined();
  await page.reload(); await ready(page);
  await expect(page.locator('.causeway-blog-Post-content textarea')).toHaveValue(body);
  expect(await publicPost(page, post.id)).toContain('Edited through Wicket');
});

test('Wicket rejects duplicate post creation and rename without changing saved posts', async ({ page }) => {
  await start(page);
  const blog = await createBlog(page);
  const first = await createPost(page);
  await page.goto(blog.url); await ready(page);
  await action(page, 'New Post');
  await field(prompt(page), 'Name').fill(first.name);
  await submit(page);
  await expect(prompt(page)).toContainText('already defined');
  await expect(field(prompt(page), 'Name')).toHaveValue(first.name);
  await cancel(page);
  const second = await createPost(page);
  await action(page, 'Rename');
  await field(prompt(page), 'Name').fill(first.name);
  await submit(page);
  await expect(prompt(page)).toContainText(/already|exists/i);
  await cancel(page);
  await expect(page.locator('h3 a.objectUrlSource')).toHaveText(second.name);
  expect(await publicPost(page, first.id)).toContain(first.name);
  expect(await publicPost(page, second.id)).toContain(second.name);
});

test('Wicket uploads and replaces pictures, edits captions, and clears the picture', async ({ page }) => {
  await start(page);
  await createBlog(page);
  const post = await createPost(page);
  await page.getByRole('link', { name: 'Picture', exact: true }).click(); await ready(page);
  await action(page, 'Update Picture');
  await prompt(page).locator('input[type="file"]').setInputFiles({ name: 'picture.png', mimeType: 'image/png', buffer: png });
  await field(prompt(page), 'Picture description').fill('Wicket caption');
  await submit(page);
  await expect(page.locator('.causeway-blog-Post-picturePreview img')).toBeVisible();
  expect(await publicPost(page, post.id)).toContain('Wicket caption');
  await page.getByRole('link', { name: 'Picture', exact: true }).click(); await ready(page);
  await propertyEditor(page, 'pictureDescription');
  await page.locator('textarea:not([readonly])').fill('Changed caption');
  await confirm(page);
  expect(await publicPost(page, post.id)).toContain('Changed caption');
  await action(page, 'Update Picture');
  await prompt(page).locator('input[type="file"]').setInputFiles({ name: 'replacement.png', mimeType: 'image/png', buffer: png });
  await field(prompt(page), 'Picture description').fill('Replacement caption');
  await submit(page);
  expect(await publicPost(page, post.id)).toContain('Replacement caption');
  await action(page, 'Clear Picture');
  await ready(page);
  await expect(page.locator('.causeway-blog-Post-picturePreview img')).toHaveCount(0);
  expect(await publicPost(page, post.id)).not.toContain('Replacement caption');
});

test('Wicket delete confirmation can be cancelled and deleting a post removes it', async ({ page }) => {
  await start(page);
  const blog = await createBlog(page);
  const post = await createPost(page);
  await action(page, 'Delete Post');
  await page.getByRole('tooltip').filter({ hasText: 'Are you sure?' })
    .getByRole('link', { name: 'Cancel', exact: true }).click();
  await ready(page);
  expect(await publicPost(page, post.id)).toContain(post.name);
  await action(page, 'Delete Post');
  await confirmAction(page);
  expect((await page.request.get('/blog/posts/' + post.id)).status()).toBe(404);
  await page.goto(blog.url); await ready(page);
  await expect(page.locator('table')).not.toContainText(post.name);
});

test('Wicket deletes a selected post by title and cascades blog deletion', async ({ page }) => {
  await start(page);
  const blog = await createBlog(page);
  const first = await createPost(page);
  await page.goto(blog.url); await ready(page);
  const second = await createPost(page);
  await page.goto(blog.url); await ready(page);
  await action(page, 'Delete Post');
  await choose(page, first.name);
  await submit(page);
  await confirmAction(page);
  expect((await page.request.get('/blog/posts/' + first.id)).status()).toBe(404);
  expect((await page.request.get('/blog/posts/' + second.id)).status()).toBe(200);
  await action(page, 'Delete Blog');
  await confirmAction(page);
  expect((await page.request.get('/blog/blogs/' + blog.id)).status()).toBe(404);
  expect((await page.request.get('/blog/posts/' + second.id)).status()).toBe(404);
});

test('Wicket finds blogs through service menus and navigates collection rows and breadcrumbs', async ({ page }) => {
  await start(page);
  const blog = await createBlog(page);
  const post = await createPost(page);
  // Causeway opens a single result directly. Two matches exercise the result table.
  await createBlog(page, blog.name + '-second');
  await menu(page, 'Blogs', 'Find Blogs');
  await field(prompt(page), 'Name').fill(blog.name);
  await submit(page);
  const row = page.locator('tbody tr').filter({ hasText: blog.name }).filter({ hasNotText: blog.name + '-second' });
  await expect(row).toBeVisible();
  await row.getByRole('link').first().click(); await ready(page);
  await expect(page.locator('h3 a.objectUrlSource')).toContainText(blog.name);
  await page.locator('tbody tr').filter({ hasText: post.name }).getByRole('link').first().click(); await ready(page);
  await expect(page.locator('h3 a.objectUrlSource')).toHaveText(post.name);
  await page.getByRole('navigation', { name: 'breadcrumb' }).getByRole('link').click(); await ready(page);
  await expect(page.locator('h3 a.objectUrlSource')).toContainText(blog.name);
  await menu(page, 'Blogs', 'Find Blog');
  await field(prompt(page), 'Name').fill(blog.name);
  await field(prompt(page), 'Handle').fill('wicket-writer');
  await submit(page);
  await expect(page.locator('h3 a.objectUrlSource')).toContainText(blog.name);
  await menu(page, 'Blogs', 'Find Blogs');
  await field(prompt(page), 'Name').fill(unique('missing'));
  await submit(page);
  await expect(page.getByText('No results', { exact: true })).toBeVisible();
});

test('Wicket changes a handle across all owned blogs and defaults new blog forms to it', async ({ page }, testInfo) => {
  await start(page, 'wicket-handles');
  const first = await createBlog(page, unique('Wicket'), 'wicket-handles');
  const second = await createBlog(page);
  await menu(page, 'Other', 'Change Handle');
  const handle = unique('handle-' + testInfo.project.name);
  await field(prompt(page), 'Handle').fill(handle);
  await submit(page);
  await confirmAction(page);
  for (const blog of [first, second]) {
    await page.goto(blog.url); await ready(page);
    await expect(page.locator('.causeway-blog-Blog-handle')).toContainText(handle);
    expect(await (await page.request.get('/blog/blogs/' + blog.id)).text()).toContain(handle);
  }
  await home(page); await action(page, 'New Blog');
  await expect(field(prompt(page), 'Handle')).toHaveValue(handle);
  await expect(field(prompt(page), 'Handle')).not.toBeEditable();
});

test('Wicket collection tables filter, sort and paginate posts', async ({ page }) => {
  await start(page);
  const blog = await createBlog(page);
  const prefix = unique('Table');
  // Seed enough real posts to exercise paging; CRUD itself is covered through UI above.
  for (let index = 0; index < 12; index++) {
    const response = await page.request.post(`/restful/objects/blog.Blog/${blog.id}/actions/createPost/invoke`, {
      headers: { Authorization: 'Basic ' + Buffer.from('wicket-writer:pass').toString('base64') },
      data: { title: { value: `${prefix}-${String(index).padStart(2, '0')}` },
        content: { value: null }, picture: { value: null }, pictureDescription: { value: null } },
    });
    expect(response.status(), await response.text()).toBe(200);
  }
  await page.reload(); await ready(page);
  const rows = page.locator('tbody tr');
  await expect(rows).toHaveCount(10);
  await expect(page.getByRole('status')).toContainText('of 12 entries');
  await page.getByRole('navigation', { name: 'pagination' }).getByRole('link', { name: 'Next', exact: true }).click();
  await expect(rows).toHaveCount(2);
  await page.getByRole('navigation', { name: 'pagination' }).getByRole('link', { name: 'First', exact: true }).click();
  await expect(rows).toHaveCount(10);
  await page.getByRole('columnheader').filter({ hasText: 'Title' }).click();
  await expect(rows.first()).toContainText(`${prefix}-00`);
  await page.getByRole('columnheader').filter({ hasText: 'Title' }).click();
  await expect(rows.first()).toContainText(`${prefix}-11`);
  await page.getByRole('searchbox', { name: 'Search:', exact: true }).fill(`${prefix}-07`);
  await expect(rows).toHaveCount(1);
  await expect(rows).toContainText(`${prefix}-07`);
  await page.getByRole('searchbox', { name: 'Search:', exact: true }).fill(unique('missing'));
  await expect(rows).toContainText('No matching records');
  await page.getByRole('searchbox', { name: 'Search:', exact: true }).fill('');
  await page.getByRole('combobox', { name: 'entries per page', exact: true }).selectOption('25');
  await expect(rows).toHaveCount(12);
});

test('Wicket enforces ownership even for another user with full realm permissions', async ({ page, browser }) => {
  await start(page);
  const blog = await createBlog(page);
  const post = await createPost(page);
  const other = await browser.newContext();
  try {
    const reader = await other.newPage();
    await start(reader, 'wicket-other');
    await expect(reader.locator('table')).not.toContainText(blog.name);
    await reader.goto(blog.url); await ready(reader);
    await expect(reader.locator('.causeway-blog-Blog-name')).toContainText(blog.name);
    await expect(reader.locator('[data-bs-content="Only the blog owner can do this"]')).not.toHaveCount(0);
    await expect(reader.getByRole('link', { name: /^New Post/ }).filter({ visible: true })).toHaveCount(0);
    await expect(reader.getByRole('link', { name: /^Rename/ }).filter({ visible: true })).toHaveCount(0);
    await reader.goto(post.url); await ready(reader);
    await expect(reader.locator('.causeway-blog-Post-content textarea')).not.toBeEditable();
    await expect(reader.getByRole('link', { name: /^Delete Post/ }).filter({ visible: true })).toHaveCount(0);
    expect(await publicPost(page, post.id)).toContain(post.content);
  } finally { await other.close(); }
});

test('Wicket hides blog actions from a user without realm permissions', async ({ page }) => {
  await login(page, 'bob');
  await page.waitForLoadState('load');
  await expect(page.locator('input[type="password"]')).toHaveCount(0);
  const nav = await navigation(page);
  await expect(nav.getByRole('button', { name: 'bob', exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: /^New Blog/ }).filter({ visible: true })).toHaveCount(0);
  await expect(page.locator('nav.navbar').getByRole('link', { name: 'Blogs', exact: true })).toHaveCount(0);
});

test('Wicket homepage filters blogs and opens a blog and its posts', async ({ page }) => {
  await start(page);
  const blog = await createBlog(page);
  const post = await createPost(page);
  const other = await createBlog(page);
  await home(page);
  const search = page.getByRole('searchbox', { name: 'Search:', exact: true });
  const rows = page.locator('tbody tr');
  await search.fill(other.name);
  await expect(rows).toHaveCount(1);
  await expect(rows).toContainText(other.name);
  await search.fill(blog.name);
  await expect(rows).toHaveCount(1);
  await expect(rows).toContainText(blog.name);
  await expect(rows).not.toContainText(other.name);
  await rows.getByRole('link').first().click(); await ready(page);
  await expect(page.locator('h3 a.objectUrlSource')).toContainText(blog.name);
  await page.locator('tbody tr').filter({ hasText: post.name }).getByRole('link').first().click();
  await ready(page);
  await expect(page.locator('h3 a.objectUrlSource')).toHaveText(post.name);
  expect(await publicPost(page, post.id)).toContain(post.content);
});

test('Wicket rejects an unreadable picture without replacing persisted content', async ({ page }) => {
  await start(page);
  await createBlog(page);
  const post = await createPost(page);
  await page.getByRole('link', { name: 'Picture', exact: true }).click(); await ready(page);
  await action(page, 'Update Picture');
  await prompt(page).locator('input[type="file"]').setInputFiles({ name: 'original.png', mimeType: 'image/png', buffer: png });
  await field(prompt(page), 'Picture description').fill('Preserved picture');
  await submit(page);
  await expect(page.locator('.causeway-blog-Post-picturePreview img')).toBeVisible();
  await page.getByRole('link', { name: 'Picture', exact: true }).click(); await ready(page);
  await action(page, 'Update Picture');
  await prompt(page).locator('input[type="file"]').setInputFiles({
    name: 'broken.png', mimeType: 'image/png', buffer: Buffer.from('not an image'),
  });
  await submit(page);
  await expect(prompt(page)).toBeVisible();
  await expect(prompt(page)).toContainText(/image|convert|format|read|invalid/i);
  await cancel(page);
  expect(await publicPost(page, post.id)).toContain(post.content);
  await expect(page.locator('.causeway-blog-Post-picturePreview img')).toBeVisible();
  expect(await publicPost(page, post.id)).toContain('Preserved picture');
});
