# EvComp — API Reference

Referência completa da API REST do EvComp. Gerada a partir do código-fonte (controllers, DTOs,
entidades e regras de serviço) em `src/main/java/br/unesp/fct/evcomp/`.

> Convenção de nomenclatura: todo o código é em português, incluindo mensagens de erro. Os
> exemplos abaixo reproduzem literalmente as mensagens que a API devolve.

---

## Visão geral

### Base URL

```
http://localhost:8080/api
```

No ambiente Docker do frontend, o Next.js faz *rewrite* de `/api/:path*` para dentro da rede
interna — o cliente sempre fala com `/api/...`, nunca com a porta 8080 diretamente.

### Autenticação

A API usa **JWT stateless**. O token pode ser enviado de duas formas, aceitas de forma
equivalente pelo filtro (`JwtAuthenticationFilter`):

- Header `Authorization: Bearer <token>` (prioridade), **ou**
- Cookie `auth_token`.

Exceção importante: **`GET /api/auth/me` só aceita o header `Authorization`** — ele lê o header
diretamente na assinatura do método (`@RequestHeader`), sem passar pelo filtro que também aceita
cookie. Uma sessão autenticada só via cookie recebe `401` nesse endpoint específico, mesmo
conseguindo acessar normalmente qualquer outra rota autenticada.

O token carrega `sub` (id do usuário), `email`, `nome`, `role` e `isColetor`, e expira em
`jwt.expiration-ms` (padrão 24h). Não há blacklist/revogação: **`POST /api/auth/logout` não
invalida o token no servidor** — é stateless por design, então um token roubado continua válido
até expirar mesmo após o "logout".

Rotas públicas (sem token): `/api/auth/**`, `/api/cadastro/**`, `/api/redefinicao-senha/**`.
Todo o resto exige autenticação; algumas exigem `ADMIN` (ver seção de cada recurso).

### Formato de erro padrão

A maioria dos endpoints segue o padrão `Map.of("error", "...")` / `Map.of("message", "...")`:

```json
{ "error": "mensagem descritiva em português" }
```

**Validação de `@Valid @RequestBody`** (Bean Validation) é interceptada globalmente
(`GlobalExceptionHandler`) e vira `400` com todos os campos inválidos concatenados:

```json
{ "error": "email: O e-mail informado é inválido.; senha: A senha é obrigatória." }
```

**Upload maior que o limite** (`spring.servlet.multipart.max-file-size=1MB`) também é
interceptado globalmente e vira `413`:

```json
{ "error": "O arquivo enviado excede o tamanho máximo de 1 MB." }
```

⚠️ **Nem todo erro de request segue esse formato.** Um corpo JSON que falha na
*desserialização* — antes de o controller rodar — não passa pelo `GlobalExceptionHandler`.
Isso acontece, por exemplo, ao mandar um valor fora do enum em
`PATCH /api/pagamentos/{id}/status` (`novoStatus` só aceita `ISENTO`/`PENDENTE`/`APROVADO`/
`RECUSADO` como literais válidos; qualquer outra string quebra a desserialização). Nesses casos
a resposta é o `/error` padrão do Spring Boot, não `{"error": "..."}`:

```json
{ "timestamp": "2026-08-28T10:00:00.000+00:00", "status": 400, "error": "Bad Request", "path": "/api/pagamentos/5/status" }
```

**Try/catch amplo**: quase todo controller envolve a lógica em `try { ... } catch (Exception e) {
500 }`, então uma exceção inesperada (NPE, parse de data inválida, etc.) normalmente vira `500`
com uma mensagem genérica — não `400` — mesmo quando a causa raiz é um dado de entrada malformado
não coberto por Bean Validation (ex.: `dataInicio` em formato inválido em `POST /api/eventos`).

### Formato de datas/horas (serialização Jackson)

| Tipo Java | Formato JSON | Exemplo |
|---|---|---|
| `LocalDate` | `"YYYY-MM-DD"` | `"2027-08-10"` |
| `LocalTime` | `"HH:mm:ss"` | `"08:00:00"` |
| `LocalDateTime` | ISO-8601 sem timezone, com nanos variáveis | `"2026-08-27T22:58:52.3895021"` |

Todos os `LocalDate`/`LocalDateTime` são interpretados no timezone da JVM (`America/Sao_Paulo`,
fixado no container). Datas de entrada (`dataInicio`, `dataFim`, etc.) são Strings no DTO,
parseadas com `LocalDate.parse(...)` — exigem `YYYY-MM-DD` exato; qualquer outro formato lança
`DateTimeParseException`, capturada pelo try/catch amplo do controller e devolvida como `500`.

### Papéis (roles)

Derivados de `Usuário.getRole()` pelo **tipo da instância**, não por um campo separado:
`ADMIN` (Administrador), `COLETOR` (ColetorDePresenca — subtipo de Participante) ou
`PARTICIPANTE`. Um `Participante` só vira "coletor" de fato quando está associado a pelo menos um
evento (`coletor_presença`); o campo `isColetor` do login/`.../me` é `false` para um usuário do
tipo `ColetorDePresenca` que ainda não foi vinculado a nenhum evento.

---

## Índice

