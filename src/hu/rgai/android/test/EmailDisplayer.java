package hu.rgai.android.test;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import com.sun.mail.smtp.SMTPTransport;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.SimpleEmailMessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.Account;
import hu.uszeged.inf.rgai.messagelog.beans.EmailAccount;
import hu.uszeged.inf.rgai.messagelog.beans.FacebookAccount;
import hu.uszeged.inf.rgai.messagelog.beans.FullEmailMessage;
import hu.uszeged.inf.rgai.messagelog.beans.GmailAccount;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailDisplayer extends Activity {

  private ProgressDialog pd = null;
  private Handler handler = null;
  private String content = null;
  private boolean loadedWithContent = false;
  private int emailID = -1;
  private AccountAndr account;
  private WebView webView = null;
  private String mailCharCode = "UTF-8";
  
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    
    setContentView(R.layout.email_displayer);
    webView = (WebView) findViewById(R.id.email_content);
    webView.getSettings().setDefaultTextEncodingName(mailCharCode);
    
    emailID = getIntent().getExtras().getInt("email_id");
    account = getIntent().getExtras().getParcelable("account");
    if (getIntent().getExtras().containsKey("email_content")) {
      loadedWithContent = true;
      content = getIntent().getExtras().getString("email_content");
//      webView.loadData(content, "text/html", mailCharCode);
      webView.loadDataWithBaseURL(null, content, "text/html", mailCharCode, null);
    } else {
      handler = new EmailContentTaskHandler();
      EmailContentGetter contentGetter = new EmailContentGetter(handler, account);
      contentGetter.execute(emailID);

      pd = new ProgressDialog(this);
      pd.setMessage("Fetching email content...");
      pd.setCancelable(false);
      pd.show();
    }
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.email_options_menu, menu);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {
      case R.id.email_reply:
        Intent intent = new Intent(this, ScheduleStarter.class);
        intent.setAction(Context.ALARM_SERVICE);
        this.sendBroadcast(intent);
//        EmailReplySender replySender = new EmailReplySender();
//        replySender.execute();
//        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void finish() {
    if (!loadedWithContent) {
      Intent resultIntent = new Intent();
      resultIntent.putExtra("email_content", content);
      resultIntent.putExtra("email_id", emailID);
      
//      if (account.getAccountType().equals(MessageProvider.Type.EMAIL)) {
        resultIntent.putExtra("account", (Parcelable)account);
//      } else if (account.getAccountType().equals(MessageProvider.Type.GMAIL)) {
//        resultIntent.putExtra("account", new GmailAccountParc((GmailAccount)account));
//      } else if (account.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
//        resultIntent.putExtra("account", new FacebookAccountParc((FacebookAccount)account));
//      }
      setResult(Activity.RESULT_OK, resultIntent);
    }
    super.finish(); //To change body of generated methods, choose Tools | Templates.
  }
  
  private class EmailContentTaskHandler extends Handler {
    
    @Override
    public void handleMessage(Message msg) {
      Bundle bundle = msg.getData();
      if (bundle != null) {
        if (bundle.get("content") != null) {
          content = bundle.getString("content");
//          webView.loadData(content, "text/html", mailCharCode);
          webView.loadDataWithBaseURL(null, content, "text/html", mailCharCode, null);
          if (pd != null) {
            pd.dismiss();
          }
        }
      }
    }
  }
  
  private class EmailContentGetter extends AsyncTask<Integer, Integer, String> {

    Handler handler;
    AccountAndr account;
    
    public EmailContentGetter(Handler handler, AccountAndr account) {
      this.handler = handler;
      this.account = account;
    }
    
    @Override
    protected String doInBackground(Integer... params) {
//      SharedPreferences sharedPref = getSharedPreferences(getString(R.string.settings_email_file_key), Context.MODE_PRIVATE);
//      String email = sharedPref.getString(getString(R.string.settings_saved_email), "");
//      String pass = sharedPref.getString(getString(R.string.settings_saved_pass), "");
//      String imap = sharedPref.getString(getString(R.string.settings_saved_imap), "");
//      MailProvider2 em = new MailProvider2(email, pass, imap, Pass.smtp);
      String content = null;
      
      try {
        if (account.getAccountType().equals(MessageProvider.Type.EMAIL)) {
          SimpleEmailMessageProvider semp = new SimpleEmailMessageProvider((EmailAccount)account);
          FullEmailMessage fm = (FullEmailMessage)semp.getMessage(params[0]);
          content = fm.getContent();
        } else if (account.getAccountType().equals(MessageProvider.Type.GMAIL)) {
          SimpleEmailMessageProvider semp = new SimpleEmailMessageProvider((GmailAccount)account);
          FullEmailMessage fm = (FullEmailMessage)semp.getMessage(params[0]);
          content = fm.getContent();
        } else if (account.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
          // TODO: getting facebook message
        }
      } catch (NoSuchProviderException ex) {
        Logger.getLogger(EmailDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (MessagingException ex) {
        Logger.getLogger(EmailDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
        Logger.getLogger(EmailDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      }
//      try {
//        content = em.getMailContent2(params[0]);
//      } catch (IOException ex) {
//        Logger.getLogger(MyService.class.getName()).log(Level.SEVERE, null, ex);
//      } catch (MessagingException ex) {
//        Logger.getLogger(EmailDisplayer.class.getName()).log(Level.SEVERE, null, ex);
//      }
//
      return content;
    }

    @Override
    protected void onPostExecute(String result) {
      Message msg = handler.obtainMessage();
      Bundle bundle = new Bundle();
      bundle.putString("content", result);
      msg.setData(bundle);
      handler.sendMessage(msg);
    }


//    @Override
//    protected void onProgressUpdate(Integer... values) {
//      Log.d(Constants.LOG, "onProgressUpdate");
//      Message msg = handler.obtainMessage();
//      Bundle bundle = new Bundle();
//
//      bundle.putInt("progress", values[0]);
//      msg.setData(bundle);
//      handler.sendMessage(msg);
//    }
  }
  
  private class EmailReplySender extends AsyncTask<Integer, Integer, String> {

//    Handler handler;
//    AccountAndr account;
    
    public EmailReplySender() {
//      this.handler = handler;
//      this.account = account;
    }
    
    @Override
    protected String doInBackground(Integer... params) {

      try {
//          SharedPreferences sharedPref = getSharedPreferences(getString(R.string.settings_email_file_key), Context.MODE_PRIVATE);
          String email = "";
          String pass = "";
//          String imap = sharedPref.getString(getString(R.string.settings_saved_imap), "");
          Properties props = System.getProperties();
          props.put("mail.smtps.host","smtp.gmail.com");
          props.put("mail.smtps.auth","true");
          Session session = Session.getInstance(props, null);
          javax.mail.Message msg = new MimeMessage(session);
          msg.setFrom(new InternetAddress(email));
          msg.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(email, false));
          msg.setSubject("Lorem ipsum -> " + System.currentTimeMillis());
          msg.setText("Árvíztűrő tükörfúrógép.");
          msg.setHeader("X-Mailer", "Copy of Tov Are's program");
          msg.setSentDate(new Date());
          SMTPTransport t = (SMTPTransport)session.getTransport("smtps");
          t.connect("smtp.gmail.com", email, pass);
          t.sendMessage(msg, msg.getAllRecipients());
//          System.out.println("Response: " + t.getLastServerResponse());
          t.close();
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      return "";
    }

    @Override
    protected void onPostExecute(String result) {
//      Message msg = handler.obtainMessage();
//      Bundle bundle = new Bundle();
//      bundle.putString("content", result);
//      msg.setData(bundle);
//      handler.sendMessage(msg);
    }


//    @Override
//    protected void onProgressUpdate(Integer... values) {
//      Log.d(Constants.LOG, "onProgressUpdate");
//      Message msg = handler.obtainMessage();
//      Bundle bundle = new Bundle();
//
//      bundle.putInt("progress", values[0]);
//      msg.setData(bundle);
//      handler.sendMessage(msg);
//    }
  }
  
}
