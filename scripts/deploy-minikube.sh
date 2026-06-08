#!/usr/bin/env bash
# =============================================================================
#  deploy-minikube.sh — Sobe a aplicação gRPC (P, A, B + frontend) no Minikube.
#
#  Faz, em ordem:
#    1. Confere pré-requisitos (minikube, kubectl, docker)
#    2. Inicia o Minikube (se ainda não estiver rodando)
#    3. Aponta o Docker para dentro do Minikube
#    4. Constrói as 4 imagens locais
#    5. Aplica os manifests de k8s/
#    6. Espera o Job de dados e os Deployments ficarem prontos
#    7. Mostra a URL de acesso e faz um smoke test via curl
#
#  Uso (no host Linux HServ):
#    chmod +x scripts/deploy-minikube.sh
#    ./scripts/deploy-minikube.sh
#
#  Para derrubar tudo depois:  kubectl delete -f k8s/
# =============================================================================

set -euo pipefail

# Cores para legibilidade da saída
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
step() { echo -e "\n${GREEN}==> $*${NC}"; }
warn() { echo -e "${YELLOW}!  $*${NC}"; }
fail() { echo -e "${RED}xx $*${NC}"; exit 1; }

# Raiz do repositório (um nível acima de scripts/)
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# -----------------------------------------------------------------------------
# 1. Pré-requisitos
# -----------------------------------------------------------------------------
step "1/7 Conferindo pré-requisitos"
for bin in minikube kubectl docker; do
  command -v "$bin" >/dev/null 2>&1 || fail "'$bin' não encontrado no PATH. Veja docs/kubernetes.md seção 4."
  echo "  - $bin: $(command -v "$bin")"
done

# -----------------------------------------------------------------------------
# 2. Iniciar o Minikube
# -----------------------------------------------------------------------------
step "2/7 Verificando o Minikube"
if minikube status >/dev/null 2>&1; then
  echo "  Minikube já está rodando."
else
  echo "  Iniciando o Minikube (driver=docker)..."
  minikube start --driver=docker
fi

# -----------------------------------------------------------------------------
# 3. Apontar o Docker para dentro do Minikube
# -----------------------------------------------------------------------------
step "3/7 Apontando o Docker para o daemon do Minikube"
eval "$(minikube docker-env)"
echo "  Docker host agora é o do Minikube (as imagens vão direto para o cluster)."

# -----------------------------------------------------------------------------
# 4. Construir as imagens
# -----------------------------------------------------------------------------
step "4/7 Construindo as imagens locais"
echo "  - flight-service:latest (./a)"
docker build -t flight-service:latest ./a
echo "  - hotel-service:latest (./b)"
docker build -t hotel-service:latest ./b
echo "  - gateway:latest (p/Dockerfile, context=raiz)"
docker build -t gateway:latest -f p/Dockerfile .
echo "  - frontend:latest (./hclient)"
docker build -t frontend:latest ./hclient

# -----------------------------------------------------------------------------
# 5. Aplicar os manifests
# -----------------------------------------------------------------------------
step "5/7 Aplicando os manifests de k8s/"
kubectl apply -f k8s/

# -----------------------------------------------------------------------------
# 6. Esperar tudo ficar pronto
# -----------------------------------------------------------------------------
step "6/7 Aguardando o Job de dados e os Deployments"
echo "  Esperando o Job data-generator concluir..."
kubectl wait --for=condition=complete job/data-generator --timeout=180s \
  || fail "Job de dados não concluiu. Veja: kubectl logs job/data-generator"

# Força a recriação dos pods para que peguem as imagens ':latest' recém-construídas.
# Sem isto, como a tag não muda, o 'kubectl apply' acima é um no-op e os pods
# continuam rodando o código ANTIGO (gotcha clássico de :latest + imagePullPolicy: Never).
DEPLOYMENTS="flight-service hotel-service gateway frontend"
echo "  Forçando rollout para pegar as imagens novas..."
for dep in $DEPLOYMENTS; do
  kubectl rollout restart "deployment/$dep"
done

echo "  Esperando os Deployments concluírem o rollout (o gateway Java é o mais lento)..."
for dep in $DEPLOYMENTS; do
  kubectl rollout status "deployment/$dep" --timeout=240s \
    || fail "Deployment '$dep' não concluiu o rollout. Veja: kubectl get pods"
done

echo
kubectl get pods

# -----------------------------------------------------------------------------
# 7. URL de acesso + smoke test
# -----------------------------------------------------------------------------
step "7/7 Endereço de acesso e smoke test"
URL="$(minikube service frontend-service --url)"
echo "  Frontend disponível em: ${URL}"

echo "  Testando o endpoint /api/packages/search via curl..."
SEARCH="${URL}/api/packages/search?origin=BSB&destination=GIG&departureDate=2026-06-10&returnDate=2026-06-15&travelers=2&maxResults=5"
if curl -sf "$SEARCH" >/dev/null; then
  echo -e "${GREEN}  OK — a aplicação respondeu com sucesso.${NC}"
  echo "  Exemplo de chamada (para print/relatório):"
  echo "    curl \"$SEARCH\""
else
  warn "O smoke test não retornou 2xx. A app pode ainda estar subindo — tente o curl acima em alguns segundos."
fi

echo -e "\n${GREEN}Deploy concluído.${NC} Abra ${URL} no browser."
echo "Para derrubar tudo:  kubectl delete -f k8s/"
