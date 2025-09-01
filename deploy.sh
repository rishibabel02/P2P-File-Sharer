#!/bin/bash

echo "=== Starting Docker Deployment ==="
set -e

echo "--> Pulling latest code from Git..."
git pull

echo "--> Pulling latest application images..."
docker compose pull

echo "--> Starting application containers..."
docker compose up -d

echo "=== Deployment Complete! ==="
echo "Application is running. Check status with 'docker-compose ps'"