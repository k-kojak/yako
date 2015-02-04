// TODO: if NO FRESH MAIL result comes, but then the amount of "initial messages" at setting panel changed,
// and then refresh pressed, then it works incorrect: not loads more message

package hu.rgai.yako.messageproviders;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPInputStream;
import com.sun.mail.smtp.SMTPTransport;
import hu.rgai.android.test.MainActivity;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.Attachment;
import hu.rgai.yako.beens.EmailAccount;
import hu.rgai.yako.beens.EmailContent;
import hu.rgai.yako.beens.EmailMessageRecipient;
import hu.rgai.yako.beens.FullMessage;
import hu.rgai.yako.beens.FullSimpleMessage;
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
import hu.rgai.yako.messageproviders.socketfactory.MySSLSocketFactory;
import hu.rgai.yako.services.schedulestarters.MainScheduler;
import hu.rgai.yako.sql.MessageRecipientDAO;
import hu.rgai.yako.view.activities.InfEmailSettingActivity;
import hu.rgai.yako.workers.TimeoutAsyncTask;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.Security;
import java.security.cert.CertPathValidatorException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.mail.search.FlagTerm;
import javax.net.ssl.SSLHandshakeException;

/**
 * Implements a simple email message providing via IMAP protocol.
 * 
 * This class (in theory) can handle all email instance types which supports IMAP.
 * 
 * @author Tamas Kojedzinszky
 */
public class SimpleEmailMessageProvider implements MessageProvider, SplittedMessageProvider {

  private final AccountFolder accountFolder;
  private final EmailAccount account;
  private String attachmentFolder = "../files/";
//  private AttachmentProgressUpdate progressUpdate = null;

  private IMAPFolder idleFolder;
  private volatile FolderIdleWithTimestamp idleThread;
  private volatile static Context context = null;
  
  
  private MessageCallback messageListener = null;


  // this String holds a unique state id for a query
  // String is a concatenation of nextMessageUID+messageCount
  // this string is unique for a state
  private String validityKey;

  private static HashMap<AccountFolder, SimpleEmailMessageProvider> providerMap = new HashMap<AccountFolder, SimpleEmailMessageProvider>();
  private static HashMap<EmailAccount, Store> storeConnections;

//  /**
//   * Constructs a SimpleEmailMessageProvider object.
//   *
//   * @param account the instance to connect with
//   * @param attachmentFolder path to folder where to save attachments
//   */
//  public SimpleEmailMessageProvider(EmailAccount account, String attachmentFolder) {
//    this.account = account;
//    this.attachmentFolder = attachmentFolder;
//    this.accountFolder = new AccountFolder(account, "Inbox");
//  }

  public static synchronized SimpleEmailMessageProvider getInstance(EmailAccount a) {
    return getInstance(a, "Inbox");
  }

  public static synchronized SimpleEmailMessageProvider getInstance(EmailAccount a, String folder) {
    AccountFolder af = new AccountFolder(a, folder);
    if (providerMap.containsKey(af) && providerMap.get(af) != null) {
      return providerMap.get(af);
    } else {
      SimpleEmailMessageProvider semp = new SimpleEmailMessageProvider(a);
      providerMap.put(af, semp);
      return semp;
    }
  }

  /**
   * Constructs a SimpleEmailMessageProvider object.
   * 
   * @param account the instance to connect with
   */
  private SimpleEmailMessageProvider(EmailAccount account) {
    this.account = account;
    this.accountFolder = new AccountFolder(account, "Inbox");
  }
  
  public Account getAccount() {
    return account;
  }
  
  /**
   * Sets properties for the given object.
   * 
   * @param props the property to set
   */
  protected void setProperties(Properties props) {
    System.setProperty("java.net.preferIPv4Stack", "true");
    
    if (this.account.isSsl()) {
      Security.setProperty("ssl.SocketFactory.provider", MySSLSocketFactory.class.getCanonicalName());
      props.setProperty("mail.imap.port", "993");
      props.setProperty("mail.smtp.port", "465");
      props.put("mail.imap.socketFactory.fallback", "false");
      props.setProperty("mail.store.protocol", "imaps");
      props.put("mail.imaps.fetchsize", "819200");
    } else {
      props.put("mail.imaps.ssl.checkserveridentity", "false");
      props.put("mail.imaps.ssl.trust", "*");
      props.setProperty("mail.imap.port", "143");
      props.setProperty("mail.smtp.port", "25");
      props.setProperty("mail.store.protocol", "imap");
      props.put("mail.imap.fetchsize", "819200");
    }
    // TODO: some accounts, like citromail does not times out
//    props.put("mail.imaps.connectiontimeout", 1000);
//    props.put("mail.imap.connectiontimeout", 1000);
//    props.put("mail.imaps.connectionpooltimeout", 1000);
//    props.put("mail.imap.connectionpooltimeout", 1000);
//    props.put("mail.imaps.onTimeout", 1000);
//    props.put("mail.imap.onTimeout", 1000);
    
//    props.put("mail.smtp.onTimeout", 5000);
  }
  
  private Store getStore(EmailAccount account) throws MessagingException {
    Store store = null;
    if (storeConnections == null) {
//      Log.d("rgai", "CREATING STORE CONTAINER");
      storeConnections = new HashMap<EmailAccount, Store>();
    } else {
      if (storeConnections.containsKey(account)) {
        store = storeConnections.get(account);
//        Log.d("rgai", "STORE EXISTS");
      }
    }
    
    if (store == null || !store.isConnected()) {
//      Log.d("rgai", "CREATING STORE || reconnection store");
      store = getStore();
      storeConnections.put(account, store);
    }
    
    return store;
  }

  private synchronized IMAPFolder getFolder() throws MessagingException {
    return getFolder(null);
  }

