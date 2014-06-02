
package hu.rgai.yako.messageproviders;

import javax.mail.Message;

/**
 *
 * @author Tamas Kojedzinszky
 */
public interface MessageCallback {
  public void messageAdded(int newMessageCount);
  public void messageRemoved(Message[] messages);
}
