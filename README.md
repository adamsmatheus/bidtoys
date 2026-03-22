# Leilão Backend

Monólito modular em **Kotlin + Spring Boot** para o sistema de leilões.

## Stack

| Tecnologia | Versão | Uso |
|---|---|---|
| Kotlin | 1.9.23 | Linguagem principal |
| Spring Boot | 3.2.4 | Framework |
| PostgreSQL | 16 | Banco de dados principal |
| Redis | 7 | Cache e suporte a realtime |
| Flyway | Embutido | Migrations |
| JWT (jjwt) | 0.12.5 | Autenticação |
| Springdoc OpenAPI | 2.4.0 | Documentação Swagger |
| Testcontainers | 1.19.7 | Testes de integração |

---

## Como executar localmente

### Pré-requisitos

- JDK 21+
- Docker e Docker Compose

### 1. Suba os serviços de infra

```bash
docker compose up -d
```

Isso inicia:
- PostgreSQL em `localhost:5432` (banco: `leilao`, user: `leilao`, senha: `leilao`)
- Redis em `localhost:6379`

### 2. Execute a aplicação

```bash
./gradlew bootRun
```

Ou com um profile específico:

```bash
# Somente API (worker desabilitado)
./gradlew bootRun --args='--spring.profiles.active=api --app.worker.auction-closure.enabled=false --app.worker.outbox.enabled=false'

# Somente worker (sem endpoints HTTP)
./gradlew bootRun --args='--spring.profiles.active=worker'
```

### 3. Acesse a documentação Swagger

```
http://localhost:8080/swagger-ui.html
```

### 4. Execute os testes

```bash
# Todos os testes (requer Docker para Testcontainers)
./gradlew test

# Apenas testes unitários (sem Testcontainers)
./gradlew test --tests "com.leilao.backend.auctions.*"
./gradlew test --tests "com.leilao.backend.bids.*"
```

---

## Arquitetura

### Estrutura de módulos

```
src/main/kotlin/com/leilao/backend/
├── LeilaoApplication.kt         # Entry point
├── shared/                      # Código compartilhado entre módulos
│   ├── api/                     # Response models genéricos (ErrorResponse, PageResponse)
│   ├── config/                  # SecurityConfig, RedisConfig, OpenApiConfig, JpaAuditing
│   ├── domain/                  # BaseEntity, AuditableEntity
│   ├── exception/               # Exceptions + GlobalExceptionHandler
│   └── security/                # JwtTokenProvider, JwtAuthFilter, UserPrincipal
├── auth/                        # Autenticação
│   ├── api/                     # AuthController + DTOs
│   └── application/             # RegisterUseCase, LoginUseCase
├── users/                       # Usuários
│   ├── api/                     # UserController + DTOs
│   ├── domain/                  # User, UserRole, UserStatus
│   └── infrastructure/          # UserRepository
├── auctions/                    # Leilões
│   ├── api/                     # AuctionController + DTOs
│   ├── application/             # Use cases: Create, Update, Submit, Start, Cancel, Finish, List
│   ├── domain/                  # Auction, AuctionStatus, AuctionImage, AuctionStatusHistory
│   └── infrastructure/          # Repositories
├── bids/                        # Lances
│   ├── api/                     # BidController + DTOs
│   ├── application/             # PlaceBidUseCase (com lock pessimista)
│   ├── domain/                  # Bid
│   └── infrastructure/          # BidRepository
├── notifications/               # Notificações
│   ├── application/             # SendWinnerNotificationUseCase
│   ├── domain/                  # Notification, enums
│   └── infrastructure/          # NotificationRepository, WhatsAppGateway (fake)
├── admin/                       # Operações administrativas
│   ├── api/                     # AdminAuctionController + DTOs
│   ├── application/             # ApproveAuctionUseCase, RejectAuctionUseCase
│   ├── domain/                  # AdminAlert, enums
│   └── infrastructure/          # AdminAlertRepository
├── audit/                       # Auditoria
│   ├── domain/                  # AuditLog
│   └── infrastructure/          # AuditLogRepository
└── worker/                      # Workers agendados
    ├── outbox/                  # OutboxEvent, OutboxEventRepository
    ├── AuctionClosureWorker.kt  # Encerra leilões expirados
    └── OutboxWorker.kt          # Processa eventos da outbox
```

### Decisões arquiteturais

