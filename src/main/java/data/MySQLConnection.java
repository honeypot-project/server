package data;

import util.DatabaseConfig;

import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLConnection {

  public static java.sql.Connection getConnection() throws SQLException {
    return DriverManager.getConnection(
      DatabaseConfig.get("db.url"),
      DatabaseConfig.get("db.user"),
      DatabaseConfig.get("db.password")
    );
  }
}
