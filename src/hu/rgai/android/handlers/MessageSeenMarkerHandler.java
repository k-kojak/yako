
package hu.rgai.android.handlers;

import android.widget.Toast;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.test.MainActivity;
import java.util.TreeSet;

public class MessageSeenMarkerHandler extends TimeoutHandler {

  public MainActivity mActivity;

  public MessageSeenMarkerHandler(MainActivity mActivity) {
    this.mActivity = mActivity;
  }
  
  @Override
  public void timeout() {
    Toast.makeText(mActivity, "Timeout while marking messages", Toast.LENGTH_LONG).show();
  }
  
  public void toastMessage(String s) {
    Toast.makeText(mActivity, s, Toast.LENGTH_LONG).show();
  }
  
  public void success(TreeSet<MessageListElement> messagesToMark, boolean seen) {
    for (MessageListElement mle : messagesToMark) {
      mle.setSeen(seen);
    }
    mActivity.notifyAdapterChange();
  }

}
