
package hu.rgai.android.messageproviders;

/**
 *
 * @author Tamas Kojedzinszky
 */
public interface MessageCallback {
  public void messageAdded(int newMessageCount);
  public void messageRemoved();
}
