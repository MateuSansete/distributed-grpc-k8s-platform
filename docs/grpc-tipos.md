# Os 4 tipos de comunicação gRPC no projeto

O enunciado (item B.1) pede a demonstração dos **quatro** padrões de RPC do gRPC e uma conclusão de uso para cada situação.

Os três primeiros já existem na **aplicação principal** (serviços A e B), sendo que o Server e Client streaming são provados através de scripts isolados na pasta `grpc_tests/`. O bidirecional é o **demo isolado** em `demo/`. Contratos originais localizados em `proto/travel.proto` e `proto/demo.proto`.

| Tipo | RPC | Onde | Stream |
|---|---|---|---|
| **Unário** | `FlightService.SearchFlights` | Módulo A (`a/server_a_flights.py`) | req único → resp única |
| **Server-streaming** | `FlightService.StreamFlights` | Módulo A | req único → **stream** de voos |
| **Client-streaming** | `HotelService.BulkSearchHotels` | Módulo B (`b/server_b_hotels.py`) | **stream** de reqs → resp única |
| **Bidirecional** | `ChatService.Chat` | Demo (`demo/chat_server.py`) | **stream** ↔ **stream** |

---

## 1. Unário — `SearchFlights`

Um request, uma response. É o fluxo principal que o Gateway Java utiliza na busca de pacotes.

```proto
rpc SearchFlights (FlightSearchRequest) returns (FlightSearchResponse);
```

```python
# a/server_a_flights.py
def SearchFlights(self, request, context):
    matched = self._filter_flights(request)
    return travel_pb2.FlightSearchResponse(flights=matched, total_found=len(matched))
```

**Evidência:** Qualquer busca na interface Frontend React dispara este RPC. O log do Gateway exibe o tempo de resposta e os itens encontrados (ex: `flight.search.time=29ms origin=BSB destination=GRU found=120`).

**Conclusão e Uso:** Deve ser utilizado na maioria das comunicações de um sistema, especificamente em operações transacionais e CRUDs tradicionais, onde o payload é previsível e o cliente exige a resposta imediatamente.

---

## 2. Server-streaming — `StreamFlights`

Um request; o servidor devolve vários objetos `Flight` em fluxo (`yield`), um de cada vez, evitando gargalos de memória no backend.

```proto
rpc StreamFlights (FlightSearchRequest) returns (stream Flight);
```

```python
# a/server_a_flights.py
def StreamFlights(self, request, context):
    for flight in self._filter_flights(request):
        time.sleep(0.05)   # simula chegada incremental
        yield flight
```

**Evidência:** Execução do script laboratorial `node grpc_tests/test_server_streaming.js`. O terminal exibe a chegada dos voos faseadamente, provando que o servidor consegue transmitir dados em "chunks" contínuos.

**Conclusão e Uso:** Ideal para consultas massivas que consumiriam muita memória RAM se montadas num array único. Casos de uso incluem: extração de grandes relatórios de banco de dados e feeds de atualização em tempo real (ex: home broker de ações).

---

## 3. Client-streaming — `BulkSearchHotels`

O cliente abre a conexão e envia vários requests em fluxo (ex: múltiplas cidades); o servidor aguarda o encerramento e responde uma única vez com os dados agregados.

```proto
rpc BulkSearchHotels (stream HotelSearchRequest) returns (HotelSearchResponse);
```

```python
# b/server_b_hotels.py
def BulkSearchHotels(self, request_iterator, context):
    all_matched = []
    for request in request_iterator:
        all_matched.extend(self._filter_hotels(request))
    return travel_pb2.HotelSearchResponse(hotels=all_matched, total_found=len(all_matched))
```

**Evidência:** Execução do script laboratorial `node grpc_tests/test_client_streaming.js`. O terminal mostra o envio compassado de requisições e a resposta sumariada do servidor ocorrendo apenas após o `call.end()` do cliente.

**Conclusão e Uso:** Excelente para cenários de alta volumetria de ingestão de dados. Casos práticos incluem: upload de arquivos pesados em pedaços, envio massivo de logs de telemetria de dispositivos IoT e processamento de dados em lote (batch processing).

---

## 4. Bidirecional — `ChatService.Chat`

Ambos os lados mantêm streams abertos e independentes simultaneamente (full-duplex) multiplexados sobre HTTP/2.

```proto
rpc Chat (stream ChatMessage) returns (stream ChatMessage);
```

**Evidência:** Implementação e instruções em `demo/README.md`. A evidência real está capturada em `docs/evidencias/chat-demo.txt`, demonstrando que o cliente envia mensagens iterativas e o servidor responde instantaneamente sem aguardar o fim do envio do cliente.

**Conclusão e Uso:** É a evolução definitiva sobre WebSockets. Utilizado obrigatoriamente em sistemas onde a comunicação assíncrona nos dois sentidos é vital: jogos multiplayer online, sistemas de chat real-time, videoconferências e plataformas de edição colaborativa simultânea (ex: Google Docs).