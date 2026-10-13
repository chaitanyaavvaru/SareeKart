# ============================================================================
# SareeKart S3 Module — Secure Private Media & Uploads Storage
# Configures private storage with encryption-at-rest and strict public access block.
#
# NOTE ON APPLICATION INTEGRATION:
# Provisioning this bucket establishes the cloud-native storage infrastructure.
# To make uploads persistent on ECS Fargate, the application controllers
# (PhotoUploadController, ReturnController) will require an AWS S3 SDK adapter
# in a future application phase to write directly to this bucket instead of
# local filesystem paths (uploads/).
# ============================================================================

resource "aws_s3_bucket" "media" {
  bucket_prefix = "${var.bucket_name_prefix}-${var.environment}-media-"
  force_destroy = var.environment == "dev" ? true : false

  tags = merge(var.tags, {
    Name        = "${var.bucket_name_prefix}-${var.environment}-media"
    Environment = var.environment
  })
}

# ── 1. Block All Public Access ───────────────────────────────────────────────
resource "aws_s3_bucket_public_access_block" "media" {
  bucket = aws_s3_bucket.media.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# ── 2. Server-Side Encryption at Rest (AES-256) ──────────────────────────────
resource "aws_s3_bucket_server_side_encryption_configuration" "media" {
  bucket = aws_s3_bucket.media.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# ── 3. Bucket Versioning ─────────────────────────────────────────────────────
resource "aws_s3_bucket_versioning" "media" {
  bucket = aws_s3_bucket.media.id

  versioning_configuration {
    status = "Enabled"
  }
}

# ── 4. Lifecycle Management (Non-Destructive Tiering & Object Hygiene) ────────
resource "aws_s3_bucket_lifecycle_configuration" "media" {
  bucket = aws_s3_bucket.media.id

  rule {
    id     = "archive-return-photos"
    status = "Enabled"

    filter {
      prefix = "return-photos/"
    }

    # Transition older evidence to Standard-IA after 90 days to reduce storage costs
    # without deleting customer return defect photos or saree drape records.
    transition {
      days          = 90
      storage_class = "STANDARD_IA"
    }

    # Automatically abort incomplete multipart uploads after 7 days
    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}
