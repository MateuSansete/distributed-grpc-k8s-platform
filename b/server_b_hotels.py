import grpc
from concurrent import futures
import time
import travel_pb2
import travel_pb2_grpc
import random

# Base de dados estática em memória: 500 hotéis
def generate_mock_hotels():
    hotels = []
    cities = ["BSB", "GIG", "GRU", "SDU", "CNF", "REC", "SSA"]
    adjectives = ["Palace", "Resort", "Plaza", "Inn", "Suites"]
    
    for i in range(1, 501):
        hotels.append(travel_pb2.Hotel(
            hotel_id=f"HTL-{i:04d}",
            name=f"Hotel {random.choice(adjectives)} {i}",
            city=random.choice(cities),
            stars=random.randint(1, 5),
            price_per_night=travel_pb2.Money(currency="BRL", amount_cents=random.randint(10000, 80000)),
            available_rooms=random.randint(1, 50),
            amenities=["wifi", "breakfast"] if random.choice([True, False]) else ["wifi"]
        ))
    return hotels

HOTELS_DB = generate_mock_hotels()

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