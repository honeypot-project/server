package domain;

public class Challenge {

  private String status;

  private int id;
  private String flag;

  public Challenge(int id) {
    this("unsolved", id);
  }

  public Challenge(String status, int id) {
    this(status, id, null);
  }

  public Challenge(String status, int id, String flag) {
    this.status = status;
    this.id = id;
    this.flag = flag;
  }

  public int getId() {
    return id;
  }

  public String getStatus() {
    return status;
  }

  public String getFlag() {
    return flag;
  }
}
