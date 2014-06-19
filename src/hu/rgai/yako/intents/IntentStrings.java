
package hu.rgai.yako.intents;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class IntentStrings {
  
  public static class Actions {
    public static final String MESSAGE_SENT_BROADCAST = "hu.rgai.yako.message_sent_broadcast";
    public static final String SMS_DELIVERED = "hu.rgai.yako.intent.action.sms_delivered";
    public static final String SMS_SENT = "hu.rgai.yako.intent.action.sms_sent";
    
  }
  
  public static class Params {
    
    public static final String EXTRA_PARAMS = "extra_params";
    public static final String FROM_NOTIFIER = "from_notifier";
    public static final String QUERY_LIMIT = "query_limit";
    public static final String QUERY_OFFSET = "query_offset";
    public static final String LOAD_MORE = "load_more";
    public static final String TYPE = "type";
    public static final String ITEM_INDEX = "item_index";
    public static final String ITEM_COUNT = "item_count";
    public static final String ACT_VIEWING_MESSAGE = "act_viewing_message";
    public static final String FORCE_QUERY = "force_query";
    public static final String RESULT = "result";
    public static final String ERROR_MESSAGE = "error_message";
    public static final String ACCOUNT = "account";

    // this 2 data is required to find a messageListElement
    public static final String MESSAGE_ID = "message_id";
    public static final String MESSAGE_ACCOUNT = "message_account";
    
    public static final String MESSAGE_SENT_RESULT_TYPE = "message_sent_result_type";
    public static final String MESSAGE_SENT_BROADCAST_DATA = "message_sent_broadcast_data";
    public static final String MESSAGE_SENT_HANDLER_DATA = "message_sent_handler_data";
    public static final String MESSAGE_SENT_RECIPIENT_NAME = "message_sent_recipient_name";
    public static final String MESSAGE_SENT_LOAD_MESSAGES_AFTER_SUCCESS = "message_sent_load_messages_after_success";
    
    public static final String MESSAGE_THREAD_CHANGED = "message_thread_changed";
    
  }
  
  
  
  
}
