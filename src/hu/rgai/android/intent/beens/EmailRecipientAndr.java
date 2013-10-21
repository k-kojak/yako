package hu.rgai.android.intent.beens;

import android.net.Uri;
import hu.uszeged.inf.rgai.messagelog.beans.EmailMessageRecipient;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class EmailRecipientAndr extends EmailMessageRecipient implements RecipientItem {
  
  private Uri imgUri;
  private int contactId;
  
  public EmailRecipientAndr(String email, String displayName, Uri imgUri, int contactId) {
    super(displayName, email);
    this.imgUri = imgUri;
    this.contactId = contactId;
  }
  
//  public EmailRecipientAndr(String name, String email) {
//    super(name, email);
//  }

  public String getData() {
    return this.getEmail();
  }
  
  public String getDisplayName() {
    return this.getName();
  }

  public Uri getImgUri() {
    return imgUri;
  }

  public int getContactId() {
    return contactId;
  }

}
