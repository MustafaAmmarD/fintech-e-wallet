# Phase 7: Deployment & CI/CD Pipeline

## What You Will Learn

In this phase, you will learn how to take your e-wallet app from "runs on my laptop" to "runs on the internet for anyone to use." This is the most important skill in software engineering — **deployment**.

---

## Key Concepts Explained

### 1. What is Deployment?

Right now, your app runs like this:

```
Your Laptop → runs Spring Boot → connects to Docker PostgreSQL on localhost
```

After deployment, it will run like this:

```
Internet User → sends HTTP request → Cloud Server → runs your Spring Boot app
                                         ↓
                                    Cloud PostgreSQL (managed database)
```

**In simple words:** Deployment means putting your application on a server that is always running, so anyone in the world can access it via a URL like `https://ewallet-api.onrender.com`.

---

### 2. What is Docker? (The Full Picture)

You already use Docker for PostgreSQL and Redis (`docker-compose.yml`). But here's the bigger picture:

**Without Docker:**

- You install Java 21 manually on the server
- You install PostgreSQL manually
- You configure everything manually
- If the server changes, you do it all again 😩

**With Docker:**

- You write a `Dockerfile` that says: "Use Java 21, copy my JAR, run it"
- Docker creates a **container** (a mini-computer) with everything pre-installed
- You can run this container on ANY server in the world — it works the same everywhere! 🎉

**Analogy:** Think of Docker like a **shipping container**. You pack your furniture (app + dependencies) into a standard container. That container can be loaded onto any ship (server) in the world — the contents are always the same.

---

### 3. What is CI/CD?

| Term   | Stands For             | What It Does                                                   |
| ------ | ---------------------- | -------------------------------------------------------------- |
| **CI** | Continuous Integration | Every time you push code to GitHub, tests run automatically    |
| **CD** | Continuous Deployment  | If tests pass, the app is automatically deployed to the server |

**Without CI/CD:** You test manually, build manually, deploy manually. Error-prone.
**With CI/CD:** Push code → GitHub runs tests → if green, deploys automatically. 🚀

---

### 4. What is Heroku?

Heroku is a popular cloud platform where you can host:

- **Web Services** (your Spring Boot app)
- **Databases** (Heroku Postgres)
- **Redis** (Heroku Data for Redis)

Since you have the **GitHub Student Developer Pack**, you get a $13/month platform credit for 12 months. This is perfect! It covers an Eco Dyno ($5/mo) for the app, a Mini Postgres Database ($5/mo), and leaves $3 for a Mini Redis instance.

With Heroku, you can push a Docker container or connect your GitHub repository, and Heroku will automatically rebuild and redeploy your app just like a professional CI/CD pipeline!

---

## Roadmap

| Step | What We Do                        | What You Learn                                      |
| ---- | --------------------------------- | --------------------------------------------------- |
| 7.1  | Create `.gitignore`               | Protect sensitive files from being pushed to GitHub |
| 7.2  | Create a `Dockerfile`             | Package your app into a Docker container            |
| 7.3  | Create `docker-compose.prod.yml`  | Run the full stack (app + DB + Redis) in Docker     |
| 7.4  | Create GitHub Actions CI Pipeline | Auto-run tests on every push                        |
| 7.5  | Deploy to Render & Supabase       | Put your app live on the internet!                  |
| 7.6  | Test the live deployment          | Verify everything works via the public URL          |

---

## Step 7.1: Create `.gitignore` ✅

A `.gitignore` file prevents sensitive or huge files from being pushed to GitHub.
We updated our existing `.gitignore` to include:

```gitignore
### Build outputs
target/
!.mvn/wrapper/maven-wrapper.jar

### OS & IDE Files
.idea/
.DS_Store

### Secrets & Local Files
*.env
.env
application-local.yml
```

Without this, you might accidentally push your database password to GitHub! 😱

---

## Step 7.2: Create a `Dockerfile` ✅

A `Dockerfile` is a recipe to build your app into a portable container. We used a **multi-stage build**:

```dockerfile
# STAGE 1: BUILDER
# Uses a heavy image with Maven to compile the code
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# STAGE 2: RUNNER
# Uses a tiny image with ONLY Java to run the app
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

This multi-stage approach is the professional standard because it keeps the final container tiny and secure!

---

## Step 7.3: Docker Compose for Production ✅

For production (or local testing of the production build), we created `docker-compose.prod.yml`.
This file spins up:

1. Our Spring Boot app (using the Dockerfile)
2. PostgreSQL 15
3. Redis 7

```yaml
version: "3.8"
services:
  app:
    build: .
    environment:
      - SPRING_PROD_PROFILES_ACTIVE=prod
      - DATABASE_URL=jdbc:postgresql://postgres:5432/ewallet_prod
      # ... other env vars ...
    depends_on:
      postgres: { condition: service_healthy }
      redis: { condition: service_healthy }

  postgres:
    image: postgres:15-alpine
    # ...
```

Notice `jdbc:postgresql://postgres:5432`. Docker sets up a private network so the app container can talk directly to the Postgres container using its name!

---

## Step 7.4: GitHub Actions CI Pipeline ✅

We created `.github/workflows/ci.yml`. This tells GitHub to build and test our code every time we push!

```yaml
name: Java CI/CD Pipeline
on:
  push:
    branches: ["main"]

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    # Start a Postgres database specifically for our Integration Tests!
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: ewallet_dev
          POSTGRES_USER: ewallet
          POSTGRES_PASSWORD: ewallet_dev_password
        ports: ["5432:5432"]

    steps:
      - uses: actions/checkout@v4 # Get code
      - uses: actions/setup-java@v4 # Install Java 21
        with:
          java-version: "21"
          distribution: "temurin"

      - name: Run Tests # Run the 36 tests!
        run: ./mvnw test
```

If you push broken code, GitHub will show a **Red X ❌** and email you.
If the code is perfect, GitHub shows a **Green Checkmark ✅**.

---

## Step 7.5: Deploy to Render (App) + Supabase (DB)

### The Deployment Process

1. Go to **[Supabase.com](https://supabase.com)** and create a free account.
2. Click **New Project**, give it a name and a strong secure database password. _Save this password!_
3. Once the project is created, go to **Project Settings -> Database**. Look for the **Connection String (URI)**. It will look like `postgresql://postgres.[ref]:[password]@aws-0-[region].pooler.supabase.com:6543/postgres`. Copy this!
4. Sign up at **[Render.com](https://render.com)** using your GitHub account.
5. In Render, create a new **Web Service** and connect it to your GitHub repository.
6. Set the environment variables in Render:
   - `DATABASE_URL` = (paste the URL from Supabase, replacing `[password]` with the real password you created)
   - `JWT_SECRET` = (make up a long secure password)
   - `SPRING_PROD_PROFILES_ACTIVE` = `prod`
7. Render automatically building your `Dockerfile` and deploys your Java app!

Every time you push to GitHub → Render detects the change → rebuilds → redeploys. Fully automatic! 🚀

---

## Step 7.6: Test the Live Deployment

After deployment, we'll test the live API using Postman or curl:

- Register a new user
- Login and get a JWT token
- Check wallet balance
- Make a transfer

This proves your entire application works in production, not just on your laptop!
