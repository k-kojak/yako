package hu.rgai.android.beens.fbintegrate;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FacebookIntegrateItem {
  
  private String name;
  private String fbAliasId;
  private String fbId;

  public FacebookIntegrateItem(String name, String fbAliasId, String fbId) {
    this.name = name;
    this.fbAliasId = fbAliasId;
    this.fbId = fbId;
  }

  public String getName() {
    return name;
  }

  public String getFbAliasId() {
    return fbAliasId;
  }

  public String getFbId() {
    return fbId;
  }

  @Override
  public String toString() {
    return "FacebookIntegrateItem{" + "name=" + name + ", fbAliasId=" + fbAliasId + ", fbId=" + fbId + '}';
  }

}
