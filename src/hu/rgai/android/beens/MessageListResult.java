
package hu.rgai.android.beens;

import java.util.List;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class MessageListResult {
  
  public static enum ResultType {NO_CHANGE, CHANGED, ERROR, FLAG_CHANGE, CANCELLED};
  
  private final List<MessageListElement> messages;
  private final ResultType resultType;

  public MessageListResult(List<MessageListElement> messages, ResultType resultType) {
    this.messages = messages;
    this.resultType = resultType;
  }

  public List<MessageListElement> getMessages() {
    return messages;
  }

  public ResultType getResultType() {
    return resultType;
  }
  
}
