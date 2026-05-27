const CONTACT_FORM_ENDPOINT = import.meta.env.VITE_CONTACT_FORM_ENDPOINT || ''

function contactFormEndpoint() {
  return window.__MTG_CONTACT_FORM_ENDPOINT__ || CONTACT_FORM_ENDPOINT
}

export class ContactConfigError extends Error {
  constructor(message = 'Canal de contato indisponivel.') {
    super(message)
    this.name = 'ContactConfigError'
    this.code = 'CONTACT_ENDPOINT_MISSING'
  }
}

export async function sendContactMessage(message) {
  const endpoint = contactFormEndpoint()
  if (!endpoint) {
    throw new ContactConfigError()
  }

  const formData = new FormData()
  formData.append('category', message.category)
  formData.append('name', message.name)
  formData.append('email', message.email)
  formData.append('wantsReply', message.wantsReply ? 'yes' : 'no')
  formData.append('subject', message.subject)
  formData.append('message', message.message)
  formData.append('relatedDeck', message.relatedDeck)
  formData.append('_gotcha', message.honeypot)

  const response = await fetch(endpoint, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
    },
    body: formData,
  })

  if (!response.ok) {
    throw new Error('Nao foi possivel enviar sua mensagem agora.')
  }

  return true
}
