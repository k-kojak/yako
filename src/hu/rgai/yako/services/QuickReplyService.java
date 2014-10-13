package hu.rgai.yako.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import hu.rgai.yako.beens.*;
import hu.rgai.yako.broadcastreceivers.SimpleMessageSentBroadcastReceiver;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.handlers.TimeoutHandler;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.FullMessageDAO;
import hu.rgai.yako.sql.MessageListDAO;
import hu.rgai.yako.view.activities.MessageReplyActivity;
import hu.rgai.yako.workers.MessageSender;
import net.htmlparser.jericho.Source;

import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Created by kojak on 10/13/2014.
 */
public class QuickReplyService extends IntentService {

  public static String ACTION_QUICK_REPLY = "hu.rgai.yako.QUICK_REPLY";

  public QuickReplyService() {
    super("Quick reply service");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    if (ACTION_QUICK_REPLY.equals(intent.getAction())) {

      NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      mNotificationManager.cancel(Settings.NOTIFICATION_NEW_MESSAGE_ID);

      long rawId = intent.getExtras().getLong(IntentStrings.Params.MESSAGE_RAW_ID);
      String quickAnswer = intent.getExtras().getString(IntentStrings.Params.QUICK_ANSWER_OPTION);

      TreeMap<Long, Account> accounts = AccountDAO.getInstance(this).getIdToAccountsMap();
      MessageListElement message = MessageListDAO.getInstance(this).getMessageByRawId(rawId, accounts);

      if (message != null) {
        String subject = "";
        String content = "";

        Account from = message.getAccount();
        if (message.getMessageType().equals(MessageProvider.Type.GMAIL)
                || message.getMessageType().equals(MessageProvider.Type.EMAIL)) {
          subject = message.getTitle();

          TreeSet<FullSimpleMessage> fullMessages = FullMessageDAO.getInstance(this).getFullSimpleMessages(this,
                  message.getRawId());
          FullSimpleMessage fullMessage = fullMessages.first();
          Source source = new Source("<br /><br />" + fullMessage.getContent().getContent());
          content = source.getRenderer().toString();
        }

        MessageRecipient recipient = MessageRecipient.Helper.personToRecipient(message.getFrom());
        SentMessageBroadcastDescriptor sentMessBroadcD = new SentMessageBroadcastDescriptor(
                SimpleMessageSentBroadcastReceiver.class, IntentStrings.Actions.MESSAGE_SENT_BROADCAST);

        SentMessageData smd = MessageReplyActivity.getSentMessageDataToAccount(recipient.getDisplayName(), from);
        sentMessBroadcD.setMessageData(smd);

        MessageSender rs = new MessageSender(recipient, from, sentMessBroadcD,
                new TimeoutHandler() {
                  @Override
                  public void onTimeout(Context context) {
                    Toast.makeText(context, "Unable to send message...", Toast.LENGTH_SHORT).show();
                  }
                },
                subject, quickAnswer + content, this);

        rs.setTimeout(20000);
        rs.executeTask(this, null);
      }
    }
  }
}
