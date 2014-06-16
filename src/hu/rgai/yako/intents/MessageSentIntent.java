
package hu.rgai.yako.intents;

import android.content.Intent;

public class MessageSentIntent extends Intent {
  
  private static final String TYPE_PARAM = "hu.rgai.yako.messagesend.type_param";
  private static final String HANDLER_PARAM = "hu.rgai.yako.messagesend.handler_param";
  
  public MessageSentIntent(String action) {
    super(action);
  }
  
  public void setSentType(int sentType) {
    this.putExtra(TYPE_PARAM, sentType);
  }
  
  public void getSentType(int sentType) {
    this.getExtras().getInt(TYPE_PARAM);
  }
  
  public void setHandlerClass(Class handlerClass) {
    this.putExtra(HANDLER_PARAM, handlerClass);
  }

}
