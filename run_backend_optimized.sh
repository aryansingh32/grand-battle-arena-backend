#!/bin/bash
echo "🚀 Starting Backend with Optimized Memory Settings..."
export MAVEN_OPTS="-XX:MaxMetaspaceSize=512m -Xmx1024m"
./mvnw spring-boot:run
