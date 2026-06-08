<!--
  RASCUNHO DO RELATÓRIO — Projeto PSPD Parte 1
  Markdown pronto para virar PDF (ex.: VS Code "Markdown PDF", Pandoc, ou
  copiar para o Word/Google Docs). Procure por "✏️ PREENCHER" para os trechos
  que só o grupo pode escrever (nomes, autoavaliações, dificuldades individuais).
  Onde houver "📸 INSERIR", cole o print correspondente de docs/evidencias/prints/.
-->

# Plataforma Distribuída de Busca de Pacotes de Viagem com gRPC e Kubernetes

**Universidade de Brasília — Faculdade de Ciências e Tecnologias em Engenharia (FCTE)**
**Curso:** Engenharia de Software
**Disciplina:** PSPD — Programação para Sistemas Paralelos e Distribuídos
**Professor:** Fernando W. Cruz
**Turma:** ✏️ PREENCHER

**Integrantes:**

| Nome | Matrícula | Principal contribuição |
|---|---|---|
| ✏️ Gabriel ... | ✏️ | Gateway P (API Gateway) + frontend |
| ✏️ Mateus ... | ✏️ | Microsserviços gRPC A e B |
| ✏️ Artur ... | ✏️ | Infraestrutura e Kubernetes |
| ✏️ Carlos ... | ✏️ | Fundamentos gRPC + relatório/slides |
| ✏️ Guilherme ... | ✏️ | Versão REST + análise de desempenho |

> Ajuste a lista acima conforme os integrantes reais do grupo.

---

## 1. Introdução

Este relatório descreve o desenvolvimento de uma **aplicação distribuída baseada
em microsserviços** e sua disponibilização em um ambiente de contêineres
orquestrado por **Kubernetes**, conforme solicitado na Parte 1 do projeto de
pesquisa da disciplina.

A solicitação envolveu: (i) estudar o framework **gRPC** e seus quatro tipos de
comunicação; (ii) construir uma aplicação cliente/servidor composta por três
módulos (P, A e B), onde P expõe uma interface REST ao cliente web e dialoga com
A e B via gRPC; (iii) montar uma **versão alternativa em REST/JSON** para
comparação de desempenho; e (iv) implantar a aplicação em **Kubernetes
(Minikube)**.

A aplicação escolhida foi um **agregador de pacotes de viagem**: o módulo P
recebe a busca do usuário (origem, destino, datas, viajantes), consulta o módulo
**A (voos)** e o módulo **B (hotéis)** e devolve a **combinação** dos dois
ordenada por preço total. É um caso adequado ao experimento porque envolve fluxo
de comunicação entre as partes e processamento de um produto cartesiano
(voos × hotéis), o que facilita as comparações de desempenho.

Este documento está organizado em: a Seção 2 trata do framework gRPC e dos testes
dos quatro tipos de comunicação; a Seção 3 detalha a aplicação distribuída e o
comparativo gRPC × REST; a Seção 4 descreve o ambiente Kubernetes; e a Seção 5
conclui com as percepções individuais e autoavaliações.

---

## 2. O Framework gRPC

O **gRPC** é um framework de chamada de procedimento remoto (RPC) de alto
desempenho, open-source, que usa **HTTP/2** como transporte e **Protocol Buffers
(Protobuf)** como linguagem de definição de interface e formato de serialização.

### 2.1 Protocol Buffers (protobuf)

O Protobuf é um mecanismo de serialização **binário** e tipado. O contrato é
definido em um arquivo `.proto` (ver `proto/travel.proto`), a partir do qual são
geradas as classes/stubs para cada linguagem. Diferente do JSON (texto), a
representação binária é mais compacta e seu parsing é mais barato em CPU, o que
impacta diretamente a latência (ver Seção 3.5).

Exemplo de mensagem do projeto:

```proto
message Money {
  string currency     = 1; // ISO 4217: "BRL", "USD"
  int64  amount_cents = 2; // evita float para não perder precisão
}
```

### 2.2 HTTP/2

O gRPC trafega sobre **HTTP/2**, que oferece **multiplexação** (várias chamadas
simultâneas sobre uma única conexão TCP persistente), compressão de cabeçalhos e
streams bidirecionais. Isso elimina o overhead de abrir conexões a cada
requisição — vantagem decisiva sob concorrência, como mostram os testes de carga.

### 2.3 Os quatro tipos de comunicação

