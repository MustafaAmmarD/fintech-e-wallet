# 🔧 Fixing "Unable to Connect" - Java Version Issue

## 🚨 Problem

Your application is **crashing during startup** due to a Java version mismatch:

```
Java Runtime (class file version 65.0), this version of the
Java Runtime only recognizes class file versions up to 63.0
```

**What this means:**

- Your `JAVA_HOME` points to **Java 19** (`C:\FlutterDev\JDK`)
- Some dependency was compiled with **Java 21** (version 65.0)
- Java 19 cannot run Java 21 code

---

## ✅ Solution: Install Java 21 LTS

### Step 1: Download Java 21

👉 **[Download Eclipse Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21)**

1. Click the link above
2. Select:
   - **Version:** 21 (LTS)
   - **Operating System:** Windows
   - **Architecture:** x64
3. Download the `.msi` installer
4. **Run the installer** and check **"Set JAVA_HOME variable"** during installation

---

### Step 2: Verify Installation

Open a **new PowerShell window** (important: new window to reload environment variables):

```powershell
java -version
```

**Expected output:**

```
openjdk version "21.0.x"
```

Also check `JAVA_HOME`:

```powershell
$env:JAVA_HOME
```

**Expected:** Should point to `C:\Program Files\Eclipse Adoptium\jdk-21.x.x.x-hotspot\`

---

### Step 3: Clean and Run

In your project directory:

```powershell
# Clean previous build artifacts
./mvnw clean

# Set profile and run
$env:SPRING_PROFILES_ACTIVE="dev"
./mvnw spring-boot:run
```

**Wait for:**

```
Started EwalletApplication in X.XXX seconds
```

---

### Step 4: Access Swagger UI

Open your browser:

```
http://localhost:8080/swagger-ui.html
```

You should now see the API documentation! 🎉

---

## 🔄 Alternative: Quick Workaround (Not Recommended)

If you can't install Java 21 right now, try disabling DevTools (which is causing the crash):

### Option A: Comment out DevTools dependency

Edit `pom.xml` and comment out lines 129-134:

```xml
<!-- TEMPORARILY DISABLED DUE TO JAVA VERSION
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
-->
```

Then:

```powershell
./mvnw clean spring-boot:run
```

**⚠️ Warning:** This disables hot-reload. You'll need to restart manually for every code change.

---

## 📋 Summary

| Issue       | Your Setup                    | Required            |
| ----------- | ----------------------------- | ------------------- |
| JAVA_HOME   | Java 19 (`C:\FlutterDev\JDK`) | Java 17 or 21       |
| System Java | Java 24 (preview)             | Java 17 or 21 (LTS) |

**Best fix:** Install Java 21 LTS from Adoptium.

---

## ❓ Need Help?

If you still have issues after installing Java 21:

1. Make sure you opened a **new terminal window**
2. Run `java -version` and `$env:JAVA_HOME` to verify
3. Try `./mvnw clean` before running again