1. [Autenticação — `/api/auth`](#autenticação--apiauth)
2. [Cadastro — `/api/cadastro`](#cadastro--apicadastro)
3. [Redefinição de senha — `/api/redefinicao-senha`](#redefinição-de-senha--apiredefinicao-senha)
4. [Eventos — `/api/eventos`](#eventos--apieventos)
5. [Modalidades de inscrição — `/api/modalidades`, `/api/eventos/{id}/modalidades`](#modalidades-de-inscrição)
6. [Atividades — `/api/atividades`](#atividades--apiatividades)
7. [Inscrições — `/api/inscricoes`](#inscrições--apiinscricoes)
8. [Presenças — `/api/presencas`](#presenças--apipresencas)
9. [Certificados — `/api/certificados`](#certificados--apicertificados)
10. [Relatórios — `/api/relatorios`](#relatórios--apirelatorios)
11. [Participantes — `/api/participantes`](#participantes--apiparticipantes)
12. [Pagamentos — `/api/pagamentos`](#pagamentos--apipagamentos)

---

## Autenticação — `/api/auth`

Rotas públicas.

### `POST /api/auth`

Login. Body:

```json
{ "email": "admin@unesp.br", "senha": "SenhaForte123" }
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `email` | string | sim | `@NotBlank`, `@Email` |
| `senha` | string | sim | `@NotBlank` |

**200 OK**

```json
{
  "message": "Login bem-sucedido",
  "nome": "Administrador do Sistema",
  "role": "ADMIN",
  "isColetor": "false",
  "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

> `isColetor` é enviado como **string** (`"true"`/`"false"`), não boolean — reflexo direto de
> `String.valueOf(isColetor)` no controller.

**400 Bad Request** — validação do body:

```json
{ "error": "email: O e-mail informado é inválido.; senha: A senha é obrigatória." }
```

**401 Unauthorized** — e-mail inexistente ou senha incorreta (mesma mensagem para os dois casos,
de propósito, para não vazar quais e-mails existem):

```json
{ "error": "Credenciais Inválidas" }
```

### `POST /api/auth/logout`

Sem body. Sempre `200`, mesmo sem token válido — é só um sinalizador para o cliente descartar o
token, não há efeito no servidor (ver nota em [Autenticação](#autenticação)).

**200 OK**

```json
{ "message": "Logout realizado com sucesso. Credenciais de acesso da sessão invalidadas." }
```

### `GET /api/auth/me`

Requer header `Authorization: Bearer <token>` (cookie **não** funciona aqui — ver nota acima).

**200 OK** — todos os campos são extraídos do JWT, sem consulta ao banco (podem estar
desatualizados se o usuário mudou de nome/role desde que o token foi emitido):

```json
{
  "id": "1",
  "nome": "Administrador do Sistema",
  "email": "admin@unesp.br",
  "role": "ADMIN",
  "isColetor": "false"
}
```

> `id` também vem como **string** (`claims.getSubject()`), diferente de outros endpoints que
> devolvem IDs como número.

**401 Unauthorized** — header ausente/mal formado:

```json
{ "error": "Token não fornecido ou inválido." }
```

**401 Unauthorized** — token presente mas inválido/expirado:

```json
{ "error": "Token inválido ou expirado." }
```

---

## Cadastro — `/api/cadastro`

Rota pública.

### `POST /api/cadastro`

```json
{ "nome": "Bruno Unesp", "email": "bruno@unesp.br", "senha": "Senha123", "ra": "221234567" }
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `nome` | string | sim | `@NotBlank`, máx. 255 |
| `email` | string | sim | `@NotBlank`, `@Email`, máx. 255 |
| `senha` | string | sim | `@NotBlank`, 6–100 caracteres |
| `ra` | string | não | máx. 9 caracteres |

⚠️ **Nota de regra de negócio**: apesar de o restante da documentação do projeto mencionar "RA
obrigatório para e-mails `@unesp.br`", **esse endpoint não valida isso no backend** — `ra` é
sempre opcional aqui, independentemente do domínio do e-mail. Se essa regra existe, é aplicada
apenas no frontend.

**200 OK**

```json
{ "message": "Cadastro realizado com sucesso" }
```

**400 Bad Request** — e-mail já cadastrado (checagem explícita, roda antes do insert):

```json
{ "error": "Erro: Este e-mail já está em uso." }
```

**400 Bad Request** — validação do body:

```json
{ "error": "senha: A senha deve ter entre 6 e 100 caracteres." }
```

**400 Bad Request** — condição de corrida: e-mail/RA únicos violados no insert (passou pela
checagem acima mas outro cadastro concorrente venceu a corrida):

```json
{ "error": "Erro de integridade. Este e-mail ou RA já pode estar em uso." }
```

**500 Internal Server Error**

```json
{ "error": "Ocorreu um erro interno no servidor. Tente novamente mais tarde." }
```

---

## Redefinição de senha — `/api/redefinicao-senha`

Rota pública. Os tokens de redefinição **vivem em memória** (`Map` estático em
`TokenRedefinicao`), não no banco — reiniciar a aplicação invalida todos os tokens pendentes.

### `POST /api/redefinicao-senha/solicitar`

Query param: `email` (string, obrigatório).

Gera um token numérico de 6 dígitos, válido por 1 hora, e envia por e-mail (via Resend).
**Sempre responde `200` com a mesma mensagem, exista ou não o e-mail** — evita enumeração de
contas cadastradas:

**200 OK**

```json
{ "message": "Instruções enviadas com sucesso." }
```

> Solicitar novamente para o mesmo usuário invalida silenciosamente qualquer token anterior
> (`TokenRedefinicao.invalidarTokensDoUsuario`) — só o último token emitido funciona.

### `POST /api/redefinicao-senha/validar`

Query param: `tokenRecebido` (int, obrigatório).

Verifica se o token existe, não expirou e não foi usado — **sem** consumi-lo. Note que esse
endpoint só confirma o formato/validade; a "3ª tentativa falha invalida o token" é um efeito
colateral do método `validarToken`, chamado por este endpoint e também pelo `/confirmar` abaixo —
ou seja, **tentativas de validação aqui contam para o limite de 3 tentativas** que bloqueia o
token antes mesmo de tentar trocar a senha.

**200 OK** (corpo é um boolean cru, não um objeto):

```json
true
```

ou

```json
false
```

Nunca retorna 4xx/5xx — token inexistente, expirado, ou incorreto resultam todos em `200` com
`false`.

### `POST /api/redefinicao-senha/confirmar`

Query params: `tokenRecebido` (int, obrigatório), `novaSenha` (string, obrigatório).

Regra de senha aplicada (`RedefinicaoSenhaService.validarSenha`, **não** é Bean Validation, então
não aparece no formato `{"error": ...}`): mínimo 8 caracteres, pelo menos 1 maiúscula e pelo
menos 1 número.

**200 OK** — senha trocada com sucesso:

```json
true
```

**400 Bad Request** — senha nova não atende aos critérios (corpo também é boolean cru, **não**
`{"error": ...}` como o resto da API):

```json
false
```

**200 OK** — token inválido/expirado/já usado (também `false`, com status 200 — não há como
distinguir "senha fraca" de "token inválido" pela resposta HTTP sozinha quando o token já falhou
antes da checagem de senha):

```json
false
```

---

## Eventos — `/api/eventos`

| Rota | Auth |
|---|---|
| `GET /api/eventos`, `/disponiveis`, `/disponiveis/{id}`, `/{id}/detalhes`, `/buscar`, `/coletor` | autenticado |
| `GET /api/eventos/{id}/participantes` | `ADMIN` |
| `POST`, `PUT`, `DELETE /api/eventos/**` | `ADMIN` |

### `GET /api/eventos`

Lista **todos** os eventos, sem paginação nem filtro. Retorna entidades `Evento` cruas (seguro —
`Evento` não tem mais nenhum campo sensível nem relação exposta por getter).

**200 OK**

```json
[
  {
    "id": 1,
    "titulo": "Semana da Computação 2027",
    "dataInicio": "2027-08-10",
    "dataFim": "2027-08-15",
    "descricao": "Evento Futuro para testes de Inscrição e Gestão.",
    "link": "http://secomp2027.com.br",
    "tipoContabilizacao": "POR_ATIVIDADE",
    "chavePix": "secomp@fct.unesp.br",
    "dataInicioInscricao": "2026-07-28",
    "dataFimInscricao": "2026-09-26"
  }
]
```

### `GET /api/eventos/disponiveis`

Filtra eventos com `dataFim >= hoje` **e** que tenham pelo menos uma atividade cadastrada.

⚠️ **Não considera o período de inscrição** (`dataInicioInscricao`/`dataFimInscricao`) — um
evento pode aparecer aqui como "disponível" e mesmo assim o `POST /api/inscricoes` recusar com
`400` por a janela de inscrição ainda não ter aberto ou já ter fechado. Um evento sem nenhuma
atividade cadastrada **nunca** aparece, mesmo dentro do período de inscrição.

**200 OK** — mesmo shape de `GET /api/eventos`, filtrado.

### `GET /api/eventos/disponiveis/{participanteId}`

Igual ao anterior, **e também** exclui eventos em que o participante já tem uma inscrição com
`status = true` (ativa/aprovada).

⚠️ Uma inscrição com `status = false` (pagamento pendente/recusado, ou cancelada) **não** exclui
o evento da lista — o participante continua vendo o evento como "disponível" e pode reenviar
`POST /api/inscricoes`, que reativa a mesma inscrição em vez de criar uma nova (ver
[Inscrições](#inscrições--apiinscricoes)).

**200 OK** — mesmo shape.

### `GET /api/eventos/buscar?tituloEvento=`

Busca por título parcial, case-insensitive (`LIKE %termo%`). Query param obrigatório:
`tituloEvento` (string).

**200 OK** — lista de `Evento` (mesmo shape).

**404 Not Found** — nenhum resultado:

```json
{ "error": "Nenhum evento encontrado com este título." }
```

### `GET /api/eventos/{eventoId}/detalhes`

**200 OK**

```json
{
  "dadosEvento": {
    "id": 1,
    "titulo": "Semana da Computação 2027",
    "dataInicio": "2027-08-10",
    "dataFim": "2027-08-15",
    "descricao": "Evento Futuro para testes de Inscrição e Gestão.",
    "link": "http://secomp2027.com.br",
    "tipoContabilizacao": "POR_ATIVIDADE",
    "chavePix": "secomp@fct.unesp.br",
    "dataInicioInscricao": "2026-07-28",
    "dataFimInscricao": "2026-09-26"
  },
  "atividades": [ /* AtividadeResponseDTO — ver seção Atividades */ ],
  "modalidades": [
    { "id": 1, "eventoId": 1, "nome": "Padrão", "descricao": null, "valor": 40.00, "ativo": true }
  ]
}
```

> `modalidades` traz **apenas as ativas** (`ativo = true`). Para ver todas (inclusive
> inativas), use `GET /api/eventos/{eventoId}/modalidades` (seção seguinte).
> `dadosEvento` **não tem** `valorInscricao` — o preço agora vive inteiramente em
> `ModalidadeInscricao` (ver nota de migração ao final da seção de Modalidades).

**404 Not Found**

```json
{ "error": "Evento não encontrado." }
```

### `GET /api/eventos/{id}/participantes` (`ADMIN`)

Lista os participantes com inscrição **ativa** (`status = true`) no evento, via
`ParticipanteResponseDTO` (sem `senha`/`RA`/`secretSeed`).

**200 OK**

```json
[
  { "id": 2, "nomeCompleto": "João da Silva", "email": "joao@unesp.br", "ra": null, "role": "PARTICIPANTE", "eventosColetados": null }
]
```

> `ra` aparece sempre como `null` no JSON — o getter `getRA()` é `@JsonIgnore`; o DTO só expõe
> `ra` via seu próprio campo/getter (`getRa()`, sem `@JsonIgnore`), mas o `fromEntity` popula esse
> campo com o valor real, então **na prática `ra` só aparece `null` se o participante de fato não
> tiver RA cadastrado** — não é um bug, é o comportamento correto do DTO.

### `GET /api/eventos/coletor`

Lista os eventos em que o usuário logado é coletor **e que estão ocorrendo atualmente**
(`dataInicio <= hoje <= dataFim`, com `null` tratado como "sem limite").

**200 OK** — lista de `Evento` (mesmo shape de `GET /api/eventos`).

**401 Unauthorized** — sem `usuarioLogadoId` na request (token ausente/inválido):

```json
{ "error": "Usuário não autenticado." }
```

**403 Forbidden** — usuário autenticado mas não é `ColetorDePresenca`:

```json
{ "error": "Usuário não é um Coletor" }
```

### `POST /api/eventos/{eventoId}/coletores/{participanteId}` (`ADMIN`)

Promove o participante a coletor **daquele evento** (não altera se ele já é coletor de outros
eventos). Sem body.

**200 OK**

```json
{ "message": "Coletor associado com sucesso." }
```

**400 Bad Request** — já é coletor deste evento:

```json
{ "error": "Participante já é coletor deste evento." }
```

**500 Internal Server Error**

```json
{ "error": "Ocorreu um erro interno no servidor ao associar o coletor." }
```

### `DELETE /api/eventos/{eventoId}/coletores/{coletorId}` (`ADMIN`)

⚠️ **Efeito colateral não óbvio**: se esse era o **último** evento do coletor, o participante é
automaticamente **rebaixado a `PARTICIPANTE`** (`tipo_usuario` volta para `PAR` no banco). Se
ainda for coletor de outros eventos, só o vínculo com este evento é removido e o tipo continua
`COL`.

**200 OK**

```json
{ "message": "Coletor removido com sucesso." }
```

**400 Bad Request** — não era coletor deste evento:

```json
{ "error": "Participante não era coletor deste evento ou a exclusão falhou." }
```

### `POST /api/eventos` (`ADMIN`)

```json
{
  "titulo": "Semana da Computação 2027",
  "descricao": "...",
  "link": "http://...",
  "tipoContabilizacao": "POR_ATIVIDADE",
  "dataInicio": "2027-08-10",
  "dataTermino": "2027-08-15",
  "chavePix": "secomp@fct.unesp.br",
  "dataInicioInscricao": "2026-07-28",
  "dataFimInscricao": "2026-09-26"
}
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `titulo` | string | sim | `@NotBlank`, máx. 255, único no banco |
| `descricao` | string | sim | `@NotBlank`, máx. 5000 |
| `link` | string | não | máx. 500 |
| `tipoContabilizacao` | string | sim | `"POR_ATIVIDADE"` ou `"POR_CARGA_TOTAL"` |
| `dataInicio`, `dataTermino` | string (`YYYY-MM-DD`) | sim | — |
| `chavePix` | string | não | máx. 255 |
| `dataInicioInscricao`, `dataFimInscricao` | string (`YYYY-MM-DD`) | sim | — |

⚠️ Não existe validação cruzada entre `dataInicio`/`dataTermino` nem entre `dataInicioInscricao`/
`dataFimInscricao` — a API aceita `dataTermino` anterior a `dataInicio` sem erro.

O evento nasce **sem nenhuma modalidade de inscrição**. Se o evento for pago, é preciso criar ao
menos uma modalidade via `POST /api/eventos/{id}/modalidades` antes que qualquer inscrição seja
possível (`POST /api/inscricoes` recusa com `400` enquanto não houver modalidade).

**200 OK**

```json
{ "message": "Evento criado com sucesso." }
```

**400 Bad Request** — título duplicado:

```json
{ "error": "Já existe um evento cadastrado com este título." }
```

**400 Bad Request** — validação do body:

```json
{ "error": "titulo: O título é obrigatório.; tipoContabilizacao: O tipo de contabilização é obrigatório." }
```

**500 Internal Server Error** — falha ao persistir (exceção engolida dentro do próprio
repositório, que devolve `false` em vez de propagar):

```json
{ "error": "Não foi possível criar o evento." }
```

**500 Internal Server Error** — qualquer outra exceção (`tipoContabilizacao` fora do enum, data
em formato inválido, etc. — caem todas no catch genérico do controller):

```json
{ "error": "Ocorreu um erro interno no servidor ao cadastrar o evento." }
```

### `PUT /api/eventos/{id}` (`ADMIN`)

Mesmo body de `POST`. Todos os campos são sobrescritos (não é um PATCH parcial).

**200 OK**

```json
{ "message": "Evento editado com sucesso." }
```

**400 Bad Request** — novo título já em uso por **outro** evento:

```json
{ "error": "Já existe um evento cadastrado com este título." }
```

**500 Internal Server Error** — falha ao persistir (mesmo padrão do `POST`, repositório engole a
exceção e devolve `false`):

```json
{ "error": "Erro ao tentar salvar o evento no banco." }
```

**500 Internal Server Error** — inclui o caso de `id` inexistente: o controller chama
`eventoEncontrado.get()` sem checar `isPresent()` antes, então um `id` inválido lança
`NoSuchElementException`, capturada pelo catch genérico e devolvida como `500`, **não `404`**:

```json
{ "error": "Ocorreu um erro interno no servidor ao editar o evento." }
```

---

## Modalidades de inscrição

Endpoints aninhados sob `/api/eventos/**` (regras de `SecurityConfig` já cobrem: `GET`
autenticado, `POST`/`PUT`/`DELETE` só `ADMIN`) mais um endpoint plano `/api/modalidades`.

### Contexto (migração do preço único para modalidades)

Até uma versão anterior, o preço da inscrição era um único campo `Evento.valorInscricao`, lido
"ao vivo" em todo lugar — **isso foi removido**. Hoje:

- Preço, nome e descrição vivem em `ModalidadeInscricao`, N para 1 com `Evento`.
- Todo evento pago precisa de **ao menos uma modalidade ativa**; um evento com uma única
  modalidade de valor `0` equivale ao antigo "evento gratuito".
- `Inscrição` guarda **o valor congelado no momento da inscrição** (`valorAplicado`) e a
  modalidade escolhida (`idModalidade`). **Alterar o preço de uma modalidade via `PUT` não
  afeta inscrições já existentes** — `valorAplicado` só é gravado na criação/reativação da
  inscrição e nunca é recalculado depois. Isso corrige uma falha do modelo antigo, em que mudar o
  preço do evento alterava retroativamente o valor devido de inscrições já feitas.
- `Evento.chavePix` continua em `Evento` — é uma propriedade de como o evento recebe PIX, não da
  modalidade.

### `GET /api/modalidades`

Todas as modalidades de **todos** os eventos (ativas e inativas), com `evento` carregado via
`JOIN FETCH`. Autenticado, sem exigir `ADMIN` (espelha `GET /api/atividades`).

**200 OK**

```json
[
  { "id": 1, "eventoId": 1, "nome": "Padrão", "descricao": null, "valor": 40.00, "ativo": true },
  { "id": 4, "eventoId": 1, "nome": "Com Camiseta", "descricao": "Inclui camiseta do evento", "valor": 60.00, "ativo": true }
]
```

### `GET /api/eventos/{eventoId}/modalidades`

Modalidades **daquele evento**, ativas e inativas — usado pelo painel admin para exibir/editar
modalidades já encerradas (diferente de `GET /api/eventos/{id}/detalhes`, que só traz as ativas).

**200 OK** — mesmo shape do endpoint acima, filtrado por evento.

**404 Not Found**

```json
{ "error": "Evento não encontrado." }
```

### `POST /api/eventos/{eventoId}/modalidades` (`ADMIN`)

```json
{ "nome": "Com Camiseta", "descricao": "Inclui camiseta do evento", "valor": 50.00, "ativo": true }
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `nome` | string | sim | `@NotBlank`, máx. 100, único **dentro do evento** (case-insensitive) |
| `descricao` | string | não | máx. 500 |
| `valor` | number | sim | `@NotNull`, `>= 0`, máx. 8 dígitos inteiros + 2 decimais |
| `ativo` | boolean | não | `null` equivale a `true` no `POST` |

**200 OK**

```json
{ "id": 5, "eventoId": 1, "nome": "Com Camiseta", "descricao": "Inclui camiseta do evento", "valor": 50.00, "ativo": true }
```

**404 Not Found** — evento inexistente:

```json
{ "error": "Evento não encontrado." }
```

**400 Bad Request** — nome duplicado no mesmo evento:

```json
{ "error": "Já existe uma modalidade com este nome neste evento." }
```

**400 Bad Request** — validação do body:

```json
{ "error": "valor: O valor da modalidade é obrigatório." }
```

### `PUT /api/eventos/{eventoId}/modalidades/{modalidadeId}` (`ADMIN`)

Mesmo body do `POST`. **`ativo` nulo no PUT preserva o valor atual** (diferente do `POST`, onde
nulo vira `true`) — só é sobrescrito se vier explicitamente `true`/`false`.

**200 OK** — modalidade atualizada, mesmo shape do `POST`.

**404 Not Found**

```json
{ "error": "Modalidade não encontrada." }
```

**400 Bad Request** — novo nome já usado por outra modalidade do mesmo evento:

```json
{ "error": "Já existe uma modalidade com este nome neste evento." }
```

### `DELETE /api/eventos/{eventoId}/modalidades/{modalidadeId}` (`ADMIN`)

**200 OK**

```json
{ "message": "Modalidade excluída com sucesso." }
```

**404 Not Found**

```json
{ "error": "Modalidade não encontrada." }
```

**400 Bad Request** — existe pelo menos uma inscrição usando essa modalidade (nota: é `400`, **não
`409`**, de propósito — orienta a desativar em vez de tratar como conflito):

```json
{ "error": "Não é possível excluir uma modalidade com inscrições associadas. Desative-a em vez de excluir." }
```

---

## Atividades — `/api/atividades`

| Rota | Auth |
|---|---|
| `GET /api/atividades/**` | autenticado |
| `POST`, `PUT`, `DELETE /api/atividades/**` | `ADMIN` |

⚠️ **Atenção — três formatos diferentes de "atividade" na mesma API**, dependendo do endpoint:

1. `GET /api/atividades`, `GET /api/atividades/ativas-coletor` e `GET /api/eventos/{id}/detalhes`
   devolvem **`AtividadeResponseDTO`** — `evento` aninhado como objeto completo, `ministrantes`
   como `[{id, nomeCompleto}]`, chaves camelCase (`preRequisitos`, `horarioInicio`...).
2. `GET /api/atividades/{id}` e `GET /api/atividades/{id}/selecionar` devolvem o **mapa cru** de
   `Atividade.pegarDadosAtividade()` — `evento_id` como número solto (não objeto), `ministrantes_ids`
   como lista de IDs (não objetos), chaves em snake_case (`pre_requisitos`).
3. `POST /api/atividades` devolve a **entidade JPA crua** (`Atividade`) — `evento` aninhado
   completo, `ministrantes` como lista de `Usuário` **crus** (não `ParticipanteResumoDTO`; sem
   vazar senha/RA/secretSeed, que são `@JsonIgnore`, mas incluindo `role` e, se o ministrante for
   coletor, `eventosColetados` aninhado).

Um cliente que espera o mesmo shape em todos os endpoints de atividade vai quebrar.

### `GET /api/atividades`

**200 OK**

```json
[
  {
    "id": 1,
    "titulo": "Palestra de Abertura",
    "descricao": "...",
    "preRequisitos": null,
    "dataInicio": "2027-08-10",
    "horarioInicio": "08:00:00",
    "dataFim": "2027-08-10",
    "horarioFim": "12:00:00",
    "maxParticipantes": 50,
    "cargaHorariaTotal": 4,
    "cargaHorariaMinistrante": 4,
    "evento": { "id": 1, "titulo": "Semana da Computação 2027", "...": "..." },
    "ministrantes": [ { "id": 4, "nomeCompleto": "Carlos Professor" } ]
  }
]
```

### `GET /api/atividades/ativas-coletor`

Atividades de eventos em que o usuário logado é coletor **e o evento ainda não terminou**
(`dataFim >= hoje`) — **não** filtra pela data/hora da própria atividade, só do evento. Uma
atividade já encerrada de um evento ainda em andamento continua aparecendo aqui.

**200 OK** — lista de `AtividadeResponseDTO`.

**401 Unauthorized**

```json
{ "error": "Não autenticado." }
```

**403 Forbidden**

```json
{ "error": "Usuário não é um coletor." }
```

### `GET /api/atividades/{id}`

**200 OK** (mapa cru — formato 2 acima):

```json
{
  "id": 1,
  "titulo": "Palestra de Abertura",
  "descricao": "...",
  "preRequisitos": null,
  "dataInicio": "2027-08-10",
  "horarioInicio": "08:00:00",
  "dataFim": "2027-08-10",
  "horarioFim": "12:00:00",
  "maxParticipantes": 50,
  "cargaHorariaTotal": 4,
  "cargaHorariaMinistrante": 4,
  "ministrantes_ids": [4],
  "evento_id": 1
}
```

**404 Not Found**

```json
{ "error": "Atividade não encontrada" }
```

### `GET /api/atividades/{id}/selecionar`

Igual ao anterior, mas só retorna `200` se a atividade estiver **dentro da janela de coleta de
presença** (10 minutos antes do horário de início até o horário de término, calculado com
`LocalDateTime` completo de data+hora).

**200 OK** — mesmo shape de `GET /api/atividades/{id}`.

**403 Forbidden** — fora da janela:

```json
{ "error": "Atividade fora do período válido de registro de presença" }
```

**404 Not Found**

```json
{ "error": "Atividade não encontrada" }
```

### `GET /api/atividades/{id}/vagas`

⚠️ **Não retorna 404 para atividade inexistente** — a query é uma subtração escalar
(`maxParticipantes - inscritos`) que simplesmente não encontra linha para um `id` inexistente,
resultando em `vagasDisponiveis: 0`.

**200 OK**

```json
{ "vagasDisponiveis": 12 }
```

Vagas negativas (não deveriam ocorrer, já que reduzir `maxParticipantes` abaixo dos inscritos
atuais é bloqueado no `PUT`) são sempre exibidas como `0` (`Math.max(0, ...)`).

### `POST /api/atividades` (`ADMIN`)

Corpo é um `Map<String, Object>` livre — **sem Bean Validation** (`@RequestBody Map`, não um
DTO com `@Valid`). Nenhum campo é formalmente obrigatório do ponto de vista de validação; campos
ausentes/nulos geram `NullPointerException` em runtime, capturada pelo catch genérico e devolvida
como `500` genérico.

```json
{
  "evento_id": 1,
  "titulo": "Palestra de Abertura",
  "descricao": "...",
  "pre_requisitos": "Nenhum",
  "data_inicio": "2027-08-10",
  "data_termino": "2027-08-10",
  "horario_inicio": "08:00:00",
  "horario_termino": "12:00:00",
  "max_participantes": 50,
  "carga_horaria_total": 4,
  "carga_horaria_ministrantes": 4,
  "ministrantes_ids": [4]
}
```

Note as chaves em **snake_case** aqui, diferente das respostas em camelCase.

**200 OK** — entidade crua (formato 3 do aviso acima), com `id` já preenchido.

**400 Bad Request** — atividade com o mesmo título já existe **no mesmo evento** (títulos podem
se repetir entre eventos diferentes):

```json
{ "error": "Já existe uma atividade com este título neste evento." }
```

**500 Internal Server Error** — inclui `evento_id` inexistente (`eventoRepository...get()` sem
checar `isPresent()`, lança exceção) e qualquer campo obrigatório ausente:

```json
{ "error": "Ocorreu um erro interno no servidor." }
```

### `PUT /api/atividades/{id}` (`ADMIN`)

PATCH parcial de fato — só os campos presentes no `Map` são alterados. Mesmas chaves snake_case
do `POST`.

⚠️ **Efeito colateral automático**: se `data_inicio`/`horario_inicio`/`data_termino`/
`horario_termino` mudarem de forma que a atividade passe a conflitar com outra atividade da
**mesma inscrição** de algum participante, esse participante é **desinscrito da atividade
editada automaticamente** (não da inscrição inteira). Se essa era a última atividade da inscrição
dele no evento, `Inscrição.status` também vira `false`.

**200 OK** — sem conflitos gerados:

```json
{ "message": "Atividade editada com sucesso!" }
```

**200 OK** — com desinscrições automáticas:

```json
{ "message": "Atividade editada com sucesso! ATENÇÃO: 2 participante(s) foram desinscritos automaticamente devido a conflito de horário." }
```

**400 Bad Request** — `max_participantes` reduzido abaixo do número de inscritos atuais:

```json
{ "error": "O número máximo de participantes não pode ser inferior aos já inscritos (30 atualmente)." }
```

**400 Bad Request** — novo título duplicado no evento:

```json
{ "error": "Já existe uma atividade com este título neste evento." }
```

**404 Not Found**

```json
{ "error": "Atividade não encontrada." }
```

### `DELETE /api/atividades/{id}?confirmar=` (`ADMIN`)

Query param `confirmar` (boolean, padrão `false`).

Se houver inscritos e `confirmar` não for `true`, a exclusão é recusada com `409` — um padrão de
"clique para confirmar" em duas etapas. Com `confirmar=true`, remove em cascata (nesta ordem)
presenças registradas na atividade e os vínculos `inscrição_atividade`, e só então a atividade.

⚠️ Essa limpeza em cascata **não** atualiza `Inscrição.status` — diferente do fluxo de conflito de
horário do `PUT` acima. Se a atividade excluída era a única de uma inscrição, essa inscrição
permanece com `status = true` mas com `atividade: []` — uma inconsistência visível via
`GET /api/inscricoes/detalhes`.

**200 OK**

```json
{ "message": "Atividade excluída com sucesso." }
```

**409 Conflict** — tem inscritos e `confirmar` ausente/`false`:

```json
{ "error": "Atividade com participantes inscritos. Confirmar exclusão?" }
```

**404 Not Found**

```json
{ "error": "Atividade não encontrada." }
```

---

## Inscrições — `/api/inscricoes`

Todas autenticadas; checagem de dono (IDOR) feita manualmente em cada método via
`usuarioLogadoId`/`usuarioLogadoRole`.

### `POST /api/inscricoes`

```json
{ "participanteId": 2, "eventoId": 1, "atividadeIds": [1, 3], "modalidadeId": 5 }
```

| Campo | Tipo | Obrigatório | Nota |
|---|---|---|---|
| `participanteId` | integer | sim | deve ser o próprio usuário logado, a menos que seja `ADMIN` |
| `eventoId` | integer | sim | — |
| `atividadeIds` | integer[] | sim | IDs de atividades do evento a inscrever |
| `modalidadeId` | integer | **condicional** | obrigatório apenas se o evento tiver **mais de uma** modalidade ativa; se tiver exatamente uma, é auto-aplicada mesmo com o campo omitido/nulo |

**Ordem de validação** (importa para prever qual erro volta primeiro):

1. `participanteId`/`eventoId` nulos → `400`.
2. Dono/ADMIN (`403` se violado).
3. Participante ou evento inexistente → `404`.
4. Janela de inscrição do evento (`dataInicioInscricao`/`dataFimInscricao`) → `400`.
5. Nenhuma atividade válida selecionada → `400`.
6. Resolução de modalidade (evento sem modalidade / `modalidadeId` inválido / seleção
   obrigatória faltando) → `400`. **Note que isso roda antes do passo 7** — mesmo que o
   participante já esteja inscrito e ativo, uma modalidade inválida/faltando é reportada primeiro.
7. Já inscrito e ativo (`status = true`) → `400`.

**200 OK** — nova inscrição ou reativação de uma cancelada:

```json
{
  "id": 5,
  "dataInscricao": "2026-08-27T22:58:52.3895021",
  "status": false,
  "evento": { "id": 1, "titulo": "Semana da Computação 2027", "...": "..." },
  "participante": {
    "id": 2, "nomeCompleto": "João da Silva", "email": "joao@unesp.br",
    "ra": "123456789", "role": "PARTICIPANTE", "eventosColetados": null,
    "secretSeed": "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP"
  },
  "atividade": [ { "id": 1, "titulo": "Palestra de Abertura", "...": "..." } ],
  "modalidade": { "id": 5, "eventoId": 1, "nome": "Com Camiseta", "descricao": "...", "valor": 60.00, "ativo": true },
  "valorAplicado": 60.00
}
```

> `participante.secretSeed` **é exposto neste endpoint** (via `ParticipanteAutenticadoDTO`) —
> proposital, é o segredo Base32 usado para gerar o TOTP de presença do próprio usuário, então
> só faz sentido devolvê-lo para o dono da inscrição (ou ADMIN agindo em nome dele).
> `status = false` significa "aguardando aprovação de pagamento" quando `valorAplicado > 0`;
> `status = true` de imediato quando `valorAplicado == 0` (modalidade gratuita).
> `valorAplicado` é o valor **congelado no momento desta chamada** — se o admin mudar o preço da
> modalidade depois, este número não muda mais nesta inscrição (ver nota em
> [Modalidades](#modalidades-de-inscrição)). `modalidade` já reflete o estado **atual** da
> modalidade (nome/preço podem ter mudado desde a inscrição) — para o valor devido, use sempre
> `valorAplicado`, nunca `modalidade.valor`.

**400 Bad Request** — período de inscrição fechado:

```json
{ "error": "O período de inscrições para este evento não está aberto." }
```

**400 Bad Request** — nenhuma atividade válida (IDs inexistentes ou de outro evento):

```json
{ "error": "Nenhuma atividade válida selecionada." }
```

**400 Bad Request** — evento sem nenhuma modalidade cadastrada:

```json
{ "error": "Este evento não possui modalidades de inscrição disponíveis." }
```

**400 Bad Request** — `modalidadeId` de outro evento, inexistente ou inativo:

```json
{ "error": "Modalidade de inscrição inválida ou indisponível para este evento." }
```

**400 Bad Request** — evento com múltiplas modalidades ativas e `modalidadeId` omitido:

```json
{ "error": "Selecione uma modalidade de inscrição." }
```

**400 Bad Request** — já inscrito e ativo:

```json
{ "error": "Participante já inscrito neste evento." }
```

**403 Forbidden** — tentando inscrever outra pessoa sem ser ADMIN:

```json
{ "error": "Você só pode realizar inscrições para a sua própria conta." }
```

**404 Not Found**

```json
{ "error": "Participante ou Evento não encontrado." }
```

### `PUT /api/inscricoes/{inscricaoId}`

Troca a lista de atividades de uma inscrição existente (não mexe em modalidade/valor/status).

```json
{ "atividadeIds": [2, 3] }
```

**200 OK** — `InscricaoResponseDTO` atualizado (mesmo shape do `POST`).

**400 Bad Request** — lista vazia ou nenhum ID válido para o evento da inscrição:

```json
{ "error": "Nenhuma atividade válida selecionada." }
```

**403 Forbidden** — não é o dono nem ADMIN:

```json
{ "error": "Você só pode alterar as suas próprias inscrições." }
```

**404 Not Found**

```json
{ "error": "Inscrição não encontrada." }
```

### `GET /api/inscricoes/minhas?participanteId=`

Retorna só os **IDs de evento** com inscrição ativa (`status = true`) — não a lista de
inscrições em si. Query param `participanteId` é **String** na assinatura do método, mas
convertido para `Integer` internamente (`NumberFormatException` em valor não numérico cai no
catch genérico → `400` com mensagem genérica, não específica de formato).

**200 OK**

```json
{ "inscritos": [1, 3] }
```

**403 Forbidden**

```json
{ "error": "Você só pode visualizar suas próprias inscrições." }
```

### `GET /api/inscricoes/detalhes?participanteId=`

Retorna **todas** as inscrições do participante (ativas e inativas/canceladas/pendentes), com
detalhes completos.

**200 OK** — lista de `InscricaoResponseDTO` (mesmo shape do `POST /api/inscricoes`).

**403 Forbidden**

```json
{ "error": "Você só pode visualizar os detalhes das suas próprias inscrições." }
```

---

## Presenças — `/api/presencas`

### `POST /api/presencas/registrar`

Chamado pelo app do **coletor** ao ler o QR Code (ou digitar o PIN manual) de um participante.

```json
{ "atividadeId": 3, "codigoParticipante": "{\"p\":2,\"t\":\"123456\"}", "timestampLido": 1787900000000 }
```

| Campo | Tipo | Obrigatório | Nota |
|---|---|---|---|
| `atividadeId` | integer | sim | — |
| `codigoParticipante` | string | sim | JSON `{"p":<idParticipante>,"t":"<totp 6 dígitos>"}` (do QR Code) **ou** apenas os 6 dígitos do TOTP digitados manualmente |
| `timestampLido` | long (epoch ms) | sim | horário em que o código foi lido no dispositivo do coletor |

**Duas checagens de tolerância de tempo independentes, com escopos diferentes:**

1. **±30 minutos** entre `timestampLido` e o relógio do servidor — protege contra
   dispositivos com relógio muito dessincronizado. Falha aqui nem chega a tentar validar o TOTP.
2. **±15 segundos** (um passo de tempo para trás/para frente) na validação do próprio TOTP
   (`TOTPUtil`, passo de 15s) — a tolerância "oficial" do algoritmo.

Isso significa que um `timestampLido` pode passar na checagem de 30 minutos e ainda assim falhar
na checagem de 15 segundos do TOTP em si — são camadas diferentes.

⚠️ O **PIN manual de 6 dígitos não é um código separado** — é literalmente o mesmo TOTP que
aparece dentro do `"t"` do QR Code, só que digitado à mão. Como o coletor não sabe de quem é o
PIN, a validação varre **todos os inscritos ativos da atividade** (O(N)) até achar um TOTP que
bata; a validação por QR Code é O(1) porque o `"p"` já identifica o participante.

⚠️ **Este endpoint não verifica se a atividade está dentro da sua própria janela de horário**
(a checagem de 10 min antes até o fim, usada por `GET /api/atividades/{id}/selecionar`, **não**
é repetida aqui). Um coletor pode registrar presença fora do horário da atividade, desde que o
`timestampLido` esteja dentro de ±30 min do relógio do servidor e o TOTP seja válido.

**200 OK**

```json
{ "message": "Presença de João da Silva confirmada!", "presenca": { "participante": { "nome": "João da Silva" } } }
```

**401 Unauthorized**

```json
{ "error": "Não autenticado." }
```

**403 Forbidden** — usuário não é coletor:

```json
{ "error": "Acesso negado. Apenas coletores podem registrar presenças." }
```

**403 Forbidden** — é coletor, mas não deste evento:

```json
{ "error": "Acesso negado. Você não é coletor deste evento." }
```

**403 Forbidden** — defasagem de relógio maior que 30 minutos:

```json
{ "error": "O timestamp da leitura possui defasagem excessiva. A sincronização falhou." }
```

**403 Forbidden** — TOTP inválido, expirado, ou participante não inscrito na atividade:

```json
{ "error": "Código de presença inválido ou expirado." }
```

**404 Not Found**

```json
{ "error": "Atividade não encontrada." }
```

**409 Conflict** — presença já registrada para este participante nesta atividade (constraint
única `idUsuário`+`idAtividade`):

```json
{ "error": "Presença já registrada para este participante nesta atividade." }
```

### `GET /api/presencas/participante/{participanteId}`

Retorna os **IDs de atividade** em que o participante tem presença confirmada — não o registro
completo.

**200 OK**

```json
[1, 3, 7]
```

**403 Forbidden**

```json
{ "error": "Acesso negado. Você só pode visualizar suas próprias presenças." }
```

---

## Certificados — `/api/certificados`

Nenhum dos três endpoints faz checagem além do exposto abaixo; `PDF` é gerado sob demanda, nunca
persistido em disco (apesar de a entidade `Certificado` ter campo `pdfPath`, ele nunca é
preenchido nesse fluxo).

### `GET /api/certificados/disponiveis/{participanteId}`

Lista o que o participante **pode tentar emitir** — eventos em que tem inscrição ativa e
atividades em que é ministrante ou tem presença possível. **Não filtra por elegibilidade real**
(presença mínima, evento encerrado) — isso só é checado em `/selecionar` e `/emitir`. Ou seja,
esta lista pode conter itens que depois serão recusados.

**200 OK**

```json
{
  "eventos": [ { "tipo": "EVENTO", "id": 1, "titulo": "Semana da Computação 2027" } ],
  "atividades": [
    { "tipo": "ATIVIDADE", "id": 2, "titulo": "Minicurso de Python [Ministrante]", "cargaHoraria": 8, "eventoId": 2, "eventoTitulo": "Workshop de IA 2025" },
    { "tipo": "ATIVIDADE", "id": 1, "titulo": "Palestra de Abertura", "cargaHoraria": 4, "eventoId": 1, "eventoTitulo": "Semana da Computação 2027" }
  ]
}
```

> Atividades ministradas têm `" [Ministrante]"` concatenado ao título e usam
> `cargaHorariaMinistrante`; atividades cursadas usam `cargaHorariaTotal`. Atividades de eventos
> `POR_CARGA_TOTAL` (contabilização pelo evento inteiro, não por atividade) **não aparecem** na
> lista de atividades — só o evento aparece em `eventos`.

**403 Forbidden**

```json
{ "error": "Você só pode visualizar seus próprios certificados." }
```

### `POST /api/certificados/selecionar`

Checa elegibilidade **sem gerar o PDF** — usado pela UI para habilitar/desabilitar o botão de
emissão antes do clique real.

```json
{ "participanteId": 2, "tipo": "EVENTO", "alvoId": 1 }
```

`tipo` é `"EVENTO"` ou `"ATIVIDADE"`.

**200 OK** — sempre `200`, mesmo quando **não** liberado (o motivo vem no corpo, não no status
HTTP):

```json
{ "liberado": true, "motivo": "Liberado" }
```

```json
{ "liberado": false, "motivo": "Presença mínima não atingida neste evento." }
```

Outros motivos possíveis para `liberado: false`: `"O evento ainda não foi finalizado."`,
`"A atividade (ou evento) ainda não foi finalizada."` (⚠️ para atividades de **um único dia**,
"finalizada" significa **o dia civil ter acabado**, não o horário de término da atividade — uma
atividade das 08h–12h continua "não finalizada" para fins de certificado até a virada do dia),
`"Você não possui presença registrada nesta atividade."`.

**403 Forbidden**

```json
{ "error": "Você só pode solicitar a emissão dos seus próprios certificados." }
```

**500 Internal Server Error**

```json
{ "error": "Erro ao selecionar certificado." }
```

### `POST /api/certificados/emitir`

Gera e devolve o PDF. **Não tem checagem de dono/IDOR** — qualquer usuário autenticado pode
emitir certificado de **qualquer `participanteId`**, diferente de todos os outros endpoints de
certificado/pagamento/inscrição, que checam `usuarioLogadoId`. Isso é uma inconsistência real do
código, não uma omissão de documentação.

```json
{ "participanteId": 2, "tipo": "ATIVIDADE", "alvoId": 3 }
```

**200 OK** — `Content-Type: application/pdf`, `Content-Disposition: attachment;
filename="<nome>.pdf"` (nome do arquivo derivado de `<nomeParticipante> - <títuloAtividadeOuEvento>.pdf`,
com caracteres `\/:*?"<>|` substituídos por `_`). Corpo é o binário do PDF.

**400 Bad Request** — qualquer uma das mensagens de "não liberado" do `/selecionar`, ex.:

```json
{ "error": "Participante não possui presença confirmada nesta atividade." }
```

**500 Internal Server Error** — inclui `participanteId` inexistente:

```json
{ "error": "Ocorreu um erro interno ao processar a emissão do certificado. Tente novamente mais tarde." }
```

---

## Relatórios — `/api/relatorios`

Todas as rotas exigem `ADMIN`.

### `GET /api/relatorios/eventos`

Lista eventos **encerrados** (`dataFim < hoje`) — a fonte de dados dos relatórios só faz sentido
para eventos já finalizados.

**200 OK** — lista de `Evento` (entidades cruas, mesmo shape de `GET /api/eventos`).

### `GET /api/relatorios/tipos`

**200 OK**

```json
["GRAFICO", "PARTICIPANTES"]
```

### `POST /api/relatorios/emitir`

```json
{ "dadosEvento": { "id": 1, "titulo": "Semana da Computação 2027" }, "tipo": "GRAFICO" }
```

`tipo` é `"GRAFICO"` ou `"PARTICIPANTES"` (case-insensitive — convertido com `.toUpperCase()`).

> **`GRAFICO`** classifica os participantes inscritos em "internos" vs. "externos" por e-mail —
> qualquer e-mail contendo `unesp.br` (não só terminando em `@unesp.br`; `.contains("unesp.br")`
> aceita, por exemplo, `aluno@unesp.br.algumacoisa.com`) conta como interno. **Não considera
> `status` da inscrição** — inscrições canceladas/pendentes de pagamento também entram na
> contagem.

**200 OK** — `Content-Type: application/pdf`, `Content-Disposition: attachment;
filename="Relatorio Participantes - <título>.pdf"` (ou `"Relatorio Grafico - ..."`). Corpo é o
binário do PDF.

**500 Internal Server Error** — `tipo` fora do enum (`IllegalArgumentException` de
`TipoRelatorio.valueOf`), `dadosEvento.id` ausente/inválido, ou template HTML/imagens ausentes
(ver nota abaixo) — **não há tratamento próprio de exceção neste endpoint**, então qualquer falha
cai no handler padrão do Spring (`/error`, não `{"error": ...}`).

⚠️ **Armadilha operacional, não é bug de código**: os templates HTML/imagens usados na geração
de PDF (`template.html`, `image1.png`, `image2.jpeg`, `image3.jpeg`) estão no `.gitignore` — um
clone novo do repositório não os recebe, e a geração de relatório/certificado falha por arquivo
ausente até que sejam adicionados manualmente.

---

## Participantes — `/api/participantes`

### `GET /api/participantes` (`ADMIN`)

Lista **todos** os usuários que são `Participante` (inclui `ColetorDePresenca`, que é subtipo).
Administradores puros não aparecem.

**200 OK**

```json
[
  { "id": 2, "nomeCompleto": "João da Silva", "email": "joao@unesp.br", "ra": "123456789", "role": "PARTICIPANTE", "eventosColetados": null },
  { "id": 3, "nomeCompleto": "Maria Coletora", "email": "maria.coletora@unesp.br", "ra": "987654321", "role": "COLETOR", "eventosColetados": [ { "id": 1, "titulo": "Semana da Computação 2027" } ] }
]
```

### `GET /api/participantes/{id}` (`ADMIN`)

**200 OK** — mesmo shape de um item da lista acima.

**404 Not Found**

```json
{ "error": "Participante não encontrado" }
```

### `PUT /api/participantes/{id}`

Autenticado (qualquer role), com checagem de dono/ADMIN manual — **não** está na lista de rotas
`ADMIN`-only do `SecurityConfig` (só o `GET` está).

```json
{ "nome": "João Silva Junior", "ra": "123456789" }
```

Corpo é um `Map<String,String>` livre, sem `@Valid` — `nome`/`ra` ausentes viram `null` e
sobrescrevem os valores atuais (não é PATCH parcial: mandar só `{"nome": "..."}` **apaga** o RA
existente, já que `ra` chega como `null` e é gravado assim).

**200 OK**

```json
{ "success": true, "message": "Informações atualizadas com sucesso" }
```

**403 Forbidden**

```json
{ "error": "Acesso negado. Você só pode editar seus próprios dados." }
```

**404 Not Found** — também cobre falha de update por qualquer outro motivo (mesma mensagem para
os dois casos):

```json
{ "error": "Participante não encontrado ou falha na atualização" }
```

---

## Pagamentos — `/api/pagamentos`

Fluxo de conferência manual de comprovante PIX. Inscrição e comprovante são desacoplados: todo
`POST /api/inscricoes` já cria (ou reaproveita) um `Pagamento` vazio para a inscrição — ver
[Inscrições](#inscrições--apiinscricoes). Regras de acesso: fila administrativa (`/pendentes`,
`/evento/{id}`, `PATCH .../status`) exige `ADMIN`; os demais endpoints são `authenticated()` com
checagem de dono feita **dentro** do controller (`validarAcessoAoPagamento` — só o dono da
inscrição ou um `ADMIN` pode ver/enviar/consultar).

### `GET /api/pagamentos/minhas?participanteId=`

**200 OK**

```json
[
  {
    "id": 5, "inscricaoId": 5, "eventoId": 1, "tituloEvento": "Semana da Computação 2027",
    "status": "PENDENTE", "temComprovante": false, "nomeArquivoOriginal": null, "tipoArquivo": null,
    "tamanhoArquivo": null, "dataEnvio": null, "dataAvaliacao": null, "motivoRecusa": null,
    "chavePix": "secomp@fct.unesp.br", "valorInscricao": 40.00, "modalidadeNome": "Padrão",
    "urlComprovante": null
  }
]
```

> `valorInscricao` (nome de campo mantido por compatibilidade com o tipo `Pagamento` do
> frontend) vem de `Inscrição.valorAplicado` — **não** do preço atual da modalidade. Mudar o
> preço da modalidade depois não altera esse valor para pagamentos já criados (mesma regra da
> seção de Modalidades).

**400 Bad Request** — `participanteId` não numérico:

```json
{ "error": "Identificador de participante inválido." }
```

**403 Forbidden**

```json
{ "error": "Você só pode visualizar os seus próprios pagamentos." }
```

### `GET /api/pagamentos/minha/{inscricaoId}`

Consulta (e, se ainda não existir, **cria**) o pagamento de uma inscrição específica —
idempotente por design (`PagamentoService.obterOuCriar`).

**200 OK** — mesmo shape de um item da lista de `/minhas`.

**403 Forbidden** — não é dono nem ADMIN:

```json
{ "error": "Você só pode acessar o pagamento da sua própria inscrição." }
```

**404 Not Found** — `inscricaoId` inexistente:

```json
{ "error": "Inscrição não encontrada." }
```

### `POST /api/pagamentos/{inscricaoId}/upload`

`multipart/form-data`, campo do arquivo chamado **`arquivo`**.

Validações (nesta ordem):

1. `StatusPagamento.ISENTO` → recusa (evento gratuito não tem o que enviar).
2. `StatusPagamento.APROVADO` → recusa (já aprovado, não pode reenviar).
3. Tamanho: máx. **1 MB**.
4. `Content-Type` declarado pelo cliente precisa estar em `image/webp`, `image/jpeg`,
   `image/png` ou `application/pdf`.
5. **Magic bytes do arquivo** precisam bater com o `Content-Type` declarado — o servidor não
   confia no `Content-Type` do multipart (ex.: um `.txt` renomeado para `.png` com Content-Type
   forjado é rejeitado mesmo passando pela checagem 4).

⚠️ **Nota de configuração**: o limite de 1 MB checado aqui no código é, na prática, **inatingível
com a config atual** — `spring.servlet.multipart.max-file-size=1MB` já rejeita o upload em uma
camada anterior (fora do controller) com `413`, para qualquer arquivo acima de 1 MB, antes mesmo
de o código do passo 3 rodar. O checque no service é defesa em profundidade caso os dois limites
algum dia divirjam.

⚠️ **Se `PAGAMENTO_ARMAZENAMENTO=S3`, todo upload falha com `500`** — `ArmazenamentoS3Strategy`
é um stub deliberado (`UnsupportedOperationException`, mensagem "Armazenamento de comprovantes em
S3 ainda não foi implementado. Use pagamento.armazenamento=BANCO ou DISCO."), diferente de
`DISCO`, que grava de fato em disco (`pagamento.diretorio-uploads`) e funciona normalmente.

Reenviar um comprovante depois de **recusado** funciona e sobrescreve o anterior (remove o
arquivo antigo do armazenamento), voltando o status para `PENDENTE` e limpando
`motivoRecusa`/`dataAvaliacao`/avaliador — o SECOMPP (projeto anterior que inspirou este fluxo)
travava esse caso; aqui foi deliberadamente corrigido.

```
POST /api/pagamentos/5/upload
Content-Type: multipart/form-data; boundary=...

------...
Content-Disposition: form-data; name="arquivo"; filename="comprovante.png"
Content-Type: image/png

<bytes>
------...
```

**200 OK** — mesmo shape de item de `/minhas`, com `temComprovante: true` e `urlComprovante`
preenchida.

**400 Bad Request** — evento gratuito:

```json
{ "error": "Este evento não possui cobrança de inscrição." }
```

**400 Bad Request** — já aprovado:

```json
{ "error": "O pagamento desta inscrição já foi aprovado." }
```

**400 Bad Request** — arquivo ausente:

```json
{ "error": "O arquivo do comprovante é obrigatório." }
```

**400 Bad Request** — acima de 1 MB (na prática, ver nota acima — normalmente vira `413` antes):

```json
{ "error": "O comprovante excede o tamanho máximo de 1 MB." }
```

**400 Bad Request** — tipo de conteúdo fora da allowlist:

```json
{ "error": "Formato inválido. Envie uma imagem (WebP, JPEG ou PNG) ou um PDF." }
```

**400 Bad Request** — magic bytes não batem com o `Content-Type` declarado:

```json
{ "error": "O conteúdo do arquivo não corresponde ao formato informado." }
```

**403 Forbidden**

```json
{ "error": "Você só pode acessar o pagamento da sua própria inscrição." }
```

**413 Payload Too Large** — acima de 1 MB, interceptado pelo `GlobalExceptionHandler` antes do
controller:

```json
{ "error": "O arquivo enviado excede o tamanho máximo de 1 MB." }
```

**500 Internal Server Error** — `pagamento.armazenamento=S3` (ver nota acima):

```json
{ "error": "Armazenamento de comprovantes em S3 ainda não foi implementado. Use pagamento.armazenamento=BANCO ou DISCO." }
```

### `GET /api/pagamentos/{id}/comprovante`

Serve o **binário** do comprovante (nunca a lista/detalhe JSON) — os bytes ficam em uma tabela
separada (`comprovante_blob`), nunca em `pagamento`, para a fila do admin não precisar carregar
imagem junto ao listar dezenas de pagamentos.

**200 OK** — `Content-Type` = o tipo do arquivo original (`image/png`, `application/pdf`, etc.),
`Content-Disposition: inline; filename="<nome original ou "comprovante">"`. Corpo é o binário.

**403 Forbidden**

```json
{ "error": "Você só pode acessar o pagamento da sua própria inscrição." }
```

**404 Not Found** — `id` de pagamento inexistente **ou** pagamento existe mas não tem comprovante
enviado (mesma mensagem para os dois casos):

```json
{ "error": "Pagamento não encontrado." }
```

ou, se o pagamento existe mas está sem comprovante:

```json
{ "error": "Nenhum comprovante foi enviado para este pagamento." }
```

### `GET /api/pagamentos/pendentes` (`ADMIN`)

Todos os pagamentos com `status = PENDENTE`, **de todos os eventos** (não paginado).

**200 OK** — lista de `PagamentoPendenteResponseDTO` (estende o shape de `/minhas` com
`participanteId`, `nomeParticipante`, `emailParticipante`):

```json
[
  {
    "id": 6, "inscricaoId": 6, "eventoId": 4, "tituloEvento": "QA - Congresso de Dados 2026",
    "status": "PENDENTE", "temComprovante": true, "nomeArquivoOriginal": "comprovante-bruno.png",
    "tipoArquivo": "image/png", "tamanhoArquivo": 70, "dataEnvio": "2026-08-27T20:00:00",
    "dataAvaliacao": null, "motivoRecusa": null, "chavePix": "congresso.qa@unesp.br",
    "valorInscricao": 80.00, "modalidadeNome": "Kit Completo",
    "urlComprovante": "/api/pagamentos/6/comprovante",
    "participanteId": 7, "nomeParticipante": "Bruno Aguardando QA", "emailParticipante": "bruno.aguardando.qa@unesp.br"
  }
]
```

> Inclui pendentes **sem** comprovante enviado ainda (`temComprovante: false`) — a fila não é só
> "aguardando avaliação", é literalmente todo `status = PENDENTE`, inclusive quem nunca enviou
> nada.

### `GET /api/pagamentos/evento/{eventoId}` (`ADMIN`)

Todos os pagamentos daquele evento, **em qualquer status** (não só pendentes).

**200 OK** — lista de `PagamentoPendenteResponseDTO`, mesmo shape acima.

### `PATCH /api/pagamentos/{id}/status` (`ADMIN`)

```json
{ "novoStatus": "RECUSADO", "motivoRecusa": "Comprovante ilegível: o valor não está visível." }
```

| Campo | Tipo | Obrigatório | Nota |
|---|---|---|---|
| `novoStatus` | string (enum) | sim | só aceita `"APROVADO"` ou `"RECUSADO"` na prática (ver abaixo) |
| `motivoRecusa` | string | não | máx. 255; ignorado se `novoStatus` for `"APROVADO"` |

`novoStatus` é tipado como o enum `StatusPagamento` completo (`ISENTO`, `PENDENTE`, `APROVADO`,
`RECUSADO`) no DTO — enviar `"ISENTO"` ou `"PENDENTE"` desserializa normalmente (são literais
válidos do enum) mas é rejeitado depois, dentro do service, com `400`. Enviar qualquer string que
**não** seja um dos 4 literais do enum falha a desserialização e nunca chega no controller — vira
o `/error` padrão do Spring (ver [Formato de erro](#formato-de-erro-padrão)), não `{"error":...}`.

Aprovar/recusar também define `Inscrição.status` do pagamento (`true` se `APROVADO`, `false` se
`RECUSADO`) — **um comprovante recusado bloqueia o ingresso do participante no evento**, mesmo
que ele já estivesse com `status = true` de uma aprovação anterior (reavaliação).

**200 OK** — `PagamentoPendenteResponseDTO` atualizado.

**400 Bad Request** — `novoStatus` diferente de `APROVADO`/`RECUSADO`:

```json
{ "error": "O novo status deve ser APROVADO ou RECUSADO." }
```

**400 Bad Request** — evento gratuito (não há o que avaliar):

```json
{ "error": "Este evento não possui cobrança de inscrição." }
```

**400 Bad Request** — sem comprovante enviado ainda:

```json
{ "error": "Não é possível avaliar um pagamento sem comprovante enviado." }
```

**400 Bad Request** — reenviar a **mesma** avaliação (idempotência: clique duplo no painel não
gera segunda avaliação/e-mail):

```json
{ "error": "Este pagamento já está marcado como APROVADO." }
```

---

## Apêndice — resumo de todas as regras de negócio não óbvias

Consolidado das notas espalhadas pelas seções acima, para referência rápida:

- **Logout não invalida o token** (JWT stateless) — só o cliente descarta o token.
- **`GET /api/auth/me` não aceita cookie**, só header `Authorization`, diferente do resto da API.
- **Cadastro não exige RA para e-mail `@unesp.br`** no backend, apesar de essa regra ser citada
  em outros documentos do projeto (é enforcement de frontend, se existir).
- **Tokens de redefinição de senha vivem em memória** — reiniciar o servidor invalida todos os
  pendentes; e **contam tentativas de `/validar` e `/confirmar` juntas** para o limite de 3
  erros antes de bloquear o token.
- **`/api/eventos/disponiveis*` ignora o período de inscrição** — só olha `dataFim` do evento e
  se há atividades cadastradas.
- **Alterar o preço de uma `ModalidadeInscricao` nunca afeta `valorAplicado` de inscrições já
  feitas** — o valor é congelado na hora da inscrição.
- **Resolução de modalidade roda antes da checagem de "já inscrito"** em `POST /api/inscricoes`
  — uma modalidade inválida é reportada mesmo que a inscrição real fosse barrada por outro
  motivo.
- **Excluir modalidade em uso devolve `400`, não `409`** — orientação deliberada para desativar.
- **`AtividadeResponseDTO`, `pegarDadosAtividade()` e a entidade crua têm formatos diferentes**
  para a "mesma" atividade, dependendo do endpoint.
- **`PUT /api/atividades/{id}` pode desinscrever participantes automaticamente** por conflito de
  horário; `DELETE` em cascata **não** ajusta `Inscrição.status` da mesma forma.
- **`GET /api/atividades/{id}/vagas` nunca retorna 404`**, mesmo para atividade inexistente
  (vagas vira `0`).
- **Presença tem duas janelas de tolerância independentes**: ±30 min (relógio do dispositivo do
  coletor) e ±15s (TOTP em si) — e **não** revalida o horário da própria atividade, diferente de
  `GET /api/atividades/{id}/selecionar`.
- **PIN manual de presença é o mesmo TOTP do QR Code**, só que digitado — não é um código
  separado.
- **`POST /api/certificados/emitir` não tem checagem de dono** — qualquer usuário autenticado
  pode emitir certificado de qualquer `participanteId` (inconsistência real, não documentação
  faltante).
- **Atividade de um único dia "não finalizada" até a virada do dia civil**, mesmo depois do
  horário de término, para fins de liberação de certificado.
- **Relatório "GRAFICO" classifica por `.contains("unesp.br")`** no e-mail, não só domínio exato,
  e ignora o `status` da inscrição (conta canceladas/pendentes também).
- **Templates de PDF (certificados/relatórios) estão fora do controle de versão** — falha de
  "arquivo não encontrado" em ambiente novo não é bug de código.
- **Upload de comprovante: o limite de 1 MB do código é normalmente inatingível** — o limite do
  Spring (`multipart.max-file-size`), idêntico, rejeita antes com `413`.
- **Reenvio de comprovante após recusa é permitido** e limpa o motivo/avaliação anteriores —
  diferente do comportamento do projeto de referência (SECOMPP), que travava o aluno recusado.
- **Avaliação de pagamento é idempotente**: repetir o mesmo status novo devolve `400`.
- **`novoStatus` inválido (fora do enum) quebra a desserialização antes do controller** — resposta
  no formato padrão do Spring Boot, não `{"error": ...}`.