O projeto demonstra os **quatro** padrões de RPC do gRPC (detalhes e evidências
em `docs/grpc-tipos.md`):

| Tipo | RPC do projeto | Onde | Fluxo |
|---|---|---|---|
| **Unário** | `FlightService.SearchFlights` | Módulo A | 1 req → 1 resp |
| **Server-streaming** | `FlightService.StreamFlights` | Módulo A | 1 req → *stream* de voos |
| **Client-streaming** | `HotelService.BulkSearchHotels` | Módulo B | *stream* de reqs → 1 resp |
| **Bidirecional** | `ChatService.Chat` | Demo (`demo/`) | *stream* ↔ *stream* |

**Unário** — um request, uma response. É o fluxo principal usado pelo Gateway na
busca de pacotes.
📸 INSERIR `print-8_unary.png` (log do gateway com a chamada `flight.search`).

**Server-streaming** — o servidor envia os voos um a um (`yield`), útil para
catálogos grandes em que se quer exibir resultados parciais sem montar tudo em
memória.
📸 INSERIR `print-9_server-streaming.png`.
> *Conclusão de uso:* extração de grandes relatórios, feeds em tempo real
> (ex.: cotações de bolsa).

**Client-streaming** — o cliente envia várias requisições em fluxo e o servidor
responde uma única vez com o agregado.
📸 INSERIR `print-10_client-streaming.png`.
> *Conclusão de uso:* upload de arquivos em pedaços, ingestão massiva de logs/IoT,
> processamento em lote.

**Bidirecional** — ambos os lados mantêm streams abertos simultaneamente
(full-duplex), multiplexados sobre HTTP/2.
📸 INSERIR `print-11_bidirectional.png` (ou referenciar `docs/evidencias/chat-demo.txt`).
> *Conclusão de uso:* chat em tempo real, jogos multiplayer, edição colaborativa.

---

## 3. A Aplicação Distribuída (B.2)

### 3.1 Funcionalidades

O agregador de viagens oferece um único serviço ao usuário final: **buscar
pacotes (voo + hotel)** para um trecho e período. O módulo P:

1. recebe a requisição REST do browser;
2. consulta **em paralelo** o módulo A (voos) e o módulo B (hotéis) via gRPC;
3. combina os resultados (produto cartesiano voo × hotel);
4. ordena por preço total e devolve os melhores pacotes em JSON.

Cada módulo é **diferente** e **complementar**: a base de voos vive em A, a de
hotéis em B, e só P sabe combiná-las — atendendo ao requisito de serviço
colaborativo distribuído.

### 3.2 Arquitetura e tecnologias

```
   Browser (HClient / React)
        │ HTTP/REST
        ▼
 ┌──────────────────┐
 │  Módulo P        │  Gateway (Java 21 / Spring Boot)
 │  REST ⇄ gRPC     │
 └───────┬──────────┘
   gRPC  │  HTTP/2
   ┌─────┴─────┐
   ▼           ▼
┌────────┐  ┌────────┐
│Módulo A│  │Módulo B│   Serviços gRPC (Python)
│ Voos   │  │ Hotéis │
└────────┘  └────────┘
```

- **Módulo P (Gateway):** Java 21 + Spring Boot — expõe `GET /api/packages/search`
  e atua como cliente gRPC de A e B.
- **Módulos A e B:** Python (gRPC) — `FlightService` e `HotelService`.
- **Frontend (HClient):** React, servido por nginx.

> **Requisito atendido:** P (Java) usa linguagem **distinta** de A e B (Python).

### 3.3 Passos para instanciação

Ambiente local com Docker Compose (detalhes em `docs/como-rodar.md`):

```bash
docker compose up --build      # sobe P, A, B, frontend (+ pilha REST)
```

📸 INSERIR `print-1_subir-stack.png` (todos os serviços `Up`).

Acesso: frontend em `http://localhost:5500`, API do gateway em
`http://localhost:8080/api/packages/search`.
📸 INSERIR `print-3-frontend-browser.png` (busca na interface) e
`print-2_funcional-caso-feliz.png` (resposta JSON ordenada por preço).

### 3.4 Testes funcionais

| Cenário | Resultado esperado | Evidência |
|---|---|---|
| Busca válida | 200 + pacotes ordenados | `print-2_funcional-caso-feliz.png` |
| Serviço A (voos) fora | **503** | `print-5_flights-503.png` |
| Serviço B (hotéis) fora | **503** | `print-6_hotels-503.png` |
| Entrada inválida (`travelers=0`) | **400** | `print-7_gateway-400.png` |

