
package hu.rgai.yako.beens;

import java.util.List;
import java.util.TreeMap;

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
     * Indicates that the returning messages are considered as the ethalon messages on the server side,
     * so the UID which not exists in the local list should be removed.
     */
    MERGE_DELETE,

//    /**
//     * Indicates that the queried message data is only the first part of the necessary query and a
//     * second query is MANDATORY to finish downloading all data which belongs to the messages.
//     * I.e: when loading all data to a message is too slow, the loading should be separated into 2 parts,
//     * in the first query only the absolutely necessary data should be downloaded (from, title, UID)
//     * and in second round other data should be downloaded (content, flags..).
//     */
//    SPLITTED_RESULT_FIRST_PART,

    /**
     * This is the second part of the SPLITTED_RESULT_FIRST_PART data: when this is the result type, it holds other
     * infos and datas about the message.
     */
    SPLITTED_RESULT_SECOND_PART,


  }
  
  private List<MessageListElement> mMessages;
  private final ResultType resultType;

  /**
   * This map holds that messages which needs more data load after the first data load.
   * In first round only quick and necessary informations are load, after that the heavy work is done.
   * String key is the id of the message on a specific domain and value (long) is the database raw id of the message
   * which is already inserted into database.
   */
  private TreeMap<String, MessageListElement> mSplittedMessages = null;

  public MessageListResult(List<MessageListElement> messages, ResultType resultType) {
    this.mMessages = messages;
    this.resultType = resultType;
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

  public void setSplittedMessages(TreeMap<String, MessageListElement> splittedMessages) {
    mSplittedMessages = splittedMessages;
  }

  public TreeMap<String, MessageListElement> getSplittedMessages() {
    return mSplittedMessages;
  }
}
