
package hu.rgai.android.workers;

import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.handlers.MessageSeenMarkerHandler;
import hu.rgai.android.messageproviders.MessageProvider;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageSeenMarkerAsyncTask extends TimeoutAsyncTask<Void, Void, Boolean> {

  private MessageProvider mProvider = null;
  private TreeSet<MessageListElement> mMessagesToMark = null;
  private final boolean mSeen;
  private MessageSeenMarkerHandler mHandler = null;
  
  
  public MessageSeenMarkerAsyncTask(MessageProvider messageProvider, TreeSet<MessageListElement> messagesToMark, boolean seen,
          MessageSeenMarkerHandler handler) {
    
    super(handler);
    
    mProvider = messageProvider;
    mMessagesToMark = messagesToMark;
    mSeen = seen;
    mHandler = handler;
    
  }
  
  
  @Override
  protected Boolean doInBackground(Void... params) {
    
    String[] ids = new String[mMessagesToMark.size()];
    int i = 0;
    for (MessageListElement mle : mMessagesToMark) {
      ids[i++] = mle.getId();
    }
    
    try {
      mProvider.markMessagesAsRead(ids, mSeen);
    } catch (Exception ex) {
      Logger.getLogger(MessageSeenMarkerAsyncTask.class.getName()).log(Level.SEVERE, null, ex);
      return false;
    }
    return true;
  }

  @Override
  protected void onPostExecute(Boolean success) {
    if (mHandler != null) {
      if (success) {
        mHandler.success(mMessagesToMark, mSeen);
      } else {
        mHandler.toastMessage("Unable to mark message status.");
      }
    }
  }

}
