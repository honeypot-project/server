package data;

import util.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLConnection {

  public static Connection getConnection() throws SQLException {
    return DriverManager.getConnection(
      DatabaseConfig.get("db.url").concat(DatabaseConfig.get("db.database")),
      DatabaseConfig.get("db.user"),
      DatabaseConfig.get("db.password")
    );
  }

  public static Connection getRootConnection() throws SQLException {
    return DriverManager.getConnection(
      DatabaseConfig.get("db.url"),
      DatabaseConfig.get("db.user"),
      DatabaseConfig.get("db.password")
    );
  }
}
