package hu.rgai.yako.view.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.widget.AbsListView.MultiChoiceModeListener;
import hu.rgai.android.test.MainActivity;
import hu.rgai.android.test.R;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.adapters.MainListAdapter;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.BatchedProcessState;
import hu.rgai.yako.beens.GpsZone;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.eventlogger.EventLogger.LogFilePaths;
import hu.rgai.yako.handlers.BatchedAsyncTaskHandler;
import hu.rgai.yako.handlers.MessageDeleteHandler;
import hu.rgai.yako.handlers.MessageSeenMarkerHandler;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.services.MainService;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.MessageListDAO;
import hu.rgai.yako.store.StoreHandler;
import hu.rgai.yako.tools.AndroidUtils;
import hu.rgai.yako.workers.BatchedAsyncTaskExecutor;
import hu.rgai.yako.workers.BatchedTimeoutAsyncTask;
import hu.rgai.yako.workers.MessageDeletionAsyncTask;
import hu.rgai.yako.workers.MessageSeenMarkerAsyncTask;

import java.util.*;

/**
 * @author Tamas Kojedzinszky
 */
public class MainActivityFragment extends Fragment {

  private ListView mListView;
  private MainListAdapter mAdapter = null;
  private TreeSet<Long> contextSelectedElements = null;
  private MainActivity mMainActivity = null;
  private Button loadMoreButton = null;
  private boolean loadMoreButtonVisible = false;
  private ProgressBar mTopProgressBar;
  private SwipeRefreshLayout mSwipeRefreshLayout;
  
  private Handler mContextBarTimerHandler = null;
  private final Runnable mContextBarTimerCallback = new Runnable() {
    public void run() {
      hideContextualActionbar();
    }
  };
  private ActionMode mActionMode = null;

  TreeMap<Long, Account> mAccounts = null;

  public static MainActivityFragment getInstance() {
    return new MainActivityFragment();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    contextSelectedElements = new TreeSet<Long>();
    mContextBarTimerHandler = new Handler();


    View mRootView = inflater.inflate(R.layout.main, container, false);
    mMainActivity = (MainActivity)getActivity();
    mSwipeRefreshLayout = (SwipeRefreshLayout) mRootView.findViewById(R.id.SwipeRefreshLayout);
    mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshListener());
    mSwipeRefreshLayout.setColorSchemeColors(
            Color.parseColor("#373737"),
            Color.parseColor("#717171"),
            Color.parseColor("#A1A1A1"),
            Color.parseColor("#F1F1F1"));
    mTopProgressBar = (ProgressBar) mRootView.findViewById(R.id.progressbar);
    mListView = (ListView) mRootView.findViewById(R.id.list);
    
    mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    mListView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
      public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {
        MainActivityFragment.this.mActionMode = mode;
        
        long rawId = ((Cursor)(mAdapter.getItem(position))).getInt(0);
        if (checked) {
          contextSelectedElements.add(rawId);
        } else {
          contextSelectedElements.remove(rawId);
        }
        if (contextSelectedElements.size() > 0) {
          startContextualActionbarTimer();
          mode.setTitle(contextSelectedElements.size() + " selected");
          if (contextSelectedElements.size() == 1) {
            mode.getMenu().findItem(R.id.reply).setVisible(true);
            Context c = MainActivityFragment.this.getActivity();
            MessageListElement mle = MessageListDAO.getInstance(c).getMessageByRawId(contextSelectedElements.first(),
                    mAccounts);
            Account acc = mle.getAccount();
            MessageProvider mp = AndroidUtils.getMessageProviderInstanceByAccount(acc, mMainActivity);
            if (mp.isMessageDeletable()) {
              mode.getMenu().findItem(R.id.discard).setVisible(true);
              if (acc.isThreadAccount()) {
                mode.getMenu().findItem(R.id.discard).setTitle(R.string.delete_thread);
              } else {
                mode.getMenu().findItem(R.id.discard).setTitle(R.string.delete_message);
              }
            } else {
              mode.getMenu().findItem(R.id.discard).setVisible(false);
            }
          } else {
            mode.getMenu().findItem(R.id.reply).setVisible(false);
            mode.getMenu().findItem(R.id.discard).setVisible(true);
          }
        } else {
          mode.finish();
        }
      }

