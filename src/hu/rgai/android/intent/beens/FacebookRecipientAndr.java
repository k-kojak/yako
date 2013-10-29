package hu.rgai.android.intent.beens;

import android.net.Uri;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.FacebookMessageRecipient;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FacebookRecipientAndr extends FacebookMessageRecipient implements RecipientItem {

  private String displayName;
  private String displayData;
  private Uri imgUri;
  private int contactId;
  
  public FacebookRecipientAndr(String displayData, String fbId, String displayName, Uri imgUri, int contactId) {
    super(Long.parseLong(fbId));
    this.displayData = displayData;
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
  public String getDisplayData() {
    return displayData;
  }
  
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
  
  public MessageProvider.Type getType() {
    return MessageProvider.Type.FACEBOOK;
  }
  
}
