// TODO: mergeMessages performance issue
// TODO: tie attachment downloading thread to message item
package hu.rgai.yako.handlers;

import android.app.Notification;
import android.util.Log;
import hu.rgai.android.test.MainActivity;
import hu.rgai.android.test.R;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.MainServiceExtraParams;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.beens.MessageListResult;
import hu.rgai.yako.broadcastreceivers.DeleteIntentBroadcastReceiver;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.eventlogger.EventLogger.LogFilePaths;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.services.NotificationReplaceService;
import hu.rgai.yako.services.QuickReplyService;
import hu.rgai.yako.smarttools.DummyQuickAnswerProvider;
import hu.rgai.yako.smarttools.QuickAnswerProvider;
import hu.rgai.yako.store.StoreHandler;
import hu.rgai.yako.tools.ProfilePhotoProvider;
import hu.rgai.yako.view.activities.MessageReplyActivity;
import hu.rgai.yako.workers.MessageListerAsyncTask;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

public class MessageListerHandler extends TimeoutHandler {

  private final Context mContext;
  private final MainServiceExtraParams mExtraParams;
  private final String mAccountDispName;

  public static final String MESSAGE_PACK_LOADED_INTENT = "massage_pack_loaded_intent";


  public MessageListerHandler(Context context, MainServiceExtraParams extraParams, String accountDisplayName) {
    mContext = context;
    mExtraParams = extraParams;
    mAccountDispName = accountDisplayName;
  }

  @Override
  public void onTimeout(Context context) {
    if (mExtraParams.isForceQuery() || mExtraParams.isLoadMore()) {
      Toast.makeText(mContext, "Connection onTimeout: " + mAccountDispName, Toast.LENGTH_LONG).show();
    }
  }


  public void finished(MessageListResult messageResult, int exceptionCode, String errorMessage) {
    int newMessageCount = 0;
    if (errorMessage != null) {
      showErrorMessage(exceptionCode, errorMessage);
    }

    if (messageResult != null) {
      MessageListResult.ResultType resultType = messageResult.getResultType();


      if (resultType.equals(MessageListResult.ResultType.MERGE_DELETE)) {
        notifyUIaboutMessageChange();
        return;
      }

      MessageListElement lastUnreadMsg = null;
      Set<Account> accountsToUpdate = new HashSet<Account>();

      if (messageResult.getMessages() != null) {
        for (MessageListElement mle : messageResult.getMessages()) {
          Date lastNotForAcc = YakoApp.getLastNotification(mle.getAccount(), mContext);
          if (!mle.isSeen() && mle.getDate().after(lastNotForAcc)) {
            if (lastUnreadMsg == null) {
              lastUnreadMsg = mle;
            }
            newMessageCount++;
            accountsToUpdate.add(mle.getAccount());
          }
        }
      } else {
        Log.d("rgai", "messageResult.getMessages() is somehow null here, result type is: " + resultType,
                new NullPointerException("messageResult.getMessages() is null"));
      }

      for (Account a : accountsToUpdate) {
        YakoApp.updateLastNotification(a, mContext);
      }
      if (newMessageCount != 0 && StoreHandler.SystemSettings.isNotificationTurnedOn(mContext)) {
        postNotification(newMessageCount, lastUnreadMsg);
      }

      notifyUIaboutMessageChange();
//      Log.d("rgai", " - - - - time to run handler: " + (System.currentTimeMillis() - s) + " ms");
    }
  }


  private void notifyUIaboutMessageChange() {
    Intent i = new Intent(MESSAGE_PACK_LOADED_INTENT);
    LocalBroadcastManager.getInstance(mContext).sendBroadcast(i);
  }



