# ============================================================================
# SareeKart Development Environment — Modular Infrastructure Composition
# Assembles VPC, Security Groups, IAM, S3, ECS, and optional RDS.
#
# Baseline Strategy:
#   1. Zero Cloud Costs: NAT Gateway disabled, RDS disabled (uses TiDB Cloud).
#   2. Security First: Non-root container (UID 1001), private subnets, private S3.
#   3. Local State: Uses local backend for offline syntax/type verification.
# ============================================================================

# ── 1. Networking Foundation ─────────────────────────────────────────────────
module "vpc" {
  source = "../../modules/vpc"

  aws_region         = var.aws_region
  environment        = var.environment
  vpc_cidr           = var.vpc_cidr
  enable_nat_gateway = var.enable_nat_gateway
  tags               = var.tags
}

# ── 2. Security Groups ───────────────────────────────────────────────────────
module "security_groups" {
  source = "../../modules/security_groups"

  environment         = var.environment
  vpc_id              = module.vpc.vpc_id
  enable_neo4j_egress = var.neo4j_enabled
  enable_rds_egress   = var.create_rds
  tidb_egress_cidrs   = var.tidb_egress_cidrs
  tags                = var.tags
}

# ── 3. S3 Storage for Media & Returns ────────────────────────────────────────
module "s3" {
  source = "../../modules/s3"

  environment        = var.environment
  bucket_name_prefix = "sareekart"
  tags               = var.tags
}

# ── 4. IAM Roles & Policies ──────────────────────────────────────────────────
module "iam" {
  source = "../../modules/iam"

  environment        = var.environment
  s3_bucket_arn      = module.s3.bucket_arn
  ssm_parameter_arns = length(var.ssm_parameter_arns) > 0 ? var.ssm_parameter_arns : [for k, v in var.ssm_secret_arns : trimspace(v) if try(trimspace(v), "") != ""]
  kms_key_arns       = var.kms_key_arns
  tags               = var.tags
}

# ── 5. Optional Managed RDS MySQL (Disabled by Default) ──────────────────────
module "rds" {
  source = "../../modules/rds"

  create_rds            = var.create_rds
  environment           = var.environment
  private_subnet_ids    = module.vpc.private_subnet_ids
  rds_security_group_id = module.security_groups.rds_security_group_id
  tags                  = var.tags
}

# ── 6. ECS Fargate Cluster, Task & ALB Ingress (Disabled by Default) ──────────
module "ecs" {
  source = "../../modules/ecs"

  create_ecs            = var.create_ecs
  assign_public_ip      = var.assign_public_ip
  environment           = var.environment
  vpc_id                = module.vpc.vpc_id
  public_subnet_ids     = module.vpc.public_subnet_ids
  private_subnet_ids    = module.vpc.private_subnet_ids
  alb_security_group_id = module.security_groups.alb_security_group_id
  ecs_security_group_id = module.security_groups.ecs_security_group_id
  execution_role_arn    = module.iam.ecs_execution_role_arn
  task_role_arn         = module.iam.ecs_task_role_arn
  container_image       = var.container_image
  neo4j_enabled         = var.neo4j_enabled
  ssm_secret_arns       = var.ssm_secret_arns

  # If RDS is enabled, point to RDS endpoint; otherwise preserve TiDB Cloud
  db_url = var.create_rds ? "jdbc:mysql://${module.rds.db_instance_endpoint}/sareekart_db?createDatabaseIfNotExist=false&useSSL=true&allowPublicKeyRetrieval=false" : var.database_url

  tags = var.tags
}
