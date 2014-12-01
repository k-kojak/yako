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
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import hu.rgai.yako.beens.*;
import hu.rgai.yako.broadcastreceivers.SimpleMessageSentBroadcastReceiver;
import hu.rgai.yako.handlers.TimeoutHandler;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.messageproviders.MessageProvider;
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

  private static final String MSG_CONTENT = "msg_content";
  private static final String MSG_RECIPIENTS = "msg_recipients";

  private FullSimpleMessage mContent = null;
  
  // instance which used to fetch email (if necessary)
  private Account mAccount;
  
  private MessageListElement mMessage;
  private ArrayList<Person> mRecipients = null;
  // the sender of the message
  private Person mFrom;
  
  private View mView;
  // a view for displaying content
  private WebView mWebView = null;
  private TextView mInfoText;
  private TextView mFoldedRecipients;
  private TextView mExpandedRecipients;
  private Map<Pair<Integer, Integer>, Person> mPersonPositions;

  private HorizontalScrollView mAnswersScrollView;
  private LinearLayout mQuickAnswerFooter;
  private boolean mQuickAnswerIsTranslated = false;
  // default character encoding of message
  private String mailCharCode = "UTF-8";

  public static EmailDisplayerFragment newInstance() {
    return new EmailDisplayerFragment();
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    
    mView = inflater.inflate(R.layout.email_displayer, container, false);
    mWebView = (WebView) mView.findViewById(R.id.email_content);
    mInfoText = (TextView) mView.findViewById(R.id.info_text);
    mFoldedRecipients = (TextView)mView.findViewById(R.id.recipients);
    mExpandedRecipients = (TextView)mView.findViewById(R.id.recipients_expanded);

    mWebView.getSettings().setDefaultTextEncodingName(mailCharCode);
    mWebView.getSettings().setBuiltInZoomControls(true);
    mWebView.getSettings().setDisplayZoomControls(false);

//    mWebView.getSettings().setLoadWithOverviewMode(true);
//    mWebView.getSettings().setUseWideViewPort(true);
//    mWebView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);

    EmailDisplayerActivity eda = (EmailDisplayerActivity)getActivity();
    mAccount = eda.getAccount();
    mMessage = eda.getMessage();
    mFrom = mMessage.getFrom();
    if (savedInstanceState != null) {
      mContent = savedInstanceState.getParcelable(MSG_CONTENT);
      mRecipients = savedInstanceState.getParcelableArrayList(MSG_RECIPIENTS);
    } else {
      mContent = (FullSimpleMessage) mMessage.getFullMessage();
      mRecipients = new ArrayList<>(eda.getRecipients());
    }
    displayMessage();


    WebViewClient mWebViewClient = new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith("mailto:")) {
          startEmailReplyActivity(mFrom);
//          Intent intent = new Intent(EmailDisplayerFragment.this.getActivity(), MessageReplyActivity.class);
//          intent.putExtra("instance", (Parcelable) mAccount);
//          intent.putExtra("from", (Parcelable) mFrom);
//          startActivityForResult(intent, MESSAGE_REPLY_REQ_CODE);
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

  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putParcelable(MSG_CONTENT, mContent);
    outState.putParcelableArrayList(MSG_RECIPIENTS, mRecipients);
    super.onSaveInstanceState(outState);
  }

  private void startEmailReplyActivity(Person to) {
    Intent intent = new Intent(getActivity(), MessageReplyActivity.class);
    intent.setAction(IntentStrings.Actions.DIRECT_EMAIL);
    intent.putExtra(IntentStrings.Params.PERSON, (Parcelable) to);
    Log.d("yako", "to -> " + to);
    startActivity(intent);
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

    List<MessageRecipient> recipients = new LinkedList<>();
    recipients.add(recipient);

    SentMessageData smd = MessageReplyActivity.getSentMessageDataToAccount(recipients, from);
    sentMessBroadcD.setMessageData(smd);



    MessageSender rs = new MessageSender(recipient.getType(), recipients, from, sentMessBroadcD,
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



    ((ImageView) mView.findViewById(R.id.avatar)).setImageBitmap(img);
    ((TextView)mView.findViewById(R.id.from_name)).setText(mFrom.getName());
    ((TextView)mView.findViewById(R.id.date)).setText(Utils.getPrettyTime(mContent.getDate()));
    ((TextView)mView.findViewById(R.id.from_email)).setText(mFrom.getId());
    ((TextView)mView.findViewById(R.id.from_email)).setText(mFrom.getId());

    setRecipientsFields();

    HtmlContent hc = mContent.getContent();
    String c = hc.getContent().toString();
    if (hc.getContentType().equals(HtmlContent.ContentType.TEXT_PLAIN)) {
      c = c.replaceAll("\n", "<br/>");
    }
    mWebView.loadDataWithBaseURL(null, c, "text/html", mailCharCode, null);
  }

  private void setRecipientsFields() {
    setFoldedRecipientsField();
    setExpandedRecipientsField();
  }

  private void setFoldedRecipientsField() {

    StringBuilder recipientsNames = new StringBuilder("to: ");

    String myEmail = mAccount.getAccountType().equals(MessageProvider.Type.GMAIL)
            ? ((GmailAccount)mAccount).getEmail() + "@gmail.com"
            : ((EmailAccount)mAccount).getEmail();

    int i = 0;
    for (Person person : mRecipients) {

      if (i > 0) {
        recipientsNames.append(", ");
      }
      if (person.getId().equals(myEmail)) {
        recipientsNames.append("me");
      } else {
        if (!person.getName().equals(person.getId())) {
          recipientsNames.append(person.getName().split(" ")[0]);
        } else {
          recipientsNames.append(person.getId().split("@")[0]);
        }
      }
      i++;
    }
    mFoldedRecipients.setText(recipientsNames);
    mFoldedRecipients.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        toggleRecipientsField(true);
      }
    });
  }

  private void setExpandedRecipientsField() {

    String recipientsNames = getExpandedRecipientsText(mRecipients);

    Spannable spannable = new SpannableString(recipientsNames);

    for (Pair<Integer, Integer> spanPos : mPersonPositions.keySet()) {
      spannable.setSpan(
              new ForegroundColorSpan(0xff35b4e3),
              spanPos.first,
              spanPos.second,
              Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    mExpandedRecipients.setText(spannable);
    setExpandedRecipientListener();
  }

  private void setExpandedRecipientListener() {
    mExpandedRecipients.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
          Layout layout = ((TextView) v).getLayout();
          int x = (int) event.getX();
          int y = (int) event.getY();
          if (layout != null) {
            int line = layout.getLineForVertical(y);
            int offset = layout.getOffsetForHorizontal(line, x);
            Person p = getPersonToClickPosition(offset);
            if (p == null) {
              toggleRecipientsField(false);
            } else {
              startEmailReplyActivity(p);
            }
          }
//          return true;
        }
        return false;
      }
    });
  }

  private String getExpandedRecipientsText(List<Person> recipients) {
    String recipientsNames = "to: ";

    mPersonPositions = new HashMap<>();

    int i = 0;
    for (Person person : recipients) {
      if (i > 0) {
        recipientsNames += ", ";
      }
      if (!person.getName().equals(person.getId())) {
        recipientsNames += person.getName() + " <";

        mPersonPositions.put(new Pair<>(recipientsNames.length(),
                recipientsNames.length() + person.getId().length()), person);

        recipientsNames += person.getId();
        recipientsNames += ">";
      } else {
        mPersonPositions.put(new Pair<>(recipientsNames.length(),
                recipientsNames.length() + person.getId().length()), person);
        recipientsNames += person.getId();
      }
      i++;
    }

    return recipientsNames;
  }

  private Person getPersonToClickPosition(int index) {
    for (Map.Entry<Pair<Integer, Integer>, Person> e : mPersonPositions.entrySet()) {
      if (e.getKey().first <= index && index <= e.getKey().second) {
        return e.getValue();
      }
    }
    return null;
  }

  private void toggleRecipientsField(boolean expand) {
    if (expand) {
      mFoldedRecipients.setVisibility(View.GONE);
      mExpandedRecipients.setVisibility(View.VISIBLE);
    } else {
      mFoldedRecipients.setVisibility(View.VISIBLE);
      mExpandedRecipients.setVisibility(View.GONE);
    }
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

      Map<String, String> postParams = new HashMap<>(2);
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
            answers = new ArrayList<>(data.length());
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
