package data;

import domain.Challenge;
import domain.HoneypotUser;
import util.HoneypotException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HoneypotDataRepoMySQLImpl implements HoneypotDataRepo {


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
  public List<Challenge> getSolvedChallenges(int userId) {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM solved_challenges WHERE user_id = ?")) {
      stmt.setInt(1, userId);
      List<Challenge> challenges = new ArrayList<>();
      ResultSet resultSet = stmt.executeQuery();
      while (resultSet.next()) {
        challenges.add(new Challenge(resultSet.getInt("solved_challenge_id")));
      }
      return challenges;
    } catch (SQLException e) {
      throw new HoneypotException("Unable to get challenges");
    }
  }

  @Override
  public boolean submitChallenge(int userId, String challengeId, String flag) {
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM challenges WHERE id = ?")) {
      stmt.setString(1, challengeId);
      ResultSet resultSet = stmt.executeQuery();
      if (resultSet.next()) {
        if (resultSet.getString("flag").equals(flag)) {
          // Flag is correct, add challenge to user
          try (Connection conn2 = MySQLConnection.getConnection();
               PreparedStatement stmt2 = conn2.prepareStatement("INSERT INTO solved_challenges (user_id, solved_challenge_id) VALUES (?, ?)")) {
            stmt2.setInt(1, userId);
            stmt2.setString(2, challengeId);
            stmt2.executeUpdate();
          } catch (SQLException e) {
            throw new HoneypotException("Unable to add challenge to user");
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
    try (Connection conn = MySQLConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE last_action > NOW() - INTERVAL 15 MINUTE")) {
      List<HoneypotUser> users = new ArrayList<>();
      ResultSet resultSet = stmt.executeQuery();
      while (resultSet.next()) {
        users.add(new HoneypotUser(
          resultSet.getInt("id"),
          resultSet.getString("username"),
          null, // Don't send password to client
          resultSet.getBoolean("disabled"),
          resultSet.getBoolean("administrator"),
          resultSet.getTimestamp("last_action").toLocalDateTime(),
          resultSet.getString("img_id")));
      }
      return users;
    } catch (SQLException e) {
      throw new HoneypotException("Unable to get online users");
    }
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
