---
name: docker-manager
description: Professional Docker container management toolkit for deployment, monitoring, error analysis, and compose operations. Use when Claude needs to manage Docker containers, build images, debug container errors, or handle Docker Compose deployments. Trigger for container lifecycle operations, image management, log analysis, and orchestration tasks.
---

# Docker Manager

Professional Docker operations toolkit for comprehensive container management, deployment, and troubleshooting.

## 🎯 When to Use This Skill

1. **Container Management**: Starting, stopping, restarting, or removing containers
2. **Image Operations**: Building, pulling, pushing, or managing Docker images
3. **Deployment**: Docker Compose service deployment and orchestration
4. **Monitoring**: Container status checking and health monitoring
5. **Troubleshooting**: Log analysis and error diagnosis for container issues
6. **Maintenance**: Image cleanup, container pruning, and system maintenance

## 🚀 Quick Start

### Container Operations
```bash
# List all containers
python scripts/docker_manager.py --action list --all

# Start a container
python scripts/docker_manager.py --action start --container my-app

# Stop container with custom timeout
python scripts/docker_manager.py --action stop --container my-app --timeout 30

# Get container logs with error analysis
python scripts/docker_manager.py --action logs --container my-app --tail 200 --analyze
```

### Image Management
```bash
# List all images
python scripts/image_manager.py --action list --all

# Build image with tag
python scripts/image_manager.py --action build --context . --tag my-app:latest

# Pull image from registry
python scripts/image_manager.py --action pull --image nginx:alpine

# Remove unused images
python scripts/image_manager.py --action prune --all --force
```

### Docker Compose Operations
```bash
# Validate compose configuration
python scripts/compose_manager.py --action validate

# Start all services
python scripts/compose_manager.py --action up --detach --build

# Scale specific service
python scripts/compose_manager.py --action scale --services web --replicas 3

# Get service logs
python scripts/compose_manager.py --action logs --services web db --tail 100
```

## 🛠️ Core Capabilities

### 1. Container Lifecycle Management
- **Start/Stop/Restart**: Reliable container state management
- **Remove**: Safe container cleanup with force options
- **Status Monitoring**: Real-time container health checks
- **Inspection**: Detailed container configuration analysis

### 2. Image Management
- **Build**: Context-aware image building with cache control
- **Registry Operations**: Pull/push with authentication handling
- **Tagging**: Image tagging and version management
- **Cleanup**: Automated image pruning and space recovery

### 3. Log Analysis & Error Detection
- **Smart Parsing**: Automatic error pattern recognition
- **Issue Classification**: Categorize errors (connection, memory, permission, etc.)
- **Root Cause Analysis**: Contextual error diagnosis
- **Performance Insights**: Resource usage pattern detection

### 4. Docker Compose Integration
- **Service Orchestration**: Multi-container application management
- **Configuration Validation**: Pre-deployment configuration checking
- **Scaling Operations**: Dynamic service scaling
- **Environment Management**: Consistent deployment environments

## 📋 Script Reference

### docker_manager.py
Main container operations script:
- Container lifecycle management
- Log retrieval and analysis
- Status monitoring
- Error detection in container operations

### image_manager.py
Image operations script:
- Image building and tagging
- Registry interactions
- Image listing and cleanup
- Size optimization recommendations

### compose_manager.py
Docker Compose operations script:
- Service deployment and scaling
- Configuration validation
- Multi-service log aggregation
- Environment consistency management

## ⚡ Best Practices

### Performance Optimization
- Use `--detach` for long-running operations
- Limit log output with `--tail` parameter
- Enable `--no-cache` only when necessary
- Regular image pruning to save disk space

### Security Considerations
- Validate image sources before pulling
- Use specific tags instead of `latest`
- Review compose configurations for secrets
- Monitor container resource limits

### Troubleshooting Workflow
1. Check container status: `docker_manager.py --action list`
2. Analyze logs: `docker_manager.py --action logs --analyze`
3. Inspect configuration: `docker_manager.py --action inspect`
4. Review image layers: `image_manager.py --action inspect`

## 🔄 Common Workflows

### Deployment Pipeline
```bash
# 1. Validate configuration
python scripts/compose_manager.py --action validate

# 2. Build new images
python scripts/image_manager.py --action build --context . --tag app:v1.0

# 3. Deploy services
python scripts/compose_manager.py --action up --detach --build

# 4. Verify deployment
python scripts/compose_manager.py --action status
```

### Error Investigation
```bash
# 1. Check container status
python scripts/docker_manager.py --action list --all

# 2. Get recent logs with analysis
python scripts/docker_manager.py --action logs --container problematic-app --tail 500 --analyze

# 3. Inspect container details
python scripts/docker_manager.py --action inspect --container problematic-app
```

### Maintenance Routine
```bash
# 1. Clean up unused containers
python scripts/docker_manager.py --action list --all

# 2. Remove stopped containers
python scripts/docker_manager.py --action remove --container <container-id> --force

# 3. Prune unused images
python scripts/image_manager.py --action prune --all --force

# 4. Check disk usage
# (Manual docker system df)
```

## 🛑 Critical Rules

1. **Always validate** Docker Compose configurations before deployment
2. **Analyze logs** when containers fail to start or behave unexpectedly
3. **Use specific tags** instead of `latest` for production deployments
4. **Monitor resource usage** to prevent container crashes
5. **Clean up regularly** to maintain system performance

## 📚 References

See [DOCKER_BEST_PRACTICES.md](references/DOCKER_BEST_PRACTICES.md) for detailed guidelines on:
- Security hardening
- Performance optimization
- Troubleshooting techniques
- Production deployment patterns

See [COMPOSE_TEMPLATES.md](references/COMPOSE_TEMPLATES.md) for:
- Common service configurations
- Multi-stage deployment patterns
- Environment-specific setups
- Backup and recovery procedures