package hu.rgai.android.intent.beens;

import android.net.Uri;
import hu.uszeged.inf.rgai.messagelog.beans.FacebookMessageRecipient;

/**
 *
 * @author Tamas Kojedzinszku
 */
public class FacebookRecipientAndr extends FacebookMessageRecipient implements RecipientItem {

  private String displayName;
  private Uri imgUri;
  private int contactId;
  
  public FacebookRecipientAndr(String fbId, String displayName, Uri imgUri, int contactId) {
    super(Long.parseLong(fbId));
    this.displayName = displayName;
    this.imgUri = imgUri;
    this.contactId = contactId;
  }
  
//  public FacebookRecipientAndr(int id, long fbId, String name, Uri imgUri) {
//    super(fbId);
//    this.displayName = name;
//    this.imgUri = imgUri;
//    this.contactId = id;
//  }
  
  public String getData() {
    return this.getId() + "";
  }

  public String getDisplayName() {
    return displayName;
  }

  public Uri getImgUri() {
    return imgUri;
  }

  public int getContactId() {
    return contactId;
  }
  
}
