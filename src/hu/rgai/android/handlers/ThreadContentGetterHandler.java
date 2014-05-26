
package hu.rgai.android.handlers;

import android.widget.Toast;
import hu.rgai.android.beens.FullThreadMessage;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.test.ThreadDisplayer;
import hu.rgai.android.workers.TimeoutAsyncTask;

public class ThreadContentGetterHandler extends TimeoutHandler {
  
  private ThreadDisplayer mThreadDisplayer;
  private MessageListElement mMessage;

  public ThreadContentGetterHandler(ThreadDisplayer mThreadDisplayer, MessageListElement message) {
    this.mThreadDisplayer = mThreadDisplayer;
    this.mMessage = message;
  }
  
  public void onComplete(boolean success, FullThreadMessage messageContent, boolean scrollToBottom) {
    if (!success) {
      Toast.makeText(mThreadDisplayer, "Error while loading content", Toast.LENGTH_LONG).show();
    } else {
      mThreadDisplayer.appendLoadedMessages(messageContent);
      mThreadDisplayer.displayMessage(scrollToBottom);
    }
    mThreadDisplayer.dismissProgressDialog();
  }

}
