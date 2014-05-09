package hu.rgai.android.beens;

import android.net.Uri;
import hu.rgai.android.messageproviders.MessageProvider;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class PhoneRecipientAndr implements MessageRecipient {

  private String displayName;
  private Uri imgUri;
  private int contactId;
  private String number;
  
  public PhoneRecipientAndr(String displayData, String number, String displayName, Uri imgUri, int contactId) {
    this.number = number;
    this.displayName = displayName;
    this.imgUri = imgUri;
    this.contactId = contactId;
  }
  
  public String getNumber() {
    return number;
  }
  
  public String getData() {
    return getNumber();
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

  public String getDisplayData() {
    return getData();
  }
  
  public MessageProvider.Type getType() {
    return MessageProvider.Type.SMS;
  }
  
}
