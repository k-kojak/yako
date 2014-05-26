
package hu.rgai.android.handlers;

/**
 *
 * @author Tamas Kojedzinszky
 */
public abstract class MessageSendHandler extends TimeoutHandler {

  public abstract void success(String name);
  public abstract void fail(String name);
  
}
