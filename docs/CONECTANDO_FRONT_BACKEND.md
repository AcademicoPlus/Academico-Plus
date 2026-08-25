# Como conectar uma tela do frontend ao backend

Guia para o grupo: como ligar uma tela React (frontend/) a um endpoint do
Spring Boot (academico-backend/). Usa como exemplo real a tela de cadastro,
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

1. **Backend rodando** em `http://localhost:8080` (`academico-backend`).
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

## 6. Mapa dos services disponíveis

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

## 7. Exercício pra praticar

Hoje só 4 telas têm UI: `login.tsx`, `cadastro.tsx`, `feedprojetos.tsx` e
`projetodetalhes.tsx`. Todo o resto da tabela acima já tem o service pronto,
só falta a tela. Bons exercícios pra fixar o padrão da seção 3, do mais
simples ao mais completo:

1. **Meu perfil** — tela que chama `usuarioService.obterMeuPerfil()` e mostra
   os dados (bom primeiro exercício, só leitura).
2. **Minhas candidaturas** — `candidaturaService.listarMinhasCandidaturas()`,
   com botão de cancelar (`cancelarCandidatura`).
3. **Botão "Candidatar-se"** dentro de `projetodetalhes.tsx` — chama
   `candidaturaService.candidatar(projeto.id)` (mistura leitura + escrita
   numa tela que já existe).
4. **Criar projeto** — formulário usando `projetoService.criarProjeto()`,
   parecido com o form de `cadastro.tsx`.
