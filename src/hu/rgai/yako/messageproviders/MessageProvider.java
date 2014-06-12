package hu.rgai.yako.messageproviders;

import android.content.Context;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.FullMessage;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.beens.MessageListResult;
import hu.rgai.yako.beens.MessageRecipient;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.cert.CertPathValidatorException;
import java.util.Set;
import java.util.TreeSet;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.net.ssl.SSLHandshakeException;

/**
 * This is the global interface for a general Message Provider (simple email, gmail, facebook, etc).
 * @author Tamas Kojedzinszky
 */
public interface MessageProvider {
  
  public enum Type {EMAIL, GMAIL, FACEBOOK, SMS};
  
  
  public Account getAccount();
  
  /**
   * Returns the list of messages.
   * 
   * @param offset the offset of the queried messages
   * @param limit the limit of queried messages
   * @param loadedMessages the list of message that already loaded
   * @return List of MessageListElement objects, the list of messages
   * @throws CertPathValidatorException
   * @throws SSLHandshakeException
   * @throws ConnectException
   * @throws NoSuchProviderException
   * @throws UnknownHostException
   * @throws IOException
   * @throws MessagingException
   * @throws AuthenticationFailedException 
   */
  public MessageListResult getMessageList(int offset, int limit, TreeSet<MessageListElement> loadedMessages) throws CertPathValidatorException,
          SSLHandshakeException, ConnectException, NoSuchProviderException, UnknownHostException,
          IOException, MessagingException, AuthenticationFailedException;
  
  /**
   * Returns the list of messages.
   * 
   * @param offset the offset of the queried messages
   * @param limit the limit of queried messages
   * @param loadedMessages 
   * @param snippetMaxLength the max length of the snippet
   * @return List of MessageListElement objects, the list of messages
   * @throws CertPathValidatorException
   * @throws SSLHandshakeException
   * @throws ConnectException
   * @throws NoSuchProviderException
   * @throws UnknownHostException
   * @throws IOException
   * @throws MessagingException
   * @throws AuthenticationFailedException 
   */
  public MessageListResult getMessageList(int offset, int limit, TreeSet<MessageListElement> loadedMessages, int snippetMaxLength) throws CertPathValidatorException,
          SSLHandshakeException, ConnectException, NoSuchProviderException, UnknownHostException,
          IOException, MessagingException, AuthenticationFailedException;
  
  /**
   * Returns a single message.
   * 
   * @param id the id of the message
   * @return a FullMessage object which contains all necessary information about the message itself
   * @throws NoSuchProviderException
   * @throws MessagingException
   * @throws IOException 
   */
  public FullMessage getMessage(String id) throws NoSuchProviderException, MessagingException, IOException;
  
  /**
   * Sets the status of a message to seen.
   * 
   * @param id the id of the message
   * @param seen true if set to seen, false if set to unseen
   * @throws NoSuchProviderException
   * @throws MessagingException
   * @throws IOException 
   */
  public void markMessageAsRead(String id, boolean seen) throws NoSuchProviderException, MessagingException, IOException;
  
  
  /**
   * Sets the status of the messages to seen.
   * 
   * @param id the ids of the messages to set flag
   * @param seen true if set to seen, false if set to unseen
   * @throws NoSuchProviderException
   * @throws MessagingException
   * @throws IOException 
   */
  public void markMessagesAsRead(String[] id, boolean seen) throws NoSuchProviderException, MessagingException, IOException;
  
  /**
   * Tells that if the message provider sends broadcast to the system when new message arrives.
   * 
   * If so, then the system does not has to select messages on cycle events.
   * For this kind of behaviour some kind of live connection needed.
   * 
   * @return true if message provider can broadcast on new message, false otherwise
   */
  public boolean canBroadcastOnNewMessage();
  
  /**
   * Returns true if the provider can send broadcast message on message change, so do not have
   * to send a full getList request.
   * 
   * @return true if can broadcast on message change, false otherwise
   */
  public boolean canBroadcastOnMessageChange();
  
  /**
   * If this is a broadcasting message provider, then this functions value must base on the
   * state of the live connection, otherwise it must be false.
   * 
   * @return true if is there a live connection with a server, false otherwise
   */
  public boolean isConnectionAlive();

  
  /**
   * Establishis a connection to a server if possible.
   * 
   * This funcion can be empty, you should write a body to it only when you can create a 
   * live connection to a server so that can notify you on a new event or new message.
   * @param context a Context object if needed for broadcast sending
   */
  public void establishConnection(Context context);
  
  
  /**
   * This function is used to drop any connections if have any.
   * 
   * Just leaeve it blank if the provider cannot broadcast messages.
   */
  public void dropConnection();
  
  
  /**
   * If true that means this message provider is able to delete messages.
   * @return true if can delete message, false otherwise
   */
  public boolean isMessageDeletable();
  
  
  /**
   * Deletes a single message item.
   * @param id the id of the message to delete
   */
  public void deleteMessage(String id) throws NoSuchProviderException, MessagingException, IOException;
  
  /**
   * Sends a message to the given recipient with the given content.
   * 
   * @param to set of recipients
   * @param content the content of the message
   * @param subject subject of the message (optional)
   * @throws NoSuchProviderException
   * @throws MessagingException
   * @throws IOException 
   */
  public void sendMessage(Set<? extends MessageRecipient> to, String content, String subject)
          throws NoSuchProviderException, MessagingException, IOException;
  
  public static class Helper {
    
    public static boolean isMessageLoaded(TreeSet<MessageListElement> messages, MessageListElement message) {
      for (MessageListElement mle : messages) {
        if (mle.getDate().equals(message.getDate()) && mle.getAccount().equals(message.getAccount())) {
          return true;
        }
      }
      return false;
    }
  }
}