package hu.rgai.android.intent.beens;

import android.net.Uri;

/**
 *
 * @author kojak
 */
public interface RecipientItem {
  public String getData();
  public String getDisplayName();
  public Uri getImgUri();
  public int getContactId();
}
