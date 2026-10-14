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

### G. Default Resource Graph vs. Paid Cloud Resources
It is critical to distinguish between **zero paid compute/database/NAT resources** and **zero AWS resources**:
- Setting `create_ecs = false`, `create_rds = false`, and `enable_nat_gateway = false` avoids all continuous hourly billing components (Application Load Balancer, Fargate vCPU/RAM, NAT Gateway, RDS MySQL instance).
- However, the default configuration still synthesizes **34 foundational AWS resources** in the Terraform dependency graph:
  - **Networking (`modules/vpc`) — 14 resources**: 1 VPC (`10.0.0.0/16`), 1 Internet Gateway, 2 Public Subnets, 2 Private Subnets, 1 Public Route Table, 2 Private Route Tables, 1 Public Internet Route (`0.0.0.0/0` -> IGW), 4 Subnet Route Table Associations.
  - **Security Groups (`modules/security_groups`) — 10 resources**: 3 Security Groups (`alb`, `ecs`, `rds`), 2 ALB Ingress Rules (ports 80 & 443), 1 ALB Egress Rule (port 8081 -> ECS SG), 1 ECS Ingress Rule (port 8081 from ALB SG), 2 ECS Egress Rules (port 443 HTTPS & port 4000 TiDB), 1 RDS Ingress Rule (port 3306 from ECS SG).
  - **Object Storage (`modules/s3`) — 5 resources**: 1 S3 Bucket (`sareekart-media-dev`), 1 Public Access Block (all 4 flags true), 1 Server-Side Encryption Configuration (`AES256`), 1 Bucket Versioning Configuration (`Enabled`), 1 Lifecycle Rule Configuration (`STANDARD_IA` transition at 90 days, abort multipart uploads at 7 days).
  - **Identity & Access Management (`modules/iam`) — 5 resources**: 1 ECS Task Execution IAM Role + AWS Managed Policy Attachment (`AmazonECSTaskExecutionRolePolicy`), 1 ECS Task IAM Role + Custom S3 Read-Only IAM Policy + Attachment.
  - **Compute (`modules/ecs`) — 0 resources**: Cluster, Task Definition, Service, ALB, Target Groups are skipped (`create_ecs = false`).
  - **Database (`modules/rds`) — 0 resources**: DB Subnet Group, DB Parameter Group, RDS MySQL Instance are skipped (`create_rds = false`).
  - **NAT Gateways (`modules/vpc`) — 0 resources**: EIP and NAT Gateway are skipped (`enable_nat_gateway = false`).
- **Operational Clarity**: An actual `terraform apply` against AWS would create these 34 foundational resources. While these resources fit within standard AWS Free Tier allowances or carry no hourly baseline charges when idle, they are real cloud infrastructure resources. Running a speculative plan against an emulator verifies this full 34-resource graph without touching AWS or incurring costs.

---

## 3. Local Offline Validation & Testing Workflow

Infrastructure code quality, syntax, cost guards, and module contracts can be verified locally **without cloud credentials or making AWS API calls** using HashiCorp Terraform 1.7.5:

```bash
# 1. Formatting check (all files)
docker run --rm -v $(pwd):/workspace -w /workspace hashicorp/terraform:1.7.5 fmt -check -recursive

# 2. Offline initialization without backend
docker run --rm -v $(pwd):/workspace -w /workspace/terraform/environments/dev hashicorp/terraform:1.7.5 init -backend=false

# 3. Offline syntax and configuration validation
docker run --rm -v $(pwd):/workspace -w /workspace/terraform/environments/dev hashicorp/terraform:1.7.5 validate

# 4. Native Mocked-Provider Tests (Terraform 1.7+ mock_provider)
docker run --rm -v $(pwd):/workspace -w /workspace/terraform/environments/dev hashicorp/terraform:1.7.5 test -no-color
```

### D. Optional Local Speculative Plan via LocalStack Emulator
To preview Terraform's execution plan and inspect resource changes against an AWS API mock, developers may optionally run a speculative plan against LocalStack.

> [!IMPORTANT]
> **LocalStack Lifecycle Notice**: LocalStack sunset its standalone Community Edition image starting March 23, 2026, and consolidated into a unified container image requiring an account and an authentication token (`LOCALSTACK_AUTH_TOKEN`). Earlier tags (such as `3.8.0`) are legacy unsupported images. When running LocalStack, developers must use an eligible no-cost authentication tier or personal account token.

When evaluating against LocalStack locally, adhere strictly to the following isolation safeguards:
1. **Fixed Mock Credentials Only**: Never supply real AWS access keys or secrets.
2. **Complete Endpoint Routing**: Explicitly route every configured AWS service (EC2, IAM, S3, STS, ECS, RDS, ELBv2, CloudWatch, Logs) to the local container endpoint (`http://localstack_plan:4566` or `http://127.0.0.1:4566`).
3. **Provider Bypass Flags**: Set `skip_credentials_validation = true`, `skip_metadata_api_check = true`, `skip_requesting_account_id = true`, and `s3_use_path_style = true`.
4. **Isolated Override Files**: Generate `localstack_override.tf` only within the local working directory (automatically ignored by `terraform/.gitignore`). Never commit override files to version control.
5. **Speculative Plan Only**: Execute `terraform plan` only. **Never run `apply`, `destroy`, or `import`**.

```bash
# Example local speculative plan workflow (with isolated network):
docker network create tf-localstack-net
docker run -d --name localstack_plan \
  --network tf-localstack-net \
  -e SERVICES=ec2,iam,s3,sts,logs,ecs,rds,elbv2 \
  -e LOCALSTACK_AUTH_TOKEN="<your_auth_token>" \
  -p 4566:4566 \
  localstack/localstack:latest

# Run speculative plan inside Docker network:
docker run --rm --network tf-localstack-net \
  -v $(pwd)/terraform:/workspace -w /workspace/environments/dev \
  hashicorp/terraform:1.7.5 plan -no-color

# Clean up:
docker rm -f localstack_plan && docker network rm tf-localstack-net
```

