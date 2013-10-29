package hu.rgai.android.intent.beens;

import android.net.Uri;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class PhoneRecipientAndr extends PhoneRecipient implements RecipientItem {

  private String displayName;
  private Uri imgUri;
  private int contactId;
  
  public PhoneRecipientAndr(String displayData, String number, String displayName, Uri imgUri, int contactId) {
    super(number);
    this.displayName = displayName;
    this.imgUri = imgUri;
    this.contactId = contactId;
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
