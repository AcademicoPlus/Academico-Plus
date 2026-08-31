# Especificação de requisitos — Academico+

Levantamento de requisitos funcionais (RF) e não-funcionais (RNF) rastreados à implementação
atual do backend, para a seção de requisitos do TCC2. RFs apontam para o(s) endpoint(s) que os
implementam (fonte: `academico-backend/src/main/java/.../controller/`); RNFs apontam para o
mecanismo de implementação real, não para uma meta abstrata.

## Requisitos funcionais

| ID | Requisito | Endpoint(s) |
|---|---|---|
| RF01 | Cadastrar-se na plataforma, com aceite obrigatório dos Termos de Uso e Política de Privacidade | `POST /auth/cadastro` |
| RF02 | Autenticar-se e receber um token de acesso | `POST /auth/login` |
| RF03 | Encerrar a sessão corrente (invalidar o token) | `POST /usuarios/logout` |
| RF04 | Recuperar acesso à conta por código enviado ao e-mail (OTP) | `POST /auth/reset-senha/solicitar`, `/preview`, `/validar`, `/redefinir` |
| RF05 | Visualizar e editar o próprio perfil | `GET /usuarios/me`, `PUT /usuarios/me` |
| RF06 | Alterar a própria senha | `PATCH /usuarios/me/senha` |
| RF07 | Enviar/atualizar foto de perfil | `POST /usuarios/me/foto` |
| RF08 | Excluir a própria conta (direito do titular, LGPD) | `DELETE /usuarios/me` |
| RF09 | Explorar perfis públicos de outros usuários, com busca e filtro por curso | `GET /usuarios/explorar`, `GET /usuarios/{id}` |
| RF10 | Gerenciar as próprias habilidades (adicionar, atualizar nível, remover) | `GET/POST /usuarios/me/habilidades`, `PATCH/DELETE /usuarios/me/habilidades/{id}` |
| RF11 | Listar cursos disponíveis (usado no cadastro) | `GET /cursos` |
| RF12 | Consultar o catálogo de habilidades e suas categorias | `GET /habilidades`, `GET /habilidades/categorias`, `GET /habilidades/{id}` |
| RF13 | Administrar o catálogo de habilidades (criar, editar, remover) — restrito a ADMIN | `POST/PUT/DELETE /habilidades` |
| RF14 | Criar um projeto, já com os requisitos de habilidade desejados | `POST /projetos` |
| RF15 | Editar título, descrição, vagas e prazo de um projeto próprio | `PUT /projetos/{id}` |
| RF16 | Alterar o status de um projeto próprio | `PATCH /projetos/{id}/status` |
| RF17 | Encerrar (inativar) um projeto próprio | `DELETE /projetos/{id}` |
| RF18 | Buscar e listar projetos ativos, por status e texto livre | `GET /projetos` |
| RF19 | Listar os próprios projetos criados e os projetos em que participo | `GET /projetos/meus-projetos`, `GET /projetos/participando` |
| RF20 | Ver o detalhe completo de um projeto | `GET /projetos/{id}` |
| RF21 | Gerenciar as habilidades exigidas por um projeto próprio | `GET/POST /projetos/{id}/habilidades`, `PATCH/DELETE /projetos/{id}/habilidades/{hid}` |
| RF22 | Gerenciar os membros de um projeto (adicionar, atualizar função, remover) | `GET/POST /projetos/{id}/membros`, `PATCH/DELETE /projetos/{id}/membros/{mid}` |
| RF23 | Candidatar-se a um projeto aberto | `POST /candidaturas` |
| RF24 | Cancelar uma candidatura própria ainda pendente | `DELETE /candidaturas/{id}` |
| RF25 | Listar as próprias candidaturas enviadas | `GET /candidaturas/minhas` |
| RF26 | Listar, aceitar e rejeitar candidaturas recebidas num projeto próprio | `GET /candidaturas/projeto/{id}`, `PATCH .../aceitar`, `PATCH .../rejeitar` |
| RF27 | Avaliar um colaborador de um projeto já encerrado (concluído ou cancelado) | `POST /projetos/{id}/avaliacoes` |
| RF28 | Consultar as avaliações recebidas por um usuário | `GET /usuarios/{id}/avaliacoes` |
| RF29 | Denunciar uma avaliação recebida considerada abusiva | `POST /avaliacoes/{id}/denuncias` |
| RF30 | Analisar e resolver denúncias pendentes — restrito a ADMIN | `GET /denuncias`, `PATCH /denuncias/{id}/resolver` |
| RF31 | Ver projetos recomendados por compatibilidade com as próprias habilidades | `GET /recomendacoes/projetos` |
| RF32 | Ver candidatos recomendados para um projeto próprio | `GET /recomendacoes/projetos/{id}/candidatos` |
| RF33 | Listar usuários e alterar o nível de acesso de um usuário — restrito a ADMIN | `GET /usuarios`, `PATCH /usuarios/{id}/nivel-acesso` |
| RF34 | Consultar os níveis de acesso existentes — restrito a ADMIN | `GET /niveis-acesso` |

