package hu.rgai.yako.messageproviders;

import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.FacebookAccount;
import hu.rgai.yako.beens.FacebookMessageRecipient;
import hu.rgai.yako.beens.FullMessage;
import hu.rgai.yako.beens.FullSimpleMessage;
import hu.rgai.yako.beens.FullThreadMessage;
import hu.rgai.yako.beens.HtmlContent;
import hu.rgai.yako.beens.MainServiceExtraParams;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.beens.MessageListResult;
import hu.rgai.yako.beens.MessageRecipient;
import hu.rgai.yako.beens.Person;
import hu.rgai.yako.beens.SentMessageBroadcastDescriptor;
import hu.rgai.yako.broadcastreceivers.MessageSentBroadcastReceiver;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.services.schedulestarters.MainScheduler;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.cert.CertPathValidatorException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.net.ssl.SSLHandshakeException;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;

/**
 * 
 * @author Tamas Kojedzinszky
 */
// TODO: this should be a singletone class
public class FacebookMessageProvider implements ThreadMessageProvider {

  private static volatile XMPPConnection xmpp = null;
  // use this variable to access facebook
  private final FacebookAccount account;

  public FacebookMessageProvider(FacebookAccount account) {
    this.account = account;
  }

  @Override
  public Account getAccount() {
    return account;
  }

  @Override
  public MessageListResult getMessageList(int offset, int limit, TreeSet<MessageListElement> loadedMessages,
                                          boolean isNewMessageArrivedRequest)
      throws CertPathValidatorException, SSLHandshakeException, ConnectException,
      NoSuchProviderException, UnknownHostException, IOException, MessagingException,
      AuthenticationFailedException {
    return getMessageList(offset, limit, loadedMessages, 20, isNewMessageArrivedRequest);
  }

