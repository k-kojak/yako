package hu.rgai.yako.smarttools;

import hu.rgai.yako.beens.MessageListElement;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by kojak on 10/13/2014.
 */
public class DummyQuickAnswerProvider implements QuickAnswerProvider {

  @Override
  public List<String> getQuickAnswers(MessageListElement message) {
    List<String> answers = new LinkedList<String>();
    if (Math.random() > 0.5) {
      answers.add("Igen");
      answers.add("Talán");
      answers.add("Sör");
    }
    return answers;
  }
}
