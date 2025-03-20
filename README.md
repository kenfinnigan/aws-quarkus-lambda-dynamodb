# Quarkus as AWS Lambda Function behind API Gateway, with DynamoDB

An example project of using Quarkus to build an AWS Lambda Function to be deployed behind an API Gateway.
The AWS Lambda Function uses DynamoDB for storing data.

## Prerequisites

### Java

1. Java / openJDK is installed

### Docker

1. [Docker](https://www.docker.com/) is installed for local testing

## Running

### Tests locally

Ensure Docker is running,
then you can test your application with:

```shell script
./mvnw test
```
