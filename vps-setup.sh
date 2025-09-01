#!/bin/bash
# File: vps-setup.sh (Updated for Docker)

# DropFlo One-Time Server Setup Script

echo "=== Starting Docker-based Server Setup ==="
set -e

# 1. System Updates & Dependency Installation
echo "--> Installing system dependencies (Git, Nginx, Docker)..."
sudo apt-get update
sudo apt-get install -y git nginx docker.io docker-compose

# Add the current user to the docker group to run docker commands without sudo
# You might need to log out and log back in for this to take effect.
sudo usermod -aG docker ${USER}

# 2. Start and enable Docker service
echo "--> Starting and enabling Docker service..."
sudo systemctl start docker
sudo systemctl enable docker

# 3. Initial Application Code Checkout
echo "--> Cloning application repository..."
git clone https://github.com/rishibabel02/P2P-File-Sharer.git
cd P2P-File-Sharer

# 4. Initial Nginx Setup
# This sets up the reverse proxy to point to the Docker containers.
echo "--> Performing initial Nginx setup..."
if [ -e /etc/nginx/sites-enabled/default ]; then
    sudo rm /etc/nginx/sites-enabled/default
fi
sudo cp config/dropflo.nginx /etc/nginx/sites-available/dropflo
sudo ln -sf /etc/nginx/sites-available/dropflo /etc/nginx/sites-enabled/dropflo
sudo nginx -t
sudo systemctl restart nginx

echo "=== Server Setup Complete! ==="
echo "The server is ready. Log out and log back in to apply Docker group permissions."
echo "Then, navigate to the P2P-File-Sharer directory and run './deploy.sh' to start the application."