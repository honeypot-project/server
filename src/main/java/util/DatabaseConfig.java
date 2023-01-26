package util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConfig {
  private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());
  private static final DatabaseConfig INSTANCE = new DatabaseConfig("/config/sql.properties");
  private final Properties properties;

  public static DatabaseConfig getInstance() {
    return INSTANCE;
  }

  private DatabaseConfig(String path) {
    properties = new Properties();

    try (InputStream is = DatabaseConfig.class.getResourceAsStream(path)) {
      properties.load(is);
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "Unable to load config", ex);
      throw new HoneypotException("Unable to load config");
    }
  }

  public static String get(String key) {
    return getInstance().readSetting(key);
  }

  public String readSetting(String key) {
    return readSetting(key, null);
  }

  public String readSetting(String key, String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }


}
