package hu.rgai.android.beens.fbintegrate;


/**
 *
 * @author Tamas Kojedzinszky
 */
public class FacebookIntegrateItem {
  
  private String name;
  private String fbAliasId;
  private String fbId;
  private String thumbImgUrl;
  private String bigThumbImgUrl;

  public FacebookIntegrateItem(String name, String fbAliasId, String fbId, String thumbImgUrl, String bigThumbImgUrl) {
    this.name = name;
    this.fbAliasId = fbAliasId;
    this.fbId = fbId;
    this.thumbImgUrl = thumbImgUrl;
    this.bigThumbImgUrl = bigThumbImgUrl;
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

  public String getThumbImgUlr() {
    return thumbImgUrl;
  }
  
  public String getBigThumbImgUlr() {
	return bigThumbImgUrl;
  }
  
  @Override
  public String toString() {
    return "FacebookIntegrateItem{" + "name=" + name + ", fbAliasId=" + fbAliasId + ", fbId=" + fbId + '}';
  }

}
