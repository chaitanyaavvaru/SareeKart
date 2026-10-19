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
      db_username        = "arn:aws:ssm:ap-south-1:123456789012:parameter/sareekart/dev/db_username"
      db_password        = "arn:aws:ssm:ap-south-1:123456789012:parameter/sareekart/dev/db_password"
      jwt_secret         = "arn:aws:ssm:ap-south-1:123456789012:parameter/sareekart/dev/jwt_secret"
      razorpay_key_id    = "arn:aws:ssm:ap-south-1:123456789012:parameter/sareekart/dev/razorpay_key_id"
      whatsapp_api_token = ""
      openai_api_key     = "   "
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

  assert {
    condition     = length(jsondecode(aws_ecs_task_definition.backend[0].container_definitions)[0].secrets) == 4
    error_message = "Container definition must contain exactly 4 secrets when 3 mandatory and 1 valid optional secret are provided (empty/whitespace optionals omitted)"
  }

  assert {
    condition     = jsondecode(aws_ecs_task_definition.backend[0].container_definitions)[0].secrets[0].name == "SPRING_DATASOURCE_USERNAME" && jsondecode(aws_ecs_task_definition.backend[0].container_definitions)[0].secrets[0].valueFrom == "arn:aws:ssm:ap-south-1:123456789012:parameter/sareekart/dev/db_username"
    error_message = "SPRING_DATASOURCE_USERNAME must map to exact db_username SSM ARN"
  }

  assert {
    condition     = jsondecode(aws_ecs_task_definition.backend[0].container_definitions)[0].secrets[1].name == "SPRING_DATASOURCE_PASSWORD" && jsondecode(aws_ecs_task_definition.backend[0].container_definitions)[0].secrets[1].valueFrom == "arn:aws:ssm:ap-south-1:123456789012:parameter/sareekart/dev/db_password"
    error_message = "SPRING_DATASOURCE_PASSWORD must map to exact db_password SSM ARN"
  }

  assert {
    condition     = jsondecode(aws_ecs_task_definition.backend[0].container_definitions)[0].secrets[2].name == "JWT_SECRET" && jsondecode(aws_ecs_task_definition.backend[0].container_definitions)[0].secrets[2].valueFrom == "arn:aws:ssm:ap-south-1:123456789012:parameter/sareekart/dev/jwt_secret"
    error_message = "JWT_SECRET must map to exact jwt_secret SSM ARN"
  }

  assert {
    condition     = jsondecode(aws_ecs_task_definition.backend[0].container_definitions)[0].secrets[3].name == "RAZORPAY_KEY_ID" && jsondecode(aws_ecs_task_definition.backend[0].container_definitions)[0].secrets[3].valueFrom == "arn:aws:ssm:ap-south-1:123456789012:parameter/sareekart/dev/razorpay_key_id"
    error_message = "Configured optional RAZORPAY_KEY_ID must map to exact razorpay_key_id SSM ARN"
  }

  assert {
    condition     = alltrue([for s in jsondecode(aws_ecs_task_definition.backend[0].container_definitions)[0].secrets : s.valueFrom != "" && s.valueFrom != null])
    error_message = "No secret mapping may have an empty or null valueFrom"
  }

  assert {
    condition     = !contains([for s in jsondecode(aws_ecs_task_definition.backend[0].container_definitions)[0].secrets : s.name], "WHATSAPP_API_TOKEN")
    error_message = "Empty optional secret WHATSAPP_API_TOKEN must be omitted from container secrets"
  }

  assert {
    condition     = !contains([for s in jsondecode(aws_ecs_task_definition.backend[0].container_definitions)[0].secrets : s.name], "OPENAI_API_KEY")
    error_message = "Whitespace-only optional secret OPENAI_API_KEY must be omitted from container secrets"
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

run "validate_ecs_fails_precondition_when_empty_string_mandatory_secret" {
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
      db_username = ""
      db_password = "arn:aws:ssm:ap-south-1:123456789012:parameter/sareekart/dev/db_password"
      jwt_secret  = "arn:aws:ssm:ap-south-1:123456789012:parameter/sareekart/dev/jwt_secret"
    }
  }

  expect_failures = [
    aws_ecs_task_definition.backend
  ]
}

run "validate_ecs_fails_precondition_when_whitespace_mandatory_secret" {
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
      db_password = "   "
      jwt_secret  = "arn:aws:ssm:ap-south-1:123456789012:parameter/sareekart/dev/jwt_secret"
    }
  }

  expect_failures = [
    aws_ecs_task_definition.backend
  ]
}

run "validate_iam_no_ssm_policy_when_arns_empty" {
  command = plan

  module {
    source = "../../modules/iam"
  }

  variables {
    environment        = "dev"
    s3_bucket_arn      = "arn:aws:s3:::sareekart-dev-media"
    ssm_parameter_arns = []
    kms_key_arns       = []
  }

  assert {
    condition     = length(aws_iam_policy.ssm_secrets_read) == 0
    error_message = "No SSM read policy should be generated when parameter ARNs are empty"
  }

  assert {
    condition     = length(aws_iam_role_policy_attachment.ecs_execution_ssm) == 0
    error_message = "No SSM policy attachment should be created when parameter ARNs are empty"
  }
}

run "validate_iam_least_privilege_ssm_policy_when_arns_configured" {
  command = plan

  module {
    source = "../../modules/iam"
  }

  variables {
    environment   = "dev"
    s3_bucket_arn = "arn:aws:s3:::sareekart-dev-media"
    ssm_parameter_arns = [
      "arn:aws:ssm:ap-south-1:123456789012:parameter/sareekart/dev/db_username",
      "arn:aws:ssm:ap-south-1:123456789012:parameter/sareekart/dev/db_password"
    ]
    kms_key_arns = [
      "arn:aws:kms:ap-south-1:123456789012:key/12345678-1234-1234-1234-123456789012"
    ]
  }

  assert {
    condition     = length(aws_iam_policy.ssm_secrets_read) == 1
    error_message = "SSM read policy should be created when parameter ARNs are configured"
  }

  assert {
    condition     = jsondecode(aws_iam_policy.ssm_secrets_read[0].policy).Statement[0].Action == ["ssm:GetParameters"]
    error_message = "SSM read policy Action must be strictly limited to ssm:GetParameters"
  }

  assert {
    condition = jsondecode(aws_iam_policy.ssm_secrets_read[0].policy).Statement[0].Resource == [
      "arn:aws:ssm:ap-south-1:123456789012:parameter/sareekart/dev/db_username",
      "arn:aws:ssm:ap-south-1:123456789012:parameter/sareekart/dev/db_password"
    ]
    error_message = "SSM read policy Resource must match exact parameter ARNs without wildcards"
  }

  assert {
    condition     = jsondecode(aws_iam_policy.ssm_secrets_read[0].policy).Statement[1].Action == ["kms:Decrypt"]
    error_message = "KMS statement Action must be strictly limited to kms:Decrypt"
  }

  assert {
    condition = jsondecode(aws_iam_policy.ssm_secrets_read[0].policy).Statement[1].Resource == [
      "arn:aws:kms:ap-south-1:123456789012:key/12345678-1234-1234-1234-123456789012"
    ]
    error_message = "KMS statement Resource must match exact key ARNs without wildcards"
  }
}
