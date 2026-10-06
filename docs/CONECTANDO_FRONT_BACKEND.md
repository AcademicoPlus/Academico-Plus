# Como conectar uma tela do frontend ao backend

Guia para o grupo: como ligar uma tela React (frontend/) a um endpoint do
Spring Boot (academico-api/). Usa como exemplo real a tela de cadastro,
que já está conectada — leia o código junto com este guia.

## 1. As 3 camadas do frontend

Toda comunicação com o backend passa por essas três camadas, nessa ordem.
Nunca pule uma camada (por exemplo, nunca chame `fetch` direto dentro de uma
página).

```
página (.tsx)  →  service (services/*.ts)  →  apiClient (services/apiClient.ts)  →  backend
```

- **`apiClient.ts`** — só existe UM. Centraliza a URL base, o header
  `Authorization`, e transforma erro HTTP em `ApiError`. Não mexe nisso pra
  conectar uma tela nova.
- **`services/*Service.ts`** — um arquivo por recurso do backend (ex.:
  `cursoService.ts` fala com `CursoController`, `authService.ts` fala com
  `AuthController`). É aqui que você cria uma função nova quando for
  conectar uma tela.
- **página (`pages/*.tsx`)** — chama a função do service dentro de um
  `useEffect` (para carregar dados) ou de um handler de evento (para enviar
  dados), e guarda o resultado em `useState`.

## 2. Pré-requisitos antes de conectar qualquer tela

1. **Backend rodando** em `http://localhost:8080` (`academico-api`).
2. **`frontend/.env`** existe e aponta pra ele — copie de `.env.example` se
   não tiver:
   ```
   VITE_API_URL=http://localhost:8080/api
   ```
   Se você criar ou editar o `.env`, **reinicie o `npm run dev`** — o Vite só
   lê esse arquivo quando o servidor sobe, não recarrega sozinho.
3. **CORS liberado no backend** para a porta que o Vite está usando (padrão
   `5173`). Isso é configurado no `application.properties`/`.env` do backend
   (`CORS_ALLOWED_ORIGINS`), não no frontend. Se o Vite subir em outra porta
   (5174, 5175...) porque a 5173 já estava ocupada, adicione essa porta
   também.

## 3. Exemplo real: como a tela de Cadastro busca a lista de cursos

**Backend** — `CursoController` expõe `GET /cursos`, público (sem exigir
login), retornando uma lista de `CursoResponse`.

**Passo 1 — criar o service** ([cursoService.ts](../frontend/src/services/cursoService.ts)):
```ts
import { apiFetch } from './apiClient';

// O tipo espelha o DTO Java (CursoResponse) campo a campo.
export type Curso = {
  id: string;
  nome: string;
  criadoEm: string;
};

export function listarCursos(): Promise<Curso[]> {
  // autenticado: false porque a rota é pública (permitAll no SecurityConfig)
  return apiFetch<Curso[]>('/cursos', { autenticado: false });
}
```

**Passo 2 — usar na página** ([cadastro.tsx](../frontend/src/pages/cadastro.tsx)):
```tsx
const [cursos, setCursos] = useState<Curso[]>([]);
const [carregandoCursos, setCarregandoCursos] = useState(true);
const [erro, setErro] = useState<string | null>(null);

useEffect(() => {
  listarCursos()
    .then(setCursos)
    .catch(() => setErro('Não foi possível carregar a lista de cursos.'))
    .finally(() => setCarregandoCursos(false));
}, []);
```

Esse par (service + `useEffect`) é o padrão para **qualquer tela que precisa
carregar dados do backend ao abrir**. Para enviar dados (POST/PUT), o padrão
é o mesmo, mas a chamada fica dentro do `handleSubmit`/handler de clique em
vez de um `useEffect` — veja `cadastrar()` em
[authService.ts](../frontend/src/services/authService.ts) e o `handleSubmit`
em `cadastro.tsx`.

## 4. Checklist para conectar uma tela nova

- [ ] Descobrir o endpoint no backend (olhar o `*Controller.java` — rota,
      método HTTP, se exige autenticação, e o DTO de request/response).
- [ ] Criar (ou reaproveitar) um `services/xxxService.ts` com:
  - um `type`/`export type` espelhando o DTO Java.
  - uma função que chama `apiFetch<TipoDeResposta>('/rota', opções)`.
