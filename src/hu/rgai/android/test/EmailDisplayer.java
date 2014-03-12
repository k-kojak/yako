package hu.rgai.android.test;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import hu.rgai.android.asynctasks.EmailContentGetter;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.intent.beens.FullSimpleMessageParc;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.intent.beens.PersonAndr;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.services.MainService;

import hu.rgai.android.tools.adapter.ContactListAdapter;

import java.io.File;


import net.htmlparser.jericho.Source;

/**
 * This class responsible for displaying an email message.
 * 
 * @author Tamas Kojedzinszky
 */
public class EmailDisplayer extends ActionBarActivity {

  private ProgressDialog pd = null;
  private Handler handler = null;
  private FullSimpleMessageParc content = null;
  // the subject of the message
  private String subject = null;
  // true if the message is already opened in the past and no need to fetch message from server
  private boolean loadedWithContent = false;
  private String emailID = "-1";
  // account which used to fetch email (if necessary)
  private AccountAndr account;
  // the sender of the message
  private PersonAndr from;
  // a view for displaying content
  private WebView webView = null;
  private WebViewClient webViewClient = null;
  // default character encoding of message
  private String mailCharCode = "UTF-8";
  
  public static final int MESSAGE_REPLY_REQ_CODE = 1;
  
  @Override
  public void onBackPressed() {
    Log.d( "willrgai", EventLogger.LOGGER_STRINGS.EMAIL.EMAIL_BACKBUTTON_STR);
    EventLogger.INSTANCE.writeToLogFile( EventLogger.LOGGER_STRINGS.EMAIL.EMAIL_BACKBUTTON_STR, true );
    super.onBackPressed();
  }
  
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    
    setContentView(R.layout.email_displayer);
    
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    
    //creating webview
    webView = (WebView) findViewById(R.id.email_content);
    webView.getSettings().setDefaultTextEncodingName(mailCharCode);
    
    // getting the information which belongs to this specific message
    MessageListElementParc mlep = (MessageListElementParc)getIntent().getExtras().getParcelable("msg_list_element");
    // setting this message to seen
    MainService.setMessageSeenAndRead(mlep);
    
    // fetching information
    emailID = mlep.getId();
    account = getIntent().getExtras().getParcelable("account");
    subject = mlep.getTitle();
    from = (PersonAndr)mlep.getFrom();
    
    // setting title of activity
    getSupportActionBar().setTitle(account.getAccountType().toString() + " | " + account.getDisplayName());
    
    // if message body already available, get it from there
    if (mlep.getFullMessage() != null) {
      loadedWithContent = true;
      content = (FullSimpleMessageParc)mlep.getFullMessage();
//      webView.loadData(content, "text/html", mailCharCode);
//      webView.loadDataWithBaseURL(null, content, "text/html", mailCharCode, null);
      displayMessage();
    
    } else {
    // if messag ebody not available, fetch it from server
      handler = new EmailContentTaskHandler();
      EmailContentGetter contentGetter = new EmailContentGetter(handler, account);
      contentGetter.execute(emailID);

      pd = new ProgressDialog(this);
      pd.setMessage("Fetching email content...");
      pd.setCancelable(true);
      pd.show();
    }
    
    // creating webview
    webViewClient = new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith("mailto:")) {
          Intent intent = new Intent(EmailDisplayer.this, MessageReply.class);
          intent.putExtra("account", (Parcelable) account);
          intent.putExtra("from", from);
          startActivityForResult(intent, MESSAGE_REPLY_REQ_CODE);
        }
        return true;
      }
    };
    webView.setWebViewClient(webViewClient);
    
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.email_message_options_menu, menu);
    return true;
  }
  
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case (MESSAGE_REPLY_REQ_CODE):
        if (resultCode == MessageReply.MESSAGE_SENT_OK) {
          Toast.makeText(this, "Message sent", Toast.LENGTH_LONG).show();
          finish();
        } else if (resultCode == MessageReply.MESSAGE_SENT_FAILED) {
          Toast.makeText(this, "Failed to send message ", Toast.LENGTH_LONG).show();
        }
        break;
    }
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {
      case R.id.message_reply:
        Intent intent = new Intent(this, MessageReply.class);
        Source source = new Source(content.getContent());
        intent.putExtra("content", source.getRenderer().toString());
        intent.putExtra("subject", subject);
        intent.putExtra("account", (Parcelable)account);
        intent.putExtra("from", from);
        startActivityForResult(intent, MESSAGE_REPLY_REQ_CODE);
        return true;
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
      // if activity loaded without content, than set infos of it to finish it
      Intent resultIntent = new Intent();
      resultIntent.putExtra("message_data", content);
      resultIntent.putExtra("message_id", emailID);
      
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
  
  
  /**
   * Displays the message.
   */
  private void displayMessage() {

	  
      // TODO: e-mail kinézet
	  
	
//	Bitmap img =ProfilePhotoProvider.getImageToUser(this, from.getContactId());
//	ImageView image = null;
//	image.setImageBitmap(img);
//	image.getResources();
	
	Uri uri= ContactListAdapter.getPhotoUriById(this,from.getContactId());
	
	System.out.println(uri.toString());
	
	File myFile = new File(uri.toString());

	System.out.println(myFile.getAbsolutePath());
	
	
    String mail = from.getId();
    
    String c = "<b>" +from.getName() +"</b>" + "<br/>" + "<small>" + "<a href=\"mailto:" + mail +"\">"+ mail + "</a>" + "</small>"+ "<br/>"+ content.getDate() + "<br/>" + content.getSubject() + "<br/>" +"<hr>" +"<br/>" + content.getContent();
    
    webView.loadDataWithBaseURL(null, c.replaceAll("\n", "<br/>"), "text/html", mailCharCode, null);
  }
  
  /**
   * Handles the result of message display.
   */
  private class EmailContentTaskHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      Bundle bundle = msg.getData();
      if (bundle != null) {
        if (bundle.get("content") != null) {
          content = bundle.getParcelable("content");
          
          // content holds a simple Person object, but "from" is came from the MainActivity
          // which is already a PersonAndr, so override it with it, so when creating parcelable
          // there will not be an error
          content.setFrom(from);

          displayMessage();
          if (pd != null) {
            pd.dismiss();
          }
        }
      }
    }
  }

}
