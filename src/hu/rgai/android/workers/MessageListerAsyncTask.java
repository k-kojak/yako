package hu.rgai.android.workers;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.FullSimpleMessage;
import hu.rgai.android.beens.MainServiceExtraParams.ParamStrings;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.beens.MessageListResult;
import hu.rgai.android.beens.Person;
import hu.rgai.android.beens.RunningSetup;
import hu.rgai.android.config.Settings;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.services.MainService;
import static hu.rgai.android.services.MainService.AUTHENTICATION_FAILED_EXCEPTION;
import static hu.rgai.android.services.MainService.CERT_PATH_VALIDATOR_EXCEPTION;
import static hu.rgai.android.services.MainService.CONNECT_EXCEPTION;
import static hu.rgai.android.services.MainService.IOEXCEPTION;
import static hu.rgai.android.services.MainService.MESSAGING_EXCEPTION;
import static hu.rgai.android.services.MainService.NO_SUCH_PROVIDER_EXCEPTION;
import static hu.rgai.android.services.MainService.OK;
import static hu.rgai.android.services.MainService.SSL_HANDSHAKE_EXCEPTION;
import static hu.rgai.android.services.MainService.UNKNOWN_HOST_EXCEPTION;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.cert.CertPathValidatorException;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.net.ssl.SSLHandshakeException;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class MessageListerAsyncTask extends AsyncTask<String, Integer, MessageListResult> {

  private Context context;
  private int result = -1;
  private String errorMessage = null;
  private final Handler handler;
  private final Account acc;
  private final MessageProvider messageProvider;
  private boolean loadMore = false;
  private int queryLimit;
  private int queryOffset;
  private RunningSetup mRunningSetup;

  private volatile static HashMap<RunningSetup, Boolean> runningTaskStack = null;

  public MessageListerAsyncTask(Context context, Handler handler, Account acc, MessageProvider messageProvider,
          boolean loadMore, int queryLimitOverride, int queryOffsetOverride) {
    this.context = context;
    this.handler = handler;
    this.acc = acc;
    this.messageProvider = messageProvider;
    this.context = context;
    this.loadMore = loadMore;
    this.queryLimit = queryLimitOverride;
    this.queryOffset = queryOffsetOverride;

    int offset = 0;
    int limit = acc.getMessageLimit();
    if (loadMore) {
      if (MainService.messages != null) {
        for (MessageListElement m : MainService.messages) {
          if (m.getAccount().equals(acc)) {
            offset++;
          }
        }
      }
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
    
    
    
//    Log.d("rgai", "do in background started: " + acc);
    MessageListResult messageResult = null;
    try {
      if (messageProvider != null) {
        Log.d("rgai", "current runsetup: " + mRunningSetup);
        if (isSetupRunning(mRunningSetup)) {
          Log.d("rgai", "SKIP SETUP");
          return new MessageListResult(null, MessageListResult.ResultType.CANCELLED);
        } else {
          Log.d("rgai", "CONTINUE SETUP");
        }

        // the already loaded messages to the specific content type...
        TreeSet<MessageListElement> loadedMessages = MainService.getLoadedMessages(acc, MainService.messages);
//        Log.d("rgai", "offset, limit: " + offset + ","+limit);
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
    
//    MainService.currentRefreshedAccountCounter++;
//    MainActivity.refreshLoadingStateRate();
//    Log.d("rgai", "do in background ENDED: " + acc);
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
      p = Person.searchPersonAndr(context, mle.getFrom());
      mle.setFrom(p);
      if (!mle.isUpdateFlags()) {
        if (mle.getFullMessage() != null && mle.getFullMessage() instanceof FullSimpleMessage) {
          ((FullSimpleMessage) mle.getFullMessage()).setFrom(p);
        }

        if (mle.getRecipientsList() != null) {
          for (int i = 0; i < mle.getRecipientsList().size(); i++) {
            p = Person.searchPersonAndr(context, mle.getRecipientsList().get(i));
            mle.getRecipientsList().set(i, p);
          }
        }
      }
    }
  }

  @Override
  protected void onPostExecute(MessageListResult messageResult) {
    Bundle bundle = new Bundle();
    Message msg = handler.obtainMessage();
    if (messageResult != null && messageResult.getResultType().equals(MessageListResult.ResultType.CANCELLED)) {
      bundle.putInt(ParamStrings.RESULT, MainService.CANCELLED);
    } else {
      if (this.result == OK) {
        // TODO: ideally this should be 1 parcelable, we should not split it here: MessageListResult should be parcelable object
        bundle.putParcelableArray("messages", messageResult.getMessages().toArray(new MessageListElement[messageResult.getMessages().size()]));
        bundle.putString("message_result_type", messageResult.getResultType().toString());
        // Log.d("rgai", "put messages("+ messages.size() + ") to bundle -> ");
      }
      bundle.putBoolean(ParamStrings.LOAD_MORE, loadMore);
      bundle.putInt(ParamStrings.RESULT, this.result);
      bundle.putString(ParamStrings.ERROR_MESSAGE, this.errorMessage);
    }

    msg.setData(bundle);
    handler.sendMessage(msg);
  }

  @Override
  protected void onPreExecute() {
    // Log.d(Constants.LOG, "onPreExecute");
  }

}