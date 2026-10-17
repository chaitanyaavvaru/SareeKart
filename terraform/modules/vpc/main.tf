# ============================================================================
# SareeKart VPC Module — Network Foundation
# Multi-AZ topology with public and private subnets.
# NAT Gateway is disabled by default to eliminate accidental idle charges.
# ============================================================================

resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-vpc"
    Environment = var.environment
  })
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-igw"
    Environment = var.environment
  })
}

# ── Public Subnets (ALB Ingress) ─────────────────────────────────────────────
resource "aws_subnet" "public" {
  count             = length(var.public_subnet_cidrs)
  vpc_id            = aws_vpc.main.id
  cidr_block        = var.public_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]
  # Public ALBs assign their own ENIs; setting false prevents unintended public IP auto-assignment
  map_public_ip_on_launch = false

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-public-${var.availability_zones[count.index]}"
    Environment = var.environment
    Tier        = "Public"
  })
}

# ── Private Subnets (ECS Fargate Tasks, RDS MySQL) ───────────────────────────
resource "aws_subnet" "private" {
  count                   = length(var.private_subnet_cidrs)
  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.private_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = false

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-private-${var.availability_zones[count.index]}"
    Environment = var.environment
    Tier        = "Private"
  })
}

# ── Public Route Table & Associations ─────────────────────────────────────────
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-public-rt"
    Environment = var.environment
  })
}

resource "aws_route_table_association" "public" {
  count          = length(aws_subnet.public)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# ── Optional NAT Gateway & Private Route Table ───────────────────────────────
# Explicitly conditional: only created when var.enable_nat_gateway = true.
resource "aws_eip" "nat" {
  count  = var.enable_nat_gateway ? 1 : 0
  domain = "vpc"

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-nat-eip"
    Environment = var.environment
  })
}

resource "aws_nat_gateway" "main" {
  count         = var.enable_nat_gateway ? 1 : 0
  allocation_id = aws_eip.nat[0].id
  subnet_id     = aws_subnet.public[0].id

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-nat-gw"
    Environment = var.environment
  })

  depends_on = [aws_internet_gateway.main]
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  dynamic "route" {
    for_each = var.enable_nat_gateway ? [1] : []
    content {
      cidr_block     = "0.0.0.0/0"
      nat_gateway_id = aws_nat_gateway.main[0].id
    }
  }

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-private-rt"
    Environment = var.environment
  })
}

resource "aws_route_table_association" "private" {
  count          = length(aws_subnet.private)
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}

# ── Amazon S3 Gateway Endpoint (Zero Cost Private S3 Access) ─────────────────
# Routes S3 traffic privately through AWS network without traversing the internet.
# S3 Gateway Endpoints carry no hourly charge and no data processing fees ($0.00).
resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.main.id
  service_name      = "com.amazonaws.${var.aws_region}.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids = concat(
    [aws_route_table.public.id],
    [aws_route_table.private.id]
  )

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-s3-endpoint"
    Environment = var.environment
  })
}
