# Build System Documentation

> **Last Updated**: November 5, 2025  
> **Gradle Version**: 9.0  
> **Build System**: Kotlin DSL  
> **Project Version**: 0.1.0-SNAPSHOT

## Context
The ERP platform is organized as a **Gradle 9.0 multi-project build** that assembles dozens of Kotlin and Quarkus services, shared libraries, and platform tooling across 11 bounded contexts. This document provides comprehensive guidance on the build system architecture, enabling contributors to extend it safely, consistently, and efficiently.

### Build System Goals
- **Consistency**: Uniform configuration across all modules via convention plugins
- **Scalability**: Support for hundreds of modules without performance degradation
- **Maintainability**: Centralized dependency management and build logic
- **Developer Experience**: Fast incremental builds and clear error messages
- **CI/CD Ready**: Optimized for automated pipelines and deployment workflows

## Toolchain & Standards

### Core Toolchain

| Component | Version | Configuration | Purpose |
|-----------|---------|---------------|---------|
| **Gradle** | 9.0 | `gradle-wrapper.properties` | Build automation and orchestration |
| **Java** | 21 | Gradle toolchain | Runtime environment for all JVM modules |
| **Kotlin** | 2.2.0 | Version catalog | Primary development language |
| **Quarkus** | 3.29.0 | BOM platform import | Cloud-native framework for services |

### Compiler Configuration

#### Kotlin Compilation
- **Progressive Mode**: Enabled to catch potential issues with evolving Kotlin features
- **Strict Nullability**: `-Xjsr305=strict` flag enforces null-safety at Java interop boundaries
- **JVM Target**: Aligned with Java 21 toolchain (automatically configured)
- **Toolchain Enforcement**: All modules compile against the same JDK version to prevent runtime inconsistencies

#### Compilation Features
```kotlin
compilerOptions {
    jvmTarget.set(JvmTarget.fromTarget("21"))
    freeCompilerArgs.addAll(listOf("-Xjsr305=strict"))
    progressiveMode.set(true)
}
```

### Testing Standards

| Framework | Version | Purpose | Usage |
|-----------|---------|---------|-------|
| **JUnit 5** | 5.11.2 | Test execution engine | Default test framework for all modules |
| **MockK** | 1.13.12 | Mocking framework | Kotlin-friendly mocking for unit tests |
| **Kotest** | 5.9.1 | Assertion library | Rich assertions and matchers |
| **Quarkus Test** | 3.29.0 | Integration testing | Container-based integration tests |

### Native Image Support
- **Builder Image**: Quarkus UBI (Universal Base Image) builder
- **Container Build**: Enabled for all native-image modules
- **Build Mode**: Optimized for production deployments with compression
- **Target Platforms**: Linux AMD64 and ARM64 (configurable)

## Multi-Project Structure

### Project Discovery

The build system uses **automatic module discovery** to simplify adding new modules:

```kotlin
// From settings.gradle.kts
rootDir
    .walkTopDown()
    .filter { it.isFile && (it.name == "build.gradle.kts" || it.name == "build.gradle") }
    .map { it.parentFile }
    .filter { it != rootDir }
    .map { it.relativeTo(rootDir).invariantSeparatorsPath }
    .filterNot { segment -> ignoredBuildDirs.any { segment.startsWith(it) } }
    .map { path -> path.replace('/', ':') }
    .forEach { modulePath -> include(modulePath) }
```

### Key Features

#### 1. Type-Safe Project Accessors
`TYPESAFE_PROJECT_ACCESSORS` is enabled, generating compile-time safe references:

```kotlin
// Instead of string-based references
implementation(project(":platform-shared:common-types"))

// Use type-safe accessors
implementation(projects.platformShared.commonTypes)
```

#### 2. Project Organization

| Directory | Purpose | Auto-Included | Example Modules |
|-----------|---------|---------------|-----------------|
| `bounded-contexts/` | Domain-specific services | ✅ Yes | `commerce-ecommerce`, `financial-accounting` |
| `platform-shared/` | Cross-cutting libraries | ✅ Yes | `common-messaging`, `common-security` |
| `platform-infrastructure/` | Infrastructure patterns | ✅ Yes | `cqrs`, `eventing`, `monitoring` |
| `api-gateway/` | API entry point | ✅ Yes | `api-gateway` |
| `portal/` | Web UI application | ✅ Yes | `portal` |
| `tests/` | Test suites | ✅ Yes | `integration`, `contract`, `e2e` |
| `build-logic/` | Convention plugins | ❌ No | Composite build |
| `buildSrc/` | Build scripts | ❌ No | Reserved directory |
| `.gradle/` | Gradle cache | ❌ No | System directory |

#### 3. Module Hierarchy

Modules follow a hierarchical structure for bounded contexts:

