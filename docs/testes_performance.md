# Validação Local e Testes de Performance

Este documento descreve os passos para validar o funcionamento da API REST localmente e executar a suíte de testes de estresse comparativos entre as arquiteturas REST e gRPC utilizando o K6.

## 1. Validação Local (Comparação REST vs gRPC)

Antes de iniciar os testes de carga, é necessário garantir que as duas implementações do Gateway estão operacionais e retornando resultados idênticos.

1. Suba a infraestrutura completa em background:
   
    ```bash
    # na raiz do repositório
    docker compose up --build   
    ```

2. Valide o endpoint REST do Gateway via curl. Exemplo de requisição buscando voos e hotéis de BSB para GIG:
    
    ```bash
    diff -u \
    <(curl -s -X GET "http://localhost:9080/api/packages/search?origin=BSB&destination=GIG&departureDate=2026-06-10&returnDate=2026-06-15&travelers=2&maxResults=50" | jq 'del(.searchTimeMs, .evaluated)') \
    <(curl -s -X GET "http://localhost:8080/api/packages/search?origin=BSB&destination=GIG&departureDate=2026-06-10&returnDate=2026-06-15&travelers=2&maxResults=50" | jq 'del(.searchTimeMs, .evaluated)')
    ```

A saída do comando diff deve ser vazia, indicando que o produto cartesiano gerado por ambas as arquiteturas é exato.

## 2. Preparação da Base de Dados (Isolamento de Variáveis)

Por padrão, a primeira execução do ambiente (`docker compose up --build`) gera automaticamente uma base inicial contendo 1.000 registros.

Para validar os gargalos de Rede (I/O) e Processamento (CPU), a suíte de testes exige rodadas com volumes de dados diferentes. 

O script de geração de dados aceita o parâmetro `--size` com limite entre `500` e `20000`.

Para recriar a base de dados antes de uma rodada de testes, execute o comando abaixo alterando o valor do parâmetro:

```bash
docker compose run --rm data-generator python /scripts/generate_data.py --size 500
```
E reinicie as APIs para que elas carreguem os novos arquivos para a memória:
```bash
docker compose restart flight flight-rest hotel hotel-rest   
```

## 3. Execução dos Testes de Performance (K6)

Os testes estão divididos em cenários de carga configuráveis via variáveis de ambiente (SCENARIO e TARGET). O script de teste (k6/benchmark.js) orquestra o comportamento.

### Comando Base

O comando padrão utiliza a rede do host para evitar latência de roteamento do Docker e injeta o script do diretório k6/:

```bash
docker run --rm -i --network host -e SCENARIO=<ID_CENARIO> -e TARGET=<ARQUITETURA> grafana/k6 run - < k6/benchmark.js
```

- TARGET: Aceita os valores `rest` ou `grpc`.

- SCENARIO: Aceita valores de 1 a 6, descritos abaixo.

### Cenários Disponíveis

#### Carga Leve e Pesada (Latência Base):
Estes cenários validam o tempo de resposta isolado de uma única requisição. Observar principalmente a métrica `http_req_duration`:

- Cenário 1: Carga muito leve (Payload pequeno).
    SCENARIO=1
- Cenário 2: Carga pesada (Payload grande, maxResults=200).
    SCENARIO=2
- Cenário 3: Múltiplas requisições sequenciais leves (1 VU).
    SCENARIO=3

#### Cenários de Estresse (Concorrência e Vazão):
Testes com duração de 30 segundos medindo requisições por segundo (`http_reqs`) e saturação de tempo de resposta na p95.

- Cenário 4: Concorrência leve (10 Virtual Users).
    SCENARIO=4
- Cenário 5: Concorrência média (50 Virtual Users).
    SCENARIO=5

- Cenário 6: Alta concorrência (100 Virtual Users).
    SCENARIO=6

### Exemplo de Execução Comparativa

Para comparar o limite de vazão entre as duas arquiteturas sob alta concorrência (Cenário 6):

#### Teste REST:
```bash
docker run --rm -i --network host -e SCENARIO=6 -e TARGET=rest grafana/k6 run - < k6/benchmark.js
```
#### Teste gRPC:
```bash
docker run --rm -i --network host -e SCENARIO=6 -e TARGET=grpc grafana/k6 run - < k6/benchmark.js
```