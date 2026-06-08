# Comparativo de Performance: gRPC × REST

Resultados dos testes de carga comparando as duas implementações do backend
(P → A/B), uma usando **gRPC** (Protobuf sobre HTTP/2) e outra usando
**REST/JSON** (sobre HTTP/1.1), sob condições idênticas.

## Metodologia

- **Ferramenta:** [k6](https://k6.io/) (`grafana/k6`), script em `k6/benchmark.js`.
- **Ambiente:** stack completa via `docker compose up`, com k6 executando em
  `--network host`. Ambas as arquiteturas sobem ao mesmo tempo, na mesma
  máquina, lendo a **mesma base de dados** (1.000 voos + 1.000 hotéis).
- **Alvo:** as duas pilhas expõem o **mesmo endpoint REST** ao k6
  (`/api/packages/search`). A diferença está no diálogo interno P↔A↔B:
  gRPC (porta 8080) vs REST/JSON (porta 9080).
- **Paridade validada:** o `diff` das respostas das duas arquiteturas é vazio
  (ver `print-4`), garantindo que processam exatamente o mesmo trabalho.
- **Evidências:** capturas do resumo do k6 em `docs/evidencias/prints/`
  (`print-12` a `print-17`).

> Métricas: `http_req_duration` (latência por requisição — **avg** e **p95**) e
> `http_reqs` (vazão em **requisições por segundo**).

## Resultados

| #   | Cenário                          | Métrica         |       gRPC |       REST | Vantagem gRPC |
| --- | -------------------------------- | --------------- | ---------: | ---------: | ------------: |
| 1   | Payload pequeno (1 VU, 100 reqs) | latência avg    |   14,21 ms |   24,82 ms |          1,7× |
|     |                                  | latência p95    |   23,50 ms |   40,99 ms |          1,7× |
|     |                                  | vazão (req/s)   |       8,66 |       7,94 |          1,1× |
| 2   | Payload grande (maxResults=200)  | latência avg    |   14,09 ms |   20,29 ms |          1,4× |
|     |                                  | latência p95    |   22,46 ms |   30,56 ms |          1,4× |
|     |                                  | vazão (req/s)   |       8,68 |       8,23 |          1,1× |
|     |                                  | dados recebidos |      11 MB |      11 MB |             — |
| 3   | Sequencial (1 VU, 2.000 reqs)    | latência avg    |   12,42 ms |   31,19 ms |          2,5× |
|     |                                  | latência p95    |   42,45 ms |   47,39 ms |          1,1× |
|     |                                  | vazão (req/s)   |     140,39 |      97,54 |          1,4× |
| 4   | Concorrência 10 VUs (30 s)       | latência avg    |   11,04 ms |   38,53 ms |          3,5× |
|     |                                  | latência p95    |   14,46 ms |   54,69 ms |          3,8× |
|     |                                  | vazão (req/s)   | **895,15** | **258,50** |      **3,5×** |
| 5   | Concorrência 50 VUs (30 s)       | latência avg    |   52,16 ms |  187,08 ms |          3,6× |
|     |                                  | latência p95    |   63,43 ms |  220,97 ms |          3,5× |
|     |                                  | vazão (req/s)   | **956,71** | **266,33** |      **3,6×** |
| 6   | Concorrência 100 VUs (30 s)      | latência avg    |  103,34 ms |  370,84 ms |          3,6× |
|     |                                  | latência p95    |  122,49 ms |  423,36 ms |          3,5× |
|     |                                  | vazão (req/s)   | **964,24** | **267,91** |      **3,6×** |

> Em todos os cenários, `checks_failed = 0,00%` e `status is 200` em 100% das
> requisições nas duas arquiteturas — ninguém ganhou por descartar carga.

## Conclusão

O gRPC supera a versão REST/JSON em **todos** os cenários, e a diferença **cresce
com a concorrência**:

- **Carga leve (1 cliente, cenários 1–3):** a vantagem é modesta (~1,4× a 2,5×).
  Sem disputa por conexões, o ganho vem principalmente da **serialização binária
  (Protobuf)** ser mais barata que serializar/parsear JSON.

- **Sob concorrência (cenários 4–6):** a diferença explode para **~3,6×** em
  vazão. Enquanto o gRPC **satura em ~960 req/s** mantendo a latência média em
  ~100 ms mesmo com 100 clientes simultâneos, o REST **estaciona em ~265 req/s**
  e a latência média dispara para **370 ms**. Ou seja, sob a mesma carga o gRPC
  atende mais que o **triplo** das requisições.

**Por que o gRPC ganha:**

1. **HTTP/2 com multiplexação:** várias chamadas trafegam em paralelo sobre uma
   **única conexão TCP persistente**. O REST/HTTP1.1 abre/reaproveita conexões
   de forma menos eficiente, e cada requisição concorrente disputa o pool —
   gargalo que aparece justamente nos cenários de 10/50/100 VUs.
2. **Protobuf (binário) vs JSON (texto):** o payload binário é menor e o
   parsing é muito mais rápido que interpretar JSON em texto, reduzindo CPU e
   latência por requisição.
3. **Conexão persistente entre P e A/B:** o canal gRPC é reutilizado a cada
   chamada; a versão REST paga o custo de orquestrar requisições HTTP a cada
   diálogo interno.

A escolha do gRPC para o backend de microsserviços se justifica: o mesmo trabalho
é entregue com **menor latência** e **maior vazão**, com a vantagem se ampliando
exatamente no regime que mais importa em produção, **muitos clientes simultâneos**.

---

> Números extraídos das capturas do k6 em `docs/evidencias/prints/print-12.1` a
> `print-17.1`. Para reproduzir, ver `docs/testes_performance.md`.
