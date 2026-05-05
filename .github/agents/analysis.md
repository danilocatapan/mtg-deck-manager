# analysis.md — Análise de impacto, bug e regressão

Responsabilidade
----------------
Fornecer passos concisos e obrigatórios para diagnosticar bugs, avaliar impacto e propor a menor correção segura. Este arquivo cobre análise de impacto, regressão e diagnóstico — não existe `debug.md` separado.

Quando usar
-----------
- Antes de qualquer PR que altere comportamento observável.  
- Ao investigar falhas relatadas em produção ou testes automatizados.

Passos obrigatórios (ordem)
--------------------------
1. Reproduza localmente — identifique entrada (request) e saída atual vs esperada.  
2. Rode testes relevantes unit/integration; capture falhas.  
3. Identifique o menor cenário reproduzível.  
4. Busque causa raiz (logs, stacktrace, histórico de commits).  
5. Liste side-effects potenciais (DB, integrações, cache).  
6. Proponha a menor correção segura (hotfix ou PR limitado).  

Trilho adicional para bug e regressão
------------------------------------
- Se for produção, isole deploys recentes e aplique rollback se necessário.  
- Para regressão em testes, analise testes que dependem do contrato e execute apenas aqueles antes de abrir PR.

Checklist operacional de conformidade arquitetural
------------------------------------------------
- Verifique contratos REST expostos (OpenAPI) e testes de contrato.  
- Verifique mudanças em entidades persistidas (migrations).  
- Verifique configurações de cache e TTL afetados.  
- Confirme que integrações externas mantêm compatibilidade (Scryfall, EDHREC ingest).  

Classificação de achados (P0/P1/P2)
----------------------------------
- P0: quebra de invariantes ou perda de dados / violação de contrato.  
- P1: degradação de qualidade perceptível (recommender ruim) ou erro repetido.  
- P2: refactor, melhoria de perf, adição de testes sem impacto imediato.

Saída esperada
--------------
- Relatório curto no PR com: cenário reproduzível, testes falhando, causa raiz proposta, plano de validação e impacto estimado.
