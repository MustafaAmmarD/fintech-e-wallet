# ==========================================
# STAGE 1: BUILDER
# ==========================================
# We start with a heavy "kitchen" that has Maven and Java 21 installed.
# We will use this to compile the code and build the .jar file.
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy only the pom.xml first. 
# This helps Docker cache the downloaded dependencies so they aren't downloaded again unless pom.xml changes.
COPY pom.xml .

# Download dependencies offline (makes future builds much faster)
RUN mvn dependency:go-offline -B

# Now copy the actual source code
COPY src ./src

# Build the application, skipping tests (tests will run in GitHub Actions instead)
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: RUNNER
# ==========================================
# Now we use a lightweight "restaurant" that ONLY has Java 21 (no Maven, no source code).
# This makes our final container very small and secure.
FROM eclipse-temurin:21-jre-alpine

# Set the working directory
WORKDIR /app

# Copy ONLY the built .jar file from the "builder" stage above
COPY --from=builder /app/target/*.jar app.jar

# Expose the port that our Spring Boot app uses
EXPOSE 8080

# The command to run when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]
