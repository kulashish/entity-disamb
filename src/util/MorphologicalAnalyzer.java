package util;

import java.util.ArrayList;

import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;
import edu.smu.tspell.wordnet.impl.file.Morphology;

public class MorphologicalAnalyzer {

	public static ArrayList<String> analyze(String mention) {

		System.setProperty("wordnet.database.dir",
				"/home/kanika/workspace/EntityDisamb/WordNet-2.0/dict/");
		WordNetDatabase database = WordNetDatabase.getFileInstance();
		Morphology id = Morphology.getInstance();
		String[] arr = id.getBaseFormCandidates(mention, SynsetType.NOUN);
		ArrayList<String> li = new ArrayList<String>();
		for (String a : arr) {
			li.add(a);
			// System.out.println(a);
		}
		li.add(mention);
		// System.out.println(li);
		return li;

	}

	public static void main(String args[]) {

		System.setProperty("wordnet.database.dir",
				"/home/kanika/workspace/EntityDisamb/WordNet-2.0/dict/");

		analyze("experienced");
	}
}