O tratamento de erro é centralizado no `GlobalExceptionHandler` do gateway, que
mapeia indisponibilidade de A/B para **503** e validação de entrada para **400**.

### 3.5 Comparativo com a versão tradicional (REST/JSON)

Foi construída uma **segunda versão** da aplicação em que o diálogo P↔A↔B usa
**REST/JSON sobre HTTP/1.1** em vez de gRPC (módulos `p_rest/`, `a_rest/`,
`b_rest/`). As duas versões produzem **respostas idênticas** (validado por `diff`
— `print-4_rest-e-grpc-iguais.png`), garantindo um comparativo justo.

Os testes de carga foram executados com **k6**, ambas as pilhas na mesma máquina,
lendo a mesma base de 1.000 voos + 1.000 hotéis (metodologia completa em
`docs/performance.md` e `docs/testes_performance.md`).

| #   | Cenário                          | Métrica       |    gRPC |    REST | Vantagem |
| --- | -------------------------------- | ------------- | ------: | ------: | -------: |
| 1   | Payload pequeno (1 VU)           | avg / p95     | 14,2 / 23,5 ms | 24,8 / 41,0 ms | ~1,7× |
| 2   | Payload grande (maxResults=200)  | avg / p95     | 14,1 / 22,5 ms | 20,3 / 30,6 ms | ~1,4× |
| 3   | Sequencial (1 VU, 2.000 reqs)    | vazão (req/s) | 140,4 | 97,5 | 1,4× |
| 4   | Concorrência 10 VUs              | vazão (req/s) | **895,1** | **258,5** | **3,5×** |
| 5   | Concorrência 50 VUs              | vazão (req/s) | **956,7** | **266,3** | **3,6×** |
| 6   | Concorrência 100 VUs             | vazão (req/s) | **964,2** | **267,9** | **3,6×** |

📸 INSERIR `print-12.1` a `print-17.1` (resumos do k6 por cenário).

**Conclusão do comparativo:** o gRPC vence em todos os cenários, e a diferença
**cresce com a concorrência**. Com 1 cliente o ganho é modesto (~1,7×), vindo
principalmente da serialização binária do Protobuf. Sob carga (10/50/100 clientes
simultâneos) a vantagem salta para **~3,6× em vazão**: o gRPC satura em ~960 req/s
mantendo ~100 ms de latência média, enquanto o REST estaciona em ~265 req/s com a
latência subindo a ~370 ms. Isso decorre da **multiplexação do HTTP/2** sobre
conexão persistente e do menor custo de (de)serialização binária — exatamente o
regime (muitos clientes) que mais importa em produção.

### 3.6 Dificuldades e metodologia

> ✏️ PREENCHER: descrever a metodologia de trabalho (divisão em módulos, branches
> por pessoa, integração via contrato `.proto` fechado primeiro) e as principais
> dificuldades técnicas encontradas. Exemplos que podem ser citados:
> - Definição do contrato `.proto` compartilhado entre Java e Python.
> - Geração da massa de dados igual para gRPC e REST (sem seed → volume
>   compartilhado no K8s).
> - Integração do gateway Java (Spring Boot) com stubs gRPC.
> - Configuração do proxy nginx do frontend para o gateway no cluster.

---

## 4. Kubernetes (B.3)

Guia conceitual e operacional completo em `docs/kubernetes.md`.

### 4.1 Arquitetura no cluster

```
            http://IP_MINIKUBE:30500
                     │
              ┌──────▼───────┐
              │ frontend     │  NodePort 30500 (nginx)
              │ /api → proxy │
              └──────┬───────┘
                     │ (DNS interno do cluster)
              ┌──────▼───────┐
              │ gateway      │  ClusterIP :8080
              └───┬──────┬───┘
            gRPC  │      │  gRPC
          ┌───────┘      └───────┐
   ┌──────▼──────┐        ┌──────▼──────┐
   │ flight-svc  │        │ hotel-svc   │  ClusterIP
   │ :50051      │        │ :50052      │
   └─────────────┘        └─────────────┘
```

Apenas o **frontend** é exposto externamente (NodePort). A, B e o gateway são
**ClusterIP** (acessíveis só dentro do cluster), como pede o enunciado.

### 4.2 Arquivos de configuração (`k8s/`)

