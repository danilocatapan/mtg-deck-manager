import { expect, test } from '@playwright/test'
import { installAuth, mockApi } from '../helpers/mockApi.mjs'

test.beforeEach(async ({ page }) => {
  await mockApi(page)
})

test('public routes expose home, contact, release notes and shared deck consult', async ({ page }) => {
  await page.goto('./')

  await expect(page.locator('main')).toHaveCount(1)
  await expect(page.getByRole('heading', { name: /Biblioteca de Decks/i })).toBeVisible()
  await expect(page.getByText('Public Gruul Revels')).toBeVisible()

  await page.goto('./#/contact')
  await expect(page).toHaveTitle(/Contato/)
  await expect(page.getByRole('heading', { name: /Fale com os mantenedores/i })).toBeFocused()

  await page.goto('./#/release-notes')
  await expect(page).toHaveTitle(/Novidades/)
  await expect(page.locator('main h1')).toBeFocused()

  await page.goto('./#/public/101')
  await expect(page).toHaveTitle(/Consultar Deck Publico/)
  await expect(page.getByRole('heading', { name: /Public Gruul Revels|Consultar Deck/i })).toBeVisible()
  await expect(page.getByText(/Xenagos, God of Revels/i).first()).toBeVisible()
})

test('authenticated import, analysis, recommendation, undo and delete stay keyboard-accessible', async ({ page }) => {
  await installAuth(page)
  await page.goto('./#/import')

  await expect(page.getByRole('heading', { name: /Importar Deck/i })).toBeVisible()
  await page.getByLabel('Nome do deck').fill('Deck Publico de Teste')
  await page.getByLabel('Comandante').fill('Xenagos, God of Revels')
  await page.getByLabel('Visibilidade').selectOption('public')
  await expect(page.getByText('Previa publica')).toBeVisible()
  await page.getByLabel('Colar lista do deck').fill('1 Sol Ring\n1 Arcane Signet\n12 Mountain\n12 Forest')
  await page.getByRole('button', { name: /Importar Deck/i }).click()

  await expect(page.locator('main h1')).toHaveText(/Deck Publico de Teste/i)
  await page.getByRole('button', { name: /Analisar Deck/i }).click()
  await expect(page.getByRole('heading', { name: /An.lise/i })).toBeVisible()
  await expect(page.getByRole('tablist')).toBeVisible()
  await page.keyboard.press('ArrowRight')

  await page.getByRole('button', { name: /Abrir recomenda/i }).click()
  await page.getByRole('button', { name: /Gerar trocas/i }).click()
  await expect(page.getByText('Por que esta troca e segura')).toBeVisible()

  await page.getByRole('button', { name: /Aplicar troca/i }).first().click()
  await expect(page.getByRole('dialog', { name: /Aplicar troca/i })).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog', { name: /Aplicar troca/i })).toHaveCount(0)
  await page.getByRole('button', { name: /Aplicar troca/i }).first().click()
  await page.getByRole('dialog', { name: /Aplicar troca/i }).getByRole('button', { name: /Aplicar troca/i }).click()
  await expect(page.getByRole('button', { name: /Desfazer/i })).toBeVisible()
  await page.getByRole('button', { name: /Desfazer/i }).click()
  await expect(page.getByText(/Troca desfeita/i)).toBeVisible()

  await page.getByRole('button', { name: /Voltar aos Decks/i }).click()
  await expect(page.getByRole('heading', { name: /Biblioteca de Decks/i })).toBeVisible()
  await page.getByRole('button', { name: /Excluir/i }).first().click()
  await expect(page.getByRole('dialog', { name: /Excluir deck/i })).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog', { name: /Excluir deck/i })).toHaveCount(0)
})
