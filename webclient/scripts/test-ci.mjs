import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { chromium } from '@playwright/test';

// Share Playwright's pinned browser with Karma, including on machines without Chrome.
const result = spawnSync(process.execPath, [
  fileURLToPath(new URL('../node_modules/@angular/cli/bin/ng.js', import.meta.url)),
  'test', '--watch=false', '--browsers=ChromeHeadlessNoSandbox', '--code-coverage',
  ...process.argv.slice(2),
], {
  stdio: 'inherit',
  env: { ...process.env, CHROME_BIN: process.env.CHROME_BIN || chromium.executablePath() },
});
if (result.error) console.error(result.error);
process.exit(result.status ?? 1);