- [ ] Na página, chamar essa função:
  - dado que carrega ao abrir a tela → dentro de `useEffect(() => { ... }, [])`.
  - dado enviado por ação do usuário (form, botão) → dentro do handler.
- [ ] Guardar o resultado em `useState` e usar esse estado no JSX (nunca
      deixar dado mockado/hardcoded ao lado do dado real).
- [ ] Tratar erro com `try/catch` (ou `.catch()`), verificando
      `erroCapturado instanceof ApiError` para mostrar `erroCapturado.message`
      (mensagem que já vem pronta do backend) — veja o `catch` do
      `handleSubmit` em `cadastro.tsx`.
- [ ] Se a rota exige login, deixar `autenticado: true` (padrão do
      `apiFetch`, pode omitir a opção) — o token salvo em `localStorage` é
      anexado automaticamente. Se for rota pública (login, cadastro, listar
      cursos), passar `autenticado: false` explicitamente.

## 5. Erros comuns

- **"Não foi possível conectar ao servidor" / erro de rede no console
  (`Failed to fetch`)** — geralmente é CORS (porta do frontend não está em
  `CORS_ALLOWED_ORIGINS` do backend) ou o backend não está rodando. Abra o
  DevTools → aba Network e veja se a requisição nem saiu (CORS bloqueado) ou
  voltou com erro.
- **`.env` alterado mas nada muda** — reinicie `npm run dev` (Vite só lê
  `.env` na subida).
- **Duas chamadas para o mesmo endpoint no Network** — comportamento normal
  do `<StrictMode>` do React em desenvolvimento (ele monta/desmonta cada
  componente de propósito pra pegar efeito colateral com bug). Some sozinho
  no build de produção; não precisa "corrigir".
- **Dropdown/campo pedindo um valor mas o backend rejeita** — confira se o
  `type` do service bate exatamente com o DTO Java atual (nome do campo,
  obrigatório ou não). DTOs mudam com frequência; se der erro de validação
  (`erros: [{ campo, mensagem }]`), o `ApiError.erros` traz o campo exato que
  falhou.

## 6. Testando sem o backend rodando

Nem sempre dá pra ter o `academico-api` + banco de dados de pé (ex.:
sem Docker, ou testando só uma tela de UI). Pra isso existe um modo de
mocks: `apiFetch` (a única função que fala com o backend, ver seção 1) pode
responder com dados fictícios em vez de fazer a chamada HTTP real.

**Como ativar:**
```
# frontend/.env
VITE_USE_MOCKS=true
```
Reinicie o `npm run dev` depois de mudar (Vite só lê `.env` na subida — mesmo
aviso da seção 2).

**Login** — use as credenciais fixas do mock:
- E-mail: `ana.silva@academico.edu.br`
- Senha: `senha123`

Qualquer outra combinação simula "Email ou senha incorretos." (401), do
jeito que a tela já trata.

**Cadastro** — qualquer e-mail funciona e "cadastra com sucesso", exceto
`duplicado@academico.edu.br`, que simula erro de e-mail já cadastrado (400).

**Cursos** — o dropdown do cadastro é preenchido com uma lista fixa de
cursos de exemplo.

**Como funciona por baixo:** `services/mocks.ts` tem uma função `mockFetch`
com um `switch` por rota (`"MÉTODO /caminho"`). `apiClient.ts` chama essa
função no lugar do `fetch` real quando `VITE_USE_MOCKS=true` — nenhuma
página ou service precisa saber a diferença.

**Conectando uma tela nova sem esperar o backend:** siga o checklist da
seção 4 normalmente (service + página), e adicione um `case` novo em
`services/mocks.ts` com dados de exemplo no formato do DTO. Uma rota sem
mock cadastrado devolve erro 501 explicando isso, em vez de falhar
silenciosamente.

Lembre de deixar `VITE_USE_MOCKS=false` (o padrão) quando for testar contra
o backend de verdade — é fácil esquecer ligado e passar horas "debugando" um
bug que não existe no backend real.

## 7. Mapa dos services disponíveis

