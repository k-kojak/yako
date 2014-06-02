
package hu.rgai.yako.handlers;

import android.widget.Toast;
import hu.rgai.yako.beens.FullThreadMessage;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.view.activities.ThreadDisplayerActivity;
import hu.rgai.yako.workers.TimeoutAsyncTask;

public class ThreadContentGetterHandler extends TimeoutHandler {
  
  private ThreadDisplayerActivity mThreadDisplayer;
  private MessageListElement mMessage;

  public ThreadContentGetterHandler(ThreadDisplayerActivity mThreadDisplayer, MessageListElement message) {
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