  private void postNotification(int newMessageCount, MessageListElement lastUnreadMsg) {
    YakoApp.setLastNotifiedMessage(lastUnreadMsg);
    NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    if (lastUnreadMsg != null) {
      boolean soundNotification = StoreHandler.SystemSettings.isNotificationSoundTurnedOn(mContext);
      boolean zoneActivated = StoreHandler.isZoneStateActivated(mContext);
      boolean vibrateNotification = StoreHandler.SystemSettings.isNotificationVibrationTurnedOn(mContext);
      boolean isEmailType = lastUnreadMsg.getMessageType().equals(MessageProvider.Type.EMAIL);
      if (!MainActivity.isMainActivityVisible()) {
        String fromNameText = "?";
        if (lastUnreadMsg.getFrom() != null) {
          fromNameText = lastUnreadMsg.getFrom().getName();
        } else {
          if (lastUnreadMsg.getRecipientsList() != null) {
            fromNameText = "";
            for (int i = 0; i < lastUnreadMsg.getRecipientsList().size(); i++) {
              if (i > 0) {
                fromNameText += ",";
              }
              fromNameText += lastUnreadMsg.getRecipientsList().get(i).getName();
            }
          }
        }

        QuickAnswerProvider qap = new DummyQuickAnswerProvider();
        List<String> answers = qap.getQuickAnswers(lastUnreadMsg);
        boolean hasQuickAnswers = false;
        if (answers != null && !answers.isEmpty()) {
          hasQuickAnswers = true;
        }

        Notification quickAnserNotification = null;
        if (hasQuickAnswers && isEmailType) {
          quickAnserNotification = buildNotification(newMessageCount, lastUnreadMsg, true, fromNameText,
                  zoneActivated, soundNotification, vibrateNotification, true, true, answers, null);
        }

        Notification simpleNotif = buildNotification(newMessageCount, lastUnreadMsg, isEmailType, fromNameText,
                zoneActivated, soundNotification, vibrateNotification, false, hasQuickAnswers, answers,
                quickAnserNotification);

        mNotificationManager.notify(Settings.NOTIFICATION_NEW_MESSAGE_ID, simpleNotif);

        // logging...
        KeyguardManager km = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH,
                EventLogger.LOGGER_STRINGS.NOTIFICATION.NOTIFICATION_POPUP_STR
                + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + km.inKeyguardRestrictedInputMode(), true);
      }
      // if main activity visible: only play sound if needed...
      else {
        if ((zoneActivated && lastUnreadMsg.isImportant() && soundNotification)
                || (!zoneActivated && soundNotification)) {
          Uri soundURI = Uri.parse("android.resource://" + mContext.getPackageName() + "/" + R.raw.alarm);
          Ringtone r = RingtoneManager.getRingtone(mContext.getApplicationContext(), soundURI);
          r.play();
        }
      }
    }
  }

  private Notification buildNotification(int newMessageCount, MessageListElement lastUnreadMsg, boolean isEmailType,
                                         String fromNameText, boolean zoneActivated, boolean soundNotification,
                                         boolean vibrateNotification, boolean isQuickAnswerNotification,
                                         boolean hasQuickAnswers, List<String> quickAnswers,
                                         Notification quickAnswerNotification) {

    Bitmap largeIcon;
    if (lastUnreadMsg.getFrom() != null) {
      largeIcon = ProfilePhotoProvider.getImageToUser(mContext, lastUnreadMsg.getFrom()).getBitmap();
    } else {
      largeIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.group_chat);
    }

    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext)
            .setLargeIcon(largeIcon)
            .setSmallIcon(R.drawable.not_ic_action_email)
            .setWhen(lastUnreadMsg.getDate().getTime())
            .setTicker(fromNameText + ": " + lastUnreadMsg.getTitle())
            .setContentInfo(lastUnreadMsg.getAccount().getDisplayName())
            .setContentTitle(fromNameText).setContentText(lastUnreadMsg.getTitle());

    if (!isQuickAnswerNotification) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        simpleButtonHandling(isEmailType, lastUnreadMsg, mBuilder, hasQuickAnswers, quickAnswers, quickAnswerNotification);
      }

      if ((zoneActivated && lastUnreadMsg.isImportant() && soundNotification)
              || (!zoneActivated && soundNotification)) {
        Uri soundURI = Uri.parse("android.resource://" + mContext.getPackageName() + "/" + R.raw.alarm);
        mBuilder.setSound(soundURI);
      }

      if ((zoneActivated && lastUnreadMsg.isImportant() && vibrateNotification)
              || (!zoneActivated) && vibrateNotification) {
        mBuilder.setVibrate(new long[] { 100, 150, 100, 150, 500, 150, 100, 150 });
      }

    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        quickReplyButtonBuilding(lastUnreadMsg, mBuilder, quickAnswers);
      }
    }


    Intent resultIntent;
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
    if (newMessageCount == 1) {
      Class classToLoad = Settings.getAccountTypeToMessageDisplayer().get(lastUnreadMsg.getAccount().getAccountType());
      resultIntent = new Intent(mContext, classToLoad);
      resultIntent.putExtra(IntentStrings.Params.MESSAGE_RAW_ID, lastUnreadMsg.getRawId());
      stackBuilder.addParentStack(MainActivity.class);
    } else {
      resultIntent = new Intent(mContext, MainActivity.class);
    }
    resultIntent.putExtra(IntentStrings.Params.FROM_NOTIFIER, true);
    stackBuilder.addNextIntent(resultIntent);
    PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    mBuilder.setContentIntent(resultPendingIntent);

    setDeleteIntent(mContext, mBuilder, lastUnreadMsg.getRawId());

    mBuilder.setAutoCancel(true);

    return mBuilder.build();
  }


  private void setDeleteIntent(Context context, NotificationCompat.Builder mBuilder, long msgRawId) {
    Intent i = new Intent(IntentStrings.Actions.DELETE_INTENT);
    i.putExtra(IntentStrings.Params.MESSAGE_RAW_ID, msgRawId);
    PendingIntent pi = PendingIntent.getBroadcast(context, DeleteIntentBroadcastReceiver.DELETE_INTENT_REQ_CODE,
            i, PendingIntent.FLAG_UPDATE_CURRENT);
    mBuilder.setDeleteIntent(pi);
  }


  private void simpleButtonHandling(boolean isEmailType, MessageListElement lastUnreadMsg,
                                    NotificationCompat.Builder mBuilder, boolean hasQuickAnswers,
                                    List<String> quickAnswers, Notification quickAnswerNotif) {

    if (isEmailType) {
      Intent intent = new Intent(mContext, MessageReplyActivity.class);
      intent.putExtra(IntentStrings.Params.MESSAGE_RAW_ID, lastUnreadMsg.getRawId());
      intent.putExtra(IntentStrings.Params.FROM_NOTIFIER, true);
      PendingIntent pIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
      mBuilder.addAction(R.drawable.ic_action_reply, "Reply", pIntent);


      if (hasQuickAnswers && quickAnswerNotif != null) {
        intent = new Intent(mContext, NotificationReplaceService.class);
        intent.setAction(NotificationReplaceService.ACTION_SWITCH_NOTIFICATIONS);
        intent.putExtra(NotificationReplaceService.SWITCH_NOTIFICATION_ARG_NOTIFICATION, quickAnswerNotif);
        pIntent = PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.addAction(0, "More...", pIntent);
      }
    } else if (hasQuickAnswers) {
      quickReplyButtonBuilding(lastUnreadMsg, mBuilder, quickAnswers);
    }
  }

  private void quickReplyButtonBuilding(MessageListElement lastUnreadMsg,
                                        NotificationCompat.Builder mBuilder, List<String> quickAnswers) {
    int i = 0;
    for (String answer : quickAnswers) {
      Intent intent = new Intent(mContext, QuickReplyService.class);
      intent.setAction(QuickReplyService.ACTION_QUICK_REPLY);
      intent.putExtra(IntentStrings.Params.MESSAGE_RAW_ID, lastUnreadMsg.getRawId());
      intent.putExtra(IntentStrings.Params.FROM_NOTIFIER, true);
      intent.putExtra(IntentStrings.Params.QUICK_ANSWER_OPTION, answer);
      PendingIntent pIntent = PendingIntent.getService(mContext, i++, intent, PendingIntent.FLAG_UPDATE_CURRENT);
      mBuilder.addAction(0, answer, pIntent);
    }
  }

  private void showErrorMessage(int result, String message) {
    String msg;
    switch (result) {
    case MessageListerAsyncTask.AUTHENTICATION_FAILED_EXCEPTION:
      msg = "Authentication failed: " + message;
      break;
    case MessageListerAsyncTask.UNKNOWN_HOST_EXCEPTION:
    case MessageListerAsyncTask.IOEXCEPTION:
    case MessageListerAsyncTask.CONNECT_EXCEPTION:
    case MessageListerAsyncTask.NO_SUCH_PROVIDER_EXCEPTION:
    case MessageListerAsyncTask.MESSAGING_EXCEPTION:
    case MessageListerAsyncTask.SSL_HANDSHAKE_EXCEPTION:
      msg = message;
      break;
    case MessageListerAsyncTask.NO_INTERNET_ACCESS:
      msg = mContext.getString(R.string.no_internet_access);
      break;
    case MessageListerAsyncTask.NO_ACCOUNT_SET:
      msg = mContext.getString(R.string.no_account_set);
      break;
    default:
      msg = mContext.getString(R.string.exception_unknown);
      break;
    }
    if (MainActivity.isMainActivityVisible()) {
      Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
    }
  }

}
