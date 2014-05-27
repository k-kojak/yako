
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
  private final boolean providerSupportsUID;

  public MessageListResult(List<MessageListElement> messages, ResultType resultType, boolean providerSupportsUID) {
    this.messages = messages;
    this.resultType = resultType;
    this.providerSupportsUID = providerSupportsUID;
  }

  public List<MessageListElement> getMessages() {
    return messages;
  }

  public ResultType getResultType() {
    return resultType;
  }

  public boolean isProviderSupportsUID() {
    return providerSupportsUID;
  }
  
}
