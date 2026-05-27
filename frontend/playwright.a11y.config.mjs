import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests/a11y',
  timeout: 60_000,
  expect: { timeout: 8_000 },
  fullyParallel: false,
  workers: 2,
  reporter: [['line']],
  use: {
    baseURL: 'http://127.0.0.1:5173/mtg-deck-manager/',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium-desktop',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 1000 } },
    },
    {
      name: 'chromium-mobile',
      use: { ...devices['Pixel 5'], viewport: { width: 390, height: 844 } },
    },
  ],
  webServer: {
    command: 'npm run dev -- --host 127.0.0.1 --port 5173',
    url: 'http://127.0.0.1:5173/mtg-deck-manager/',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
    env: {
      ...process.env,
      VITE_GOOGLE_CLIENT_ID: 'test-client',
      VITE_CONTACT_FORM_ENDPOINT: 'http://127.0.0.1:8080/contact-test',
    },
  },
})
