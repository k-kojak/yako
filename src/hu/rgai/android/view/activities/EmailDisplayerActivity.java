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
import android.support.v4.app.FragmentActivity;
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
import hu.rgai.android.asynctasks.EmailMessageMarker;
import hu.rgai.android.intent.beens.FullSimpleMessageParc;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.intent.beens.PersonAndr;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.services.MainService;
import static hu.rgai.android.test.EmailDisplayer.MESSAGE_REPLY_REQ_CODE;
import hu.rgai.android.test.MainActivity;
import hu.rgai.android.test.MessageReply;
import hu.rgai.android.test.R;
import hu.rgai.android.view.fragments.EmailDisplayerFragment;
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
  private static final int NUM_PAGES = 5;
  private MessageListElementParc mMessage = null;
  private AccountAndr mAccount = null;
  private boolean mFromNotification = false;
  private String mSubject = null;
  private PersonAndr mFrom = null;
  private FullSimpleMessageParc mContent = null;
  private MyPageChangeListener mPageChangeListener = null;
  
  /**
   * The pager widget, which handles animation and allows swiping horizontally to access
   * previous and next wizard steps.
   */
  private ViewPager mPager;
  /**
   * The pager adapter, which provides the pages to the view pager widget.
   */
  private PagerAdapter mPagerAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_email_displayer);

    mAccount = getIntent().getExtras().getParcelable("account");
    String mlepId = getIntent().getExtras().getString("msg_list_element_id");
    mMessage = MainService.getListElementById(mlepId, mAccount);
    mContent = (FullSimpleMessageParc)mMessage.getFullMessage();
    MainService.setMessageSeenAndRead(mMessage);

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
    mPager = (ViewPager) findViewById(R.id.pager);
    mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(),
            mContent.getAttachments() != null && !mContent.getAttachments().isEmpty());
    mPager.setAdapter(mPagerAdapter);
    mPageChangeListener = new MyPageChangeListener();
    mPager.setOnPageChangeListener(mPageChangeListener);
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
        Source source = new Source(mContent.getContent().getContent());
        intent.putExtra("content", source.getRenderer().toString());
        intent.putExtra("subject", mSubject);
        intent.putExtra("account", (Parcelable) mAccount);
        intent.putExtra("from", mFrom);
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
    private boolean mIsAttachmentVisible = false;

    public ScreenSlidePagerAdapter(FragmentManager fm, boolean attachmentPresent) {
      super(fm);
      this.mIsAttachmentPresent = attachmentPresent;
    }

    @Override
    public Fragment getItem(int position) {
      Log.d("rgai", "getItem: " + position);
      if (position == 0) {
        EmailDisplayerFragment edf = new EmailDisplayerFragment(mAccount, mMessage);
        return edf;
      } else {
        EmailDisplayerFragment edf = new EmailDisplayerFragment(mAccount, mMessage);
        return edf;
//            return ScreenSlidePageFragment.create(position);
      }

    }

    @Override
    public int getCount() {
      return (mIsAttachmentPresent ? 2 : 1);
    }
  }
}
