import os
import json
from fastapi import FastAPI
from typing import Optional
import uvicorn

app = FastAPI(title="Hotel Service REST - Module B")

# Inicializa a base global em memória
HOTELS_DB = []

# Caminho idêntico ao do servidor gRPC para ler o mesmo arquivo JSON compartilhado
JSON_FILE_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'data', 'hotels_db.json'))

# Carrega os dados do arquivo uma única vez na inicialização do processo
try:
    with open(JSON_FILE_PATH, 'r', encoding='utf-8') as f:
        HOTELS_DB = json.load(f)
    print(f"[REST B] Sucesso: Carregados {len(HOTELS_DB)} hotéis da base estática.")
except FileNotFoundError:
    print(f"[REST B] Erro: Arquivo não encontrado em {JSON_FILE_PATH}")
    print("Certifique-se de rodar o gerador de dados e mapear os volumes corretamente.")
except Exception as e:
    print(f"[REST B] Falha crítica ao carregar a base de hotéis: {e}")

@app.get("/api/hotels")
async def search_hotels(
    city: str,
    guests: Optional[int] = 0,
    min_stars: Optional[int] = None,
    max_price_per_night: Optional[int] = None
):
    print(f"[Module B REST] Busca para a cidade: {city} | Hospedes: {guests}")
    
    matched = []
    for h in HOTELS_DB:
        if h["city"] != city:
            continue
        if guests > 0 and h["available_rooms"] < guests:
            continue
        if min_stars is not None and h["stars"] < min_stars:
            continue
        if max_price_per_night is not None and h["price_per_night"]["amount_cents"] > max_price_per_night:
            continue
            
        matched.append(h)

    return {
        "hotels": matched,
        "total_found": len(matched)
    }

if __name__ == '__main__':
    # Roda o servidor localmente na porta 5002
    uvicorn.run(app, host="0.0.0.0", port=5002)