package hu.rgai.yako.view.fragments;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import hu.rgai.yako.beens.*;
import hu.rgai.yako.broadcastreceivers.SimpleMessageSentBroadcastReceiver;
import hu.rgai.yako.handlers.TimeoutHandler;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.tools.RemoteMessageController;
import hu.rgai.yako.view.activities.MessageReplyActivity;
import hu.rgai.android.test.R;
import hu.rgai.yako.tools.ProfilePhotoProvider;
import hu.rgai.yako.tools.Utils;
import hu.rgai.yako.view.activities.EmailDisplayerActivity;
import hu.rgai.yako.workers.MessageSender;
import hu.rgai.yako.workers.TimeoutAsyncTask;
import net.htmlparser.jericho.Source;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

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
  private TextView mAnswersShowHide;
  private TextView mInfoText;
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
    mAnswersShowHide = (TextView) mView.findViewById(R.id.quick_answer_btn);
    mInfoText = (TextView) mView.findViewById(R.id.info_text);
    mWebView.getSettings().setDefaultTextEncodingName(mailCharCode);
    mWebView.getSettings().setBuiltInZoomControls(true);

    
    EmailDisplayerActivity eda = (EmailDisplayerActivity)getActivity();
    mAccount = eda.getAccount();
    mMessage = eda.getMessage();
    mFrom = mMessage.getFrom();
    mContent = (FullSimpleMessage) mMessage.getFullMessage();
    displayMessage();


    WebViewClient mWebViewClient = new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith("mailto:")) {
          Intent intent = new Intent(EmailDisplayerFragment.this.getActivity(), MessageReplyActivity.class);
          intent.putExtra("instance", (Parcelable) mAccount);
          intent.putExtra("from", (Parcelable) mFrom);
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

    QuickAnswerLoader qal = new QuickAnswerLoader(inflater, container, mContent.getContent().getContent().toString());
    qal.setTimeout(5000);
    qal.executeTask(getActivity(), new Void[]{});

    return mView;
  }

  private void loadQuickAnswers(LayoutInflater inflater, ViewGroup container, List<String> answers, boolean timeout) {
    mQuickAnswerFooter = (LinearLayout) mView.findViewById(R.id.quick_answer_footer);
    final TextView answersShowHide = (TextView) mView.findViewById(R.id.quick_answer_btn);
    mInfoText = (TextView) mView.findViewById(R.id.info_text);
    if (timeout) {
      showTextOnQuickAnswerAndHide(R.string.service_not_available);
    } else {
      if (answers == null || answers.isEmpty()) {
        showTextOnQuickAnswerAndHide(R.string.no_qa_available);
      } else {
        mInfoText.setVisibility(View.GONE);
        mAnswersScrollView = (HorizontalScrollView) mView.findViewById(R.id.quick_answers);
        answersShowHide.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            quickAnswerToggle();
          }
        });

        ViewGroup ansHolder = (ViewGroup) mAnswersScrollView.findViewById(R.id.quick_answer_inner);
        mQuickAnswerFooter.setVisibility(View.VISIBLE);
        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, r.getDisplayMetrics());
        int i = 0;
        for (final String s : answers) {
          if (i > 0) {
            LinearLayout v = new LinearLayout(getActivity());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) (px * 1), ViewGroup.LayoutParams.MATCH_PARENT);
            params.topMargin = (int) (8 * px);
            params.bottomMargin = (int) (8 * px);
            v.setBackgroundColor(0xff393939);
            v.setLayoutParams(params);
            ansHolder.addView(v);
          }

          TextView tv = (TextView) inflater.inflate(R.layout.quick_answer_item, container, false);
          tv.setText(s);
          tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              quickAnswerClicked(s);
            }
          });
          ansHolder.addView(tv);

          i++;
        }

      }
    }
  }

  private void showTextOnQuickAnswerAndHide(int resId) {
    mInfoText.setText(resId);
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        ObjectAnimator mover = ObjectAnimator.ofFloat(mQuickAnswerFooter, "translationY", 0, mQuickAnswerFooter.getHeight());
        mover.setDuration(250);
        mover.start();
      }
    }, 3000);
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
  }

  private void quickAnswerClicked(String answer) {
    Account from = mMessage.getAccount();
//    Source source = new Source("<br /><br /><hr />" + mContent.getContent().getContent());
    String content = MessageReplyActivity.getDesignedQuotedText(mContent.getContent().getContent().toString());

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
    ((TextView)mView.findViewById(R.id.from_email)).setText(mFrom.getId());
    
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

  private class QuickAnswerTimeoutHandler extends TimeoutHandler {

    @Override
    public void onTimeout(Context context) {
      loadQuickAnswers(null, null, null, true);
    }
  }

  private class QuickAnswerLoader extends TimeoutAsyncTask<Void, Void, String> {

    private static final String requestMod = "yako_quick_answer";
    private final LayoutInflater mInflater;
    private final ViewGroup mContainer;
    private final String mText;


    public QuickAnswerLoader(LayoutInflater inflater, ViewGroup container, String text) {
      super(new QuickAnswerTimeoutHandler());
      mInflater = inflater;
      mContainer = container;
      mText = text;
    }

    @Override
    protected String doInBackground(Void... params) {
      String result = null;
      Source source = new Source(mText);
      String plainText = source.getRenderer().toString();

      Map<String, String> postParams = new HashMap<String, String>(2);
      postParams.put("mod", requestMod);
      postParams.put("text", plainText);
      HttpResponse response = RemoteMessageController.sendPostRequest(postParams);
      if (response != null) {
        result = RemoteMessageController.responseToString(response);
      }
      return result;
    }

    @Override
    protected void onPostExecute(String result) {
      if (result == null) {
        loadQuickAnswers(mInflater, mContainer, null, false);
      } else {
        try {
          JSONObject root = new JSONObject(result);
          JSONArray data = root.getJSONArray("data");
          List<String> answers = null;
          if (data != null && data.length() != 0) {
            answers = new ArrayList<String>(data.length());
            for (int i = 0; i < data.length(); i++) {
              answers.add(data.get(i).toString());
            }
          }
          loadQuickAnswers(mInflater, mContainer, answers, false);
        } catch (JSONException e) {
          loadQuickAnswers(mInflater, mContainer, null, false);
          e.printStackTrace();
        }
      }
    }
  }

}
