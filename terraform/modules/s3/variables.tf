variable "environment" {
  type        = string
  description = "Deployment environment name"
  default     = "dev"
}

variable "bucket_name_prefix" {
  type        = string
  description = "Prefix for the S3 bucket name (must be globally unique across AWS)"
  default     = "sareekart"
}

variable "tags" {
  type        = map(string)
  description = "Resource tags"
  default = {
    Project   = "SareeKart"
    ManagedBy = "Terraform"
  }
}
