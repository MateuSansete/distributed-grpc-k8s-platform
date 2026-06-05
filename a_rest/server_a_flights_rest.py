import os
import json
from datetime import datetime
from fastapi import FastAPI, HTTPException
from typing import Optional
import uvicorn

app = FastAPI(title="Flight Service REST - Module A")

# Inicializa a base global em memória
FLIGHTS_DB = []

# Caminho idêntico ao do servidor gRPC para ler o mesmo arquivo JSON compartilhado
JSON_FILE_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'data', 'flights_db.json'))

# Carrega os dados do arquivo uma única vez na inicialização do processo
try:
    with open(JSON_FILE_PATH, 'r', encoding='utf-8') as f:
        FLIGHTS_DB = json.load(f)
    print(f"[REST A] Sucesso: Carregados {len(FLIGHTS_DB)} voos da base estática.")
except FileNotFoundError:
    print(f"[REST A] Erro: Arquivo não encontrado em {JSON_FILE_PATH}")
    print("Certifique-se de rodar o gerador de dados e mapear os volumes corretamente.")
    # Não encerra com exit(1) imediatamente para permitir que o framework suba e reporte o estado se necessário
except Exception as e:
    print(f"[REST A] Falha crítica ao carregar a base de voos: {e}")

@app.get("/api/flights")
async def search_flights(
    origin: str,
    destination: str,
    departure_date: Optional[str] = None,
    passengers: Optional[int] = 0,
    max_price: Optional[int] = None
):
    print(f"[Module A REST] Busca: {origin} -> {destination} | Passag: {passengers}")
    
    req_date = None
    if departure_date:
        try:
            # Tenta fazer o parse da data enviada pela query string (esperado YYYY-MM-DD)
            req_date = datetime.fromisoformat(departure_date).date()
        except ValueError:
            raise HTTPException(status_code=400, detail="Formato de data inválido. Use YYYY-MM-DD")

    matched = []
    for f in FLIGHTS_DB:
        if f["origin"] != origin or f["destination"] != destination:
            continue
        if passengers > 0 and f["available_seats"] < passengers:
            continue
        
        # Regra de casamento de data idêntica ao gRPC ("na data ou depois")
        if req_date:
            # Converte a string ISO do JSON para date objeto para comparação precisa
            f_date = datetime.fromisoformat(f["departure_time"].replace('Z', '+00:00')).date()
            if f_date < req_date:
                continue
                
        # Filtro opcional de preço máximo (em centavos)
        if max_price is not None and f["price"]["amount_cents"] > max_price:
            continue
            
        matched.append(f)

    return {
        "flights": matched,
        "total_found": len(matched)
    }

if __name__ == '__main__':
    # Roda o servidor localmente na porta 5001
    uvicorn.run(app, host="0.0.0.0", port=5001)