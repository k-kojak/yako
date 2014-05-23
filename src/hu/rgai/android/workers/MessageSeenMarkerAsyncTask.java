
package hu.rgai.android.workers;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.handlers.MessageSeenMarkerHandler;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.test.YakoApp;
import hu.rgai.android.tools.AndroidUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;

public class MessageSeenMarkerAsyncTask extends TimeoutAsyncTask<Void, Void, Void> {

  private MessageProvider mProvider = null;
  private TreeSet<MessageListElement> mMessagesToMark = null;
  private final boolean mSeen;
  private MessageSeenMarkerHandler mHandler = null;
  
  
  public MessageSeenMarkerAsyncTask(MessageProvider messageProvider, TreeSet<MessageListElement> messagesToMark, boolean seen,
          MessageSeenMarkerHandler handler) {
    
    mProvider = messageProvider;
    mMessagesToMark = messagesToMark;
    mSeen = seen;
    mHandler = handler;
    
    setTimeoutHandler(mHandler);
  }
  
  @Override
  protected Void doInBackground(Void... params) {
    
    String[] ids = new String[mMessagesToMark.size()];
    int i = 0;
    for (MessageListElement mle : mMessagesToMark) {
      ids[i++] = mle.getId();
    }
    
    try {
      mProvider.markMessagesAsRead(ids, mSeen);
    // TODO: add google analytics exception tracking
    } catch (Exception ex) {
      mHandler.toastMessage("Unable to mark message status.");
      Logger.getLogger(MessageSeenMarkerAsyncTask.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }

  @Override
  protected void onPostExecute(Void result) {
    mHandler.success(mMessagesToMark, mSeen);
  }
  
  

}
