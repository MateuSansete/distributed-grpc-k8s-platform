"""Cliente do demo bidirecional (ChatService.Chat).

Envia uma sequência de mensagens e, EM PARALELO, imprime as respostas do
servidor chegando em fluxo. Como o gRPC bidi é full-duplex, as respostas
intercalam com os envios (não é request/response sequencial).
"""
import threading
import time

import grpc
import demo_pb2
import demo_pb2_grpc


def _now_ms():
    return int(time.time() * 1000)


def generate_messages():
    """Gera o stream de saída do cliente (uma mensagem a cada 0.4s)."""
    frases = [
        "Oi servidor, tudo bem?",
        "Esse e o demo bidirecional do gRPC.",
        "Mensagens trafegam nos dois sentidos ao mesmo tempo.",
        "Tchau!",
    ]
    for frase in frases:
        print(f"[Cliente] -> enviando: {frase}")
        yield demo_pb2.ChatMessage(user="gabriel", text=frase, timestamp=_now_ms())
        time.sleep(0.4)


def run():
    with grpc.insecure_channel("localhost:50061") as channel:
        stub = demo_pb2_grpc.ChatServiceStub(channel)
        responses = stub.Chat(generate_messages())
        for resp in responses:
            print(f"[Cliente] <- recebido de '{resp.user}': {resp.text}")
    print("[Cliente] Stream encerrado.")


if __name__ == "__main__":
    run()