```
bounded-contexts/
  ├── commerce/
  │   ├── commerce-b2b/              → :bounded-contexts:commerce:commerce-b2b
  │   ├── commerce-ecommerce/        → :bounded-contexts:commerce:commerce-ecommerce
  │   └── commerce-shared/
  │       ├── catalog-shared/        → :bounded-contexts:commerce:commerce-shared:catalog-shared
  │       └── order-shared/          → :bounded-contexts:commerce:commerce-shared:order-shared
```

### Build Performance

#### Gradle Daemon Configuration
```properties
# gradle.properties
org.gradle.jvmargs=-Xms512m -Xmx2048m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError
```

#### Performance Optimizations
- **Parallel Builds**: Enabled by default in Gradle 9.0
- **Configuration Cache**: Available for deterministic builds
- **Build Cache**: Local and remote caching supported
- **Incremental Compilation**: Kotlin incremental compilation enabled

## Convention Plugins

All service and library modules apply **internal convention plugins** published from the composite build at `build-logic/`. This approach centralizes build configuration and ensures consistency across all modules.

### Available Convention Plugins

| Plugin ID | Applied To | Key Responsibilities | Plugin File |
|-----------|-----------|---------------------|-------------|
| `erp.kotlin-conventions` | Kotlin libraries, domain modules | • Kotlin/JVM plugin configuration<br>• Java 21 toolchain setup<br>• Compiler flags (`-Xjsr305=strict`, progressive mode)<br>• Test framework dependencies (JUnit 5, MockK, Kotest)<br>• `useJUnitPlatform()` test configuration | `KotlinConventionsPlugin.kt` |
| `erp.quarkus-conventions` | Quarkus services | • Inherits all Kotlin conventions<br>• Quarkus plugin and BOM<br>• REST/Jackson/Hibernate bundles<br>• SLF4J logging<br>• Quarkus test configuration<br>• Development mode support | `QuarkusConventionsPlugin.kt` |
| `erp.native-image-conventions` | Native-enabled services | • Inherits all Quarkus conventions<br>• Elytron security integration<br>• OpenTelemetry observability<br>• Caching extensions<br>• PostgreSQL JDBC drivers<br>• Native image build flags<br>• Container build configuration | `NativeImageConventionsPlugin.kt` |

### Plugin Architecture

```
┌─────────────────────────────────────┐
│ erp.native-image-conventions        │
│ (Native-specific extensions)        │
├─────────────────────────────────────┤
│ erp.quarkus-conventions             │
│ (Quarkus framework setup)           │
├─────────────────────────────────────┤
│ erp.kotlin-conventions              │
│ (Base Kotlin/JVM configuration)     │
└─────────────────────────────────────┘
```

### Convention Plugin Details

#### 1. Kotlin Conventions (`erp.kotlin-conventions`)

**Purpose**: Base configuration for all Kotlin modules

**Applied Dependencies**:
```kotlin
dependencies {
    testImplementation("junit-jupiter")
    testImplementation("mockk")
    testImplementation("kotest-runner")
    testImplementation("kotest-assertions")
}
```

**Compiler Configuration**:
- Progressive mode enabled
- Strict JSR-305 null checking
- JVM target aligned with Java 21
- JUnit Platform for test execution

**Usage Example**:
```kotlin
// In a domain module: customer-domain/build.gradle.kts
plugins {
    id("erp.kotlin-conventions")
}

dependencies {
    // Add module-specific dependencies here
    implementation(projects.platformShared.commonTypes)
}
```

#### 2. Quarkus Conventions (`erp.quarkus-conventions`)

**Purpose**: Standard Quarkus service configuration

**Applied Dependencies**:
- Quarkus BOM (Bill of Materials)
- Core Quarkus bundles (REST, Jackson, Hibernate, Validator)
- SLF4J API for logging
- Quarkus Arc (CDI container)

**Additional Features**:
- Quarkus dev mode support
- REST endpoint generation
- Dependency injection configuration
- Test resource management

**Usage Example**:
```kotlin
// In an application module: customer-crm/build.gradle.kts
plugins {
    id("erp.quarkus-conventions")
}

dependencies {
    implementation(projects.customerRelation.customerDomain)
    implementation(projects.platformShared.commonSecurity)
}
```

#### 3. Native Image Conventions (`erp.native-image-conventions`)

**Purpose**: Production-ready native executable configuration

**Additional Dependencies**:
- Quarkus Elytron Security
- Quarkus OpenTelemetry
- Quarkus Cache
- PostgreSQL JDBC driver

**Native Build Configuration**:
- Container-based builds enabled
- UBI builder image configured
- Verbose native-image logging
- Optimized for production size

**Usage Example**:
```kotlin
// In a service module: commerce-ecommerce/build.gradle.kts
plugins {
    id("erp.native-image-conventions")
}

// Native image will be built with:
// ./gradlew :bounded-contexts:commerce:commerce-ecommerce:build -Dquarkus.package.type=native
```

