variable "create_ecs" {
  type        = bool
  description = "Controls provisioning of ECS Cluster, Service, Task Definition, and ALB. Defaults to FALSE to guarantee ₹0 / 0 paid resources on default plan."
  default     = false
}

variable "assign_public_ip" {
  type        = bool
  description = "Whether to assign a public IP to ECS tasks. Defaults to FALSE (secure private-subnet default). In development environments without a NAT Gateway, this can be explicitly enabled to provide outbound egress to ECR, CloudWatch, and TiDB Cloud."
  default     = false
}

variable "task_subnets" {
  type        = list(string)
  description = "Explicit subnets for ECS task placement. If null, automatically selects public subnets when assign_public_ip is true, or private subnets when false."
  default     = null
}

variable "environment" {
  type        = string
  description = "Deployment environment name"
  default     = "dev"
}

variable "vpc_id" {
  type        = string
  description = "VPC ID where ECS and ALB reside"
}

variable "public_subnet_ids" {
  type        = list(string)
  description = "Public subnet IDs for ALB placement (minimum 2 AZs)"
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "Private subnet IDs for ECS Fargate task placement"
}

variable "alb_security_group_id" {
  type        = string
  description = "Security group ID for the ALB"
}

variable "ecs_security_group_id" {
  type        = string
  description = "Security group ID for ECS Fargate tasks"
}

variable "execution_role_arn" {
  type        = string
  description = "IAM Role ARN for ECS agent execution (ECR, CloudWatch, SSM)"
}

variable "task_role_arn" {
  type        = string
  description = "IAM Role ARN for application container process (S3 access)"
}

variable "container_image" {
  type        = string
  description = "Container image URI (e.g. ECR repository URL or Docker image tag)"
  default     = "sareekart-backend:prod"
}

variable "container_cpu" {
  type        = number
  description = "Fargate CPU units (256 = 0.25 vCPU)"
  default     = 256
}

variable "container_memory" {
  type        = number
  description = "Fargate memory in MiB (512 MiB bounds matching Render free-tier)"
  default     = 512
}

variable "desired_count" {
  type        = number
  description = "Desired number of ECS task instances (defaults to 1 to prevent Flyway lock contention)"
  default     = 1
}

variable "db_url" {
  type        = string
  description = "JDBC connection string (points to TiDB Cloud by default or RDS endpoint)"
  default     = "jdbc:mysql://<tidb_or_rds_host>:3306/sareekart_db?useSSL=true&allowPublicKeyRetrieval=true"
}

variable "neo4j_enabled" {
  type        = bool
  description = "Whether Neo4j Knowledge Graph is enabled. Defaults to false (graceful degradation via MySQL fallback)."
  default     = false
}

variable "ssm_secret_arns" {
  type = object({
    db_username                   = optional(string)
    db_password                   = optional(string)
    jwt_secret                    = optional(string)
    razorpay_key_id               = optional(string)
    razorpay_key_secret           = optional(string)
    razorpay_webhook_secret       = optional(string)
    whatsapp_api_token            = optional(string)
    whatsapp_phone_number_id      = optional(string)
    whatsapp_webhook_verify_token = optional(string)
    whatsapp_app_secret           = optional(string)
    openai_api_key                = optional(string)
  })
  description = "SSM Parameter Store full ARNs for container secrets injection. Mandatory secrets (db_username, db_password, jwt_secret) must be provided when create_ecs is true."
  default     = {}
}

variable "tags" {
  type        = map(string)
  description = "Resource tags"
  default = {
    Project   = "SareeKart"
    ManagedBy = "Terraform"
  }
}
