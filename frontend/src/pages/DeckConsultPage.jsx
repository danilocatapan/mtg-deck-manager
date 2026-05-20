import { useMemo } from 'react'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import DeckCardListView from '../components/DeckCardListView'

export default function DeckConsultPage({ deck, isAuthenticated = false, onBack, onCopy, onLike, onLoginRequired }) {
  const cards = useMemo(() => deck?.cards || [], [deck?.cards])
  const totalCards = useMemo(() => cards.reduce((sum, card) => sum + Number(card.quantity || 0), 0), [cards])
  const displayTotal = deck?.mainDeckSize ?? totalCards
  const canCopyDeck = !deck?.ownedByCurrentUser
  const sourceLabel = deck?.sourceType === 'external' && deck?.externalSource ? ` - ${deck.externalSource}` : ''

  return (
    <main>
      <section className="zone zone-command page-heading deck-editor-heading">
        <div>
          <p className="eyebrow">Consulta{sourceLabel}</p>
          <h1>{deck?.name || 'Deck'}</h1>
          <p className="page-description">
            {deck?.commander} - {displayTotal}/99 cartas - {deck?.visibility === 'public' ? 'Publico' : 'Privado'}
            {deck?.author ? ` - por ${deck.author}` : ''}
            {typeof deck?.likeCount === 'number' ? ` - ${deck.likeCount} like${deck.likeCount === 1 ? '' : 's'}` : ''}
          </p>
        </div>
        <div className="actions-row">
          {deck?.visibility === 'public' && (
            <Button onClick={() => isAuthenticated ? onLike?.(deck) : onLoginRequired?.()}>
              {deck?.likedByCurrentUser ? 'Remover like' : 'Curtir'}
            </Button>
          )}
          {canCopyDeck && (
            <Button onClick={() => isAuthenticated ? onCopy?.(deck) : onLoginRequired?.()}>
              {isAuthenticated ? 'Copiar para minha biblioteca' : 'Entrar para copiar'}
            </Button>
          )}
          <Button variant="secondary" onClick={onBack}>Voltar aos Decks</Button>
        </div>
      </section>

      <Card className="zone zone-library">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Somente leitura</p>
            <h2>Lista do deck</h2>
            <p>Este modo permite consultar a lista sem editar, excluir ou aplicar recomendacoes. Copias entram como privadas na sua biblioteca.</p>
          </div>
        </div>

        <DeckCardListView
          cards={cards}
          title="Lista do deck"
          description="Use os filtros para localizar cartas e agrupe por tipo para revisar a construcao."
          emptyMessage="Este deck nao possui cartas cadastradas."
        />
      </Card>
    </main>
  )
}
