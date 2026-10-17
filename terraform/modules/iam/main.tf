# ============================================================================
# SareeKart IAM Module — Least-Privilege Execution & Task Roles
# Distinguishes between infrastructure orchestration (execution role)
# and application business logic (task role).
# ============================================================================

# ── 1. ECS Task Execution Role (Container Lifecycle, Logs, Secrets) ──────────
resource "aws_iam_role" "ecs_execution" {
  name = "sareekart-${var.environment}-ecs-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-ecs-execution-role"
    Environment = var.environment
  })
}

# Standard AWS managed policy for ECR image pull and CloudWatch logging
resource "aws_iam_role_policy_attachment" "ecs_execution_standard" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Least-privilege policy allowing ECS agent to fetch application secrets from SSM
resource "aws_iam_policy" "ssm_secrets_read" {
  name        = "sareekart-${var.environment}-ssm-secrets-read"
  description = "Allows ECS agent to read SareeKart configuration secrets from SSM"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = concat(
      [
        {
          Effect = "Allow"
          Action = [
            "ssm:GetParameters",
            "ssm:GetParameter",
            "ssm:GetParametersByPath"
          ]
          Resource = length(var.ssm_parameter_arns) > 0 ? var.ssm_parameter_arns : ["arn:aws:ssm:*:*:parameter/sareekart/${var.environment}/*"]
        }
      ],
      length(var.kms_key_arns) > 0 ? [
        {
          Effect = "Allow"
          Action = [
            "kms:Decrypt"
          ]
          Resource = var.kms_key_arns
        }
      ] : []
    )
  })
}

resource "aws_iam_role_policy_attachment" "ecs_execution_ssm" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = aws_iam_policy.ssm_secrets_read.arn
}

# ── 2. ECS Task Role (Runtime Application Process Permissions) ───────────────
resource "aws_iam_role" "ecs_task" {
  name = "sareekart-${var.environment}-ecs-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-ecs-task-role"
    Environment = var.environment
  })
}

# Policy allowing the Spring Boot container to store and read media uploads from S3
resource "aws_iam_policy" "s3_media_access" {
  name        = "sareekart-${var.environment}-s3-media-access"
  description = "Allows Spring Boot to write and read saree media and return photos"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject"
        ]
        Resource = "${var.s3_bucket_arn}/*"
      },
      {
        Effect = "Allow"
        Action = [
          "s3:ListBucket"
        ]
        Resource = var.s3_bucket_arn
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_s3" {
  role       = aws_iam_role.ecs_task.name
  policy_arn = aws_iam_policy.s3_media_access.arn
}
