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
  private int result;
  private String errorMessage = null;
  private final Handler handler;
  private final Account acc;
  private final MessageProvider messageProvider;
  private boolean loadMore = false;
  private int queryLimit;
  private int queryOffset;

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

    if (runningTaskStack == null) {
      runningTaskStack = new HashMap<RunningSetup, Boolean>();
    }
  }

  @Override
  protected MessageListResult doInBackground(String... params) {
    RunningSetup runSetup = new RunningSetup(acc, queryOffset, queryLimit);
    if (isSetupRunning(runSetup)) {
      Log.d("rgai", "This setup is already running: " + runSetup);
      return new MessageListResult(null, MessageListResult.ResultType.CANCELLED);
    }
    
    Log.d("rgai", "do in background started: " + acc);
    MessageListResult messageResult = null;
    try {
      if (messageProvider != null) {
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

        if (queryLimit != -1 && queryOffset != -1) {
          offset = queryOffset;
          limit = queryLimit;
        }

        // the already loaded messages to the specific content type...
        TreeSet<MessageListElement> loadedMessages = MainService.getLoadedMessages(acc, MainService.messages);

        messageResult = messageProvider.getMessageList(offset, limit, loadedMessages, Settings.MAX_SNIPPET_LENGTH);
        if (messageResult.getResultType().equals(MessageListResult.ResultType.CHANGED)) {
          // searching for android contacts
          extendPersonObject(messageResult.getMessages());
        }

      }

    } catch (AuthenticationFailedException ex) {
      ex.printStackTrace();
      this.result = AUTHENTICATION_FAILED_EXCEPTION;
      this.errorMessage = acc.getDisplayName();
      return null;
    } catch (CertPathValidatorException ex) {
      ex.printStackTrace();
      this.result = CERT_PATH_VALIDATOR_EXCEPTION;
      return null;
    } catch (SSLHandshakeException ex) {
      ex.printStackTrace();
      this.result = SSL_HANDSHAKE_EXCEPTION;
      return null;
    } catch (NoSuchProviderException ex) {
      ex.printStackTrace();
      this.result = NO_SUCH_PROVIDER_EXCEPTION;
      return null;
    } catch (ConnectException ex) {
      ex.printStackTrace();
      this.result = CONNECT_EXCEPTION;
      return null;
    } catch (UnknownHostException ex) {
      ex.printStackTrace();
      this.result = UNKNOWN_HOST_EXCEPTION;
      return null;
    } catch (MessagingException ex) {
      ex.printStackTrace();
      this.result = MESSAGING_EXCEPTION;
      return null;
    } catch (IOException ex) {
      ex.printStackTrace();
      this.result = IOEXCEPTION;
      return null;
    }
    this.result = OK;
    runningTaskStack.put(runSetup, false);
    Log.d("rgai", "do in background ENDED: " + acc);
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
    if (messageResult.getResultType().equals(MessageListResult.ResultType.CANCELLED)) {
      return;
    }
    Message msg = handler.obtainMessage();
    Bundle bundle = new Bundle();
    if (this.result == OK) {
      // TODO: ideally this should be 1 parcelable, we should not split it here: MessageListResult should be parcelable object
      bundle.putParcelableArray("messages", messageResult.getMessages().toArray(new MessageListElement[messageResult.getMessages().size()]));
      bundle.putString("message_result_type", messageResult.getResultType().toString());
      // Log.d("rgai", "put messages("+ messages.size() + ") to bundle -> ");
    }
    bundle.putBoolean(ParamStrings.LOAD_MORE, loadMore);
    bundle.putInt(ParamStrings.RESULT, this.result);
    bundle.putString(ParamStrings.ERROR_MESSAGE, this.errorMessage);

    msg.setData(bundle);
    handler.sendMessage(msg);
  }

  @Override
  protected void onPreExecute() {
    // Log.d(Constants.LOG, "onPreExecute");
  }

  private class RunningSetup {

    private final Account mAccount;
    private final int offset;
    private final int limit;

    public RunningSetup(Account mAccount, int offset, int limit) {
      this.mAccount = mAccount;
      this.offset = offset;
      this.limit = limit;
    }

    @Override
    public int hashCode() {
      int hash = 7;
      hash = 11 * hash + (this.mAccount != null ? this.mAccount.hashCode() : 0);
      hash = 11 * hash + this.offset;
      hash = 11 * hash + this.limit;
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final RunningSetup other = (RunningSetup) obj;
      if (this.mAccount != other.mAccount && (this.mAccount == null || !this.mAccount.equals(other.mAccount))) {
        return false;
      }
      if (this.offset != other.offset) {
        return false;
      }
      if (this.limit != other.limit) {
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return "RunningSetup{" + "mAccount=" + mAccount + ", offset=" + offset + ", limit=" + limit + '}';
    }
    
  }
  
}
