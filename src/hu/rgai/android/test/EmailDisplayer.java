package hu.rgai.android.test;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import hu.rgai.android.asynctasks.EmailMessageMarker;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.intent.beens.FullSimpleMessageParc;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.intent.beens.PersonAndr;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.services.MainService;
import hu.rgai.android.tools.ProfilePhotoProvider;
import hu.rgai.android.tools.Utils;
import hu.uszeged.inf.rgai.messagelog.beans.HtmlContent;
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
  
  // indicates if activity opens from notification or not
  private boolean fromNotification = false;
  // the subject of the message
  private String subject = null;
  // true if the message is already opened in the past and no need to fetch message from server
  private boolean loadedWithContent = false;
//  private String emailID = "-1";
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
    Log.d("willrgai", EventLogger.LOGGER_STRINGS.EMAIL.EMAIL_BACKBUTTON_STR);
    EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.EMAIL.EMAIL_BACKBUTTON_STR, true);
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
//    MessageListElementParc mlep = (MessageListElementParc) getIntent().getExtras().getParcelable("msg_list_element");
    account = getIntent().getExtras().getParcelable("account");
    String mlepId = getIntent().getExtras().getString("msg_list_element_id");
    MessageListElementParc mlep = MainService.getListElementById(mlepId, account);
    // setting this message to seen in list
    MainService.setMessageSeenAndRead(mlep);
    
    // setting message status to read at imap
    EmailMessageMarker messageMarker = new EmailMessageMarker(handler, account);
    messageMarker.execute(mlepId);

    if (getIntent().getExtras().containsKey("from_notifier") && getIntent().getExtras().getBoolean("from_notifier")) {
      fromNotification = true;
    }
    // fetching information
//    emailID = mlep.getId();
    
    subject = mlep.getTitle();
    from = (PersonAndr) mlep.getFrom();

    // setting title of activity
    getSupportActionBar().setTitle(subject);

    // if message body already available, get it from there
    loadedWithContent = true;
    content = (FullSimpleMessageParc) mlep.getFullMessage();
    displayMessage();

    // creating webview
    webViewClient = new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith("mailto:")) {
          Intent intent = new Intent(EmailDisplayer.this, MessageReply.class);
          intent.putExtra("account", (Parcelable) account);
          intent.putExtra("from", from);
          startActivityForResult(intent, MESSAGE_REPLY_REQ_CODE);
        } else {
          Intent i = new Intent(Intent.ACTION_VIEW);
          i.setData(Uri.parse(url));
          startActivity(i);
        }
        return true;
      }
    };
    webView.setWebViewClient(webViewClient);
//    webView.getSettings().setLoadWithOverviewMode(true);
//    webView.getSettings().setUseWideViewPort(true);
//    webView.getSettings().setBuiltInZoomControls(true);
//    webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
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
        Source source = new Source(content.getContent().getContent());
        intent.putExtra("content", source.getRenderer().toString());
        intent.putExtra("subject", subject);
        intent.putExtra("account", (Parcelable) account);
        intent.putExtra("from", from);
        startActivityForResult(intent, MESSAGE_REPLY_REQ_CODE);
        return true;
      case android.R.id.home:
        Intent upIntent = NavUtils.getParentActivityIntent(this);
        if (fromNotification) {
          TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities();
        } else {
          finish();
        }
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void finish() {
    if (!loadedWithContent) {
      Intent resultIntent = new Intent();
      setResult(Activity.RESULT_OK, resultIntent);
    }
    super.finish();
  }
  
  /**
   * Displays the message.
   */
  private void displayMessage() {
    Bitmap img = ProfilePhotoProvider.getImageToUser(this, from.getContactId());
    ((ImageView)findViewById(R.id.avatar)).setImageBitmap(img);
    ((TextView)findViewById(R.id.from_name)).setText(from.getName());
    ((TextView)findViewById(R.id.date)).setText(Utils.getPrettyTime(content.getDate()));
    
    HtmlContent hc = content.getContent();
    String c = hc.getContent().toString();
    if (hc.getContentType().equals(HtmlContent.ContentType.TEXT_PLAIN)) {
      c = c.replaceAll("\n", "<br/>");
    }
    webView.loadDataWithBaseURL(null, c, "text/html", mailCharCode, null);
  }

}
