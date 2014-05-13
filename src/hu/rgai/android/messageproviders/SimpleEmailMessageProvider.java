// TODO: if NO FRESH MAIL result comes, but then the amount of initial messages changed, then refresh pressed, then it works incorrect: not loads more message

package hu.rgai.android.messageproviders;

import android.content.Context;
import android.util.Log;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPInputStream;
import com.sun.mail.smtp.SMTPTransport;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.Attachment;
import hu.rgai.android.beens.EmailAccount;
import hu.rgai.android.beens.EmailContent;
import hu.rgai.android.beens.EmailMessageRecipient;
import hu.rgai.android.beens.FullMessage;
import hu.rgai.android.beens.FullSimpleMessage;
import hu.rgai.android.beens.HtmlContent;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.beens.MessageListResult;
import hu.rgai.android.beens.MessageRecipient;
import hu.rgai.android.beens.Person;
import hu.rgai.android.test.MainActivity;
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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.net.ssl.SSLHandshakeException;
import net.htmlparser.jericho.Source;

/**
 * Implements a simple email message providing via IMAP protocol.
 * 
 * This class (in theory) can handle all email account types which supports IMAP.
 * 
 * @author Tamas Kojedzinszky
 */
public class SimpleEmailMessageProvider implements MessageProvider {

  private EmailAccount account;
  private String attachmentFolder = "../files/";
  private AttachmentProgressUpdate progressUpdate = null;
  private static HashMap<EmailAccount, Store> connections;
  private static HashMap<AccountFolder, IMAPFolder> folders;
  
  // this map holds a unique state id for a query
  // String is a concatenation of nextMessageUID+messageCount
  // this string is unique for a state
  private static HashMap<EmailAccount, String> validityMap;

  /**
   * Constructs a SimpleEmailMessageProvider object.
   * 
   * @param account the account to connect with
   * @param attachmentFolder path to folder where to save attachments
   */
  public SimpleEmailMessageProvider(EmailAccount account, String attachmentFolder) {
    this.account = account;
    this.attachmentFolder = attachmentFolder;
  }
  
  /**
   * Constructs a SimpleEmailMessageProvider object.
   * 
   * @param account the account to connect with
   */
  public SimpleEmailMessageProvider(EmailAccount account) {
    this.account = account;
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
      Security.setProperty("ssl.SocketFactory.provider", "hu.rgai.android.messageproviders.socketfactory.MySSLSocketFactory");
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
  }
  
  private Store getStore(EmailAccount account) throws MessagingException {
    Store store = null;
    if (connections == null) {
//      Log.d("rgai", "CREATING STORE CONTAINER");
      connections = new HashMap<EmailAccount, Store>();
    } else {
      if (connections.containsKey(account)) {
        store = connections.get(account);
//        Log.d("rgai", "STORE EXISTS");
      }
    }
    
    if (store == null || !store.isConnected()) {
//      Log.d("rgai", "CREATING STORE || reconnection store");
      store = getStore();
      connections.put(account, store);
    }
    
    return store;
  }
  
  private IMAPFolder getFolder(EmailAccount account, String folder) throws MessagingException {
    IMAPFolder imapFolder = null;
    AccountFolder accFolder = new AccountFolder(account, folder);
    if (folders == null) {
//      Log.d("rgai", "CREATING FOLDER CONTAINER");
      folders = new HashMap<AccountFolder, IMAPFolder>();
    } else {
      if (folders.containsKey(accFolder)) {
        imapFolder = folders.get(accFolder);
//        Log.d("rgai", "FOLDER EXISTS");
      }
    }
    
    if (imapFolder == null) {
//      Log.d("rgai", "CREATING imapFolder || reconnecting imapFolder");
      Store store = getStore(account);
      if (store != null) {
        imapFolder = (IMAPFolder)store.getFolder(folder);
        folders.put(accFolder, imapFolder);
      }
    } else {
//      Log.d("rgai", "IMAPFolder OK, already opened");
    }
    
    if (imapFolder != null && !imapFolder.isOpen()) {
      imapFolder.open(Folder.READ_ONLY);
    }
    
    return imapFolder;
  }
  
  
  
  /**
   * Connects to imap, returns a store.
   * 
   * @return the store where, where you can read the emails
   * @throws NoSuchProviderException
   * @throws MessagingException
   * @throws AuthenticationFailedException 
   */
  private Store getStore() throws NoSuchProviderException, MessagingException, AuthenticationFailedException {
    Properties props = System.getProperties();
    this.setProperties(props);
    Session session = Session.getDefaultInstance(props, null);
    Store store;
    if (this.account.isSsl()) {
      store = session.getStore("imaps");
    } else {
      store = session.getStore("imap");
    }
    store.connect(account.getImapAddress(), account.getEmail(), account.getPassword());
    
    
    return store;
  }
  
