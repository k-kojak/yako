package hu.rgai.android.handlers;

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
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.Toast;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.MainServiceExtraParams;
import hu.rgai.android.beens.MainServiceExtraParams.ParamStrings;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.beens.MessageListResult;
import hu.rgai.android.config.Settings;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.eventlogger.rsa.RSAENCODING;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.services.MainService;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.MainActivity;
import hu.rgai.android.test.MessageReply;
import hu.rgai.android.test.R;
import hu.rgai.android.test.ThreadDisplayer;
import hu.rgai.android.test.YakoApp;
import hu.rgai.android.tools.ProfilePhotoProvider;
import hu.rgai.android.workers.MessageListerAsyncTask;
import static hu.rgai.android.workers.MessageListerAsyncTask.OK;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class MessageListerHandler extends TimeoutHandler {

  private final YakoApp mYakoApp;
  private final MainServiceExtraParams mExtraParams;
  private final String mAccountDispName;

  public MessageListerHandler(YakoApp yakoApp, MainServiceExtraParams extraParams, String accountDisplayName) {
    mYakoApp = yakoApp;
    mExtraParams = extraParams;
    mAccountDispName = accountDisplayName;
  }

  @Override
  public void timeout() {
    if (mExtraParams.isForceQuery() || mExtraParams.isLoadMore()) {
      Toast.makeText(mYakoApp, "Connection timeout: " + mAccountDispName, Toast.LENGTH_LONG).show();
    }
  }

  public void finished(MessageListResult messageResult, boolean loadMore, int result, String errorMessage) {
//    Bundle bundle = new Bundle();
//    Message msg = handler.obtainMessage();
//    if (messageResult != null && messageResult.getResultType().equals(MessageListResult.ResultType.CANCELLED)) {
//      bundle.putInt(ParamStrings.RESULT, MainService.CANCELLED);
//    } else {
//      if (this.result == OK) {
//        // TODO: ideally this should be 1 parcelable, we should not split it here: MessageListResult should be parcelable object
//        bundle.putParcelableArray("messages", messageResult.getMessages().toArray(new MessageListElement[messageResult.getMessages().size()]));
//        bundle.putString("message_result_type", messageResult.getResultType().toString());
//        // Log.d("rgai", "put messages("+ messages.size() + ") to bundle -> ");
//      }
//      bundle.putBoolean(ParamStrings.LOAD_MORE, loadMore);
//      bundle.putInt(ParamStrings.RESULT, this.result);
//      bundle.putString(ParamStrings.ERROR_MESSAGE, this.errorMessage);
//    }
//
//    msg.setData(bundle);
//    handler.sendMessage(msg);

    int newMessageCount = 0;
//      if (bundle != null) {
//        if (bundle.get(ParamStrings.RESULT) != null) {
//          Log.d("rgai", "MessageListerAsyncTaskResult: " + bundle.getInt(ParamStrings.RESULT));
    if (errorMessage != null) {
      showErrorMessage(result, errorMessage);
//            MainActivity.showErrorMessage(bundle.getInt(ParamStrings.RESULT), bundle.getString(ParamStrings.ERROR_MESSAGE));
    }
//          Log.d("rgai", "##currentRefreshedAccountCounter++");
//          MainService.currentRefreshedAccountCounter++;
    // TODO: send broadcast to main activity to refresh loading state rate
//          MainActivity.refreshLoadingStateRate();
//          boolean loadMore = bundle.getBoolean(ParamStrings.LOAD_MORE);
    if (result == OK) {
      MessageListElement[] newMessages = messageResult.getMessages().toArray(new MessageListElement[messageResult.getMessages().size()]);
      MessageListResult.ResultType resultType = messageResult.getResultType();

            // if NO_CHANGE or ERROR, then just return, we do not have to merge because messages
      // is probably empty anyway...
      if (resultType.equals(MessageListResult.ResultType.NO_CHANGE) || resultType.equals(MessageListResult.ResultType.ERROR)) {
        return;
      }

      /*
       * If new message packet comes from Facebook, and newMessages contains groupMessages,
       * send a broadcast so the group Facebook chat is notified about the new messages.
       */
      if (newMessages != null) {
        boolean sendBC = false;
        for (int i = 0; i < newMessages.length; i++) {
          MessageListElement m = newMessages[i];
//                Log.d("rgai", "m:" + m);
          if (!m.isUpdateFlags() && m.getMessageType().equals(MessageProvider.Type.FACEBOOK) && m.isGroupMessage()) {
            sendBC = true;
            break;
          }
        }
        if (sendBC) {
          Intent i = new Intent(Settings.Intents.NOTIFY_NEW_FB_GROUP_THREAD_MESSAGE);
          mYakoApp.sendBroadcast(i);
        }
      }

      this.mergeMessages(newMessages, loadMore, resultType);
      MessageListElement lastUnreadMsg = null;

      Set<Account> accountsToUpdate = new HashSet<Account>();

      for (MessageListElement mle : mYakoApp.getMessages()) {
//              if (mle.equals(MainService.actViewingMessage) || mle.equals(actViewingMessageAtThread)) {
//                mle.setSeen(true);
//                mle.setUnreadCount(0);
//              }
        Date lastNotForAcc = mYakoApp.getLastNotification(mle.getAccount());
        if (!mle.isSeen() && mle.getDate().after(lastNotForAcc)) {
          if (lastUnreadMsg == null) {
            lastUnreadMsg = mle;
          }
          newMessageCount++;
          accountsToUpdate.add(mle.getAccount());
        }
      }
      for (Account a : accountsToUpdate) {
        mYakoApp.updateLastNotification(a);
      }
      if (newMessageCount != 0 && StoreHandler.SystemSettings.isNotificationTurnedOn(mYakoApp)) {
        builNotification(newMessageCount, lastUnreadMsg);
      }
      // TODO: NOTIFY A DIFFERENT WAY!
      MainActivity.notifyMessageChange(loadMore);
    }

    
  }

  private void builNotification(int newMessageCount, MessageListElement lastUnreadMsg) {
    mYakoApp.setLastNotifiedMessage(lastUnreadMsg);
    NotificationManager mNotificationManager = (NotificationManager) mYakoApp.getSystemService(Context.NOTIFICATION_SERVICE);
    if (lastUnreadMsg != null) {
      boolean soundNotification = StoreHandler.SystemSettings.isNotificationSoundTurnedOn(mYakoApp);
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

        Bitmap largeIcon;
        if (lastUnreadMsg.getFrom() != null) {
          largeIcon = ProfilePhotoProvider.getImageToUser(mYakoApp, lastUnreadMsg.getFrom().getContactId()).getBitmap();
        } else {
          largeIcon = BitmapFactory.decodeResource(mYakoApp.getResources(), R.drawable.group_chat);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mYakoApp)
                .setLargeIcon(largeIcon)
                .setSmallIcon(R.drawable.not_ic_action_email)
                .setWhen(lastUnreadMsg.getDate().getTime())
                .setTicker(fromNameText + ": " + lastUnreadMsg.getTitle())
                .setContentInfo(lastUnreadMsg.getAccount().getDisplayName())
                .setContentTitle(fromNameText).setContentText(lastUnreadMsg.getTitle());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && lastUnreadMsg.getMessageType().equals(MessageProvider.Type.EMAIL)) {
          notificationButtonHandling(lastUnreadMsg, mBuilder);
        }

        if (soundNotification) {
          Uri soundURI = Uri.parse("android.resource://" + mYakoApp.getPackageName() + "/" + R.raw.alarm);
          mBuilder.setSound(soundURI);
        }

        if (StoreHandler.SystemSettings.isNotificationVibrationTurnedOn(mYakoApp)) {
          mBuilder.setVibrate(new long[]{100, 150, 100, 150, 500, 150, 100, 150});
        }

        Intent resultIntent;
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mYakoApp);
        if (newMessageCount == 1) {
          Class classToLoad = Settings.getAccountTypeToMessageDisplayer().get(lastUnreadMsg.getAccount().getAccountType());
          resultIntent = new Intent(mYakoApp, classToLoad);
//            resultIntent.putExtra("msg_list_element_id", lastUnreadMsg.getId());
          resultIntent.putExtra("message", lastUnreadMsg);
          stackBuilder.addParentStack(MainActivity.class);
        } else {
          resultIntent = new Intent(mYakoApp, MainActivity.class);
        }
        resultIntent.putExtra(ParamStrings.FROM_NOTIFIER, true);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setAutoCancel(true);
        KeyguardManager km = (KeyguardManager) mYakoApp.getSystemService(Context.KEYGUARD_SERVICE);
        mNotificationManager.notify(Settings.NOTIFICATION_NEW_MESSAGE_ID, mBuilder.build());
        EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.NOTIFICATION.NOTIFICATION_POPUP_STR
                + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + km.inKeyguardRestrictedInputMode(), true);
      } // if main activity visible: only play sound
      else {
        if (soundNotification) {
          Uri soundURI = Uri.parse("android.resource://" + mYakoApp.getPackageName() + "/" + R.raw.alarm);
          Ringtone r = RingtoneManager.getRingtone(mYakoApp.getApplicationContext(), soundURI);
          r.play();
        }
      }
    }
  }

  private void notificationButtonHandling(MessageListElement lastUnreadMsg,
          NotificationCompat.Builder mBuilder) {

    Intent intent = new Intent(mYakoApp, MessageReply.class);
    intent.putExtra("message", (Parcelable) lastUnreadMsg);
    intent.putExtra(ParamStrings.FROM_NOTIFIER, true);
//      intent.putExtra("account", (Parcelable) lastUnreadMsg.getAccount());
    //startActivityForResult(intent, MESSAGE_REPLY_REQ_CODE);
    PendingIntent pIntent = PendingIntent.getActivity(mYakoApp, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    mBuilder.addAction(R.drawable.ic_action_reply, "Reply", pIntent);

  }

  /**
   *
   * @param newMessages the list of new messages
   * @param loadMoreRequest true if result of "load more" action, false otherwise, which
   * means this is a refresh action
   */
  private void mergeMessages(MessageListElement[] newMessages, boolean loadMoreRequest, MessageListResult.ResultType resultType) {
    for (MessageListElement newMessage : newMessages) {
      boolean contains = false;
      MessageListElement storedFoundMessage = null;
        // .contains not work, because the date of new item != date of old item
      // and
      // tree search does not return a valid value
      // causes problem at thread type messages like Facebook

      for (MessageListElement storedMessage : mYakoApp.getMessages()) {
        if (storedMessage.equals(newMessage)) {
          contains = true;
          storedFoundMessage = storedMessage;
        }
      }
      if (!contains) {
        mYakoApp.getMessages().add(newMessage);

        if ((ThreadDisplayer.actViewingMessage != null && newMessage.equals(ThreadDisplayer.actViewingMessage))
                || (ThreadDisplayer.actViewingMessage == null && MainActivity.isMainActivityVisible())) {
          loggingNewMessageArrived(newMessage, true);
        } else {
          loggingNewMessageArrived(newMessage, false);
        }
      } else {
//          Log.d("rgai", "MESSAGE ALREADY IN LIST -> " + newMessage);
        // only update old messages' flags with the new one, and nothing else
        if (newMessage.isUpdateFlags()) {
//            Log.d("rgai", "JUST UPDATE SEEN INFO!");
          if (storedFoundMessage != null) {
            storedFoundMessage.setSeen(newMessage.isSeen());
            storedFoundMessage.setUnreadCount(newMessage.getUnreadCount());
          }
        } else {
//            Log.d("rgai", "HANDLE AS \"NEW\" MESSAGE -> " + newMessage);
          MessageListElement itemToRemove = null;
          for (MessageListElement oldMessage : mYakoApp.getMessages()) {
            if (newMessage.equals(oldMessage)) {
              // first updating person info anyway..
              oldMessage.setFrom(newMessage.getFrom());

              /*
               * "Marking" FB message seen here. Do not change info of the item,
               * if the date is the same, so the queried data will not override
               * the displayed object. Facebook does not mark messages as seen
               * when opening them, so we have to handle it at client side. OR
               * if we check the message at FB, then turn it seen at the app
               */
              if (newMessage.getDate().after(oldMessage.getDate()) || newMessage.isSeen() && !oldMessage.isSeen()) {
                itemToRemove = oldMessage;
                break;
              }
            }
          }
          if (itemToRemove != null) {
            mYakoApp.getMessages().remove(itemToRemove);
            mYakoApp.getMessages().add(newMessage);
          }
        }
      }
    }

    // checking for deleted messages here
    if (resultType == MessageListResult.ResultType.CHANGED && !loadMoreRequest) {
      deleteMergeMessages(newMessages);
    }
  }

  private synchronized void deleteMergeMessages(MessageListElement[] newMessages) {
    if (newMessages.length > 0) {
      TreeSet<MessageListElement> msgs = mYakoApp.getLoadedMessages(newMessages[0].getAccount());
      SortedSet<MessageListElement> messagesToRemove = msgs.headSet(newMessages[newMessages.length - 1]);

      for (int i = 0; i < newMessages.length; i++) {
        if (messagesToRemove.contains(newMessages[i])) {
          messagesToRemove.remove(newMessages[i]);
        }
      }
      for (MessageListElement mle : messagesToRemove) {
        mYakoApp.getMessages().remove(mle);
      }
    }
  }

  private void loggingNewMessageArrived(MessageListElement mle, boolean messageIsVisible) {
    if (mle.getDate().getTime() > EventLogger.INSTANCE.getLogfileCreatedTime()) {
      String fromID = mle.getFrom() == null ? mle.getRecipientsList() == null ? "NULL" : mle.getRecipientsList().get(0).getId() : mle.getFrom().getId();
      StringBuilder builder = new StringBuilder();
      builder.append(mle.getDate().getTime());
      builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      builder.append(EventLogger.LOGGER_STRINGS.OTHER.NEW_MESSAGE_STR);
      builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      builder.append(mle.getId());
      builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      builder.append(ThreadDisplayer.actViewingMessage == null ? "null" : ThreadDisplayer.actViewingMessage.getId());
      builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      builder.append(messageIsVisible);
      builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      builder.append(mle.getMessageType());
      builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      builder.append(RSAENCODING.INSTANCE.encodingString(fromID));
      EventLogger.INSTANCE.writeToLogFile(builder.toString(), false);
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
        msg = mYakoApp.getString(R.string.no_internet_access);
        break;
      case MessageListerAsyncTask.NO_ACCOUNT_SET:
        msg = mYakoApp.getString(R.string.no_account_set);
        break;
      default:
        msg = mYakoApp.getString(R.string.exception_unknown);
        break;
    }
    if (MainActivity.isMainActivityVisible()) {
      Toast.makeText(mYakoApp, msg, Toast.LENGTH_LONG).show();
    }
  }

}
