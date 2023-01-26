package domain;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.vertx.core.json.JsonArray;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HoneypotUser {
  private final int id;
  private final String username;
  private final String password;
  private final boolean disabled;
  private final boolean administrator;
  private final LocalDateTime lastAction;
  private final String imgId;
  private final List<Challenge> solvedChallenges;

  public HoneypotUser(int id) {
    this(id, null, null, false, false, null, null, new ArrayList<>());
  }

  public HoneypotUser(int id, String username, String password, boolean disabled, boolean administrator, LocalDateTime lastAction, String imgId) {
    this(id, username, password, disabled, administrator, lastAction, imgId, new ArrayList<>());
  }

  public HoneypotUser(int id, String username, String password, boolean disabled, boolean administrator, LocalDateTime lastAction, String imgId, List<Challenge> solvedChallenges) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.disabled = disabled;
    this.administrator = administrator;
    this.lastAction = lastAction;
    this.imgId = imgId;
    this.solvedChallenges = solvedChallenges;
  }

  public int getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  @JsonIgnore
  public String getPassword() {
    return password;
  }

  public boolean isDisabled() {
    return disabled;
  }

  public boolean isAdmin() {
    return administrator;
  }

  @JsonGetter("last_action")
  public String getLastAction() {
    return lastAction.toString();
  }

  @JsonGetter("img_id")
  public String getImgId() {
    return imgId;
  }

  @JsonGetter("challenges")
  public List<Challenge> getSolvedChallenges() {
    return solvedChallenges;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HoneypotUser that = (HoneypotUser) o;
    return id == that.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public void addChallenge(int challengeId) {
    solvedChallenges.add(new Challenge(challengeId));
  }
}
