# SareeKart Infrastructure as Code (Terraform)

This directory contains the modular Infrastructure as Code (IaC) definitions for SareeKart's AWS deployment architecture.

---

## 1. Directory Structure & Modular Design

```
terraform/
├── README.md                           # Architecture and usage documentation
├── environments/
│   └── dev/                            # Development environment composition
│       ├── main.tf                     # Module assembly and wiring
│       ├── variables.tf                # Parameter declarations with cost guards
│       ├── outputs.tf                  # Exported infrastructure endpoints
│       ├── versions.tf                 # Pinned provider (~> 5.40.0) & local backend
│       └── terraform.tfvars.example    # Variable template with placeholders
└── modules/
    ├── vpc/                            # Multi-AZ VPC, subnets, route tables, conditional NAT
    ├── security_groups/                # Least-privilege ingress/egress rules (ALB -> ECS -> RDS)
    ├── iam/                            # Separate ECS execution role and task role
    ├── s3/                             # Private media storage bucket, encryption, public block
    ├── rds/                            # Optional MySQL 8.0 instance (disabled by default)
    └── ecs/                            # ECS Fargate cluster, task def (UID 1001), ALB ingress
```

---

## 2. Key Architectural Decisions

### A. Database Strategy: TiDB Cloud Preserved by Default
SareeKart’s active production database is **TiDB Cloud Serverless** (MySQL 8.0 wire-compatible), hosting 37 tables managed via 17 Flyway migrations.
- In `environments/dev/variables.tf`, `create_rds` defaults to **`false`**.
- The `modules/rds` module is treated as an **optional future migration target**. Keeping it disabled eliminates recurring database charges (~$15–$25/mo) and avoids data migration disruption.

### B. Security-First Container Execution (Non-Root)
- The ECS Task Definition in `modules/ecs/main.tf` explicitly specifies:
  ```json
  "user": "1001:1001"
  ```
- This directly aligns with our container hardening in Step 3.2, ensuring the Spring Boot process runs as unprivileged user `appuser` (UID 1001) inside AWS Fargate.

### C. Persistent Storage & Non-Destructive Media Lifecycle
- `modules/s3` provisions a private S3 bucket with AES256 server-side encryption and all public access blocked.
- **Application Note**: The Spring Boot application currently writes photo uploads to `uploads/` on the local disk. In ECS Fargate, task local disk is ephemeral. To achieve true cloud persistence, an S3 SDK client adapter must be integrated into `PhotoUploadController` and `ReturnController` before relying on S3 in production.
- **Non-Destructive Lifecycle**: Rather than automatically expiring or deleting customer return defect photos, the bucket lifecycle configuration transitions objects under `return-photos/` to `STANDARD_IA` after 90 days, preserving critical evidence and historical object versions indefinitely while lowering storage costs. Incomplete multipart uploads are aborted after 7 days.

### D. Zero-Spend Baseline Cost Guards
Every cost-incurring cloud component is guarded by an explicit toggle that defaults to **`false`**:

| Resource | Default State | Rationale | Cost If Enabled |
|---|---|---|---|
| **ALB & ECS Service** | Disabled (`create_ecs = false`) | Default plan provisions ₹0 / 0 compute resources | ~$25.70/mo ($16.20 ALB + $9.50 Fargate) |
| **NAT Gateway** | Disabled (`enable_nat_gateway = false`) | Avoids idle gateway charge | ~$32.85/mo per gateway |
| **RDS MySQL** | Disabled (`create_rds = false`) | Retains existing zero-cost TiDB Cloud | ~$15.00–$25.00/mo (db.t4g.micro) |
| **ECS Desired Count** | 1 (`desired_count = 1`) | Prevents Flyway migration locking contention | N/A |
| **Neo4j** | Disabled (`neo4j_enabled = false`) | Uses built-in MySQL relational fallback (`fallbackHydrate`) | N/A |