Todo `*Controller.java` do backend já tem um `services/*Service.ts`
correspondente no frontend, pronto pra importar — a "camada 2" da seção 1 já
está feita para toda a API. O que falta, na maioria dos casos, é só a
**tela** (camada 3): pegar a função do service e ligar num componente.

| Service (frontend) | Controller (backend) | Cobre |
|---|---|---|
| `cursoService.ts` | `CursoController` | Listar cursos (usado no cadastro) |
| `authService.ts` | `AuthController` | Login, cadastro, reset de senha (4 passos) |
| `usuarioService.ts` | `UsuarioController` | Meu perfil, logout, editar perfil/senha/foto, excluir conta, admin (listar/explorar/promover) |
| `usuarioHabilidadeService.ts` | `UsuarioHabilidadeController` | Habilidades (tags) do meu perfil |
| `habilidadeService.ts` | `HabilidadeController` | Catálogo de habilidades (leitura geral, escrita admin) |
| `nivelAcessoService.ts` | `NivelAcessoController` | Níveis de acesso (admin) |
| `projetoService.ts` | `ProjetoController` | Listar/criar/editar/cancelar projetos (usado no feed e nos detalhes) |
| `projetoHabilidadeService.ts` | `ProjetoHabilidadeController` | Habilidades exigidas de um projeto |
| `projetoMembroService.ts` | `ProjetoMembroController` | Equipe de um projeto |
| `candidaturaService.ts` | `CandidaturaController` | Candidatar-se, aceitar, rejeitar, cancelar |
| `avaliacaoService.ts` | `ProjetoAvaliacaoController` + parte do `UsuarioController` | Avaliar participante, ver avaliações recebidas |
| `denunciaService.ts` | `DenunciaController` | Denunciar avaliação abusiva, fila de revisão (admin) |
| `recomendacaoService.ts` | `RecomendacaoController` | Projetos/candidatos recomendados por habilidade |

Cada arquivo tem, no topo, um comentário dizendo qual `Controller.java`
ele espelha — é o primeiro lugar a olhar quando for usar um.

## 8. Exercício pra praticar

O frontend hoje já cobre autenticação (`login.tsx`, `cadastro.tsx`), dashboard
(`dashboard.tsx`), projetos (`projetos.tsx`, `detalhes-projeto.tsx`,
`criar-projeto.tsx`, `editar-projeto.tsx`), candidaturas (`candidaturas.tsx`,
mais o fluxo de candidatar-se/aceitar/rejeitar dentro de
`detalhes-projeto.tsx`) e um painel de administração (`admin.tsx`,
`admin-usuarios.tsx`, `admin-habilidades.tsx`, `admin-denuncias.tsx`). O que
ainda falta tela, com o service já pronto pra usar — bons exercícios pra
fixar o padrão da seção 3, do mais simples ao mais completo:

1. **Meu perfil** — tela que chama `usuarioService.obterMeuPerfil()` e
   permite editar (`atualizarMeuPerfil`), trocar senha
   (`alterarMinhaSenha`), enviar foto (`enviarFotoDePerfil`) e gerenciar as
   próprias habilidades (`usuarioHabilidadeService.ts`). O menu lateral já
   linka pra `/perfil`, mas a rota não existe em `App.tsx` — é o próximo
   passo natural.
2. **Encontrar Pessoas** (`/pessoas`) — hoje é só um texto "em construção"
   direto no `App.tsx`. Trocar por uma tela de busca usando
   `usuarioService.explorarPerfis()` (busca por nome + filtro por curso, já
   paginado).
3. **Avaliar colaborador** — formulário de nota (1 a 5) + comentário usando
   `avaliacaoService.avaliarParticipante(projetoId, avaliadoId, nota,
   comentario)`, liberado depois que o projeto está `CONCLUIDO` ou
   `CANCELADO`; e uma seção/tela pra `avaliacaoService.listarAvaliacoesRecebidas(usuarioId)`.
   Nenhuma das duas tem UI ainda.
4. **Candidatos recomendados** — em `detalhes-projeto.tsx`, pro criador do
   projeto, usar `recomendacaoService.recomendarCandidatos(projetoId)` pra
   sugerir quem se encaixa nas habilidades exigidas. Hoje só a metade
   "projetos recomendados" (`recomendarProjetos`) tem tela, no dashboard.
