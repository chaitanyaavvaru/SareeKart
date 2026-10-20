output "vpc_id" {
  description = "The ID of the SareeKart VPC"
  value       = aws_vpc.main.id
}

output "vpc_cidr" {
  description = "The CIDR block of the SareeKart VPC"
  value       = aws_vpc.main.cidr_block
}

output "public_subnet_ids" {
  description = "List of IDs of public subnets"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "List of IDs of private subnets"
  value       = aws_subnet.private[*].id
}

output "s3_endpoint_id" {
  description = "The ID of the S3 Gateway VPC Endpoint"
  value       = aws_vpc_endpoint.s3.id
}

output "s3_prefix_list_id" {
  description = "The prefix list ID of the S3 Gateway VPC Endpoint"
  value       = aws_vpc_endpoint.s3.prefix_list_id
}

output "nat_gateway_enabled" {
  description = "Whether NAT Gateway is enabled in the VPC"
  value       = var.enable_nat_gateway
}
