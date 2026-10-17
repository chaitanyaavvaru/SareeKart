# ============================================================================
# SareeKart ECS Fargate Module — Container Orchestration & Ingress
#
# COST GUARD:
# This entire module is GUARDED by var.create_ecs (default: false).
# When false, zero AWS compute or load balancing resources are provisioned ($0).
#
# NETWORKING & EGRESS ARCHITECTURE:
# ECS Fargate tasks need outbound internet connectivity to:
#   1. Pull images from ECR / registries
#   2. Ship logs to AWS CloudWatch Logs
#   3. Retrieve database credentials / secrets from AWS SSM Parameter Store
#   4. Query external TiDB Cloud Serverless (MySQL 8 wire-compatible on port 4000)
#
# In production, this egress is routed through AWS NAT Gateways or Interface VPC
# Endpoints in private subnets (assign_public_ip = false).
# In development (₹0 cloud budget with NAT disabled), tasks can be placed in
# public subnets with assign_public_ip = true. Inbound traffic remains 100% secure
# because the ecs security group allows ingress ONLY from the ALB (port 8081).
# ============================================================================

resource "aws_ecs_cluster" "main" {
  count = var.create_ecs ? 1 : 0
  name  = "sareekart-${var.environment}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-cluster"
    Environment = var.environment
  })
}

# ── CloudWatch Logs for Container Output ─────────────────────────────────────
resource "aws_cloudwatch_log_group" "ecs" {
  count             = var.create_ecs ? 1 : 0
  name              = "/ecs/sareekart-${var.environment}-backend"
  retention_in_days = 14

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-backend-logs"
    Environment = var.environment
  })
}

# ── Application Load Balancer (Public Ingress) ───────────────────────────────
resource "aws_lb" "main" {
  count              = var.create_ecs ? 1 : 0
  name               = "sareekart-${var.environment}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [var.alb_security_group_id]
  subnets            = var.public_subnet_ids

  enable_deletion_protection = false

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-alb"
    Environment = var.environment
  })
}

# ── ALB Target Group with Stickiness for STOMP WebSockets ─────────────────────
# WEBSOCKET SCALING NOTE:
# Target group stickiness (lb_cookie) provides connection affinity for STOMP
# WebSockets (Customer Support Chat & Live Artisan Streaming) to a single task.
# CRITICAL: ALB stickiness is NOT a substitute for an external pub/sub message
# broker (e.g. Redis Pub/Sub, RabbitMQ, or ElastiCache) when scaling to
# desired_count > 1. In multi-task clusters, STOMP messages must be broadcast
# across tasks using an external message broker relay.
resource "aws_lb_target_group" "backend" {
  count                = var.create_ecs ? 1 : 0
  name                 = "sareekart-${var.environment}-tg"
  port                 = 8081
  protocol             = "HTTP"
  vpc_id               = var.vpc_id
  target_type          = "ip"
  deregistration_delay = 30

  stickiness {
    type            = "lb_cookie"
    cookie_duration = 86400
    enabled         = true
  }

  health_check {
    enabled             = true
    path                = "/actuator/health/readiness"
    port                = "8081"
    protocol            = "HTTP"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-tg"
    Environment = var.environment
  })
}

# ── HTTP Ingress Listener (Port 80) ──────────────────────────────────────────
# In production with an active ACM certificate, this listener redirects to 443.
resource "aws_lb_listener" "http" {
  count             = var.create_ecs ? 1 : 0
  load_balancer_arn = aws_lb.main[0].arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend[0].arn
  }
}

