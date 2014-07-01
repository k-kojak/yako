package hu.rgai.yako.handlers;

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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import hu.rgai.android.test.MainActivity;
import hu.rgai.android.test.R;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.MainServiceExtraParams;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.beens.MessageListResult;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.eventlogger.rsa.RSAENCODING;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.MessageListDAO;
import hu.rgai.yako.store.StoreHandler;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.tools.ProfilePhotoProvider;
import hu.rgai.yako.view.activities.MessageReplyActivity;
import hu.rgai.yako.view.activities.ThreadDisplayerActivity;
import hu.rgai.yako.workers.MessageListerAsyncTask;
import static hu.rgai.yako.workers.MessageListerAsyncTask.OK;

import java.util.*;

public class MessageListerHandler extends TimeoutHandler {

  private final Context mContext;
  private final MainServiceExtraParams mExtraParams;
  private final String mAccountDispName;
  
  public static final String MESSAGE_PACK_LOADED_INTENT = "massage_pack_loaded_intent";

  private TreeMap<Account, Long> mAccountsAccountKey = null;
  private TreeMap<Long, Account> mAccountsIntegerKey = null;

  public MessageListerHandler(Context context, MainServiceExtraParams extraParams, String accountDisplayName) {
    mContext = context;
    mExtraParams = extraParams;
    mAccountDispName = accountDisplayName;
    mAccountsAccountKey = AccountDAO.getInstance(context).getAccountToIdMap();
    mAccountsIntegerKey = AccountDAO.getInstance(context).getIdToAccountsMap();
  }

  @Override
  public void timeout(Context context) {
    if (mExtraParams.isForceQuery() || mExtraParams.isLoadMore()) {
      Toast.makeText(mContext, "Connection timeout: " + mAccountDispName, Toast.LENGTH_LONG).show();
    }
  }

  
  public void finished(MessageListResult messageResult, boolean loadMore, int result, String errorMessage) {
    Log.d("rgai3", "Message lister handler finished");
    int newMessageCount = 0;
    if (errorMessage != null) {
      showErrorMessage(result, errorMessage);
    }
    if (result == OK) {
      MessageListElement[] newMessages = messageResult.getMessages().toArray(new MessageListElement[messageResult.getMessages().size()]);
      MessageListResult.ResultType resultType = messageResult.getResultType();

      
      /*
       * if NO_CHANGE or ERROR, then just return, we do not have to merge because messages
       * is probably empty anyway...
       */
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
          if (!m.isUpdateFlags() && m.getMessageType().equals(MessageProvider.Type.FACEBOOK) && m.isGroupMessage()) {
            sendBC = true;
            break;
          }
        }
        if (sendBC) {
          Intent i = new Intent(Settings.Intents.NOTIFY_NEW_FB_GROUP_THREAD_MESSAGE);
          mContext.sendBroadcast(i);
        }
      }



      this.mergeMessages(newMessages, loadMore, resultType);
      MessageListElement lastUnreadMsg = null;

      Set<Account> accountsToUpdate = new HashSet<Account>();

      if (ThreadDisplayerActivity.actViewingMessage != null) {
        long accountId = mAccountsAccountKey.get(ThreadDisplayerActivity.actViewingMessage.getAccount());
        long storedMessageId = MessageListDAO.getInstance(mContext).getMessageRawId(ThreadDisplayerActivity.actViewingMessage, accountId);
        if (storedMessageId != -1) {
          MessageListDAO.getInstance(mContext).updateMessageToSeen(storedMessageId);
        }
      }


      for (MessageListElement mle : MessageListDAO.getInstance(mContext).getAllMessages(mAccountsIntegerKey)) {
//        if (mle.equals(ThreadDisplayerActivity.actViewingMessage)) {
//          mle.setSeen(true);
//          mle.setUnreadCount(0);
//        }
        Date lastNotForAcc = YakoApp.getLastNotification(mle.getAccount(), mContext);
        if (!mle.isSeen() && mle.getDate().after(lastNotForAcc)) {
          if (lastUnreadMsg == null) {
            lastUnreadMsg = mle;
          }
          newMessageCount++;
          accountsToUpdate.add(mle.getAccount());
        }
      }
      for (Account a : accountsToUpdate) {
        YakoApp.updateLastNotification(a, mContext);
      }
      if (newMessageCount != 0 && StoreHandler.SystemSettings.isNotificationTurnedOn(mContext)) {
        builNotification(newMessageCount, lastUnreadMsg);
      }
      
