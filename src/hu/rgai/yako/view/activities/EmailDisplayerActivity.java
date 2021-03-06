package hu.rgai.yako.view.activities;

import android.app.ProgressDialog;
import android.content.*;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.view.*;
import hu.rgai.android.test.R;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.beens.*;
import hu.rgai.yako.config.ErrorCodes;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.handlers.MessageListerHandler;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.sql.MessageRecipientDAO;
import hu.rgai.yako.tools.AndroidUtils;
import hu.rgai.yako.view.extensions.NonSwipeableViewPager;
import hu.rgai.yako.view.extensions.ZoneDisplayActionBarActivity;
import hu.rgai.yako.view.fragments.EmailAttachmentFragment;
import hu.rgai.yako.view.fragments.EmailDisplayerFragment;
import hu.rgai.yako.workers.MessageSeenMarkerAsyncTask;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.QuickContactBadge;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import hu.rgai.yako.eventlogger.EventLogger.LogFilePaths;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.FullMessageDAO;
import hu.rgai.yako.sql.MessageListDAO;
import hu.rgai.yako.intents.IntentStrings;

import java.util.TreeMap;

public class EmailDisplayerActivity extends ZoneDisplayActionBarActivity {

  private Menu mMenu;
  private MessageListElement mMessage = null;
  private boolean mFromNotification = false;
  private FullSimpleMessage mContent = null;
  private List<Person> mRecipients = null;
  private MyPageChangeListener mPageChangeListener = null;
  private ProgressDialog pd = null;
  private CustomBroadcastReceiver mBcReceiver = null;

