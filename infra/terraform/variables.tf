variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "image" {
  description = "Container image for ticketmesh-core"
  type        = string
}

variable "db_host" {
  description = "Database host"
  type        = string
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "ticketmesh_db"
}

variable "db_user" {
  description = "Database user"
  type        = string
}

variable "db_password" {
  description = "Database password"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT signing secret"
  type        = string
  sensitive   = true
}

variable "subnet_ids" {
  description = "Subnet IDs for the Fargate service"
  type        = list(string)
}

variable "security_group_id" {
  description = "Security group ID for the service"
  type        = string
}

variable "target_group_arn" {
  description = "ALB target group ARN"
  type        = string
}

variable "min_capacity" {
  description = "Minimum task count"
  type        = number
  default     = 2
}

variable "max_capacity" {
  description = "Maximum task count (autoscaling)"
  type        = number
  default     = 20
}

variable "log_retention_days" {
  type    = number
  default = 14
}
