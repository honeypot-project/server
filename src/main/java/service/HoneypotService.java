package service;

import com.password4j.Hash;
import com.password4j.Password;
import data.HoneypotDataRepo;
import data.Repos;
import domain.Challenge;
import domain.HoneypotUser;
import io.vertx.ext.web.FileUpload;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public class HoneypotService {
  private static final String LOGIN_SUCCESSFUL = "login successful";
  private static final String SQL_SELECT_USER_BY_ID = "SELECT * FROM users WHERE id = ?";
  private static final String SQL_SELECT_ALL_USERS_JOIN_SOLVED_CHALLENGES = "SELECT users.id, users.username, users.disabled, users.administrator, sc.solved_challenge_id" +
    " FROM users LEFT JOIN solved_challenges sc on users.id = sc.user_id";
  private static final String SQL_ORDER_BY_CHALLENGE_ID = " ORDER BY solved_challenge_id";
  private static final String SQL_WHERE_LAST_ACTION_IN_30_MINUTES = " WHERE users.last_action > NOW() - INTERVAL 30 MINUTE";

  private static final HoneypotDataRepo repo = Repos.dataRepo;


  public HoneypotUser register(String username, String password) {
    // Check if parameters are valid
    if (!parametersAreValid(username, password)) return null;

    // Check if user already exists
    if (userExists(username)) return null;

    // Lowercase username
    final String validatedUsername = username.toLowerCase();

    // Hash password
    Hash hash = Password.hash(password).addRandomSalt().withArgon2();

    // Create user
    return new HoneypotUser(-1, validatedUsername, hash.getResult(), false, false, null, null);

  }

  public HoneypotUser login(String username, String password) {
    // Lowercase username
    final String validatedUsername = username.toLowerCase();

    // Get user
    HoneypotUser user = repo.getUserByUsername(validatedUsername);

    // Check if user exists
    if (user == null) {
      return null;
    }

    // Check if password is correct
    if (Password.check(password, user.getPassword()).withArgon2()) {
      return user;
    } else {
      return null;
    }
  }

  public static boolean parametersAreValid(String username, String password) {

    Boolean usernameEmpty = username == null || username.isEmpty();
    Boolean passwordEmpty = password == null || password.isEmpty();

    if (usernameEmpty || passwordEmpty) {
      return false;
    }

    // Validate username
    if (!username.matches("^[a-zA-Z0-9]+$")) {
      return false;
    }

    return true;
  }

  private static boolean userExists(String username) {
    HoneypotUser user = repo.getUserByUsername(username);
    return user != null;
  }

  public void uploadImg(int userId, List<FileUpload> files) {

    // Get file
    FileUpload file = files.get(0);

    // Get file extension
    String extension = file.fileName().substring(file.fileName().lastIndexOf(".") + 1);

    // Generate random file name
    String fileName = UUID.randomUUID().toString() + "." + extension;

    // Create uploads folder if it doesn't exist
    if (!new File("uploads/images/").exists()) {
      new File("uploads/images").mkdirs();
    }

    // Move new image to uploads folder
    new File(file.uploadedFileName()).renameTo(new File("uploads/images/" + fileName));


    HoneypotUser user = repo.getUser(userId);

    // Delete old image if it exists
    String oldImgId = user.getImgId();
    if (oldImgId != null) {
      if (new File("uploads/images/" + oldImgId).exists()) {
        try {
          Files.delete(Paths.get("uploads/images/" + oldImgId));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    // Update database with new image id
    repo.uploadImg(userId, fileName);
  }


  public void deleteFiles(List<FileUpload> files) {
    for (FileUpload file : files) {
      new File(file.uploadedFileName()).delete();
    }
  }

  public List<Challenge> getChallenges(int userId) {
    return repo.getSolvedChallenges(userId);
  }

  public boolean submitChallenge(int userId, String challengeId, String flag) {
    return repo.submitChallenge(userId, challengeId, flag);
  }

  public void toggleUser(int userToBeToggled) {
    repo.toggleUser(userToBeToggled);
  }

  public List<HoneypotUser> getOnlineUsers() {
    return repo.getOnlineUsers();
  }

  public void updateAdminRights(int userToMakeAdmin) {
    repo.updateAdminRights(userToMakeAdmin);
  }
}
