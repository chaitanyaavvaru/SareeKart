variable "environment" {
  type        = string
  description = "Deployment environment name"
  default     = "dev"
}

variable "s3_bucket_arn" {
  type        = string
  description = "ARN of the SareeKart media S3 bucket for task role permissions"
  default     = "*"
}

variable "tags" {
  type        = map(string)
  description = "Resource tags"
  default = {
    Project   = "SareeKart"
    ManagedBy = "Terraform"
  }
}
