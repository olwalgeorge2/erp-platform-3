# Test JDBC connection with same config as Quarkus
$jarPath = "$env:USERPROFILE\.m2\repository\org\postgresql\postgresql\42.7.2\postgresql-42.7.2.jar"
if (-not (Test-Path $jarPath)) {
    Write-Host "Downloading PostgreSQL JDBC driver..." -ForegroundColor Yellow
    $null = New-Item -ItemType Directory -Force -Path (Split-Path $jarPath)
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.2/postgresql-42.7.2.jar" -OutFile $jarPath
}

$javaCode = @'
import java.sql.*;
public class TestJDBC {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://127.0.0.1:5432/erp_identity";
        String user = "erp_user";
        String password = "";
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("✓ Connection successful!");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT current_database(), current_user, version()");
            if (rs.next()) {
                System.out.println("Database: " + rs.getString(1));
                System.out.println("User: " + rs.getString(2));
                System.out.println("Version: " + rs.getString(3));
            }
            conn.close();
        } catch (SQLException e) {
            System.err.println("✗ Connection failed: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            e.printStackTrace();
        }
    }
}
'@

Set-Content -Path "TestJDBC.java" -Value $javaCode
javac TestJDBC.java
java -cp ".;$jarPath" TestJDBC
Remove-Item TestJDBC.java, TestJDBC.class -ErrorAction SilentlyContinue
