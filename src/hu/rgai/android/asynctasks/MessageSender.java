package hu.rgai.android.asynctasks;

import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.eventlogger.rsa.RSAENCODING;
import hu.rgai.android.intent.beens.RecipientItem;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.intent.beens.account.SmsAccountAndr;
import hu.rgai.android.messageproviders.FacebookMessageProvider;
import hu.rgai.android.messageproviders.SmsMessageProvider;
import hu.rgai.android.test.MessageReply;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.SimpleEmailMessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.EmailMessageRecipient;
import hu.uszeged.inf.rgai.messagelog.beans.FacebookMessageRecipient;
import hu.uszeged.inf.rgai.messagelog.beans.MessageRecipient;
import hu.uszeged.inf.rgai.messagelog.beans.account.EmailAccount;
import hu.uszeged.inf.rgai.messagelog.beans.account.FacebookAccount;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * 
 * @author Tamas Kojedzinszky
 */
public class MessageSender extends AsyncTask<Integer, String, Boolean> {

  private final Context context;
  private final RecipientItem recipient;
  private final Handler handler;
  private final List<AccountAndr> accounts;
  private final String content;
  private final String subject;
  // private String recipients;

  private final String result = null;

  public MessageSender(RecipientItem recipient, List<AccountAndr> accounts, Handler handler,
          String subject, String content, Context context) {
    this.recipient = recipient;
    this.accounts = accounts;
    this.handler = handler;
    this.subject = subject;
    this.content = content;
    this.context = context;
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
    AccountAndr acc = getAccountForType(recipient.getType());
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
        mp = new SmsMessageProvider(context);
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
  private AccountAndr getAccountForType(MessageProvider.Type type) {
    boolean m = type.equals(MessageProvider.Type.EMAIL) || type.equals(MessageProvider.Type.GMAIL);
    for (AccountAndr acc : accounts) {
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
      return new SmsAccountAndr();
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
