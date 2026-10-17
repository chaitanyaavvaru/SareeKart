# ============================================================================
# Terraform Native Mocked-Provider Test Suite
# Validates module graph, cost guards, and output expressions without cloud
# credentials or external emulators (Terraform 1.7+ mock_provider).
# ============================================================================

mock_provider "aws" {}

run "validate_default_graph_and_cost_guards" {
  command = plan

  assert {
    condition     = var.create_ecs == false
    error_message = "create_ecs should default to false for ₹0 baseline"
  }

  assert {
    condition     = var.create_rds == false
    error_message = "create_rds should default to false to retain TiDB Cloud"
  }

  assert {
    condition     = var.enable_nat_gateway == false
    error_message = "enable_nat_gateway should default to false to avoid idle gateway costs"
  }

  assert {
    condition     = var.neo4j_enabled == false
    error_message = "neo4j_enabled should default to false to use relational MySQL fallback"
  }

  assert {
    condition     = var.environment == "dev"
    error_message = "Default environment should be dev"
  }

  assert {
    condition     = var.aws_region == "ap-south-1"
    error_message = "Default AWS region should be ap-south-1"
  }

  assert {
    condition     = output.alb_dns_name == "alb_disabled"
    error_message = "ALB DNS name output must reflect disabled state"
  }

  assert {
    condition     = output.ecs_cluster_name == "ecs_cluster_disabled"
    error_message = "ECS cluster output must reflect disabled state"
  }

  assert {
    condition     = output.ecs_service_name == "ecs_service_disabled"
    error_message = "ECS service output must reflect disabled state"
  }

  assert {
    condition     = output.rds_endpoint == "rds_disabled_using_tidb_cloud"
    error_message = "RDS endpoint output must reflect TiDB Cloud preservation"
  }
}

run "validate_s3_gateway_endpoint" {
  command = plan

  module {
    source = "../../modules/vpc"
  }

  assert {
    condition     = aws_vpc_endpoint.s3.vpc_endpoint_type == "Gateway"
    error_message = "S3 VPC endpoint must be configured as a Gateway endpoint"
  }

  assert {
    condition     = aws_vpc_endpoint.s3.service_name == "com.amazonaws.ap-south-1.s3"
    error_message = "S3 VPC endpoint service name must match the regional S3 service"
  }
}

run "validate_ecs_secrets_wiring_when_enabled" {
  command = plan

  module {
    source = "../../modules/ecs"
  }

  variables {
    create_ecs            = true
    vpc_id                = "vpc-12345678"
    public_subnet_ids     = ["subnet-11111111", "subnet-22222222"]
    private_subnet_ids    = ["subnet-33333333", "subnet-44444444"]
    alb_security_group_id = "sg-11111111"
    ecs_security_group_id = "sg-22222222"
    execution_role_arn    = "arn:aws:iam::123456789012:role/exec"
    task_role_arn         = "arn:aws:iam::123456789012:role/task"
    ssm_secret_arns = {
      db_username = "arn:aws:ssm:ap-south-1:123456789012:parameter/sareekart/dev/db_username"
      db_password = "arn:aws:ssm:ap-south-1:123456789012:parameter/sareekart/dev/db_password"
      jwt_secret  = "arn:aws:ssm:ap-south-1:123456789012:parameter/sareekart/dev/jwt_secret"
    }
  }

  assert {
    condition     = length(aws_ecs_task_definition.backend) == 1
    error_message = "Task definition should be created when create_ecs is true and mandatory secrets are provided"
  }

  assert {
    condition     = aws_ecs_task_definition.backend[0].family == "sareekart-dev-backend"
    error_message = "Task definition family should match environment naming"
  }
}

run "validate_ecs_fails_precondition_when_missing_mandatory_secrets" {
  command = plan

  module {
    source = "../../modules/ecs"
  }

  variables {
    create_ecs            = true
    vpc_id                = "vpc-12345678"
    public_subnet_ids     = ["subnet-11111111", "subnet-22222222"]
    private_subnet_ids    = ["subnet-33333333", "subnet-44444444"]
    alb_security_group_id = "sg-11111111"
    ecs_security_group_id = "sg-22222222"
    execution_role_arn    = "arn:aws:iam::123456789012:role/exec"
    task_role_arn         = "arn:aws:iam::123456789012:role/task"
    ssm_secret_arns       = {}
  }

  expect_failures = [
    aws_ecs_task_definition.backend
  ]
}
