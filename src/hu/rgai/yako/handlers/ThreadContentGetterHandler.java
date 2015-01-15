
package hu.rgai.yako.handlers;

import android.widget.Toast;
import hu.rgai.android.test.R;
import hu.rgai.yako.beens.FullThreadMessage;
import hu.rgai.yako.view.activities.ThreadDisplayerActivity;

public class ThreadContentGetterHandler extends TimeoutHandler {
  
  private final ThreadDisplayerActivity mThreadDisplayer;

  public ThreadContentGetterHandler(ThreadDisplayerActivity mThreadDisplayer) {
    this.mThreadDisplayer = mThreadDisplayer;
  }
  
  public void onComplete(boolean loadQuickAnswers, boolean saveToDbAfterLoad, boolean success, FullThreadMessage messageContent, boolean scrollToBottom) {
    if (!success) {
      Toast.makeText(mThreadDisplayer, mThreadDisplayer.getString(R.string.error_while_loading_content), Toast.LENGTH_LONG).show();
    } else {
      mThreadDisplayer.appendLoadedMessages(messageContent, saveToDbAfterLoad);
      mThreadDisplayer.displayMessage(loadQuickAnswers, scrollToBottom, saveToDbAfterLoad);
    }
    mThreadDisplayer.dismissProgressDialog();
  }

}
