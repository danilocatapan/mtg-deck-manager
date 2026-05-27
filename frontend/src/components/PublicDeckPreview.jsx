export default function PublicDeckPreview({ name, commander, total = 0 }) {
  return (
    <section className="public-deck-preview" aria-label="Previa publica do deck">
      <div className="public-deck-preview-card">
        <div>
          <p className="eyebrow">Previa publica</p>
          <h2>{name?.trim() || 'Nome do deck'}</h2>
          <p>{commander?.trim() || 'Comandante ainda nao informado'}</p>
        </div>
        <div className="public-deck-preview-meta" aria-label="Resumo da vitrine">
          <span>{Number(total || 0)}/99 cartas</span>
          <span>Publico</span>
        </div>
      </div>
      <ul className="public-deck-preview-list">
        <li>Aparece na Vitrine e pode ser aberto por link compartilhavel.</li>
        <li>Pode receber likes e ser copiado por outros usuarios autenticados.</li>
        <li>E-mail, dono tecnico, historico e dados privados nao sao exibidos.</li>
      </ul>
    </section>
  )
}
