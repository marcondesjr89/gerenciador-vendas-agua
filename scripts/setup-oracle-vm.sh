#!/usr/bin/env bash
# ===================================================================
# Script de Inicializacao e Setup Automatizado para VM Ubuntu na OCI
# Gerenciador de Vendas de Agua Mineral (Spring Boot 3 + Docker)
# ===================================================================

set -e

echo ">>> [1/5] Atualizando pacotes do sistema operacional..."
sudo apt-get update && sudo apt-get upgrade -y
sudo apt-get install -y ca-certificates curl gnupg lsb-release git iptables-persistent ufw

echo ">>> [2/5] Ajustando regras de firewall no Ubuntu (OCI / iptables / ufw)..."
# Desbloquear portas 80, 443 e 22 no iptables interno do Ubuntu na Oracle
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save || true

# Configurar ufw de forma consistente
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable || true

echo ">>> [3/5] Instalando Docker Engine e Docker Compose Plugin..."
if ! command -v docker &> /dev/null; then
    sudo install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    sudo chmod a+r /etc/apt/keyrings/docker.gpg

    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
      sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

    sudo apt-get update
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    sudo systemctl enable docker
    sudo systemctl start docker
    sudo usermod -aG docker "$USER"
    echo "Docker instalado com sucesso!"
else
    echo "Docker ja esta instalado."
fi

echo ">>> [4/5] Configurando SWAP de 4GB para estabilidade sob carga..."
if ! grep -q "swapfile" /proc/swaps; then
    sudo fallocate -l 4G /swapfile || sudo dd if=/dev/zero of=/swapfile bs=1M count=4096
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
    sudo swapon /swapfile
    echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
    echo "SWAP de 4GB ativado com sucesso!"
else
    echo "SWAP ja configurado."
fi

echo ">>> [5/5] Setup de ambiente concluido com sucesso!"
echo ">>> Para aplicar as permissoes do grupo Docker na sua sessao atual, execute: newgrp docker"