### Benefits of Convention Plugins

✅ **Consistency**: All modules follow the same standards  
✅ **Maintainability**: Changes propagate to all modules automatically  
✅ **Simplicity**: Module `build.gradle.kts` files stay minimal (typically <20 lines)  
✅ **Type Safety**: Compile-time verification of build scripts  
✅ **Discoverability**: Clear convention structure aids onboarding  

### Creating a New Module

**Step 1**: Choose the appropriate convention based on module type:
- Domain/Library modules → `erp.kotlin-conventions`
- Service modules → `erp.quarkus-conventions`
- Production services → `erp.native-image-conventions`

**Step 2**: Create minimal `build.gradle.kts`:
```kotlin
plugins {
    id("erp.quarkus-conventions")  // or appropriate convention
}

dependencies {
    // Add only module-specific dependencies
    implementation(projects.platformShared.commonTypes)
}
```

**Step 3**: Verify inclusion with `./gradlew projects`

## Dependency Management

### Version Catalog (`gradle/libs.versions.toml`)

All dependencies are centrally managed using **Gradle Version Catalogs**, providing:
- Single source of truth for versions
- Type-safe dependency references
- Consistent versioning across all modules
- Easy version updates and auditing

### Catalog Structure

#### Version Declarations
```toml
[versions]
java = "21"
kotlin = "2.2.0"
quarkus = "3.29.0"
junit = "5.11.2"
mockk = "1.13.12"
kotest = "5.9.1"
slf4j = "2.0.16"
```

#### Library Definitions
```toml
[libraries]
quarkus-bom = { group = "io.quarkus.platform", name = "quarkus-bom", version.ref = "quarkus" }
quarkus-rest = { group = "io.quarkus", name = "quarkus-rest", version.ref = "quarkus" }
quarkus-messaging = { group = "io.quarkus", name = "quarkus-messaging", version.ref = "quarkus" }
quarkus-messaging-kafka = { group = "io.quarkus", name = "quarkus-messaging-kafka", version.ref = "quarkus" }
quarkus-kafka-client = { group = "io.quarkus", name = "quarkus-kafka-client", version.ref = "quarkus" }
junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "junit" }
# ... more libraries
```

#### Dependency Bundles
```toml
[bundles]
quarkus-core = [
  "quarkus-arc",
  "quarkus-rest",
  "quarkus-rest-jackson",
  "quarkus-logging-json",
  "quarkus-hibernate-orm",
  "quarkus-hibernate-validator",
  "quarkus-messaging",
  "quarkus-messaging-kafka",
  "quarkus-kafka-client"
]
testing-core = [
  "junit-jupiter",
  "mockk",
  "kotest-runner",
  "kotest-assertions"
]
```

#### Plugin Definitions
```toml
[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-allopen = { id = "org.jetbrains.kotlin.plugin.allopen", version.ref = "kotlin" }
quarkus = { id = "io.quarkus", version.ref = "quarkus" }
```

### Using the Version Catalog

#### In Module Build Scripts
```kotlin
dependencies {
    // Single library reference
    implementation(libs.quarkus.rest)
    
    // Bundle reference (multiple libraries at once)
    implementation(libs.bundles.quarkus.core)
    
    // Test dependencies
    testImplementation(libs.bundles.testing.core)
    
    // Type-safe project references
    implementation(projects.platformShared.commonTypes)
}
```

#### In Root Build Script
```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.quarkus) apply false
}
```

### Platform BOM Management

#### Quarkus BOM
The platform imports the Quarkus Bill of Materials to ensure version alignment:

```kotlin
dependencies {
    implementation(platform(libs.quarkus.bom))
    // All quarkus-* dependencies inherit versions from BOM
}
```

**Benefits**:
- Consistent Quarkus extension versions
- Tested dependency combinations
- Simplified dependency declarations
- Automatic transitive dependency management

### Adding New Dependencies

#### Step 1: Add Version (if new)
```toml
[versions]
my-library = "1.2.3"
```

#### Step 2: Define Library
```toml
[libraries]
my-library = { group = "com.example", name = "my-library", version.ref = "my-library" }
```

#### Step 3: (Optional) Create Bundle
```toml
[bundles]
my-feature = ["my-library", "related-library"]
```

#### Step 4: Use in Module
```kotlin
dependencies {
    implementation(libs.my.library)
}
```

### Dependency Guidelines

✅ **DO**:
- Add all dependencies to the version catalog
- Use semantic versioning references
- Group related dependencies into bundles
- Document breaking changes in version updates
- Test dependency updates in CI before merging

❌ **DON'T**:
- Hard-code versions in module build scripts
- Add `implementation()` dependencies directly to convention plugins (use catalog)
- Use snapshot versions in production modules
- Mix dependency declaration styles

### Repository Configuration

```kotlin
// In settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()  // Primary repository
    }
}
```

