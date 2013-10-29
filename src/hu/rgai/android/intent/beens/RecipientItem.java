package hu.rgai.android.intent.beens;

import android.net.Uri;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;

/**
 *
 * @author Tamas Kojedzinszky
 */
public interface RecipientItem {
  public String getDisplayData();
  public String getData();
  public String getDisplayName();
  public Uri getImgUri();
  public int getContactId();
  public MessageProvider.Type getType();
}
