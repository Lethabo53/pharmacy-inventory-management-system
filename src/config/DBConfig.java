package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database configuration. Reads db.properties from the working directory
 * (or the path given with -Dpims.config=/path/db.properties).
 */
public final class DBConfig {
    private static String url  = "jdbc:mysql://localhost:3306/pims_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static String user = "root";
    private static String pass = "";

    static {
        String path = System.getProperty("pims.config", "db.properties");
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(path)) {
            p.load(in);
            url  = p.getProperty("db.url", url);
            user = p.getProperty("db.user", user);
            pass = p.getProperty("db.password", pass);
        } catch (IOException ignored) {
            // use defaults
        }
    }

    private DBConfig() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }
}
