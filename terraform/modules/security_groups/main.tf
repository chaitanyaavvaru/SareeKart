# ============================================================================
# SareeKart Security Groups Module — Least-Privilege Network Isolation
# Defines strictly tiered ingress/egress: Internet -> ALB -> ECS -> RDS
# Zero public exposure for backend or database ports.
#
# Consistent Rule-Management Model:
# Uses dedicated aws_vpc_security_group_ingress_rule and
# aws_vpc_security_group_egress_rule resources exclusively, avoiding the
# antipattern of mixing inline rules with standalone rule resources.
# ============================================================================

# ── 1. Application Load Balancer Security Group ──────────────────────────────
resource "aws_security_group" "alb" {
  name_prefix = "sareekart-${var.environment}-alb-sg-"
  description = "Controls public inbound traffic to the SareeKart ALB"
  vpc_id      = var.vpc_id

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-alb-sg"
    Environment = var.environment
  })

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_vpc_security_group_ingress_rule" "alb_http" {
  security_group_id = aws_security_group.alb.id
  description       = "Allow inbound HTTP from internet on port 80 (redirected to HTTPS)"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
  cidr_ipv4         = "0.0.0.0/0"

  tags = merge(var.tags, {
    Name = "sareekart-${var.environment}-alb-ingress-http"
  })
}

resource "aws_vpc_security_group_ingress_rule" "alb_https" {
  security_group_id = aws_security_group.alb.id
  description       = "Allow inbound HTTPS from internet on port 443"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
  cidr_ipv4         = "0.0.0.0/0"

  tags = merge(var.tags, {
    Name = "sareekart-${var.environment}-alb-ingress-https"
  })
}

resource "aws_vpc_security_group_egress_rule" "alb_to_ecs" {
  security_group_id            = aws_security_group.alb.id
  description                  = "Allow outbound traffic strictly to backend ECS tasks on port 8081"
  from_port                    = 8081
  to_port                      = 8081
  ip_protocol                  = "tcp"
  referenced_security_group_id = aws_security_group.ecs.id

  tags = merge(var.tags, {
    Name = "sareekart-${var.environment}-alb-egress-to-ecs"
  })
}

# ── 2. ECS Fargate Backend Security Group ────────────────────────────────────
resource "aws_security_group" "ecs" {
  name_prefix = "sareekart-${var.environment}-ecs-sg-"
  description = "Allows ingress strictly from ALB on container port 8081"
  vpc_id      = var.vpc_id

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-ecs-sg"
    Environment = var.environment
  })

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_vpc_security_group_ingress_rule" "ecs_from_alb" {
  security_group_id            = aws_security_group.ecs.id
  description                  = "Inbound strictly from ALB on backend port 8081"
  from_port                    = 8081
  to_port                      = 8081
  ip_protocol                  = "tcp"
  referenced_security_group_id = aws_security_group.alb.id

  tags = merge(var.tags, {
    Name = "sareekart-${var.environment}-ecs-ingress-from-alb"
  })
}

# ── ECS Egress Rules (Least-privilege operational destinations) ──────────────

# 1. Outbound HTTPS (Port 443)
# Residual Risk Note: Required for AWS APIs (ECR pull, CloudWatch logs, SSM secrets)
# when NAT/PrivateLink endpoints are disabled for ₹0 cost, as well as external
# payment (Razorpay) and messaging (Meta WhatsApp Cloud API) SaaS gateways.
resource "aws_vpc_security_group_egress_rule" "ecs_https" {
  security_group_id = aws_security_group.ecs.id
  description       = "Outbound HTTPS (port 443) to AWS APIs, Razorpay, and Meta WhatsApp"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
  cidr_ipv4         = "0.0.0.0/0"

  tags = merge(var.tags, {
    Name = "sareekart-${var.environment}-ecs-egress-https"
  })
}

# 2. Outbound MySQL wire protocol to TiDB Cloud (Port 4000)
# Residual Risk Note: TiDB Cloud Serverless clusters utilize dynamic public endpoints
# without dedicated static IP allocations. Defaults to 0.0.0.0/0 but configurable via
# var.tidb_egress_cidrs for dedicated VPC peering / private proxy architectures.
resource "aws_vpc_security_group_egress_rule" "ecs_tidb" {
  for_each          = toset(var.tidb_egress_cidrs)
  security_group_id = aws_security_group.ecs.id
  description       = "Outbound MySQL wire protocol (port 4000) to TiDB Cloud Serverless"
  from_port         = 4000
  to_port           = 4000
  ip_protocol       = "tcp"
  cidr_ipv4         = each.value

  tags = merge(var.tags, {
    Name = "sareekart-${var.environment}-ecs-egress-tidb"
  })
}

# 3. Optional Outbound Bolt protocol to Neo4j (Port 7687)
# Disabled by default; provisioned only when neo4j integration is explicitly enabled.
resource "aws_vpc_security_group_egress_rule" "ecs_neo4j" {
  count             = var.enable_neo4j_egress ? 1 : 0
  security_group_id = aws_security_group.ecs.id
  description       = "Outbound Bolt protocol (port 7687) to Neo4j Aura (conditional)"
  from_port         = 7687
  to_port           = 7687
  ip_protocol       = "tcp"
  cidr_ipv4         = "0.0.0.0/0"

  tags = merge(var.tags, {
    Name = "sareekart-${var.environment}-ecs-egress-neo4j"
  })
}

# 4. Optional Outbound MySQL to RDS (Port 3306)
# Conditional on RDS being explicitly enabled; connects strictly to RDS SG.
resource "aws_vpc_security_group_egress_rule" "ecs_to_rds" {
  count                        = var.enable_rds_egress ? 1 : 0
  security_group_id            = aws_security_group.ecs.id
  description                  = "Outbound MySQL strictly to RDS on port 3306 (conditional)"
  from_port                    = 3306
  to_port                      = 3306
  ip_protocol                  = "tcp"
  referenced_security_group_id = aws_security_group.rds.id

  tags = merge(var.tags, {
    Name = "sareekart-${var.environment}-ecs-egress-to-rds"
  })
}

# ── 3. RDS MySQL Security Group ──────────────────────────────────────────────
resource "aws_security_group" "rds" {
  name_prefix = "sareekart-${var.environment}-rds-sg-"
  description = "Allows MySQL ingress strictly from ECS Fargate tasks on port 3306"
  vpc_id      = var.vpc_id

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-rds-sg"
    Environment = var.environment
  })

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_vpc_security_group_ingress_rule" "rds_from_ecs" {
  security_group_id            = aws_security_group.rds.id
  description                  = "MySQL access strictly from ECS backend tasks on port 3306"
  from_port                    = 3306
  to_port                      = 3306
  ip_protocol                  = "tcp"
  referenced_security_group_id = aws_security_group.ecs.id

  tags = merge(var.tags, {
    Name = "sareekart-${var.environment}-rds-ingress-from-ecs"
  })
}