  public void setAttachmentProgressUpdateListener(AttachmentProgressUpdate progressUpdate) {
    this.progressUpdate = progressUpdate;
  }
  
  @Override
  public MessageListResult getMessageList(int offset, int limit, TreeSet<MessageListElement> loadedMessages) throws CertPathValidatorException, SSLHandshakeException,
          ConnectException, NoSuchProviderException, UnknownHostException, IOException, MessagingException, AuthenticationFailedException {
    return getMessageList(offset, limit, loadedMessages, 20);
  }
  
  @Override
  public MessageListResult getMessageList(int offset, int limit, TreeSet<MessageListElement> loadedMessages, int snippetMaxLength)
          throws CertPathValidatorException, SSLHandshakeException, ConnectException,
          NoSuchProviderException, UnknownHostException, IOException, MessagingException,
          AuthenticationFailedException {
    
    List<MessageListElement> emails = new LinkedList<MessageListElement>();
    
    IMAPFolder imapFolder = getFolder(account, "Inbox");
    
    if (imapFolder == null) {
//      Log.d("rgai", "IT WAS UNABLE TO OPEN FOLDER: " + account + ", " + "Inbox");
      return new MessageListResult(emails, MessageListResult.ResultType.ERROR);
    }
    
    int messageCount = imapFolder.getMessageCount();
    
    if (offset == 0 && !hasNewMail(imapFolder, loadedMessages, messageCount)) {
//      Log.d("rgai", "NO FRESH MAIL");
      if (MainActivity.isMainActivityVisible()) {
//        Log.d("rgai", "CHECKING FLAG STATUS: MainA is visible");
        return getFlagChangesOfMessages(messageCount, limit, offset, loadedMessages);
      } else {
//        Log.d("rgai", "NOT CHECKING FLAG STATUS: MainA is not visible");
        return new MessageListResult(emails, MessageListResult.ResultType.NO_CHANGE);
      }
    }
    
//    int messageCount = inbox.getMessageCount();
    int start = Math.max(1, messageCount - limit - offset + 1);
    int end = start + limit > messageCount ? messageCount : start + limit;
    
    // we are refreshing here, not loading older messages
    
//    Log.d("rgai", "messageCount: " + messageCount);
//    Log.d("rgai", "start: " + start);
//    Log.d("rgai", "end: " + end);
//    Log.d("rgai", "account: " + account);
    Message messages[] = imapFolder.getMessages(start, end);
//    inbox.get
    

    for (int i = messages.length - 1; i >= 0; i--) {
      Message m = messages[i];
      long uid = imapFolder.getUID(m);
      
      Flags flags = m.getFlags();
      boolean seen = flags.contains(Flags.Flag.SEEN);
      
      
      Date date = m.getSentDate();

      
      // Skipping email from listing, because it is a spam probably,
      // ...at least citromail does not give any information about the email in some cases,
      // and in web browsing it displays an "x" sign before the title, which may indicate
      // that this is a spam
      Person fromPerson = getSenderPersonObject(m);
      if (fromPerson == null) {
        // skipping email
      } else {
        
        MessageListElement testerElement = new MessageListElement(uid + "", seen, fromPerson, date, account, Type.EMAIL, true);
        
        
        if (MessageProvider.Helper.isMessageLoaded(loadedMessages, testerElement)) {
          emails.add(testerElement);
          continue;
        }/* else {
          System.out.println("adding: new Date("+ date.getTime() +"l), \""+ fromEmail +"\")");
        }*/
        
        EmailContent content = getMessageContent(m);
        
        String subject = m.getSubject();
        if (subject != null) {
          subject = prepareMimeFieldToDecode(subject);
          try {
            subject = MimeUtility.decodeText(subject);
          } catch (java.io.UnsupportedEncodingException ex) {
          }
        } else {
          try {
            Source source = new Source(content.getContent().getContent());
            String decoded = source.getRenderer().toString();
            String snippet = decoded.substring(0, Math.min(snippetMaxLength, decoded.length()));
            subject = snippet;
          } catch (StackOverflowError so) {
          }
          if (subject == null) {
            subject = "<No subject>";
          }
        }
        
//        System.out.println("fromName -> " + fromName);
//        System.out.println("fromEmail -> " + fromEmail);
        
        MessageListElement mle = new MessageListElement(uid + "", seen, subject, "",
                fromPerson, null, date, account, Type.EMAIL);
        FullSimpleMessage fsm = new FullSimpleMessage(uid + "", subject,
                content.getContent(), date, fromPerson, false, Type.EMAIL, content.getAttachmentList());
        mle.setFullMessage(fsm);
        emails.add(mle);
      }
    }
    imapFolder.close(true);
//    store.close();
    return new MessageListResult(emails, MessageListResult.ResultType.CHANGED);
  }
  
