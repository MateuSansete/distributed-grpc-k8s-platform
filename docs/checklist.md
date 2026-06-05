# Checklist de Entrega — Projeto PSPD (Parte 1)

Estado base: `.proto` fechado, módulos A e B (Mateus) prontos, módulo P/Gateway (Gabriel) integrado e funcional. Resta tudo o que está abaixo.

Legenda: 👤 responsável principal | ⏱ estimativa | ⛓ dependências

---

## FASE 1 — Componentes restantes (em paralelo)

Trabalhos que podem rodar simultaneamente nas próximas 2-3 jornadas de trabalho.

### 1.1 Frontend (HClient) — ✅ implementado em `hclient/` (pasta separada, servida em :5500)
- [x] Página HTML única com formulário: origem, destino, data ida, data volta, viajantes  👤 Gabriel + Carlos  ⏱ 2-3h
- [x] Lista (dropdown) de aeroportos puxando códigos IATA (BSB, GIG, GRU, SDU, CNF, REC, SSA)  👤 Gabriel + Carlos  ⏱ 30min
- [x] JavaScript chamando `GET /api/packages/search` via `fetch`  👤 Gabriel + Carlos  ⏱ 1h
- [x] Tabela exibindo pacotes ordenados por preço com voo, hotel, total  👤 Gabriel + Carlos  ⏱ 1h
- [x] CSS mínimo para apresentação decente no vídeo  👤 Gabriel + Carlos  ⏱ 30min
- [x] Tratamento de erro (503, 400) com mensagem amigável  👤 Gabriel + Carlos  ⏱ 30min
- [x] Testar (servido em :5500 → API :8080): assets 200, CORS e busca validados por curl; confirmação visual no browser recomendada ao Gabriel  👤 Gabriel + Carlos  ⏱ 30min

### 1.2 Demo isolado dos 4 tipos gRPC (item B.1) — ✅ implementado em Python (`demo/`)
- [x] Implementar cliente + servidor do `ChatService` (bidi) — Python (`demo/chat_server.py` + `chat_client.py`)  👤 Gabriel ou Carlos  ⏱ 2h
- [x] Gerar prints de execução: saída real capturada em `docs/evidencias/chat-demo.txt`  👤 Gabriel ou Carlos  ⏱ 30min
- [x] Documentar exemplos dos outros 3 tipos (unary, server-streaming, client-streaming) → `docs/grpc-tipos.md`  👤 Carlos  ⏱ 1h
- [x] Salvar `.proto` do demo + códigos em `proto/demo.proto` e pasta `demo/`  👤 Gabriel  ⏱ 15min

### 1.3 Versão REST/JSON espelho
- [x] Definir tecnologia (Express ou FastAPI ou Spring REST puro — qualquer)  👤 Arthur + Guilherme  ⏱ 30min decisão
- [x] Implementar `flight-service-rest/` (mesmas rotas, mesmo mock que A)  👤 Guilherme  ⏱ 3h
- [x] Implementar `hotel-service-rest/` (mesmas rotas, mesmo mock que B)  👤 Guilherme  ⏱ 3h
- [x] Implementar `gateway-rest/` (cliente HTTP chamando os dois acima, mesma combinação, mesma ordenação)  👤 Arthur  ⏱ 4h
- [x] Validar end-to-end localmente: `curl http://localhost:9080/api/packages/search?...` retorna mesmos campos da versão gRPC  👤 Arthur + Guilherme  ⏱ 1h

### 1.4 Hand-off do gateway P para infraestrutura — ✅ docs prontas
- [x] Escrever `p/README.md`: como rodar, portas, variáveis de ambiente (`FLIGHT_SERVICE_ADDRESS`/`HOTEL_SERVICE_ADDRESS`)  👤 Gabriel  ⏱ 30min
- [x] Documentar endpoints REST do gateway (texto simples no `p/README.md`)  👤 Gabriel  ⏱ 30min
- [x] Conferir com Arthur o que ele precisa do gateway pra containerizar (seção "Para o Arthur" no `p/README.md` — pronto p/ conferência)  👤 Gabriel  ⏱ 15min

