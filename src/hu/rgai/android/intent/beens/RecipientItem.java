package hu.rgai.android.intent.beens;

import android.net.Uri;

/**
 *
 * @author Tamas Kojedzinszky
 */
public interface RecipientItem {
  public String getData();
  public String getDisplayName();
  public Uri getImgUri();
  public int getContactId();
}
