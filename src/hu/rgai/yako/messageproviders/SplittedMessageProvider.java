package hu.rgai.yako.messageproviders;

import hu.rgai.yako.beens.MessageListResult;

import javax.mail.MessagingException;
import java.io.IOException;
import java.security.cert.CertPathValidatorException;
import java.util.TreeMap;

/**
 * Created by kojak on 11/4/2014.
 */
public interface SplittedMessageProvider {

  /**
   * Returns the remaining data to a messages.
   *
   * @param messagesToLoad messages map where the key is the unique id on a domain (account), the value is the
   *                       database raw id
   * @return
   * @throws CertPathValidatorException
   * @throws IOException
   * @throws MessagingException
   */
  public MessageListResult loadDataToMessages(TreeMap<String, Long> messagesToLoad)
          throws CertPathValidatorException, IOException, MessagingException;


}