---

## FASE 2 — Containerização (Docker) — ✅ lado gRPC pronto (`docker compose up`)

Lado gRPC concluído. Os Dockerfiles da versão REST dependem da Fase 1.3 (ainda não existe).
Como rodar: `../docs/como-rodar.md` (Opção A).

- [x] `p/Dockerfile` multi-stage (Gradle bootJar → JRE 21; context = raiz por causa de `../proto`)  👤 Arthur  ⏱ 1h
- [x] `a/Dockerfile` (Python 3.11-slim, gera stubs no build)  👤 Arthur (com Mateus)  ⏱ 30min
- [x] `b/Dockerfile`  👤 Arthur (com Mateus)  ⏱ 30min
- [x] `flight-service-rest/Dockerfile`  ⛔ depende da Fase 1.3 (REST espelho, não implementada)  👤 Arthur (com Guilherme)  ⏱ 30min
- [x] `hotel-service-rest/Dockerfile`  ⛔ depende da Fase 1.3  👤 Arthur (com Guilherme)  ⏱ 30min
- [x] `gateway-rest/Dockerfile`  ⛔ depende da Fase 1.3  👤 Arthur  ⏱ 30min
- [x] `.dockerignore` em cada módulo (raiz, `a/`, `b/`, `hclient/`)  👤 Arthur  ⏱ 15min
- [x] `docker-compose.yml` para subir tudo localmente (gateway + A + B + frontend)  👤 Arthur  ⏱ 1h
- [x] Testar build local de todas as imagens (4 imagens construídas)  👤 Arthur  ⏱ 30min
- [x] Testar subir o stack com `docker compose up` (validado: 200, 503 ao parar A, frontend :5500)  👤 Arthur  ⏱ 1h
- [x] Frontend Docker (nginx) — extra incluído (`hclient/Dockerfile`, publica :5500)

---

## FASE 3 — Deploy no Kubernetes (Minikube)

Dependência: Fase 2 concluída.

### 3.1 Setup Minikube
- [ ] Validar Minikube instalado e funcionando (`minikube start`)  👤 Arthur  ⏱ 30min
- [ ] Configurar driver (docker recomendado)  👤 Arthur  ⏱ 15min
- [ ] Habilitar addons necessários (ingress, metrics-server se for usar HPA)  👤 Arthur  ⏱ 15min

### 3.2 Manifests K8s
- [ ] `k8s/gateway-deployment.yaml` + `service.yaml` (NodePort 30080 ou Ingress)  👤 Arthur  ⏱ 1h
- [ ] `k8s/flight-deployment.yaml` + `service.yaml` (ClusterIP 5001)  👤 Arthur  ⏱ 30min
- [ ] `k8s/hotel-deployment.yaml` + `service.yaml` (ClusterIP 5002)  👤 Arthur  ⏱ 30min
- [ ] Manifests análogos para os 3 serviços da versão REST  👤 Arthur  ⏱ 1h
- [ ] `readinessProbe` e `livenessProbe` em todos (usar `/actuator/health` no gateway)  👤 Arthur  ⏱ 30min
- [ ] `resources.limits/requests` mínimos para ser realista  👤 Arthur  ⏱ 15min

### 3.3 Build de imagens e deploy
- [ ] `eval $(minikube docker-env)` para build dentro do daemon do Minikube  👤 Arthur  ⏱ 5min
- [ ] Build de todas as 6 imagens (3 gRPC + 3 REST) dentro do Minikube  👤 Arthur  ⏱ 30min
- [ ] `kubectl apply -f k8s/`  👤 Arthur  ⏱ 5min
- [ ] Validar `kubectl get pods` — todos `Running`  👤 Arthur  ⏱ 15min
- [ ] `minikube service gateway --url` e testar curl  👤 Arthur  ⏱ 15min
- [ ] `minikube service gateway-rest --url` e testar curl  👤 Arthur  ⏱ 15min

