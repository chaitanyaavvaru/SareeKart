variable "environment" {
  type        = string
  description = "Deployment environment name"
  default     = "dev"
}

variable "vpc_id" {
  type        = string
  description = "The VPC ID where security groups will be provisioned"
}

variable "enable_neo4j_egress" {
  type        = bool
  description = "Whether to allow outbound Bolt traffic (port 7687) from ECS tasks to Neo4j Aura. Disabled by default."
  default     = false
}

variable "tidb_egress_cidrs" {
  type        = list(string)
  description = "CIDR blocks permitted for outbound MySQL wire protocol (port 4000) to TiDB Cloud. Defaults to ['0.0.0.0/0'] because TiDB Cloud Serverless uses dynamic multi-tenant public endpoints without dedicated static IP ranges. Configure with specific CIDRs if TiDB Dedicated VPC peering or stable IP egress is available."
  default     = ["0.0.0.0/0"]
}

variable "enable_rds_egress" {
  type        = bool
  description = "Whether to allow outbound MySQL traffic (port 3306) from ECS tasks to RDS. Disabled by default."
  default     = false
}

variable "tags" {
  type        = map(string)
  description = "Resource tags"
  default = {
    Project   = "SareeKart"
    ManagedBy = "Terraform"
  }
}
