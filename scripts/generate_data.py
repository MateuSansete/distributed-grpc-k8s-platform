import json
import os
import argparse
import sys
import random
from datetime import datetime, timedelta, timezone

def get_dataset_size():
    parser = argparse.ArgumentParser(description="Gerador de massa de dados.")
    parser.add_argument(
        '--size', 
        type=int, 
        default=1000, 
        help="Quantidade de registros a gerar (min: 500, max: 20000)"
    )
    
    args = parser.parse_args()
    
    if not (500 <= args.size <= 20000):
        print(f"Erro: O tamanho {args.size} está fora dos limites permitidos (500 a 20000).", file=sys.stderr)
        sys.exit(1)
        
    return args.size

def generate_static_database():
    # 1. Define e garante a existência do diretório compartilhado 'data'
    # Cria a pasta um nível acima se o script estiver dentro de uma subpasta
    base_dir = os.path.dirname(os.path.abspath(__file__))
    data_dir = os.path.join(base_dir, '..', 'data')
    os.makedirs(data_dir, exist_ok=True)
    
    # Pools de dados idênticos aos códigos originais
    airlines = ["Latam", "Gol", "Azul", "Avianca", "Emirates"]
    airports = ["BSB", "GIG", "GRU", "SDU", "CNF", "REC", "SSA"]
    adjectives = ["Palace", "Resort", "Plaza", "Inn", "Suites"]
    
    # Fixa o momento atual com fuso horário UTC para o cálculo das janelas de voo
    now = datetime.now(timezone.utc)
    
    # =========================================================================
    # 1. GERAÇÃO DO CATÁLOGO DE VOOS (Módulo A)
    # =========================================================================
    flights = []
    for i in range(1, tamanho_base + 1):
        days_ahead = random.randint(0, 30)
        dep_dt = now + timedelta(days=days_ahead, hours=random.randint(1, 23))
        arr_dt = dep_dt + timedelta(hours=random.randint(1, 5))
        
        origin = random.choice(airports)
        destination = random.choice(airports)
        while origin == destination:
            destination = random.choice(airports)
            
        flight = {
            "flight_id": f"FL-{i:04d}",
            "airline": random.choice(airlines),
            "origin": origin,
            "destination": destination,
            # Formato de string RFC 3339 / ISO 8601 (O ParseDict do gRPC converte isso para Timestamp nativamente)
            "departure_time": dep_dt.strftime('%Y-%m-%dT%H:%M:%SZ'),
            "arrival_time": arr_dt.strftime('%Y-%m-%dT%H:%M:%SZ'),
            "duration_minutes": int((arr_dt - dep_dt).total_seconds() / 60),
            "price": {
                "currency": "BRL",
                "amount_cents": random.randint(20000, 150000)
            },
            "available_seats": random.randint(5, 150)
        }
        flights.append(flight)
        
    # =========================================================================
    # 2. GERAÇÃO DO CATÁLOGO DE HOTÉIS (Módulo B)
    # =========================================================================
    hotels = []
    for i in range(1, tamanho_base + 1):
        hotel = {
            "hotel_id": f"HTL-{i:04d}",
            "name": f"Hotel {random.choice(adjectives)} {i}",
            "city": random.choice(airports),  # No projeto, cidades e aeroportos compartilham os mesmos códigos IATA
            "stars": random.randint(1, 5),
            "price_per_night": {
                "currency": "BRL",
                "amount_cents": random.randint(10000, 80000)
            },
            "available_rooms": random.randint(1, 50),
            "amenities": ["wifi", "breakfast"] if random.choice([True, False]) else ["wifi"]
        }
        hotels.append(hotel)
        
    # =========================================================================
    # 3. ESCRITA DOS ARQUIVOS EM DISCO
    # =========================================================================
    flights_path = os.path.join(data_dir, 'flights_db.json')
    hotels_path = os.path.join(data_dir, 'hotels_db.json')
    
    with open(flights_path, 'w', encoding='utf-8') as f:
        json.dump(flights, f, indent=2, ensure_ascii=False)
        
    with open(hotels_path, 'w', encoding='utf-8') as f:
        json.dump(hotels, f, indent=2, ensure_ascii=False)
        
    print("[Gerador] Inicialização concluída com sucesso.")
    print(f"[Gerador] Exportados {len(flights)} voos para: {flights_path}")
    print(f"[Gerador] Exportados {len(hotels)} hotéis para: {hotels_path}")

if __name__ == '__main__':
    tamanho_base = get_dataset_size()
    print(f"Iniciando geração de {tamanho_base} registros...")
    generate_static_database()