
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
      String to = intent.getStringExtra(IntentStrings.Params.MESSAGE_SENT_RECIPIENT_NAME);
      switch(resultType) {
        case MessageSentBroadcastReceiver.MESSAGE_SENT_SUCCESS:
          showNotification(context, true, to);
          break;
        case MessageSentBroadcastReceiver.MESSAGE_SENT_FAILED:
          showNotification(context, false, to);
          break;
        default:
          break;
      }
    }
  }
  
  private void showNotification(final Context context, boolean success, String to) {
    String ticker = success ? "Message sent" : "Sending failed";
    String title = success ? "Message sent to" : "Failed to send message to:";

    NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.not_ic_action_email)
            .setTicker(ticker)
            .setContentTitle(title)
            .setContentText(to);
    mBuilder.setAutoCancel(true);
    mNotificationManager.notify(Settings.NOTIFICATION_SENT_MESSAGE_ID, mBuilder.build());

    if (success) {
      new Timer().schedule(new TimerTask() {
        @Override
        public void run() {
          NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
          mNotificationManager.cancel(Settings.NOTIFICATION_SENT_MESSAGE_ID);
        }
      }, 5000);
    }
  }

}
