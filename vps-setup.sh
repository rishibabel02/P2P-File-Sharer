#!/bin/bash

# Exit on any error
set -e

echo "=== Starting Simple DropFlo Setup ==="

# 1. Install Dependencies
echo "--> Installing Java, Node.js, Nginx, PM2, Maven, and Git..."
sudo apt update
sudo apt upgrade -y
sudo apt install -y openjdk-17-jdk nginx maven git certbot python3-certbot-nginx
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs
sudo npm install -g pm2

# 2. Clone Repository
# echo "--> Cloning application repository..."
# git clone https://github.com/rishibabel02/P2P-File-Sharer.git
# cd P2P-File-Sharer

# 3. Build Backend
echo "--> Building Java backend..."
mvn clean package

# 4. Build Frontend
echo "--> Building frontend..."
cd ui
npm install
npm run build
cd ..

# 5. Set up Nginx and SSL with Certbot
echo "--> Setting up Nginx and getting SSL certificate..."

if [ -e /etc/nginx/sites-enabled/default ]; then
    sudo rm /etc/nginx/sites-enabled/default
    echo "Removed default Nginx site configuration."
fi

# Create a basic Nginx config file for Certbot to use for validation
cat <<EOF | sudo tee /etc/nginx/sites-available/dropflo
server {
    listen 80;
    server_name dropflo.click www.dropflo.click; 
    return 301 https://$host$request_uri;
}
server {
    listen 443 ssl;
    server_name dropflo.click www.dropflo.click;

    ssl_certificate /etc/letsencrypt/live/dropflo.click/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/dropflo.click/privkey.pem;

    # Backend API
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_cache_bypass \$http_upgrade;
    }

    # Frontend
    location / {
        root /home/ubuntu/P2P-File-Sharer/ui/build;
        index index.html;
        try_files \$uri /index.html;
    }

    add_header X-Content-Type-Options nosniff;
    add_header X-Frame-Options SAMEORIGIN;
    add_header X-XSS-Protection "1; mode=block";
    }
EOF

# Create the symbolic link to enable the dropflo site
sudo ln -sf /etc/nginx/sites-available/dropflo /etc/nginx/sites-enabled/dropflo

sudo nginx -t
if [ $? -eq 0 ]; then
    sudo systemctl restart nginx
    echo "Nginx configured and restarted successfully."
else
    echo "Nginx configuration test failed. Please check /etc/nginx/nginx.conf and /etc/nginx/sites-available/dropflo."
    exit 1
fi

# Set up SSL with Let's Encrypt 
echo "Setting up SSL with Let's Encrypt..."
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d dropflo.click

# Start backend with PM2
echo "Starting backend with PM2..."

# Ensure all dependencies are in the classpath
CLASSPATH="target/P2P-File-Sharing-1.0-SNAPSHOT.jar:$(mvn dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=/dev/stdout -q)"
pm2 start --name dropflo-backend java -- -cp "$CLASSPATH" p2p.App

# # Start frontend with PM2
# echo "Starting frontend with PM2..."
# cd ui
# pm2 start npm --name dropflo-frontend -- start
# cd ..


# 7. Finalize PM2 Setup
pm2 save
pm2 startup

echo "=== Setup Complete! ==="
echo "Your application is now running and accessible at https://www.dropflo.click"