  private synchronized IMAPFolder getFolder(IMAPFolder folder) throws MessagingException {
    return getFolder(folder, false);
  }
  
  private synchronized IMAPFolder getFolder(IMAPFolder folder, boolean attachListener) throws MessagingException {
//    Log.d("rgai", "getFolder");
    

    if (folder == null) {
//      Log.d("rgai", "CREATING imapFolder || reconnecting imapFolder");
      Store store = getStore(account);
      if (store != null) {
        if (store.isConnected()) {
          try {
            folder = (IMAPFolder) store.getFolder(accountFolder.folder);
          } catch (IllegalStateException e) {
            return null;
          }
        } else {
          return null;
        }
        
        if (folder != null && !folder.isOpen()) {
          folder.open(Folder.READ_ONLY);
        }
        if (attachListener && messageListener != null) {
//          Log.d("rgai", "adding event listeners....");
          folder.addMessageCountListener(new MessageCountListener() {
            public void messagesAdded(MessageCountEvent mce) {
              messageListener.messageAdded(mce.getMessages());
            }
            public void messagesRemoved(MessageCountEvent mce) {
              messageListener.messageRemoved(mce.getMessages());
            }
          });
        }
      }
    } else {
      Log.d("rgai", "IMAPFolder OK, already opened");
    }
    
    if (folder != null && !folder.isOpen()) {
      folder.open(Folder.READ_ONLY);
    }
    
    return folder;
  }
  
  /**
   * Connects to imap, returns a store.
   * 
   * @return the store where, where you can read the emails
   * @throws NoSuchProviderException
   * @throws MessagingException
   * @throws AuthenticationFailedException 
   */
  private synchronized Store getStore() throws NoSuchProviderException, MessagingException, AuthenticationFailedException {
    Properties props = System.getProperties();
    this.setProperties(props);
    Session session = Session.getDefaultInstance(props, null);
    Store store;
    if (this.account.isSsl()) {
      store = session.getStore("imaps");
    } else {
      store = session.getStore("imap");
    }
    Log.d("rgai3", "connecting with account: " + account);
    store.connect(account.getImapAddress(), account.getEmail(), account.getPassword());
    
    return store;
  }
  

  @Override
  public MessageListResult getMessageList(int offset, int limit, TreeSet<MessageListElement> loadedMessages,
                                          boolean isNewMessageArrivedRequest)
          throws CertPathValidatorException, SSLHandshakeException, ConnectException, NoSuchProviderException,
          UnknownHostException, IOException, MessagingException, AuthenticationFailedException {

    return getMessageList(offset, limit, loadedMessages, 20, isNewMessageArrivedRequest);
  }

  private void putTime(HashMap<String, Long> map, String s, long startTime) {
    if (!map.containsKey(s)) {
      map.put(s, 0l);
    }
    map.put(s, map.get(s) + (System.currentTimeMillis() - startTime));
  }

