# Como rodar tudo — Docker

Sobe a stack inteira (gateway P + FlightService A + HotelService B + frontend) com **um comando**.

## Pré-requisito

- **Docker** + **Docker Compose v2** (`docker compose version`).

Não precisa de JDK, Python, venv nem gerar stubs — tudo acontece dentro das imagens.

## Subir

```bash
# na raiz do repositório
docker compose up --build
```

Quando subir:

- Frontend: **http://localhost:5500**
- API do gateway: **http://localhost:8080**

## Componentes

| Serviço | Porta | Publicada no host? |
|---|---|---|
| Frontend (HClient / nginx) | 5500 | sim |
| Gateway P (REST) | 8080 | sim |
| FlightService A (gRPC) | 50051 | não (interno) |
| HotelService B (gRPC) | 50052 | não (interno) |

## Comandos úteis

```bash
docker compose ps                # status dos serviços
docker compose logs -f gateway   # logs do gateway
docker compose stop flight       # derruba o FlightService (para testar 503)
docker compose start flight      # religa
docker compose down              # encerra e remove tudo
```

## Testar

> A busca casa por **código de aeroporto** (BSB, GIG, GRU, SDU, CNF, REC, SSA), não por nome.

```bash
# Caso feliz (200)
curl "http://localhost:8080/api/packages/search?origin=BSB&destination=GIG&departureDate=2026-06-10&returnDate=2026-06-15&travelers=2&maxResults=3"

# Erro 400 (input inválido)
curl -s "http://localhost:8080/api/packages/search?origin=BSB&destination=GIG&departureDate=2026-06-10&returnDate=2026-06-15&travelers=-1" -w "\n[%{http_code}]\n"

# Erro 503 (serviço fora): pare o A e refaça a busca
docker compose stop flight
curl -s "http://localhost:8080/api/packages/search?origin=BSB&destination=GIG&departureDate=2026-06-10&returnDate=2026-06-15&travelers=2" -w "\n[%{http_code}]\n"
docker compose start flight
```

Pelo navegador: abra **http://localhost:5500**, escolha `BSB → GIG`, datas e viajantes, e busque.

## Demo gRPC bidirecional (item B.1)

Roda à parte (não está no compose).

## Troubleshooting

| Sintoma | Causa | Solução |
|---|---|---|
| Porta 8080/5500 já em uso | processo local ou outro compose no ar | `docker compose down` (ou pare o processo local) |
| Mudei código e não refletiu | imagem em cache | `docker compose up --build` (ou `docker compose build --no-cache`) |
| Gateway responde 503 logo após subir | A/B ainda inicializando | aguarde alguns segundos e repita (canais gRPC são lazy) |
| Busca volta vazia | usou nome de cidade em vez do código IATA | use BSB, GIG, GRU, SDU, CNF, REC, SSA |
