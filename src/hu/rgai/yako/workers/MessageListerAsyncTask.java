package hu.rgai.yako.workers;

import android.content.Context;
import android.os.Message;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.FullSimpleMessage;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.beens.MessageListResult;
import hu.rgai.yako.beens.Person;
import hu.rgai.yako.beens.RunningSetup;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.handlers.MessageListerHandler;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.MessageListDAO;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.cert.CertPathValidatorException;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.net.ssl.SSLHandshakeException;

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
  public static final int CANCELLED = 11;
  
  
  
  private final Context mContext;
  private int result = -1;
  private String errorMessage = null;
  private final Account acc;
  private final MessageProvider messageProvider;
  private boolean loadMore = false;
  private int queryLimit;
  private int queryOffset;
  private final MessageListerHandler mHandler;
  private final RunningSetup mRunningSetup;
  

  private volatile static HashMap<RunningSetup, Boolean> runningTaskStack = null;

  public MessageListerAsyncTask(Context context, Account acc, MessageProvider messageProvider,
          boolean loadMore, int queryLimitOverride, int queryOffsetOverride, MessageListerHandler handler) {
    
    super(handler);
    mContext = context;
    this.acc = acc;
    this.messageProvider = messageProvider;
    this.loadMore = loadMore;
    this.queryLimit = queryLimitOverride;
    this.queryOffset = queryOffsetOverride;
    this.mHandler = handler;

    int offset = 0;
    int limit = Settings.MESSAGE_QUERY_LIMIT;
    if (loadMore) {
//      if (YakoApp.getMessages() != null) {
//        for (MessageListElement m : YakoApp.getMessages()) {
//          if (m.getAccount().equals(acc)) {
            offset = MessageListDAO.getInstance(mContext).getAllMessagesCount(acc.getDatabaseId());
//          }
//        }
//      }
    }

    if (queryLimit == -1 || queryOffset == -1) {
      queryOffset = offset;
      queryLimit = limit;
    }
    
    this.mRunningSetup = new RunningSetup(acc, queryLimit, queryOffset);
    
    if (runningTaskStack == null) {
      runningTaskStack = new HashMap<RunningSetup, Boolean>();
    }
  }
  
  public void cancelRunningSetup() {
    if (runningTaskStack.containsKey(mRunningSetup)) {
      runningTaskStack.remove(mRunningSetup);
    }
  }

  @Override
  protected MessageListResult doInBackground(String... params) {
    
    
    MessageListResult messageResult = null;
    try {
      if (messageProvider != null) {
        if (isSetupRunning(mRunningSetup)) {
          return new MessageListResult(null, MessageListResult.ResultType.CANCELLED);
        } else {
        }

        // the already loaded messages to the specific content type...
        TreeMap<Long, Account> accounts = AccountDAO.getInstance(mContext).getIdToAccountsMap();
        TreeSet<MessageListElement> loadedMessages = MessageListDAO.getInstance(mContext).getAllMessages(accounts, acc.getDatabaseId());
        messageResult = messageProvider.getMessageList(queryOffset, queryLimit, loadedMessages, Settings.MAX_SNIPPET_LENGTH);
        if (messageResult.getResultType().equals(MessageListResult.ResultType.CHANGED)) {
          // searching for android contacts
          extendPersonObject(messageResult.getMessages());
        }
      }
    } catch (AuthenticationFailedException ex) {
      ex.printStackTrace();
      this.result = AUTHENTICATION_FAILED_EXCEPTION;
      this.errorMessage = acc.getDisplayName();
    } catch (CertPathValidatorException ex) {
      ex.printStackTrace();
      this.result = CERT_PATH_VALIDATOR_EXCEPTION;
    } catch (SSLHandshakeException ex) {
      ex.printStackTrace();
      this.result = SSL_HANDSHAKE_EXCEPTION;
    } catch (NoSuchProviderException ex) {
      ex.printStackTrace();
      this.result = NO_SUCH_PROVIDER_EXCEPTION;
    } catch (ConnectException ex) {
      ex.printStackTrace();
      this.result = CONNECT_EXCEPTION;
    } catch (UnknownHostException ex) {
      ex.printStackTrace();
      this.result = UNKNOWN_HOST_EXCEPTION;
    } catch (MessagingException ex) {
      ex.printStackTrace();
      this.result = MESSAGING_EXCEPTION;
    } catch (IOException ex) {
      ex.printStackTrace();
      this.result = IOEXCEPTION;
    } finally {
      runningTaskStack.put(mRunningSetup, false);
    }
    // if result is UNSET
    if (this.result == -1) {
      this.result = OK;
    }
    
    return messageResult;
  }


  private synchronized static boolean isSetupRunning(RunningSetup rs) {
    if (runningTaskStack.containsKey(rs) && runningTaskStack.get(rs)) {
      return true;
    } else {
      runningTaskStack.put(rs, true);
      return false;
    }
  }


  private void extendPersonObject(List<MessageListElement> origi) {
    Person p;
    for (MessageListElement mle : origi) {
      p = Person.searchPersonAndr(mContext, mle.getFrom());
      mle.setFrom(p);
      if (!mle.isUpdateFlags()) {
        if (mle.getFullMessage() != null && mle.getFullMessage() instanceof FullSimpleMessage) {
          ((FullSimpleMessage) mle.getFullMessage()).setFrom(p);
        }

        if (mle.getRecipientsList() != null) {
          for (int i = 0; i < mle.getRecipientsList().size(); i++) {
            p = Person.searchPersonAndr(mContext, mle.getRecipientsList().get(i));
            mle.getRecipientsList().set(i, p);
          }
        }
      }
    }
  }


  @Override
  protected void onBatchedPostExecute(MessageListResult messageResult) {
    mHandler.finished(messageResult, loadMore, result, errorMessage);
  }

}