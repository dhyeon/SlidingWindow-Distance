import java.util.ArrayList;

public class ClozeInstance {
    public String instanceInfo;
    public String turkInfo;
    public String story;
    String questions = "";
    String[] options = new String[400];
    String correctAnswers = "";
    int correctAnswersInt = 999;
    boolean NeedsMultipleSentences = false;

    // curator annotation
    public byte[] storyAnt;
    public byte[][] questionAnt;
}