  private MessageListResult getFlagChangesOfMessages(int messageCount,
          int limit, int offset, TreeSet<MessageListElement> loadedMessages) throws MessagingException {
    
    List<MessageListElement> emails = new LinkedList<MessageListElement>();
    Store store = getStore(account);
    
    if (store == null || !store.isConnected()) {
      return new MessageListResult(emails, MessageListResult.ResultType.ERROR);
    }
    
    IMAPFolder imapFolder = (IMAPFolder)store.getFolder("Inbox");
    imapFolder.open(Folder.READ_ONLY);
    
    messageCount = imapFolder.getMessageCount();
    
    int start = Math.max(1, messageCount - limit - offset + 1);
    int end = start + limit > messageCount ? messageCount : start + limit;
    Message messages[] = imapFolder.getMessages(start, end);

    for (int i = messages.length - 1; i >= 0; i--) {
      Message m = messages[i];
      long uid = imapFolder.getUID(m);
      
      Flags flags = m.getFlags();
      boolean seen = flags.contains(Flags.Flag.SEEN);
      
      MessageListElement testerElement = new MessageListElement(uid + "", seen, null, null, account, Type.EMAIL, true);
      if (MessageProvider.Helper.isMessageLoaded(loadedMessages, testerElement)) {
        emails.add(testerElement);
      }
    }
    
    return new MessageListResult(emails, MessageListResult.ResultType.FLAG_CHANGE);
  }
  
  private boolean hasNewMail(IMAPFolder inbox, TreeSet<MessageListElement> loadedMessages, int messageCount) throws MessagingException {
    
//    Store s = getStore(account);
    
//    Message messages[] = inbox.getMessages(endIndex, endIndex);
//    Log.d("rgai", "uidValidity: " + inbox.getUIDValidity());
    
    String nextUID = inbox.getUIDNext() + "";
    
    
    String newKey = nextUID+"_"+messageCount;
    
    String storedKey = getValidityString(account);
    if (newKey.equals(storedKey)) {
      return false;
    } else {
      validityMap.put(account, newKey);
      return true;
    }
    
    
    
//    for (Message m : messages) {
//      String uid = inbox.getUID(m) + "";
//      MessageListElement firstLoaded = null;
//      if (loadedMessages != null && !loadedMessages.isEmpty()) {
//        firstLoaded = loadedMessages.first();
//      }
//      if (firstLoaded != null) {
////        Log.d("rgai", "FirstLoaded: " + firstLoaded.toString());
////        Log.d("rgai", "current uid: " + uid);
//        if (firstLoaded.getId().equals(uid)) {
//          return false;
//        }
//      }
//    }
//    return true;
  }
  
  private String getValidityString(EmailAccount account) {
    if (validityMap == null) {
      validityMap = new HashMap<EmailAccount, String>();
      return null;
    } else {
      return validityMap.get(account);
    }
  }
  
  private Person getSenderPersonObject(Message m) throws MessagingException {
    String from = null;
    if (m.getFrom() != null) {
      from = m.getFrom()[0].toString();
      from = prepareMimeFieldToDecode(from);
    }
    if (from != null) {
      try {
        from = MimeUtility.decodeText(from);
      } catch (java.io.UnsupportedEncodingException ex) {
        ex.printStackTrace();
      }
    }

    // Skipping email from listing, because it is a spam probably,
    // ...at least citromail does not give any information about the email in some cases,
    // and in web browsing it displays an "x" sign before the title, which may indicate
    // that this is a spam
    if (from == null) {
      // skipping email
      return null;
    } else {
      String fromName = null;
      String fromEmail = null;
      String regex = "(.*)<([^<>]*)>";
//        System.out.println("SimpleEmailMessageProvider: from -> " + from);
      if (from.matches(regex)) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(from);
        while(matcher.find()) {
          fromName = matcher.group(1);
          fromEmail = matcher.group(2);
          break;
        }
      } else {
        fromName = from;
        fromEmail = from;
      }
      return new Person(fromEmail.trim(), fromName.trim(), MessageProvider.Type.EMAIL);
    }
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
    
//    System.setProperty("javax.activation.debug", "true");
    HtmlContent content = new HtmlContent();
    List<Attachment> attachments = null;
    
