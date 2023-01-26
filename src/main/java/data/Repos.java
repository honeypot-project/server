package data;

import service.HoneypotService;

public class Repos {
  public static final HoneypotDataRepo dataRepo = new HoneypotDataRepoMySQLImpl();
  public static final HoneypotService service = new HoneypotService();

  private Repos() {
    /* config class */
  }
}
