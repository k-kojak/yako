
package hu.rgai.yako.handlers;

import android.content.Context;
import android.widget.Toast;
import hu.rgai.yako.beens.FullSimpleMessage;
import hu.rgai.yako.beens.MessageListElement;

/**
 *
 * @author Tamas Kojedzinszky
 */
public abstract class MessageDeleteHandler extends TimeoutHandler {

  protected Context mContext;

  public MessageDeleteHandler(Context context) {
    mContext = context;
  }
  
 
  @Override
  public void timeout() {
    Toast.makeText(mContext, "Timeout while deleting message", Toast.LENGTH_LONG).show();
  }
  
  public void toastMessage(String s) {
    Toast.makeText(mContext, s, Toast.LENGTH_LONG).show();
  }
  
  public abstract void onMainListDelete(MessageListElement messageToDelete);
  
  public abstract void onThreadListDelete(MessageListElement messageToDelete, FullSimpleMessage simpleMessageId);
  
  public void onComplete() {}
}
