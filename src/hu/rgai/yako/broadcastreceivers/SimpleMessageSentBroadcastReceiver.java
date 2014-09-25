
package hu.rgai.yako.broadcastreceivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import hu.rgai.android.test.R;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.MainServiceExtraParams;
import hu.rgai.yako.beens.SentMessageBroadcastDescriptor;
import hu.rgai.yako.beens.SentMessageData;
import hu.rgai.yako.beens.SimpleSentMessageData;
import hu.rgai.yako.beens.SmsSentMessageData;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.services.schedulestarters.MainScheduler;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class SimpleMessageSentBroadcastReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals(IntentStrings.Actions.MESSAGE_SENT_BROADCAST)) {
      showNotification(context, intent);
    }
  }
  
  private void showNotification(final Context context, Intent intent) {
    SentMessageBroadcastDescriptor sentMessageData = intent.getParcelableExtra(IntentStrings.Params.MESSAGE_SENT_BROADCAST_DATA);
    
    int resultType = sentMessageData.getResultType();
    int itemCount = -1;
    int itemIndex = -1;
    String to = null;
    Account accountToLoad = null;
    if (sentMessageData.getMessageData() instanceof SmsSentMessageData) {
      SmsSentMessageData smsData = (SmsSentMessageData)sentMessageData.getMessageData();
      itemCount = smsData.getItemCount();
      itemIndex = smsData.getItemIndex();
      to = smsData.getRecipientName();
      accountToLoad = smsData.getAccountToLoad();
    } else {
      SimpleSentMessageData simpleData = (SimpleSentMessageData)sentMessageData.getMessageData();
      to = simpleData.getRecipientName();
      accountToLoad = simpleData.getAccountToLoad();
    }
    
    String ticker;
    String title;
    switch(resultType) {
      case MessageSentBroadcastReceiver.MESSAGE_DELIVERED:
        ticker = "Message delivered";
        title = "Message delivered to";
        break;
      case MessageSentBroadcastReceiver.MESSAGE_DELIVER_FAILED:
        ticker = "Deliver failed";
        title = "Failed to deliver message to";
        break;
      case MessageSentBroadcastReceiver.MESSAGE_SENT_SUCCESS:
        ticker = "Message sent";
        title = "Message sent to";
        break;
      case MessageSentBroadcastReceiver.MESSAGE_SENT_FAILED:
        ticker = "Sending failed";
        title = "Failed to send message to:";
        break;
      default:
        ticker = null;
        title = null;
        break;
    }
    
    if (ticker != null && title != null) {
      
      NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

      NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
              .setSmallIcon(R.drawable.not_ic_action_email)
              .setTicker(ticker)
              .setContentTitle(title)
              .setContentText(to);
      if (itemCount > 0) {
        mBuilder.setContentInfo((itemIndex + 1) + "/" + itemCount);
      }
      mBuilder.setAutoCancel(true);
      final int sentId = (int)(Math.random() * 100000);
      mNotificationManager.notify(sentId, mBuilder.build());
      
      if (resultType == MessageSentBroadcastReceiver.MESSAGE_DELIVERED || resultType == MessageSentBroadcastReceiver.MESSAGE_SENT_SUCCESS) {
        new Timer().schedule(new TimerTask() {
          @Override
          public void run() {
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(sentId);
          }
        }, 5000);
      }
      
      // if message was sent succesfully, and we have an instance to refresh, then refresh it at main list
      if (resultType == MessageSentBroadcastReceiver.MESSAGE_SENT_SUCCESS && accountToLoad != null) {
        MainServiceExtraParams eParams = new MainServiceExtraParams();
        eParams.addAccount(accountToLoad);
        eParams.setForceQuery(true);
        Intent i = new Intent(context, MainScheduler.class);
        i.setAction(Context.ALARM_SERVICE);
        i.putExtra(IntentStrings.Params.EXTRA_PARAMS, eParams);
        context.sendBroadcast(i);
      }
    }
    
  }

}
