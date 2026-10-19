import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  // Surface intermittent failures instead of hiding them behind successful retries.
  retries: 0,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  forbidOnly: !!process.env['CI'],
  reporter: [['list'], ['html', { open: 'never' }], ['junit', { outputFile: 'test-results/e2e.xml' }]],
  use: {
    baseURL: 'http://127.0.0.1:18080',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    launchOptions: process.env['CHROME_BIN'] ? { executablePath: process.env['CHROME_BIN'] } : {},
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    { name: 'mobile', use: { ...devices['iPhone 13'], defaultBrowserType: 'chromium' } },
  ],
  webServer: [
    {
      command: 'java -Dloader.path=../target/test-classes -Dloader.main=com.alexbalmus.acbblog.webapp.BrowserTestApplication -cp ../target/acb-blog-1.0.0-SNAPSHOT-exec.jar org.springframework.boot.loader.launch.PropertiesLauncher --server.port=18080 --spring.ai.openai.api-key=test-only --spring.datasource.url=jdbc:h2:mem:e2e --eclipselink.application-location=../target',
      url: 'http://127.0.0.1:18080/blog',
      timeout: 180_000,
      reuseExistingServer: false,
    },
    {
      command: 'node e2e/serve.mjs',
      url: 'http://127.0.0.1:14200',
      reuseExistingServer: false,
    },
  ],
});
