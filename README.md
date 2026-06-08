# PSPD: Distributed Itinerary Aggregator

Projeto acadêmico desenvolvido para a disciplina de **Programação para Sistemas Paralelos e Distribuídos (PSPD)** da UnB/FCTE. O objetivo é construir uma aplicação de microsserviços distribuídos utilizando **gRPC** para comunicação interna e **Kubernetes (Minikube)** para orquestração.

## 📚 Documentação

| Documento | Descrição |
|---|---|
| [Como rodar (local)](docs/como-rodar.md) | Subir a aplicação localmente com Docker Compose |
| [Rodar no Kubernetes](docs/kubernetes.md) | Deploy no Minikube (manifests, comandos, troubleshooting) |
| [Como rodar os testes](docs/testes_performance.md) | Executar a suíte de benchmark (k6) gRPC × REST |
| [Resultados de performance](docs/performance.md) | Tabela comparativa gRPC × REST + conclusão |
| [Tipos de comunicação gRPC](docs/grpc-tipos.md) | Os 4 tipos (unary, server/client-streaming, bidirecional) |

## 📋 Proposta do Projeto
A aplicação consiste em um agregador de viagens que combina dados de voos e hotéis. O sistema segue a arquitetura de microserviços:

* **Módulo P (Gateway):** Interface Web e orquestração (combinação de dados).
* **Módulos A e B (Backends):** Serviços gRPC especializados em Voos e Hotéis.

## 🏗️ Arquitetura (Visão Geral)

```
   Browser (HClient)
        │ HTTP/REST
        ▼
 ┌──────────────────┐
 │  Módulo P        │  Gateway (Java/Spring Boot)
 │  REST ⇄ gRPC     │  expõe REST ao browser, fala gRPC com A e B
 └───────┬──────────┘
   gRPC  │  HTTP/2
   ┌─────┴─────┐
   ▼           ▼
┌────────┐  ┌────────┐
│Módulo A│  │Módulo B│   Serviços gRPC (Python)
│ Voos   │  │ Hotéis │
└────────┘  └────────┘
```

> Diagrama detalhado do deploy no cluster em [`docs/kubernetes.md`](docs/kubernetes.md).

## 🛠️ Tecnologias
* **Comunicação:** gRPC (Protobuf, HTTP/2) vs. REST-API (JSON, HTTP/1.1)
* **Infraestrutura:** Docker, Kubernetes (Minikube)
* **Linguagens:** Java 21 / Spring Boot (Módulo P — Gateway), Python (Módulos A e B), React (frontend HClient)

## 👥 Equipe e Responsabilidades
* **Artur:** Infraestrutura e Kubernetes (HServ)
* **Mateus:** Microsserviços gRPC (Módulos A e B)
* **Gabriel:** API Gateway e Combinação (Módulo P)
* **Gabriel e Carlos:** Frontend (HClient) e Fundamentos gRPC
* **Arthur e Guilherme:** Versão REST e Análise de Desempenho
* **Carlos e Guilherme:** Relatório e Slides

## 🚀 Status
- [x] Definição do contrato (.proto)
- [x] Desenvolvimento dos Módulos (P, A, B) + frontend
- [x] Implementação da versão REST para benchmark
- [x] Deploy no Kubernetes (Minikube) — ver [`docs/kubernetes.md`](docs/kubernetes.md)
- [x] Testes de Performance — ver [`docs/performance.md`](docs/performance.md)
- [ ] Relatório final e slides
