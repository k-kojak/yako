
package hu.rgai.android.asynctasks;

import android.os.AsyncTask;
import android.os.Handler;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.test.EmailDisplayer;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.SimpleEmailMessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.account.EmailAccount;
import hu.uszeged.inf.rgai.messagelog.beans.account.GmailAccount;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class EmailMessageMarker extends AsyncTask<String, Integer, Void> {

    Handler handler;
    AccountAndr account;
    
    public EmailMessageMarker(Handler handler, AccountAndr account) {
      this.handler = handler;
      this.account = account;
    }
    
    @Override
    protected Void doInBackground(String... params) {
      
      try {
        if (account.getAccountType().equals(MessageProvider.Type.EMAIL)) {
          SimpleEmailMessageProvider semp = new SimpleEmailMessageProvider((EmailAccount)account);
          semp.markMessageAsRead(params[0]);
        } else if (account.getAccountType().equals(MessageProvider.Type.GMAIL)) {
          SimpleEmailMessageProvider semp = new SimpleEmailMessageProvider((GmailAccount)account);
          semp.markMessageAsRead(params[0]);
        }
      } catch (NoSuchProviderException ex) {
        Logger.getLogger(EmailDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (MessagingException ex) {
        Logger.getLogger(EmailDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
        Logger.getLogger(EmailDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      }

      return null;
    }

  }
