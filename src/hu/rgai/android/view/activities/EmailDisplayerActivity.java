package hu.rgai.android.view.activities;

import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.FullSimpleMessage;
import hu.rgai.android.beens.MainServiceExtraParams.ParamStrings;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.test.YakoApp;
import hu.rgai.android.test.MessageReply;
import hu.rgai.android.test.R;
import hu.rgai.android.tools.AndroidUtils;
import hu.rgai.android.tools.view.NonSwipeableViewPager;
import hu.rgai.android.view.fragments.EmailAttachmentFragment;
import hu.rgai.android.view.fragments.EmailDisplayerFragment;
import hu.rgai.android.workers.MessageSeenMarkerAsyncTask;
import java.util.Arrays;
import java.util.TreeSet;

public class EmailDisplayerActivity extends ActionBarActivity {

  private MessageListElement mMessage = null;
  private boolean mFromNotification = false;
  private FullSimpleMessage mContent = null;
  private MyPageChangeListener mPageChangeListener = null;
  
  private NonSwipeableViewPager mPager;
  private PagerAdapter mPagerAdapter;
  public static final int MESSAGE_REPLY_REQ_CODE = 1;
  
  private YakoApp mYakoApp = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mYakoApp = (YakoApp)getApplication();
    
    Tracker t = ((YakoApp)getApplication()).getTracker();
    t.setScreenName(this.getClass().getName());
    t.send(new HitBuilders.AppViewBuilder().build());
    
    setContentView(R.layout.activity_email_displayer);

    mMessage = getIntent().getExtras().getParcelable("message");
    mContent = (FullSimpleMessage)mMessage.getFullMessage();
    
    mYakoApp.setMessageSeenAndReadLocally(mMessage);
    
    getSupportActionBar().setTitle(mContent.getSubject());

    
    // marking message as read
    MessageProvider provider = AndroidUtils.getMessageProviderInstanceByAccount(mMessage.getAccount(), this);
    MessageSeenMarkerAsyncTask marker = new MessageSeenMarkerAsyncTask(provider,
            new TreeSet<MessageListElement>(Arrays.asList(new MessageListElement[]{mMessage})),
            true, null);
    marker.executeTask(new Void[]{});

    
    // handling if we came from notifier
    if (getIntent().getExtras().containsKey(ParamStrings.FROM_NOTIFIER)
            && getIntent().getExtras().getBoolean(ParamStrings.FROM_NOTIFIER)) {
      mFromNotification = true;
    }

    
    // Instantiate a ViewPager and a PagerAdapter.
    mPager = (NonSwipeableViewPager) findViewById(R.id.pager);
    mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(),
            mContent.getAttachments() != null && !mContent.getAttachments().isEmpty());
    mPager.setAdapter(mPagerAdapter);
    mPageChangeListener = new MyPageChangeListener();
    mPager.setOnPageChangeListener(mPageChangeListener);
  }

  @Override
  protected void onPause() {
    super.onPause(); //To change body of generated methods, choose Tools | Templates.
    Tracker t = ((YakoApp)getApplication()).getTracker();
    t.setScreenName(this.getClass().getName() + " - pause");
    t.send(new HitBuilders.AppViewBuilder().build());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.email_message_options_menu, menu);

    if (mContent.getAttachments() == null || mContent.getAttachments().isEmpty()) {
      menu.findItem(R.id.attachments).setVisible(false);
    }
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.message_reply:
        Intent intent = new Intent(this, MessageReply.class);
//        Source source = new Source(mContent.getContent().getContent());
        intent.putExtra("message", (Parcelable)mMessage);
//        intent.putExtra("account", (Parcelable) mAccount);
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

  /**
   * A simple pager adapter that represents 5 {@link ScreenSlidePageFragment} objects, in
   * sequence.
   */
  
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
    EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.EMAIL.EMAIL_BACKBUTTON_STR, true);
    super.onBackPressed();
  }
  
}
