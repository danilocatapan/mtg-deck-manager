import { expect, test } from '@playwright/test'
import { installAuth, mockApi } from '../helpers/mockApi.mjs'

test('public routes expose home, contact, release notes and shared deck consult', async ({ page }) => {
  await mockApi(page)
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
  await mockApi(page)
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
  await page.getByRole('button', { name: /Analisar( Deck)?$/i }).last().click()
  await expect(page.getByRole('heading', { name: /An.lise/i })).toBeVisible()
  await expect(page.getByRole('tablist')).toBeVisible()
  await page.keyboard.press('ArrowRight')

  await page.getByRole('button', { name: /Recomenda/i }).last().click()
  await page.getByRole('button', { name: /Gerar trocas/i }).click()
  await expect(page.getByText('Por que esta troca e segura')).toBeVisible()
  await page.getByRole('button', { name: 'Util', exact: true }).click()
  await expect(page.getByText('Feedback registrado')).toBeVisible()

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

test('api startup state remains explicit and stable', async ({ page }) => {
  await mockApi(page, { apiFailure: 'startup' })
  await page.goto('./')

  await expect(page.getByRole('heading', { name: /Biblioteca de Decks/i })).toBeVisible()
  await expect(page.getByText(/API iniciando/i)).toBeVisible()
})

test('contact supports success and release notes empty state', async ({ page }) => {
  await page.addInitScript(() => {
    window.__MTG_CONTACT_FORM_ENDPOINT__ = 'http://127.0.0.1:8080/contact-test'
    const originalFetch = window.fetch.bind(window)
    window.fetch = (input, init) => {
      const url = String(input)
      if (init?.method === 'POST' && (url.includes('/contact-test') || url.includes('formspree'))) {
        return Promise.resolve(new Response('{"ok":true}', {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }))
      }
      return originalFetch(input, init)
    }
  })
  await mockApi(page)
  await page.route('**/contact-test**', (route) => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: '{"ok":true}',
  }))
  await page.route('**/release-notes.json', (route) => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: '[]',
  }))

  await page.goto('./#/contact')
  await page.getByLabel('Categoria').selectOption('suggestion')
  await page.getByLabel('Assunto').fill('Melhoria de UX')
  await page.getByLabel('Mensagem').fill('Fluxo validado por teste Playwright.')
  await page.getByRole('button', { name: /Enviar mensagem/i }).click()
  await expect(page.getByText(/Mensagem enviada/i)).toBeVisible()

  await page.goto('./#/release-notes')
  await expect(page.getByText('Nenhuma nota de versao', { exact: true })).toBeVisible()
})

test('meta admin handles authorization states with mocks', async ({ page }) => {
  await mockApi(page)
  await page.goto('./#/meta-admin')
  await expect(page.getByRole('heading', { name: /Acesso restrito/i })).toBeVisible()

  await installAuth(page, { email: 'dcatapan@gmail.com' })
  await page.goto('./#/meta-admin')
  await expect(page.getByRole('heading', { name: /Meta automatico/i })).toBeVisible()
  await expect(page.getByText(/Expandir corpus versionado/i)).toBeVisible()
  await page.getByRole('button', { name: /Sincronizar meta agora/i }).click()
  await expect(page.getByText(/24 decks persistidos/i)).toBeVisible()
})

test('shared deck editor routes and card popovers support keyboard dismissal', async ({ page }) => {
  await mockApi(page)
  await installAuth(page)
  await page.goto('./#/deck/1/edit')

  await expect(page.locator('main h1')).toHaveText(/Gruul Revels/i)
  await expect(page).toHaveURL(/#\/deck\/1\/edit/)

  const solRingPreview = page.getByRole('button', { name: /Ver imagem de Sol Ring/i }).first()
  await solRingPreview.focus()
  await expect(page.locator('.card-name-popover')).toBeVisible()
  await solRingPreview.press('Escape')
  await expect(page.locator('.card-name-popover')).toHaveCount(0)

  await page.goto('./#/deck/1/analysis')
  await expect(page).toHaveTitle(/Analise do Deck/)
  await expect(page.getByRole('heading', { name: /An.lise/i })).toBeVisible()

  await page.goto('./#/deck/1/recommendations')
  await expect(page).toHaveTitle(/Recomendacoes do Deck/)
  await expect(page.locator('main h2').filter({ hasText: /Recomenda/ }).first()).toBeVisible()
})