  @Override
  public MessageListResult getMessageList(int offset, int limit, TreeSet<MessageListElement> loadedMessages,
                                          int snippetMaxLength, boolean isNewMessageArrivedRequest)
          throws CertPathValidatorException, SSLHandshakeException, ConnectException, NoSuchProviderException,
          UnknownHostException, IOException, MessagingException, AuthenticationFailedException {

    long s1 = System.currentTimeMillis();
    HashMap<String, Long> times = new HashMap<>();
    try {
      List<MessageListElement> emails = new LinkedList<>();

      long s3 = System.currentTimeMillis();
      IMAPFolder imapFolder = null;
      try {
        imapFolder = (IMAPFolder) getStore(account).getFolder("Inbox");
      } catch (AuthenticationFailedException e) {
        Log.d("kojak", "", e);
      }
      if (imapFolder == null) {
        return new MessageListResult(emails, MessageListResult.ResultType.ERROR);
      }
      putTime(times, "getStoreGetFolder", s3);
      
      long s4 = System.currentTimeMillis();
      imapFolder.open(Folder.READ_ONLY);
      UIDFolder uidImapFolder = imapFolder;


      int messageCount = imapFolder.getMessageCount();
      putTime(times, "openFolderGetMsgCount", s4);
      long s5 = System.currentTimeMillis();


      long nextUID = getNextUID(imapFolder, uidImapFolder, messageCount);
      putTime(times, "getNextUID", s5);
      if (offset == 0 && !hasNewMail(nextUID, messageCount) && !loadedMessages.isEmpty()) {
        Log.d("rgai", "NO NEW MAIL AND NO DELETION AND VALID KEY");
        if (MainActivity.isMainActivityVisible()) {
          return getFlagChangesOfMessages(loadedMessages);
        } else {
          return new MessageListResult(emails, MessageListResult.ResultType.NO_CHANGE);
        }
      } else {
        Log.d("rgai", "NEW MAIL OR DELETION OR VALIDITY KEY");
      }


      int start = Math.max(1, messageCount - limit - offset + 1);
      int end = start + limit > messageCount ? messageCount : start + limit;

      Message[] messages;
      List<MessageListElement> flagMessages = null;
      if (offset == 0 && !loadedMessages.isEmpty()) {
        long s6 = System.currentTimeMillis();
        messages = uidImapFolder.getMessagesByUID(getSmallestUID(loadedMessages), UIDFolder.LASTUID);
        putTime(times, "getMessagesByUid", s6);

        s6 = System.currentTimeMillis();
        MessageListResult mlr = getFlagChangesOfMessages(loadedMessages);
        putTime(times, "getFlagChanges", s6);

        s6 = System.currentTimeMillis();
        flagMessages = mlr.getMessages();
        putTime(times, "getMessages", s6);
      } else {
        messages = imapFolder.getMessages(start, end);
      }
      long s2 = System.currentTimeMillis();
      for (int i = messages.length - 1; i >= 0; i--) {

        long s7 = System.currentTimeMillis();
        Message m = messages[i];
        long uid = uidImapFolder.getUID(m);
        putTime(times, "getUID", s7);


        if (flagMessages != null) {
          boolean found = false;
          for (MessageListElement flagMessage : flagMessages) {
            if (flagMessage.getId().equals(Long.toString(uid))) {
              emails.add(flagMessage);
              found = true;
              break;
            }
          }
          if (found) {
//            Log.d("rgai", "continue here...just update flag...");
            continue;
          }
        }

        boolean seen = true;
        List<Person> allRecipients = null;
        if (isNewMessageArrivedRequest) {
          Flags flags = m.getFlags();
          seen = flags.contains(Flags.Flag.SEEN);

          Address[] recAddresses = m.getAllRecipients();
          allRecipients = getAllRecipients(recAddresses);
        }



        s7 = System.currentTimeMillis();
        Date date = m.getSentDate();
        putTime(times, "getSentDate", s7);

        // Skipping email from listing, because it is a spam probably,
        // ...at least citromail does not give any information about the email in some cases,
        // and in web browsing it displays an "x" sign before the title, which may indicate
        // that this is a spam

        s7 = System.currentTimeMillis();
        Person fromPerson = getSenderPersonObject(m);
        putTime(times, "getFrom", s7);
        if (fromPerson == null) {
          // skipping email
        } else {

          MessageListElement testerElement = new MessageListElement(-1, Long.toString(uid), seen, fromPerson, date,
                  account, Type.EMAIL, true);

          MessageListElement loadedMsg = MessageProvider.Helper.isMessageLoaded(loadedMessages, testerElement);
          if (loadedMsg != null) {
            emails.add(testerElement);
            continue;
          }/* else {
            System.out.println("adding: new Date("+ date.getTime() +"l), \""+ fromEmail +"\")");
          }*/


          EmailContent content = null;
          if (isNewMessageArrivedRequest) {
            content = getMessageContent(m);
          }


          s7 = System.currentTimeMillis();
          String subject = m.getSubject();
          putTime(times, "getSubject", s7);
          if (subject != null) {
            subject = prepareMimeFieldToDecode(subject);
            try {
              subject = MimeUtility.decodeText(subject);
            } catch (java.io.UnsupportedEncodingException ex) {
              Log.d("rgai", "", ex);
            }
          } else {
//            try {
//              Source source = new Source(content.getContent().getContent());
//              String decoded = source.getRenderer().toString();
////              String snippet = decoded.substring(0, Math.min(snippetMaxLength, decoded.length()));
//              String snippet = decoded;
//              subject = snippet;
//            } catch (StackOverflowError so) {
//              Log.d("rgai", "", so);
//            }
//            if (subject == null) {
              subject = "<No subject>";
//            }
          }

          int attachSize = 0;
          FullSimpleMessage fsm = null;

          if (isNewMessageArrivedRequest) {
            attachSize = content.getAttachmentList() != null ? content.getAttachmentList().size() : 0;
            fsm = new FullSimpleMessage(-1, uid + "", subject, content.getContent(), date, fromPerson,
                    false, Type.EMAIL, content.getAttachmentList());
          }

          boolean splittedMessage = !isNewMessageArrivedRequest;
          MessageListElement mle = new MessageListElement(splittedMessage, -1, String.valueOf(uid),
                  seen, subject, "", attachSize, fromPerson, null, date, account, Type.EMAIL, false);

          mle.setFullMessage(fsm);
          mle.setRecipients(allRecipients);
          emails.add(mle);
        }
      }
      Log.d("yako", "total for cycle time -> " + (System.currentTimeMillis() - s2));


      imapFolder.close(false);
      Log.d("yako", "total time -> " + (System.currentTimeMillis() - s1));

      long sum = 0;
      for (Long e : times.values()) {
        sum += e;
      }
      Log.d("yako", "total for cycle time based on times map-> " + sum);
      for (Map.Entry<String, Long> e : times.entrySet()) {
        Formatter f = new Formatter().format("%.2f", (e.getValue() * 100.0 / sum));
        Log.d("yako", "\t" + e.getKey() + " -> " + e.getValue() + " -> "+ f.toString() +" %");
      }

      return new MessageListResult(emails, MessageListResult.ResultType.CHANGED);
    } catch (AuthenticationFailedException ex) {
      throw ex;
    } catch (NoSuchProviderException ex) {
      throw ex;
    } catch (MessagingException ex) {
      throw ex;
    } finally {
      
    }
  }

  private static List<Person> getAllRecipients(Address[] recs) {
    if (recs == null) {
      return null;
    }

    List<Person> recipients = new ArrayList<>(recs.length);
    for (Address a : recs) {
      recipients.add(getPersonFromAddress(a));
    }

    return recipients;
  }

