package hu.rgai.yako.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import hu.rgai.android.test.MainActivity;
import hu.rgai.android.test.R;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.adapters.MainListAdapter;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.BatchedProcessState;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.handlers.BatchedAsyncTaskHandler;
import hu.rgai.yako.handlers.MessageSeenMarkerHandler;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.services.MainService;
import hu.rgai.yako.tools.AndroidUtils;
import hu.rgai.yako.tools.IntentParamStrings;
import hu.rgai.yako.workers.BatchedAsyncTaskExecutor;
import hu.rgai.yako.workers.BatchedTimeoutAsyncTask;
import hu.rgai.yako.workers.MessageSeenMarkerAsyncTask;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Tamas Kojedzinszky
 */
public class MainActivityFragment extends Fragment {

  private ListView lv;
  private View mRootView = null;
  private MainListAdapter mAdapter = null;
  private TreeSet<MessageListElement> contextSelectedElements = null;
  private MainActivity mMainActivity = null;
  private Button loadMoreButton = null;
  private boolean loadMoreButtonVisible = false;
  private ProgressBar mTopProgressBar;

  public static MainActivityFragment getInstance() {
    
    return new MainActivityFragment();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("rgai", "fragment: oncreate!");
    contextSelectedElements = new TreeSet<MessageListElement>();
    
    
    mRootView = inflater.inflate(R.layout.main, container, false);
    mMainActivity = (MainActivity)getActivity();
    
    mTopProgressBar = (ProgressBar) mRootView.findViewById(R.id.progressbar);
    lv = (ListView) mRootView.findViewById(R.id.list);
    
    lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    lv.setMultiChoiceModeListener(new MultiChoiceModeListener() {
      public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {
        if (position != 0) {
          if (checked) {
            contextSelectedElements.add((MessageListElement)mAdapter.getItem(position));
          } else {
            contextSelectedElements.remove((MessageListElement)mAdapter.getItem(position));
          }
          if (contextSelectedElements.size() == 1) {
            mode.getMenu().findItem(R.id.reply).setVisible(true);
          } else {
            mode.getMenu().findItem(R.id.reply).setVisible(false);
          }
          mode.setTitle(contextSelectedElements.size() + " selected");
        }
      }

      public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.main_list_context_menu, menu);
        contextSelectedElements.clear();
        return true;
      }

      public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
      }

