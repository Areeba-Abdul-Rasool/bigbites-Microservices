# BigBites – DevOps Setup Guide

Complete step-by-step guide to containerise, provision AWS infrastructure,
and set up CI/CD for the BigBites microservices project.

---

## Project Structure

```
bigbites/
├── cart-service/
├── order-service/
├── payment-service/
├── product-service/
├── notification-service/
└── bigbites-devops/               ← this folder
    ├── cart-service/Dockerfile
    ├── order-service/Dockerfile
    ├── payment-service/Dockerfile
    ├── product-service/Dockerfile
    ├── notification-service/Dockerfile
    ├── docker-compose.yml          ← local dev
    ├── docker-compose.prod.yml     ← EC2 production
    ├── application-prod.yml        ← shared prod config
    ├── ec2-userdata.sh
    ├── init.sql
    ├── terraform/main.tf
    └── .github/workflows/ci-cd.yml
```

---

## Step 1 – Local Development

### Prerequisites
- Docker Desktop installed
- Java 21 + Maven

### Run everything locally
```bash
cd bigbites-devops
docker compose up --build
```

This starts: MySQL · Zookeeper · Kafka · all 5 Spring Boot services

| Service             | Port |
|---------------------|------|
| order-service       | 8081 |
| product-service     | 8082 |
| payment-service     | 8083 |
| notification-service| 8084 |
| cart-service        | 8085 |

### Verify Kafka topics were auto-created
```bash
docker exec bigbites-kafka \
  kafka-topics --bootstrap-server localhost:9092 --list
```

---

## Step 2 – Add `application-prod.yml` to each service

Copy `application-prod.yml` into every service under:
```
src/main/resources/application-prod.yml
```

This profile reads everything from environment variables — no hardcoded secrets.

---

## Step 3 – Add Actuator to every service (required for health checks)

Add to each `pom.xml`:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Add to each `application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
```

---

## Step 4 – AWS Infrastructure with Terraform

### 4.1 Prerequisites
```bash
# Install Terraform
brew install terraform       # macOS
# or download from https://terraform.io

# Install AWS CLI & configure
aws configure
# Enter: Access Key ID, Secret Key, Region (ap-south-1), output (json)
```

### 4.2 Provision everything
```bash
cd bigbites-devops/terraform

terraform init
terraform plan -var="db_password=YourSecurePass123!" \
               -var="key_pair_name=your-ec2-keypair"
terraform apply -var="db_password=YourSecurePass123!" \
                -var="key_pair_name=your-ec2-keypair"
```

Terraform creates:
- **VPC** with 2 public subnets
- **RDS MySQL 8.0** (db.t3.micro, 20 GB)
- **MSK Kafka** (2 brokers, kafka.t3.small)
- **S3 bucket** for uploads (versioned + encrypted)
- **5 ECR repositories** (one per service)
- **EC2 t3.medium** with Docker pre-installed via UserData
- **IAM role** giving EC2 access to ECR and S3

### 4.3 Note the outputs
```
ec2_public_ip    = "X.X.X.X"
rds_endpoint     = "bigbites-mysql.xxxx.ap-south-1.rds.amazonaws.com:3306"
msk_brokers      = "b-1.bigbites-kafka.xxxx:9092,b-2.bigbites-kafka.xxxx:9092"
s3_bucket        = "bigbites-uploads-abcd1234"
ecr_repositories = { "cart-service" = "123456789.dkr.ecr..." ... }
```

---

## Step 5 – Configure the EC2 `.env` file

SSH into your EC2 instance:
```bash
ssh -i your-key.pem ec2-user@<EC2_PUBLIC_IP>
```

Edit the env file with Terraform's output values:
```bash
nano /home/ec2-user/bigbites/.env
```