| Arquivo | Conteúdo |
|---|---|
| `data.yaml` | PVC + ConfigMap (gerador) + Job que popula a massa de dados |
| `flight.yaml` | Deployment + Service do Módulo A |
| `hotel.yaml` | Deployment + Service do Módulo B |
| `gateway.yaml` | Deployment + Service do Módulo P |
| `frontend.yaml` | ConfigMap (nginx) + Deployment + Service (NodePort) do frontend |

> **Detalhe de projeto:** as imagens de A e B não embutem os dados. Um `Job`
> (`data-generator`) gera a base **uma vez** num volume compartilhado (PVC), e
> um `initContainer` em A/B espera os arquivos existirem antes de subir — evitando
> `CrashLoopBackOff`.

### 4.3 Passos e comandos

Deploy automatizado pelo script `scripts/deploy-minikube.sh`, que executa:

```bash
minikube start --driver=docker            # 1. inicia o cluster
eval $(minikube docker-env)               # 2. aponta o docker para o Minikube
docker build -t flight-service:latest ./a # 3. constrói as imagens...
docker build -t hotel-service:latest ./b
docker build -t gateway:latest -f p/Dockerfile .
docker build -t frontend:latest ./hclient
kubectl apply -f k8s/                     # 4. aplica os manifests
kubectl rollout restart deployment --all  # 5. garante imagens novas nos pods
minikube service frontend-service --url   # 6. obtém a URL de acesso
```

📸 INSERIR `print-18_k8s.png` (`kubectl get pods` todos `Running` + Job
`Completed`), `print-19_minikube-service.png` / `print-20_minikube-frontend-url.png`
(serviços e URL), `print-21_k8s-browser.png` (app no browser via cluster) e
`print-22_k8s-gateway-logs.png` (logs do gateway chamando A/B via gRPC).

### 4.4 Dificuldades e resultados

> ✏️ PREENCHER: relatar as dificuldades do ambiente K8s e os resultados. Pontos
> reais que podem ser citados:
> - Imagens `:latest` + `imagePullPolicy: Never`: o `kubectl apply` não recria
>   pods quando a tag não muda → foi preciso `kubectl rollout restart` para os
>   pods pegarem o código novo (resolvido no script de deploy).
> - Massa de dados no cluster (sem volume os pods A/B faziam CrashLoop) →
>   resolvido com PVC + Job + initContainer.
> - Integração Docker Desktop ↔ WSL para build local.
>
> **Resultado alcançado:** aplicação distribuída rodando ponta a ponta no
> Minikube, acessível pelo browser, com P orquestrando A e B via gRPC.

---

## 5. Conclusão

> ✏️ PREENCHER: texto conclusivo do grupo sobre o experimento (2–3 parágrafos):
> o que foi construído, o que o comparativo gRPC × REST evidenciou e o
> aprendizado sobre Cloud Native / Kubernetes.

### 5.1 Considerações individuais e autoavaliação

> Cada integrante escreve um parágrafo sobre o que fez, o que aprendeu e atribui
> uma nota de autoavaliação (0 a 10) conforme o envolvimento.

**✏️ Gabriel** — contribuição, aprendizado, dificuldades. Autoavaliação: __/10

**✏️ Mateus** — contribuição, aprendizado, dificuldades. Autoavaliação: __/10

**✏️ Artur** — contribuição, aprendizado, dificuldades. Autoavaliação: __/10

**✏️ Carlos** — contribuição, aprendizado, dificuldades. Autoavaliação: __/10

**✏️ Guilherme** — contribuição, aprendizado, dificuldades. Autoavaliação: __/10

---

## Apêndice

### A. Contrato gRPC

Arquivo completo em `proto/travel.proto` (FlightService, HotelService,
TravelPackageService e ChatService).

### B. Manifests Kubernetes

Diretório `k8s/` (`data.yaml`, `flight.yaml`, `hotel.yaml`, `gateway.yaml`,
`frontend.yaml`).

### C. Instruções de execução

- Local (Docker Compose): `docs/como-rodar.md`
- Kubernetes (Minikube): `docs/kubernetes.md` + `scripts/deploy-minikube.sh`
- Testes de performance: `docs/testes_performance.md`
- Roteiro de evidências: `docs/guia-evidencias.md`

### D. Tipos de comunicação gRPC

Detalhamento e códigos em `docs/grpc-tipos.md`.
