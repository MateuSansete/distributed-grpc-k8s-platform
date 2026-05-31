import grpc
from concurrent import futures
import time
from datetime import datetime, timedelta, timezone
from google.protobuf.timestamp_pb2 import Timestamp
import travel_pb2
import travel_pb2_grpc
import random

# Base de dados estática em memória: 500 voos
def generate_mock_flights():
    flights = []
    airlines = ["Latam", "Gol", "Azul", "Avianca", "Emirates"]
    airports = ["BSB", "GIG", "GRU", "SDU", "CNF", "REC", "SSA"]
    
    now = datetime.now(timezone.utc)
    
    for i in range(1, 501):
        # Gera voos para os próximos 30 dias
        days_ahead = random.randint(0, 30)
        dep_dt = now + timedelta(days=days_ahead, hours=random.randint(1, 23))
        arr_dt = dep_dt + timedelta(hours=random.randint(1, 5))
        
        dep_time = Timestamp()
        dep_time.FromDatetime(dep_dt)
        arr_time = Timestamp()
        arr_time.FromDatetime(arr_dt)
        
        flight = travel_pb2.Flight(
            flight_id=f"FL-{i:04d}",
            airline=random.choice(airlines),
            origin=random.choice(airports),
            destination=random.choice(airports),
            departure_time=dep_time,
            arrival_time=arr_time,
            duration_minutes=int((arr_dt - dep_dt).total_seconds() / 60),
            price=travel_pb2.Money(currency="BRL", amount_cents=random.randint(20000, 150000)),
            available_seats=random.randint(5, 150)
        )
        while flight.origin == flight.destination:
            flight.destination = random.choice(airports)
        flights.append(flight)
    return flights

FLIGHTS_DB = generate_mock_flights()

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