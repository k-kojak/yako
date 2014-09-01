
package hu.rgai.yako.handlers;

import android.widget.Toast;
import hu.rgai.yako.beens.FullThreadMessage;
import hu.rgai.yako.view.activities.ThreadDisplayerActivity;

public class ThreadContentGetterHandler extends TimeoutHandler {
  
  private final ThreadDisplayerActivity mThreadDisplayer;

  public ThreadContentGetterHandler(ThreadDisplayerActivity mThreadDisplayer) {
    this.mThreadDisplayer = mThreadDisplayer;
  }
  
  public void onComplete(boolean saveToDbAfterLoad, boolean success, FullThreadMessage messageContent, boolean scrollToBottom) {
    if (!success) {
      Toast.makeText(mThreadDisplayer, "Error while loading content", Toast.LENGTH_LONG).show();
    } else {
      mThreadDisplayer.appendLoadedMessages(messageContent, saveToDbAfterLoad);
      mThreadDisplayer.displayMessage(scrollToBottom, saveToDbAfterLoad);
    }
    mThreadDisplayer.dismissProgressDialog();
  }

}
