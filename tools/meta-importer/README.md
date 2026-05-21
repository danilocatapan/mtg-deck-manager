# Meta Importer (offline)

Versao docs: 2026-05-21
Ultima atualizacao: 2026-05-21

Scripts offline para coletar/normalizar dados de meta EDH e gerar arquivos em `backend/src/main/resources/meta/commanders`.

Use esta pasta para preparacao offline de dados. Em runtime, a API deve preferir dataset local, cache/adapters existentes e endpoints administrativos controlados.

## Uso

```bash
node import-edhrec.mjs --commander "Xenagos, God of Revels" --slug xenagos-god-of-revels
```

## Regras

- Nunca usar scraping em runtime da API.
- Nao versionar chaves, cookies, tokens ou dumps sensiveis.
- Validar o JSON gerado antes de copiar para `backend/src/main/resources/meta/commanders`.
- Quando o dado passar a afetar recomendacoes, atualizar testes representativos de meta/recomendacao.