  private NonSwipeableViewPager mPager;
  private PagerAdapter mPagerAdapter;
  public static final int MESSAGE_REPLY_REQ_CODE = 1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState, true, false, true);

    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);

    Tracker t = ((YakoApp) getApplication()).getTracker();
    t.setScreenName(((Object)this).getClass().getName());
    t.send(new HitBuilders.AppViewBuilder().build());

    setContentView(R.layout.activity_email_displayer);

    long rawId = getIntent().getExtras().getLong(IntentStrings.Params.MESSAGE_RAW_ID);

    TreeMap<Long, Account> accounts = AccountDAO.getInstance(this).getIdToAccountsMap();
    mMessage = MessageListDAO.getInstance(this).getMessageByRawId(rawId, accounts);

    if (mMessage == null) {
      finish(ErrorCodes.MESSAGE_IS_NULL_ON_MESSAGE_OPEN);
      return;
    }

    mBcReceiver = new CustomBroadcastReceiver();

  }

  @Override
  protected void onResume() {
    super.onResume();

    toggleProgressDialog(true);

    IntentFilter bcFilter = new IntentFilter(MessageListerHandler.SPLITTED_PACK_LOADED_INTENT);
    LocalBroadcastManager.getInstance(this).registerReceiver(mBcReceiver, bcFilter);

    boolean hasContent = loadFullMessageContent();
    if (hasContent) {
      displayMessage();
    }/* else {
      // wait for asynctask to finish content load
    }*/
  }

  private boolean loadFullMessageContent() {
    TreeSet<FullSimpleMessage> fullMessages = FullMessageDAO.getInstance(this).getFullSimpleMessages(this, mMessage.getRawId());
    if (fullMessages != null && !fullMessages.isEmpty()) {
      mContent = fullMessages.first();
      mMessage.setFullMessage(mContent);
      mRecipients = MessageRecipientDAO.getInstance(this)
              .getRecipientsToMessageId(this, mMessage);
      return true;
    } else {
      return false;
    }
  }

  private void displayMessage() {

    setActionbarMenu();

    toggleProgressDialog(false);
    MessageListDAO.getInstance(this).updateMessageToSeen(mMessage.getRawId(), true);

    getSupportActionBar().setTitle(mContent.getSubject());

    // marking message as read
    MessageProvider provider = AndroidUtils.getMessageProviderInstanceByAccount(mMessage.getAccount(), this);
    TreeSet<String> messagesToMark = new TreeSet<>();
    messagesToMark.add(mMessage.getId());
    MessageSeenMarkerAsyncTask marker = new MessageSeenMarkerAsyncTask(provider, messagesToMark, true, null);
    marker.executeTask(this, null);

    // handling if we came from notifier
    if (getIntent().getExtras().containsKey(IntentStrings.Params.FROM_NOTIFIER)
            && getIntent().getExtras().getBoolean(IntentStrings.Params.FROM_NOTIFIER)) {
      mFromNotification = true;
    }

    // Instantiate a ViewPager and a PagerAdapter.
    mPager = (NonSwipeableViewPager) findViewById(R.id.pager);
    mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(),
            mContent.getAttachments() != null
                    && !mContent.getAttachments().isEmpty());
    mPager.setAdapter(mPagerAdapter);
    mPageChangeListener = new MyPageChangeListener();
    mPager.setOnPageChangeListener(mPageChangeListener);
  }

  private void setActionbarMenu() {
    if (mMenu == null || mMessage == null || mContent == null) {
      return;
    }

    mMenu.findItem(R.id.message_reply).setVisible(true);

    if (mContent.getAttachments() != null && !mContent.getAttachments().isEmpty()) {
      mMenu.findItem(R.id.attachments).setVisible(true);
    }

    Person contactPerson = Person.searchPersonAndr(this, mMessage.getFrom());
    if (contactPerson.getContactId() == -1) {
      mMenu.findItem(R.id.add_email_contact).setVisible(true);
    }

    if (mRecipients.size() > 1) {
      mMenu.findItem(R.id.message_reply_to_all).setVisible(true);
    }

  }

  public synchronized void toggleProgressDialog(boolean show) {
    if (show) {
      if (pd == null) {
        pd = new ProgressDialog(this);
        pd.setCancelable(false);
      }
      pd.show();
      pd.setContentView(R.layout.progress_dialog);
    } else {
      if (pd != null) {
        pd.dismiss();
      }
    }
  }

  private void finish(int code) {
    ((YakoApp) getApplication()).sendAnalyticsError(code);
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        finish();
      }
    });
    builder.setTitle(getString(R.string.error));
    builder.setMessage(
        String.format(getString(R.string.connection_error_pls_try_later_xcode), code)).show();
  }

  @Override
  protected void onPause() {
    super.onPause();
    Tracker t = ((YakoApp) getApplication()).getTracker();
    t.setScreenName(((Object)this).getClass().getName() + " - pause");
    t.send(new HitBuilders.AppViewBuilder().build());

    LocalBroadcastManager.getInstance(this).unregisterReceiver(mBcReceiver);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    mMenu = menu;
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.email_message_options_menu, mMenu);
    setActionbarMenu();
    return true;
  }


  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent;
    switch (item.getItemId()) {
      case R.id.message_reply:
        intent = new Intent(this, MessageReplyActivity.class);
        intent.putExtra(IntentStrings.Params.MESSAGE_RAW_ID, mMessage.getRawId());
        startActivityForResult(intent, MESSAGE_REPLY_REQ_CODE);
        return true;
      case R.id.message_reply_to_all:
        intent = new Intent(this, MessageReplyActivity.class);
        intent.putExtra(IntentStrings.Params.MESSAGE_RAW_ID, mMessage.getRawId());
        intent.putExtra(IntentStrings.Params.MESSAGE_REPLY_TO_ALL, true);
        startActivityForResult(intent, MESSAGE_REPLY_REQ_CODE);
        return true;
      case android.R.id.home:
        // Navigate "up" the demo structure to the launchpad activity.
        // See http://developer.android.com/design/patterns/navigation.html for more.
        if (mFromNotification) {
          Intent upIntent = NavUtils.getParentActivityIntent(this);
          TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities();
        } else {
          finish();
        }
        return true;

      case R.id.add_email_contact:
        ArrayList<String> contactDatas = new ArrayList<>();
        contactDatas.add(mMessage.getFrom().getId());
        QuickContactBadge badgeSmall = AndroidUtils.addToContact(mMessage.getMessageType(), this, contactDatas);
        badgeSmall.onClick(item.getActionView());
        return true;

      case R.id.attachments:
        if (mPageChangeListener.getSelectedPosition() == 1) {
          mPager.setCurrentItem(0);
        } else {
          mPager.setCurrentItem(1);
        }
        return true;

    }

    return super.onOptionsItemSelected(item);
  }

  public MessageListElement getMessage() {
    return mMessage;
  }

  public Account getAccount() {
    return mMessage.getAccount();
  }

  public FullSimpleMessage getContent() {
    return mContent;
  }

  public List<Person> getRecipients() {
    return mRecipients;
  }

  private class MyPageChangeListener extends ViewPager.SimpleOnPageChangeListener {

    int mPosition;

    @Override
    public void onPageSelected(int position) {
      mPosition = position;
    }

    public int getSelectedPosition() {
      return mPosition;
    }
  }

  private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

    private boolean mIsAttachmentPresent = false;

    public ScreenSlidePagerAdapter(FragmentManager fm, boolean attachmentPresent) {
      super(fm);
      this.mIsAttachmentPresent = attachmentPresent;
    }

    @Override
    public Fragment getItem(int position) {
      if (position == 0) {
        EmailDisplayerFragment edf = EmailDisplayerFragment.newInstance();
        return edf;
      } else {
        EmailAttachmentFragment eaf = EmailAttachmentFragment.newInstance();
        return eaf;
      }
    }

    @Override
    public int getCount() {
      return (mIsAttachmentPresent ? 2 : 1);
    }
  }

  @Override
  public void onBackPressed() {
    Log.d("willrgai", EventLogger.LOGGER_STRINGS.EMAIL.EMAIL_BACKBUTTON_STR);
    EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, EventLogger.LOGGER_STRINGS.EMAIL.EMAIL_BACKBUTTON_STR, true);
    super.onBackPressed();
  }

  private class CustomBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction() != null) {
        if (intent.getAction().equals(MessageListerHandler.SPLITTED_PACK_LOADED_INTENT)) {
          boolean hasContent = loadFullMessageContent();
          if (hasContent) {
            displayMessage();
          }
        }
      }
    }
  }

}