# ── ECS Task Definition (Non-Root Execution: UID 1001) ───────────────────────
resource "aws_ecs_task_definition" "backend" {
  count                    = var.create_ecs ? 1 : 0
  family                   = "sareekart-${var.environment}-backend"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = tostring(var.container_cpu)
  memory                   = tostring(var.container_memory)
  execution_role_arn       = var.execution_role_arn
  task_role_arn            = var.task_role_arn

  container_definitions = jsonencode([
    {
      name      = "sareekart-backend"
      image     = var.container_image
      essential = true

      # Enforces unprivileged execution inside the container
      user = "1001:1001"

      portMappings = [
        {
          containerPort = 8081
          hostPort      = 8081
          protocol      = "tcp"
        }
      ]

      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "JAVA_TOOL_OPTIONS", value = "-Xmx384m -Xms128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1" },
        { name = "SPRING_DATASOURCE_URL", value = var.db_url },
        { name = "SPRING_JPA_HIBERNATE_DDL_AUTO", value = "validate" },
        { name = "SPRING_FLYWAY_ENABLED", value = "true" },
        { name = "SPRING_FLYWAY_BASELINE_ON_MIGRATE", value = "false" },
        { name = "NEO4J_ENABLED", value = tostring(var.neo4j_enabled) },
        { name = "HIKARI_MAX_POOL_SIZE", value = "8" },
        { name = "HIKARI_MIN_IDLE", value = "2" }
      ]

      # Dynamic SSM Parameter Store secret injection
      secrets = concat(
        try(var.ssm_secret_arns.db_username, null) != null ? [{ name = "SPRING_DATASOURCE_USERNAME", valueFrom = var.ssm_secret_arns.db_username }] : [],
        try(var.ssm_secret_arns.db_password, null) != null ? [{ name = "SPRING_DATASOURCE_PASSWORD", valueFrom = var.ssm_secret_arns.db_password }] : [],
        try(var.ssm_secret_arns.jwt_secret, null) != null ? [{ name = "JWT_SECRET", valueFrom = var.ssm_secret_arns.jwt_secret }] : [],
        try(var.ssm_secret_arns.razorpay_key_id, null) != null ? [{ name = "RAZORPAY_KEY_ID", valueFrom = var.ssm_secret_arns.razorpay_key_id }] : [],
        try(var.ssm_secret_arns.razorpay_key_secret, null) != null ? [{ name = "RAZORPAY_KEY_SECRET", valueFrom = var.ssm_secret_arns.razorpay_key_secret }] : [],
        try(var.ssm_secret_arns.razorpay_webhook_secret, null) != null ? [{ name = "RAZORPAY_WEBHOOK_SECRET", valueFrom = var.ssm_secret_arns.razorpay_webhook_secret }] : [],
        try(var.ssm_secret_arns.whatsapp_api_token, null) != null ? [{ name = "WHATSAPP_API_TOKEN", valueFrom = var.ssm_secret_arns.whatsapp_api_token }] : [],
        try(var.ssm_secret_arns.whatsapp_phone_number_id, null) != null ? [{ name = "WHATSAPP_PHONE_NUMBER_ID", valueFrom = var.ssm_secret_arns.whatsapp_phone_number_id }] : [],
        try(var.ssm_secret_arns.whatsapp_webhook_verify_token, null) != null ? [{ name = "WHATSAPP_WEBHOOK_VERIFY_TOKEN", valueFrom = var.ssm_secret_arns.whatsapp_webhook_verify_token }] : [],
        try(var.ssm_secret_arns.whatsapp_app_secret, null) != null ? [{ name = "WHATSAPP_APP_SECRET", valueFrom = var.ssm_secret_arns.whatsapp_app_secret }] : [],
        try(var.ssm_secret_arns.openai_api_key, null) != null ? [{ name = "OPENAI_API_KEY", valueFrom = var.ssm_secret_arns.openai_api_key }] : []
      )

      healthCheck = {
        command     = ["CMD-SHELL", "curl -f http://localhost:8081/actuator/health/liveness || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.ecs[0].name
          "awslogs-region"        = "ap-south-1"
          "awslogs-stream-prefix" = "backend"
        }
      }
    }
  ])

  lifecycle {
    precondition {
      condition = !var.create_ecs || (
        try(var.ssm_secret_arns.db_username, null) != null &&
        try(var.ssm_secret_arns.db_password, null) != null &&
        try(var.ssm_secret_arns.jwt_secret, null) != null
      )
      error_message = "When create_ecs is true, mandatory SSM Parameter Store ARNs (db_username, db_password, jwt_secret) must be provided in var.ssm_secret_arns."
    }
  }

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-task-def"
    Environment = var.environment
  })
}

# ── ECS Fargate Service ──────────────────────────────────────────────────────
resource "aws_ecs_service" "backend" {
  count           = var.create_ecs ? 1 : 0
  name            = "sareekart-${var.environment}-backend-svc"
  cluster         = aws_ecs_cluster.main[0].id
  task_definition = aws_ecs_task_definition.backend[0].arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.task_subnets != null ? var.task_subnets : (var.assign_public_ip ? var.public_subnet_ids : var.private_subnet_ids)
    security_groups  = [var.ecs_security_group_id]
    assign_public_ip = var.assign_public_ip
  }

  # Health check grace period (120s): Prevents ECS from terminating and replacing tasks
  # during container startup and schema initialization before ALB readiness probes stabilize.
  # Note on ECS behavior: In AWS ECS, if an ALB target group health check fails after the grace
  # period, the ECS service scheduler will terminate and replace the task. If a shared database
  # suffers a persistent outage, tasks could enter a replacement cycle unless external mitigation
  # or circuit breakers are configured.
  health_check_grace_period_seconds = 120

  load_balancer {
    target_group_arn = aws_lb_target_group.backend[0].arn
    container_name   = "sareekart-backend"
    container_port   = 8081
  }

  deployment_controller {
    type = "ECS"
  }

  depends_on = [aws_lb_listener.http]

  tags = merge(var.tags, {
    Name        = "sareekart-${var.environment}-backend-svc"
    Environment = var.environment
  })
}
