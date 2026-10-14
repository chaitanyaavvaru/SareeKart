# ============================================================================
# SareeKart Dev Environment — Input Variables
# All variables feature safe defaults with cost-incurring features disabled.
# ============================================================================

variable "aws_region" {
  type        = string
  description = "Target AWS Region"
  default     = "ap-south-1" # Mumbai region
}

variable "environment" {
  type        = string
  description = "Environment identifier"
  default     = "dev"
}

variable "vpc_cidr" {
  type        = string
  description = "VPC CIDR block"
  default     = "10.0.0.0/16"
}

variable "enable_nat_gateway" {
  type        = bool
  description = "Controls NAT Gateway provisioning. Defaults to FALSE to eliminate idle costs."
  default     = false
}

variable "create_rds" {
  type        = bool
  description = "Explicit toggle to provision RDS MySQL. Defaults to FALSE to retain zero-cost TiDB Cloud."
  default     = false
}

variable "create_ecs" {
  type        = bool
  description = "Explicit toggle to provision ECS Cluster, Task, Service, and ALB. Defaults to FALSE to guarantee zero cloud spend."
  default     = false
}

variable "assign_public_ip" {
  type        = bool
  description = "Explicit development-only toggle: whether to assign public IP to ECS tasks when NAT Gateway is disabled. Defaults to FALSE. When explicitly enabled for dev, tasks run in public subnets with strict ALB-only security group ingress."
  default     = false
}

variable "database_url" {
  type        = string
  description = "Database connection URL (defaults to existing TiDB Cloud Serverless endpoint)"
  default     = "jdbc:mysql://<tidb_host>:4000/sareekart_db?useSSL=true&allowPublicKeyRetrieval=true"
}

variable "tidb_egress_cidrs" {
  type        = list(string)
  description = "CIDR blocks permitted for ECS outbound connections to TiDB Cloud on port 4000. Defaults to ['0.0.0.0/0'] due to dynamic endpoints in TiDB Cloud Serverless."
  default     = ["0.0.0.0/0"]
}

variable "neo4j_enabled" {
  type        = bool
  description = "Whether Neo4j graph features are enabled. Defaults to FALSE (MySQL fallback active)."
  default     = false
}

variable "container_image" {
  type        = string
  description = "Container image URI for the backend task"
  default     = "sareekart-backend:prod"
}

variable "tags" {
  type        = map(string)
  description = "Common resource tags"
  default = {
    Project   = "SareeKart"
    ManagedBy = "Terraform"
  }
}
