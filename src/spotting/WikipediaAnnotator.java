package spotting;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.wikipedia.miner.annotation.Disambiguator;
import org.wikipedia.miner.annotation.Topic;
import org.wikipedia.miner.annotation.TopicDetector;
import org.wikipedia.miner.annotation.preprocessing.DocumentPreprocessor;
import org.wikipedia.miner.annotation.preprocessing.PreprocessedDocument;
import org.wikipedia.miner.annotation.preprocessing.WikiPreprocessor;
import org.wikipedia.miner.annotation.tagging.DocumentTagger;
import org.wikipedia.miner.annotation.tagging.DocumentTagger.RepeatMode;
import org.wikipedia.miner.annotation.tagging.WikiTagger;
import org.wikipedia.miner.annotation.weighting.LinkDetector;
import org.wikipedia.miner.model.Label;

public class WikipediaAnnotator {

	DocumentPreprocessor _preprocessor;
	Disambiguator _disambiguator;
	TopicDetector _topicDetector;
	LinkDetector _linkDetector;
	DocumentTagger _tagger;
	// Wikipedia _wikipedia;
	//static Wikisaurus thesaurus = null;
	static FoldedWikisaurus thesaurus = null;
	DecimalFormat _df = new DecimalFormat("#0%");

	public WikipediaAnnotator() throws Exception {

		// WikipediaConfiguration conf = new WikipediaConfiguration(
		// new File(
		// "/home/kanika/workspace/wikipedia-miner-1.2.0/wikipedia-config.xml"));

		// conf.clearDatabasesToCache() ;
		//thesaurus = new Wikisaurus();
		thesaurus = new FoldedWikisaurus();
		//_wikipedia = new Wikipedia(conf, false);
		_preprocessor = new WikiPreprocessor(thesaurus._wikipedia);
		_disambiguator = new Disambiguator(thesaurus._wikipedia);
		_disambiguator
				.loadClassifier(new File(
						"/home/kanika/workspace/wikipedia-miner-1.2.0/models/annotate/disambig_en_In.model"));
		_topicDetector = new TopicDetector(thesaurus._wikipedia,
				_disambiguator, true, false);

		_linkDetector = new LinkDetector(thesaurus._wikipedia);
		_linkDetector
				.loadClassifier(new File(
						"/home/kanika/workspace/wikipedia-miner-1.2.0/models/annotate/detect_en_In.model"));
		_tagger = new WikiTagger();

	}
	
	public HashMap<String, Label.Sense[]> annotate(String originalMarkup) {
		HashMap<String, Label.Sense[]> mention2ent = new HashMap<String, Label.Sense[]>();
		try {

			long prestartTime = System.currentTimeMillis();

			PreprocessedDocument doc = _preprocessor.preprocess(originalMarkup);
			
			long preendTime = System.currentTimeMillis();
			
			long diff1 = (preendTime - prestartTime);

			System.out.println("Time taken by preprocessor : " + diff1 + " milliseconds");
			
			long topstartTime = System.currentTimeMillis();

			Collection<Topic> allTopics = _topicDetector.getTopics(doc, null);
			System.out.println("\nAll detected topics:");
			
			long topendTime = System.currentTimeMillis();
			
			long diff2 = (topendTime - topstartTime);

			System.out.println("Time taken by topicdetector : " + diff2 + " milliseconds");

			for (Topic t : allTopics)
				System.out.println(" - " + t.getTitle());
			
			long besttopstartTime = System.currentTimeMillis();

			ArrayList<Topic> bestTopics = _linkDetector.getBestTopics(
					allTopics, 0.1);
			
			long besttopendTime = System.currentTimeMillis();
			
			long diff3 = (besttopendTime - besttopstartTime);

			System.out.println("Time taken by besttopicdetector : " + diff3 + " milliseconds");

			for (Topic t : allTopics)
				System.out.println(" - " + t.getTitle());

			System.out.println("\nTopics that are probably good links:");
			for (Topic t : bestTopics)
				System.out.println(" - " + t.getTitle() + "["+ _df.format(t.getWeight()) + "]");

			String newMarkup = _tagger.tag(doc, allTopics, RepeatMode.ALL);
			System.out.println("\nAugmented markup:\n" + newMarkup + "\n");
			
			String ResultString = null;
			try {
				Pattern regex = Pattern.compile("\\[\\[(.*?)\\]\\]",
						Pattern.DOTALL | Pattern.MULTILINE);
				Matcher regexMatcher = regex.matcher(newMarkup);
				ArrayList<String> mentions = new ArrayList<String>();
				ArrayList<Integer> offsets = new ArrayList<Integer>();
				int curr = 0;
				while (regexMatcher.find()) {
					ResultString = regexMatcher.group(1);
					String[] arr = ResultString.split("\\|");

					if (arr.length > 1) {
						mentions.add(arr[1]);
						int off = originalMarkup.indexOf(arr[1], curr);
						offsets.add(off);
						curr = off + arr[1].length()-1;

					} else {
						mentions.add(arr[0]);
						int off = originalMarkup.indexOf(arr[0], curr);
						offsets.add(off);
						curr = off + arr[0].length()-1;
					}

				}
				for (int i = 0; i < mentions.size(); i++) {
					System.out.println(mentions.get(i) + "_" +
					offsets.get(i));

					Label label = thesaurus._wikipedia
							.getLabel(mentions.get(i));
					Label.Sense[] sens = label.getSenses();
					Vector<Label.Sense> filteredsenses = new Vector();
					for (int j = 0; j < sens.length; j++) {
						if(sens[j].getPriorProbability() > 0.1){
							filteredsenses.add(sens[j]);
							System.out.println(sens[j].getTitle() + "  " + sens[j].getPriorProbability());
						}
						//mention2ent.get(mentions.get(i) + "_" + offsets.get(i)).add(sens[j].getTitle());
					}
					Label.Sense[] newsens = new Label.Sense[filteredsenses.size()];
					for (int j = 0; j < filteredsenses.size(); j++) {
						newsens[j] = filteredsenses.elementAt(j);
						//mention2ent.get(mentions.get(i) + "_" + offsets.get(i)).add(sens[j].getTitle());
					}
					mention2ent.put(mentions.get(i) + "_" + offsets.get(i),
//							sens);
							newsens);


				}
			} catch (PatternSyntaxException ex) {
				ex.printStackTrace();
			}

		}

		catch (Exception e) {
			e.printStackTrace();
		}
		return mention2ent;
	}

	public static void main(String args[]) throws Exception {

		WikipediaAnnotator annotator = new WikipediaAnnotator();
		System.out.println("a5");
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		System.out.println("a6");
		while (true) {
			System.out.println("Enter snippet to annotate (or ENTER to quit):");
			String line = reader.readLine();

			if (line.trim().length() == 0)
				break;
			HashMap<String, Label.Sense[]> ment2ent = annotator.annotate(line);
			// for (String key : ment2ent.keySet()) {
			// System.out.println("--------------------" + key
			// + "----------------------");
			// for (int j = 0; j < ment2ent.get(key).size(); j++)
			// System.out.println(ment2ent.get(key).get(j));
			// }

		}
	}

}