  @Override
  public MessageListResult loadDataToMessages(TreeMap<String, MessageListElement> messagesToLoad) throws CertPathValidatorException, IOException, MessagingException {

    long s1 = System.currentTimeMillis();
    HashMap<String, Long> times = new HashMap<>();
    try {
      List<MessageListElement> emails = new LinkedList<>();

      long s3 = System.currentTimeMillis();
      IMAPFolder imapFolder = (IMAPFolder)getStore(account).getFolder("Inbox");
      putTime(times, "getStoreGetFolder", s3);
      if (imapFolder == null) {
        return new MessageListResult(emails, MessageListResult.ResultType.ERROR);
      }

      imapFolder.open(Folder.READ_ONLY);
      UIDFolder uidImapFolder = imapFolder;

      String uidsString[] = messagesToLoad.keySet().toArray(new String[messagesToLoad.keySet().size()]);
      long uids[] = new long[uidsString.length];
      for (int i = 0; i < uidsString.length; i++) {
        uids[i] = Long.parseLong(uidsString[i]);
      }

      Message[] messages = uidImapFolder.getMessagesByUID(uids);

      for (int i = messages.length - 1; i >= 0; i--) {

        Message m = messages[i];
        if (m == null) continue;

        long uid = uidImapFolder.getUID(m);

        MessageListElement mle = messagesToLoad.get(String.valueOf(uid));

        Address[] recAddresses = m.getAllRecipients();
        List<Person> allRecipients = getAllRecipients(recAddresses);

        Flags flags = m.getFlags();
        boolean seen = flags.contains(Flags.Flag.SEEN);
        EmailContent content = getMessageContent(m);
        int attachSize = content.getAttachmentList() != null ? content.getAttachmentList().size() : 0;

        mle.setSeen(seen);
        mle.setAttachmentCount(attachSize);
        mle.setRecipients(allRecipients);

        FullSimpleMessage fsm = new FullSimpleMessage(-1, String.valueOf(uid), mle.getTitle(),
                content.getContent(), mle.getDate(), mle.getFrom(), false, Type.EMAIL, content.getAttachmentList());
        mle.setFullMessage(fsm);
        emails.add(mle);
      }


      imapFolder.close(false);
      Log.d("yako", "total time -> " + (System.currentTimeMillis() - s1));

      long sum = 0;
      for (Long e : times.values()) {
        sum += e;
      }
      Log.d("yako", "total for cycle time based on times map-> " + sum);
      for (Map.Entry<String, Long> e : times.entrySet()) {
        Formatter f = new Formatter().format("%.2f", (e.getValue() * 100.0 / sum));
        Log.d("yako", "\t" + e.getKey() + " -> " + e.getValue() + " -> "+ f.toString() +" %");
      }

      return new MessageListResult(emails, MessageListResult.ResultType.SPLITTED_RESULT_SECOND_PART);
    } catch (AuthenticationFailedException ex) {
      throw ex;
    }
  }
  
  private long getNextUID(IMAPFolder folder, UIDFolder uidImapFolder, int messageCount) throws MessagingException {
    if (account.getAccountType().equals(MessageProvider.Type.GMAIL)) {
      return folder.getUIDNext();
    } else {
      int start = Math.max(1, messageCount);
      int end = start;
      Message messages[] = folder.getMessages(start, end);
      return uidImapFolder.getUID(messages[0]) + 1;
    }
  }
  
  private MessageListResult getFlagChangesOfMessages(TreeSet<MessageListElement> loadedMessages) throws MessagingException {
    
    List<MessageListElement> emails = new LinkedList<MessageListElement>();
    Store store = getStore(account);
    
    if (store == null || !store.isConnected()) {
      return new MessageListResult(emails, MessageListResult.ResultType.ERROR);
    }
    IMAPFolder imapFolder = (IMAPFolder)store.getFolder(accountFolder.folder);
    imapFolder.open(Folder.READ_ONLY);
    UIDFolder uidFolder = imapFolder;
    
    long smallestUID = getSmallestUID(loadedMessages);

    Message messages[] = imapFolder.getMessagesByUID(smallestUID, UIDFolder.LASTUID);

    FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
    Message unseenMsgs[] = imapFolder.search(ft, messages);

    addMessagesToListAs(uidFolder, emails, unseenMsgs, loadedMessages, false);

    // searching for seen messages
    List<Message> seenMessages = new ArrayList<Message>(Arrays.asList(messages));
    for (int k = 0; k < unseenMsgs.length; k++) {
      String unseenUid = Long.toString(uidFolder.getUID(unseenMsgs[k]));
      for (int l = 0; l < seenMessages.size(); ) {
        String seenUid = Long.toString(uidFolder.getUID(seenMessages.get(l)));
        if (seenUid.equals(unseenUid)) {
          seenMessages.remove(l);
        } else {
          l++;
        }
      }
    }

    addMessagesToListAs(uidFolder, emails, seenMessages.toArray(new Message[seenMessages.size()]), loadedMessages, true);

    imapFolder.close(false);

    return new MessageListResult(emails, MessageListResult.ResultType.FLAG_CHANGE);
  }


  private long getSmallestUID(TreeSet<MessageListElement> messages) {
    long smallestUID = Long.MAX_VALUE;
    for (MessageListElement mle : messages) {
      long mleUID = Long.parseLong(mle.getId());
      if (mleUID < smallestUID) {
        smallestUID = mleUID;
      }
    }
    return smallestUID;
  }


  private void addMessagesToListAs(UIDFolder folder, List<MessageListElement> emails, Message[] messages,
                                   TreeSet<MessageListElement> loadedMessages, boolean seen) throws MessagingException {
    for (int i = 0; i < messages.length; i++) {
      Message m = messages[i];
      if (m != null) {
        long uid = folder.getUID(m);
        MessageListElement testerElement = new MessageListElement(-1, Long.toString(uid), seen, null, null, account,
                Type.EMAIL, true);
        MessageListElement loadedMsg = MessageProvider.Helper.isMessageLoaded(loadedMessages, testerElement);
        if (loadedMsg != null) {
          testerElement.setDate(loadedMsg.getDate());
          emails.add(testerElement);
        }
      }
    }
  }
  
  private boolean hasNewMail(long nextUID, int messageCount) throws MessagingException {
    String newKey = nextUID+"_"+messageCount;
    Log.d("rgai", "messageLoadKey: " + newKey);
    
    if (newKey.equals(validityKey)) {
      return false;
    } else {
      validityKey = newKey;
      return true;
    }
    
  }


