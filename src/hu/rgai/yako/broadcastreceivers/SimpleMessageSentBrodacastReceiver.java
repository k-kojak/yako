
package hu.rgai.yako.broadcastreceivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;
import hu.rgai.android.test.R;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.intents.IntentStrings;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class SimpleMessageSentBrodacastReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals(IntentStrings.Actions.MESSAGE_SENT_BROADCAST)) {
      int resultType = intent.getIntExtra(IntentStrings.Params.MESSAGE_SENT_RESULT_TYPE, -1);
      int itemCount = intent.getIntExtra(IntentStrings.Params.ITEM_COUNT, -1);
      int itemIndex = intent.getIntExtra(IntentStrings.Params.ITEM_INDEX, -1);
      String to = intent.getStringExtra(IntentStrings.Params.MESSAGE_SENT_RECIPIENT_NAME);
      showNotification(context, resultType, itemIndex, itemCount, to);
    }
  }
  
  private void showNotification(final Context context, int resultType, int itemIndex, int itemCount, String to) {
    
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
      
//      if (resultType == MessageSentBroadcastReceiver.MESSAGE_DELIVERED || resultType == MessageSentBroadcastReceiver.MESSAGE_SENT_SUCCESS) {
//        new Timer().schedule(new TimerTask() {
//          @Override
//          public void run() {
//            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//            mNotificationManager.cancel(sentId);
//          }
//        }, 5000);
//      }
    }
    
  }

}
