terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
}

locals {
  name = "ticketmesh"
  tags = {
    Project = "TicketMesh"
    Managed = "terraform"
  }
}

# ── ECS Cluster (Fargate) ────────────────────────────────────────────────
resource "aws_ecs_cluster" "this" {
  name = "${local.name}-cluster"
  tags = local.tags
}

# ── Task Definition ──────────────────────────────────────────────────────
resource "aws_ecs_task_definition" "core" {
  family                   = "${local.name}-core"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.execution.arn
  container_definitions = jsonencode([
    {
      name      = "ticketmesh-core"
      image     = var.image
      essential = true
      portMappings = [{ containerPort = 8080, protocol = "tcp" }]
      environment = [
        { name = "DB_URL", value = "jdbc:postgresql://${var.db_host}:5432/${var.db_name}?useSSL=false" },
        { name = "DB_USERNAME", value = var.db_user },
        { name = "DB_PASSWORD", value = var.db_password },
        { name = "JWT_SECRET", value = var.jwt_secret },
        { name = "BOOTSTRAP_ADMIN_USERNAME", value = "admin" },
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.core.name
          "awslogs-region"        = var.region
          "awslogs-stream-prefix" = "ticketmesh"
        }
      }
      healthCheck = {
        command  = ["CMD-SHELL", "wget -q -O - http://localhost:8080/actuator/health || exit 1"]
        interval = 20
        retries  = 5
        timeout  = 5
      }
    }
  ])
}

# ── Service + Auto Scaling (horizontal) ──────────────────────────────────
resource "aws_ecs_service" "core" {
  name            = "${local.name}-core"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.core.arn
  desired_count   = var.min_capacity
  launch_type     = "FARGATE"
  depends_on      = [aws_iam_role_policy.execution_ecs]

  network_configuration {
    subnets         = var.subnet_ids
    security_groups = [var.security_group_id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = var.target_group_arn
    container_name   = "ticketmesh-core"
    container_port   = 8080
  }
}

resource "aws_appautoscaling_target" "core" {
  max_capacity       = var.max_capacity
  min_capacity       = var.min_capacity
  resource_id        = "service/${aws_ecs_cluster.this.name}/${aws_ecs_service.core.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "cpu" {
  name               = "${local.name}-cpu"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.core.resource_id
  scalable_dimension = aws_appautoscaling_target.core.scalable_dimension
  service_namespace  = aws_appautoscaling_target.core.service_namespace
  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value       = 60
    scale_in_cooldown  = 120
    scale_out_cooldown = 20
  }
}

# ── IAM ──────────────────────────────────────────────────────────────────
resource "aws_iam_role" "execution" {
  name = "${local.name}-execution"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy" "execution_ecs" {
  name = "${local.name}-execution-policy"
  role = aws_iam_role.execution.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogStream",
          "logs:PutLogEvents",
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = ["ecr:GetAuthorizationToken", "ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer"]
        Resource = "*"
      }
    ]
  })
}

# ── Observability ────────────────────────────────────────────────────────
resource "aws_cloudwatch_log_group" "core" {
  name              = "/ecs/${local.name}-core"
  retention_in_days = var.log_retention_days
  tags              = local.tags
}
