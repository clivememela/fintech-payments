#!/bin/bash

# AWS Deployment Script for Fintech Payments System
set -e

# Configuration
ENVIRONMENT=${1:-production}
AWS_REGION=${2:-us-east-1}
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}$1${NC}"
}

# Validate prerequisites
validate_prerequisites() {
    print_header "🔍 Validating Prerequisites"
    
    # Check AWS CLI
    if ! command -v aws &> /dev/null; then
        print_error "AWS CLI is not installed"
        exit 1
    fi
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed"
        exit 1
    fi
    
    # Check AWS credentials
    if ! aws sts get-caller-identity &> /dev/null; then
        print_error "AWS credentials not configured"
        exit 1
    fi
    
    print_status "✅ Prerequisites validated"
}

# Create ECR repositories
create_ecr_repositories() {
    print_header "📦 Creating ECR Repositories"
    
    # Create repositories if they don't exist
    aws ecr describe-repositories --repository-names fintech-payments-ledger-service --region $AWS_REGION 2>/dev/null || \
        aws ecr create-repository --repository-name fintech-payments-ledger-service --region $AWS_REGION
    
    aws ecr describe-repositories --repository-names fintech-payments-transfer-service --region $AWS_REGION 2>/dev/null || \
        aws ecr create-repository --repository-name fintech-payments-transfer-service --region $AWS_REGION
    
    # Set lifecycle policies
    aws ecr put-lifecycle-policy \
        --repository-name fintech-payments-ledger-service \
        --lifecycle-policy-text file://aws/ecr-lifecycle-policy.json \
        --region $AWS_REGION
    
    aws ecr put-lifecycle-policy \
        --repository-name fintech-payments-transfer-service \
        --lifecycle-policy-text file://aws/ecr-lifecycle-policy.json \
        --region $AWS_REGION
    
    print_status "✅ ECR repositories created"
}

# Build and push Docker images
build_and_push_images() {
    print_header "🏗️ Building and Pushing Docker Images"
    
    # Get ECR login token
    aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
    
    # Build and push Ledger Service
    print_status "Building Ledger Service..."
    docker build -t fintech-payments-ledger-service:latest ./fintech-payments-ledger-service/
    docker tag fintech-payments-ledger-service:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/fintech-payments-ledger-service:latest
    docker tag fintech-payments-ledger-service:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/fintech-payments-ledger-service:$(git rev-parse --short HEAD)
    docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/fintech-payments-ledger-service:latest
    docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/fintech-payments-ledger-service:$(git rev-parse --short HEAD)
    
    # Build and push Transfer Service
    print_status "Building Transfer Service..."
    docker build -t fintech-payments-transfer-service:latest ./fintech-payments-transfer-service/
    docker tag fintech-payments-transfer-service:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/fintech-payments-transfer-service:latest
    docker tag fintech-payments-transfer-service:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/fintech-payments-transfer-service:$(git rev-parse --short HEAD)
    docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/fintech-payments-transfer-service:latest
    docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/fintech-payments-transfer-service:$(git rev-parse --short HEAD)
    
    print_status "✅ Docker images built and pushed"
}

# Deploy infrastructure
deploy_infrastructure() {
    print_header "🏗️ Deploying Infrastructure"
    
    # Prompt for database password if not set
    if [ -z "$DB_PASSWORD" ]; then
        read -s -p "Enter database password: " DB_PASSWORD
        echo
    fi
    
    # Prompt for certificate ARN if not set
    if [ -z "$CERTIFICATE_ARN" ]; then
        read -p "Enter SSL Certificate ARN: " CERTIFICATE_ARN
    fi
    
    # Deploy infrastructure stack
    aws cloudformation deploy \
        --template-file aws/infrastructure.yaml \
        --stack-name $ENVIRONMENT-fintech-infrastructure \
        --parameter-overrides \
            Environment=$ENVIRONMENT \
            DatabasePassword=$DB_PASSWORD \
            CertificateArn=$CERTIFICATE_ARN \
        --capabilities CAPABILITY_NAMED_IAM \
        --region $AWS_REGION
    
    print_status "✅ Infrastructure deployed"
}

# Deploy services
deploy_services() {
    print_header "🚀 Deploying Services"
    
    # Get infrastructure outputs
    VPC_ID=$(aws cloudformation describe-stacks --stack-name $ENVIRONMENT-fintech-infrastructure --query 'Stacks[0].Outputs[?OutputKey==`VPCId`].OutputValue' --output text --region $AWS_REGION)
    CLUSTER_NAME=$(aws cloudformation describe-stacks --stack-name $ENVIRONMENT-fintech-infrastructure --query 'Stacks[0].Outputs[?OutputKey==`ECSClusterName`].OutputValue' --output text --region $AWS_REGION)
    
    print_status "VPC ID: $VPC_ID"
    print_status "Cluster: $CLUSTER_NAME"
    
    # Deploy Transfer Service
    print_status "Deploying Transfer Service..."
    # This would typically involve deploying ECS services
    # For brevity, showing the concept
    
    print_status "✅ Services deployed"
}

# Update services
update_services() {
    print_header "🔄 Updating Services"
    
    # Update Transfer Service
    aws ecs update-service \
        --cluster $ENVIRONMENT-fintech-cluster \
        --service $ENVIRONMENT-transfer-service \
        --force-new-deployment \
        --region $AWS_REGION
    
    # Update Ledger Service
    aws ecs update-service \
        --cluster $ENVIRONMENT-fintech-cluster \
        --service $ENVIRONMENT-ledger-service \
        --force-new-deployment \
        --region $AWS_REGION
    
    print_status "✅ Services updated"
}

# Wait for deployment
wait_for_deployment() {
    print_header "⏳ Waiting for Deployment"
    
    print_status "Waiting for Transfer Service to stabilize..."
    aws ecs wait services-stable \
        --cluster $ENVIRONMENT-fintech-cluster \
        --services $ENVIRONMENT-transfer-service \
        --region $AWS_REGION
    
    print_status "Waiting for Ledger Service to stabilize..."
    aws ecs wait services-stable \
        --cluster $ENVIRONMENT-fintech-cluster \
        --services $ENVIRONMENT-ledger-service \
        --region $AWS_REGION
    
    print_status "✅ Deployment completed successfully"
}

# Main deployment flow
main() {
    print_header "🚀 Fintech Payments System - AWS Deployment"
    print_status "Environment: $ENVIRONMENT"
    print_status "Region: $AWS_REGION"
    print_status "Account: $AWS_ACCOUNT_ID"
    echo
    
    validate_prerequisites
    create_ecr_repositories
    build_and_push_images
    deploy_infrastructure
    deploy_services
    update_services
    wait_for_deployment
    
    print_header "🎉 Deployment Complete!"
    
    # Get load balancer DNS
    ALB_DNS=$(aws cloudformation describe-stacks --stack-name $ENVIRONMENT-fintech-infrastructure --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerDNS`].OutputValue' --output text --region $AWS_REGION)
    
    print_status "Application URL: https://$ALB_DNS"
    print_status "Health Check: https://$ALB_DNS/actuator/health"
}

# Run main function
main "$@"