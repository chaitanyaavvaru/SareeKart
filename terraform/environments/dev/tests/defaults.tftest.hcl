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
