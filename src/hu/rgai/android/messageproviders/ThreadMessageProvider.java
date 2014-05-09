
package hu.rgai.android.messageproviders;

import hu.rgai.android.beens.FullMessage;
import java.io.IOException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;

/**
 *
 * @author Tamas Kojedzinszky
 */
public interface ThreadMessageProvider extends MessageProvider {
  public FullMessage getMessage(String id, int offset, int limit) throws NoSuchProviderException, MessagingException, IOException;
}