**Repository Strategy**:
- `FAIL_ON_PROJECT_REPOS`: Prevents individual modules from declaring repositories
- Centralized repository configuration in `settings.gradle.kts`
- Maven Central as the primary dependency source
- Additional repositories can be added for specific corporate artifacts

## Common Workflows

### Development Workflows

#### Build All Modules
```powershell
# Full build (compile + test + assemble)
./gradlew build

# Fast build without tests
./gradlew assemble

# Clean and build
./gradlew clean build
```

#### Running Tests
```powershell
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :bounded-contexts:commerce:commerce-ecommerce:test

# Run tests matching pattern
./gradlew test --tests "*CustomerService*"

# Run tests with detailed output
./gradlew test --info

# Run tests with parallel execution
./gradlew test --parallel --max-workers=4
```

#### Development Mode
```powershell
# Start Quarkus dev mode for a service
./gradlew :bounded-contexts:commerce:commerce-ecommerce:quarkusDev

# Dev mode features:
# - Live reload on code changes
# - Dev UI at http://localhost:8080/q/dev/
# - Continuous testing
# - Interactive debugging
```

#### Code Quality
```powershell
# Format Kotlin code with ktlint
./ktlint.bat -F

# Check code formatting
./ktlint.bat

# Run ktlint on specific module
./ktlint.bat "bounded-contexts/commerce/**/*.kt"
```

#### Project Navigation
```powershell
# List all projects
./gradlew projects

# Show project dependencies
./gradlew :module-name:dependencies

# Show project dependency tree
./gradlew :module-name:dependencies --configuration runtimeClasspath

# Analyze dependency insight
./gradlew :module-name:dependencyInsight --dependency kotlin-stdlib
```

### Building Native Images

```powershell
# Build native executable for a module
./gradlew :bounded-contexts:commerce:commerce-ecommerce:build -Dquarkus.package.type=native

# Build with container-based build (requires Docker)
./gradlew :module-name:build -Dquarkus.package.type=native -Dquarkus.native.container-build=true

# Build all native-enabled services
./gradlew build -Dquarkus.package.type=native
```

**Native Build Output**:
- Location: `module-name/build/module-name-0.1.0-SNAPSHOT-runner`
- Executable: Directly runnable native binary
- Size: Typically 50-100MB (optimized)

### Dependency Updates

```powershell
# Check for dependency updates (requires dependency-analysis plugin)
./gradlew dependencyUpdates

# Refresh dependencies
./gradlew --refresh-dependencies

# Force dependency resolution
./gradlew build --refresh-dependencies
```

### Build Cache Management

```powershell
# Clean build cache
./gradlew clean

# Clean specific module
./gradlew :module-name:clean

# Clean all build directories
./gradlew cleanBuildCache
```

### Troubleshooting

```powershell
# Debug build issues with stack traces
./gradlew build --stacktrace

# Full debug information
./gradlew build --debug > build-debug.log

# Profile build performance
./gradlew build --profile
# Report: build/reports/profile/profile-*.html

# Stop Gradle daemon
./gradlew --stop

# Check Gradle version
./gradlew --version
```

### Multi-Module Operations

```powershell
# Build specific bounded context
./gradlew :bounded-contexts:commerce:build

# Build all application modules
./gradlew :bounded-contexts:*:*-application:build

# Test all domain modules
./gradlew :bounded-contexts:*:*-domain:test

# Parallel execution for faster builds
./gradlew build --parallel
```

### Configuration Cache (Advanced)

```powershell
# Enable configuration cache for faster builds
./gradlew build --configuration-cache

# Invalidate configuration cache
./gradlew build --configuration-cache --rerun-tasks
```

**Configuration Cache Benefits**:
- ⚡ Up to 10x faster configuration phase
- 📦 Cached task graph for incremental builds
- 🔄 Reused across multiple builds

### Task Information

```powershell
# List all available tasks
./gradlew tasks

# List tasks with details
./gradlew tasks --all

# Show task details
./gradlew help --task test

# Dry run (show task execution plan)
./gradlew build --dry-run
```

## CI/CD Integration

### Deployment Infrastructure

The `deployment/` directory contains all automation for building, testing, and deploying the platform:

```
deployment/
├── ci/                 # CI/CD pipeline definitions
│   ├── jenkins/       # Jenkinsfile configurations
│   ├── github/        # GitHub Actions workflows
│   └── azure/         # Azure Pipelines YAML
├── docker/            # Dockerfile templates
├── helm/              # Kubernetes Helm charts
├── kubernetes/        # K8s manifests
└── terraform/         # Infrastructure as Code
```

### CI Pipeline Integration

#### Expected Build Outputs

