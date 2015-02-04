
package hu.rgai.yako.handlers;

import java.util.List;

import android.content.Context;
import android.widget.Toast;
import hu.rgai.android.test.R;
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
  public void onTimeout(Context context) {
    Toast.makeText(mContext, R.string.timeout_while_deleting_msg, Toast.LENGTH_LONG).show();
  }
  
  public void toastMessage(String s) {
    Toast.makeText(mContext, s, Toast.LENGTH_LONG).show();
  }
  
  public abstract void onMainListDelete(List<MessageListElement> deletedMessageList);
  
  public abstract void onThreadListDelete(long deletedMessageListRawId, String deletedSimpleMessageId,
                                          boolean isInternetNeededForProvider);
  
  public void onComplete() {}
}
