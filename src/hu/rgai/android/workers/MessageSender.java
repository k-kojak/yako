package hu.rgai.android.workers;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;
import hu.rgai.android.beens.EmailMessageRecipient;
import hu.rgai.android.beens.FacebookMessageRecipient;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.eventlogger.rsa.RSAENCODING;
import hu.rgai.android.beens.MessageRecipient;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.EmailAccount;
import hu.rgai.android.beens.FacebookAccount;
import hu.rgai.android.beens.SmsAccount;
import hu.rgai.android.messageproviders.FacebookMessageProvider;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.messageproviders.SimpleEmailMessageProvider;
import hu.rgai.android.messageproviders.SmsMessageProvider;
import hu.rgai.android.test.AnalyticsApp;
import hu.rgai.android.test.MessageReply;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;

/**
 * 
 * @author Tamas Kojedzinszky
 */
public class MessageSender extends AsyncTask<Integer, String, Boolean> {

  private final Context context;
  private final AnalyticsApp mApplication;
  private final MessageRecipient recipient;
  private final Handler handler;
  private final Handler mGeneralHandler;
  private final List<Account> accounts;
  private final String content;
  private final String subject;
  // private String recipients;

  private final String result = null;

  public MessageSender(MessageRecipient recipient, List<Account> accounts, Handler handler,
          String subject, String content, Context context, AnalyticsApp application, Handler generalHandler) {
    this.recipient = recipient;
    this.accounts = accounts;
    this.handler = handler;
    this.mGeneralHandler = generalHandler;
    this.subject = subject;
    this.content = content;
    this.context = context;
    this.mApplication = application;
    // this.subject = subject;
    // this.recipients = recipients;
  }

  boolean isValidContent() {
    if ( recipient.getType().equals( MessageProvider.Type.FACEBOOK)
        || recipient.getType().equals( MessageProvider.Type.SMS) ) {
      if ( content.length() == 0 )
        return false;
    }
    return true;
  }

  @Override
  protected Boolean doInBackground(Integer... params) {
    Account acc = getAccountForType(recipient.getType());
    if ( acc != null && isValidContent() ) {
      MessageProvider mp = null;
      Set<MessageRecipient> recipients = null;
      if (recipient.getType().equals(MessageProvider.Type.FACEBOOK)) {
        mp = new FacebookMessageProvider((FacebookAccount) acc);
        recipients = new HashSet<MessageRecipient>();
        recipients.add(new FacebookMessageRecipient(recipient.getData()));
      } else if (recipient.getType().equals(MessageProvider.Type.EMAIL) || recipient.getType().equals(MessageProvider.Type.GMAIL)) {
        publishProgress(acc.getDisplayName());
        // Toast.makeText(this.g, "Sending message with email: " +
        // acc.getDisplayName(), Toast.LENGTH_LONG).show();
        mp = new SimpleEmailMessageProvider((EmailAccount) acc);
        recipients = new HashSet<MessageRecipient>();
        recipients.add(new EmailMessageRecipient(recipient.getDisplayName(), recipient.getData()));
      } else if (recipient.getType().equals(MessageProvider.Type.SMS)) {
        mp = new SmsMessageProvider(context, mApplication, mGeneralHandler);
        recipients = new HashSet<MessageRecipient>();
        recipients.add((MessageRecipient) recipient);
      }
      if (mp != null && recipients != null) {
        while (true) {
          try {
            mp.sendMessage(recipients, content, subject);
          } catch (NoSuchProviderException ex) {
            Logger.getLogger(MessageReply.class.getName()).log(Level.SEVERE, null, ex);
            break;
          } catch (MessagingException ex) {
            Logger.getLogger(MessageReply.class.getName()).log(Level.SEVERE, null, ex);
            break;
          } catch (IOException ex) {
            Logger.getLogger(MessageReply.class.getName()).log(Level.SEVERE, null, ex);
            break;
          }

          loggingSendMessage();

          break;
        }
      }
    }
    return true;
  }

  private void loggingSendMessage() {
    StringBuilder builder = new StringBuilder();
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SENDMESSAGE_STR);
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    builder.append(recipient.getType());
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    builder.append(content);
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    builder.append(recipient.getContactId());
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    builder.append(RSAENCODING.INSTANCE.encodingString(recipient.getData()));
    EventLogger.INSTANCE.writeToLogFile(builder.toString(), true);
  }

  // TODO: gmail != email
  private Account getAccountForType(MessageProvider.Type type) {
    boolean m = type.equals(MessageProvider.Type.EMAIL) || type.equals(MessageProvider.Type.GMAIL);
    for (Account acc : accounts) {
      if (m) {
        if (acc.getAccountType().equals(MessageProvider.Type.EMAIL) || acc.getAccountType().equals(MessageProvider.Type.GMAIL)) {
          return acc;
        }
      } else {
        if (acc.getAccountType().equals(type)) {
          return acc;
        }
      }
    }
    if (type.equals(MessageProvider.Type.SMS)) {
      return SmsAccount.account;
    }
    return null;
  }

  @Override
  protected void onPostExecute(Boolean success) {
    if (handler != null) {
      Message msg = handler.obtainMessage();
      Bundle bundle = new Bundle();
      bundle.putBoolean("success", success);
      msg.setData(bundle);
      handler.sendMessage(msg);
    }
  }

  @Override
  protected void onProgressUpdate(String... values) {
    Toast.makeText(context, "Sending message with " + values[0], Toast.LENGTH_LONG).show();
  }

}
