# ============================================================================
# SareeKart Terraform — Provider & Version Constraints
# Pinned versions adhering to enterprise predictability.
# Local backend is selected for initial offline validation.
# ============================================================================

terraform {
  required_version = ">= 1.5.0, < 2.0.0"

  required_providers {
    # Deliberate provider compatibility and reproducibility choice: The AWS
    # provider is constrained to the 5.40.x release line (~> 5.40.0) to ensure
    # deterministic resource schemas and avoid unexpected upstream behavioral changes
    # across team environments. Any provider version upgrade should be evaluated
    # as an isolated, dedicated change.
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.40.0"
    }
  }

  # Local state backend for safe, offline development validation.
  # Remote S3 state + DynamoDB state locking is documented for cloud deployment.
  backend "local" {
    path = "terraform.tfstate"
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "SareeKart"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}
