package hu.rgai.android.workers;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;
import hu.rgai.android.beens.EmailMessageRecipient;
import hu.rgai.android.beens.FacebookMessageRecipient;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.eventlogger.rsa.RSAENCODING;
import hu.rgai.android.beens.MessageRecipient;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.EmailAccount;
import hu.rgai.android.beens.FacebookAccount;
import hu.rgai.android.handlers.MessageSendHandler;
import hu.rgai.android.messageproviders.FacebookMessageProvider;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.messageproviders.SimpleEmailMessageProvider;
import hu.rgai.android.messageproviders.SmsMessageProvider;
import hu.rgai.android.test.YakoApp;
import hu.rgai.android.test.MessageReply;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;

/**
 * 
 * @author Tamas Kojedzinszky
 */
public class MessageSender extends TimeoutAsyncTask<Void, String, Integer> {

  private final Context context;
  private final MessageRecipient recipient;
  private final MessageSendHandler handler;
  private final Account fromAccount;
  private final String content;
  private final String subject;
  // private String recipients;
  
  private static final int SUCCESS = 0;
  private static final int FAIL = 1;


  public MessageSender(MessageRecipient recipient, Account fromAccount, MessageSendHandler handler,
          String subject, String content, Context context, YakoApp application, Handler generalHandler) {
    
    super(handler);
    
    this.recipient = recipient;
    this.fromAccount = fromAccount;
    this.handler = handler;
    this.subject = subject;
    this.content = content;
    this.context = context;
  }

  private boolean isValidContent() {
    if ( recipient.getType().equals( MessageProvider.Type.FACEBOOK)
        || recipient.getType().equals( MessageProvider.Type.SMS) ) {
      if ( content.length() == 0 )
        return false;
    }
    return true;
  }

  @Override
  protected Integer doInBackground(Void... params) {
    if ( fromAccount != null && isValidContent() ) {
      MessageProvider mp = null;
      Set<MessageRecipient> recipients = null;
      if (recipient.getType().equals(MessageProvider.Type.FACEBOOK)) {
        mp = new FacebookMessageProvider((FacebookAccount) fromAccount);
        recipients = new HashSet<MessageRecipient>();
        recipients.add(new FacebookMessageRecipient(recipient.getData()));
      } else if (recipient.getType().equals(MessageProvider.Type.EMAIL) || recipient.getType().equals(MessageProvider.Type.GMAIL)) {
        publishProgress(fromAccount.getDisplayName());
        mp = new SimpleEmailMessageProvider((EmailAccount) fromAccount);
        recipients = new HashSet<MessageRecipient>();
        recipients.add(new EmailMessageRecipient(recipient.getDisplayName(), recipient.getData()));
      } else if (recipient.getType().equals(MessageProvider.Type.SMS)) {
        mp = new SmsMessageProvider(context);
        recipients = new HashSet<MessageRecipient>();
        recipients.add((MessageRecipient) recipient);
      }
      if (mp != null && recipients != null) {
        try {
          mp.sendMessage(recipients, content, subject);
        } catch (NoSuchProviderException ex) {
          Logger.getLogger(MessageReply.class.getName()).log(Level.SEVERE, null, ex);
          return FAIL;
        } catch (MessagingException ex) {
          Logger.getLogger(MessageReply.class.getName()).log(Level.SEVERE, null, ex);
          return FAIL;
        } catch (IOException ex) {
          Logger.getLogger(MessageReply.class.getName()).log(Level.SEVERE, null, ex);
          return FAIL;
        }
        loggingSendMessage();
      }
    }
    return SUCCESS;
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

  @Override
  protected void onPostExecute(Integer resultCode) {
    if (handler != null) {
      if (resultCode == SUCCESS) {
        handler.success(recipient.getDisplayName());
      } else {
        handler.fail(recipient.getDisplayName());
      }
    }
  }

  @Override
  protected void onProgressUpdate(String... values) {
    Toast.makeText(context, "Sending message ...", Toast.LENGTH_SHORT).show();
  }

}
