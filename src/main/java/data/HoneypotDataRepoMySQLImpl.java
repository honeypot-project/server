package data;

import domain.Challenge;
import domain.HoneypotUser;
import util.DatabaseConfig;
import util.HoneypotException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class HoneypotDataRepoMySQLImpl implements HoneypotDataRepo {
  @Override
  public void setup() {
    try (Connection conn = MySQLConnection.getRootConnection()) {
      conn.prepareStatement("CREATE DATABASE IF NOT EXISTS honeypot").execute();
    } catch (SQLException e) {
      throw new HoneypotException("Unable to create database");
    }
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + DatabaseConfig.get("db.database") + ".users (" +
           "id INT NOT NULL AUTO_INCREMENT," +
           "username VARCHAR(255) NOT NULL," +
           "password VARCHAR(500) NOT NULL," +
           "disabled BOOLEAN default false," +
           "administrator BOOLEAN default false," +
           "last_action datetime null," +
           "img_id VARCHAR(500) null ," +
           "PRIMARY KEY (id)," +
           "UNIQUE (username)" +
           ")")) {
      stmt.execute();
    } catch (SQLException e) {
      throw new HoneypotException("Unable to create users table");
    }
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + DatabaseConfig.get("db.database") + ".challenges (" +
           "challenge_id INT NOT NULL," +
           "flag VARCHAR(255) NOT NULL," +
           "PRIMARY KEY (challenge_id)," +
           "UNIQUE (flag)" +
           ")")) {
      stmt.execute();
    } catch (SQLException e) {
      throw new HoneypotException("Unable to create challenges table");
    }

    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + DatabaseConfig.get("db.database") + ".solved_challenges (" +
           "user_id INT NOT NULL," +
           "solved_challenge_id INT NOT NULL," +
           "PRIMARY KEY (user_id, solved_challenge_id)," +
           "FOREIGN KEY (user_id) REFERENCES users(id)," +
           "FOREIGN KEY (solved_challenge_id) REFERENCES challenges(challenge_id)" +
           ")")) {
      stmt.execute();
    } catch (SQLException e) {
      throw new HoneypotException("Unable to create user_challenges table");
    }

    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("select * from " + DatabaseConfig.get("db.database") + ".challenges")
    ) {
      ResultSet resultSet = stmt.executeQuery();
      if (!resultSet.next()) {
        insertFlag(1, "FLAG{XSS-IS-EASY-PEASY-LEMON-SQEEZY}");
        insertFlag(2, "FLAG{INSECURE-COOKI3-VULNERABILITY}");
        insertFlag(3, "FLAG{DEFAULT-CREDENTIALS-ARE-A-NO-GO}");
        insertFlag(4, "FLAG{YOU-ARE-A-BEAST-GOOD-JOB-NERD}");
        insertFlag(5, "FLAG{YOU-FOUND-ME-WELL-DONE}");
      }


    } catch (SQLException ex) {
      throw new HoneypotException("Couldn't get existing values of table challenges");
    }
  }

  private void insertFlag(int id, String flag) {
    try (Connection conn2 = MySQLConnection.getConnection();
         PreparedStatement stmt2 = conn2.prepareStatement("INSERT INTO " + DatabaseConfig.get("db.database") + ".challenges (challenge_id, flag)" +
           "VALUES (?, ?);")
    ) {
      stmt2.setInt(1, id);
      stmt2.setString(2, flag);
      stmt2.execute();
    } catch (SQLException e) {
      throw new HoneypotException("Unable to insert data");
    }
  }

  @Override
  public HoneypotUser getUserByUsername(String username) {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
      stmt.setString(1, username);
      ResultSet resultSet = stmt.executeQuery();
      if (resultSet.next()) {
        LocalDateTime lastAction = null;
        if (resultSet.getTimestamp("last_action") != null) {
          lastAction = resultSet.getTimestamp("last_action").toLocalDateTime();
        }
        return new HoneypotUser(
          resultSet.getInt("id"),
          resultSet.getString("username"),
          resultSet.getString("password"),
          resultSet.getBoolean("disabled"),
          resultSet.getBoolean("administrator"),
          lastAction,
          resultSet.getString("img_id"));
      }
      return null;
    } catch (SQLException e) {
      throw new HoneypotException("Unable to get user");
    }
  }

  @Override
  public HoneypotUser getUser(int userId) {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {
      stmt.setInt(1, userId);
      ResultSet resultSet = stmt.executeQuery();
      if (resultSet.next()) {
        return new HoneypotUser(
          resultSet.getInt("id"),
          resultSet.getString("username"),
          resultSet.getString("password"),
          resultSet.getBoolean("disabled"),
          resultSet.getBoolean("administrator"),
          resultSet.getTimestamp("last_action").toLocalDateTime(),
          resultSet.getString("img_id"));
      }
      return null;
    } catch (SQLException e) {
      throw new HoneypotException("Unable to get user");
    }
  }

  @Override
  public List<HoneypotUser> getUsers() {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("select * from users")) {
      Map<Integer, HoneypotUser> users = new HashMap<>();
      ResultSet resultSet = stmt.executeQuery();
      while (resultSet.next()) {
        LocalDateTime lastAction = null;
        if (resultSet.getTimestamp("last_action") != null) {
          lastAction = resultSet.getTimestamp("last_action").toLocalDateTime();
        }
        List<Challenge> solvedChallenges = getSolvedChallenges(resultSet.getInt("id"));
        users.put(resultSet.getInt("id"), new HoneypotUser(
          resultSet.getInt("id"),
          resultSet.getString("username"),
          resultSet.getString("password"),
          resultSet.getBoolean("disabled"),
          resultSet.getBoolean("administrator"),
          lastAction,
          resultSet.getString("img_id"),
          solvedChallenges));
      }
      // Convert HashMap to List
      List<HoneypotUser> usersList = new ArrayList<>();
      for (Map.Entry<Integer, HoneypotUser> entry : users.entrySet()) {
        usersList.add(entry.getValue());
      }
      return usersList;
    } catch (SQLException e) {
      throw new HoneypotException("Unable to get users");
    }
  }

  @Override
  public void addUser(HoneypotUser user) {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)")) {
      stmt.setString(1, user.getUsername());
      stmt.setString(2, user.getPassword());
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new HoneypotException("Unable to add user");
    }
  }

  @Override
  public HoneypotUser login(String username, String password) {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {
      stmt.setString(1, username);
      stmt.setString(2, password);
      ResultSet resultSet = stmt.executeQuery();
      if (resultSet.next()) {
        return new HoneypotUser(
          resultSet.getInt("id"),
          resultSet.getString("username"),
          resultSet.getString("password"),
          resultSet.getBoolean("disabled"),
          resultSet.getBoolean("administrator"),
          resultSet.getTimestamp("last_action").toLocalDateTime(),
          resultSet.getString("img_id"));
      }
      return null;
    } catch (SQLException e) {
      throw new HoneypotException("Unable to login");
    }
  }

  @Override
  public List<Challenge> getSolvedChallenges(int userId) {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM solved_challenges WHERE user_id = ?")) {
      stmt.setInt(1, userId);
      ResultSet solvedChallengesSet = stmt.executeQuery();
      List<Challenge> allChallenges = getAllChallenges();

      while (solvedChallengesSet.next()) {
        for (Challenge challenge : allChallenges) {
          if (challenge.getId() == solvedChallengesSet.getInt("solved_challenge_id")) {
            challenge.setSolved(true);
          }
        }
      }

      allChallenges.sort(Comparator.comparingInt(Challenge::getId));
      return allChallenges;

    } catch (SQLException e) {
      throw new HoneypotException("Unable to get challenges");
    }
  }

  private List<Challenge> getAllChallenges() {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM challenges")) {
      ResultSet resultSet = stmt.executeQuery();

      List<Challenge> challenges = new ArrayList<>();

      while (resultSet.next()) {
        challenges.add(new Challenge(resultSet.getInt("challenge_id")));
      }

      return challenges;

    } catch (SQLException e) {
      throw new HoneypotException("Unable to get challenges");
    }
  }

  @Override
  public boolean submitChallenge(int userId, String challengeId, String flag) {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM challenges WHERE challenge_id = ?")) {
      stmt.setString(1, challengeId);
      ResultSet resultSet = stmt.executeQuery();

      List<Challenge> solvedChallenges = getSolvedChallenges(userId);

      for (Challenge challenge : solvedChallenges) {
        if (challenge.getId() == Integer.parseInt(challengeId)) {
          return false;
        }
      }

      if (resultSet.next()) {
        if (resultSet.getString("flag").equals(flag)) {
          try (PreparedStatement stmt2 = conn.prepareStatement("INSERT INTO solved_challenges (user_id, solved_challenge_id) VALUES (?, ?)")) {
            stmt2.setInt(1, userId);
            stmt2.setString(2, challengeId);
            stmt2.executeUpdate();
          }
          return true;
        }
      }
      return false;
    } catch (SQLException e) {
      throw new HoneypotException("Unable to submit challenge");
    }
  }

  @Override
  public void updateLastAction(int userId) {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("UPDATE users SET last_action = NOW() WHERE id = ?")) {
      stmt.setInt(1, userId);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new HoneypotException("Unable to update last action");
    }

  }

  @Override
  public void toggleUser(int userId) {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("UPDATE users SET disabled = NOT disabled WHERE id = ?")) {
      stmt.setInt(1, userId);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new HoneypotException("Unable to toggle user");
    }

  }

  @Override
  public List<HoneypotUser> getOnlineUsers() {
    // Get all users and then filter out the ones that are not online
    List<HoneypotUser> allUsers = getUsers();
    List<HoneypotUser> onlineUsers = new ArrayList<>();
    allUsers.forEach(user -> {
      if (user.getLastAction().isAfter(LocalDateTime.now().minusMinutes(15))) {
        onlineUsers.add(user);
      }
    });
    return onlineUsers;
  }

  @Override
  public boolean uploadImg(int userId, String imgId) {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("UPDATE users SET img_id = ? WHERE id = ?")) {
      stmt.setString(1, imgId);
      stmt.setInt(2, userId);
      stmt.executeUpdate();
      return true;
    } catch (SQLException e) {
      throw new HoneypotException("Unable to upload image");
    }
  }

  @Override
  public boolean updateAdminRights(int userId) {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("UPDATE users SET administrator = NOT administrator WHERE id = ?")) {
      stmt.setInt(1, userId);
      stmt.executeUpdate();
      return true;
    } catch (SQLException e) {
      throw new HoneypotException("Unable to make admin");
    }
  }

  @Override
  public boolean isUserAdmin(int userID) {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT administrator FROM users WHERE id = ?")) {
      stmt.setInt(1, userID);
      ResultSet resultSet = stmt.executeQuery();
      if (resultSet.next()) {
        return resultSet.getBoolean("administrator");
      }
      return false;
    } catch (SQLException e) {
      throw new HoneypotException("Unable to check if user is admin");
    }
  }
}
