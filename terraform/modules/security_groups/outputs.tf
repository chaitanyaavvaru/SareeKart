output "alb_security_group_id" {
  description = "The security group ID of the Application Load Balancer"
  value       = aws_security_group.alb.id
}

output "ecs_security_group_id" {
  description = "The security group ID of the ECS Fargate service"
  value       = aws_security_group.ecs.id
}

output "rds_security_group_id" {
  description = "The security group ID of the RDS database"
  value       = aws_security_group.rds.id
}
