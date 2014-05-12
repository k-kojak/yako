package hu.rgai.android.beens;

import android.os.Parcelable;
import hu.rgai.android.messageproviders.MessageProvider;
import java.io.Serializable;



/**
 *
 * @author Tamas Kojedzinszky
 */
public interface Account extends Parcelable, Serializable {
  
  public String getDisplayName();
  
  public MessageProvider.Type getAccountType();
  
  @Override
  public boolean equals(Object account);
  
  public int getMessageLimit();
  
  @Override
  public String toString();
  
  public boolean isInternetNeededForLoad();
  
}
