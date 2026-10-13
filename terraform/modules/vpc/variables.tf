variable "environment" {
  type        = string
  description = "Deployment environment name (e.g., dev, staging, prod)"
  default     = "dev"
}

variable "vpc_cidr" {
  type        = string
  description = "CIDR block for the SareeKart VPC"
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  type        = list(string)
  description = "List of availability zones for multi-AZ subnet distribution"
  default     = ["ap-south-1a", "ap-south-1b"]
}

variable "public_subnet_cidrs" {
  type        = list(string)
  description = "CIDR blocks for public subnets (minimum 2 AZs for ALB)"
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  type        = list(string)
  description = "CIDR blocks for private subnets (ECS tasks, RDS)"
  default     = ["10.0.10.0/24", "10.0.20.0/24"]
}

variable "enable_nat_gateway" {
  type        = bool
  description = "Whether to provision a NAT Gateway for private subnets. Defaults to false to prevent ongoing AWS charges (~$32+/mo)."
  default     = false
}

variable "tags" {
  type        = map(string)
  description = "Common resource tags"
  default = {
    Project   = "SareeKart"
    ManagedBy = "Terraform"
  }
}