### E. ECS Egress & NAT-Less Dev Networking
ECS Fargate tasks need outbound internet connectivity to pull container images from ECR, stream logs to AWS CloudWatch Logs, retrieve configuration secrets from AWS SSM Parameter Store, and connect to external TiDB Cloud Serverless (`<tidb_host>:4000`).
- **Production Mode (`enable_nat_gateway = true`, `assign_public_ip = false`)**: Tasks reside in private subnets and route outbound traffic through a managed NAT Gateway or dedicated AWS VPC Interface Endpoints (Privatelink for ECR, CloudWatch, SSM). This is the default network configuration.
- **Development / Zero-Cost Mode (Explicit Opt-In: `enable_nat_gateway = false`, `assign_public_ip = true`)**: By default, `assign_public_ip` is `false`. When explicitly set to `true` for dev environments without a NAT Gateway, tasks are placed in public subnets with an assigned public IPv4 address to reach ECR, CloudWatch, and TiDB Cloud directly. Inbound traffic remains strictly isolated because the `aws_security_group.ecs` security group permits ingress **only from the ALB security group on port 8081**, rejecting all direct inbound connections from the internet.

### F. WebSocket Session Affinity vs. Distributed Pub/Sub Relay
- In `modules/ecs/main.tf`, the ALB target group configures cookie-based stickiness (`type = "lb_cookie"` with 86400s duration) for STOMP WebSockets (live artisan streaming and support chat).
- **Architectural Distinction**: ALB cookie stickiness provides session affinity to route an individual client back to the same container instance. However, stickiness is **not a substitute for a clustered pub/sub message broker** (e.g., Redis Pub/Sub, RabbitMQ, or AWS ElastiCache) when horizontally scaling beyond 1 task (`desired_count > 1`). For multi-instance broadcast, a shared message broker relay must be configured in `WebSocketConfig.java`.

---

## 3. Local Offline Validation Workflow

To validate Terraform formatting and syntax **without downloading cloud providers or making AWS API calls**, use the official pinned Docker image:

```bash
# 1. Formatting check (all files)
docker run --rm -v $(pwd):/workspace -w /workspace hashicorp/terraform:1.7.5 fmt -check -recursive

# 2. Offline initialization without backend
docker run --rm -v $(pwd):/workspace -w /workspace/terraform/environments/dev hashicorp/terraform:1.7.5 init -backend=false

# 3. Offline syntax and configuration validation
docker run --rm -v $(pwd):/workspace -w /workspace/terraform/environments/dev hashicorp/terraform:1.7.5 validate
```

---

## 4. GitHub Actions CI & Security Integration

Infrastructure code quality and security are validated across two dedicated, read-only CI jobs:

### A. Terraform Validation CI (`terraform-ci`)
- **`terraform fmt -check -recursive`**: Enforces canonical indentation and style.
- **`terraform init -backend=false`**: Downloads provider schema plugins without remote state or credentials, verifying package checksums against `.terraform.lock.hcl`.
- **`terraform validate`**: Verifies attribute types, module inputs/outputs, and HCL grammar offline with zero cloud cost.

### B. Terraform Security & Lint CI (`terraform-security-ci`)
- **TFLint (`v0.64.0`)**: Runs Terraform best-practice linting and evaluates the pinned AWS ruleset (`v0.49.0`). Scans recursively using `--force`, which reports lint findings without failing on findings alone while preserving non-zero exit codes on fatal configuration or execution errors. Action pinned to immutable commit SHA (`terraform-linters/setup-tflint@6ffdbaa3... # v6.3.2`).
- **Trivy (`v0.75.0`)**: Scans `terraform/` for Infrastructure as Code (IaC) security misconfigurations across all resource types via `aquasecurity/trivy-action@ed142fd0... # v0.36.0` with `version: 'v0.75.0'`.
- **Audit Baseline**: Operates under a report-only baseline during the Step 4.4 audit phase (`exit-code: 0` on Trivy, `--force` on TFLint), ensuring full finding transparency in job logs while still failing on scanner installation or execution failures.


---

## 5. Future Remote State Strategy (Cloud Deployment)

When migrating from local testing to multi-developer cloud deployments, update `environments/dev/versions.tf`:

```hcl
terraform {
  backend "s3" {
    bucket         = "sareekart-terraform-state-<ACCOUNT_ID>"
    key            = "dev/terraform.tfstate"
    region         = "ap-south-1"
    dynamodb_table = "sareekart-terraform-locks"
    encrypt        = true
  }
}
```
*(The state bucket and DynamoDB lock table should be provisioned via a dedicated bootstrap script before running remote state operations).*
