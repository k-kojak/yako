package hu.rgai.android.view.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import hu.rgai.android.intent.beens.FullSimpleMessageParc;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.intent.beens.PersonAndr;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.test.MessageReply;
import hu.rgai.android.test.R;
import hu.rgai.android.tools.ProfilePhotoProvider;
import hu.rgai.android.tools.Utils;
import hu.rgai.android.view.activities.EmailDisplayerActivity;
import hu.uszeged.inf.rgai.messagelog.beans.HtmlContent;

/**
 * This class responsible for displaying an email message.
 *
 * @author Tamas Kojedzinszky
 */
public class EmailDisplayerFragment extends Fragment {

  private FullSimpleMessageParc mContent = null;
  
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

//  public EmailDisplayerFragment() {
//  }

  public static final EmailDisplayerFragment newInstance() {
    EmailDisplayerFragment edf = new EmailDisplayerFragment();
    
    return edf;
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EmailDisplayerActivity eda = (EmailDisplayerActivity)getActivity();
    mAccount = eda.getAccount();
    mMessage = eda.getMessage();
  }
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.email_displayer, container, false);
   
    mWebView = (WebView) mView.findViewById(R.id.email_content);
    mWebView.getSettings().setDefaultTextEncodingName(mailCharCode);
    
    mFrom = (PersonAndr) mMessage.getFrom();
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
  
  
  /**
   * Displays the message.
   */
  private void displayMessage() {
    Bitmap img = ProfilePhotoProvider.getImageToUser(this.getActivity(), mFrom.getContactId()).getBitmap();
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
