# ECS Deploy

Inspired by other ECS deploy tools out there, this Spring based command line application aims to be simple and provide a way to quickly update
an ECS service's Docker container image. It works by providing details about the target ECS Cluster/Service and providing
a new Docker image tag. This tool currently has some assumptions:

- The Cluster and Service details provided actually exist
- The user will provide proper AWS credentials as one would for the official AWS CLI
- The latest registered Task Definition is the one being replaced; not necessarily the one that is currently running!
- You expect a "rollback" and auto-de-registration of the updated resources if something goes wrong

## Options
### Required
- `-c` or `--cluster`:
    - The ECS Service name or ARN
    - An SSM Parameter name containing the ECS Service name or ARN
- `-s` or `--service`:
  - The ECS Service name or ARN
  - An SSM Parameter name containing the ECS Service name or ARN
- `--tag`:
  - The new Docker Image tag

### Optional
- `--region`:
  - Target AWS Region

## Current Tech Stack
- Java 17
- Spring Boot 2.7.2
- AWS ECS SDK V2

## TODO
- Make this into a native project with one of the GraalVM based frameworks