  private Person getSenderPersonObject(Message m) throws MessagingException {
    if (m.getFrom() != null) {
      return getPersonFromAddress(m.getFrom()[0]);
    } else {
      return null;
    }
  }

  /**
   * Extracts the person information from Address object.
   *
   * @param address the raw input Address object from the email
   * @return Person object
   */
  private static Person getPersonFromAddress(Address address) {

    if (address == null) {
      return null;
    }

    String from = address.toString();
    from = prepareMimeFieldToDecode(from);

    if (from != null) {
      try {
        from = MimeUtility.decodeText(from);
      } catch (java.io.UnsupportedEncodingException ex) {
        Log.d("rgai", "", ex);
      }
    } else {
      return null;
    }

    String fromName;
    String fromEmail;
    String regex = "(.*)<([^<>]*)>";

    if (from.matches(regex)) {
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(from);
      matcher.find();
      fromName = matcher.group(1);
      fromEmail = matcher.group(2);
    } else {
      fromName = from;
      fromEmail = from;
    }
    return new Person(fromEmail.trim(), fromName.trim(), MessageProvider.Type.EMAIL);

  }


  /**
   * Replaces the "x-unknown" encoding type with "iso-8859-2" to be able to decode the mime
   * string and removes the quotation marks if there is any.
   * 
   * @param text a mime encoded raw text
   * @return a changed raw text with corrected encoding
   */
  private static String prepareMimeFieldToDecode(String text) {
    text = text.trim();
    if (text.indexOf("=?x-unknown?") != -1) {
      text = text.replace("x-unknown", "iso-8859-2");
    }
    int quotStart = text.indexOf("\"");
    int quotEnd = text.lastIndexOf("\"");
    if (quotStart != -1 && quotStart == 0 && quotStart != quotEnd) {
      StringBuilder sb = new StringBuilder(text);
      // replacing the starting quot
      sb.replace(0, 1, "");
      sb.replace(quotEnd - 1, quotEnd, "");
      text = sb.toString();
    }
    return text;
  }
  
  /**
   * Extracts the content of an email message.
   * 
   * Since the content's MIME type varies, the extraction has different ways.
   * 
   * @param fullMessage the full message content
   * @return the extracted content of an email message
   * @throws MessagingException
   * @throws IOException 
   */
  protected EmailContent getMessageContent(Message fullMessage) throws MessagingException, IOException {
//    return new EmailContent(new HtmlContent("test", HtmlContent.ContentType.TEXT), null);
//    System.setProperty("javax.activation.debug", "true");
    HtmlContent content = new HtmlContent();
    List<Attachment> attachments = null;

    Object msg;
    try {
      msg = fullMessage.getContent();
    } catch (UnsupportedEncodingException e) {
      Log.d("yako", "", e);
      HtmlContent hc = new HtmlContent("<span style='color: red;'>Unsupported content encoding</span>",
              HtmlContent.ContentType.TEXT_HTML);
      return new EmailContent(hc, null);
    }

    if (fullMessage.isMimeType("text/*")) {

      if (fullMessage.isMimeType("text/html")) {
        content = new HtmlContent((String) msg, HtmlContent.ContentType.TEXT_HTML);
      } else if (fullMessage.isMimeType("text/plain")) {
        content = new HtmlContent((String) msg, HtmlContent.ContentType.TEXT_PLAIN);
      } else {
        content = new HtmlContent((String) msg, HtmlContent.ContentType.TEXT);
      }

    } else if (fullMessage.isMimeType("multipart/*")) {

      if (msg instanceof Multipart) {
        Multipart mp = (Multipart) msg;
        content = getContentOfMultipartMessage(mp, 0);
        attachments = getAttachmentsOfMultipartMessage(mp, true, 0);

      }

    } else if (msg instanceof IMAPInputStream) {
      IMAPInputStream imapIs = (IMAPInputStream) msg;
      InputStream dis = MimeUtility.decode(imapIs, "binary");

      BufferedReader br = new BufferedReader(new InputStreamReader(dis));
      String line;
      while ((line = br.readLine()) != null) {
        content.getContent().append(line);
      }
    } else {
      System.out.println("Nem tudom 2");
    }
    return new EmailContent(content, attachments);
  }
  
  /**
   * Returns an attachment list for a given Mime Multipart object.
   * 
   * @param mp the input Mime part of the email's content
   * @param level debug variable
   * @return list of files where the attachments are saved
   * @throws MessagingException
   * @throws IOException 
   */
  private List<Attachment> getAttachmentsOfMultipartMessage(Multipart mp, boolean onlyInfo, int level) throws MessagingException, IOException {
    List<Attachment> files = new LinkedList<Attachment>();

    for (int j = 0; j < mp.getCount(); j++) {
      
      Part bp = mp.getBodyPart(j);
      String contentType = bp.getContentType().toLowerCase();
      
      if (!Part.ATTACHMENT.equalsIgnoreCase(bp.getDisposition())) {
        if (contentType.indexOf("multipart/") != -1) {
          files.addAll(getAttachmentsOfMultipartMessage((Multipart)(bp.getContent()), onlyInfo, level + 1));
        }
        continue;
      }
      String fName = bp.getFileName() == null ? "noname" : bp.getFileName();
      files.add(new Attachment(MimeUtility.decodeText(fName), bp.getSize()));
      if (!onlyInfo) {
        InputStream is = bp.getInputStream();
        File f = new File(this.attachmentFolder + bp.getFileName());
        FileOutputStream fos = new FileOutputStream(f);
        byte[] buf = new byte[4096];
        int bytesRead;
        while((bytesRead = is.read(buf))!=-1) {
            fos.write(buf, 0, bytesRead);
        }
        fos.close();
      }
      
    }
    
    return files;
  }

  
  
