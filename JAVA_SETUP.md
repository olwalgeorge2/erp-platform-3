# Development Environment Setup

## Java Configuration

The project is now configured to use Java 21 (Eclipse Adoptium) permanently:

- **JAVA_HOME**: `C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot`
- **Java Version**: OpenJDK 21.0.9 (Temurin-21.0.9+10-LTS)

### Environment Variables

The following environment variables have been set permanently at the user level:

```
JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot
PATH=%JAVA_HOME%\bin;... (Java bin added to PATH)
```

### Build Commands

You can now use the following commands to build the project:

1. **Using the custom wrapper** (recommended):
   ```cmd
   .\gradle.cmd clean build
   ```

2. **Using Java directly**:
   ```cmd
   java -Xmx64m -Xms64m -classpath "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain clean build
   ```

### VS Code Configuration

VS Code is configured with the correct Java paths in `.vscode/settings.json`:
- Java Language Server uses the permanent Java installation
- Kotlin Language Server uses the correct Java home
- Build configuration is properly set up

### Verification

To verify the setup works correctly:

```powershell
# Check environment variables
$env:JAVA_HOME
java --version

# Test Gradle build
.\gradle.cmd --version
```

## Notes

- The environment variables persist across terminal sessions and system reboots
- VS Code will automatically use the correct Java installation
- The Gradle cache corruption issue has been resolved
- All dependencies should resolve correctly now