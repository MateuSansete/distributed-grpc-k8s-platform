import grpc
from concurrent import futures
import time
import json
import os
from google.protobuf.json_format import ParseDict
import travel_pb2
import travel_pb2_grpc

FLIGHTS_DB = []

JSON_FILE_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'data', 'flights_db.json'))

# Leitura do arquivo e conversão para objetos Protobuf
try:
    with open(JSON_FILE_PATH, 'r', encoding='utf-8') as f:
        flights_data = json.load(f)
        
        for flight_dict in flights_data:
            # Cria a instância vazia da mensagem Protobuf
            flight_pb = travel_pb2.Flight()
            
            # Mapeamento automático das chaves do dicionário para o objeto gRPC
            ParseDict(flight_dict, flight_pb)
            
            # Adiciona à lista global
            FLIGHTS_DB.append(flight_pb)
            
    print(f"[Init] Sucesso: Carregados {len(FLIGHTS_DB)} voos da base estática.")
except FileNotFoundError:
    print(f"[Erro] Arquivo não encontrado: {JSON_FILE_PATH}")
    print("Certifique-se de rodar o gerador de dados antes de iniciar o servidor.")
    exit(1)
except Exception as e:
    print(f"[Erro] Falha ao carregar a base de voos: {e}")
    exit(1)


class FlightServiceServicer(travel_pb2_grpc.FlightServiceServicer):
    
    def _filter_flights(self, request):
        req_date = request.departure_date.ToDatetime().date() if request.departure_date.seconds > 0 else None
        
        matched = []
        for f in FLIGHTS_DB:
            if f.origin != request.origin or f.destination != request.destination:
                continue
            if request.passengers > 0 and f.available_seats < request.passengers:
                continue
            # Casamento "na data ou depois": evita exigir match exato de dia
            # (os 500 voos mock estão espalhados aleatoriamente em ~30 dias,
            # então igualdade estrita quase sempre retornava zero voos).
            if req_date and f.departure_time.ToDatetime().date() < req_date:
                continue
            if request.HasField("max_price") and f.price.amount_cents > request.max_price.amount_cents:
                continue
            matched.append(f)
        return matched

    def SearchFlights(self, request, context):
        print(f"[Module A] Busca Unary: {request.origin} -> {request.destination} | Passag: {request.passengers}")
        matched_flights = self._filter_flights(request)
        return travel_pb2.FlightSearchResponse(flights=matched_flights, total_found=len(matched_flights))

    def StreamFlights(self, request, context):
        print(f"[Module A] Busca Server-Streaming: {request.origin} -> {request.destination}")
        matched_flights = self._filter_flights(request)
        for flight in matched_flights:
            time.sleep(0.05) # Simula delay de rede para testes
            yield flight

def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    travel_pb2_grpc.add_FlightServiceServicer_to_server(FlightServiceServicer(), server)
    server.add_insecure_port('[::]:50051')
    server.start()
    print("[Module A] FlightService rodando na porta 50051...")
    server.wait_for_termination()

if __name__ == '__main__':
    serve()