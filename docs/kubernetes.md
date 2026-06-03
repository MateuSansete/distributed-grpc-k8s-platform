# Guia de Infraestrutura: Kubernetes com Minikube

**Responsável:** Arthur (Parte 1 — HServ)  
**Objetivo:** Empacotar os serviços P, A, B e frontend em containers Docker e orquestrá-los com Kubernetes, de forma que qualquer servidor com K8s possa rodar o projeto.

---

## Índice

1. [O que é Kubernetes e por que usamos?](#1-o-que-é-kubernetes-e-por-que-usamos)
2. [Arquitetura da aplicação no cluster](#2-arquitetura-da-aplicação-no-cluster)
3. [Conceitos fundamentais (leia antes de começar)](#3-conceitos-fundamentais)
4. [Pré-requisitos — o que instalar](#4-pré-requisitos)
5. [Estrutura dos arquivos criados](#5-estrutura-dos-arquivos-criados)
6. [Passo a passo para subir a aplicação no Minikube](#6-passo-a-passo)
7. [Comandos úteis para o dia a dia](#7-comandos-úteis)
8. [Subir em qualquer cluster K8s (Docker Hub)](#8-subir-em-qualquer-cluster-k8s)
9. [Troubleshooting](#9-troubleshooting)

---

## 1. O que é Kubernetes e por que usamos?

**Kubernetes (K8s)** é um sistema que gerencia containers em produção. Enquanto o `docker compose` é ótimo para rodar tudo na sua máquina, o Kubernetes foi feito para ambientes reais: múltiplos servidores, reinício automático em caso de falha, balanceamento de carga, etc.

**Por que usamos aqui?**  
O professor exige que o projeto rode em ambiente K8s. Além disso, usar K8s deixa o projeto **portável**: qualquer servidor com Kubernetes instalado consegue subir a aplicação só com os arquivos `.yaml` deste repositório.

**Minikube** é uma versão do Kubernetes que roda em uma única máquina (sua ou do servidor Linux). É a forma padrão de desenvolver/testar localmente antes de ir para um cluster real.

---

## 2. Arquitetura da aplicação no cluster

```
                    FORA DO CLUSTER
                         │
               http://IP_MINIKUBE:30500
                         │
                         ▼
              ┌─────────────────────┐
              │   frontend (nginx)  │  ← NodePort 30500
              │   /usr/share/html   │
              │                     │
              │  /api/* ──proxy──►  │
              └──────────┬──────────┘
                         │ http interno (DNS do cluster)
                         ▼
              ┌─────────────────────┐
              │  gateway-service    │  ← ClusterIP (só interno)
              │  Java Spring Boot   │
              │  porta 8080         │
              └────────┬──────┬─────┘
              gRPC     │      │ gRPC
              (HTTP/2) │      │ (HTTP/2)
               ┌───────┘      └───────┐
               ▼                      ▼
  ┌────────────────────┐  ┌────────────────────┐
  │  flight-service    │  │  hotel-service     │
  │  Python gRPC       │  │  Python gRPC       │
  │  porta 50051       │  │  porta 50052       │
  └────────────────────┘  └────────────────────┘
```

**Regra importante:** A e B só ficam acessíveis **dentro** do cluster (ClusterIP). O único ponto de entrada externo é o frontend na porta 30500. O browser chama `/api/...` no frontend, que o nginx repassa internamente ao gateway, que por sua vez chama A e B via gRPC.

---

## 3. Conceitos fundamentais

Antes de rodar os comandos, entenda o que cada peça faz:

### Pod
A menor unidade do K8s. É basicamente um container rodando. Pods são **temporários** — se morrem, o K8s cria outro.

### Deployment
É a "receita" que diz ao K8s: *"quero N cópias desse container, com essa imagem e essas configurações"*. Se um Pod morrer, o Deployment garante que um novo suba.

### Service
Cria um endereço de rede estável para um grupo de Pods. Sem o Service, você nunca saberia o IP de um Pod (ele muda a cada restart). Com o Service, você usa o **nome** (`flight-service`, `gateway-service`) e o K8s resolve.

Existem três tipos usados aqui:
- **ClusterIP**: só acessível de dentro do cluster. Usado para A, B e gateway.
- **NodePort**: expõe uma porta no IP do nó (servidor). Usado para o frontend — é assim que o browser acessa a aplicação.

### ConfigMap
Arquivo de configuração que você injeta no container sem precisar reconstruir a imagem. Usamos para o arquivo de configuração do nginx do frontend.

### imagePullPolicy: Never
Diz ao K8s para usar a imagem que já existe localmente no Minikube (não tentar baixar do Docker Hub). Essencial quando você constrói as imagens dentro do Minikube.

---

## 4. Pré-requisitos

Execute em um servidor **Linux** (Ubuntu/Debian recomendado):

### 4.1 Docker

```bash
# Instala o Docker
curl -fsSL https://get.docker.com | sh

# Adiciona seu usuário ao grupo docker (sem precisar de sudo)
sudo usermod -aG docker $USER

# Aplica o grupo sem precisar fazer logout
newgrp docker

# Verifica
docker --version
```

### 4.2 kubectl (o CLI do Kubernetes)

```bash
# Baixa o binário
curl -LO "https://dl.k8s.io/release/$(curl -sL https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"

# Instala
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Verifica
kubectl version --client
```

### 4.3 Minikube

```bash
# Baixa e instala
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# Verifica
minikube version
```

---

## 5. Estrutura dos arquivos criados

```
k8s/
├── flight.yaml     # Deployment + Service para o Serviço A (FlightService)
├── hotel.yaml      # Deployment + Service para o Serviço B (HotelService)
├── gateway.yaml    # Deployment + Service para o Módulo P (Gateway)
└── frontend.yaml   # ConfigMap (nginx) + Deployment + Service para o Frontend
```

Cada arquivo segue o padrão:
```yaml
kind: Deployment   # "quero esse container rodando"
---
kind: Service      # "cria um endereço fixo para ele"
```

---

## 6. Passo a passo

### 6.1 Iniciar o Minikube

```bash
# Inicia o Minikube usando Docker como driver (não precisa de VM extra)
minikube start --driver=docker

# Verifica se está rodando
minikube status
```

Saída esperada:
```
minikube
type: Control Plane
host: Running
kubelet: Running
apiserver: Running
kubeconfig: Configured
```

### 6.2 Apontar o Docker para dentro do Minikube

Este é o passo mais importante e mais esquecido. O Minikube tem seu próprio Docker interno. Se você construir as imagens com o Docker normal da sua máquina, o Minikube não vai encontrá-las.

```bash
# Configura o terminal para usar o Docker do Minikube
eval $(minikube docker-env)

# Confirme que funcionou — você verá os containers internos do Minikube
docker ps
```

> **Importante:** esse comando só vale para a sessão atual do terminal. Se abrir outro terminal, rode `eval $(minikube docker-env)` novamente.

### 6.3 Construir as imagens Docker

Ainda com o Docker apontando para dentro do Minikube, construa todas as imagens:

```bash
# Vá para a raiz do repositório
cd /caminho/para/distributed-grpc-k8s-platform

# Serviço A — FlightService (Python)
docker build -t flight-service:latest ./a

# Serviço B — HotelService (Python)
docker build -t hotel-service:latest ./b

# Módulo P — Gateway (Java). O contexto precisa ser a raiz do repo
# porque o Gradle acessa a pasta /proto durante o build.
docker build -t gateway:latest -f p/Dockerfile .

# Frontend (nginx)
docker build -t frontend:latest ./hclient
```

Verifique que as imagens estão disponíveis dentro do Minikube:

```bash
docker images | grep -E "flight-service|hotel-service|gateway|frontend"
```

### 6.4 Aplicar os manifestos Kubernetes

```bash
# Aplica todos os arquivos da pasta k8s/ de uma vez
kubectl apply -f k8s/
```

Saída esperada:
```
deployment.apps/flight-service created
service/flight-service created
deployment.apps/hotel-service created
service/hotel-service created
deployment.apps/gateway created
service/gateway-service created
configmap/nginx-frontend-config created
deployment.apps/frontend created
service/frontend-service created
```

### 6.5 Verificar se tudo subiu

```bash
# Mostra o status de todos os Pods
kubectl get pods

# Aguarde todos ficarem com STATUS = Running
# (pode demorar 1-2 minutos na primeira vez, o gateway Java é mais lento)
```

Saída esperada (todos `Running`):
```
NAME                               READY   STATUS    RESTARTS   AGE
flight-service-7d8b9c4f6-x2k9p    1/1     Running   0          90s
hotel-service-6f9d8c7b5-m3j2q     1/1     Running   0          90s
gateway-5c8f7b6d4-k1p8n           1/1     Running   0          75s
frontend-4b7c6d5f3-n2m9r          1/1     Running   0          90s
```

Verifique os Services também:
```bash
kubectl get services
```

Saída esperada:
```
NAME               TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)
flight-service     ClusterIP   10.96.x.x       <none>        50051/TCP
hotel-service      ClusterIP   10.96.x.x       <none>        50052/TCP
gateway-service    ClusterIP   10.96.x.x       <none>        8080/TCP
frontend-service   NodePort    10.96.x.x       <none>        80:30500/TCP
```

### 6.6 Descobrir o endereço de acesso

```bash
# Obtém a URL completa para abrir no browser
minikube service frontend-service --url
```

Isso vai retornar algo como:
```
http://192.168.49.2:30500
```

Abra esse endereço no browser. Você verá o formulário de busca de viagens.

---

## 7. Comandos úteis

```bash
# Ver os Pods em tempo real (atualiza a cada 2s)
kubectl get pods -w

# Ver logs de um serviço (substitua pelo nome real do pod)
kubectl logs -f pod/flight-service-7d8b9c4f6-x2k9p

# Ver logs do gateway (útil para depurar chamadas gRPC)
kubectl logs -f deployment/gateway

# Ver detalhes de um Pod (útil quando fica em CrashLoopBackOff)
kubectl describe pod/NOME_DO_POD

# Reiniciar um Deployment (força re-criação dos Pods)
kubectl rollout restart deployment/gateway

# Derrubar tudo (os Pods são deletados, mas o cluster continua rodando)
kubectl delete -f k8s/

# Parar o Minikube (suspende o cluster, não apaga nada)
minikube stop

# Apagar tudo e começar do zero
minikube delete
```

---

## 8. Subir em qualquer cluster K8s

Para rodar em um servidor real (não Minikube), as imagens precisam estar em um registro público ou privado acessível ao cluster. O mais simples é o **Docker Hub**.

### 8.1 Publicar imagens no Docker Hub

```bash
# Faça login (crie conta em hub.docker.com se não tiver)
docker login

# Substitua SEU_USUARIO pelo seu usuário do Docker Hub
USUARIO=SEU_USUARIO

# Build + push de cada imagem
docker build -t $USUARIO/flight-service:latest ./a
docker push $USUARIO/flight-service:latest

docker build -t $USUARIO/hotel-service:latest ./b
docker push $USUARIO/hotel-service:latest

docker build -t $USUARIO/gateway:latest -f p/Dockerfile .
docker push $USUARIO/gateway:latest

docker build -t $USUARIO/frontend:latest ./hclient
docker push $USUARIO/frontend:latest
```

### 8.2 Atualizar os YAMLs

Nos arquivos da pasta `k8s/`, substitua `image: nome:latest` pelo caminho completo:

```yaml
# Antes (Minikube local):
image: flight-service:latest
imagePullPolicy: Never

# Depois (Docker Hub):
image: SEU_USUARIO/flight-service:latest
imagePullPolicy: IfNotPresent
```

Faça isso nos 4 arquivos: `flight.yaml`, `hotel.yaml`, `gateway.yaml`, `frontend.yaml`.

### 8.3 Aplicar no cluster remoto

```bash
# Configure o kubectl para apontar para o cluster remoto
# (o provedor do cluster dará as instruções específicas)

# Aplique da mesma forma
kubectl apply -f k8s/
```

---

## 9. Troubleshooting

### Pod em `CrashLoopBackOff` ou `Error`

```bash
# Veja os logs do container que está falhando
kubectl logs deployment/NOME_DO_DEPLOYMENT

# Veja os eventos do Pod (mostra o motivo da falha)
kubectl describe pod/NOME_DO_POD
```

### "ImagePullBackOff" — imagem não encontrada

Isso acontece quando o K8s não acha a imagem. Causas comuns:
- Esqueceu de rodar `eval $(minikube docker-env)` antes do `docker build`
- Nome da imagem no YAML está diferente do que foi construído

```bash
# Confirme as imagens disponíveis no Minikube
eval $(minikube docker-env)
docker images
```

### Gateway não consegue falar com A ou B

Verifique se os Services de A e B estão `Running`:
```bash
kubectl get services
kubectl get pods
```

Se os Pods estiverem OK mas o gateway reclamar de conexão, verifique as variáveis de ambiente:
```bash
kubectl exec deployment/gateway -- env | grep SERVICE_ADDRESS
```

### Frontend não carrega ou API retorna erro

Verifique se o nginx está recebendo as requisições corretamente:
```bash
kubectl logs deployment/frontend
```

Se aparecer erro de DNS (`could not be resolved`), o gateway pode ainda estar subindo. Aguarde e tente novamente.

### Resetar tudo e começar do zero

```bash
kubectl delete -f k8s/
eval $(minikube docker-env)

# Rebuildas as imagens e reaplica
docker build -t flight-service:latest ./a
docker build -t hotel-service:latest ./b
docker build -t gateway:latest -f p/Dockerfile .
docker build -t frontend:latest ./hclient

kubectl apply -f k8s/
```
