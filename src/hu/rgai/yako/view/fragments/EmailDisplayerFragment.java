package hu.rgai.yako.view.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.FullSimpleMessage;
import hu.rgai.yako.beens.HtmlContent;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.beens.Person;
import hu.rgai.yako.view.activities.MessageReplyActivity;
import hu.rgai.android.test.R;
import hu.rgai.yako.tools.ProfilePhotoProvider;
import hu.rgai.yako.tools.Utils;
import hu.rgai.yako.view.activities.EmailDisplayerActivity;

/**
 * This class responsible for displaying an email message.
 *
 * @author Tamas Kojedzinszky
 */
public class EmailDisplayerFragment extends Fragment {

  private FullSimpleMessage mContent = null;
  
  // account which used to fetch email (if necessary)
  private Account mAccount;
  
  private MessageListElement mMessage;
  // the sender of the message
  private Person mFrom;
  
  private View mView;
  // a view for displaying content
  private WebView mWebView = null;
  private WebViewClient mWebViewClient = null;
  // default character encoding of message
  private String mailCharCode = "UTF-8";
  public static final int MESSAGE_REPLY_REQ_CODE = 1;

  public static final EmailDisplayerFragment newInstance() {
    EmailDisplayerFragment edf = new EmailDisplayerFragment();
    
    return edf;
  }
  

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    
    mView = inflater.inflate(R.layout.email_displayer, container, false);
    mWebView = (WebView) mView.findViewById(R.id.email_content);
    mWebView.getSettings().setDefaultTextEncodingName(mailCharCode);
    
    
    EmailDisplayerActivity eda = (EmailDisplayerActivity)getActivity();
    mAccount = eda.getAccount();
    mMessage = eda.getMessage();
    mFrom = (Person) mMessage.getFrom();
    mContent = (FullSimpleMessage) mMessage.getFullMessage();
    displayMessage();
    
    
    
    mWebViewClient = new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith("mailto:")) {
          Intent intent = new Intent(EmailDisplayerFragment.this.getActivity(), MessageReplyActivity.class);
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
