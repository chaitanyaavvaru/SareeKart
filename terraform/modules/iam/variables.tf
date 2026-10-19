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

variable "ssm_parameter_arns" {
  type        = list(string)
  description = "Explicit list of SSM Parameter ARNs that the ECS execution role is permitted to read. When empty, no SSM policy is attached."
  default     = []
}

variable "kms_key_arns" {
  type        = list(string)
  description = "Optional KMS key ARNs for decrypting SSM parameters. When provided, grants kms:Decrypt to ECS execution role."
  default     = []
}

variable "tags" {
  type        = map(string)
  description = "Resource tags"
  default = {
    Project   = "SareeKart"
    ManagedBy = "Terraform"
  }
}
