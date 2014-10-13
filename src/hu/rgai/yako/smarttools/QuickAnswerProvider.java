package hu.rgai.yako.smarttools;

import hu.rgai.yako.beens.MessageListElement;
import java.util.List;

public interface QuickAnswerProvider {
  public List<String> getQuickAnswers(MessageListElement message);
}
