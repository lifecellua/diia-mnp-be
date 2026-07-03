# Use the official Gradle image to create a build artifact
FROM dockyard.diia.org.ua/docker-cache/library/gradle:9-jdk21 AS builder

ARG mavenToken

RUN apt-get update  && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

# Copy local code to the container image
COPY . .

# Build a release artifact
RUN gradle \
  -DmavenToken=$mavenToken \
  -Dorg.gradle.jvmargs="-Xmx2g -XX:MaxMetaspaceSize=512m" \
  clean build -x test

run ls -lah /app/build/libs/

# Use eclipse-temurin for runtime
FROM dockyard.diia.org.ua/docker-cache/library/eclipse-temurin:21-jdk

WORKDIR /app

RUN apt-get update && apt-get upgrade -y

# Set environment variables
ENV diia-documents=localhost:9292
ENV diia-user=localhost:9292

# Copy the jar to the production image from the builder stage
COPY --from=builder /app/build/libs/app.jar /app/lifecell-mnp-be.jar

# Set the startup command to execute the jar
CMD ["java", "-jar", "/lifecell-mnp-be.jar"]