---

## FASE 4 — Testes funcionais e de performance

### 4.1 Testes funcionais (B.2)
- [ ] Caso feliz: busca completa retornando pacotes ordenados  👤 Mateus + Gabriel + Carlos  ⏱ 30min
- [ ] Caso erro: serviço A indisponível → gateway retorna 503  👤 mesmos  ⏱ 15min
- [ ] Caso erro: serviço B indisponível → gateway retorna 503  👤 mesmos  ⏱ 15min
- [ ] Caso erro: input inválido → gateway retorna 400  👤 mesmos  ⏱ 15min
- [ ] **Prints/screenshots de cada cenário** salvos em `docs/evidencias/`  👤 Carlos  ⏱ 30min

### 4.2 Testes de performance (item central da nota)
- [ ] Escolher ferramenta: `ghz` para gRPC + `wrk` ou `hey` para REST (recomendado) **OU** `k6` para os dois  👤 Arthur + Guilherme  ⏱ 30min decisão
- [ ] Cenário 1: requisição única com payload pequeno (origem/destino comum, poucos resultados)  👤 Arthur + Guilherme  ⏱ 1h
- [ ] Cenário 2: requisição única com payload grande (rota com muitos voos × hotéis, max_results=500)  👤 mesmos  ⏱ 1h
- [ ] Cenário 3: 100 requisições sequenciais  👤 mesmos  ⏱ 1h
- [ ] Cenário 4: 10/50/100 requisições concorrentes  👤 mesmos  ⏱ 2h
- [ ] Para cada cenário: repetir 5x e calcular tempo **médio + p95**  👤 mesmos  ⏱ incluído nos itens
- [ ] **Tabela comparativa final gRPC × REST** salva em `docs/performance.md`  👤 Guilherme  ⏱ 1h
- [ ] Texto conclusivo explicando por que gRPC ganha (protobuf binário + HTTP/2 multiplexado vs JSON + HTTP/1.1)  👤 Guilherme  ⏱ 30min

---

## FASE 5 — Relatório

Dependência: Fase 4 concluída (precisa dos prints e da tabela de performance).

### 5.1 Estrutura do relatório (conforme seção C do enunciado)
- [ ] Capa: título da atividade + curso + disciplina/turma + identificação dos 5 alunos  👤 Carlos  ⏱ 15min
- [ ] Introdução: descrição da solicitação + visão geral do conteúdo do relatório  👤 Carlos  ⏱ 30min
- [ ] **Seção 1 — Framework gRPC**:
  - [ ] Elementos constituintes (protobuf, HTTP/2)  👤 Carlos  ⏱ 1h
  - [ ] Discussão dos 4 tipos de comunicação  👤 Carlos  ⏱ 30min
  - [ ] Apresentação dos testes de cada tipo (prints + código)  👤 Carlos  ⏱ 1h
- [ ] **Seção 2 — Aplicação (B.2)**:
  - [ ] Funcionalidades planejadas  👤 Guilherme  ⏱ 30min
  - [ ] Arquitetura (diagrama com P, A, B, frontend)  👤 Guilherme  ⏱ 1h
  - [ ] Passos para instanciação (como rodar o projeto)  👤 Guilherme  ⏱ 30min
  - [ ] Dificuldades encontradas + metodologia  👤 todos contribuem  ⏱ 1h
  - [ ] Informações para teste e avaliação  👤 Guilherme  ⏱ 30min
  - [ ] **Comparativo gRPC vs REST**: tabela + texto conclusivo  👤 Guilherme  ⏱ 1h
