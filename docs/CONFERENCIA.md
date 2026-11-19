# Conferência — Global Solution / Java Advanced

Base: PDF 2TDS Agosto — Global Solution 2026/2, pp. 51–59; tema Serviços e direitos / 08 Participação cidadã; feedback da entrega AMAZUL aplicado ao novo domínio.

| Rubrica (pp. 53–54) | Onde está atendida |
| --- | --- |
| API e modelagem (20) | Quatro entidades JPA relacionadas; CRUD de temas e de rascunhos de consulta com DTOs, filtro, paginação, ordenação, Location e códigos de sucesso adequados. |
| Consultas (15) | Método derivado para temas normalizados; JPQL de agregação de votos; Specification para filtros combináveis de consulta, incluindo visibilidade. |
| Validação e erros (10) | Bean Validation no corpo, nos filtros e na carga demo; RestControllerAdvice; erros de autenticação e papel também estruturados. |
| Cache (10) | Detalhe de tema e resultado encerrado; invalidação nas escritas pertinentes, vinculada a transações. |
| HATEOAS (10) | Respostas HAL com self, tema ou consultas relacionadas e ações por papel/estado; links de paginação. |
| Actuator (5) | health e info públicos; metrics disponível ao gestor. Readiness informa quando a carga inicial terminou. |
| Swagger e README (10) | Documentação executável da API, segurança e respostas de erro; README com integrantes, execução, contas demo, diagrama e justificativas. O link do pitch ainda precisa ser incluído. |
| Segurança (20) | Cadastro de cidadão, BCrypt, login JWT assinado com expiração, restrições de papéis e CORS configurável. |

As pontuações acima são pesos da rubrica, não notas atribuídas.

| Feedback da AMAZUL | Aplicação nesta entrega |
| --- | --- |
| Tema como texto livre | Entidade Tema com chave única normalizada. O vínculo por FK substitui texto livre. |
| Respostas não registradas | Voto registra escolha, data e vínculos; unicidade por conta e consulta; resultado agregado sem identidade. Para este domínio, não há gabarito de acerto/erro. |
| Busca sem paginação e sem tema | Paginação e ordenação no banco; busca por título, tema, bairro e estado. |
| H2 em arquivo com ddl-auto=update | H2 em memória com create-drop e carga inicial. O esquema e os dados são recriados ao reiniciar a API. |
| Único commit e nomes abreviados | Histórico separado por mudanças reais e nomes completos no README. Publique o histórico, não só o ZIP extraído. |

## Verificações

JDK 17: `mvn clean verify` com nove testes de integração no H2 para cadastro e hash, login, 401/403, CRUD, normalização, cache e invalidação, voto único, apuração JPQL, filtros/paginação, validações, CORS, links HAL, Actuator e OpenAPI. A configuração Docker Compose utiliza a API com H2 embutido. O contrato HTTP usado pelo mobile permanece o mesmo.

## O que depende da equipe

1. Publicar o repositório público com o projeto na raiz e preservar a evolução dos commits.
2. Gravar um pitch de até cinco minutos e acrescentar o link ao README. O PDF também indica enviar o link na área de trabalhos de Java Advanced destinada ao vídeo.
3. Entregar na FIAP o TXT com somente a URL pública do repositório (p. 56). Prazo geral do PDF: **19/11/2026, às 23h55**.
4. Integrar o Mobile e demonstrar o app consumindo a API na apresentação de **23 a 27/11**, na aula da disciplina (p. 57). O contrato de integração está no README; o aplicativo Flutter integrado é entregue em projeto próprio.

Não houve publicação ou submissão.
