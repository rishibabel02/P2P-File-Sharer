#!/bin/bash
# File: deploy.sh (Updated for Docker)

# A script to deploy the latest version of the DropFlo application using Docker Compose

echo "=== Starting Docker Deployment ==="
set -e

# 1. Get the latest code
echo "--> Pulling latest code from Git..."
git pull

# 2. Build and run the containers in detached mode
echo "--> Building and starting application containers..."
docker-compose up --build -d

echo "=== Deployment Complete! ==="
echo "Application is running. Check status with 'docker-compose ps'"