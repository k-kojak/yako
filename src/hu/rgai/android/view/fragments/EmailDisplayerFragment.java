package hu.rgai.android.view.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import hu.rgai.android.test.MessageReply;
import hu.rgai.android.test.R;
import hu.rgai.android.tools.ProfilePhotoProvider;
import hu.rgai.android.tools.Utils;
import hu.uszeged.inf.rgai.messagelog.beans.HtmlContent;
import net.htmlparser.jericho.Source;

/**
 * This class responsible for displaying an email message.
 *
 * @author Tamas Kojedzinszky
 */
public class EmailDisplayerFragment extends Fragment {

  private ProgressDialog pd = null;
  private Handler handler = null;
  private FullSimpleMessageParc mContent = null;
  private Menu menu;
  
  // indicates if activity opens from notification or not
  private boolean fromNotification = false;
  // the subject of the message
  private String mSubject = null;
  // true if the message is already opened in the past and no need to fetch message from server
  private boolean mLoadedWithContent = false;
//  private String emailID = "-1";
  // account which used to fetch email (if necessary)
  private AccountAndr mAccount;
  
  private MessageListElementParc mMessage;
  // the sender of the message
  private PersonAndr mFrom;
  
  private View mView;
  // a view for displaying content
  private WebView mWebView = null;
  private WebViewClient mWebViewClient = null;
  // default character encoding of message
  private String mailCharCode = "UTF-8";
  public static final int MESSAGE_REPLY_REQ_CODE = 1;

  public EmailDisplayerFragment(AccountAndr account, MessageListElementParc mlep) {
    this.mAccount = account;
    this.mMessage = mlep;
  }

  
  
  //  @Override
  //  public void onBackPressed() {
  //    Log.d("willrgai", EventLogger.LOGGER_STRINGS.EMAIL.EMAIL_BACKBUTTON_STR);
  //    EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.EMAIL.EMAIL_BACKBUTTON_STR, true);
  //    super.onBackPressed();
  //  }
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.email_displayer, container, false);
   
    mWebView = (WebView) mView.findViewById(R.id.email_content);
    mWebView.getSettings().setDefaultTextEncodingName(mailCharCode);
    
    mSubject = mMessage.getTitle();
    mFrom = (PersonAndr) mMessage.getFrom();


    // if message body already available, get it from there
    mLoadedWithContent = true;
    mContent = (FullSimpleMessageParc) mMessage.getFullMessage();
    displayMessage();
    
    mWebViewClient = new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith("mailto:")) {
          Intent intent = new Intent(EmailDisplayerFragment.this.getActivity(), MessageReply.class);
          intent.putExtra("account", (Parcelable) mAccount);
          intent.putExtra("from", mFrom);
          startActivityForResult(intent, MESSAGE_REPLY_REQ_CODE);
        } else {
          Intent i = new Intent(Intent.ACTION_VIEW);
          i.setData(Uri.parse(url));
          startActivity(i);
        }
        return true;
      }
    };
    mWebView.setWebViewClient(mWebViewClient);
    
    
    return mView;
  }
  
  
//  @Override
//  public void finish() {
//    if (!mLoadedWithContent) {
//      Intent resultIntent = new Intent();
//      setResult(Activity.RESULT_OK, resultIntent);
//    }
//    super.finish();
//  }
  
  /**
   * Displays the message.
   */
  private void displayMessage() {
    Bitmap img = ProfilePhotoProvider.getImageToUser(this.getActivity(), mFrom.getContactId());
    ((ImageView)mView.findViewById(R.id.avatar)).setImageBitmap(img);
    ((TextView)mView.findViewById(R.id.from_name)).setText(mFrom.getName());
    ((TextView)mView.findViewById(R.id.date)).setText(Utils.getPrettyTime(mContent.getDate()));
    
    HtmlContent hc = mContent.getContent();
    String c = hc.getContent().toString();
    if (hc.getContentType().equals(HtmlContent.ContentType.TEXT_PLAIN)) {
      c = c.replaceAll("\n", "<br/>");
    }
    mWebView.loadDataWithBaseURL(null, c, "text/html", mailCharCode, null);
  }

}
