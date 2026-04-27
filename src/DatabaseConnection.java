import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL      = System.getenv("DB_URL");
    private static final String USER     = System.getenv("DB_USER");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    private static Connection connection = null;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Connected to database.");
            } catch (SQLException e) {
                System.out.println("❌ Database connection failed: " + e.getMessage());
                System.out.println("   Make sure MySQL is running and your password is correct in DatabaseConnection.java");
            }
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                System.out.println("❌ Error closing connection: " + e.getMessage());
            }
        }
    }
}