  /**
   * Returns the pure String content of a Mime Multipart message.
   * 
   * @param mp the input Mime part of the email's content
   * @param level debug variable
   * @return the pure String content of a Mime Multipart message
   * @throws MessagingException
   * @throws IOException 
   */
  private HtmlContent getContentOfMultipartMessage(Multipart mp, int level) throws MessagingException, IOException {
    HtmlContent content = new HtmlContent();
    
    boolean htmlFound = false;
    for (int j = 0; j < mp.getCount(); j++) {
      
      Part bp = mp.getBodyPart(j);
      String contentType = bp.getContentType().toLowerCase();
      // Give some initial date to content, to not return with null, so we can debug later
      if (content.getContent().length() == 0) {
        content = new HtmlContent("<this message should not occure...>", HtmlContent.ContentType.TEXT_PLAIN);
      }
      
      if (contentType.indexOf("multipart/") != -1) {
        content = getContentOfMultipartMessage((Multipart)(bp.getContent()), level + 1);
      } else if (contentType.indexOf("text/plain") != -1) {
        if (!htmlFound) {
          try {
            content = new HtmlContent(bp.getContent().toString(), HtmlContent.ContentType.TEXT_PLAIN);
          } catch (UnsupportedEncodingException ex) {
            Log.d("rgai", "", ex);
          }
        }
      } else if (contentType.indexOf("text/html") != -1) {
        content = new HtmlContent(bp.getContent().toString(), HtmlContent.ContentType.TEXT_HTML);
        break;
      }
    }
    
    return content;
  }
  


  @Override
  public FullMessage getMessage(String id) throws NoSuchProviderException, MessagingException, IOException {

    Folder  queryFolder = getFolder();

    if (queryFolder == null) return null;
    
    EmailContent content;
    
    Message ms = queryFolder.getMessage(Integer.parseInt(id));
//    ms.s
    
    List<Person> to = new LinkedList<>();
    
    Address[] addr = ms.getAllRecipients();
    for (Address a : addr) {
      to.add(getPersonFromAddress(a));
    }
    content = getMessageContent(ms);
    
    Person from = getPersonFromAddress(ms.getFrom()[0]);
    
    String subject = ms.getSubject();
    
    Date date = ms.getSentDate();

    queryFolder.close(true);
    
    return new FullSimpleMessage(-1, id, subject, content.getContent(), date, from, false, MessageProvider.Type.EMAIL, null);
    
  }
  
  public byte[] getAttachmentOfMessage(String messageId,
          String attachmentId, AttachmentProgressUpdate onProgressUpdate)
          throws NoSuchProviderException, MessagingException, IOException {
    
    IMAPFolder folder = (IMAPFolder)getStore().getFolder("Inbox");
    UIDFolder uidFolder = (UIDFolder)folder;
    folder.open(Folder.READ_ONLY);
    
    Message ms = uidFolder.getMessageByUID(Long.parseLong(messageId));

    byte[] data = getMessageAttachment(ms, attachmentId, onProgressUpdate);
    
    folder.close(false);
    
    return data;
    
  }
  

