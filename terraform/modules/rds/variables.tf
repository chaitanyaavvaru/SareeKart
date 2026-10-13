variable "create_rds" {
  type        = bool
  description = "Explicit toggle to provision RDS MySQL. DEFAULTS TO FALSE to preserve zero-cost TiDB Cloud integration and eliminate unwanted database hourly charges."
  default     = false
}

variable "environment" {
  type        = string
  description = "Deployment environment name"
  default     = "dev"
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "Private subnet IDs for the RDS subnet group"
}

variable "rds_security_group_id" {
  type        = string
  description = "Security group ID allowing port 3306 ingress strictly from ECS"
}

variable "instance_class" {
  type        = string
  description = "RDS instance size"
  default     = "db.t4g.micro"
}

variable "allocated_storage" {
  type        = number
  description = "Storage allocated to RDS instance in GiB"
  default     = 20
}

variable "database_name" {
  type        = string
  description = "Initial MySQL database name"
  default     = "sareekart_db"
}

variable "admin_username" {
  type        = string
  description = "Master MySQL database username"
  default     = "sareekart_admin"
}

variable "admin_password" {
  type        = string
  description = "Master MySQL database password (placeholder for planning; injected via SSM/secrets in practice)"
  sensitive   = true
  default     = "CHANGE_ME_IN_SECURE_PARAM_STORE"
}

variable "tags" {
  type        = map(string)
  description = "Resource tags"
  default = {
    Project   = "SareeKart"
    ManagedBy = "Terraform"
  }
}
