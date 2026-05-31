"""Servidor do demo bidirecional (ChatService.Chat).

Recebe um stream de ChatMessage e responde em stream, demonstrando full-duplex:
para cada mensagem do cliente, o servidor envia DUAS respostas (um ACK e um
auto-reply), provando que os dois lados trafegam de forma independente sobre o
mesmo canal HTTP/2.
"""
import time
from concurrent import futures

import grpc
import demo_pb2
import demo_pb2_grpc


def _now_ms():
    return int(time.time() * 1000)


class ChatServicer(demo_pb2_grpc.ChatServiceServicer):

    def Chat(self, request_iterator, context):
        print("[Servidor] Stream bidirecional aberto.")
        for msg in request_iterator:
            print(f"[Servidor] Recebido de '{msg.user}': {msg.text}")

            # Resposta 1: confirmação imediata (ACK).
            yield demo_pb2.ChatMessage(
                user="servidor",
                text=f"ACK recebi: '{msg.text}'",
                timestamp=_now_ms(),
            )

            # Resposta 2: auto-reply (mostra que o servidor empurra mais de uma
            # mensagem por mensagem recebida, sem esperar o cliente).
            time.sleep(0.1)
            yield demo_pb2.ChatMessage(
                user="bot",
                text=f"Olá {msg.user}, sua mensagem tinha {len(msg.text)} caracteres.",
                timestamp=_now_ms(),
            )
        print("[Servidor] Cliente encerrou o stream de entrada.")


def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    demo_pb2_grpc.add_ChatServiceServicer_to_server(ChatServicer(), server)
    server.add_insecure_port("[::]:50061")
    server.start()
    print("[Servidor] ChatService (bidi) rodando na porta 50061...")
    server.wait_for_termination()


if __name__ == "__main__":
    serve()
