# Memora

Plataforma de aprendizagem baseada em ciência cognitiva: recuperação ativa, repetição espaçada (FSRS) e prática intercalada. O primeiro módulo especializado é o de idiomas.

Esta é a **etapa 1**: projetos estruturados, autenticação real, catálogo de idiomas, dashboard com dados da API e identidade visual. O motor FSRS, os exercícios e a geração por IA ainda não estão ligados.

O produto funciona **integralmente sem IA**. Gemini é opcional e só entra quando o usuário pede geração de conteúdo.

## Arquitetura

Dois projetos independentes na mesma raiz:

```
memora/
├── backend/          API REST (Java 25, Spring Boot 4, Maven)
├── frontend/         SPA (Angular 22, Tailwind CSS 4)
├── docs/adr/         decisões arquiteturais
├── docker-compose.yml
└── .env.example
```

Monólito modular no backend, organizado por domínio (`authentication`, `users`, `languages`, `gamification`, `analytics`, `ai`, `shared`, e os pacotes-placeholder `learning`, `exercises`, `reviews`). Detalhes em [`docs/adr/0001-modular-monolith.md`](docs/adr/0001-modular-monolith.md).

## Tecnologias e versões

| Camada | Stack |
|---|---|
| Backend | Java 25, Spring Boot 4.1.2, Maven Wrapper |
| Persistência | PostgreSQL 18, Spring Data JPA, Flyway (`ddl-auto=validate`) |
| Segurança | Spring Security, JWT (JJWT 0.13.0), refresh cookie HttpOnly |
| API | Spring Web MVC, Jakarta Validation, Springdoc OpenAPI 3.2.0 |
| Observabilidade | Spring Boot Actuator (`/actuator/health`, `info`, `metrics`) |
| Testes | JUnit 5, Mockito, Testcontainers (PostgreSQL) |
| Frontend | Angular 22.2, TypeScript 6, RxJS 7.8, Signals |
| UI | Tailwind CSS 4.3, Lucide, Apache ECharts 6 / ngx-echarts 22, GSAP 3 |
| CI | GitHub Actions (JDK 25 + Node 26) |

## Como executar

### 1. Pré-requisitos

- JDK 25
- Node.js 22+ (a CI usa 26)
- Docker, para o PostgreSQL (e para os testes de integração)

### 2. Variáveis de ambiente

```bash
cp .env.example .env
```

Gere um segredo JWT de pelo menos 32 caracteres (`openssl rand -base64 48`) e coloque em `MEMORA_SECURITY_JWT_SECRET`. No perfil `dev` o backend aceita um fallback inseguro para não travar o primeiro `spring-boot:run`.

`GEMINI_API_KEY` pode ficar vazia. A plataforma não depende dela.

### 3. Banco de dados

```bash
docker compose up -d postgres
```

A porta padrão é `5432`. Se ela já estiver ocupada, defina `POSTGRES_PORT` e `SPRING_DATASOURCE_URL` no `.env` (por exemplo `5433`).

### 4. Backend

```bash
cd backend
./mvnw spring-boot:run          # Linux / macOS
.\mvnw.cmd spring-boot:run      # Windows
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

### 5. Frontend

Em outro terminal:

```bash
cd frontend
npm install
npm start
```

Abra http://localhost:4200. O dev server faz proxy de `/api` para `http://localhost:8080`, então o cookie de refresh é first-party (ver [ADR 0002](docs/adr/0002-authentication-and-token-storage.md)).

### Stack completa em containers

```bash
docker compose --profile app up -d --build
```

Frontend em http://localhost:4200 (Nginx faz proxy de `/api` para o backend).

## Testes

```bash
cd backend
# Se JAVA_HOME apontar para outro JDK, force o 25:
#   set JAVA_HOME=C:\Program Files\Java\jdk-25     (Windows)
#   export JAVA_HOME=/usr/lib/jvm/jdk-25           (Linux)
./mvnw verify                 # sobe PostgreSQL via Testcontainers

cd ../frontend
npx prettier --check "src/**/*.{ts,html,css,json}"
npm test
npm run build
```

## Autenticação

1. `POST /api/auth/register` ou `/login` devolve um access token JWT (15 min) e grava um refresh token opaco em cookie HttpOnly, `SameSite=Lax`, path `/api/auth`.
2. O Angular guarda o access token **só em memória**.
3. No bootstrap, `POST /api/auth/refresh` tenta restaurar a sessão.
4. Rotação a cada uso, com detecção de reuso da família do token.

Não coloque o access token no `localStorage`.

## O que esta etapa entrega

- Conta: cadastro, login, logout, refresh, perfil
- Idiomas: catálogo, matrícula, níveis CEFR, foco, remoção
- Progresso: XP, nível, sequência e meta diária (regras no backend; cliente só altera a meta)
- Dashboard: dados reais da API e estados vazios explícitos para o que ainda não existe
- Landing pública, shell desktop/mobile, páginas-placeholder para estudo, estatísticas e conquistas
- Status de IA (`GET /api/ai/status`) sem expor chave
- Dockerfiles, Compose, Flyway, Swagger, Actuator, CI

## O que fica para as próximas etapas

1. Motor FSRS com testes contra a especificação (ADR 0004)
2. Itens de aprendizagem, exercícios tipados e sessões de estudo
3. Interleaving, histórico de revisões e métricas derivadas (acertos, retenção, evolução semanal)
4. Adapter Gemini com JSON estruturado, timeout e cota por usuário (ADR 0003)
5. Missões, conquistas e XP concedido por sessão (sem duplicar recompensa)
6. Redis, RabbitMQ, Prometheus e Grafana — só quando houver carga que os justifique

## Decisões

| ADR | Tema |
|---|---|
| [0001](docs/adr/0001-modular-monolith.md) | Monólito modular por domínio |
| [0002](docs/adr/0002-authentication-and-token-storage.md) | JWT curto + refresh rotativo em cookie |
| [0003](docs/adr/0003-optional-ai-provider.md) | IA opcional, server-side, substituível |
| [0004](docs/adr/0004-learning-engine-data-model.md) | Modelo do motor e FSRS |
