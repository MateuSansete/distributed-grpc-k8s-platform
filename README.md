# PSPD: Distributed Itinerary Aggregator

Projeto acadêmico desenvolvido para a disciplina de **Programação para Sistemas Paralelos e Distribuídos (PSPD)** da UnB/FCTE. O objetivo é construir uma aplicação de microsserviços distribuídos utilizando **gRPC** para comunicação interna e **Kubernetes (Minikube)** para orquestração.

## 📋 Proposta do Projeto
A aplicação consiste em um agregador de viagens que combina dados de voos e hotéis. O sistema segue a arquitetura de microserviços:

* **Módulo P (Gateway):** Interface Web e orquestração (combinação de dados).
* **Módulos A e B (Backends):** Serviços gRPC especializados em Voos e Hotéis.

## 🏗️ Arquitetura (Visão Geral)

![Diagrama da Arquitetura](assets/arq  .png)

## 🛠️ Tecnologias
* **Comunicação:** gRPC (Protobuf, HTTP/2) vs. REST-API (JSON, HTTP/1.1)
* **Infraestrutura:** Docker, Kubernetes (Minikube)
* **Linguagens:** *[Insira aqui as linguagens que escolheram, ex: Python, Go, Node.js]*

## 👥 Equipe e Responsabilidades
* **Arthur:** Infraestrutura e Kubernetes (HServ)
* **Mateus:** Microsserviços gRPC (Módulos A e B)
* **Gabriel:** API Gateway e Combinação (Módulo P)
* **Gabriel e Carlos:** Frontend (HClient) e Fundamentos gRPC
* **Arthur e Guilherme:** Versão REST e Análise de Desempenho
* **Carlos e Guilherme:** Relatório e Slides

## 🚀 Status
- [ ] Definição do contrato (.proto)
- [ ] Desenvolvimento dos Módulos (P, A, B)
- [ ] Implementação da versão REST para benchmark
- [ ] Deploy no Kubernetes
- [ ] Testes de Performance e Relatório Final