## Requisitos não-funcionais

| ID | Requisito | Como é atendido hoje |
|---|---|---|
| RNF01 | Segurança de autenticação e senhas | JWT stateless assinado (HMAC), senhas com hash BCrypt, blacklist de tokens revogados (`tokens_revogados`) para logout imediato |
| RNF02 | Proteção contra força bruta e flood | Rate limit por conta (5 tentativas falhas / 15 min de bloqueio, compartilhado entre login e OTP) e por IP (120 req/min) — ver [decisão 5](./decisoes-arquiteturais.md#5-rate-limiting-em-dois-níveis-estado-em-memória) |
| RNF03 | Desempenho em listagens | Paginação em toda listagem de coleção; índices de banco nas colunas usadas como filtro/FK (ver [diagrama ER](./diagramas.md#1-diagrama-entidade-relacionamento)) |
| RNF04 | Conformidade com a LGPD | Aceite obrigatório dos Termos de Uso/Política de Privacidade no cadastro (`termosAceitosEm`); exclusão lógica de conta a pedido do titular (`DELETE /usuarios/me`); direitos do titular descritos em `legal/POLITICA_DE_PRIVACIDADE.md`, cada um ligado a um endpoint concreto |
| RNF05 | Documentação da API sempre atualizada | Swagger UI / OpenAPI gerados em tempo de execução a partir do próprio código (`springdoc-openapi`), não um documento manual que desatualiza |
| RNF06 | Usabilidade (objetivo específico do TCC: "avaliar usabilidade através de testes com usuários") | **Parcialmente verificável**: o frontend já cobre autenticação, dashboard, exploração/criação/edição de projetos e candidaturas (enviar, acompanhar, cancelar, aceitar/rejeitar) — dá pra testar o fluxo principal ponta a ponta com usuários reais. Ainda faltam telas de avaliação entre colegas, recomendação de candidatos e perfil próprio (services já prontos no frontend, sem tela — ver `docs/CONECTANDO_FRONT_BACKEND.md`) |
| RNF07 | Manutenibilidade | Arquitetura em camadas (controller → service → repository), DTOs por recurso, exceções de domínio centralizadas num `@RestControllerAdvice`; suíte de testes automatizados cobrindo as regras de negócio críticas (ver [decisão 8](./decisoes-arquiteturais.md#8-estratégia-de-testes-em-camadas-unitário--slice--poucos-e2e)) |
| RNF08 | Auditabilidade da moderação | Soft-delete em avaliações removidas por denúncia procedente preserva o histórico completo (avaliação original + denúncia + resolução do admin) para eventual auditoria |

## Nota sobre rastreabilidade

Esta tabela foi gerada a partir da leitura direta dos 13 controllers e ~46 endpoints do backend
(estado do código em agosto de 2026). Ao adicionar ou remover um endpoint, atualize a linha
correspondente aqui — o objetivo é que este documento nunca fique defasado em relação ao que a
API realmente expõe, para que a banca possa verificar a rastreabilidade RF ↔ código diretamente.