| Module Type | Output Artifact | Location | Format |
|-------------|----------------|----------|--------|
| Quarkus Service (JVM) | Uber JAR | `module/build/*-runner.jar` | Executable JAR |
| Quarkus Service (Native) | Native executable | `module/build/*-runner` | Binary |
| Kotlin Library | JAR | `module/build/libs/*.jar` | Library JAR |
| Portal Application | Static assets | `portal/build/dist/` | HTML/JS/CSS |

#### CI Task Examples

```powershell
# CI Build Task
./gradlew clean build --no-daemon --parallel

# CI Test with Reports
./gradlew test jacocoTestReport --no-daemon
# Reports: build/reports/jacoco/test/html/index.html

# CI Native Build
./gradlew build -Dquarkus.package.type=native --no-daemon

# CI Docker Image Build
./gradlew :module-name:build
docker build -t registry.example.com/module-name:$VERSION .
```

### Docker Integration

#### JVM Dockerfile Pattern
```dockerfile
FROM eclipse-temurin:21-jre
COPY build/*-runner.jar /app/application.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
```

#### Native Dockerfile Pattern
```dockerfile
FROM registry.access.redhat.com/ubi9/ubi-minimal:9.3
COPY build/*-runner /app/application
EXPOSE 8080
ENTRYPOINT ["/app/application"]
```

### Gradle Daemon Considerations

**CI/CD Best Practice**: Use `--no-daemon` flag to prevent daemon issues:

```powershell
./gradlew build --no-daemon
```

**Reasons**:
- Prevents memory leaks in long-running CI jobs
- Ensures clean build environment per execution
- Avoids daemon version conflicts
- Simplifies resource cleanup

### Build Artifacts Management

```powershell
# Generate build scan for CI analysis
./gradlew build --scan

# Publish artifacts to local repo
./gradlew publishToMavenLocal

# Generate SBOM (Software Bill of Materials)
./gradlew cyclonedxBom  # If plugin enabled
```

### Performance Optimization for CI

#### Gradle Configuration for CI
```properties
# gradle.properties (CI-specific)
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.daemon=false
org.gradle.configureondemand=false
```

#### Build Cache Strategy
- **Local Cache**: Enabled for developer machines
- **Remote Cache**: Configure for CI to share artifacts across builds
- **Cache Key**: Based on inputs (source files, dependencies, configuration)

### Pipeline Hooks

CI pipelines can call Gradle tasks at different stages:

| Stage | Gradle Task | Purpose |
|-------|------------|---------|
| **Compile** | `./gradlew compileKotlin` | Syntax validation |
| **Test** | `./gradlew test` | Unit test execution |
| **Integration Test** | `./gradlew integrationTest` | Integration test execution |
| **Quality Gate** | `./gradlew check` | Static analysis, formatting |
| **Package** | `./gradlew assemble` | Build artifacts |
| **Native Build** | `./gradlew build -Dquarkus.package.type=native` | Native executable |
| **Publish** | `./gradlew publish` | Artifact repository upload |

### Example CI Workflow (GitHub Actions)

```yaml
name: Build and Test

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '21'
      
      - name: Setup Gradle
        uses: gradle/gradle-build-action@v2
        with:
          cache-read-only: false
      
      - name: Build with Gradle
        run: ./gradlew build --no-daemon --parallel
      
      - name: Upload Test Reports
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-reports
          path: '**/build/reports/tests/'
```

### Adding New CI Tasks

When adding new Gradle tasks for CI automation:

1. **Document the Task**: Add description and group
   ```kotlin
   tasks.register("myCustomTask") {
       group = "ci"
       description = "Custom task for CI pipeline"
       doLast {
           // Task implementation
       }
   }
   ```

2. **Ensure Idempotency**: Tasks should be repeatable
3. **Add to Pipeline**: Update CI configuration files
4. **Test Locally**: Verify with `--no-daemon` flag
5. **Document**: Update this document with new task information

## Extending the Build System

### Creating a New Module

#### Step-by-Step Guide

**1. Create Module Directory Structure**
```powershell
# For a new domain module
mkdir bounded-contexts\customer-relation\customer-loyalty

# For a shared library
mkdir platform-shared\common-validation
```

**2. Create `build.gradle.kts`**
```kotlin
// bounded-contexts/customer-relation/customer-loyalty/build.gradle.kts
plugins {
    id("erp.kotlin-conventions")  // Choose appropriate convention
}

dependencies {
    // Add module-specific dependencies
    implementation(projects.platformShared.commonTypes)
    implementation(projects.customerRelation.customerShared)
}
```

**3. Verify Module Inclusion**
```powershell
# List all projects to confirm new module is included
./gradlew projects

# Should show:
# :bounded-contexts:customer-relation:customer-loyalty
```

**4. Create Source Structure**
```
customer-loyalty/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── kotlin/
    │   │   └── com/example/erp/customer/loyalty/
    │   └── resources/
    └── test/
        ├── kotlin/
        │   └── com/example/erp/customer/loyalty/
        └── resources/
```

