# ============================================================================
# SareeKart Dev Environment — Outputs
# ============================================================================

output "vpc_id" {
  description = "The ID of the SareeKart VPC"
  value       = module.vpc.vpc_id
}

output "alb_dns_name" {
  description = "Public DNS name of the Application Load Balancer"
  value       = module.ecs.alb_dns_name
}

output "ecs_cluster_name" {
  description = "Name of the ECS cluster"
  value       = module.ecs.cluster_name
}

output "ecs_service_name" {
  description = "Name of the backend ECS service"
  value       = module.ecs.service_name
}

output "s3_bucket_arn" {
  description = "ARN of the private S3 media bucket"
  value       = module.s3.bucket_arn
}

output "rds_endpoint" {
  description = "RDS connection endpoint (displays 'rds_disabled_using_tidb_cloud' when disabled)"
  value       = module.rds.db_instance_endpoint
}

output "s3_endpoint_id" {
  description = "The ID of the S3 Gateway VPC Endpoint"
  value       = module.vpc.s3_endpoint_id
}
