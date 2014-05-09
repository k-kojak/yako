package hu.rgai.android.beens;

import android.net.Uri;
import hu.rgai.android.messageproviders.MessageProvider;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FacebookMessageRecipient implements MessageRecipient {
  
  protected String id;
  protected String displayName;
  protected String displayData;
  protected Uri imgUri;
  protected int contactId;

  public FacebookMessageRecipient(String id) {
    this.id = id;
  }
  
  public FacebookMessageRecipient(String displayData, String fbId, String displayName, Uri imgUri, int contactId) {
    this.id = fbId;
    this.displayData = displayData;
    this.displayName = displayName;
    this.imgUri = imgUri;
    this.contactId = contactId;
  }

  public String getId() {
    return id;
  }
  
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
