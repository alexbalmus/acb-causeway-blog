import { expect, Locator, Page } from '@playwright/test';
import { randomUUID } from 'node:crypto';

export const unique = (prefix: string) => `${prefix}-${randomUUID().slice(0, 10)}`;
export const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=', 'base64');

export async function login(page: Page, username = 'wicket-writer', password = 'pass') {
  await page.goto('/wicket/');
  await page.locator('input[name="username"]').fill(username);
  await page.locator('input[type="password"]').fill(password);
  await expect(page.getByLabel('Timezone', { exact: true })).not.toHaveValue('');
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.locator('input[type="password"]')).toHaveCount(0);
  await page.waitForLoadState('load');
}

export async function home(page: Page) {
  await page.goto('/wicket/');
  await expect(page.locator('.dt-container')).toBeVisible();
}

export async function ready(page: Page) {
  await page.waitForFunction(() => {
    const wicket = (window as unknown as { Wicket?: { channelManager?: { channels: Record<string, { busy: boolean }> } } }).Wicket;
    return document.readyState === 'complete' && !!wicket?.channelManager
      && Object.values(wicket.channelManager.channels).every(channel => !channel.busy);
  }, undefined, { timeout: 10_000 });
}

// Causeway M2 renders scalar labels without a `for` attribute. Scope inputs to
// their labelled scalar component instead of relying on generated Wicket IDs.
export function field(scope: Locator, label: string) {
  return scope.locator('.scalarNameAndValueComponentType').filter({
    has: scope.page().locator('label.scalarName').filter({ hasText: new RegExp(`^${label}$`) }),
  }).locator('input.scalarValue, textarea.scalarValue, select');
}

export function prompt(page: Page) {
  return page.locator('form.actionParametersForm').filter({ visible: true });
}

export async function action(page: Page, name: string) {
  await ready(page);
  // Match the action label while allowing Causeway's trailing prompt ellipsis.
  await page.getByRole('link', { name: new RegExp(`^${name}(?:[^a-zA-Z].*)?$`) })
    .filter({ visible: true }).click();
}

export async function navigation(page: Page) {
  await ready(page);
  const bar = page.locator('nav.navbar').first();
  const toggle = bar.getByRole('button', { name: 'Toggle navigation', exact: true });
  if (await toggle.isVisible() && await toggle.getAttribute('aria-expanded') !== 'true') await toggle.click();
  // Restricted users may have an empty primary menu; the account menu remains available.
  await expect(bar.locator('.app-actions').last()).toBeVisible();
  return bar;
}

export async function menu(page: Page, group: string, name: string) {
  const bar = await navigation(page);
  const groupLink = bar.getByRole('link', { name: group, exact: true });
  // The viewer opens desktop dropdowns on hover. Clicking would close them again.
  if ((page.viewportSize()?.width ?? 1280) >= 768) await groupLink.hover();
  else await groupLink.click();
  await bar.getByRole('link', { name: new RegExp(`^${name}(?:[^a-zA-Z].*)?$`) }).click();
}

export async function submit(page: Page) {
  await prompt(page).getByRole('button', { name: 'OK', exact: true }).click();
  await ready(page);
}

export async function choose(page: Page, value: string) {
  await prompt(page).getByRole('combobox').filter({ visible: true }).click();
  const search = page.locator('.select2-container--open .select2-search__field');
  if (await search.isVisible()) await search.fill(value);
  await page.getByRole('option', { name: value, exact: true }).click();
}

export async function object(page: Page) {
  const header = page.locator('h3 a.objectUrlSource');
  await expect(header).toBeVisible();
  const href = await header.evaluate(e => (e as HTMLAnchorElement).href);
  const match = /blog\.(Blog|Post):([0-9]+)/.exec(href);
  expect(match, 'Expected a persistent blog or post object').not.toBeNull();
  return { url: href, id: match![2], type: match![1] };
}

export async function createBlog(page: Page, name = unique('Wicket'), initialHandle = 'wicket-writer') {
  await home(page);
  await action(page, 'New Blog');
  await expect(page.getByRole('dialog')).toBeVisible();
  await field(prompt(page), 'Name').fill(name);
  const handle = field(prompt(page), 'Handle');
  if (await handle.isEditable()) await handle.fill(initialHandle);
  await submit(page);
  await expect(prompt(page)).toHaveCount(0);
  await expect(page.locator('h3 a.objectUrlSource')).toContainText(name);
  return { ...await object(page), name };
}

export async function createPost(page: Page, name = unique('Article'), content = 'Written through Wicket') {
  await action(page, 'New Post');
  await expect(page.getByRole('dialog')).toBeVisible();
  await field(prompt(page), 'Name').fill(name);
  await field(prompt(page), 'Content').fill(content);
  await submit(page);
  await expect(prompt(page)).toHaveCount(0);
  await expect(page.locator('h3 a.objectUrlSource')).toHaveText(name);
  return { ...await object(page), name, content };
}

export async function propertyEditor(page: Page, property: string) {
  await ready(page);
  await page.locator(`.causeway-blog-Post-${property} .scalarValueInlinePromptLink`).click();
  await expect(page.getByRole('button', { name: 'OK', exact: true })).toBeVisible();
}