      public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (mTopProgressBar.getVisibility() == View.GONE) {
          MenuInflater inflater = mode.getMenuInflater();
          inflater.inflate(R.menu.main_list_context_menu, menu);
          contextSelectedElements.clear();
          return true;
        } else {
          return false;
        }

      }

      public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
      }

      public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
          case R.id.reply:
            if (contextSelectedElements.size() == 1) {
              long firstId = contextSelectedElements.first();
              MessageListElement message = MessageListDAO.getInstance(getActivity()).getMessageByRawId(firstId, mAccounts);
              Class classToLoad = Settings.getAccountTypeToMessageReplyer().get(message.getAccount().getAccountType());
              Intent intent = new Intent(mMainActivity, classToLoad);
              intent.putExtra(IntentStrings.Params.MESSAGE_RAW_ID, message.getRawId());
//              intent.putExtra(IntentStrings.Params.MESSAGE_ACCOUNT, (Parcelable) message.getAccount());
              mMainActivity.startActivity(intent);
            }
            hideContextualActionbar();
            return true;
          case R.id.discard:
            contextActionDeleteMessage();
            return true;
          case R.id.mark_seen:
            contextActionMarkMessage(true);
            hideContextualActionbar();
            return true;
          case R.id.mark_unseen:
            contextActionMarkMessage(false);
            hideContextualActionbar();
            return true;
          default:
            return false;
        }
      }

      public void onDestroyActionMode(ActionMode mode) {
        cancelContextualActionbarTimer();
        contextSelectedElements.clear();
      }
    });

    loadMoreButton = new Button(mMainActivity);
    loadMoreButton.setVisibility(View.GONE);
    loadMoreButton.setText("Load more ...");
    loadMoreButton.getBackground().setAlpha(0);
    loadMoreButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View arg0) {
        EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, EventLogger.LOGGER_STRINGS.CLICK.CLICK_LOAD_MORE_BTN, true);
        mMainActivity.loadMoreMessage();
      }
    });
    
    mListView.addFooterView(loadMoreButton);
    
    if (BatchedAsyncTaskExecutor.isProgressRunning(MainService.MESSAGE_LIST_QUERY_KEY)) {
      loadMoreButton.setEnabled(false);
    }

    mListView.setOnScrollListener(new LogOnScrollListener(mListView, mAdapter));
    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> av, View arg1, int itemIndex, long arg3) {
        if (av.getItemAtPosition(itemIndex) == null) return;
        Cursor cursorOfMessage = (Cursor)av.getItemAtPosition(itemIndex);
        MessageListElement message = MessageListDAO.cursorToMessageListElement(cursorOfMessage, mAccounts);
        Account a = message.getAccount();
        Class classToLoad = Settings.getAccountTypeToMessageDisplayer().get(a.getAccountType());
        Intent intent = new Intent(mMainActivity, classToLoad);
//        intent.putExtra(IntentStrings.Params.MESSAGE_ID, message.getId());
//        intent.putExtra(IntentStrings.Params.MESSAGE_ACCOUNT, (Parcelable) message.getAccount());
        intent.putExtra(IntentStrings.Params.MESSAGE_RAW_ID, message.getRawId());
        boolean changed = !message.isSeen();
        if (!message.isSeen()) {
          MessageListDAO.getInstance(getActivity()).updateMessageToSeen(message.getRawId(), true);
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
        mMainActivity.appendVisibleElementToStringBuilder(builder, mListView, mAdapter);
        builder.append(changed);
        EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, builder.toString(), true);
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
    super.onResume();
    mAccounts = AccountDAO.getInstance(getActivity()).getIdToAccountsMap();
    updateAdapter();
    setLoadMoreButtonVisibility();
  }

  @Override
  public void onPause() {
    super.onPause();
    hideContextualActionbar();
  }
  
  
  private void cancelContextualActionbarTimer() {
    if (mContextBarTimerHandler != null) {
      mContextBarTimerHandler.removeCallbacks(mContextBarTimerCallback);
    }
  }
  
  private void startContextualActionbarTimer() {
    cancelContextualActionbarTimer();
    mContextBarTimerHandler.postDelayed(mContextBarTimerCallback, 10000);
  }

  
  public void hideContextualActionbar() {
    if (mActionMode != null) {
      mActionMode.finish();
      contextSelectedElements.clear();
    }
  }
  
    
  private void contextActionDeleteMessage() {
    cancelContextualActionbarTimer();
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {

      LinkedList<MessageListElement> deletemessages = new LinkedList<MessageListElement>();
      MessageListElement mle;
      
      for (Long idx : contextSelectedElements) {        
        mle = MessageListDAO.getInstance(getActivity()).getMessageByRawId(idx, mAccounts);
        deletemessages.add(mle);        
      }
     
      MessageDeleteHandler handler = new MessageDeleteHandler(getActivity()) {
        @Override
        public void onMainListDelete(List<MessageListElement> deletedMessageList) {
          try {
            MessageListDAO.getInstance(getActivity()).removeMessage(getActivity(), deletedMessageList);
          } catch (Exception e) {
            Log.d("rgai", "", e);
          }
          notifyAdapterChange();
        }

        @Override
        public void onThreadListDelete(long deletedMessageListRawId, String deletedSimpleMessageId,
                                       boolean isInternetNeededForProvider) {}

        @Override
        public void onComplete() {
          mTopProgressBar.setVisibility(View.GONE);
        }

        @Override
        public void onTimeout(Context context) {
          Toast.makeText(mContext, "Timeout while deleting message", Toast.LENGTH_LONG).show();
          mTopProgressBar.setVisibility(View.GONE);
        }
      };      
      
      MessageDeletionAsyncTask messageMarker = new MessageDeletionAsyncTask(deletemessages, null,
           handler, true, getActivity().getApplicationContext());
      messageMarker.setTimeout(10000);
      messageMarker.executeTask(MainActivityFragment.this.getActivity(), null);

      mTopProgressBar.setVisibility(View.VISIBLE);

      hideContextualActionbar();
      }
    });
    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        startContextualActionbarTimer();
      }
    }); 
    builder.setTitle("Delete message");
    builder.setMessage("Delete selected message"+ (contextSelectedElements.size() == 1 ? "" : "s") +"?").show();
    
  }
  
  
  private void contextActionMarkMessage(boolean seen) {
    
    HashMap<Account, TreeSet<String>> messageIdsToAccounts = new HashMap<Account, TreeSet<String>>();
    for (Long rawId : contextSelectedElements) {
      MessageListElement mle = MessageListDAO.getInstance(getActivity()).getMinimalMessage(rawId, mAccounts);
      MessageListDAO.getInstance(getActivity()).updateMessageToSeen(rawId, seen);
      if (!messageIdsToAccounts.containsKey(mle.getAccount())) {
        messageIdsToAccounts.put(mle.getAccount(), new TreeSet<String>());
      }
      messageIdsToAccounts.get(mle.getAccount()).add(mle.getId());
    }
    
    // TODO: block auto update while marking messages
    
    MessageSeenMarkerHandler handler = new MessageSeenMarkerHandler(this);
    List<BatchedTimeoutAsyncTask> tasks = new LinkedList<BatchedTimeoutAsyncTask>();
    for (Map.Entry<Account, TreeSet<String>> entry : messageIdsToAccounts.entrySet()) {
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
      batchedMarker.execute(getActivity());
    } catch (Exception ex) {
      Log.d("rgai", "mark message exception", ex);
    }
  }
  
  public ListView getListView() {
    return mListView;
  }
  
  public MainListAdapter getAdapter() {
    return mAdapter;
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
      
      mSwipeRefreshLayout.setEnabled(false);
      if (lv != null && lv.getChildCount() > 0) {
        if (lv.getChildAt(0).getTop() == 0) {
          mSwipeRefreshLayout.setEnabled(true);
        }
      }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
      if (!contextSelectedElements.isEmpty()) {
        startContextualActionbarTimer();
      }
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
      EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, builder.toString(), true);
    }

  }
  
  
  public void loadStateChanged(boolean refreshing) {
    if (loadMoreButton != null) {
      if (refreshing) {
        mSwipeRefreshLayout.setRefreshing(true);
        mSwipeRefreshLayout.setEnabled(false);
        loadMoreButton.setEnabled(false);
      } else {
        mSwipeRefreshLayout.setRefreshing(false);
        mSwipeRefreshLayout.setEnabled(true);
        loadMoreButton.setEnabled(true);
      }
    }
  }
  
  public void notifyAdapterChange() {
    updateAdapter();
    setLoadMoreButtonVisibility();
    if (mAdapter != null) {
      if (!contextSelectedElements.isEmpty()) {
        for (int i = 1; i < mAdapter.getCount(); i++) {
          long rawId = ((Cursor) (mAdapter.getItem(i))).getLong(0);
          mListView.setItemChecked(i, contextSelectedElements.contains(rawId));
        }
      }
    }
  }


  private void setLoadMoreButtonVisibility() {
    if (mAdapter != null) {
      if (!mAdapter.isEmpty() && !loadMoreButtonVisible) {
        loadMoreButton.setVisibility(View.VISIBLE);
        loadMoreButtonVisible = true;
      }
    }
  }


  private void updateAdapter() {

    LinkedList<Long> accountIds = new LinkedList<>();
    
    if (!MainActivity.selectedAccounts.isEmpty()) {
      for (int i=0; i < MainActivity.selectedAccounts.size(); i++) {
        accountIds.add(MainActivity.selectedAccounts.get(i).getDatabaseId());
      }
    }

    boolean zoneActivated = StoreHandler.isZoneStateActivated(getActivity());
    GpsZone closestZone = GpsZone.getClosest(YakoApp.getSavedGpsZones(getActivity()));
    int importantDrawable = closestZone != null ? closestZone.getZoneType().getDrawable() : R.drawable.ic_important;
    if (mAdapter == null) {
      mAdapter = new MainListAdapter((YakoApp)getActivity().getApplication(), mMainActivity, importantDrawable,
              zoneActivated, closestZone,
              MessageListDAO.getInstance(getActivity()).getAllMessagesCursor(accountIds, true, zoneActivated && closestZone != null), mAccounts);
    } else {
      Cursor newCursor = MessageListDAO.getInstance(getActivity()).getAllMessagesCursor(accountIds, true, zoneActivated && closestZone != null);
      mAdapter.changeCursor(newCursor);
      mAdapter.setImportantDrawable(importantDrawable);
      mAdapter.setZoneActivity(zoneActivated);
      mAdapter.setClosestZone(closestZone);
      mAdapter.setAccounts(mAccounts);
      mAdapter.notifyDataSetChanged();
    }

    if (mListView.getAdapter() == null) {
      mListView.setAdapter(mAdapter);
    }
  }

  private class SwipeRefreshListener implements SwipeRefreshLayout.OnRefreshListener {

    @Override
    public void onRefresh() {
      EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, EventLogger.LOGGER_STRINGS.CLICK.CLICK_REFRESH_BTN, true);
      mMainActivity.reloadMessages(true);
    }
  }
  
}
