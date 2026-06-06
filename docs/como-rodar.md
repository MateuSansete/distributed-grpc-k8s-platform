# Como rodar tudo — Docker

Sobe a stack inteira (gateway P + FlightService A + HotelService B + frontend) com **um comando**.

## Pré-requisito

- **Docker** + **Docker Compose v2** (`docker compose version`).

Não precisa de JDK, Python, venv nem gerar stubs — tudo acontece dentro das imagens.

---

## Subir

```bash
# na raiz do repositório
docker compose up --build
```

Quando subir:

- **Frontend:** http://localhost:5500
- **API do gateway (Stack gRPC):** http://localhost:8080
- **API do gateway (Stack REST):** http://localhost:9080

---

## Componentes

| Serviço | Porta | Publicada no host? |
|---|---|---|
| Frontend (HClient / nginx) | 5500 | sim |
| Gateway P (gRPC) | 8080 | sim |
| FlightService A (gRPC) | 50051 | não (interno) |
| HotelService B (gRPC) | 50052 | não (interno) |
| Gateway P (REST) | 9080 | sim |
| FlightService A (REST) | 5001 | não (interno) |
| HotelService B (REST) | 5002 | não (interno) |

---

## Gerenciamento da Base de Dados

Os serviços consomem uma base estática em JSON (`data/flights_db.json` e `data/hotels_db.json`) para garantir resultados consistentes nos testes de performance. A base de dados é gerada automaticamente apenas no primeiro `docker compose up` (com o tamanho padrão de 1.000 registros). A partir da segunda execução, o sistema passa a reaproveitar os dados já existentes.

Para forçar a geração de uma nova base de dados (sobrescrevendo a atual) você pode definir a quantidade de registros informando o parâmetro `--size` (min: 500, max: 20000).

Execute o script gerador diretamente dentro do contêiner isolado:

```bash
docker compose run --rm data-generator python /scripts/generate_data.py --size 5000
```

> **Nota:** Se o parâmetro `--size` for omitido, o tamanho padrão de 1.000 será gerado.

Reinicie as APIs para que elas carreguem os novos arquivos para a memória:

```bash
docker compose restart flight flight-rest hotel hotel-rest
```

---

## Comandos úteis

```bash
docker compose ps                # status dos serviços
docker compose logs -f gateway   # logs do gateway
docker compose stop flight       # derruba o FlightService (para testar 503)
docker compose start flight      # religa
docker compose down              # encerra e remove tudo
```

---

## Testar

A busca casa por código de aeroporto (`BSB`, `GIG`, `GRU`, `SDU`, `CNF`, `REC`, `SSA`), não por nome.

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

Pelo navegador: abra http://localhost:5500, escolha `BSB → GIG`, datas e viajantes, e busque.

---

## Demonstração dos 4 Tipos gRPC (Item B.1)

O edital exige a validação técnica dos 4 fluxos gRPC suportados. Abaixo os passos para reproduzir as evidências:

### 1. Comunicação Unária (Unary)

1. Certifique-se de que a infraestrutura está ativa (`docker compose up`).
2. Acesse a interface web em http://localhost:5500.
3. Dispare uma busca de voos (ex: `BSB -> GIG`).

A resposta imediata consolidada na tabela é a prova da comunicação Unária operando na lógica principal (Gateway).

### 2. Fluxos Intermediários: Server & Client Streaming

Embora implementados nos microsserviços em Python, o Gateway Java utiliza apenas fluxo unário. Para proteger os microsserviços do acesso externo do host, as suas portas (`50051` e `50052`) não estão publicadas fora da rede interna do Docker Compose.

Para homologar as funções de streaming sem alterar o `docker-compose.yml`, utilizamos testes laboratoriais em Node.js rodando por dentro da rede interna utilizando o terminal do contêiner Frontend ou do próprio repositório.

Instale as dependências (caso rode fora do container, exige mapeamento de porta):

```bash
cd laboratorio
npm install
```

Para testar o **Server-Streaming** (o cliente solicita 1 vez; o servidor devolve um fluxo contínuo de voos):

```bash
node teste_server_streaming.js
```

Para testar o **Client-Streaming** (o cliente envia várias cidades em fluxo; o servidor devolve o total consolidado numa única resposta no final):

```bash
node teste_client_streaming.js
```

> **Nota:** Caso queira rodar os scripts sem alterar o `docker-compose.yml` localmente, execute o node encapsulado: `docker compose exec frontend sh -c "node /caminho/do/script.js"` (caso a imagem comporte Node.js).

### 3. Comunicação Bidirecional (Full-Duplex)

A implementação do fluxo simultâneo — onde ambos escrevem e leem no mesmo túnel — não possui aderência arquitetural a uma busca transacional de passagens. Validamos este item através de um laboratório de Chat isolado na pasta `demo/`.

Abra dois terminais lado a lado na pasta raiz e certifique-se de ter o Python e a biblioteca gRPC local (`pip install grpcio grpcio-tools`).

**Terminal 1** — Inicie o servidor:

```bash
python demo/chat_server.py
```

**Terminal 2** — Inicie o cliente:

```bash
python demo/chat_client.py
```

---

## Troubleshooting

| Sintoma | Causa | Solução |
|---|---|---|
| Porta 8080/5500 já em uso | processo local ou outro compose no ar | `docker compose down` (ou pare o processo local) |
| Mudei código e não refletiu | imagem em cache | `docker compose up --build` (ou `docker compose build --no-cache`) |
| Gateway responde 503 logo após subir | A/B ainda inicializando | aguarde alguns segundos e repita (canais gRPC são lazy) |
| Busca volta vazia | usou nome de cidade em vez do código IATA | use `BSB`, `GIG`, `GRU`, `SDU`, `CNF`, `REC`, `SSA` |