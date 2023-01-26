package data;

import domain.Challenge;
import domain.HoneypotUser;

import java.util.List;

public interface HoneypotDataRepo {
  HoneypotUser getUser(int userId);
  List<HoneypotUser> getUsers();
  void addUser(HoneypotUser user);
  HoneypotUser login(String username, String password);
  List<Challenge> getSolvedChallenges(int userId);
  boolean submitChallenge(int userId, String challengeId, String flag);
  void updateLastAction(int userId);
  void toggleUser(int userId);
  List<HoneypotUser> getOnlineUsers();
  boolean uploadImg(int userId, String imgId);
  boolean updateAdminRights(int userId);
  boolean isUserAdmin(int userId);

  HoneypotUser getUserByUsername(String username);

  void setup();
}
