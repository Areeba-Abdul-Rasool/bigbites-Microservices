# ─────────────────────────────────────────────────────────────
#  BigBites – AWS Infrastructure (Terraform)
#  Resources: VPC · RDS · MSK · S3 · ECR (x5) · EC2
# ─────────────────────────────────────────────────────────────

terraform {
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.0" }
  }
  # Recommended: use S3 backend
  # backend "s3" {
  #   bucket = "bigbites-tf-state"
  #   key    = "prod/terraform.tfstate"
  #   region = "ap-south-1"
  # }
}

provider "aws" {
  region = var.aws_region
}

variable "aws_region"  { default = "ap-south-1" }
variable "db_password" { sensitive = true }
variable "key_pair_name" {}

# ── VPC & Subnets ────────────────────────────────────────────
resource "aws_vpc" "bigbites" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  tags = { Name = "bigbites-vpc" }
}

resource "aws_subnet" "public_a" {
  vpc_id            = aws_vpc.bigbites.id
  cidr_block        = "10.0.1.0/24"
  availability_zone = "${var.aws_region}a"
  map_public_ip_on_launch = true
  tags = { Name = "bigbites-public-a" }
}

resource "aws_subnet" "public_b" {
  vpc_id            = aws_vpc.bigbites.id
  cidr_block        = "10.0.2.0/24"
  availability_zone = "${var.aws_region}b"
  map_public_ip_on_launch = true
  tags = { Name = "bigbites-public-b" }
}

resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.bigbites.id
  tags   = { Name = "bigbites-igw" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.bigbites.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }
}

resource "aws_route_table_association" "a" {
  subnet_id      = aws_subnet.public_a.id
  route_table_id = aws_route_table.public.id
}
resource "aws_route_table_association" "b" {
  subnet_id      = aws_subnet.public_b.id
  route_table_id = aws_route_table.public.id
}

# ── Security Groups ──────────────────────────────────────────
resource "aws_security_group" "ec2_sg" {
  name   = "bigbites-ec2-sg"
  vpc_id = aws_vpc.bigbites.id

  ingress { from_port = 22;   to_port = 22;   protocol = "tcp"; cidr_blocks = ["0.0.0.0/0"] }
  ingress { from_port = 8081; to_port = 8085; protocol = "tcp"; cidr_blocks = ["0.0.0.0/0"] }
  egress  { from_port = 0;    to_port = 0;    protocol = "-1";  cidr_blocks = ["0.0.0.0/0"] }

  tags = { Name = "bigbites-ec2-sg" }
}

resource "aws_security_group" "rds_sg" {
  name   = "bigbites-rds-sg"
  vpc_id = aws_vpc.bigbites.id

  ingress {
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2_sg.id]
  }
  egress { from_port = 0; to_port = 0; protocol = "-1"; cidr_blocks = ["0.0.0.0/0"] }

  tags = { Name = "bigbites-rds-sg" }
}

# ── RDS MySQL ────────────────────────────────────────────────
resource "aws_db_subnet_group" "bigbites" {
  name       = "bigbites-db-subnet"
  subnet_ids = [aws_subnet.public_a.id, aws_subnet.public_b.id]
}

resource "aws_db_instance" "mysql" {
  identifier             = "bigbites-mysql"
  engine                 = "mysql"
  engine_version         = "8.0"
  instance_class         = "db.t3.micro"   # upgrade to t3.small for prod
  allocated_storage      = 20
  db_name                = "BigBites"
  username               = "admin"
  password               = var.db_password
  db_subnet_group_name   = aws_db_subnet_group.bigbites.name
  vpc_security_group_ids = [aws_security_group.rds_sg.id]
  skip_final_snapshot    = true
  publicly_accessible    = false

  tags = { Name = "bigbites-rds" }
}

# ── S3 Bucket ────────────────────────────────────────────────
resource "aws_s3_bucket" "uploads" {
  bucket = "bigbites-uploads-${random_id.suffix.hex}"
  tags   = { Name = "bigbites-uploads" }
}

resource "random_id" "suffix" {
  byte_length = 4
}

resource "aws_s3_bucket_versioning" "uploads" {
  bucket = aws_s3_bucket.uploads.id
  versioning_configuration { status = "Enabled" }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "uploads" {
  bucket = aws_s3_bucket.uploads.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# ── ECR Repositories ─────────────────────────────────────────
locals {
  services = ["cart-service", "order-service", "payment-service", "product-service", "notification-service"]
}

resource "aws_ecr_repository" "services" {
  for_each             = toset(local.services)
  name                 = "bigbites-${each.key}"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration { scan_on_push = true }
}

# Lifecycle: keep only last 5 images per repo
resource "aws_ecr_lifecycle_policy" "prune" {
  for_each   = aws_ecr_repository.services
  repository = each.value.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 5 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 5
      }
      action = { type = "expire" }
    }]
  })
}

# ── MSK (Kafka) ──────────────────────────────────────────────
resource "aws_msk_cluster" "bigbites" {
  cluster_name           = "bigbites-kafka"
  kafka_version          = "3.6.0"
  number_of_broker_nodes = 2

  broker_node_group_info {
    instance_type   = "kafka.t3.small"
    client_subnets  = [aws_subnet.public_a.id, aws_subnet.public_b.id]
    storage_info {
      ebs_storage_info { volume_size = 20 }
    }
  }

  encryption_info {
    encryption_in_transit { client_broker = "TLS_PLAINTEXT" }
  }

  tags = { Name = "bigbites-msk" }
}

# ── IAM Role for EC2 (ECR + S3 access) ──────────────────────
resource "aws_iam_role" "ec2_role" {
  name = "bigbites-ec2-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecr_full" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_iam_role_policy_attachment" "s3_full" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "bigbites-ec2-profile"
  role = aws_iam_role.ec2_role.name
}

# ── EC2 Instance ─────────────────────────────────────────────
data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]
  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }
}

resource "aws_instance" "app_server" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = "t3.medium"   # 2 vCPU, 4 GB – comfortable for 5 containers
  key_name               = var.key_pair_name
  subnet_id              = aws_subnet.public_a.id
  vpc_security_group_ids = [aws_security_group.ec2_sg.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  user_data = file("${path.module}/ec2-userdata.sh")

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  tags = { Name = "bigbites-app-server" }
}

# ── Outputs ──────────────────────────────────────────────────
output "ec2_public_ip"    { value = aws_instance.app_server.public_ip }
output "rds_endpoint"     { value = aws_db_instance.mysql.endpoint }
output "s3_bucket"        { value = aws_s3_bucket.uploads.bucket }
output "ecr_repositories" { value = { for k, v in aws_ecr_repository.services : k => v.repository_url } }
output "msk_brokers"      { value = aws_msk_cluster.bigbites.bootstrap_brokers }