**5. Test the Module**
```powershell
# Compile the new module
./gradlew :bounded-contexts:customer-relation:customer-loyalty:compileKotlin

# Run tests
./gradlew :bounded-contexts:customer-relation:customer-loyalty:test
```

### Module Type Guidelines

#### Domain Module
```kotlin
plugins {
    id("erp.kotlin-conventions")
}

dependencies {
    // Domain modules should have minimal dependencies
    implementation(projects.platformShared.commonTypes)
    
    // No infrastructure dependencies!
    // No framework dependencies!
}
```

#### Application Module
```kotlin
plugins {
    id("erp.quarkus-conventions")
}

dependencies {
    // Reference the domain module
    implementation(projects.boundedContexts.customerRelation.customerDomain)
    
    // Platform utilities
    implementation(projects.platformShared.commonSecurity)
    implementation(projects.platformShared.commonMessaging)
}
```

#### Infrastructure Module
```kotlin
plugins {
    id("erp.quarkus-conventions")
}

dependencies {
    // Domain and application layers
    implementation(projects.boundedContexts.customerRelation.customerDomain)
    implementation(projects.boundedContexts.customerRelation.customerApplication)
    
    // External integrations
    implementation(libs.quarkus.jdbc.postgresql)
    implementation(libs.quarkus.hibernate.orm)
}
```

#### Native Service Module
```kotlin
plugins {
    id("erp.native-image-conventions")
}

dependencies {
    // Full vertical slice
    implementation(projects.boundedContexts.customerRelation.customerDomain)
    implementation(projects.boundedContexts.customerRelation.customerApplication)
    implementation(projects.boundedContexts.customerRelation.customerInfrastructure)
}
```

### Adding Custom Build Logic

#### Option 1: Module-Specific Logic
For logic needed in a single module, add directly to `build.gradle.kts`:

```kotlin
plugins {
    id("erp.quarkus-conventions")
}

// Custom task
tasks.register("generateOpenApiSpec") {
    group = "documentation"
    description = "Generates OpenAPI specification"
    doLast {
        // Task implementation
    }
}

dependencies {
    // Module dependencies
}
```

#### Option 2: Shared Build Logic (Recommended)
For logic needed across multiple modules, add to `build-logic`:

**Step 1**: Create new plugin in `build-logic/src/main/kotlin/`
```kotlin
// build-logic/src/main/kotlin/OpenApiConventionsPlugin.kt
package erp.platform.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class OpenApiConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        // Shared build logic here
        tasks.register("generateOpenApiSpec") {
            group = "documentation"
            description = "Generates OpenAPI specification"
            doLast {
                // Reusable implementation
            }
        }
    }
}
```

**Step 2**: Register plugin in `build-logic/build.gradle.kts`
```kotlin
gradlePlugin {
    plugins {
        create("openapi-conventions") {
            id = "erp.openapi-conventions"
            implementationClass = "erp.platform.buildlogic.OpenApiConventionsPlugin"
        }
    }
}
```

**Step 3**: Use in modules
```kotlin
plugins {
    id("erp.quarkus-conventions")
    id("erp.openapi-conventions")
}
```

### Modifying Existing Conventions

**Best Practice**: Update convention plugins rather than duplicating logic

```kotlin
// build-logic/src/main/kotlin/QuarkusConventionsPlugin.kt
class QuarkusConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        // Apply base conventions
        pluginManager.apply("erp.kotlin-conventions")
        pluginManager.apply("io.quarkus")
        
        // Add Quarkus-specific configuration
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        
        dependencies {
            add("implementation", platform(libs.findLibrary("quarkus-bom").get()))
            add("implementation", libs.findBundle("quarkus-core").get())
            // Add new shared dependencies here
        }
    }
}
```

### Dependency Scope Best Practices

```kotlin
dependencies {
    // API: Exposes types in public API
    api(projects.platformShared.commonTypes)
    
    // Implementation: Internal dependency
    implementation(projects.customerRelation.customerDomain)
    
    // CompileOnly: Compile-time only (e.g., annotations)
    compileOnly(libs.annotations)
    
    // RuntimeOnly: Runtime-only dependency
    runtimeOnly(libs.postgresql.driver)
    
    // Test dependencies
    testImplementation(libs.bundles.testing.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}
```

### Multi-Platform Support (Future)

If targeting multiple platforms (JVM, Native, JS):

```kotlin
plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js(IR) {
        browser()
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.platformShared.commonTypes)
            }
        }
        val jvmMain by getting
        val jsMain by getting
    }
}
```

### Build Script Debugging

```powershell
# Debug build script configuration
./gradlew help --debug

# Show effective build configuration
./gradlew properties

# Verify plugin application
./gradlew buildEnvironment

# Check dependency resolution
./gradlew dependencies --configuration compileClasspath
```

### Common Pitfalls to Avoid