#### Monólito modular
Cada módulo tem suas camadas internas (`api`, `application`, `domain`, `infrastructure`). Módulos se comunicam via interfaces e injeção de dependência, nunca por acesso direto a repositórios alheios (exceção: use cases de orquestração que cruzam módulos).

#### API vs Worker no mesmo codebase
O mesmo JAR pode rodar como API ou Worker usando Spring profiles e properties:
- `app.worker.auction-closure.enabled=false` desliga o worker de encerramento
- `app.worker.outbox.enabled=false` desliga o worker de outbox
- Em produção, rode dois processos: um com workers habilitados, outro sem

#### Lock pessimista para lances
`PlaceBidUseCase` usa `SELECT FOR UPDATE` via `findByIdWithLock()`. Isso garante que dois lances simultâneos no mesmo leilão sejam serializados no banco, resolvendo o empate pelo timestamp de criação do `Bid`.

#### Padrão Outbox
Ao encerrar um leilão com vencedor, o `FinishAuctionUseCase` insere um evento em `outbox_events` dentro da mesma transação. O `OutboxWorker` consome esses eventos de forma assíncrona e envia notificações. Isso desacopla o encerramento do leilão do envio de mensagens externas.

#### Prorrogação de leilão
A lógica está no domínio (`Auction.receiveNewBid()`): se `ends_at` for menos de 1 minuto no futuro quando o lance chega, soma +2 minutos.

#### Versionamento otimista
A entidade `Auction` usa `@Version` para proteger contra atualizações concorrentes fora do fluxo de lances (que usa lock pessimista).

---

## Endpoints principais

### Auth
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/auth/register` | Registrar novo usuário |
| POST | `/api/auth/login` | Login (retorna JWT) |
| GET | `/api/users/me` | Perfil do usuário autenticado |

### Leilões
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/auctions` | Criar leilão (DRAFT) |
| PUT | `/api/auctions/{id}` | Editar leilão (DRAFT/REJECTED) |
| POST | `/api/auctions/{id}/submit` | Enviar para aprovação |
| POST | `/api/auctions/{id}/start` | Iniciar leilão |
| POST | `/api/auctions/{id}/cancel` | Cancelar leilão |
| GET | `/api/auctions/{id}` | Detalhe do leilão |
| GET | `/api/auctions` | Listar leilões (paginado, filtro por status) |

### Lances
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/auctions/{id}/bids` | Dar lance |
| GET | `/api/auctions/{id}/bids` | Listar lances do leilão |

### Admin (requer ROLE_ADMIN)
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/admin/auctions/{id}/approve` | Aprovar leilão |
| POST | `/api/admin/auctions/{id}/reject` | Rejeitar leilão com motivo |

---

## Estados do leilão

```
DRAFT → PENDING_APPROVAL → READY_TO_START → ACTIVE → FINISHED_WITH_WINNER
                        ↓                         ↓
                    REJECTED                FINISHED_NO_BIDS
                        ↓
              (volta para PENDING_APPROVAL)

DRAFT/REJECTED/READY_TO_START → CANCELLED
```

---

## Próximos passos recomendados

### Alta prioridade
1. **Realtime** — Implementar WebSocket ou SSE para atualizar frontend em tempo real quando um lance é feito ou o leilão encerra
2. **Rate limiting** — Limitar lances por usuário/leilão usando Redis para evitar spam
3. **Upload de imagens** — Integrar com S3/R2/MinIO para upload e gestão de `auction_images`
4. **WhatsApp real** — Substituir `FakeWhatsAppGateway` por implementação com WhatsApp Cloud API

### Médio prazo
5. **Retry de notificação** — Adicionar lógica de reprocessamento para `outbox_events` com status `FAILED`
6. **Autenticação refinada** — Refresh token, revogação de JWT via Redis
7. **Admin panel** — Endpoints para listar alertas, usuários, resolver alertas
8. **Auditoria automática** — Listener JPA ou AOP para registrar mudanças em `audit_logs` automaticamente

### Qualidade
9. **Testes de integração** — Expandir cobertura com Testcontainers para os principais fluxos E2E
10. **Observabilidade** — Adicionar tracing (OpenTelemetry), métricas de negócio (Micrometer)
11. **Índices e performance** — Revisar queries N+1 com `@EntityGraph` ou `JOIN FETCH`
