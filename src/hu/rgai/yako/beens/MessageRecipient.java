package hu.rgai.yako.beens;

import android.net.Uri;
import hu.rgai.yako.messageproviders.MessageProvider;

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
