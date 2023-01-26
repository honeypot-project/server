package data;

import domain.HoneypotUser;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface HoneypotDataRepo {
  HoneypotUser getUser(String userId);
  List<HoneypotUser> getUsers();
  void addUser(HoneypotUser user);
  HoneypotUser login(String username, String password);
  JsonObject getChallenges();
  boolean submitChallenge(String username, String challengeId, String flag);
  void updateLastAction(String userId);
  void toggleUser(String username);
  List<HoneypotUser> getOnlineUsers();
  boolean uploadImg(String userId, String imgId);
  boolean makeAdmin(String username);
  boolean isUserAdmin(String userId);

  HoneypotUser getUserByUsername(String username);
}
