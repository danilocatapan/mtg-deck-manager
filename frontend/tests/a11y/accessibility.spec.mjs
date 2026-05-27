import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import { installAuth, mockApi } from '../helpers/mockApi.mjs'

test.beforeEach(async ({ page }) => {
  await mockApi(page)
})

test('public pages pass axe and landmark checks', async ({ page }) => {
  for (const hash of ['', '#/contact', '#/release-notes', '#/public/101']) {
    await page.goto(`./${hash}`)
    await expect(page.locator('main')).toHaveCount(1)

    const results = await new AxeBuilder({ page })
      .exclude('#credential_picker_container')
      .analyze()

    expect(results.violations, formatViolations(results.violations)).toEqual([])
  }
})

test('authenticated import preview and editor pass axe checks', async ({ page }) => {
  await installAuth(page)
  await page.goto('./#/import')
  await page.getByLabel('Nome do deck').fill('Deck Publico de Teste')
  await page.getByLabel('Comandante').fill('Xenagos, God of Revels')
  await page.getByLabel('Visibilidade').selectOption('public')
  await page.getByLabel('Colar lista do deck').fill('1 Sol Ring\n1 Arcane Signet\n12 Mountain\n12 Forest')
  await expect(page.getByText('Previa publica')).toBeVisible()

  let results = await new AxeBuilder({ page }).analyze()
  expect(results.violations, formatViolations(results.violations)).toEqual([])

  await page.getByRole('button', { name: /Importar Deck/i }).click()
  await expect(page.locator('main h1')).toHaveText(/Deck Publico de Teste/i)
  await page.getByRole('button', { name: /Analisar( Deck)?$/i }).last().click()
  await page.getByRole('button', { name: /Recomenda/i }).last().click()
  await page.getByRole('button', { name: /Gerar trocas/i }).click()
  await expect(page.getByText('Por que esta troca e segura')).toBeVisible()

  results = await new AxeBuilder({ page }).analyze()
  expect(results.violations, formatViolations(results.violations)).toEqual([])
})

function formatViolations(violations) {
  return violations
    .map((violation) => {
      const targets = violation.nodes.map((node) => node.target.join(' ')).join(', ')
      return `${violation.id} (${violation.impact}): ${violation.help} -> ${targets}`
    })
    .join('\n')
}
