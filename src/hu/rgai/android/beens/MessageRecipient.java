package hu.rgai.android.beens;

import android.net.Uri;
import hu.rgai.android.messageproviders.MessageProvider;

/**
 *
 * @author Tamas Kojedzinszky
 */
public interface MessageRecipient {
  public String getDisplayData();
  public String getData();
  public String getDisplayName();
  public Uri getImgUri();
  public int getContactId();
  public MessageProvider.Type getType();
}