  private byte[] getMessageAttachment(Message message, String attachmentId,
          AttachmentProgressUpdate onProgressUpdate) throws IOException, MessagingException {
    Object msg = message.getContent();
    ByteArrayOutputStream buffer = null;
    if (msg instanceof Multipart) {
      Multipart mp = (Multipart) msg;
      for (int j = 0; j < mp.getCount(); j++) {
        Part bp = mp.getBodyPart(j);
        if (!Part.ATTACHMENT.equalsIgnoreCase(bp.getDisposition())) {
          continue;
        }
        if (MimeUtility.decodeText(bp.getFileName()).equals(attachmentId)) {
          
          InputStream is = bp.getInputStream();
          buffer = new ByteArrayOutputStream();
          int nRead;
          byte[] data = new byte[65536];
          int fullSize = bp.getSize();
          while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
            if (onProgressUpdate != null) {
              onProgressUpdate.onProgressUpdate(buffer.size() * 100 / fullSize);
            }
          }
          buffer.flush();
          is.close();
          return buffer.toByteArray();
        }
      }
    }
    return null;
    
  }

  @Override
  public void sendMessage(Context context, SentMessageBroadcastDescriptor sentMessageData,
                          Set<? extends MessageRecipient> to, String content, String subject) {
    
    Properties props = System.getProperties();
    this.setProperties(props);
    props.put("mail.smtps.host", account.getSmtpAddress());
    props.put("mail.smtps.auth","true");
    Session session = Session.getInstance(props, null);
    javax.mail.Message msg = new MimeMessage(session);
    
    boolean success = true;
    try {
      // FIXME: this is a VERY VERY UGRLY solution
      if (account.getImapAddress().equals("mail.inf.u-szeged.hu")) {
        msg.setFrom(new InternetAddress(account.getEmail() + "@inf.u-szeged.hu"));
      
      } else {
        msg.setFrom(new InternetAddress(account.getEmail()));
      }
    
    
      String addressList = getAddressList(to);

      msg.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(addressList));
      msg.setSubject(subject);
      msg.setText(content);
      msg.setHeader("Content-Type", "text/html; charset=UTF-8");
      msg.setSentDate(new Date());
      SMTPTransport t;
      if (account.isSsl()) {
        t = (SMTPTransport)session.getTransport("smtps");
      } else {
        t = (SMTPTransport)session.getTransport("smtp");
      }
      t.connect(account.getSmtpAddress(), account.getEmail(), account.getPassword());
      t.sendMessage(msg, msg.getAllRecipients());
      t.close();
    } catch (MessagingException ex) {
      Log.d("rgai", "", ex);
      success = false;
    }

    MessageProvider.Helper.sendMessageSentBroadcast(context, sentMessageData,
            success ? MessageSentBroadcastReceiver.MESSAGE_SENT_SUCCESS : MessageSentBroadcastReceiver.MESSAGE_SENT_FAILED);
    
  }
  
  private static String getAddressList(Set<? extends MessageRecipient> to) {
    StringBuilder sb = new StringBuilder();
    for (MessageRecipient p : to) {
      EmailMessageRecipient er = (EmailMessageRecipient)p;
      if (er.getEmail().length() > 0) {
        if (sb.toString().length() > 0) {
          sb.append(", ");
        }
        if (er.getName() != null && er.getName().length() > 0) {
          String name = er.getName();
          try {
            name = MimeUtility.encodeText(name);
          } catch (UnsupportedEncodingException e) {
            Log.d("yako", "", e);
          }
          sb.append(name);
        }
        sb.append("<").append(er.getEmail()).append(">");
      }
    }
    
    return sb.toString();
  }

  public void markMessagesAsRead(String[] ids, boolean seen) throws NoSuchProviderException, MessagingException, IOException {
    IMAPFolder folder = (IMAPFolder)getStore().getFolder("Inbox");
    folder.open(Folder.READ_WRITE);
    UIDFolder uidFolder = folder;
    
    Message[] msgs;
    

    long[] uids = new long[ids.length];
    int i = 0;
    for (String s : ids) {
      try {
        uids[i++] = Long.parseLong(s);
      } catch (Exception ex) {
        Log.d("rgai", "", ex);
      }
    }
    // TODO: if instance not support UID, then use simple id
    msgs = uidFolder.getMessagesByUID(uids);

    folder.setFlags(msgs, new Flags(Flags.Flag.SEEN), seen);

    folder.close(false);
  }
  
  @Override
  public void markMessageAsRead(String id, boolean seen) throws NoSuchProviderException, MessagingException, IOException {
    
    IMAPFolder folder = (IMAPFolder)getStore().getFolder("Inbox");
    folder.open(Folder.READ_WRITE);
    UIDFolder uidFolder = (UIDFolder)folder;
    
    Message ms = uidFolder.getMessageByUID(Long.parseLong(id));

    if (ms != null) {
      ms.setFlag(Flags.Flag.SEEN, seen);
    }
    folder.close(false);
  }

  public boolean canBroadcastOnNewMessage() {
    return account.getAccountType().equals(MessageProvider.Type.GMAIL)
            || account.getImapAddress().equals(InfEmailSettingActivity.IMAP_ADDRESS);
  }

  public boolean isConnectionAlive() {
    if (idleThread != null) {
      
      if (idleThread.folderIdle.isRunning()) {
        if (idleThread.insertTime + Settings.ESTABLISHED_CONNECTION_TIMEOUT * 1000l < System.currentTimeMillis()) {
          return false;
        } else {
          return true;
        }
      } else {
        return false;
      }
    } else {
      return false;
    }
  }
  
  private void initMessageListener(final Context context) {
    if (messageListener == null) {    
      messageListener = new MessageCallback() {
        public void messageAdded(Message[] messages) {
          Intent service = new Intent(context, MainScheduler.class);
          service.setAction(Context.ALARM_SERVICE);
          
          MainServiceExtraParams eParams = new MainServiceExtraParams();
          eParams.setOnNewMessageArrived(true);
          eParams.addAccount(account);
          eParams.setQueryOffset(0);
          eParams.setQueryLimit(messages.length);
          eParams.setForceQuery(true);
          service.putExtra(IntentStrings.Params.EXTRA_PARAMS, eParams);
          
          context.sendBroadcast(service);
        }

        public void messageRemoved(Message[] messages) {
          Log.d("rgai", "message removed...." + messages.length);
          Intent service = new Intent(context, MainScheduler.class);
          service.setAction(Context.ALARM_SERVICE);
          
          MainServiceExtraParams eParams = new MainServiceExtraParams();
          eParams.addAccount(account);
          eParams.setForceQuery(true);
          eParams.setMessagesRemovedAtServer(true);
          service.putExtra(IntentStrings.Params.EXTRA_PARAMS, eParams);
          
          context.sendBroadcast(service);
        }
      };
    }
  }
  
  public synchronized void establishConnection(Context cont) {
    if (context == null) {
      context = cont;
    }
    initMessageListener(context);
    if (!isConnectionAlive()) {
      // dropping previous connection if any
      if (idleThread != null) {
        if (idleThread.folderIdle != null) {
          idleThread.folderIdle.forceStop();
        }
      }
      
//      Log.d("rgai", "Establishing connection: " + instance);
      try {
        if (idleFolder == null) {
          idleFolder = getFolder(idleFolder, true);
        }
//        IMAPFolder folder = getFolder(idleFolder, account, "Inbox", true);
        if (idleFolder == null) return;
        
        FolderIdle fi = new FolderIdle(idleFolder, this);
        Thread t = new Thread(fi);
        t.start();
//        if (idleThread == null) {
//          idleThread = new HashMap<AccountFolder, FolderIdleWithTimestamp>();
//        }
        idleThread = new FolderIdleWithTimestamp(fi, System.currentTimeMillis());
      } catch (MessagingException ex) {
        Log.d("rgai", "", ex);
      }
    } else {
//      Log.d("rgai", "No thanks, my connection is already alive: " + instance);
    }
  }
  
  public boolean canBroadcastOnMessageChange() {
    return false;
  }
  
  public void dropConnection(Context context) {
    Store store = null;
    if (storeConnections != null && storeConnections.containsKey(account)) {
      Log.d("rgai", "REMOVING FROM storeStack");
      store = storeConnections.get(account);
      storeConnections.remove(account);
    }

    ConnectionDropper dropper = new ConnectionDropper(idleThread, store);
    dropper.executeTask(context, null);
  }


  @Override
  public String toString() {
    return "SimpleEmailMessageProvider{" + "instance=" + account + '}';
  }


  public boolean isMessageDeletable() {
    return true;
  }

  @Override
  public boolean testConnection() throws MessagingException {
    getStore(account);
    return true;
  }


  @Override
  public MessageListResult getUIDListForMerge(String lowestStoredMessageUID) throws MessagingException {
    UIDFolder folder = getFolder();

    if (folder == null) {
      return null;
    }
    List<MessageListElement> mles = new LinkedList<MessageListElement>();
    Message[] messages = folder.getMessagesByUID(Long.parseLong(lowestStoredMessageUID), UIDFolder.LASTUID);
    for (int i = 0; i < messages.length; i++) {
      Message m = messages[i];
      if (m != null) {
        long uid = folder.getUID(m);
        MessageListElement testerElement = new MessageListElement(Long.toString(uid), account);
        mles.add(testerElement);
      }
    }
    return new MessageListResult(mles, MessageListResult.ResultType.MERGE_DELETE);
  }


  public void deleteMessage(String id) throws NoSuchProviderException, MessagingException, IOException {
    IMAPFolder folder = (IMAPFolder)getStore().getFolder("Inbox");
    folder.open(Folder.READ_WRITE);
    UIDFolder uidFolder = (UIDFolder)folder;
    
    Message ms = uidFolder.getMessageByUID(Long.parseLong(id));

    if (ms != null) {
      ms.setFlag(Flags.Flag.DELETED, true);
    }
    folder.close(true);
  }



  public interface AttachmentProgressUpdate {
    public void onProgressUpdate(int progress);
  }

  private static class AccountFolder {
    private EmailAccount account = null;
    private String folder = null;

    public AccountFolder(EmailAccount account, String folder) {
      this.account = account;
      this.folder = folder;
    }

    @Override
    public int hashCode() {
      int hash = 3;
      hash = 67 * hash + (this.account != null ? this.account.hashCode() : 0);
      hash = 67 * hash + (this.folder != null ? this.folder.hashCode() : 0);
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final AccountFolder other = (AccountFolder) obj;
      if (this.account != other.account && (this.account == null || !this.account.equals(other.account))) {
        return false;
      }
      if ((this.folder == null) ? (other.folder != null) : !this.folder.equals(other.folder)) {
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return "AccountFolder{" + "instance=" + account + ", folder=" + folder + '}';
    }
  }

  
  private class FolderIdle implements Runnable {

    volatile boolean mRunning = false;
    private IMAPFolder mFolder;
    private SimpleEmailMessageProvider mMessageProvider;
    private boolean forceStop = false;
    
    public FolderIdle(IMAPFolder folder, SimpleEmailMessageProvider messageProvider) {
      this.mFolder = folder;
      this.mMessageProvider = messageProvider;
      
    }
    
    public void run() {
      
      if (mFolder != null) {
        try {
          mRunning = true;
          if (!mFolder.isOpen()) {
            mFolder.open(Folder.READ_ONLY);
          }
          
          mFolder.idle();
        } catch (Exception ex) {
          Log.d("rgai", "", ex);
        } finally {
          mRunning = false;
          if (!forceStop) {
            mMessageProvider.establishConnection(null);
          }
        }
      }
      
    }
    
    public boolean isRunning() {
      return mRunning;
    }
    
    public void forceStop() {
      forceStop = true;
      if (mFolder.isOpen()) {
        try {
          mFolder.close(false);
        } catch (MessagingException ex) {
          Log.d("rgai", "", ex);
        }
      }
        idleFolder = null;
//      }
    }
  }
  
  private class ConnectionDropper extends TimeoutAsyncTask<Void, Void, Void> {

    private final FolderIdleWithTimestamp folderIdleWithTimestamp;
    private final Store store;
    
    public ConnectionDropper(FolderIdleWithTimestamp folderIdleThread, Store store) {
      super(null);
      this.folderIdleWithTimestamp = folderIdleThread;
      this.store = store;
    }
    
    @Override
    protected Void doInBackground(Void... params) {
      if (folderIdleWithTimestamp != null && folderIdleWithTimestamp.folderIdle != null) {
        folderIdleWithTimestamp.folderIdle.forceStop();
      }
      
      if (store != null) {
        try {
          store.close();
        } catch (MessagingException ex) {
          Log.d("rgai", "", ex);
        }
      }
      
      return null;
    }
    
  }
  
//  private class FolderNoop implements Runnable {
//
//    IMAPFolder folder;
//    
//    public FolderNoop(IMAPFolder folder) {
//      this.folder = folder;
//    }
//    
//    public void run() {
//      if (folder != null) { 
//        try {
//          Log.d("rgai", "send NOOP");
//          if (!folder.isOpen()) {
//            folder.open(Folder.READ_ONLY);
//          }
//          
//          folder.doCommand(new IMAPFolder.ProtocolCommand() {
//            public Object doCommand(IMAPProtocol imapp) throws ProtocolException {
//              imapp.simpleCommand("NOOP", null);
//              return null;
//            }
//          });
//        } catch (Exception ex) {
//          Log.d("rgai", "Eception when sending noop.");
//          Logger.getLogger(SimpleEmailMessageProvider.class.getName()).log(Level.SEVERE, null, ex);
//        }
//      }
//    }
//    
//  }
  
  private class FolderIdleWithTimestamp {
    private FolderIdle folderIdle;
    private long insertTime;

    public FolderIdleWithTimestamp(FolderIdle folderIdle, long insertTime) {
      this.folderIdle = folderIdle;
      this.insertTime = insertTime;
    }
    
  }
}
