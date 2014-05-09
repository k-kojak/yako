/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hu.rgai.android.view.activities;

import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
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
import hu.rgai.android.beens.FullSimpleMessage;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.beens.Person;
import hu.rgai.android.beens.Account;
import hu.rgai.android.services.MainService;
import hu.rgai.android.test.AnalyticsApp;
import hu.rgai.android.test.MessageReply;
import hu.rgai.android.test.R;
import hu.rgai.android.tools.view.NonSwipeableViewPager;
import hu.rgai.android.view.fragments.EmailAttachmentFragment;
import hu.rgai.android.view.fragments.EmailDisplayerFragment;
import hu.rgai.android.workers.EmailMessageMarker;
import net.htmlparser.jericho.Source;

/**
 * Demonstrates a "screen-slide" animation using a {@link ViewPager}. Because
 * {@link ViewPager} automatically plays such an animation when calling
 * {@link ViewPager#setCurrentItem(int)}, there isn't any animation-specific code in this
 * sample.
 *
 * <p>This sample shows a "next" button that advances the user to the next step in a
 * wizard, animating the current screen out (to the left) and the next screen in (from the
 * right). The reverse animation is played when the user presses the "previous"
 * button.</p>
 *
 * @see ScreenSlidePageFragment
 */
public class EmailDisplayerActivity extends ActionBarActivity {

  /**
   * The number of pages (wizard steps) to show in this demo.
   */
  private MessageListElement mMessage = null;
  private Account mAccount = null;
  private boolean mFromNotification = false;
  private final String mSubject = null;
  private Person mFrom = null;
  private FullSimpleMessage mContent = null;
  private MyPageChangeListener mPageChangeListener = null;
  
  private NonSwipeableViewPager mPager;
  private PagerAdapter mPagerAdapter;
  public static final int MESSAGE_REPLY_REQ_CODE = 1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d("rgai", "EMAIL DISP. ACTIVITY: onCreate: " + getIntent().getExtras().getString("msg_list_element_id"));
    
    Tracker t = ((AnalyticsApp)getApplication()).getTracker();
    t.setScreenName(this.getClass().getName());
    t.send(new HitBuilders.AppViewBuilder().build());
    
    setContentView(R.layout.activity_email_displayer);

    mAccount = getIntent().getExtras().getParcelable("account");
    String mlepId = getIntent().getExtras().getString("msg_list_element_id");
    mMessage = MainService.getListElementById(mlepId, mAccount);
    mContent = (FullSimpleMessage)mMessage.getFullMessage();
    mFrom = mMessage.getFrom();
    MainService.setMessageSeenAndRead(mMessage);
    
    getSupportActionBar().setTitle(mContent.getSubject());

    EmailMessageMarker messageMarker = new EmailMessageMarker(mAccount);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      messageMarker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mlepId);
    } else {
      messageMarker.execute(mlepId);
    }

    if (getIntent().getExtras().containsKey("from_notifier") && getIntent().getExtras().getBoolean("from_notifier")) {
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
    Tracker t = ((AnalyticsApp)getApplication()).getTracker();
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
        intent.putExtra("account", (Parcelable) mAccount);
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

//            case R.id.action_next:
//                // Advance to the next step in the wizard. If there is no next step, setCurrentItem
//                // will do nothing.
//                mPager.setCurrentItem(mPager.getCurrentItem() + 1);
//                return true;
    }

    return super.onOptionsItemSelected(item);
  }

  public MessageListElement getMessage() {
    return mMessage;
  }

  public Account getAccount() {
    return mAccount;
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
