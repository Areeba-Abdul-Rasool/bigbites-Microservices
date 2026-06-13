#!/bin/bash
# ─────────────────────────────────────────────────────────────
#  BigBites – EC2 UserData Bootstrap Script
#  Amazon Linux 2023  |  Run once on first boot
# ─────────────────────────────────────────────────────────────

set -e
exec > /var/log/bigbites-init.log 2>&1

echo "=== BigBites EC2 Bootstrap starting at $(date) ==="

# ── 1. System updates ────────────────────────────────────────
yum update -y

# ── 2. Install Docker ────────────────────────────────────────
yum install -y docker
systemctl enable docker
systemctl start docker
usermod -aG docker ec2-user

# Docker Compose v2 plugin
mkdir -p /usr/local/lib/docker/cli-plugins
curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# ── 3. Install AWS CLI v2 ────────────────────────────────────
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o /tmp/awscliv2.zip
unzip -q /tmp/awscliv2.zip -d /tmp
/tmp/aws/install

# ── 4. App directory ─────────────────────────────────────────
mkdir -p /home/ec2-user/bigbites
chown ec2-user:ec2-user /home/ec2-user/bigbites

# ── 5. Environment file (EDIT BEFORE LAUNCHING INSTANCE) ─────
# Store secrets in AWS Secrets Manager or SSM Parameter Store
# and fetch them here. Example using SSM:
#
# AWS_REGION="ap-south-1"
# DB_ENDPOINT=$(aws ssm get-parameter --name /bigbites/db/endpoint \
#   --query Parameter.Value --output text --region $AWS_REGION)
#
# For simplicity, the .env is written directly below.
# REPLACE PLACEHOLDER VALUES BEFORE USE.

cat > /home/ec2-user/bigbites/.env << 'ENV_EOF'
# ── Database (AWS RDS) ──────────────────────────────────────
SPRING_DATASOURCE_URL=jdbc:mysql://<RDS_ENDPOINT>:3306/BigBites?useSSL=true&requireSSL=false
DB_USER=admin
DB_PASS=REPLACE_ME

# ── Kafka (AWS MSK) ─────────────────────────────────────────
# Comma-separated MSK broker endpoints
KAFKA_BROKERS=<MSK_BROKER_1>:9092,<MSK_BROKER_2>:9092

# ── S3 ───────────────────────────────────────────────────────
AWS_S3_BUCKET=bigbites-uploads
AWS_REGION=ap-south-1

# ── ECR ──────────────────────────────────────────────────────
ECR_REGISTRY=<ACCOUNT_ID>.dkr.ecr.ap-south-1.amazonaws.com
IMAGE_TAG=latest
ENV_EOF

chmod 600 /home/ec2-user/bigbites/.env
chown ec2-user:ec2-user /home/ec2-user/bigbites/.env

echo "=== Bootstrap complete at $(date) ==="
