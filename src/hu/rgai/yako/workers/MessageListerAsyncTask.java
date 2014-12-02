package hu.rgai.yako.workers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import hu.rgai.android.test.MainActivity;
import hu.rgai.yako.beens.*;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.eventlogger.EventLogger.LogFilePaths;
import hu.rgai.yako.handlers.MessageListerHandler;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.messageproviders.SplittedMessageProvider;
import hu.rgai.yako.sql.FullMessageDAO;
import hu.rgai.yako.sql.MessageListDAO;
import hu.rgai.yako.sql.MessageRecipientDAO;
import hu.rgai.yako.view.activities.ThreadDisplayerActivity;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.net.ssl.SSLHandshakeException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.cert.CertPathValidatorException;
import java.util.*;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class MessageListerAsyncTask extends BatchedTimeoutAsyncTask<String, Integer, MessageListResult> {

  public static final int OK = 0;
  public static final int UNKNOWN_HOST_EXCEPTION = 1;
  public static final int IOEXCEPTION = 2;
  public static final int CONNECT_EXCEPTION = 3;
  public static final int NO_SUCH_PROVIDER_EXCEPTION = 4;
  public static final int MESSAGING_EXCEPTION = 5;
  public static final int SSL_HANDSHAKE_EXCEPTION = 6;
  public static final int CERT_PATH_VALIDATOR_EXCEPTION = 7;
  public static final int NO_INTERNET_ACCESS = 8;
  public static final int NO_ACCOUNT_SET = 9;
  public static final int AUTHENTICATION_FAILED_EXCEPTION = 10;

  
  
  private final Context mContext;
  private int result = -1;
  private String errorMessage = null;
  private final Account mAccount;
  private final MessageProvider mMessageProvider;
  private boolean mLoadMore = false;
  private boolean mSplittedMessageSecondPart = false;
  private boolean mNewMessageArrivedRequest = false;
  private TreeMap<String, MessageListElement> mSplittedMessages = new TreeMap<>();
  private boolean mMessageDeleteAtServer = false;
  private int mQueryLimit;
  private int mQueryOffset;
  private final MessageListerHandler mHandler;
  private final RunningSetup mRunningSetup;
  private final TreeMap<Account, Long> mAccountsAccountKey;
  private final TreeMap<Long, Account> mAccountsIntegerKey;
  

  private volatile static HashMap<RunningSetup, Boolean> runningTaskStack = null;

  public MessageListerAsyncTask (Context context, TreeMap<Account, Long> accountsAccountKey,
                                 TreeMap<Long, Account> accountsIntegerKey, Account acc,
                                 MessageProvider messageProvider, boolean loadMore,
                                 boolean splittedMessageSecondPart, boolean newMessageArrivedRequest,
                                 TreeMap<String, MessageListElement> splittedMessages,
                                 boolean messageDeleteAtServer, int queryLimitOverride, int queryOffsetOverride,
                                 MessageListerHandler handler) {
    
    super(handler);
    mContext = context;
    mAccountsAccountKey = accountsAccountKey;
    mAccountsIntegerKey = accountsIntegerKey;
    mAccount = acc;
    mMessageProvider = messageProvider;
    mLoadMore = loadMore;
    mSplittedMessageSecondPart = splittedMessageSecondPart;
    mNewMessageArrivedRequest = newMessageArrivedRequest;
    mSplittedMessages = splittedMessages;
    mMessageDeleteAtServer = messageDeleteAtServer;
    mQueryLimit = queryLimitOverride;
    mQueryOffset = queryOffsetOverride;
    mHandler = handler;

    int offset = 0;
    int limit = Settings.MESSAGE_QUERY_LIMIT;
    if (loadMore) {
      offset = MessageListDAO.getInstance(mContext).getAllMessagesCount(acc.getDatabaseId());
    }

    if (mQueryLimit == -1 || mQueryOffset == -1) {
      mQueryOffset = offset;
      mQueryLimit = limit;
    }
    
    this.mRunningSetup = new RunningSetup(acc, mQueryLimit, mQueryOffset);
    
    if (runningTaskStack == null) {
      runningTaskStack = new HashMap<>();
    }
  }
  

  @Override
  protected MessageListResult doInBackground(String... params) {

    MessageListResult messageResult = null;

    try {

      messageResult = getMessagesFromProvider();

    } catch (AuthenticationFailedException ex) {
      Log.d("rgai", "", ex);
      this.result = AUTHENTICATION_FAILED_EXCEPTION;
      this.errorMessage = mAccount.getDisplayName();
    } catch (CertPathValidatorException ex) {
      Log.d("rgai", "", ex);
      this.result = CERT_PATH_VALIDATOR_EXCEPTION;
    } catch (SSLHandshakeException ex) {
      Log.d("rgai", "", ex);
      this.result = SSL_HANDSHAKE_EXCEPTION;
    } catch (NoSuchProviderException ex) {
      Log.d("rgai", "", ex);
      this.result = NO_SUCH_PROVIDER_EXCEPTION;
    } catch (ConnectException ex) {
      Log.d("rgai", "", ex);
      this.result = CONNECT_EXCEPTION;
    } catch (UnknownHostException ex) {
      Log.d("rgai", "", ex);
      this.result = UNKNOWN_HOST_EXCEPTION;
    } catch (MessagingException ex) {
      Log.d("rgai", "", ex);
      this.result = MESSAGING_EXCEPTION;
    } catch (IOException ex) {
      Log.d("rgai", "", ex);
      this.result = IOEXCEPTION;
    } finally {
      runningTaskStack.put(mRunningSetup, false);
    }
    // if result is UNSET
    if (this.result == -1) {
      this.result = OK;
    }

    if (messageResult != null) {
      processMessageResult(messageResult);
    }

    return messageResult;
  }

  private void processMessageResult(MessageListResult messageResult) {
    MessageListResult.ResultType resultType = messageResult.getResultType();

    // if NO_CHANGE or ERROR, then just return, we do not have to merge because messages are probably empty anyway...
    if (result == OK) {
      if (resultType.equals(MessageListResult.ResultType.SPLITTED_RESULT_SECOND_PART)) {
        handleSplittedMessageSecondPart(messageResult);
      } else if (!resultType.equals(MessageListResult.ResultType.NO_CHANGE)
            && !resultType.equals(MessageListResult.ResultType.ERROR)) {
        runPostProcess(messageResult);
      }
    }

    List<MessageListElement> msgs = new ArrayList<>(MessageListDAO.getInstance(mContext)
            .getAllMessages(mAccountsIntegerKey));

    messageResult.setMessages(msgs);
  }

  private void handleSplittedMessageSecondPart(MessageListResult messageResult) {
    for (MessageListElement mle : messageResult.getMessages()) {
//      searchPersonAndrInRecipients(mContext, mle);
      MessageListDAO.getInstance(mContext).updateMessageToSeen(mle.getRawId(), mle.isSeen());

      MessageRecipientDAO.getInstance(mContext).insertRecipients(mContext, mle);

      FullSimpleMessage fsm = (FullSimpleMessage)mle.getFullMessage();
      FullMessageDAO.getInstance(mContext).insertMessage(mContext, mle.getRawId(), fsm);
    }
  }

  private MessageListResult getMessagesFromProvider()
          throws MessagingException, IOException, CertPathValidatorException {

    MessageListResult messageResult = null;
    if (mMessageProvider != null) {
      if (isSetupRunning(mRunningSetup)) {
        return new MessageListResult(null, MessageListResult.ResultType.CANCELLED);
      }


      if (mSplittedMessageSecondPart && mMessageProvider instanceof SplittedMessageProvider) {
        SplittedMessageProvider smp = (SplittedMessageProvider)mMessageProvider;
        messageResult = smp.loadDataToMessages(mSplittedMessages);
      } else {
        // get the already loaded messages to the specific content type...
        TreeSet<MessageListElement> loadedMessages = MessageListDAO.getInstance(mContext).getAllMessagesToAccount(mAccount);
        if (mMessageDeleteAtServer) {
          long minUID = Long.MAX_VALUE;
          for (MessageListElement mle : loadedMessages) {
            long uid = Long.parseLong(mle.getId());
            if (uid < minUID) minUID = uid;
          }
          messageResult = mMessageProvider.getUIDListForMerge(Long.toString(minUID));
        } else {
          messageResult = mMessageProvider.getMessageList(mQueryOffset, mQueryLimit,
                  loadedMessages, mNewMessageArrivedRequest);
        }
      }
    }
    return messageResult;
  }


  private void runPostProcess(MessageListResult msgResult) {
    MessageListElement[] newMessages;

    if (msgResult.getMessages() != null) {
      newMessages = msgResult
              .getMessages()
              .toArray(new MessageListElement[msgResult.getMessages().size()]);
    } else {
      newMessages = new MessageListElement[0];
    }

    MessageListResult.ResultType resultType = msgResult.getResultType();

    if (resultType.equals(MessageListResult.ResultType.MERGE_DELETE)) {
      reMatchMessages(newMessages);
      return;
    }


    boolean sendBC = false;

    for (MessageListElement m : newMessages) {
      if (!m.isUpdateFlags() && m.getMessageType().equals(MessageProvider.Type.FACEBOOK) && m.isGroupMessage()) {
        sendBC = true;
        break;
      }
    }
    if (sendBC) {
      Intent i = new Intent(Settings.Intents.NOTIFY_NEW_FB_GROUP_THREAD_MESSAGE);
      mContext.sendBroadcast(i);
    }

    TreeMap<Long, MessageListElement> allStoredMessages = MessageListDAO.getInstance(mContext).getAllMessagesMap(mAccountsIntegerKey);
    TreeMap<String, MessageListElement> splittedMessages = mergeMessages(newMessages, allStoredMessages, mLoadMore, resultType);
    msgResult.setSplittedMessages(splittedMessages);
    if (ThreadDisplayerActivity.actViewingMessage != null) {
      long accountId = mAccountsAccountKey.get(ThreadDisplayerActivity.actViewingMessage.getAccount());
      long storedMessageId = MessageListDAO.getInstance(mContext).getMessageRawId(ThreadDisplayerActivity.actViewingMessage,
              accountId);
      if (storedMessageId != -1) {
        MessageListDAO.getInstance(mContext).updateMessageToSeen(storedMessageId, true);
      }
    }
  }


  private void reMatchMessages(MessageListElement[] etalonMessages) {
    if (etalonMessages != null && etalonMessages.length > 0) {
      Account a = etalonMessages[0].getAccount();
      TreeSet<MessageListElement> storedMessages = MessageListDAO.getInstance(mContext).getAllMessagesToAccount(a);
      List<MessageListElement> messagesToRemove = new LinkedList<>();
      for (MessageListElement stored : storedMessages) {
        boolean found = false;
        for (MessageListElement etalonMessage : etalonMessages) {
          if (stored.getId().equals(etalonMessage.getId())) {
            found = true;
            break;
          }
        }
        if (!found) {
          messagesToRemove.add(stored);
        }
      }
      // if there is nothing to remove, do not call remove messages, because that case it will remove all messages
      // associated with the given account
      if (!messagesToRemove.isEmpty()) {
        try {
          MessageListDAO.getInstance(mContext).deleteMessages(mContext, a.getDatabaseId(), messagesToRemove);
        } catch (Exception e) {
          Log.d("rgai", "", e);
        }

      }
    }
  }


  /**
   * @param newMessages     the list of new messages
   * @param loadMoreRequest true if result of "load more" action, false otherwise, which
   *                        means this is a refresh action
   */
  private TreeMap<String, MessageListElement> mergeMessages(MessageListElement[] newMessages, TreeMap<Long, MessageListElement> storedMessages,
                             boolean loadMoreRequest, MessageListResult.ResultType resultType) {
    // TODO: optimize message merge

    TreeMap<String, MessageListElement> splittedMessages = new TreeMap<>();
    for (MessageListElement newMessage : newMessages) {
//        MessageListElement storedFoundMessage = null;
      // .contains not work, because the date of new item != date of old item
      // and
      // tree search does not return a valid value
      // causes problem at thread type messages like Facebook
      long accountId = mAccountsAccountKey.get(newMessage.getAccount());
      long storedMessageRawId = MessageListDAO.getInstance(mContext).getMessageRawId(newMessage, accountId);

      Person searchedFrom = Person.searchPersonAndr(mContext, newMessage.getFrom());
      if (searchedFrom != null && !searchedFrom.equals(newMessage.getFrom())) {
        newMessage.setFrom(searchedFrom);
      }

      // if message is not stored in database
      if (storedMessageRawId == -1) {
//        boolean isImportant = MessagePredictionProvider.Helper.isImportant(mpp.predictMessage(mContext, newMessage));
        boolean isImportant = false;
        newMessage.setIsImportant(isImportant);
        long rawId = MessageListDAO.getInstance(mContext).insertMessage(mContext, newMessage, mAccountsAccountKey);
        newMessage.setRawId(rawId);
        if (newMessage.isSplittedMessage()) {
          splittedMessages.put(newMessage.getId(), newMessage);
        }

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
          if (storedMessages.get(storedMessageRawId).isSeen() != newMessage.isSeen()) {
            MessageListDAO.getInstance(mContext).updateMessageToSeen(storedMessageRawId, newMessage.isSeen());
          }
        } else {
          // first updating person info anyway..
          if (!storedMessages.get(storedMessageRawId).getFrom().equals(newMessage.getFrom())) {
            MessageListDAO.getInstance(mContext).updateFrom(mContext, storedMessageRawId, newMessage.getFrom());
          }

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
          MessageListElement oldMessage = storedMessages.get(storedMessageRawId);
          if (!newMessage.getDate().equals(oldMessage.getDate()) || newMessage.isSeen() && !oldMessage.isSeen()) {
            MessageListDAO.getInstance(mContext).updateMessage(storedMessageRawId, newMessage.isSeen(), newMessage.getUnreadCount(),
                    newMessage.getDate(), newMessage.getTitle(), newMessage.getSubTitle());
          }
        }
      }
    }

    // checking for deleted messages here
    if ((resultType == MessageListResult.ResultType.CHANGED)
            && !loadMoreRequest) {
      deleteMergeMessages(newMessages);
    }

    return splittedMessages;
  }

  private static void searchPersonAndrInRecipients(Context context, MessageListElement mle) {
    if (mle != null && mle.getRecipientsList() != null) {
      List<Person> andrPerson = new LinkedList<>();
      for (Person p : mle.getRecipientsList()) {
        Person searchedFrom = Person.searchPersonAndr(context, p);
        if (searchedFrom != null && !searchedFrom.equals(p)) {
          andrPerson.add(searchedFrom);
        }
      }
      mle.setRecipients(andrPerson);
    }
  }


  private void deleteMergeMessages(MessageListElement[] newMessages) {
    if (newMessages.length > 0) {
      long accountId = mAccountsAccountKey.get(newMessages[0].getAccount());
      TreeSet<MessageListElement> msgs = MessageListDAO
              .getInstance(mContext)
              .getAllMessages(mAccountsIntegerKey, accountId);

      SortedSet<MessageListElement> messagesToRemove;
      messagesToRemove = msgs.headSet(newMessages[newMessages.length - 1]);


      for (MessageListElement newMessage : newMessages) {
        if (messagesToRemove.contains(newMessage)) {
          messagesToRemove.remove(newMessage);
        }
      }

      for (MessageListElement mle : messagesToRemove) {
        long accId = mAccountsAccountKey.get(mle.getAccount());
        try {
          MessageListDAO.getInstance(mContext).deleteMessage(mContext, mle, accId);
        } catch (Exception e) {
          Log.d("yako", "", e);
        }
      }
    }
  }


  private synchronized static boolean isSetupRunning(RunningSetup rs) {
    if (runningTaskStack.containsKey(rs) && runningTaskStack.get(rs)) {
      return true;
    } else {
      runningTaskStack.put(rs, true);
      return false;
    }
  }


  @Override
  protected void onBatchedPostExecute(MessageListResult messageResult) {
    mHandler.finished(messageResult, result, errorMessage);
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
      builder.append(fromID);
      builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      builder.append(mle.getFullMessage());
      EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_MESSAGES_PATH, builder.toString(), false);
    }
  }

}