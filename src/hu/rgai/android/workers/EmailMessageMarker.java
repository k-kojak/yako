
package hu.rgai.android.workers;

import android.os.AsyncTask;
import android.os.Handler;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.EmailAccount;
import hu.rgai.android.beens.GmailAccount;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.messageproviders.SimpleEmailMessageProvider;
import hu.rgai.android.view.activities.EmailDisplayerActivity;
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

    Account account;
    
    public EmailMessageMarker(Account account) {
      this.account = account;
    }
    
    @Override
    protected Void doInBackground(String... params) {
      
      try {
        if (account.getAccountType().equals(MessageProvider.Type.EMAIL)) {
          SimpleEmailMessageProvider semp = new SimpleEmailMessageProvider((EmailAccount)account);
          semp.markMessageAsRead(params[0], true);
        } else if (account.getAccountType().equals(MessageProvider.Type.GMAIL)) {
          SimpleEmailMessageProvider semp = new SimpleEmailMessageProvider((GmailAccount)account);
          semp.markMessageAsRead(params[0], true);
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
