
package hu.rgai.android.workers;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.test.AnalyticsApp;
import hu.rgai.android.tools.AndroidUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;

public class MessageSeenMarkerAsyncTask extends AsyncTask<Void, Void, Void> {

  TreeSet<MessageListElement> mMessages = null;
  Context mContext = null;
  AnalyticsApp mAnalyticsApp = null;
  Handler mHandler = null;
  boolean mSeen;
  
  public MessageSeenMarkerAsyncTask(TreeSet<MessageListElement> messages, Context context, AnalyticsApp analyticsApp,
          Handler handler, boolean seen) {
    this.mMessages = messages;
    this.mContext = context;
    this.mAnalyticsApp = analyticsApp;
    this.mHandler = handler;
    this.mSeen = seen;
  }
  
  @Override
  protected Void doInBackground(Void... params) {
    HashMap<Account, TreeSet<MessageListElement>> messagesToAccounts = new HashMap<Account, TreeSet<MessageListElement>>();
    for (MessageListElement mle : mMessages) {
      if (!messagesToAccounts.containsKey(mle.getAccount())) {
        messagesToAccounts.put(mle.getAccount(), new TreeSet<MessageListElement>());
      }
      messagesToAccounts.get(mle.getAccount()).add(mle);
    }
    
    for (Map.Entry<Account, TreeSet<MessageListElement>> entry : messagesToAccounts.entrySet()) {
      MessageProvider mp = AndroidUtils.getMessageProviderInstanceByAccount(entry.getKey(), mContext, mAnalyticsApp, mHandler);
      String[] ids = new String[entry.getValue().size()];
      int i = 0;
      for (MessageListElement mle : entry.getValue()) {
        ids[i++] = mle.getId();
      }
      try {
        mp.markMessagesAsRead(ids, mSeen);
//        Log.d("rgai", "Marking message as seen/unseen: " + mSeen);
      } catch (MessagingException ex) {
        Toast.makeText(mContext, "Unable to mark message status.", Toast.LENGTH_SHORT).show();
      } catch (IOException ex) {
        Toast.makeText(mContext, "Unable to mark message status.", Toast.LENGTH_SHORT).show();
      }
    }
    
    return null;
  }
  
  

}