❌ **DON'T**:
- Hard-code versions in module build scripts
- Create circular dependencies between modules
- Apply plugins without understanding their impact
- Add infrastructure dependencies to domain modules
- Duplicate build logic across modules

✅ **DO**:
- Use convention plugins for shared configuration
- Reference dependencies through version catalog
- Keep domain modules framework-independent
- Document custom build logic
- Test build changes locally before pushing

### Build System Governance

#### Review Checklist for Build Changes

- [ ] Version catalog updated for new dependencies
- [ ] Convention plugins updated if shared logic needed
- [ ] Build tested with `./gradlew clean build`
- [ ] CI pipeline compatibility verified
- [ ] Documentation updated (this file)
- [ ] Dependency licenses reviewed
- [ ] Performance impact assessed
- [ ] Team notified of breaking changes

#### Build Configuration Ownership

| Component | Owner | Approval Required |
|-----------|-------|------------------|
| Convention Plugins | Platform Team | Yes |
| Version Catalog | Platform Team | Yes |
| Root build.gradle.kts | Platform Team | Yes |
| Module build.gradle.kts | Module Team | No (within guidelines) |
| Dependency Additions | Module Team | No (documented in catalog) |
| Major Gradle Upgrades | Platform Team | Yes |

---

## Troubleshooting Guide

### Common Build Issues

#### Issue: "Could not resolve dependency"

**Symptoms**: Dependency resolution fails during build

**Solutions**:
```powershell
# Refresh dependencies
./gradlew build --refresh-dependencies

# Clear dependency cache
rm -r $HOME/.gradle/caches/

# Check repository connectivity
./gradlew dependencies --configuration compileClasspath
```

#### Issue: "Java version mismatch"

**Symptoms**: Build fails with incorrect Java version

**Solutions**:
```powershell
# Check Gradle's detected Java
./gradlew -version

# Verify toolchain configuration
./gradlew -q javaToolchains

# Force specific Java home (if needed)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
./gradlew build
```

#### Issue: "Gradle daemon issues"

**Symptoms**: Builds hang or fail intermittently

**Solutions**:
```powershell
# Stop all daemons
./gradlew --stop

# Clear daemon logs
Remove-Item -Recurse -Force $HOME\.gradle\daemon\

# Rebuild with no daemon
./gradlew build --no-daemon
```

#### Issue: "Out of memory errors"

**Symptoms**: Build fails with `OutOfMemoryError`

**Solutions**:
```properties
# Increase heap in gradle.properties
org.gradle.jvmargs=-Xms1g -Xmx4g -XX:MaxMetaspaceSize=1g
```

```powershell
# Or set for single build
./gradlew build -Dorg.gradle.jvmargs="-Xmx4g"
```

#### Issue: "Circular dependency detected"

**Symptoms**: Module dependency graph has cycles

**Solutions**:
1. Review module dependencies with `./gradlew :module:dependencies`
2. Refactor to break circular dependency (extract shared module)
3. Use event-driven communication instead of direct dependencies

#### Issue: "Task output caching issues"

**Symptoms**: Builds don't use cached outputs

**Solutions**:
```powershell
# Clear build cache
./gradlew cleanBuildCache

# Disable cache for debugging
./gradlew build --no-build-cache

# Check cache configuration
./gradlew buildCacheDebugInfo
```

#### Issue: "Kotlin compilation fails"

**Symptoms**: Kotlin compiler errors or crashes

**Solutions**:
```powershell
# Clean and rebuild
./gradlew clean build

# Check Kotlin version compatibility
./gradlew dependencies | Select-String "kotlin"

# Verbose Kotlin compilation
./gradlew build -Dkotlin.compiler.execution.strategy=in-process
```

### Performance Optimization

#### Build Analysis

```powershell
# Generate build profile
./gradlew build --profile --offline --rerun-tasks

# Open report (Windows)
start build\reports\profile\profile-*.html
```

#### Performance Tips

1. **Enable Parallel Builds**
   ```properties
   # gradle.properties
   org.gradle.parallel=true
   org.gradle.workers.max=4
   ```

2. **Enable Configuration Cache**
   ```powershell
   ./gradlew build --configuration-cache
   ```

3. **Use Build Cache**
   ```properties
   # gradle.properties
   org.gradle.caching=true
   ```

4. **Optimize Test Execution**
   ```kotlin
   tasks.withType<Test> {
       maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
   }
   ```

5. **Exclude Unnecessary Tasks**
   ```powershell
   # Skip tests for faster builds
   ./gradlew build -x test
   ```

### Diagnostic Commands

```powershell
# Full system diagnostics
./gradlew --version
./gradlew properties
./gradlew projects
./gradlew tasks --all
./gradlew dependencies
./gradlew buildEnvironment

# Debugging specific issues
./gradlew build --info        # Detailed logging
./gradlew build --debug       # Debug logging
./gradlew build --stacktrace  # Show stack traces
./gradlew build --scan        # Build scan (if enabled)
```

