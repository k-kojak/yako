
package hu.rgai.yako.workers;

import hu.rgai.yako.handlers.MessageSeenMarkerHandler;
import hu.rgai.yako.messageproviders.MessageProvider;

import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageSeenMarkerAsyncTask extends BatchedTimeoutAsyncTask<Void, Void, Boolean> {

  private MessageProvider mProvider = null;
  private TreeSet<String> mMessagesToMark = null;
  private final boolean mSeen;
  private MessageSeenMarkerHandler mHandler = null;
  
  
  public MessageSeenMarkerAsyncTask(MessageProvider messageProvider, TreeSet<String> messagesToMark, boolean seen,
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
    for (String messageId : mMessagesToMark) {
      ids[i++] = messageId;
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
  protected void onBatchedPostExecute(Boolean success) {
    if (mHandler != null) {
      if (success) {
        mHandler.success();
      } else {
        mHandler.toastMessage("Unable to mark message status.");
      }
    }
  }

}
