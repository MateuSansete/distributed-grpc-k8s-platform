# Os 4 tipos de comunicação gRPC no projeto

O enunciado (item B.1) pede a demonstração dos **quatro** padrões de RPC do gRPC. Os três
primeiros já existem na **aplicação principal** (serviços A e B); o bidirecional é o
**demo isolado** em [`../demo/`](../demo/). Contratos em
[`../proto/travel.proto`](../proto/travel.proto) e [`../proto/demo.proto`](../proto/demo.proto).

| Tipo | RPC | Onde | Stream |
|---|---|---|---|
| **Unário** | `FlightService.SearchFlights` | Módulo A (`a/server_a_flights.py`) | req único → resp única |
| **Server-streaming** | `FlightService.StreamFlights` | Módulo A | req único → **stream** de voos |
| **Client-streaming** | `HotelService.BulkSearchHotels` | Módulo B (`b/server_b_hotels.py`) | **stream** de reqs → resp única |
| **Bidirecional** | `ChatService.Chat` | Demo (`demo/chat_server.py`) | **stream** ↔ **stream** |

---

## 1. Unário — `SearchFlights`

Um request, uma response. É o que o gateway usa na busca de pacotes.

```proto
rpc SearchFlights (FlightSearchRequest) returns (FlightSearchResponse);
```

```python
# a/server_a_flights.py
def SearchFlights(self, request, context):
    matched = self._filter_flights(request)
    return travel_pb2.FlightSearchResponse(flights=matched, total_found=len(matched))
```

**Evidência:** qualquer busca no frontend/`curl` ao gateway dispara este RPC
(ver log do gateway: `flight.search.time=...ms ... found=N`).

## 2. Server-streaming — `StreamFlights`

Um request; o servidor devolve vários `Flight` em fluxo (`yield`), um de cada vez.

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

## 3. Client-streaming — `BulkSearchHotels`

O cliente envia vários requests em fluxo; o servidor responde **uma vez** com o agregado.

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

## 4. Bidirecional — `ChatService.Chat`

Os dois lados mantêm streams abertos simultaneamente (full-duplex sobre HTTP/2).

```proto
rpc Chat (stream ChatMessage) returns (stream ChatMessage);
```

Implementação e instruções em [`../demo/README.md`](../demo/README.md).
**Evidência real capturada:** [`evidencias/chat-demo.txt`](evidencias/chat-demo.txt) — mostra
mensagens do cliente e do servidor intercaladas no mesmo stream.

---

### Por que isso importa para o relatório

O gRPC modela os quatro padrões nativamente porque roda sobre **HTTP/2** (streams
multiplexados) + **Protobuf** (serialização binária). Em REST/HTTP1.1 o equivalente a
streaming exige gambiarras (long-polling, SSE, websockets). Esse é um dos argumentos do
comparativo gRPC × REST (Fase 4).