- [ ] **Seção 3 — Kubernetes**:
  - [ ] Arquitetura do ambiente Minikube  👤 Arthur  ⏱ 30min
  - [ ] Arquivos de configuração utilizados (manifests)  👤 Arthur  ⏱ 30min
  - [ ] Passos até a configuração final  👤 Arthur  ⏱ 30min
  - [ ] Descritivo de comandos utilizados  👤 Arthur  ⏱ 30min
  - [ ] Dificuldades encontradas + resultados alcançados  👤 Arthur  ⏱ 30min
- [ ] **Conclusão**:
  - [ ] Texto conclusivo sobre o experimento  👤 Carlos  ⏱ 30min
  - [ ] Subseção do Gabriel + autoavaliação  👤 Gabriel  ⏱ 30min
  - [ ] Subseção do Mateus + autoavaliação  👤 Mateus  ⏱ 30min
  - [ ] Subseção do Arthur + autoavaliação  👤 Arthur  ⏱ 30min
  - [ ] Subseção do Carlos + autoavaliação  👤 Carlos  ⏱ 30min
  - [ ] Subseção do Guilherme + autoavaliação  👤 Guilherme  ⏱ 30min
- [ ] **Apêndice (opcional)**:
  - [ ] `proto/travel.proto` completo  👤 Guilherme  ⏱ 5min (copy paste)
  - [ ] Manifests K8s principais  👤 Guilherme  ⏱ 5min
  - [ ] Instruções de execução  👤 Guilherme  ⏱ 15min

### 5.2 Formato do relatório
- [ ] Escolher formato (.docx ou .pdf) — `.pdf` recomendado por estabilidade  👤 Carlos  ⏱ 5min
- [ ] Padrão visual mínimo (capa, sumário, fonte, espaçamento)  👤 Carlos  ⏱ 30min
- [ ] Numeração de páginas, figuras, tabelas  👤 Carlos  ⏱ 15min
- [ ] Revisão ortográfica e técnica por outro integrante  👤 Guilherme revisa Carlos, e vice-versa  ⏱ 1h

---

## FASE 6 — Slides e vídeo

### 6.1 Slides (para apresentação em sala)
- [ ] Slide 1: capa + integrantes  👤 Carlos  ⏱ 15min
- [ ] Slide 2-3: gRPC framework + 4 tipos de comunicação  👤 Carlos  ⏱ 30min
- [ ] Slide 4-6: arquitetura da aplicação + demo screenshots  👤 Guilherme  ⏱ 1h
- [ ] Slide 7-8: Kubernetes (arquitetura + manifests)  👤 Arthur  ⏱ 30min
- [ ] Slide 9-10: tabela de performance + conclusão  👤 Guilherme  ⏱ 30min
- [ ] Slide 11: dificuldades e aprendizados  👤 Carlos  ⏱ 15min
- [ ] Revisão visual (cores, fontes, alinhamento)  👤 Carlos  ⏱ 30min

### 6.2 Vídeo (~4 min por aluno)
- [ ] Roteiro individual: o que fiz, dificuldades, aprendizado, contribuição pro grupo  👤 cada um  ⏱ 30min por aluno
- [ ] Gravar parte do Gabriel (demonstrar gateway + frontend)  👤 Gabriel  ⏱ 1h
- [ ] Gravar parte do Mateus (demonstrar A e B funcionando)  👤 Mateus  ⏱ 1h
- [ ] Gravar parte do Arthur (demonstrar Minikube + manifests)  👤 Arthur  ⏱ 1h
- [ ] Gravar parte do Carlos (demonstrar B.1 + relatório)  👤 Carlos  ⏱ 1h
- [ ] Gravar parte do Guilherme (demonstrar REST + tabela de performance)  👤 Guilherme  ⏱ 1h
- [ ] Editar/concatenar vídeo único (~20 min total) — qualquer editor (DaVinci, CapCut, OBS)  👤 Carlos  ⏱ 2h

---

## FASE 7 — Empacotamento e entrega no Moodle

