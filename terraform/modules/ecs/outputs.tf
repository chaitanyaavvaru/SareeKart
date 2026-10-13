output "cluster_id" {
  description = "The ID of the ECS cluster"
  value       = var.create_ecs ? aws_ecs_cluster.main[0].id : null
}

output "cluster_name" {
  description = "The name of the ECS cluster"
  value       = var.create_ecs ? aws_ecs_cluster.main[0].name : "ecs_cluster_disabled"
}

output "alb_dns_name" {
  description = "Public DNS name of the Application Load Balancer"
  value       = var.create_ecs ? aws_lb.main[0].dns_name : "alb_disabled"
}

output "service_name" {
  description = "Name of the backend ECS service"
  value       = var.create_ecs ? aws_ecs_service.backend[0].name : "ecs_service_disabled"
}
