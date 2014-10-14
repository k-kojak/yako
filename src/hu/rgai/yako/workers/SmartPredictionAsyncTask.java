package hu.rgai.yako.workers;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.FullSimpleMessage;
import hu.rgai.yako.beens.FullThreadMessage;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.handlers.MessageListerHandler;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.smarttools.DummyMessagePredictionProvider;
import hu.rgai.yako.smarttools.MessagePredictionProvider;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.FullMessageDAO;
import hu.rgai.yako.sql.MessageListDAO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;
import java.util.TreeSet;

public class SmartPredictionAsyncTask extends TimeoutAsyncTask<Void, Void, Void> {

  private final boolean mSingleRun;
  private final MessageListElement mMessage;
  private final Context mContext;

  public SmartPredictionAsyncTask(Context context, boolean singleRun, MessageListElement message) {
    super(null);
    mContext = context;
    mSingleRun = singleRun;
    mMessage = message;
  }

  public SmartPredictionAsyncTask(Context context, boolean singleRun) {
    this(context, singleRun, null);
  }

  @Override
  protected Void doInBackground(Void... params) {
    Collection<MessageListElement> messages;
    if (mSingleRun) {
      messages = new ArrayList<MessageListElement>(1);
      messages.add(mMessage);
    } else {
      TreeMap<Long, Account> accounts = AccountDAO.getInstance(mContext).getIdToAccountsMap();
      messages = MessageListDAO.getInstance(mContext).getAllMessages(accounts);
    }

    MessagePredictionProvider predictionProvider = new DummyMessagePredictionProvider();
    for (MessageListElement mle : messages) {
      TreeSet<FullSimpleMessage> contents = FullMessageDAO.getInstance(mContext).getFullSimpleMessages(mContext,
              mle.getRawId());
      if (mle.getMessageType().equals(MessageProvider.Type.EMAIL) || mle.getMessageType().equals(MessageProvider.Type.GMAIL)) {
        mle.setFullMessage(contents.first());
      } else {
        mle.setFullMessage(new FullThreadMessage(contents));
      }
      double ratio = predictionProvider.predictMessage(mContext, mle);
      MessageListDAO.getInstance(mContext).setMessageAsImportant(mle.getRawId(),
              MessagePredictionProvider.Helper.isImportant(ratio));
    }

    return null;
  }

  @Override
  protected void onPostExecute(Void aVoid) {
    Intent i = new Intent(MessageListerHandler.MESSAGE_PACK_LOADED_INTENT);
    LocalBroadcastManager.getInstance(mContext).sendBroadcast(i);
  }

}
