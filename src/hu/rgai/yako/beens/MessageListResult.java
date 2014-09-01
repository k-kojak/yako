
package hu.rgai.yako.beens;

import java.util.List;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class MessageListResult {
  
  public static enum ResultType {
    NO_CHANGE,
    CHANGED,
    ERROR,
    FLAG_CHANGE,
    CANCELLED,

    /**
     * Indicates that the returning mMessages are considered as the actual mMessages on the server side,
     * so the UID which not in this list could be removed.
     */
    MERGE_DELETE
  };
  
  private List<MessageListElement> mMessages;
  private final ResultType resultType;
//  private final boolean providerSupportsUID;

  public MessageListResult(List<MessageListElement> messages, ResultType resultType) {
    this.mMessages = messages;
    this.resultType = resultType;
//    this.providerSupportsUID = providerSupportsUID;
  }

  public List<MessageListElement> getMessages() {
    return mMessages;
  }

  public ResultType getResultType() {
    return resultType;
  }

  public void setMessages(List<MessageListElement> messages) {
    mMessages = messages;
  }

}
