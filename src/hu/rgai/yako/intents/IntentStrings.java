
package hu.rgai.yako.intents;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class IntentStrings {
  
  public static class Actions {
    public static final String MESSAGE_SENT_BROADCAST = "hu.rgai.yako.message_sent_broadcast";
    
  }
  
  public static class Params {
    
    public static final String EXTRA_PARAMS = "extra_params";
    public static final String FROM_NOTIFIER = "from_notifier";
    public static final String QUERY_LIMIT = "query_limit";
    public static final String QUERY_OFFSET = "query_offset";
    public static final String LOAD_MORE = "load_more";
    public static final String TYPE = "type";
    public static final String ACT_VIEWING_MESSAGE = "act_viewing_message";
    public static final String FORCE_QUERY = "force_query";
    public static final String RESULT = "result";
    public static final String ERROR_MESSAGE = "error_message";

    // this 2 data is required to find a messageListElement
    public static final String MESSAGE_ID = "message_id";
    public static final String MESSAGE_ACCOUNT = "message_account";
    
    public static final String MESSAGE_SENT_RESULT_TYPE = "message_sent_result_type";
    public static final String MESSAGE_SENT_HANDLER_INTENT = "handler_intent_param";
    public static final String MESSAGE_SENT_RECIPIENT_NAME = "message_sent_recipient_name";
    
  }
  
  
  
  
}
