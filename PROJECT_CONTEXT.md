# Projeto

- **Nome:** MTG Deck Manager
- **Descrição:** Aplicação para gerenciamento, análise e recomendação de decks de Magic: The Gathering. Permite buscar cartas via Scryfall, criar/editar/deletar decks, exportar listas em texto, realizar análise determinística do deck e gerar recomendações heurísticas.

# Stack

- **Backend:** Java 25 + Quarkus
- **Frontend:** React (Vite)
- **API externa:** Scryfall (REST)
- **Banco de dados de desenvolvimento/testes:** H2 (in-memory)

# Arquitetura

- **Backend:** REST API organizada em camadas (controller → service → domain/model → repository/client). Uso de MicroProfile Rest Client para Scryfall, Hibernate ORM / Panache para persistência, e SmallRye OpenAPI para documentação.
- **Frontend:** SPA React estruturada em `pages`, `components` e `services` (API client). Estado local com `useState` para casos simples; navegação feita com estado local (sem router nesta fase).
- **Integração:** Comunicação via HTTP JSON (endpoints REST), CORS habilitado para desenvolvimento.

# Funcionalidades implementadas

- Busca de cartas (integração com Scryfall) — endpoint: `GET /cards?name=`.
- CRUD completo de decks (persistência H2 / Panache) — endpoints: `GET /decks`, `POST /decks`, `GET /decks/{id}`, `PUT /decks/{id}`, `DELETE /decks/{id}`.
- Exportação de deck em texto (formato "quantity name") — `GET /decks/{id}/export`.
- Engine de análise determinística: média de CMC, curva de mana, contagem de ramp/draw/removal — `GET /decks/{id}/analysis`.
- Engine de recomendações heurísticas: gera `add` e `cut` e justifica por gaps e score — `POST /decks/{id}/recommendations`.

# Endpoints principais

- `GET /cards?name=` — pesquisa de cartas na API Scryfall (proxied)
- `GET /decks` — lista decks
- `POST /decks` — cria um deck
- `GET /decks/{id}` — obtém deck por id
- `PUT /decks/{id}` — atualiza deck
- `DELETE /decks/{id}` — remove deck
- `GET /decks/{id}/export` — exporta deck como `text/plain` (lista)
- `GET /decks/{id}/analysis` — retorna métricas e curva de mana
- `POST /decks/{id}/recommendations` — retorna recomendações com base em parâmetros

# Padrões adotados

- Clean Code e pequenas funções coesas.
- Separação de responsabilidades (controllers finos, services com lógica de negócio, DTOs e mappers quando necessários).
- Logs estruturados (categoria `com.mtg` no backend) para rastreabilidade.
- Documentação OpenAPI/Swagger (SmallRye).
- Testes unitários e de integração automatizados (JUnit + Quarkus Test).
- Evitar dependência de modelos generativos; regras e heurísticas determinísticas para análise/recommendação.

# Fases concluídas

1. Integração Scryfall (MicroProfile REST client)
2. CRUD de Deck (Panache + H2)
3. Exportação (texto)
4. Engine de Análise (curva de mana, métricas)
5. Engine de Recomendação (heurísticas + scoring)
6. Frontend: scaffold React + consumo dos endpoints (listar, criar/editar, buscar cartas, análise, recomendações)

# Próximos passos (curto/médio prazo)

- Refinamento de UI/UX (estética, feedbacks, navegação clara).
- Melhorias de performance e caching seletivo (Scryfall + consultas pesadas).
- Adição de validação e mappers robustos no backend (bean validation).
- Segurança: hardening, validação de inputs, rate limiting, autenticação/autorizações quando necessário.
- CI/CD: configurar pipelines (GitHub Actions) para build, testes e deploy.
- Avaliar integração futura com IA (por exemplo, LangChain4j) para recomendações avançadas — apenas como opção, não substitui heurísticas determinísticas.

# Regras e boas práticas de desenvolvimento

- Sempre adicionar logs para operações significativas e erros.
- Sempre criar testes ao introduzir lógica de negócio.
- Manter métodos pequenos e coesos; preferir composição a duplicação.
- Validar entradas no backend (bean validation) e aplicar respostas HTTP adequadas.
- Não expor chaves/segredos em código; usar variáveis de ambiente/configuration.

# Observações operacionais

- Ambiente de desenvolvimento local: Quarkus dev (`mvnw quarkus:dev`) e Vite (`npm run dev`).
- Arquivos relevantes:
  - `backend/src/main/java/...` — controllers, services, clients, domain
  - `backend/src/main/resources/application.properties` — configurações (porta, CORS, Scryfall URL)
  - `frontend/src/pages`, `frontend/src/components`, `frontend/src/services/api.js` — UI e integração

# Contato/Onboarding rápido

- Para executar localmente:
  1. Defina `JAVA_HOME` apropriado e rode `./mvnw quarkus:dev` em `backend`.
  2. No diretório `frontend` rode `npm install` (se necessário) e `npm run dev`.
  3. Abra `http://localhost:5173` (frontend) e confirme comunicação com backend `http://localhost:8080`.

Este arquivo deve ser mantido conciso e atualizado sempre que houver mudanças significativas em arquitetura, stack ou etapas do roadmap.
