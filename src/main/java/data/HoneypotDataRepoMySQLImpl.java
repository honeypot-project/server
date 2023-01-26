package data;

import domain.Challenge;
import domain.HoneypotUser;
import io.vertx.core.json.JsonObject;
import util.HoneypotErrors;
import util.HoneypotException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class HoneypotDataRepoMySQLImpl implements HoneypotDataRepo {


  @Override
  public HoneypotUser getUserByUsername(String username) {
try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
      stmt.setString(1, username);
      ResultSet resultSet = stmt.executeQuery();
      if (resultSet.next()) {
        return new HoneypotUser(
          resultSet.getInt("id"),
          resultSet.getString("username"),
          null, // Don't send password to client
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
  public HoneypotUser getUser(String userId) {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {
      stmt.setString(1, userId);
      ResultSet resultSet = stmt.executeQuery();
      if (resultSet.next()) {
        return new HoneypotUser(
          resultSet.getInt("id"),
          resultSet.getString("username"),
          null, // Don't send password to client
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
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users LEFT JOIN solved_challenges sc on users.id = sc.user_id ORDER BY solved_challenge_id")) {
      Map<Integer, HoneypotUser> users = new HashMap<>();
      ResultSet resultSet = stmt.executeQuery();
      while (resultSet.next()) {
        // Check first if user is already in HashMap and if so, add the challengeId
        if (users.containsKey(resultSet.getInt("id"))) {
          HoneypotUser user = users.get(resultSet.getInt("id"));
          user.addChallenge(resultSet.getInt("solved_challenge_id"));
        } else {
          HoneypotUser userToBeAdded = new HoneypotUser(
            resultSet.getInt("id"),
            resultSet.getString("username"),
            null, // Don't send password to client
            resultSet.getBoolean("disabled"),
            resultSet.getBoolean("administrator"),
            resultSet.getTimestamp("last_action").toLocalDateTime(),
            resultSet.getString("img_id"));

          userToBeAdded.addChallenge(resultSet.getInt("solved_challenge_id"));

          users.put(
            resultSet.getInt("id"),
            userToBeAdded
          );

        }
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
          null, // Don't send password to client
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
  public JsonObject getChallenges() {
    return null;
  }

  @Override
  public boolean submitChallenge(String username, String challengeId, String flag) {
    return false;
  }

  @Override
  public void updateLastAction(String userId) {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("UPDATE users SET last_action = NOW() WHERE id = ?")) {
      stmt.setString(1, userId);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new HoneypotException("Unable to update last action");
    }

  }

  @Override
  public void toggleUser(String username) {

  }

  @Override
  public List<HoneypotUser> getOnlineUsers() {
    return null;
  }

  @Override
  public boolean uploadImg(String userId, String imgId) {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("UPDATE users SET img_id = ? WHERE id = ?")) {
      stmt.setString(1, imgId);
      stmt.setString(2, userId);
      stmt.executeUpdate();
      return true;
    } catch (SQLException e) {
      throw new HoneypotException("Unable to upload image");
    }
  }

  @Override
  public boolean makeAdmin(String username) {
    return false;
  }

  @Override
  public boolean isUserAdmin(String userID) {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT administrator FROM users WHERE id = ?")) {
      stmt.setString(1, userID);
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
