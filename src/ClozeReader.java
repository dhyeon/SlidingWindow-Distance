
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ClozeReader {
	public static List<ClozeInstance> clozeAll = new ArrayList<ClozeInstance>();

	public static void main(String[] args) {
		readAll();
	}

	public static void readAll() {
		String initials = "data/";
		System.out.println("Starting to read the files! ");
		//readTSV(initials + "wdw3.test.tsv");
		//readTSV(initials + "wdw.test.tsv");
		//readTSV(initials + "papert.test.tsv");
		//readTSV(initials + "paperls.test.tsv");
		//readTSV(initials + "cbt.test.tsv");
		readTSV(initials + "cnn.test.tsv");

		System.out.println("Done with reading the files! ");
	}

	public static void readTSV(String f) {
		String ansF = f.replace(".tsv", ".ans");

		List<String> answers = new ArrayList<String>();
		List<Integer> answersInt = new ArrayList<Integer>();

		// Read Answer File
		try {
			InputStream ips = new FileInputStream(ansF);
			InputStreamReader ipsr = new InputStreamReader(ips);
			BufferedReader br = new BufferedReader(ipsr);
			String line;
			while ((line = br.readLine()) != null) {
				String optString = line.substring(1, 2);
				int opt = Integer.parseInt(optString);
				answers.add(line);
				answersInt.add(opt);

			}
			br.close();
		} catch (Exception e) {
			System.out.println(e.toString());
		}

		// Read Question File
		int it = 0;
		try {
			InputStream ips = new FileInputStream(f);
			InputStreamReader ipsr = new InputStreamReader(ips);
			BufferedReader br = new BufferedReader(ipsr);
			String line;
			while ((line = br.readLine()) != null) {
				//System.out.println(line);
				String[] split = line.split("\t");
				//System.out.println(split.length);
				ClozeInstance ins = new ClozeInstance();
				ins.instanceInfo = split[0];
				ins.turkInfo = split[1];
				ins.story = split[2];
				ins.questions = split[3].replace("one: ", "");
				ins.NeedsMultipleSentences = false;
				
				for (int j = 4; j < split.length; j++) {
					//System.out.println("j = " + j + " ----> " + split[j]);
					ins.options[j-4] = split[j];
				}
				ins.correctAnswers = answers.get(it);
				ins.correctAnswersInt = answersInt.get(it);
				it++;
				
				clozeAll.add(ins);
				/*
				System.out.println(ins.story);
				System.out.println(ins.questions);
				
				for(String s: ins.options){
					System.out.println(s);
				}
				
				System.out.println(ins.correctAnswers);
				System.out.println(ins.correctAnswersInt);
				*/
			}
			br.close();
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}
}
