package hu.rgai.yako.view.fragments;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import hu.rgai.yako.beens.*;
import hu.rgai.yako.broadcastreceivers.SimpleMessageSentBroadcastReceiver;
import hu.rgai.yako.handlers.TimeoutHandler;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.smarttools.DummyQuickAnswerProvider;
import hu.rgai.yako.smarttools.QuickAnswerProvider;
import hu.rgai.yako.view.activities.MessageReplyActivity;
import hu.rgai.android.test.R;
import hu.rgai.yako.tools.ProfilePhotoProvider;
import hu.rgai.yako.tools.Utils;
import hu.rgai.yako.view.activities.EmailDisplayerActivity;
import hu.rgai.yako.workers.MessageSender;
import net.htmlparser.jericho.Source;

import java.util.LinkedList;
import java.util.List;

/**
 * This class responsible for displaying an email message.
 *
 * @author Tamas Kojedzinszky
 */
public class EmailDisplayerFragment extends Fragment {

  private FullSimpleMessage mContent = null;
  
  // instance which used to fetch email (if necessary)
  private Account mAccount;
  
  private MessageListElement mMessage;
  // the sender of the message
  private Person mFrom;
  
  private View mView;
  // a view for displaying content
  private WebView mWebView = null;
  private WebViewClient mWebViewClient = null;
  private HorizontalScrollView mAnswersScrollView;
  private LinearLayout mQuickAnswerFooter;
  private boolean mQuickAnswerIsTranslated = false;
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
    mWebView.getSettings().setBuiltInZoomControls(true);

    
    EmailDisplayerActivity eda = (EmailDisplayerActivity)getActivity();
    mAccount = eda.getAccount();
    mMessage = eda.getMessage();
    mFrom = mMessage.getFrom();
    mContent = (FullSimpleMessage) mMessage.getFullMessage();
    displayMessage();

    
    
    mWebViewClient = new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith("mailto:")) {
          Intent intent = new Intent(EmailDisplayerFragment.this.getActivity(), MessageReplyActivity.class);
          intent.putExtra("instance", (Parcelable) mAccount);
          intent.putExtra("from", (Parcelable)mFrom);
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

    loadQuickAnswers(inflater, container);

    return mView;
  }

  private void loadQuickAnswers(LayoutInflater inflater, ViewGroup container) {
    mQuickAnswerFooter = (LinearLayout) mView.findViewById(R.id.quick_answer_footer);
    mAnswersScrollView = (HorizontalScrollView)mView.findViewById(R.id.quick_answers);
    final TextView answersShowHide = (TextView)mView.findViewById(R.id.quick_answer_btn);
    answersShowHide.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        quickAnswerToggle();
      }
    });

    ViewGroup ansHolder = (ViewGroup)mAnswersScrollView.findViewById(R.id.quick_answer_inner);
    QuickAnswerProvider qap = new DummyQuickAnswerProvider();
    List<String> answers = qap.getQuickAnswers(mMessage);

    if (answers != null && !answers.isEmpty()) {
      for (final String s : answers) {
        TextView tv = (TextView) inflater.inflate(R.layout.quick_answer_item, container, false);
        tv.setText(s);
        tv.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            quickAnswerClicked(s);
          }
        });
        ansHolder.addView(tv);
      }

      new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
          quickAnswerToggle();
        }
      }, 1000);
    } else {
      mQuickAnswerFooter.setVisibility(View.GONE);
    }
  }

  private void quickAnswerToggle() {

    ObjectAnimator mover;

    if (!mQuickAnswerIsTranslated) {
      mover = ObjectAnimator.ofFloat(mQuickAnswerFooter, "translationY", 0, mAnswersScrollView.getHeight());
    } else {
      mover = ObjectAnimator.ofFloat(mQuickAnswerFooter, "translationY", mAnswersScrollView.getHeight(), 0);
    }
    mQuickAnswerIsTranslated = !mQuickAnswerIsTranslated;
    mover.setDuration(200);
    mover.start();
//    TranslateAnimation ta;
//    if (!mQuickAnswerIsTranslated) {
//      ta = new TranslateAnimation(0, 0, 0.0f, mAnswersScrollView.getHeight());
//      ta.setAnimationListener(new QuickAnswerAnimationListener(mAnswersScrollView.getHeight()));
//    } else {
//      ta = new TranslateAnimation(0, 0, 0.0f, -mAnswersScrollView.getHeight());
//      ta.setAnimationListener(new QuickAnswerAnimationListener(0));
//    }
//    mQuickAnswerIsTranslated = !mQuickAnswerIsTranslated;
//    ta.setDuration(300);
//    mQuickAnswerFooter.startAnimation(ta);
  }

  private void quickAnswerClicked(String answer) {
    Account from = mMessage.getAccount();
    Source source = new Source("<br /><br />" + mContent.getContent().getContent());
    String content = source.getRenderer().toString();

    MessageRecipient recipient = MessageRecipient.Helper.personToRecipient(mMessage.getFrom());
    SentMessageBroadcastDescriptor sentMessBroadcD = new SentMessageBroadcastDescriptor(
            SimpleMessageSentBroadcastReceiver.class, IntentStrings.Actions.MESSAGE_SENT_BROADCAST);

    SentMessageData smd = MessageReplyActivity.getSentMessageDataToAccount(recipient.getDisplayName(), from);
    sentMessBroadcD.setMessageData(smd);

    MessageSender rs = new MessageSender(recipient, from, sentMessBroadcD,
            new TimeoutHandler() {
              @Override
              public void onTimeout(Context context) {
                Toast.makeText(getActivity(), "Unable to send message...", Toast.LENGTH_SHORT).show();
              }
            },
            mMessage.getTitle(), answer + content, getActivity());
    rs.setTimeout(20000);
    rs.executeTask(getActivity(), null);
    getActivity().finish();
  }
  
  
  /**
   * Displays the message.
   */
  private void displayMessage() {
    Bitmap img = ProfilePhotoProvider.getImageToUser(this.getActivity(), mFrom).getBitmap();
    ((ImageView)mView.findViewById(R.id.avatar)).setImageBitmap(img);
    ((TextView)mView.findViewById(R.id.from_name)).setText(mFrom.getName());
    ((TextView)mView.findViewById(R.id.date)).setText(Utils.getPrettyTime(mContent.getDate()));
    ((TextView)mView.findViewById(R.id.from_email)).setText(mFrom.getId());;
    
    String recipientsNames = "to: ";
    LinkedList<Person> recipientsList = (LinkedList<Person>) mMessage.getRecipientsList();

    for(Person person : recipientsList) {
      recipientsNames += person.getName();

      if(!recipientsList.getLast().equals(person)) {
        recipientsNames += " ,"; 
      }
    }
    
    ((TextView)mView.findViewById(R.id.recipients)).setText(recipientsNames);

    
    HtmlContent hc = mContent.getContent();
    String c = hc.getContent().toString();
    if (hc.getContentType().equals(HtmlContent.ContentType.TEXT_PLAIN)) {
      c = c.replaceAll("\n", "<br/>");
    }
    mWebView.loadDataWithBaseURL(null, c, "text/html", mailCharCode, null);
  }

  private class QuickAnswerAnimationListener implements Animation.AnimationListener {

    private int mNewMargin;


    private QuickAnswerAnimationListener(int newMargin) {
      mNewMargin = newMargin;
    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {
      RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)mQuickAnswerFooter.getLayoutParams();
      params.bottomMargin = -mNewMargin;
      mQuickAnswerFooter.setLayoutParams(params);
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }
  }

}
