package hu.rgai.android.messageproviders;

import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.account.FacebookAccount;
import hu.uszeged.inf.rgai.messagelog.beans.FacebookMessageRecipient;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.FullFacebookMessage;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.FullSimpleMessage;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.MessageAtom;
import hu.uszeged.inf.rgai.messagelog.beans.MessageListElement;
import hu.uszeged.inf.rgai.messagelog.beans.MessageRecipient;
import hu.uszeged.inf.rgai.messagelog.beans.Person;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.cert.CertPathValidatorException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.net.ssl.SSLHandshakeException;

import org.jivesoftware.smack.Chat;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;


import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.Response;
import com.facebook.model.GraphObject;
import java.util.Map;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FacebookMessageProvider implements MessageProvider {

  // use this variable to access facebook
  private FacebookAccount account;
  private MessageListener mslistener;
//  private Activity activity;

// public void onCreate(Bundle savedInstanceState) {
//         super.onCreate(savedInstanceState);
//         // setContentView(R.layout.activity_main);
// }
//
  public FacebookMessageProvider(FacebookAccount account) {
    this.account = account;
    System.out.println("1111111");
//    this.activity = activity;
    System.out.println("222222");
  }

  @Override
  public List<MessageListElement> getMessageList(int offset, int limit) throws CertPathValidatorException,
          SSLHandshakeException, ConnectException, NoSuchProviderException, UnknownHostException, IOException,
          MessagingException, AuthenticationFailedException {
    
    String fqlQuery = "SELECT author_id, body, created_time, message_id, thread_id "
            + "FROM message "
            + "WHERE thread_id = 2225482129944;";
//    String fqlQuery = "SELECT name FROM user WHERE uid = me();";
    Bundle params = new Bundle();
    params.putString("q", fqlQuery);

    Session session = Session.getActiveSession();
    Request request = new Request(
            session,
            "/fql", 
            params, 
            HttpMethod.GET, 
            new Request.Callback(){
              public void onCompleted(Response response) {
                if (response != null) {
                  Log.d("rgai", "Got results: " + response.toString());
                  if (response.getGraphObject() != null) {
                    
                    Map<String, Object> m = response.getGraphObject().asMap();
                    Log.d("rgai", m.keySet().toString());
                    for (String s : m.keySet()) {
                      Log.d("rgai", m.get(s).toString());
                    }
//                    Map<String, Object> m = response.getGraphObject().asMap();
//                    Log.d("rgai", m.keySet().toString());
                  }
                } else {
                  Log.d("rgai", "RESPONSE IS NULL");
                }
              }
            });
    Request.executeAndWait(request);
    
    // EXAMPLE CODE FOR PETI

    ///facebook
    System.out.println("333333333");
    // start Facebook Login
//         Session.
//    Session.openActiveSession(this, true, new Session.StatusCallback() {
      // callback when session changes state
//         @Override
//      public void call(Session session, SessionState state, Exception exception) {
//        if (session.isOpened()) {
//          System.out.println("44444");
//          onClickPickQuery();
//         // make request to the /me API
//         Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {
          //
//         // callback after Graph API response with user object
//         @Override
//         public void onCompleted(GraphUser user, Response response) {
//         if (user != null) {
//         TextView welcome = (TextView) findViewById(R.id.hello);
//         welcome.setText("Hello " + user.getName() + "!");
//         }
//         }
//         });
//        } else {
//
//          Log.i("FONTOS", "Nincs kapcsoalt");
//        }
//      }
//    });


    //onClickPickQuery();
    ///facebook





    List<MessageListElement> messages = new LinkedList<MessageListElement>();
    // getting sender information, just give a random id, and the real name of the user
    Person sender = new Person(1, "Kis Zoltán");

    messages.add(new MessageListElement(1, false, "Title", "Subtitle", sender, new Date(), Type.FACEBOOK));

    sender = new Person(2, "Nagy Aladár");
    messages.add(new MessageListElement(2, false, "Title2", "Subtitle2", sender, new Date(), Type.FACEBOOK));

    return messages;
  }

  @Override
  public FullSimpleMessage getMessage(long id) throws NoSuchProviderException, MessagingException, IOException {
    // EXAMPLE CODE FOR PETI
    Person sender = new Person(3, "Zelk Zoltán");
    FullFacebookMessage ffm = new FullFacebookMessage(id, sender, Type.EMAIL);

    ffm.addMessage(new MessageAtom("This is the content of a message item...", new Date(), null, null));
    ffm.addMessage(new MessageAtom("This is the content of another...", new Date(), null, null));
    ffm.addMessage(new MessageAtom("This is the third message...", new Date(), null, null));

    return ffm;
  }

  @Override
  public void sendMessage(Set<? extends MessageRecipient> to, String content, String subject) throws
          NoSuchProviderException, MessagingException, IOException {

    ConnectionConfiguration config = new ConnectionConfiguration("chat.facebook.com", 5222, "chat.facebook.com");

    config.setSASLAuthenticationEnabled(true);
    config.setSecurityMode(SecurityMode.enabled);
    config.setRosterLoadedAtLogin(true);
    config.setSendPresence(false);

    final XMPPConnection xmpp = new XMPPConnection(config);

    try {
      xmpp.connect();
      SmackConfiguration.setPacketReplyTimeout(10000);
      xmpp.login(account.getUniqueName(), account.getPassword());
    } catch (XMPPException e) {
      xmpp.disconnect();
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }

    for (MessageRecipient mr : to) {
      FacebookMessageRecipient fmr = (FacebookMessageRecipient) mr;

      Chat chat = xmpp.getChatManager().createChat(fmr.getId() + "@chat.facebook.com", mslistener);

      try {
        chat.sendMessage(content);
      } catch (XMPPException e) {
        e.printStackTrace();
      }

      // sending message here to facebook user using the information in FacebookMessageRecipient class
    }
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
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

      }
    });
    Request.executeAndWait(request);
    // Request.executeBatchAndWait(request);
//                 Request.executeBatchAsync(request);

  }
}