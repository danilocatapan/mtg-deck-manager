import Button from './ui/Button'
import createIcon from '../assets/icons/create.png'
import importIcon from '../assets/icons/import.png'

export default function DeckList({ decks = [], onEdit, onDelete, onCreate, onImport }) {
  if (!decks || decks.length === 0) {
    return (
      <div className="empty-state empty-state-hero">
        <p className="eyebrow">Biblioteca vazia</p>
        <h3>Nenhum deck ainda</h3>
        <p>Crie um deck Commander do zero ou importe uma lista de texto quando ja tiver as 99 cartas separadas.</p>
        <div className="actions-row">
          <Button className="cta-primary" onClick={onCreate}>
            <img className="btn-icon" src={createIcon} alt="" aria-hidden="true" />
            Criar Deck
          </Button>
          <Button variant="secondary" onClick={onImport}>
            <img className="btn-icon" src={importIcon} alt="" aria-hidden="true" />
            Importar Deck
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="deck-list">
      {decks.map((deck) => {
        const totalCards = deck.cards
          ?.filter((card) => (card.zone || 'main') === 'main')
          .reduce((sum, card) => sum + Number(card.quantity || 0), 0) ?? 0

        return (
          <div key={deck.id} className="deck-card">
            <div>
              <div className="deck-title">{deck.name}</div>
              <div className="deck-subtitle">{deck.commander}</div>
              <div className={`deck-count ${totalCards > 99 ? 'is-invalid' : ''}`}>{totalCards}/99 cartas</div>
            </div>
            <div className="actions-row">
              <Button variant="secondary" onClick={() => onEdit && onEdit(deck)}>Editar</Button>
              <Button variant="danger" onClick={() => onDelete && onDelete(deck)}>Excluir</Button>
            </div>
          </div>
        )
      })}
    </div>
  )
}