  @Override
  public MessageListResult getMessageList(int offset, int limit, TreeSet<MessageListElement> loadedMessages,
                                          int snippetMaxLength, boolean isNewMessageArrivedRequest)
      throws CertPathValidatorException, SSLHandshakeException, ConnectException,
      NoSuchProviderException, UnknownHostException, IOException, MessagingException,
      AuthenticationFailedException {
    Bundle params = new Bundle();

    final List<MessageListElement> messages = new LinkedList<MessageListElement>();

    String fqlQuery = "{"
        + "'msgs':"
        + "'SELECT thread_id, originator, recipients, unread, unseen, subject, snippet, updated_time "
        + " FROM thread"
        + " WHERE folder_id = 0"
        + " ORDER BY updated_time DESC LIMIT " + offset + "," + limit + "'"
        + ","
        + "'friend':"
        + "'SELECT name, username, uid"
        + " FROM user"
        + " WHERE uid IN (SELECT recipients FROM #msgs)'"
        + "}";
    params.putString("q", fqlQuery);

    Session session = Session.getActiveSession();
    Request request = new Request(
        session,
        "/fql",
        params,
        HttpMethod.GET,
        new Request.Callback() {
          @Override
          public void onCompleted(Response response) {
            if (response != null) {
              if (response.getGraphObject() != null) {
                try {
                  GraphObject go = response.getGraphObject();
                  JSONObject jso = go.getInnerJSONObject();
                  JSONArray arr = jso.getJSONArray("data");

                  // loop through result sets
                  for (int i = 0; i < (arr.length()); i++) {
                    JSONObject resultSet = arr.getJSONObject(i);
                    String resSetName = resultSet.getString("name");
                    String resSet = resultSet.getString("fql_result_set");
                    if (resSetName.equals("msgs")) {
                      JSONArray msgArr = new JSONArray(resSet);

                      // loop through messages
                      for (int j = 0; j < msgArr.length(); j++) {
                        JSONObject msg = msgArr.getJSONObject(j);

                        // fetching recipients
                        JSONArray recipientsArr = new JSONArray(msg.getString("recipients"));
                        List<String> recipIds = new LinkedList<String>();
                        List<Person> recipients = new LinkedList<Person>();
                        for (int l = 0; l < recipientsArr.length(); l++) {
                          String id = recipientsArr.getString(l);
                          if (!account.getId().equals(id)) {
                            recipIds.add(id);
                            recipients.add(new Person(id, null, Type.FACEBOOK));
                          }
                        }

                        // recipipIds MUST contain an id which is not mine
                        assert !recipIds.isEmpty();

                        // building list item title
                        boolean seen = msg.getInt("unseen") == 0;
                        int unreadCount = msg.getInt("unread");
                        String snippet = msg.getString("snippet");
                        Person from = null;
                        if (recipients.size() == 1) {
                          from = recipients.get(0);
                        }
                        messages.add(new MessageListElement(
                                msg.getString("thread_id"),
                                seen,
                                snippet,
                                unreadCount,
                                0,
                                from,
                                recipients,
                                new Date(msg.getLong("updated_time") * 1000),
                                account,
                                MessageProvider.Type.FACEBOOK));
                      }
                    } else if (resSetName.equals("friend")) {
                      JSONArray userArr = new JSONArray(resSet);
                      // loop through friends
                      for (int j = 0; j < userArr.length(); j++) {
                        JSONObject msg = userArr.getJSONObject(j);
                        // matching friend names to messages by id
                        for (int k = 0; k < messages.size(); k++) {
                          if (messages.get(k).getFrom() != null) {
                            if (messages.get(k).getFrom().getId().equals(msg.getString("uid"))) {
                              messages.get(k).getFrom().setName(msg.getString("name"));
                              messages.get(k).getFrom().setSecondaryName(msg.getString("username"));
                            }
                          }
                          if (messages.get(k).isGroupMessage()) {
                            if (messages.get(k).getRecipientsList() != null) {
                              for (Person rec : messages.get(k).getRecipientsList()) {
                                if (rec.getId().equals(msg.getString("uid"))) {
                                  rec.setName(msg.getString("name"));
                                  rec.setSecondaryName(msg.getString("username"));
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                } catch (Throwable t) {
                 Log.d("rgai", "", t);
                }
              }
            }
          }
        });
    Request.executeAndWait(request);
    MessageListResult mlr = new MessageListResult(messages, MessageListResult.ResultType.CHANGED);
    return mlr;
  }


  private void initConnection(FacebookAccount fba, final Context context) {
    // Log.d("rgai", "initing xmpp connection");
    if (xmpp == null || !xmpp.isConnected()) {
      // Log.d("rgai", "try connecting to XMPP");
      StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
      StrictMode.setThreadPolicy(policy);

      ConnectionConfiguration config = new ConnectionConfiguration("chat.facebook.com", 443, "chat.facebook.com");
      config.setSASLAuthenticationEnabled(true);
      config.setSecurityMode(SecurityMode.enabled);
      config.setRosterLoadedAtLogin(true);
      config.setSendPresence(false);

      xmpp = new XMPPConnection(config);

      try {
        xmpp.connect();
        SmackConfiguration.setPacketReplyTimeout(10000);
        xmpp.login(fba.getUniqueName(), fba.getPassword());
        // Log.d("rgai", "connected to XMPP");

        xmpp.getChatManager().addChatListener(new ChatManagerListener() {
          @Override
          public void chatCreated(Chat chat, boolean arg1) {

            chat.addMessageListener(new MessageListener() {
              @Override
              public void processMessage(Chat chat, Message message) {
                if (message != null && message.getBody() != null) {
                  // Log.d("rgai", "MESSAGE FROM -> " + message.getFrom());
                  Intent res = new Intent(Settings.Intents.NEW_MESSAGE_ARRIVED_BROADCAST);
                  res.putExtra("type", MessageProvider.Type.FACEBOOK.toString());
                  context.sendBroadcast(res);

                  // always run MainService, so new messages can be stored
                  
                  Intent service = new Intent(context, MainScheduler.class);
                  service.setAction(Context.ALARM_SERVICE);
                  MainServiceExtraParams eParams = new MainServiceExtraParams();
                  eParams.addAccount(account);
                  eParams.setForceQuery(true);
                  service.putExtra(IntentStrings.Params.EXTRA_PARAMS, eParams);
                  context.sendBroadcast(service);

                }
              }
            });
          }
        });

      } catch (XMPPException e) {
        Log.d("rgai", "XMPP connection failed:", e);
        xmpp.disconnect();
      } catch (Exception k) {
        Log.d("rgai", "XMPP connection failed", k);
      }
    }
  }


  @Override
  public FullThreadMessage getMessage(String id, int offset, int limit) throws NoSuchProviderException, MessagingException, IOException {

    Bundle params = new Bundle();
    final FullThreadMessage ftm = new FullThreadMessage();

    String fqlQuery = "{"
        + "'msgs':"
        + "'SELECT author_id, body, created_time, message_id"
        + " FROM message"
        + " WHERE thread_id = " + id + ""
        + " ORDER BY created_time DESC"
        + " LIMIT " + offset + "," + limit + "'"
        + ","
        + "'friend':"
        + "'SELECT name, username, uid"
        + " FROM user"
        + " WHERE uid IN (SELECT author_id FROM #msgs)'"
        + "}";
    params.putString("q", fqlQuery);

    Session session = Session.getActiveSession();
    Request request = new Request(
            session,
            "/fql",
            params,
            HttpMethod.GET,
            new Request.Callback() {
              public void onCompleted(Response response) {
                if (response != null) {
                  if (response.getGraphObject() != null) {
                    try {
                      GraphObject go = response.getGraphObject();
                      JSONObject jso = go.getInnerJSONObject();
                      JSONArray arr = jso.getJSONArray("data");

                      // loop through result sets
                      // TODO: we should first collect the persons, that would more efficient
                      for (int i = 0; i < (arr.length()); i++) {
                        JSONObject resultSet = arr.getJSONObject(i);
                        String resSetName = resultSet.getString("name");
                        String resSet = resultSet.getString("fql_result_set");
                        if (resSetName.equals("msgs")) {
                          JSONArray msgArr = new JSONArray(resSet);

                          // loop through messages
                          for (int j = 0; j < msgArr.length(); j++) {
                            JSONObject msg = msgArr.getJSONObject(j);

                            // building list item title
//                    int unreadCount = msg.getInt("unread");
                            String body = msg.getString("body");

                            ftm.addMessage(new FullSimpleMessage(
                                    -1,
                                    msg.getString("message_id"),
                                    "",
                                    new HtmlContent(body, HtmlContent.ContentType.TEXT_PLAIN),
                                    new Date(msg.getLong("created_time") * 1000),
                                    new Person(msg.getString("author_id"), null, MessageProvider.Type.FACEBOOK),
                                    msg.getString("author_id").equals(account.getId()),
                                    MessageProvider.Type.FACEBOOK,
                                    null));
                          }
                        } else if (resSetName.equals("friend")) {
                          JSONArray userArr = new JSONArray(resSet);
                          // loop through friends
                          for (int j = 0; j < userArr.length(); j++) {
                            JSONObject user = userArr.getJSONObject(j);
                            // matching friend names to messages by id
                            for (FullSimpleMessage ma : ftm.getMessages()) {
                              if (ma.getFrom().getId().equals(user.getString("uid"))) {
                                ma.getFrom().setName(user.getString("name"));
                                ma.getFrom().setSecondaryName(user.getString("username"));

                              }
                            }
                          }
                        }
                      }
                    } catch (Throwable t) {
                      Log.d("rgai", "", t);
                    }
                  }
                }
              }
            });
    Request.executeAndWait(request);

    return ftm;
  }

  @Override
  public void sendMessage(Context context, SentMessageBroadcastDescriptor sentMessageData, Set<? extends MessageRecipient> to,
          String content, String subject) {

    ConnectionConfiguration config = new ConnectionConfiguration("chat.facebook.com", 443, "chat.facebook.com");

    config.setSASLAuthenticationEnabled(true);
    config.setSecurityMode(SecurityMode.enabled);
    config.setRosterLoadedAtLogin(true);
    config.setSendPresence(false);

    boolean success = true;
    
    if (xmpp == null || !xmpp.isConnected()) {
       // TODO: show notification if connection was unsuccessful and message was not sent!
      try {
        xmpp.connect();
        SmackConfiguration.setPacketReplyTimeout(10000);
        xmpp.login(account.getUniqueName(), account.getPassword());
      } catch (XMPPException e) {
        Log.d("yako", "", e);
        xmpp.disconnect();
        success = false;
      } catch (Exception e) {
        Log.d("yako", "", e);
        success = false;
      }
    }

    if (success) {
      for (MessageRecipient mr : to) {
        if (!success) {
          break;
        }
        FacebookMessageRecipient fmr = (FacebookMessageRecipient) mr;

        String toStr = fmr.getId() + "@chat.facebook.com";
  //      Log.d("rgai", "SENDING MESSAGE TO: " + toStr);
        Chat chat = xmpp.getChatManager().createChat(toStr, null);
        
        try {
          chat.sendMessage(content);
        } catch (XMPPException e) {
          Log.d("rgai", "", e);
          success = false;
        } catch (IllegalStateException ex) {
          Log.d("rgai", "", ex);
          success = false;
        }
      }
    }
    
    
    
    MessageProvider.Helper.sendMessageSentBroadcast(context, sentMessageData,
            success ? MessageSentBroadcastReceiver.MESSAGE_SENT_SUCCESS : MessageSentBroadcastReceiver.MESSAGE_SENT_FAILED);
    
  }

  public void onClickPickQuery() {

    String fqlQuery = "{"
        + "'notifications':'SELECT unread, unseen,recent_authors, thread_id FROM thread where folder_id=0 AND unseen > 0 ORDER BY thread_id DESC ',"
        + "'messages':'SELECT author_id, created_time, body FROM message WHERE thread_id IN "
        + "(SELECT thread_id FROM #notifications)',"
        + "'users':'SELECT uid, name, pic_square FROM user WHERE uid IN "
        + "(SELECT author_id FROM #messages)',"
        + "}";

    Bundle params = new Bundle();
    params.putString("q", fqlQuery);

    Session session = Session.getActiveSession();
    Request request = new Request(session, "/fql", params, HttpMethod.GET,
        new Request.Callback() {
          @Override
          public void onCompleted(Response response) {

            try {

              GraphObject go = response.getGraphObject();
              JSONObject jso = go.getInnerJSONObject();
              JSONArray arr = jso.getJSONArray("data").getJSONObject(1)
                  .getJSONArray("fql_result_set");
              JSONArray arr2 = jso.getJSONArray("data").getJSONObject(2)
                  .getJSONArray("fql_result_set");

              if (arr.length() != 0) {
                String name = "";
                for (int i = 0; i < (arr.length()); i++) {

                  JSONObject json_obj = arr.getJSONObject(i);
                  JSONObject json_obj2;

                  for (int j = 0; j < (arr2.length()); j++) {

                    json_obj2 = arr2.getJSONObject(j);

                    if (json_obj.getString("author_id").contains(json_obj2.getString("uid"))) {
                      name += json_obj2.getString("name");
                      name += " : ";

                    }

                  }

                  name += json_obj.getString("body");
                  name += "\n";
                  Log.i("FONTOS", name.toString());

                }

              } else {

                Log.i("FONTOS", "Nincs adat!");
              }

        } catch (JSONException e) {
          Log.d("rgai", "", e);
        }

          }
        });
    Request.executeAndWait(request);

  }

  @Override
  public FullMessage getMessage(String id) throws NoSuchProviderException, MessagingException, IOException {
    return getMessage(id, 0, 20);
  }

  @Override
  public void markMessageAsRead(String id, boolean seen) throws NoSuchProviderException, MessagingException, IOException {
    // we cannot set facebook messages status to read...
  }

  @Override
  public boolean canBroadcastOnNewMessage() {
    return true;
  }

  @Override
  public boolean isConnectionAlive() {
    return xmpp != null && xmpp.isConnected();
  }

  @Override
  public synchronized void establishConnection(Context context) {
    if (!isConnectionAlive()) {
      initConnection(account, context);
    }
  }

  @Override
  public boolean canBroadcastOnMessageChange() {
    return false;
  }

  @Override
  public String toString() {
    return "FacebookMessageProvider{" + "instance=" + account + '}';
  }

  public void dropConnection(Context context) {
    if (isConnectionAlive()) {
      xmpp.disconnect();
      xmpp = null;
    }
  }

  @Override
  public void markMessagesAsRead(String[] id, boolean seen) throws NoSuchProviderException, MessagingException, IOException {
    // we cannot mark facebook messages on server side
  }

  @Override
  public boolean isMessageDeletable() {
    return false;
  }

  @Override
  public boolean testConnection() {
    return true;
  }

  @Override
  public MessageListResult getUIDListForMerge(String lowestStoredMessageUID) {
    Log.d("rgai", "NOT SUPPORTED YET", new Exception("method not supported"));
    return null;
  }

  public void deleteThread(String id) {
    // we cannot delete facebook messages through API
  }

  @Override
  public void deleteMessage(String id) {
    // we cannot delete facebook messages through API
  }

}