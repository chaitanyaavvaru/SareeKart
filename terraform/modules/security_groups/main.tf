# ============================================================================
# SareeKart Security Groups Module — Least-Privilege Network Isolation
# Defines strictly tiered ingress/egress: Internet -> ALB -> ECS -> RDS
# Zero public exposure for backend or database ports.
# ============================================================================

# ── 1. Application Load Balancer Security Group ──────────────────────────────
resource "aws_security_group" "alb" {
  name_prefix = "sareekart-${var.environment}-alb-sg-"
  description = "Controls public inbound traffic to the SareeKart ALB"
  vpc_id      = var.vpc_id

  ingress {
    description = "HTTP ingress (redirected to HTTPS)"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS ingress"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "Allow all outbound from ALB (restricted at container ingress)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-alb-sg"
    Environment = var.environment
  })

  lifecycle {
    create_before_destroy = true
  }
}

# ── 2. ECS Fargate Backend Security Group ────────────────────────────────────
resource "aws_security_group" "ecs" {
  name_prefix = "sareekart-${var.environment}-ecs-sg-"
  description = "Allows ingress strictly from ALB on container port 8081"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Inbound from ALB only on backend port 8081"
    from_port       = 8081
    to_port         = 8081
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    description = "Outbound to external APIs (Razorpay, Meta WhatsApp, TiDB Cloud, ECR)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-ecs-sg"
    Environment = var.environment
  })

  lifecycle {
    create_before_destroy = true
  }
}

# ── 3. RDS MySQL Security Group ──────────────────────────────────────────────
resource "aws_security_group" "rds" {
  name_prefix = "sareekart-${var.environment}-rds-sg-"
  description = "Allows MySQL ingress strictly from ECS Fargate tasks on port 3306"
  vpc_id      = var.vpc_id

  ingress {
    description     = "MySQL access strictly from ECS backend tasks"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-rds-sg"
    Environment = var.environment
  })

  lifecycle {
    create_before_destroy = true
  }
}
