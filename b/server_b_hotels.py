import grpc
from concurrent import futures
import time
import json
import os
from google.protobuf.json_format import ParseDict
import travel_pb2
import travel_pb2_grpc

HOTELS_DB = []

JSON_FILE_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'data', 'hotels_db.json'))

# Leitura do arquivo e conversão para objetos Protobuf
try:
    with open(JSON_FILE_PATH, 'r', encoding='utf-8') as f:
        hotels_data = json.load(f)
        
        for hotel_dict in hotels_data:
            # Cria a instância vazia da mensagem Protobuf
            hotel_pb = travel_pb2.Hotel()
            
            # Mapeamento automático das chaves do dicionário para o objeto gRPC
            ParseDict(hotel_dict, hotel_pb)
            
            # Adiciona à lista global
            HOTELS_DB.append(hotel_pb)
            
    print(f"[Init] Sucesso: Carregados {len(HOTELS_DB)} hotéis da base estática.")
except FileNotFoundError:
    print(f"[Erro] Arquivo não encontrado: {JSON_FILE_PATH}")
    print("Certifique-se de rodar o gerador de dados antes de iniciar o servidor.")
    exit(1)
except Exception as e:
    print(f"[Erro] Falha ao carregar a base de hotéis: {e}")
    exit(1)


class HotelServiceServicer(travel_pb2_grpc.HotelServiceServicer):
    
    def _filter_hotels(self, request):
        matched = []
        for h in HOTELS_DB:
            if h.city != request.city:
                continue
            # Considerando 1 quarto por guest para fins de viabilidade do pacote
            if request.guests > 0 and h.available_rooms < request.guests:
                continue
            if request.HasField("min_stars") and h.stars < request.min_stars:
                continue
            if request.HasField("max_price_per_night") and h.price_per_night.amount_cents > request.max_price_per_night.amount_cents:
                continue
            matched.append(h)
        return matched

    def SearchHotels(self, request, context):
        print(f"[Module B] Busca Unary para a cidade: {request.city} | Hospedes: {request.guests}")
        matched_hotels = self._filter_hotels(request)
        return travel_pb2.HotelSearchResponse(hotels=matched_hotels, total_found=len(matched_hotels))

    def BulkSearchHotels(self, request_iterator, context):
        print("[Module B] Recebida busca Client-Streaming (Bulk)...")
        all_matched_hotels = []
        
        for request in request_iterator:
            all_matched_hotels.extend(self._filter_hotels(request))
            
        return travel_pb2.HotelSearchResponse(
            hotels=all_matched_hotels,
            total_found=len(all_matched_hotels)
        )

def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    travel_pb2_grpc.add_HotelServiceServicer_to_server(HotelServiceServicer(), server)
    server.add_insecure_port('[::]:50052')
    server.start()
    print("[Module B] HotelService rodando na porta 50052...")
    server.wait_for_termination()

if __name__ == '__main__':
    serve()