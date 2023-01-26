package data;

public class Repos {
  public static final HoneypotDataRepo dataRepo = new HoneypotDataRepoMySQLImpl();

  private Repos() {
    /* config class */
  }
}
