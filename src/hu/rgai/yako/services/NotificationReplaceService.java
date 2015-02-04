package hu.rgai.yako.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import hu.rgai.yako.config.Settings;

/**
 * Created by kojak on 10/13/2014.
 */
public class NotificationReplaceService extends IntentService {

  public static String ACTION_SWITCH_NOTIFICATIONS = "hu.rgai.yako.SWITCH_NOTIFICATIONS";
  public static String SWITCH_NOTIFICATION_ARG_NOTIFICATION = "NOTIF_NOTIFICATION";

  public NotificationReplaceService() {
    super("Notification replace service");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    if (ACTION_SWITCH_NOTIFICATIONS.equals(intent.getAction())) {
      int notifiId = Settings.NOTIFICATION_NEW_MESSAGE_ID;
      Notification notification = intent.getParcelableExtra(SWITCH_NOTIFICATION_ARG_NOTIFICATION);

      // Creating the new notification based on the data came from the intent
      NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      mgr.notify(notifiId, notification);
    }
  }
}