      public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
          case R.id.reply:
            if (contextSelectedElements.size() == 1) {
              MessageListElement message = contextSelectedElements.first();
              Class classToLoad = Settings.getAccountTypeToMessageReplyer().get(message.getAccount().getAccountType());
              Intent intent = new Intent(mMainActivity, classToLoad);
              intent.putExtra(IntentParamStrings.MESSAGE_ID, message.getId());
              intent.putExtra(IntentParamStrings.MESSAGE_ACCOUNT, (Parcelable) message.getAccount());
              mMainActivity.startActivity(intent);
            }
            mode.finish();
            return true;
          case R.id.mark_seen:
            contextActionMarkMessage(true);
            mode.finish();
            return true;
          case R.id.mark_unseen:
            contextActionMarkMessage(false);
            mode.finish();
            return true;
          default:
            return false;
        }
      }

      public void onDestroyActionMode(ActionMode mode) {
        // Here you can make any necessary updates to the activity when
        // the CAB is removed. By default, selected items are deselected/unchecked.
      }
    });

    loadMoreButton = new Button(mMainActivity);
    loadMoreButton.setVisibility(View.GONE);
    loadMoreButton.setText("Load more ...");
    loadMoreButton.getBackground().setAlpha(0);
    loadMoreButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View arg0) {
        EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.CLICK.CLICK_LOAD_MORE_BTN, true);
        mMainActivity.loadMoreMessage();
      }
    });
    
    lv.addFooterView(loadMoreButton);
    
    if (BatchedAsyncTaskExecutor.isProgressRunning(MainService.MESSAGE_LIST_QUERY_KEY)) {
      loadMoreButton.setEnabled(false);
    }

    mAdapter = new MainListAdapter(mMainActivity);
    lv.setAdapter(mAdapter);
    lv.setOnScrollListener(new LogOnScrollListener(lv, mAdapter));
    lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> av, View arg1, int itemIndex, long arg3) {
        if (av.getItemAtPosition(itemIndex) == null) return;
        MessageListElement message = (MessageListElement) av.getItemAtPosition(itemIndex);
        Account a = message.getAccount();
        Class classToLoad = Settings.getAccountTypeToMessageDisplayer().get(a.getAccountType());
        Intent intent = new Intent(mMainActivity, classToLoad);
        intent.putExtra(IntentParamStrings.MESSAGE_ID, message.getId());
        intent.putExtra(IntentParamStrings.MESSAGE_ACCOUNT, (Parcelable) message.getAccount());
        boolean changed = YakoApp.setMessageSeenAndReadLocally(message);
        if (changed) {
          message.setSeen(true);
          message.setUnreadCount(0);
          mAdapter.notifyDataSetChanged();
        }

        loggingOnClickEvent(message, changed);
        mMainActivity.startActivityForResult(intent, Settings.ActivityRequestCodes.FULL_MESSAGE_RESULT);
      }

      /**
       * Performs a log event when an item clicked on the main view list.
       */
      private void loggingOnClickEvent(MessageListElement message, boolean changed) {
        StringBuilder builder = new StringBuilder();
        appendClickedElementDatasToBuilder(message, builder);
        mMainActivity.appendVisibleElementToStringBuilder(builder, lv, mAdapter);
        builder.append(changed);
        EventLogger.INSTANCE.writeToLogFile(builder.toString(), true);
      }

      private void appendClickedElementDatasToBuilder(MessageListElement message, StringBuilder builder) {
        builder.append(EventLogger.LOGGER_STRINGS.MAINPAGE.STR);
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.CLICK_TO_MESSAGEGROUP_STR);
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
        builder.append(message.getId());
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      }
    });
    return mRootView;
  }
    
    
  @Override
  public void onResume() {
    super.onResume(); //To change body of generated methods, choose Tools | Templates.
  }

  private void contextActionMarkMessage(boolean seen) {
    
    HashMap<Account, TreeSet<MessageListElement>> messagesToAccounts = new HashMap<Account, TreeSet<MessageListElement>>();
    for (MessageListElement mle : contextSelectedElements) {
      if (!messagesToAccounts.containsKey(mle.getAccount())) {
        messagesToAccounts.put(mle.getAccount(), new TreeSet<MessageListElement>());
      }
      messagesToAccounts.get(mle.getAccount()).add(mle);
    }
    
    // TODO: block auto update while marking messages
    
    MessageSeenMarkerHandler handler = new MessageSeenMarkerHandler(this);
    List<BatchedTimeoutAsyncTask> tasks = new LinkedList<BatchedTimeoutAsyncTask>();
    for (Map.Entry<Account, TreeSet<MessageListElement>> entry : messagesToAccounts.entrySet()) {
      MessageProvider mp = AndroidUtils.getMessageProviderInstanceByAccount(entry.getKey(), getActivity());
      MessageSeenMarkerAsyncTask messageMarker = new MessageSeenMarkerAsyncTask(mp, entry.getValue(), seen, handler);
      messageMarker.setTimeout(10000);
      tasks.add(messageMarker);
    }
    mTopProgressBar.setVisibility(View.VISIBLE);
    try {
      BatchedAsyncTaskExecutor batchedMarker = new BatchedAsyncTaskExecutor(tasks, MainActivity.BATCHED_MESSAGE_MARKER_KEY, new BatchedAsyncTaskHandler() {
        public void batchedTaskDone(boolean cancelled, String progressKey, BatchedProcessState processState) {
          if (processState.isDone()) {
            mTopProgressBar.setVisibility(View.GONE);
          }
        }
      });
      batchedMarker.execute();
    } catch (Exception ex) {
      Logger.getLogger(MainActivityFragment_F.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  
  private View getContent() {
    return null;
//        setContentView(R.layout.main);
//        lv = (ListView) findViewById(R.id.list);
//        mTopProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        
//        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
//        lv.setMultiChoiceModeListener(new MultiChoiceModeListener() {
//
//          public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {
//            if (position != 0) {
//              if (checked) {
//                contextSelectedElements.add((MessageListElement)adapter.getItem(position));
//              } else {
//                contextSelectedElements.remove((MessageListElement)adapter.getItem(position));
//              }
//              if (contextSelectedElements.size() == 1) {
//                mode.getMenu().findItem(R.id.reply).setVisible(true);
//              } else {
//                mode.getMenu().findItem(R.id.reply).setVisible(false);
//              }
//              mode.setTitle(contextSelectedElements.size() + " selected");
//            }
//          }
//
//          public boolean onCreateActionMode(ActionMode mode, Menu menu) {
//            MenuInflater inflater = mode.getMenuInflater();
//            inflater.inflate(R.menu.main_list_context_menu, menu);
//            contextSelectedElements.clear();
//            return true;
//          }
//
//          public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
//            return false;
//          }
//
//          public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
//            switch (item.getItemId()) {
//              case R.id.reply:
//                if (contextSelectedElements.size() == 1) {
//                  MessageListElement message = contextSelectedElements.first();
//                  Class classToLoad = Settings.getAccountTypeToMessageReplyer().get(message.getAccount().getAccountType());
////                  Intent intent = new Intent(MainActivityFragment_F.this, classToLoad);
////                  intent.putExtra(IntentParamStrings.MESSAGE_ID, message.getId());
////                  intent.putExtra(IntentParamStrings.MESSAGE_ACCOUNT, (Parcelable) message.getAccount());
////                  MainActivityFragment_F.this.startActivity(intent);
//                }
//                mode.finish();
//                return true;
//              case R.id.mark_seen:
//                contextActionMarkMessage(true);
//                mode.finish();
//                return true;
//              case R.id.mark_unseen:
//                contextActionMarkMessage(false);
//                mode.finish();
//                return true;
//              default:
//                return false;
//            }
//          }
//
//          public void onDestroyActionMode(ActionMode mode) {
//            // Here you can make any necessary updates to the activity when
//            // the CAB is removed. By default, selected items are deselected/unchecked.
//          }
//        });
//
////        loadMoreButton = new Button(this);
//        loadMoreButton.setText("Load more ...");
//        loadMoreButton.getBackground().setAlpha(0);
//        loadMoreButton.setOnClickListener(new View.OnClickListener() {
//          @Override
//          public void onClick(View arg0) {
//            EventLogger.INSTANCE.writeToLogFile(EventLogger.LOGGER_STRINGS.CLICK.CLICK_LOAD_MORE_BTN, true);
//            loadMoreMessage();
//          }
//        });
//        lv.addFooterView(loadMoreButton);
//        if (BatchedAsyncTaskExecutor.isProgressRunning(MainService.MESSAGE_LIST_QUERY_KEY)) {
//          loadMoreButton.setEnabled(false);
//        }
//
////        adapter = new MainListAdapter(this);
////        adapter.setListFilter(actSelectedFilter);
//        lv.setAdapter(adapter);
//        lv.setOnScrollListener(new LogOnScrollListener(lv, adapter));
//        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//          @Override
//          public void onItemClick(AdapterView<?> av, View arg1, int itemIndex, long arg3) {
//            if (av.getItemAtPosition(itemIndex) == null) return;
//            MessageListElement message = (MessageListElement) av.getItemAtPosition(itemIndex);
//            Account a = message.getAccount();
//            Class classToLoad = Settings.getAccountTypeToMessageDisplayer().get(a.getAccountType());
////            Intent intent = new Intent(MainActivityFragment_F.this, classToLoad);
////            intent.putExtra(IntentParamStrings.MESSAGE_ID, message.getId());
////            intent.putExtra(IntentParamStrings.MESSAGE_ACCOUNT, (Parcelable) message.getAccount());
//
//            boolean changed = YakoApp.setMessageSeenAndReadLocally(message);
//            if (changed) {
//              message.setSeen(true);
//              message.setUnreadCount(0);
//              adapter.notifyDataSetChanged();
//            }
//
//            loggingOnClickEvent(message, changed);
////            MainActivityFragment_F.this.startActivityForResult(intent, Settings.ActivityRequestCodes.FULL_MESSAGE_RESULT);
//          }
//
//          /**
//           * Performs a log event when an item clicked on the main view list.
//           */
//          private void loggingOnClickEvent(MessageListElement message, boolean changed) {
//            StringBuilder builder = new StringBuilder();
//            appendClickedElementDatasToBuilder(message, builder);
//            MainActivityFragment_F.this.appendVisibleElementToStringBuilder(builder, lv, adapter);
//            builder.append(changed);
//            EventLogger.INSTANCE.writeToLogFile(builder.toString(), true);
//          }
//
//          private void appendClickedElementDatasToBuilder(MessageListElement message, StringBuilder builder) {
//            builder.append(EventLogger.LOGGER_STRINGS.MAINPAGE.STR);
//            builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
//            builder.append(EventLogger.LOGGER_STRINGS.OTHER.CLICK_TO_MESSAGEGROUP_STR);
//            builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
//            builder.append(message.getId());
//            builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
//          }
//        });
//      } else if (YakoApp.getMessages().isEmpty()) {
////        TextView text = new TextView(this);
////        text.setText(this.getString(R.string.empty_list));
////        text.setGravity(Gravity.CENTER);
////        this.setContentView(text);
//      }
//    } else {
////      TextView text = new TextView(this);
////      text.setText(this.getString(R.string.no_internet_access));
////      text.setGravity(Gravity.CENTER);
////      setContentView(text);
//    }
  }
  

  private class LogOnScrollListener implements AbsListView.OnScrollListener {
    final ListView lv;

    final MainListAdapter adapter;

    public LogOnScrollListener(ListView lv, MainListAdapter adapter) {
      this.lv = lv;
      this.adapter = adapter;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
      // TODO Auto-generated method stub
      StringBuilder builder = new StringBuilder();

      builder.append(EventLogger.LOGGER_STRINGS.MAINPAGE.STR);
      builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      if (scrollState == 1) {
        builder.append(EventLogger.LOGGER_STRINGS.SCROLL.START_STR);
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      } else {
        builder.append(EventLogger.LOGGER_STRINGS.SCROLL.END_STR);
        builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
      }
      mMainActivity.appendVisibleElementToStringBuilder(builder, lv, adapter);
      EventLogger.INSTANCE.writeToLogFile(builder.toString(), true);
    }

  }
  
  
  public void loadStateChanged(boolean refreshing) {
    if (loadMoreButton != null) {
      if (refreshing) {
        loadMoreButton.setEnabled(false);
      } else {
        loadMoreButton.setEnabled(true);
      }
    }
  }
  
  public void notifyAdapterChange() {
    Log.d("rgai", "adapter count: " + lv.getFooterViewsCount());
    mAdapter.notifyDataSetChanged();
    if (!mAdapter.isEmpty() && !loadMoreButtonVisible) {
      loadMoreButton.setVisibility(View.VISIBLE);
      Log.d("rgai", "adding footer view...");
      loadMoreButtonVisible = true;
    }
  }

}
