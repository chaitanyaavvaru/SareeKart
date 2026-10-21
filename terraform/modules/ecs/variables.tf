variable "create_ecs" {
  type        = bool
  description = "Controls provisioning of ECS Cluster, Service, Task Definition, and ALB. Defaults to FALSE to guarantee ₹0 / 0 paid resources on default plan."
  default     = false
}

variable "task_subnet_tier" {
  type        = string
  description = "Network subnet tier for ECS task placement. Valid values: 'private' (default secure tier) or 'public' (development smoke-testing tier). When 'public', assign_public_ip must be explicitly true. When 'private', assign_public_ip must be false. Public subnet placement incurs AWS public IPv4 address charges ($0.005/hr ~ $3.65/mo per continuously assigned address), plus Fargate compute and ALB charges. For Option B (private), pricing for VPC Interface Endpoints and NAT Gateway must be recalculated for ap-south-1."
  default     = "private"

  validation {
    condition     = contains(["private", "public"], lower(var.task_subnet_tier))
    error_message = "task_subnet_tier must be either 'private' or 'public'."
  }
}

variable "assign_public_ip" {
  type        = bool
  description = "Whether to assign a public IP to ECS tasks. Defaults to FALSE (secure private-subnet default). In development environments without a NAT Gateway, this can be explicitly enabled alongside task_subnet_tier = 'public' for development smoke testing (Option A), which incurs AWS public IPv4 address charges of $0.005/hr (~$3.65/mo per continuously assigned address) in addition to Fargate and ALB compute costs."
  default     = false
}

variable "task_subnets" {
  type        = list(string)
  description = "Explicit subnets for ECS task placement. If null, automatically selects public_subnet_ids when task_subnet_tier is 'public', or private_subnet_ids when 'private'. If provided when create_ecs is true, must be a non-empty list whose elements reside strictly within the verified subnet IDs of the selected tier (public_subnet_ids for 'public' or private_subnet_ids for 'private'); arbitrary subnet IDs and empty lists are rejected."
  default     = null
}

variable "enable_nat_gateway" {
  type        = bool
  description = "Whether a NAT Gateway is enabled in the VPC for private task outbound egress. When create_ecs is true and task_subnet_tier is 'private', an outbound route (enable_nat_gateway = true) is strictly required to pull container images, retrieve SSM secrets, query TiDB Cloud (port 4000), and access external SaaS APIs (port 443)."
  default     = false
}

variable "s3_bucket_name" {
  type        = string
  description = "Name of the S3 media bucket for persistent file storage"
  default     = ""
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
  description = "SSM Parameter Store full ARNs for container secrets injection. Mandatory secrets (db_username, db_password, jwt_secret) must be provided with non-empty ARNs when create_ecs is true."
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
