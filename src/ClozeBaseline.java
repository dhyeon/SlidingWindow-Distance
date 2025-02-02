import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ClozeBaseline {
    static String initials = "data/";

    static double dweight;
    static boolean whichNot;
    static boolean doStem;
    static HashSet<String> stopWords;
    static String separators = " |\n|\t";

    public static void main(String[] args) throws IOException {
        initialize();
        baseline();
    }

    private static void initialize() throws IOException {
        dweight = 1;
        doStem = false;
        whichNot = false;

        readStopWords();
        ClozeReader.readAll();
    }

    public static void baseline() {
        int correctMulti = 0, correctSingle = 0, correctAll = 0, totalMulti = 0, totalSingle = 0, totalAll = 0;
        for (ClozeInstance ins : ClozeReader.clozeAll) {

			/*
			System.out.println(ins.story);
			System.out.println(ins.questions);
			
			for(String s: ins.options){
				System.out.println(s);
			}
			
			System.out.println(ins.correctAnswers);
			System.out.println(ins.correctAnswersInt);
			
			
			
			// Passage
			System.out.println("\n=========================");
			System.out.println("Passage");
			System.out.println("=========================\n");
			
			*/
			String normedPassage = normalizeString(ins.story);
            String[] normedPassageArr = normedPassage.split(separators);
			
            // Candidate
            String[] candidateAnswers = ins.options;
			
			// Questions
            String normedQuestion = normalizeString(ins.questions);
            boolean chooseBest = true;
			
            double[] answerScores = ScoreAnswers(normedPassageArr, normedQuestion, candidateAnswers, stopWords);
            
            
            List<Integer> selectedAnswers = chooseBest ? BestItems(answerScores) : WorstItems(answerScores);
            
            if (selectedAnswers.contains(ins.correctAnswersInt)) {
                double credit = 1.0 / selectedAnswers.size();  // Partial credit if there was a tie
                //double credit = 1.0;
                if (ins.NeedsMultipleSentences) {
                    correctMulti += credit;
                } else {
                    correctSingle += credit;
                }
                correctAll += credit;
            }

            if (ins.NeedsMultipleSentences)
                totalMulti += 1;
            else
                totalSingle += 1;
            totalAll += 1;

        }

        System.out.println("Correct / All = " + correctAll + " out of " + totalAll + " -> " + 100.0 * correctAll / totalAll);
        System.out.println("Correct / Single = " + correctSingle + " out of " + totalSingle + " -> " + 100.0 * correctSingle / totalSingle);
    }
    static double[] ScoreAnswers(String[] normedPassageArr, String normedQuestion, String[] candidateAnswers, HashSet<String> stopWords) {
        HashMap<String, Double> invWordCounts = compInvWordCounts(normedPassageArr);

        double[] scores = new double[candidateAnswers.length];
        for (int i = 0; i < candidateAnswers.length; ++i) {
            String normedCandidateAnswer = normalizeString(candidateAnswers[i]);
            String catted = new String(normedQuestion); //String.Copy(normedQuestion);
            catted += " ";
            catted += normedCandidateAnswer;
            HashSet<String> hs = makeSet(catted);
            double scoreW = scanWindow(hs, normedPassageArr, invWordCounts);
            double scoreD = distQA(normedQuestion, normedCandidateAnswer, normedPassageArr, stopWords);
            
            
            double score = scoreW - dweight * scoreD;  

            scores[i] = scoreW;
        }
        return scores;
    }

    static double[] ScoreAnswersNormalized(String[] normedPassageArr, String normedQuestion, String[] candidateAnswers, HashSet<String> stopWords) {
        HashMap<String, Double> invWordCounts = compInvWordCounts(normedPassageArr);
        double[] scores = new double[candidateAnswers.length];
        int avgSize = 0;
        for (int i = 0; i < candidateAnswers.length; ++i) {
            String normedCandidateAnswer = normalizeString(candidateAnswers[i]);
            String catted = new String(normedQuestion); //String.Copy(normedQuestion);
            catted += " ";
            catted += normedCandidateAnswer;
            //Console.WriteLine("----------------> avgSize = {0} ", avgSize); 

            avgSize += catted.split(" ").length;
            HashSet<String> hs = makeSet(catted);
            hs.add(catted);
            double scoreW = scanWindow(hs, normedPassageArr, invWordCounts);
            double scoreD = distQA(normedQuestion, normedCandidateAnswer, normedPassageArr, stopWords);
            double score = scoreW - dweight * scoreD;

            scores[i] = score;
        }

        // normalize
        avgSize = avgSize / candidateAnswers.length;
        for (int i = 0; i < scores.length; i++)
            scores[i] = scores[i] / avgSize;
        return scores;
    }

    static double distQA(String normedQuestion, String normedCandidateAnswer, String[] normedPassageArr, HashSet<String> stopWords) {
        // Make a copy for safety.
        String[] passage = new String[normedPassageArr.length];

        System.arraycopy(normedPassageArr, 0, passage, 0, normedPassageArr.length);
        //Array.Copy(normedPassageArr, passage, normedPassageArr.length);
        String[] question = normedQuestion.split(separators);
        String[] answer = normedCandidateAnswer.split(separators);
        int nWords = passage.length;
        int maxStringLen = 1000;  // Make this a no-op, for now.
        HashSet<String> passageH = new HashSet<String>();

        for (String str : passage) {
            passageH.add(str.length() > maxStringLen ? str.substring(0, maxStringLen) : str);
        }

        HashSet<String> qhs = new HashSet<String>();

        for (String str : question) {
            String strShort = str.length() > maxStringLen ? str.substring(0, maxStringLen) : str;
            if (!stopWords.contains(str) && passageH.contains(strShort)) {
                qhs.add(strShort);
            }
        }

        HashSet<String> ahs = new HashSet<String>();
        for (String str : answer) {
            String strShort = str.length() > maxStringLen ? str.substring(0, maxStringLen) : str;
            if (!stopWords.contains(str) && passageH.contains(strShort) && !qhs.contains(strShort)) {
                ahs.add(strShort);
            }
        }

        if (ahs.size() == 0 || qhs.size() == 0)
            return 1.0;

        double totScore = 0.0;
        for (String qStr : qhs)
            totScore += findMinScore(qStr, ahs, passage);

        return totScore / (double) qhs.size();
    }

    static double scanWindow(HashSet<String> hs, String[] aPassage, HashMap<String, Double> invWordCounts) {
        //int qLen = hs.size();
        int qLen = 15;
        int pLen = aPassage.length;
        double bestMatch = 0.0;// double.NegativeInfinity;
        int bestIndex = -1; // Debug
        for (int i = 0; i < pLen - qLen + 1; ++i) {
            double match = 0.0;
            for (int j = 0; j < qLen; ++j) {
                String word = aPassage[i + j]; // Debug
                if (hs.contains(word))
                    match += invWordCounts.get(word);
            }
            if (match > bestMatch) {
                bestIndex = i;
                bestMatch = match;
            }
        }
        return bestMatch;
    }

    public static String normalizeString(String str) {
        str = str.replace("\\newline", " ");
        str = str.replaceAll("[.,;:?]", " ");
        str = str.replaceAll("\\s+", " ");
        str = str.trim();
        return str.toLowerCase();
    }

    // Assumes that the answer strings (in aH) all occur in the passage array (pA), although the question string (qS) may not.
    // Note also that qS does not itself occur in aH.
    static double findMinScore(String questionS, HashSet<String> answersH, String[] passageA) {
        int nWords = passageA.length;
        int minDist = nWords - 1;
        for (int i = 0; i < nWords; ++i) {
            String passageTerm = passageA[i];

            if (passageTerm.equals(questionS)) {
                for (int j = 0; j < nWords; ++j) {
                    String passageTerm2 = passageA[j];
                    if (answersH.contains(passageTerm2) && Math.abs(i - j) < minDist)
                        minDist = Math.abs(i - j);
                }
            }
        }

        return (double) minDist / (double) nWords;
    }

    static HashMap<String, Double> compInvWordCounts(String[] aPassage) {
        HashMap<String, Double> count = new HashMap<String, Double>();
        HashMap<String, Double> invCount = new HashMap<String, Double>();
        for (int i = 0; i < aPassage.length; ++i) {
            String word = aPassage[i];
            //if (doStem) 
            //	word = stemmer.stemTerm(word);
            if (count.containsKey(word))
                count.put(word, count.get(word) + 1.0);
            else
                count.put(word, 1.0);
        }
        for (String key : count.keySet()) {
            //invCount.Add(key, 1.0 / count[key]);
            invCount.put(key, Math.log(1.0 + 1.0 / count.get(key))); // Add 1.0 to ensure positivity.
        }
        return invCount;
    }

    public static void readStopWords() throws IOException {
        stopWords = new HashSet<String>();
        String stopF = initials + "stopwords.txt";
        InputStream ips = new FileInputStream(stopF);
        InputStreamReader ipsr = new InputStreamReader(ips);
        BufferedReader br = new BufferedReader(ipsr);
        String line;
        while ((line = br.readLine()) != null) {
            stopWords.add(line);
        }
    }

    public static List<Integer> BestItems(double[] vals) {
        List<Integer> bestItems = new ArrayList<Integer>();
        double bestVal = -10000;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] >= bestVal) {
                if (vals[i] > bestVal) {
                    bestVal = vals[i];
                    bestItems.clear();
                }
                bestItems.add(i);
            }
        }
        return bestItems;
    }


    static List<Integer> BestItemsWithThreshold(double[] vals, double thr) {
        List<Integer> bestItems = new ArrayList<Integer>();
        double bestVal = -10000;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] >= bestVal) {
                if (vals[i] > bestVal) {
                    bestVal = vals[i];
                    bestItems.clear();
                }
                if (vals[i] >= thr)
                    bestItems.add(i);
            }
        }
        return bestItems;
    }

    // ErinRen: just like BestItems, but for the items with the lowest score
    static List<Integer> WorstItems(double[] vals) {
        List<Integer> worstItems = new ArrayList<Integer>();
        double worstVal = +10000;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] <= worstVal) {
                if (vals[i] < worstVal) {
                    worstVal = vals[i];
                    worstItems.clear();
                }
                worstItems.add(i);
            }
        }
        return worstItems;
    }

    static List<Integer> WorstItemsWithThreshold(double[] vals, double thr) {
        List<Integer> worstItems = new ArrayList<Integer>();
        double worstVal = +10000;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] <= worstVal) {
                if (vals[i] < worstVal) {
                    worstVal = vals[i];
                    worstItems.clear();
                }
                if (vals[i] < thr)
                    worstItems.add(i);
            }
        }
        return worstItems;
    }

    static HashSet<String> makeSet(String str) {
        HashSet<String> hs = new HashSet<String>();
        String[] words = str.split(separators);
        for (int i = 0; i < words.length; ++i) {
            String word = words[i];
            hs.add(word);
        }
        return hs;
    }

    static HashSet<String> makeSet(String[] strArr) {
        HashSet<String> hs = new HashSet<String>();
        for (String str : strArr) {
            String strTrimmed = str.trim();
            hs.add(strTrimmed);
        }
        return hs;
    }
}
