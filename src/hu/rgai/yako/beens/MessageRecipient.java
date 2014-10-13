package hu.rgai.yako.beens;

import android.net.Uri;
import hu.rgai.yako.messageproviders.MessageProvider;

/**
 *
 * @author Tamas Kojedzinszky
 */
public interface MessageRecipient {
  public String getDisplayData();

  public String getData();

  public String getDisplayName();

  public Uri getImgUri();

  public int getContactId();

  public MessageProvider.Type getType();

  public class Helper {
    public static MessageRecipient personToRecipient(Person p) {
      MessageRecipient ri = null;

      if (p != null) {
        switch (p.getType()) {
          case FACEBOOK:
            ri = new FacebookMessageRecipient(p.getName(), p.getId(), p.getName(), null, (int) p.getContactId());
            break;
          case SMS:
            ri = new SmsMessageRecipient(p.getName(), p.getId(), p.getName(), null, (int) p.getContactId());
            break;
          case EMAIL:
          case GMAIL:
            ri = new EmailMessageRecipient(p.getName(), p.getId(), p.getName(), null, (int) p.getContactId());
            break;
          default:
            break;
        }
      }
      return ri;
    }
  }
}