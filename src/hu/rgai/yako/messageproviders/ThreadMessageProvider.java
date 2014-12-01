
package hu.rgai.yako.messageproviders;

import hu.rgai.yako.beens.FullMessage;
import java.io.IOException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;

/**
 *
 * @author Tamas Kojedzinszky
 */
public interface ThreadMessageProvider extends MessageProvider {
  
  public FullMessage getMessage(String id, int offset, int limit) throws MessagingException, IOException;
  
  public void deleteThread(String id);
  
}