    Object msg = fullMessage.getContent();
    
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
      files.add(new Attachment(MimeUtility.decodeText(bp.getFileName()), bp.getSize()));
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
            Logger.getLogger(SimpleEmailMessageProvider.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      } else if (contentType.indexOf("text/html") != -1) {
        htmlFound = true;
        content = new HtmlContent(bp.getContent().toString(), HtmlContent.ContentType.TEXT_HTML);
        break;
      }
    }
    
    return content;
  }
  
  /**
   * Extracts the person information from Address object.
   * 
   * @param a the raw input Address object from the email
   * @return Person object
   */
  private static Person getPersonFromAddress(Address a) {
    String ad = a.toString();
    
    ad = prepareMimeFieldToDecode(ad);
    try {
      ad = MimeUtility.decodeText(ad);
    } catch (UnsupportedEncodingException ex) {
      ex.printStackTrace();
    }
    
    int mailOp = ad.indexOf("<");
    int mailCl = ad.indexOf(">");
    String name = null;
    String email;
    if (mailOp != -1 && mailOp + 1 < mailCl) {
      email = ad.substring(mailOp + 1, mailCl);
      name = ad.substring(0, mailOp).trim();
    } else {
      email = ad.trim();
    }
    return new Person(email, name, MessageProvider.Type.EMAIL);
  }

  @Override
  public FullMessage getMessage(String id) throws NoSuchProviderException, MessagingException, IOException {

    IMAPFolder folder = getFolder(account, "Inbox");
    
    if (folder == null) return null;
    
//    if (!folder.isOpen()) {
//      folder.open(Folder.READ_WRITE);
//    }
    EmailContent content;
    
    Message ms = folder.getMessage(Integer.parseInt(id));
//    ms.s
    
    List<Person> to = new LinkedList<Person>();
    
    Address[] addr = ms.getAllRecipients();
    for (Address a : addr) {
      to.add(getPersonFromAddress(a));
    }
    content = getMessageContent(ms);
    
    Person from = getPersonFromAddress(ms.getFrom()[0]);
    
    String subject = ms.getSubject();
    
    Date date = ms.getSentDate();
    
    folder.close(true);
    
    return new FullSimpleMessage(id, subject, content.getContent(), date, from, false, MessageProvider.Type.EMAIL, null);
    
  }
  
  public byte[] getAttachmentOfMessage(String messageId, String attachmentId) throws NoSuchProviderException, MessagingException, IOException {
    IMAPFolder folder = getFolder(account, "Inbox");
    
    if (folder == null) return null;
    
//    if (!folder.isOpen()) {
//      folder.open(Folder.READ_WRITE);
//    }
    
    Message ms = folder.getMessage(Integer.parseInt(messageId));
    
    return getMessageAttachment(ms, attachmentId);
    
  }
  
  private byte[] getMessageAttachment(Message message, String attachmentId) throws IOException, MessagingException {
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
            if (progressUpdate != null) {
              progressUpdate.onProgressUpdate(buffer.size() * 100 / fullSize);
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
  public void sendMessage(Set<? extends MessageRecipient> to, String content, String subject) throws
          NoSuchProviderException, MessagingException, IOException, AddressException {
    
    Properties props = System.getProperties();
    this.setProperties(props);
    props.put("mail.smtps.host", account.getSmtpAddress());
    props.put("mail.smtps.auth","true");
    Session session = Session.getInstance(props, null);
    javax.mail.Message msg = new MimeMessage(session);
    
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
//    msg.setHeader("X-Mailer", "");
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
          sb.append(er.getName());
        }
        sb.append("<").append(er.getEmail()).append(">");
      }
    }
    
    return sb.toString();
  }

  @Override
  public void markMessageAsRead(String id) throws NoSuchProviderException, MessagingException, IOException {
    
    IMAPFolder folder = getFolder(account, "Inbox");
    
    if (folder == null) return;
    
//    if (!folder.isOpen()) {
//      folder.open(Folder.READ_WRITE);
//    }
    
    Message ms = folder.getMessageByUID(Long.parseLong(id));
    if (ms != null) {
      ms.setFlag(Flags.Flag.SEEN, true);
    }
  }

  public boolean canBroadcastOnNewMessage() {
    return false;
  }

  public boolean isConnectionAlive() {
    return false;
  }

  public void establishConnection(Context context) {
    // we cannot establish a live connection with the server
  }
  
  public interface AttachmentProgressUpdate {
    public void onProgressUpdate(int progress);
  }

  private class AccountFolder {
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
    
  }

  @Override
  public String toString() {
    return "SimpleEmailMessageProvider{" + "account=" + account + '}';
  }
  
}
