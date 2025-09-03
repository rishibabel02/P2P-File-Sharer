#!/bin/bash
# Exit on any error
set -e

echo "=== Starting Simple DropFlo Setup ==="

# 1. Install All Dependencies
echo "--> Installing Java, Node.js, Nginx, PM2, Maven, Git, and Certbot..."
sudo apt-get update
sudo apt-get upgrade -y
# Install all dependencies in one go
sudo apt-get install -y openjdk-17-jdk nginx maven git certbot python3-certbot-nginx
# Install Node.js v18.x
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs
# Install PM2 globally
sudo npm install -g pm2

# 2. Clone Repository and Build Applications
# This script assumes it is run from WITHIN the cloned repository folder.
echo "--> Building Java backend..."
mvn clean package

echo "--> Building Next.js frontend..."
cd ui
npm install
npm run build
cd ..

# 3. Set up Nginx and SSL with Certbot
echo "--> Setting up Nginx and getting SSL certificate..."

# Remove the default Nginx config if it exists
if [ -f /etc/nginx/sites-enabled/default ]; then
    sudo rm /etc/nginx/sites-enabled/default
fi

# Create a temporary Nginx config file for Certbot to perform its validation
cat <<EOF | sudo tee /etc/nginx/sites-available/dropflo
server {
    listen 80;
    server_name dropflo.click www.dropflo.click;

    # A temporary root for Certbot's webroot challenge
    root /var/www/html;
    index index.html index.htm;
}
EOF

# Enable the temporary config and restart Nginx
sudo ln -sf /etc/nginx/sites-available/dropflo /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl restart nginx

# Run Certbot to get the SSL certificate.
# This will automatically modify the Nginx config to enable HTTPS.
echo "--> Running Certbot..."
sudo certbot --nginx --redirect --non-interactive --agree-tos --expand -m rishibabel02@gmail.com -d dropflo.click -d www.dropflo.click

# Overwrite the Nginx config with our FINAL version, which includes proxying
echo "--> Applying final Nginx configuration..."
cat <<EOF | sudo tee /etc/nginx/sites-available/dropflo
server {
    listen 80;
    server_name dropflo.click www.dropflo.click;
    # This redirect block is created by Certbot
    return 301 https://\$host\$request_uri;
}

server {
    listen 443 ssl http2;
    server_name dropflo.click www.dropflo.click;

    # These SSL certificate paths are created and managed by Certbot
    ssl_certificate /etc/letsencrypt/live/dropflo.click/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/dropflo.click/privkey.pem;

    # Backend API proxy
    location /api/ {
        proxy_pass http://localhost:8080/;
    }

    # Frontend proxy
    location / {
        proxy_pass http://localhost:3000;
    }
}
EOF

# Test and restart Nginx with the final configuration
sudo nginx -t && sudo systemctl restart nginx
echo "--> Nginx and SSL configured successfully."

# 4. Start Backend and Frontend with PM2
echo "--> Starting backend with PM2..."
CLASSPATH="target/P2P-File-Sharing-1.0-SNAPSHOT.jar"
# Using an absolute path for the JAR is more robust
pm2 start --name dropflo-backend "java -cp $(pwd)/$CLASSPATH p2p.App"

echo "--> Starting frontend with PM2..."
cd ui
# 'npm start' runs the optimized Next.js production server
pm2 start npm --name dropflo-frontend -- start
cd ..

# 5. Finalize PM2 Setup
echo "--> Configuring PM2 to start on boot..."
pm2 save
# This command generates another command you may need to run.
# The script will pause and wait for you to run it if necessary.
sudo env PATH=\$PATH:/usr/bin /usr/lib/node_modules/pm2/bin/pm2 startup systemd -u ubuntu --hp /home/ubuntu

echo "=== Setup Complete! ==="
echo "Your application should now be running and accessible at https://www.dropflo.click"