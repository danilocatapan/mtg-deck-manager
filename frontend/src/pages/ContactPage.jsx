import { useMemo, useState } from 'react'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import StateMessage from '../components/ui/StateMessage'
import { ContactConfigError, sendContactMessage } from '../services/contactApi'

const INITIAL_FORM = {
  category: '',
  name: '',
  email: '',
  wantsReply: false,
  subject: '',
  message: '',
  relatedDeck: '',
  honeypot: '',
}

const CATEGORY_OPTIONS = [
  ['suggestion', 'Sugestao'],
  ['bug', 'Bug'],
  ['deck-recommendation', 'Recomendacao de deck'],
  ['login-account', 'Login/conta'],
  ['privacy', 'Privacidade'],
  ['other', 'Outro'],
]

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const SUBJECT_MAX_LENGTH = 120
const MESSAGE_MAX_LENGTH = 3000

export default function ContactPage({ onBack }) {
  const [form, setForm] = useState(INITIAL_FORM)
  const [errors, setErrors] = useState({})
  const [status, setStatus] = useState('idle')
  const [notice, setNotice] = useState(null)

  const remainingMessageChars = useMemo(
    () => MESSAGE_MAX_LENGTH - form.message.length,
    [form.message.length],
  )

  function updateField(field, value) {
    setForm((current) => ({ ...current, [field]: value }))
    setErrors((current) => ({ ...current, [field]: null }))
    setNotice(null)
  }

  async function handleSubmit(event) {
    event.preventDefault()
    const nextForm = normalizeForm(form)
    const nextErrors = validate(nextForm)

    setForm(nextForm)
    setErrors(nextErrors)

    if (Object.keys(nextErrors).length > 0) {
      setStatus('idle')
      return
    }

    try {
      setStatus('submitting')
      await sendContactMessage(nextForm)
      setForm(INITIAL_FORM)
      setErrors({})
      setNotice({ tone: 'success', title: 'Mensagem enviada', message: 'Obrigado pelo contato. Sua mensagem foi enviada aos mantenedores do projeto.' })
      setStatus('sent')
    } catch (error) {
      setStatus('error')
      if (error instanceof ContactConfigError) {
        setNotice({
          tone: 'error',
          title: 'Contato ainda nao configurado',
          message: 'O canal de contato ainda nao recebeu o endpoint do Formspree. Use o link do GitHub na Politica de Privacidade enquanto isso.',
        })
        return
      }
      setNotice({
        tone: 'error',
        title: 'Nao foi possivel enviar',
        message: 'Tente novamente em alguns instantes. Se o problema continuar, use o link do GitHub na Politica de Privacidade.',
      })
    }
  }

  const submitting = status === 'submitting'

  return (
    <main>
      <section className="zone zone-command page-heading">
        <div>
          <p className="eyebrow">Contato</p>
          <h1>Fale com os mantenedores</h1>
          <p className="page-description">Envie sugestoes, bugs e duvidas sobre o MTG Deck Manager sem precisar entrar com Google.</p>
        </div>
        <div className="actions-row">
          <Button variant="secondary" onClick={onBack}>Voltar para biblioteca</Button>
        </div>
      </section>

      {notice && (
        <StateMessage tone={notice.tone} title={notice.title}>
          {notice.message}
        </StateMessage>
      )}

      <Card className="zone zone-library contact-card">
        <form className="contact-form" onSubmit={handleSubmit} noValidate>
          <div className="contact-grid">
            <label className="field-label">
              <span className="field-label-text">Categoria</span>
              <select
                name="category"
                value={form.category}
                onChange={(event) => updateField('category', event.target.value)}
                aria-invalid={Boolean(errors.category)}
                aria-describedby={errors.category ? 'contact-category-error' : undefined}
                required
              >
                <option value="">Selecione uma categoria</option>
                {CATEGORY_OPTIONS.map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
              {errors.category && <span id="contact-category-error" className="field-error">{errors.category}</span>}
            </label>

            <label className="field-label">
              <span className="field-label-text">Nome</span>
              <input
                name="name"
                value={form.name}
                onChange={(event) => updateField('name', event.target.value)}
                placeholder="Como podemos te chamar?"
                autoComplete="name"
              />
            </label>

            <label className="field-label">
              <span className="field-label-text">E-mail</span>
              <input
                name="email"
                type="email"
                value={form.email}
                onChange={(event) => updateField('email', event.target.value)}
                placeholder="voce@example.com"
                autoComplete="email"
                aria-invalid={Boolean(errors.email)}
                aria-describedby={errors.email ? 'contact-email-error' : undefined}
              />
              {errors.email && <span id="contact-email-error" className="field-error">{errors.email}</span>}
            </label>

            <label className="contact-checkbox">
              <input
                name="wantsReply"
                type="checkbox"
                checked={form.wantsReply}
                onChange={(event) => updateField('wantsReply', event.target.checked)}
              />
              <span>Quero receber resposta</span>
            </label>
          </div>

          <label className="field-label">
            <span className="field-label-text">Assunto</span>
            <input
              name="subject"
              value={form.subject}
              onChange={(event) => updateField('subject', event.target.value)}
              maxLength={SUBJECT_MAX_LENGTH}
              placeholder="Resumo rapido do contato"
              aria-invalid={Boolean(errors.subject)}
              aria-describedby={errors.subject ? 'contact-subject-error' : undefined}
              required
            />
            {errors.subject && <span id="contact-subject-error" className="field-error">{errors.subject}</span>}
          </label>

          <label className="field-label">
            <span className="field-label-text">Mensagem</span>
            <textarea
              name="message"
              value={form.message}
              onChange={(event) => updateField('message', event.target.value)}
              maxLength={MESSAGE_MAX_LENGTH}
              placeholder="Conte o que aconteceu, qual sugestao voce tem ou que parte do fluxo precisa de atencao."
              aria-invalid={Boolean(errors.message)}
              aria-describedby={`contact-message-help${errors.message ? ' contact-message-error' : ''}`}
              required
            />
            <small id="contact-message-help">{remainingMessageChars} caracteres restantes</small>
            {errors.message && <span id="contact-message-error" className="field-error">{errors.message}</span>}
          </label>

          <label className="field-label">
            <span className="field-label-text">Deck, comandante ou URL publica relacionada</span>
            <input
              name="relatedDeck"
              value={form.relatedDeck}
              onChange={(event) => updateField('relatedDeck', event.target.value)}
              placeholder="Ex.: Atraxa, deck publico, fluxo de recomendacao..."
            />
          </label>

          <label className="contact-honeypot" aria-hidden="true">
            <span>Deixe este campo em branco</span>
            <input
              name="_gotcha"
              tabIndex="-1"
              autoComplete="off"
              value={form.honeypot}
              onChange={(event) => updateField('honeypot', event.target.value)}
            />
          </label>

          <div className="contact-privacy-note">
            <strong>Privacidade</strong>
            <p>Nao envie senhas, tokens, exports privados completos ou dados sensiveis. Use este canal apenas para mensagens de suporte do projeto.</p>
            <a href={`${import.meta.env.BASE_URL}privacy-policy.html`}>Ver Politica de Privacidade</a>
          </div>

          <div className="form-actions contact-actions">
            <Button type="submit" loading={submitting}>Enviar mensagem</Button>
            <Button type="button" variant="secondary" onClick={onBack} disabled={submitting}>Voltar para biblioteca</Button>
          </div>
        </form>
      </Card>
    </main>
  )
}

function normalizeForm(form) {
  return {
    category: form.category.trim(),
    name: form.name.trim(),
    email: form.email.trim(),
    wantsReply: form.wantsReply,
    subject: form.subject.trim(),
    message: form.message.trim(),
    relatedDeck: form.relatedDeck.trim(),
    honeypot: form.honeypot.trim(),
  }
}

function validate(form) {
  const errors = {}

  if (!form.category) {
    errors.category = 'Escolha uma categoria.'
  }

  if (form.wantsReply && !form.email) {
    errors.email = 'Informe um e-mail para receber resposta.'
  } else if (form.email && !EMAIL_PATTERN.test(form.email)) {
    errors.email = 'Informe um e-mail valido.'
  }

  if (!form.subject) {
    errors.subject = 'Informe um assunto.'
  }

  if (!form.message) {
    errors.message = 'Escreva a mensagem.'
  }

  return errors
}