---

## 4. Testing & Validation Scope & Boundaries

SareeKart leverages layered offline testing. It is essential to recognize what each validation method proves and where its boundaries lie:

### A. Native Mocked-Provider Tests (`terraform test` with `mock_provider`)
- **What it Validates**:
  - Validates HCL expressions, variable types, and default values.
  - Verifies module composition wiring and parameter passing between `environments/dev` and `modules/*`.
  - Enforces baseline cost guards (`create_ecs = false`, `create_rds = false`, `enable_nat_gateway = false`, `neo4j_enabled = false`).
  - Verifies output fallback strings (`alb_disabled`, `ecs_cluster_disabled`, `ecs_service_disabled`, `rds_disabled_using_tidb_cloud`).
- **Boundaries & Limitations**:
  - Uses synthetic data for computed attributes; does not evaluate AWS API schemas or network calls.
  - Does not replace real cloud infrastructure testing.

### B. LocalStack Emulator Emulation Boundaries
For developers executing speculative plans against LocalStack:

| Architecture Domain | What LocalStack Emulates | Emulation Boundaries & Differences from Live AWS |
|---|---|---|
| **VPC & Networking** | VPC, subnets, route tables, IGWs, CIDR calculations | Does not simulate AWS backbone routing latency, VPC peering bandwidth, or real NAT throughput limits. |
| **Security Groups** | Security groups, ingress/egress rules (`aws_vpc_security_group_*_rule`) | Graph relationships are registered; does not simulate actual stateful packet filtering or kernel-level network ACL enforcement. |
| **IAM & STS** | Role definitions, trust policies, policy attachments, mock STS caller identity | Validates IAM HCL syntax and ARN structures; does **not** evaluate complex IAM conditions, permission boundaries, or service-linked role constraints. |
| **S3 Storage** | Bucket creation, SSE-S3 encryption config, versioning, public access blocks, lifecycle rule declarations | Fully emulates S3 API calls locally; does not simulate cross-region replication latency or AWS KMS HSM integrations. |
| **ECS & Fargate** | ECS cluster and task definition registration | LocalStack stubs the ECS API; it does **not** execute real Fargate containers, provision AWS VPC ENIs, or simulate task health check timeouts. |
| **RDS MySQL** | DB subnet groups, parameter groups | Stubs RDS metadata; it does **not** provision a managed MySQL instance with automated multi-AZ failover, automated snapshots, or RDS proxy. |
| **ELBv2 (ALB)** | Target groups, listeners, listener rules | Stubs ELBv2 responses; does not simulate real cookie-based session stickiness routing or active health check probes against live backends. |

> [!CAUTION]
> Neither native mocked-provider tests nor speculative emulator plans prove real AWS deployment compatibility, cloud quotas, or operational readiness. They are pre-deployment structural and logic verification gates.

---

## 5. GitHub Actions CI & Security Integration

Infrastructure code quality, security, and logic testing are validated across dedicated, read-only CI jobs:

### A. Terraform Validation CI (`terraform-ci`)
- **`terraform fmt -check -recursive`**: Enforces canonical indentation and style across all modules.
- **`terraform init -backend=false`**: Downloads provider schema plugins without remote state or credentials, verifying package checksums against `.terraform.lock.hcl`.
- **`terraform validate`**: Verifies attribute types, module inputs/outputs, and HCL grammar offline with zero cloud cost.

### B. Terraform Security & Lint CI (`terraform-security-ci`)
- **TFLint (`v0.64.0`)**: Runs Terraform best-practice linting and evaluates the pinned AWS ruleset (`v0.49.0`). Scans recursively using `--force`, which reports lint findings without failing on findings alone while preserving non-zero exit codes on fatal configuration or execution errors. Action pinned to immutable commit SHA (`terraform-linters/setup-tflint@6ffdbaa3... # v6.3.2`).
- **Trivy (`v0.75.0`)**: Scans `terraform/` for Infrastructure as Code (IaC) security misconfigurations across all resource types via `aquasecurity/trivy-action@ed142fd0... # v0.36.0` with `version: 'v0.75.0'`.
- **Audit Baseline & Accepted Residual Exposures**: Operates under a report-only baseline during the audit phase (`exit-code: 0` on Trivy, `--force` on TFLint), ensuring full finding transparency in job logs while still failing on scanner installation or execution failures.
  - *Accepted Residual Findings*: Trivy flags two Critical egress rules (`AVD-AWS-0104`) on the ECS Security Group:
    1. Outbound HTTPS (port 443) to `0.0.0.0/0` (required for AWS ECR container image pulls and AWS CloudWatch Logs streaming).
    2. Outbound MySQL (port 4000) to `0.0.0.0/0` (required to connect to external TiDB Cloud Serverless gateway, which utilizes dynamic multi-tenant cloud IP addresses).
  - Both findings are documented as accepted architectural requirements under the current zero-spend serverless database deployment.

### C. Terraform Native Test CI (`terraform-test-ci`)
- **`terraform test -no-color`**: Runs automated integration and cost-guard tests in `terraform/environments/dev/tests/defaults.tftest.hcl` using Terraform 1.7's native `mock_provider`.
- Runs with zero third-party dependencies, zero cloud credentials, zero external tokens, and zero Docker service containers.
- Enforces baseline cost guards (`create_ecs = false`, `create_rds = false`, `enable_nat_gateway = false`) and verifies output expressions in <5 seconds.


---

## 6. Future Remote State Strategy (Cloud Deployment)

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
