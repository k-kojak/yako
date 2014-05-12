package hu.rgai.android.beens;

import android.net.Uri;
import hu.rgai.android.messageproviders.MessageProvider;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class EmailMessageRecipient implements MessageRecipient {
  
  protected String name;
  protected String email;
  protected Uri imgUri;
  protected int contactId;

  public EmailMessageRecipient(String displayData, String email, String displayName, Uri imgUri, int contactId) {
    this(displayName, email);
    this.imgUri = imgUri;
    this.contactId = contactId;
  }
  
  public EmailMessageRecipient(String name, String email) {
    this.name = name;
    this.email = email;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
  
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

  public String getDisplayData() {
    return getData();
  }
  
  public MessageProvider.Type getType() {
    return MessageProvider.Type.EMAIL;
  }
  
}
