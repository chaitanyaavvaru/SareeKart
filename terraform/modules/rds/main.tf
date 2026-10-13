# ============================================================================
# SareeKart RDS Module — Optional Managed Relational Database
#
# ARCHITECTURAL NOTE:
# This module is DISABLED BY DEFAULT (create_rds = false) because SareeKart's
# active production database is TiDB Cloud Serverless (MySQL 8.0 wire-compatible).
#
# Migrating to AWS RDS MySQL represents an explicit future decision requiring
# data migration of the 37 canonical tables from backups/sareekart_db_latest.sql.
# Keeping it disabled prevents unexpected hourly AWS charges (~$15-$25/mo).
# ============================================================================

resource "aws_db_subnet_group" "rds" {
  count       = var.create_rds ? 1 : 0
  name_prefix = "sareekart-${var.environment}-db-subnets-"
  subnet_ids  = var.private_subnet_ids

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-db-subnet-group"
    Environment = var.environment
  })
}

resource "aws_db_parameter_group" "mysql8" {
  count       = var.create_rds ? 1 : 0
  name_prefix = "sareekart-${var.environment}-mysql8-params-"
  family      = "mysql8.0"

  parameter {
    name  = "character_set_server"
    value = "utf8mb4"
  }

  parameter {
    name  = "collation_server"
    value = "utf8mb4_unicode_ci"
  }

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-mysql8-params"
    Environment = var.environment
  })
}

resource "aws_db_instance" "main" {
  count = var.create_rds ? 1 : 0

  identifier_prefix     = "sareekart-${var.environment}-mysql-"
  engine                = "mysql"
  engine_version        = "8.0.35"
  instance_class        = var.instance_class
  allocated_storage     = var.allocated_storage
  max_allocated_storage = 50 # Prevents runaway autoscale storage costs
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = var.database_name
  username = var.admin_username
  password = var.admin_password

  db_subnet_group_name   = aws_db_subnet_group.rds[0].name
  parameter_group_name   = aws_db_parameter_group.mysql8[0].name
  vpc_security_group_ids = [var.rds_security_group_id]

  publicly_accessible = false
  multi_az            = false # Set to true for Architecture B production redundancy
  skip_final_snapshot = var.environment == "dev" ? true : false

  backup_retention_period = var.environment == "dev" ? 1 : 7
  backup_window           = "03:00-04:00"
  maintenance_window      = "Sun:04:30-Sun:05:30"

  deletion_protection = var.environment == "prod" ? true : false

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-mysql"
    Environment = var.environment
  })
}