### Getting Help

1. **Check Gradle Documentation**: https://docs.gradle.org
2. **Quarkus Guides**: https://quarkus.io/guides/
3. **Kotlin Documentation**: https://kotlinlang.org/docs/
4. **Team Wiki**: `docs/` directory for project-specific guidance
5. **Build Scan**: Enable for detailed build analysis

---

## Best Practices Summary

### Development Best Practices

✅ **Always**:
- Use type-safe project accessors (`projects.*`)
- Reference dependencies through version catalog
- Apply appropriate convention plugins
- Run tests before committing
- Keep domain modules framework-free
- Document build script changes

❌ **Never**:
- Hard-code dependency versions
- Create circular module dependencies
- Add repositories in module build scripts
- Commit build outputs to version control
- Skip tests to "save time"
- Bypass convention plugins

### Module Organization

```
module-name/
├── build.gradle.kts           # Minimal, uses conventions
├── src/
│   ├── main/
│   │   ├── kotlin/           # Kotlin source code
│   │   │   └── com/example/erp/...
│   │   └── resources/        # Resources, configs
│   │       └── application.properties
│   └── test/
│       ├── kotlin/           # Test source code
│       │   └── com/example/erp/...
│       └── resources/        # Test resources
└── build/                    # Generated (gitignored)
```

### Dependency Declaration Order

```kotlin
dependencies {
    // 1. API dependencies (exposed in public API)
    api(projects.platformShared.commonTypes)
    
    // 2. Implementation dependencies (internal)
    implementation(projects.customerRelation.customerDomain)
    implementation(libs.bundles.quarkus.core)
    
    // 3. Compile-only dependencies
    compileOnly(libs.annotations)
    
    // 4. Runtime dependencies
    runtimeOnly(libs.postgresql.driver)
    
    // 5. Test dependencies
    testImplementation(libs.bundles.testing.core)
    testImplementation(projects.platformShared.testFixtures)
    testRuntimeOnly(libs.junit.platform.launcher)
}
```

---

## Migration Guide

### Upgrading Gradle Version

```powershell
# Update wrapper to latest version
./gradlew wrapper --gradle-version=9.1

# Verify upgrade
./gradlew --version

# Test build with new version
./gradlew clean build --warning-mode=all
```

### Upgrading Kotlin Version

**Step 1**: Update version catalog
```toml
[versions]
kotlin = "2.3.0"  # New version
```

**Step 2**: Test compilation
```powershell
./gradlew clean compileKotlin
```

**Step 3**: Run full test suite
```powershell
./gradlew test
```

**Step 4**: Address deprecations
```powershell
./gradlew build --warning-mode=all
```

### Upgrading Quarkus Version

**Step 1**: Update version catalog
```toml
[versions]
quarkus = "3.30.0"  # New version
```

**Step 2**: Check migration guide
Visit: https://github.com/quarkusio/quarkus/wiki/Migration-Guides

**Step 3**: Update platform BOM
```powershell
./gradlew dependencies --refresh-dependencies
```

**Step 4**: Test services
```powershell
./gradlew :bounded-contexts:commerce:commerce-ecommerce:quarkusTest
```

---

## References

### Documentation
- **Architecture Overview**: `docs/ARCHITECTURE.md`
- **Context Map**: `docs/CONTEXT_MAP.md`
- **Deployment Guide**: `deployment/README.md`
- **Testing Strategy**: `tests/README.md`
- **ADRs**: `docs/adr/` (Architecture Decision Records)

### External Resources
- [Gradle Documentation](https://docs.gradle.org/current/userguide/userguide.html)
- [Kotlin DSL Reference](https://docs.gradle.org/current/userguide/kotlin_dsl.html)
- [Quarkus Guides](https://quarkus.io/guides/)
- [Version Catalogs](https://docs.gradle.org/current/userguide/platforms.html)
- [Convention Plugins](https://docs.gradle.org/current/userguide/custom_plugins.html)

### Quick Reference

| Task | Command |
|------|---------|
| Full Build | `./gradlew build` |
| Fast Build | `./gradlew assemble -x test` |
| Run Tests | `./gradlew test` |
| Dev Mode | `./gradlew :module:quarkusDev` |
| Format Code | `./ktlint.bat -F` |
| List Projects | `./gradlew projects` |
| Show Dependencies | `./gradlew :module:dependencies` |
| Clean Build | `./gradlew clean build` |
| Native Build | `./gradlew build -Dquarkus.package.type=native` |
| Stop Daemon | `./gradlew --stop` |

---

> **Note**: This document is maintained by the Platform Team. All build system changes should be documented here. For questions or suggestions, contact the Platform Team or open an issue in the repository.

**Last Review**: November 5, 2025  
**Next Review**: Quarterly or on major version upgrades
