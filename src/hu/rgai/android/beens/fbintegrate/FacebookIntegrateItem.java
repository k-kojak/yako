package hu.rgai.android.beens.fbintegrate;

import android.graphics.Bitmap;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FacebookIntegrateItem {
  
  private String name;
  private String fbAliasId;
  private String fbId;
  private Bitmap img;
  private Bitmap fullImg;

  public FacebookIntegrateItem(String name, String fbAliasId, String fbId, Bitmap img, Bitmap fullImg) {
    this.name = name;
    this.fbAliasId = fbAliasId;
    this.fbId = fbId;
    this.img = img;
    this.fullImg = fullImg;
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

  public Bitmap getImg() {
    return img;
  }
  
  public Bitmap getFullImg() {
    return fullImg;
  }
  
  @Override
  public String toString() {
    return "FacebookIntegrateItem{" + "name=" + name + ", fbAliasId=" + fbAliasId + ", fbId=" + fbId + '}';
  }

}
