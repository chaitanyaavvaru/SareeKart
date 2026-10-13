output "db_instance_endpoint" {
  description = "Connection endpoint for the RDS instance"
  value       = var.create_rds ? aws_db_instance.main[0].endpoint : "rds_disabled_using_tidb_cloud"
}

output "db_instance_address" {
  description = "Hostname of the RDS instance"
  value       = var.create_rds ? aws_db_instance.main[0].address : "rds_disabled_using_tidb_cloud"
}

output "db_instance_id" {
  description = "The ID of the RDS instance"
  value       = var.create_rds ? aws_db_instance.main[0].id : null
}
