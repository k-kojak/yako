
package hu.rgai.yako.handlers;

import android.content.Context;
import android.os.Parcelable;

/**
 *
 * @author Tamas Kojedzinszky
 */
public abstract class MessageSendHandler extends TimeoutHandler implements Parcelable {

  public static final int FAIL = 0;
  public static final int SENT = 1;
  public static final int DELIVERED = 2;

  protected Context mContext;
  
  public MessageSendHandler(Context context) {
    mContext = context;
  }
  
  
  public abstract void success(String name);
  public abstract void fail(String name);
  public abstract void delivered(String name);
  
}
