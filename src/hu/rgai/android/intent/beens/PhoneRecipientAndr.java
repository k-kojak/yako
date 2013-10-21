package hu.rgai.android.intent.beens;

import android.net.Uri;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class PhoneRecipientAndr extends PhoneRecipient implements RecipientItem {

  private String displayName;
  private Uri imgUri;
  private int contactId;
  
  public PhoneRecipientAndr(String number, String displayName, Uri imgUri, int contactId) {
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
  
}
