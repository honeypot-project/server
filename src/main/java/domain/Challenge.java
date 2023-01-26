package domain;

public class Challenge {

  private int id;
  private boolean solved;

  public Challenge(int id) {
    this(id, false);
  }

  public Challenge(int id, boolean solved) {
    this.id = id;
    this.solved = solved;
  }

  public int getId() {
    return id;
  }

  public boolean isSolved() {
    return solved;
  }

  public void setSolved(boolean b) {
    solved = b;
  }
}
