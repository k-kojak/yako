
package hu.rgai.android.workers;

import android.os.AsyncTask;
import android.os.Handler;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.view.activities.EmailDisplayerActivity;
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

    AccountAndr account;
    
    public EmailMessageMarker(AccountAndr account) {
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
        Logger.getLogger(EmailDisplayerActivity.class.getName()).log(Level.SEVERE, null, ex);
      } catch (MessagingException ex) {
        Logger.getLogger(EmailDisplayerActivity.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
        Logger.getLogger(EmailDisplayerActivity.class.getName()).log(Level.SEVERE, null, ex);
      }

      return null;
    }

  }