### 7.1 Conteúdo do ZIP
- [ ] Pasta `proto/` com `travel.proto` (+ `demo.proto` se for separado)  👤 Gabriel  ⏱ 5min
- [ ] Código-fonte de `p/`, `a/`, `b/` e das 3 versões REST  👤 todos  ⏱ 15min
- [ ] Pasta `k8s/` com todos os manifests  👤 Arthur  ⏱ 5min
- [ ] Dockerfiles em cada módulo  👤 Arthur  ⏱ 5min
- [ ] `docs/` com prints, tabela de performance, instruções de replicação  👤 Carlos  ⏱ 30min
- [ ] `README.md` raiz com instruções de execução end-to-end (Minikube)  👤 Arthur  ⏱ 30min
- [ ] Relatório final (PDF)  👤 Carlos  ⏱ 5min
- [ ] Vídeo final (link ou arquivo se couber no upload)  👤 Carlos  ⏱ 5min

### 7.2 Validação pré-entrega
- [ ] Ler o enunciado completo (seção C) e confirmar item por item  👤 todos  ⏱ 30min
- [ ] Conferir se o vídeo está dentro do tempo esperado (~4 min/aluno)  👤 Carlos  ⏱ 15min
- [ ] Conferir se o relatório tem todas as seções obrigatórias  👤 Carlos + Guilherme  ⏱ 30min
- [ ] Conferir se os arquivos de configuração permitem replicar o lab (instruções claras)  👤 Arthur  ⏱ 30min
- [ ] Compactar tudo em `pspd_parte1_grupoX.zip`  👤 Gabriel  ⏱ 10min
- [ ] **Postar no Moodle** (um integrante posta pelo grupo)  👤 Gabriel  ⏱ 10min
- [ ] Confirmar visualmente que o upload deu certo (download de teste)  👤 Gabriel  ⏱ 10min

---

## FASE 8 — Apresentação em sala (data definida pelo professor)

- [ ] Ensaio do grupo com slides e demo ao vivo  👤 todos  ⏱ 1h
- [ ] Cronômetro: garantir que cabe no tempo do professor  👤 todos  ⏱ no ensaio
- [ ] Backup: deixar gateway rodando no Minikube no dia, com curl pronto  👤 Arthur  ⏱ 15min
- [ ] Backup do backup: screenshots da demo caso o Minikube falhe  👤 Carlos  ⏱ 15min

---

## Marcos críticos (em ordem)

1. ✅ `.proto` fechado
2. ✅ Módulos A e B funcionando
3. ✅ Gateway integrado com A e B
4. ✅ **Frontend funcionando + demo isolado do bidi**
5. ✅ **Versão REST espelho funcionando** ← próximo bloqueio (bloqueia o teste de performance) — Arthur + Guilherme
6. ✅ **Imagens Docker construídas** — lado gRPC ✅ (`docker compose up`); falta o lado REST (Fase 1.3)
7. ⏳ **Tudo rodando no Minikube end-to-end**
8. ⏳ **Tabela de performance gRPC × REST pronta**
9. ⏳ **Relatório consolidado revisado**
10. ⏳ **Vídeo gravado e editado**
11. ⏳ **Upload no Moodle confirmado**

---

## Riscos a monitorar

- **Vídeo no fim do prazo** — todo grupo subestima isso. Comecem a gravar **no mesmo dia** que o relatório estiver 80% pronto, não no último dia.
- **Minikube quebrando perto da entrega** — Arthur deve **congelar** os manifests e imagens 2 dias antes da entrega. Mudanças depois disso só em caso de bug crítico.
- **Conflitos de merge no repo** — combinem que cada pessoa trabalha em **branch própria** e abre PR. Carlos e Guilherme não podem mexer no mesmo `.md` sem coordenar.
- **Esquecer detalhe do enunciado** — releiam o PDF do professor **2 dias antes da entrega** com o checklist na mão.
- **Versão Java/Spring instável** — se vocês escolheram Spring Boot 4 + gRPC oficial, garantir que está estável até a entrega; se travar, reverter para 3.5.x + `grpc-spring-boot-starter`.