      Intent i = new Intent(MESSAGE_PACK_LOADED_INTENT);
      LocalBroadcastManager.getInstance(mContext).sendBroadcast(i);
    }
    
  }

  private void builNotification(int newMessageCount, MessageListElement lastUnreadMsg) {
    YakoApp.setLastNotifiedMessage(lastUnreadMsg);
    NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    if (lastUnreadMsg != null) {
      boolean soundNotification = StoreHandler.SystemSettings.isNotificationSoundTurnedOn(mContext);
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
          largeIcon = ProfilePhotoProvider.getImageToUser(mContext, lastUnreadMsg.getFrom().getContactId()).getBitmap();
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && lastUnreadMsg.getMessageType().equals(MessageProvider.Type.EMAIL)) {
          notificationButtonHandling(lastUnreadMsg, mBuilder);
        }

        if (soundNotification) {
          Uri soundURI = Uri.parse("android.resource://" + mContext.getPackageName() + "/" + R.raw.alarm);
          mBuilder.setSound(soundURI);
        }

        if (StoreHandler.SystemSettings.isNotificationVibrationTurnedOn(mContext)) {
          mBuilder.setVibrate(new long[]{100, 150, 100, 150, 500, 150, 100, 150});
        }

        Intent resultIntent;
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        if (newMessageCount == 1) {
          Class classToLoad = Settings.getAccountTypeToMessageDisplayer().get(lastUnreadMsg.getAccount().getAccountType());
          resultIntent = new Intent(mContext, classToLoad);
          resultIntent.putExtra(IntentStrings.Params.MESSAGE_ID, lastUnreadMsg.getId());
          resultIntent.putExtra(IntentStrings.Params.MESSAGE_ACCOUNT, (Parcelable)lastUnreadMsg.getAccount());
          stackBuilder.addParentStack(MainActivity.class);
        } else {
          resultIntent = new Intent(mContext, MainActivity.class);
        }
        resultIntent.putExtra(IntentStrings.Params.FROM_NOTIFIER, true);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setAutoCancel(true);
        KeyguardManager km = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        mNotificationManager.notify(Settings.NOTIFICATION_NEW_MESSAGE_ID, mBuilder.build());
        EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.NOTIFICATION.NOTIFICATION_POPUP_STR
                + EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR + km.inKeyguardRestrictedInputMode(), true);
      }
      // if main activity visible: only play sound
      else {
        if (soundNotification) {
          Uri soundURI = Uri.parse("android.resource://" + mContext.getPackageName() + "/" + R.raw.alarm);
          Ringtone r = RingtoneManager.getRingtone(mContext.getApplicationContext(), soundURI);
          r.play();
        }
      }
    }
  }

  private void notificationButtonHandling(MessageListElement lastUnreadMsg,
          NotificationCompat.Builder mBuilder) {

    Intent intent = new Intent(mContext, MessageReplyActivity.class);
    intent.putExtra(IntentStrings.Params.MESSAGE_ID, lastUnreadMsg.getId());
    intent.putExtra(IntentStrings.Params.MESSAGE_ACCOUNT, (Parcelable) lastUnreadMsg.getAccount());
    intent.putExtra(IntentStrings.Params.FROM_NOTIFIER, true);
    PendingIntent pIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    mBuilder.addAction(R.drawable.ic_action_reply, "Reply", pIntent);

  }

  /**
   *
   * @param newMessages the list of new messages
   * @param loadMoreRequest true if result of "load more" action, false otherwise, which
   * means this is a refresh action
   */
  private void mergeMessages(MessageListElement[] newMessages, boolean loadMoreRequest, MessageListResult.ResultType resultType) {
    // TODO: optimize message merge
//    synchronized (YakoApp.getMessages()) {
    for (MessageListElement newMessage : newMessages) {
//        MessageListElement storedFoundMessage = null;
      // .contains not work, because the date of new item != date of old item
      // and
      // tree search does not return a valid value
      // causes problem at thread type messages like Facebook

      long accountId = mAccountsAccountKey.get(newMessage.getAccount());
      long storedMessageRawId = MessageListDAO.getInstance(mContext).getMessageRawId(newMessage, accountId);

      // if message is not stored in database
      if (storedMessageRawId == -1) {
        MessageListDAO.getInstance(mContext).insertMessage(newMessage, mAccountsAccountKey);
//          YakoApp.getMessages().add(newMessage);

        if ((ThreadDisplayerActivity.actViewingMessage != null && newMessage.equals(ThreadDisplayerActivity.actViewingMessage))
                || (ThreadDisplayerActivity.actViewingMessage == null && MainActivity.isMainActivityVisible())) {
          loggingNewMessageArrived(newMessage, true);
        } else {
          loggingNewMessageArrived(newMessage, false);
        }
      } else {

        // only update old messages' flags with the new one, and nothing else
        if (newMessage.isUpdateFlags()) {
//            Log.d("rgai3", "update flags..");
          MessageListDAO.getInstance(mContext).updateMessageToSeen(storedMessageRawId);
//            if (storedFoundMessage != null) {
//              storedFoundMessage.setSeen(newMessage.isSeen());
//              storedFoundMessage.setUnreadCount(newMessage.getUnreadCount());
//            }
        } else {
//            Log.d("rgai3", "NOT update flags..");
//            MessageListElement itemToRemove = null;
//            for (MessageListElement oldMessage : YakoApp.getMessages()) {
//              if (newMessage.equals(oldMessage)) {
//                Log.d("rgai3", "IGEN, equals..");
              // first updating person info anyway..
              MessageListDAO.getInstance(mContext).updateFrom(storedMessageRawId, newMessage.getFrom());
//                oldMessage.setFrom(newMessage.getFrom());

              /**
               * "Marking" FB message seen here. Do not change info of the item,
               * if the date is the same, because the queried data would override
               * the displayed object. Facebook does not mark messages as seen
               * when opening it, so we have to handle it at client side. OR
               * if we check the message at FB, then turn it seen at the app
               *
               * plus if newmessage is BEFORE the oldMessage's date, thats ok, because
               * if you delete the last element, then the "new element" is older than the
               * old one
               */
              MessageListElement oldMessage = MessageListDAO.getInstance(mContext).getMessageByRawId(storedMessageRawId, mAccountsIntegerKey);
              if (!newMessage.getDate().equals(oldMessage.getDate()) || newMessage.isSeen() && !oldMessage.isSeen()) {
                MessageListDAO.getInstance(mContext).updateMessage(storedMessageRawId, newMessage.isSeen(), newMessage.getUnreadCount(),
                        newMessage.getDate(), newMessage.getTitle(), newMessage.getSubTitle());
//                  itemToRemove = oldMessage;
//                  break;
              }
//              }
//            }
//            if (itemToRemove != null) {
//              YakoApp.getMessages().remove(itemToRemove);
//              YakoApp.getMessages().add(newMessage);
//            }
        }
      }
    }
      
    // checking for deleted messages here
    if (resultType == MessageListResult.ResultType.CHANGED && !loadMoreRequest) {
      deleteMergeMessages(newMessages);
    }
      
//    }
    
  }

  private void deleteMergeMessages(MessageListElement[] newMessages) {
    if (newMessages.length > 0) {
      long accountId = mAccountsAccountKey.get(newMessages[0].getAccount());
      TreeSet<MessageListElement> msgs = MessageListDAO.getInstance(mContext).getAllMessages(mAccountsIntegerKey, accountId);
      
      SortedSet<MessageListElement> messagesToRemove;
      messagesToRemove = msgs.headSet(newMessages[newMessages.length - 1]);

      
      for (int i = 0; i < newMessages.length; i++) {
        if (messagesToRemove.contains(newMessages[i])) {
          messagesToRemove.remove(newMessages[i]);
        }
      }

      for (MessageListElement mle : messagesToRemove) {
        long accId = mAccountsAccountKey.get(mle.getAccount());
        MessageListDAO.getInstance(mContext).removeMessage(mle, accId);
//        YakoApp.getMessages().remove(mle);
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
      builder.append(ThreadDisplayerActivity.actViewingMessage == null ? "null" : ThreadDisplayerActivity.actViewingMessage.getId());
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