```env
SPRING_DATASOURCE_URL=jdbc:mysql://<RDS_ENDPOINT>/BigBites?useSSL=true&requireSSL=false
DB_USER=admin
DB_PASS=YourSecurePass123!

KAFKA_BROKERS=<MSK_BROKER_1>:9092,<MSK_BROKER_2>:9092

AWS_S3_BUCKET=bigbites-uploads-abcd1234
AWS_REGION=ap-south-1

ECR_REGISTRY=<ACCOUNT_ID>.dkr.ecr.ap-south-1.amazonaws.com
IMAGE_TAG=latest
```

---

## Step 6 – GitHub Actions CI/CD

### 6.1 Add GitHub Secrets

Go to your repo → Settings → Secrets and variables → Actions

| Secret Name          | Value                                      |
|---------------------|--------------------------------------------|
| `AWS_ACCESS_KEY_ID`  | IAM user access key                       |
| `AWS_SECRET_ACCESS_KEY` | IAM user secret key                   |
| `AWS_ACCOUNT_ID`     | Your 12-digit AWS account ID              |
| `EC2_HOST`           | EC2 public IP from terraform output       |
| `EC2_SSH_KEY`        | Contents of your `.pem` private key file  |

### 6.2 Pipeline Stages

```
push to main
    │
    ▼
[changes]          Detect which services changed
    │
    ▼
[build-and-test]   mvn clean verify for each changed service
    │
    ▼
[docker-push]      Build Docker image → push to ECR
    │
    ▼
[deploy]           SSH to EC2 → docker compose pull + up
```

- **PRs**: only runs build-and-test (no deploy)
- **Merge to main**: full pipeline including deploy
- **Path filtering**: only rebuilds services that have code changes

---

## Step 7 – Manual First Deploy

On first deploy, push any change to trigger the pipeline, or run manually:

```bash
# On EC2 – first time pull and start
cd /home/ec2-user/bigbites
aws ecr get-login-password --region ap-south-1 \
  | docker login --username AWS --password-stdin \
    <ACCOUNT_ID>.dkr.ecr.ap-south-1.amazonaws.com

source .env
docker compose -f docker-compose.prod.yml up -d
```

---

## Step 8 – Verify Deployment

```bash
# Check all containers are running
docker ps

# Check logs
docker logs bigbites-order --tail=50
docker logs bigbites-notification --tail=50

# Test endpoints
curl http://<EC2_IP>:8082/products
curl http://<EC2_IP>:8081/orders
```

---

## Architecture Overview

```
GitHub Push
    │
    ▼
GitHub Actions CI/CD
    │── mvn test
    │── docker build
    │── ECR push
    └── EC2 SSH deploy
              │
              ▼
         EC2 (t3.medium)
              │
    ┌─────────┴─────────┐
    │   Docker Network   │
    │                    │
    │  cart     :8085   │
    │  order    :8081   │──────► RDS MySQL
    │  product  :8082   │        (managed)
    │  payment  :8083   │
    │  notif    :8084   │──────► MSK Kafka
    └────────────────────┘        (managed)
              │
              └──────────────► S3 Bucket
                                (uploads)
```

---

## Kafka Topics Reference

| Topic                    | Producer         | Consumer             |
|--------------------------|------------------|----------------------|
| `order.placed`           | order-service    | notification-service |
| `order.status.updated`   | order-service    | notification-service |
| `order.cancelled`        | order-service    | notification-service |
| `payment.success`        | payment-service  | notification-service |
| `payment.failed`         | payment-service  | notification-service |
| `payment.refunded`       | payment-service  | notification-service |
| `product.stock.deducted` | product-service  | notification-service |
| `product.low.stock`      | product-service  | notification-service |

---

## Troubleshooting

**Services can't connect to Kafka:**
```bash
# Check MSK security group allows port 9092 from EC2 SG
```

**RDS connection refused:**
```bash
# Ensure RDS SG inbound rule allows 3306 from EC2 SG (not 0.0.0.0/0)
```

**ECR push fails in CI:**
```bash
# Verify IAM user has AmazonEC2ContainerRegistryFullAccess policy
```

**Container exits immediately:**
```bash
docker logs bigbites-<service>
# Usually a missing env var or DB connection issue
```
